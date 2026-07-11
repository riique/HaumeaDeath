# HaumeaDeath

<p align="center">
  <strong>Morte com coordenadas, inventário pré-morte e restauração segura</strong><br/>
  Quando o jogador morre, o chat mostra o local e botões para <em>ver</em> ou <em>recuperar</em> o inventário.
</p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20.2-62b47a?style=for-the-badge&logo=minecraft&logoColor=white" />
  <img alt="Loader" src="https://img.shields.io/badge/Loader-Fabric-dbb69b?style=for-the-badge" />
  <img alt="Java" src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" />
  <img alt="Version" src="https://img.shields.io/badge/Version-1.0.0-informational?style=for-the-badge" />
</p>

---

## O que é

**HaumeaDeath** é um mod Fabric focado em servidor que, em cada morte:

1. **Salva um snapshot** do inventário (main + armadura + offhand) **antes** do drop vanilla  
2. Envia no chat do jogador:
   - **Coordenadas** da morte + dimensão  
   - **`[Ver inventário]`** — abre um baú **somente leitura** com o inventário da morte  
   - **`[Copiar inventário]`** — abre uma **GUI de confirmação** (verde = restaurar, vermelho = cancelar)  
3. Ao confirmar, **restaura** o inventário da morte no jogador que renasceu  

### Regras de segurança

| Regra | Detalhe |
|-------|---------|
| Só o dono | Apenas o UUID do jogador que morreu pode ver/restaurar **o próprio** inventário |
| Uma vez | Cada snapshot só pode ser **restaurado uma vez** |
| Sem apagar itens atuais | Itens que o jogador tiver na hora da restauração são **dropados no chão** (não somem em silêncio) |
| Drops do mundo | O loot no chão da morte **não é removido** automaticamente |

### Exemplo no chat

```text
HaumeaDeath » Você morreu em -25, 113, -383 [Overworld]
Ações:  [Ver inventário]  [Copiar inventário]
```

> **Dica:** após morrer, **renasça** e clique nos botões no histórico do chat (comandos por clique funcionam melhor depois do respawn).

---

## Comandos

| Comando | Descrição |
|---------|-----------|
| `/haumeadeath view` | Abre a GUI de visualização do inventário da última morte |
| `/haumeadeath restore` | Abre a GUI de confirmação para restaurar o inventário |

Os botões do chat executam esses comandos.

---

## Requisitos

- **Minecraft** `1.20.2`
- **Fabric Loader** `≥ 0.15.0`
- **Fabric API** para `1.20.2` (ex.: `0.91.6+1.20.2`)
- **Java** `17+` no servidor

> **Nota:** mods Fabric são versionados por Minecraft. Este build mira **1.20.2**. Paper/Spigot/Forge **não** carregam este jar.

---

## Instalação (servidor)

1. Instale o [Fabric](https://fabricmc.net/use/server/) no servidor `1.20.2`.
2. Coloque o **Fabric API** em `mods/`.
3. Baixe o `HaumeaDeath-1.0.0.jar` (Release ou build local) e coloque em `mods/`.
4. Reinicie o servidor.
5. Morra (em survival) e confira o chat — ou use `/haumeadeath view` após uma morte.

Não é necessário no cliente — tudo roda no servidor. Jogadores só precisam de um cliente compatível com o server.

---

## Build a partir do código

```bash
git clone https://github.com/riique/HaumeaDeath.git
cd HaumeaDeath

# Java 17+ recomendado para o toolchain
./gradlew build
```

O jar sai em:

```text
build/libs/HaumeaDeath-1.0.0.jar
```

Copie para a pasta `mods/` do servidor e reinicie.

---

## Estrutura do projeto

```text
HaumeaDeath/
├── src/main/java/com/haumea/death/
│   ├── HaumeaDeathMod.java              # morte, chat, comandos, restore
│   ├── DeathRecord.java                 # snapshot NBT do inventário
│   ├── DeathStorage.java                # armazenamento em memória
│   ├── ViewOnlyScreenHandler.java       # GUI só visualização
│   └── ConfirmRestoreScreenHandler.java # GUI confirmar / cancelar
├── src/main/resources/
│   └── fabric.mod.json
├── build.gradle
├── gradle.properties
└── README.md
```

---

## Desenvolvimento

| Item | Valor |
|------|--------|
| Mod ID | `haumeadeath` |
| Grupo Maven | `com.haumea` |
| Entrypoint | `com.haumea.death.HaumeaDeathMod` |
| Loom | `1.7.4` |
| Yarn | `1.20.2+build.4` |

Snapshot de morte via `ServerLivingEntityEvents.ALLOW_DEATH` (antes dos drops). GUIs usam `GenericContainerScreenHandler` com slots bloqueados.

Para outra versão de Minecraft, ajuste `gradle.properties` (`minecraft_version`, `yarn_mappings`, `fabric_version`) e recompile. APIs podem mudar entre versões.

---

## Licença

Distribuído sob a licença **[MIT](LICENSE)**. Use, modifique e redistribua à vontade.

---

<p align="center">
  Feito com ☕ para a comunidade Haumea<br/>
  Irmão do <a href="https://github.com/riique/HaumeaPing">HaumeaPing</a>
</p>
