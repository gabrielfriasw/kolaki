# Publicando uma versão

As releases oficiais são criadas automaticamente ao enviar uma tag no formato
`vMAJOR.MINOR.PATCH`, por exemplo `v1.0.0`.

O workflow valida o projeto, monta um APK assinado, gera o SHA-256, atesta sua
origem e publica ambos na release do GitHub.

## Segredos necessários

- `KOLAKI_KEYSTORE_BASE64`
- `KOLAKI_KEYSTORE_PASSWORD`
- `KOLAKI_KEY_ALIAS`
- `KOLAKI_KEY_PASSWORD`

A chave de assinatura deve ser mantida em backup seguro. Perdê-la impede que
novas versões atualizem uma instalação existente.

## Link estável

O site pode usar diretamente:

`https://github.com/gabrielfriasw/kolaki/releases/latest/download/kolaki.apk`
