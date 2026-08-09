# KolaKi

[![ci](https://github.com/gabrielfriasw/kolaki/actions/workflows/ci.yml/badge.svg)](https://github.com/gabrielfriasw/kolaki/actions/workflows/ci.yml)
[![release](https://img.shields.io/github/v/release/gabrielfriasw/kolaki?display_name=tag)](https://github.com/gabrielfriasw/kolaki/releases/latest)

KolaKi é um criador e instalador de figurinhas para WhatsApp, feito em Kotlin
e Jetpack Compose. Seu código-fonte é público para consulta. Sem anúncio,
cadastro, assinatura ou envio das fotos para um servidor: o processamento
acontece no próprio celular.

> Cria. Kola. Manda.

## O que já funciona

- onboarding, home, pacotes, editor, configurações, sobre e tela de sucesso;
- seleção múltipla pelo Android Photo Picker;
- conversão local para WebP estático em 512 × 512 e até 100 KB;
- conversão de GIFs para figurinhas animadas em WebP;
- pacotes de 3 a 30 figurinhas, conforme o contrato oficial do WhatsApp;
- editor com recorte, zoom, deslocamento e rotação;
- persistência local de pacotes e preferências;
- tema claro, escuro ou do sistema;
- `ContentProvider` dinâmico e intent oficial `ENABLE_STICKER_PACK`;
- ícone adaptativo e interface neo-brutalista acessível.

## Stack

O catálogo central fica em `gradle/libs.versions.toml`. A base usa:

- Kotlin 2.4.10 e JDK 17;
- Android Gradle Plugin 9.3.1 e Gradle 9.7.0;
- compile/target SDK 37 e min SDK 24;
- Compose BOM 2026.06.01;
- Core 1.19.0, Activity 1.13.0, Lifecycle 2.11.0 e Navigation 2.9.8;
- DataStore 1.2.1, ExifInterface 1.4.2, Coil 3.5.0 e FFmpegKit 8.1.7.

Somente releases estáveis são usadas.

## Download

[Baixar o APK mais recente](https://github.com/gabrielfriasw/kolaki/releases/latest/download/kolaki.apk)

Cada versão oficial inclui o APK assinado e seu arquivo SHA-256. O Android pode
pedir autorização para instalar aplicativos obtidos pelo navegador.

## Rodando

1. Instale Android Studio com JDK 17 e Android SDK 37.
2. Abra a raiz deste repositório.
3. Rode `./gradlew assembleDebug` ou execute a configuração `app` no Studio.

O APK de desenvolvimento é gerado em
`app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura

- `data/`: modelos, preferências, persistência e processamento das imagens;
- `provider/`: contrato de leitura consumido pelo WhatsApp;
- `ui/components/`: componentes neo-brutalistas e mascote vetorial;
- `ui/screens/`: fluxo completo do aplicativo;
- `ui/theme/`: tokens de cor e tipografia do conceito visual.

## Privacidade

O app usa o Photo Picker e não pede acesso amplo à galeria. As imagens
selecionadas são lidas, processadas e armazenadas no diretório privado do app.
Não há permissão de internet no manifesto.

## Licença

O código-fonte é publicado somente para consulta, com todos os direitos
reservados. Sem autorização prévia por escrito, não é permitido usar,
modificar, distribuir, compilar ou vender o código. APKs oficiais podem ser
instalados e usados para fins pessoais e não comerciais. O KolaKi não é
software open source segundo a definição da OSI.

Veja `LICENSE` e `THIRD_PARTY_NOTICES.md`.
