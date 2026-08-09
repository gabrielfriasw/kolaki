package com.kolvexo.kolaki

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kolvexo.kolaki.data.AppPreferences
import com.kolvexo.kolaki.data.PackRepository
import com.kolvexo.kolaki.data.StickerEdit
import com.kolvexo.kolaki.data.StickerPack
import com.kolvexo.kolaki.data.ThemeMode
import com.kolvexo.kolaki.ui.screens.AboutScreen
import com.kolvexo.kolaki.ui.screens.EditorScreen
import com.kolvexo.kolaki.ui.screens.HomeScreen
import com.kolvexo.kolaki.ui.screens.LicensesScreen
import com.kolvexo.kolaki.ui.screens.OnboardingScreen
import com.kolvexo.kolaki.ui.screens.PackDetailsScreen
import com.kolvexo.kolaki.ui.screens.SettingsScreen
import com.kolvexo.kolaki.ui.screens.SplashScreen
import com.kolvexo.kolaki.ui.screens.SuccessScreen
import com.kolvexo.kolaki.ui.theme.KolaKiTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Pack = "pack/{packId}"
    const val Editor = "editor/{packId}/{fileName}"
    const val Settings = "settings"
    const val About = "about"
    const val Licenses = "licenses"
    const val Success = "success"

    fun pack(id: String) = "pack/$id"
    fun editor(packId: String, fileName: String) = "editor/$packId/$fileName"
}

@Composable
fun KolakiApp(repository: PackRepository, preferences: AppPreferences) {
    val packs by repository.packs.collectAsStateWithLifecycle()
    val processing by repository.processing.collectAsStateWithLifecycle()
    val onboardingFlow = remember(preferences) { preferences.onboardingDone.map<Boolean, Boolean?> { it } }
    val onboardingDone by onboardingFlow.collectAsStateWithLifecycle(initialValue = null)
    val themeMode by preferences.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val whatsAppResult = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            nav.navigate(Routes.Success)
        } else {
            val validation = result.data?.getStringExtra("validation_error")
            Toast.makeText(
                context,
                validation?.let { "O WhatsApp recusou o pacote: $it" }
                    ?: "O pacote não foi adicionado. Confere se o WhatsApp está atualizado.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun addToWhatsApp(pack: StickerPack) {
        val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply {
            putExtra("sticker_pack_id", pack.identifier)
            putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            putExtra("sticker_pack_name", pack.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            whatsAppResult.launch(Intent.createChooser(intent, "Adicionar ao WhatsApp"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp não encontrado neste celular.", Toast.LENGTH_LONG).show()
        }
    }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            .onFailure { Toast.makeText(context, "Não consegui abrir esse link.", Toast.LENGTH_SHORT).show() }
    }

    KolaKiTheme(themeMode) {
        NavHost(navController = nav, startDestination = Routes.Splash) {
            composable(Routes.Splash) {
                SplashScreen {
                    val destination = if (onboardingDone == true) Routes.Home else Routes.Onboarding
                    nav.navigate(destination) { popUpTo(Routes.Splash) { inclusive = true } }
                }
            }
            composable(Routes.Onboarding) {
                val finish: () -> Unit = {
                    scope.launch {
                        preferences.finishOnboarding()
                        nav.navigate(Routes.Home) { popUpTo(Routes.Onboarding) { inclusive = true } }
                    }
                }
                OnboardingScreen(onSkip = finish, onFinish = finish)
            }
            composable(Routes.Home) {
                HomeScreen(
                    packs = packs,
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onOpenPack = { nav.navigate(Routes.pack(it)) },
                    onCreatePack = { name ->
                        scope.launch {
                            val pack = repository.createPack(name)
                            nav.navigate(Routes.pack(pack.identifier))
                        }
                    },
                )
            }
            composable(
                Routes.Pack,
                arguments = listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                val pack = packs.firstOrNull { it.identifier == packId }
                if (pack == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    PackDetailsScreen(
                        pack = pack,
                        processing = processing,
                        fileFor = { repository.stickerFile(packId, it) },
                        onBack = { nav.popBackStack() },
                        onImagesSelected = { uris ->
                            scope.launch {
                                repository.addImages(packId, uris).onFailure {
                                    Toast.makeText(context, it.message ?: "Algo deu errado.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onEditSticker = { nav.navigate(Routes.editor(packId, it)) },
                        onAddToWhatsApp = { addToWhatsApp(pack) },
                    )
                }
            }
            composable(
                Routes.Editor,
                arguments = listOf(
                    navArgument("packId") { type = NavType.StringType },
                    navArgument("fileName") { type = NavType.StringType },
                ),
            ) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                val fileName = entry.arguments?.getString("fileName").orEmpty()
                val pack = packs.firstOrNull { it.identifier == packId }
                val sticker = pack?.stickers?.firstOrNull { it.fileName == fileName }
                EditorScreen(
                    file = repository.stickerFile(packId, fileName),
                    animated = sticker?.animated == true,
                    imageVersion = pack?.imageDataVersion ?: 0,
                    processing = processing,
                    onClose = { nav.popBackStack() },
                    onEdit = { edit: StickerEdit, onComplete: () -> Unit ->
                        scope.launch {
                            repository.editSticker(packId, fileName, edit)
                                .onSuccess { onComplete() }
                                .onFailure {
                                    Toast.makeText(context, it.message ?: "Algo deu errado.", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                )
            }
            composable(Routes.Settings) {
                val allFiles = packs.flatMap { pack ->
                    pack.stickers.flatMap { sticker ->
                        listOfNotNull(
                            repository.stickerFile(pack.identifier, sticker.fileName),
                            sticker.sourceFileName?.let { repository.stickerFile(pack.identifier, it) },
                        )
                    } +
                        repository.stickerFile(pack.identifier, pack.trayImageFile)
                }
                SettingsScreen(
                    themeMode = themeMode,
                    stickerCount = packs.sumOf { it.stickers.size },
                    usedBytes = allFiles.filter { it.exists() }.sumOf { it.length() },
                    onBack = { nav.popBackStack() },
                    onThemeChange = { scope.launch { preferences.setTheme(it) } },
                    onAbout = { nav.navigate(Routes.About) },
                    onSourceCode = { openUrl(BuildConfig.REPOSITORY_URL) },
                    onLicenses = { nav.navigate(Routes.Licenses) },
                )
            }
            composable(Routes.About) {
                AboutScreen(
                    onBack = { nav.popBackStack() },
                    onGitHub = { openUrl(BuildConfig.REPOSITORY_URL) },
                )
            }
            composable(Routes.Licenses) {
                LicensesScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.Success) {
                SuccessScreen(
                    onOpenWhatsApp = {
                        val launch = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                            ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                        if (launch != null) context.startActivity(launch)
                    },
                    onDone = { nav.navigate(Routes.Home) { popUpTo(Routes.Home) { inclusive = false } } },
                )
            }
        }
    }
}
