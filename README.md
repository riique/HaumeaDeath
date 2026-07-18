# HaumeaDeath

Veja onde morreu, confira o inventário anterior à morte e restaure os itens com confirmação — sem depender de um mod no cliente.

O HaumeaDeath é um mod Fabric para servidores Minecraft 1.20.2. Antes do fluxo vanilla de morte, ele guarda uma cópia do inventário do jogador, informa coordenadas e dimensão no chat e oferece ações clicáveis para visualizar ou recuperar o snapshot.

## Fluxo do jogador

1. O servidor registra inventário principal, armadura e mão secundária antes dos drops.
2. O chat mostra coordenadas e dimensão da morte.
3. **Ver inventário** abre um baú somente para leitura.
4. **Copiar inventário** abre uma tela de confirmação.
5. Ao confirmar, os itens atuais são derrubados aos pés do jogador e o snapshot é aplicado.

Exemplo:

```text
HaumeaDeath » Você morreu em -25, 113, -383 [Overworld]
Ações: [Ver inventário] [Copiar inventário]
```

Depois de renascer, use os botões no histórico do chat ou os comandos.

## Comandos

| Comando | Ação |
| --- | --- |
| `/haumeadeath view` | abre o último inventário salvo em modo somente leitura |
| `/haumeadeath restore` | abre a confirmação de restauração |

As ações são vinculadas ao UUID: cada jogador só acessa o próprio snapshot.

## Regras importantes

- Apenas a morte mais recente de cada jogador fica disponível.
- Cada snapshot só pode ser restaurado uma vez durante a execução atual do servidor.
- Itens carregados no momento da restauração são derrubados no chão antes da troca.
- Os drops criados pela morte original não são removidos.

> [!WARNING]
> Como os drops da morte permanecem no mundo, restaurar o snapshot pode duplicar itens caso alguém também recolha os drops originais. Use o mod apenas em servidores cuja política aceite esse comportamento.

## Estado do armazenamento

Os snapshots ficam em memória, em um `ConcurrentHashMap`.

Isso significa que:

- reiniciar ou parar o servidor apaga todos os snapshots;
- uma nova morte substitui o registro anterior;
- não há banco de dados, arquivo de recuperação ou histórico;
- a marca de “já restaurado” também não sobrevive a reinícios.

Essa limitação deve ser considerada antes de usar o mod em produção.

## Requisitos

- Minecraft Java Edition `1.20.2`;
- Fabric Loader `0.15.0` ou superior;
- Fabric API compatível, configurada no projeto como `0.91.6+1.20.2`;
- Java 17 ou superior no servidor.

O JAR não é compatível com Paper, Spigot ou Forge. O cliente não precisa instalar o mod.

## Instalação

1. Instale o [Fabric Server](https://fabricmc.net/use/server/) para Minecraft 1.20.2.
2. Coloque o Fabric API em `mods/`.
3. Coloque `HaumeaDeath-1.0.0.jar` em `mods/`.
4. Reinicie o servidor.
5. Faça um teste controlado antes de liberar o recurso aos jogadores.

## Compilar

No Linux ou macOS:

```bash
git clone https://github.com/riique/HaumeaDeath.git
cd HaumeaDeath
./gradlew build
```

No PowerShell:

```powershell
git clone https://github.com/riique/HaumeaDeath.git
Set-Location HaumeaDeath
.\gradlew.bat build
```

O JAR é gerado em:

```text
build/libs/HaumeaDeath-1.0.0.jar
```

## Estrutura

| Arquivo | Responsabilidade |
| --- | --- |
| `HaumeaDeathMod.java` | evento de morte, mensagens, comandos e restauração |
| `DeathRecord.java` | serialização NBT do inventário |
| `DeathStorage.java` | último snapshot em memória por UUID |
| `ViewOnlyScreenHandler.java` | visualização com slots bloqueados |
| `ConfirmRestoreScreenHandler.java` | confirmação ou cancelamento |
| `fabric.mod.json` | metadados e dependências Fabric |

## Desenvolvimento

| Item | Valor |
| --- | --- |
| Mod ID | `haumeadeath` |
| Versão | `1.0.0` |
| Loom | `1.7.4` |
| Yarn | `1.20.2+build.4` |
| Entrypoint | `com.haumea.death.HaumeaDeathMod` |

O snapshot é capturado por `ServerLivingEntityEvents.ALLOW_DEATH`, antes dos drops vanilla. Ao atualizar a versão do Minecraft, revise as APIs de inventário, NBT, eventos e telas.

## Contribuição

Ao relatar um problema, informe versão do servidor, Fabric Loader, Fabric API e a sequência entre morte, respawn e restauração. Não teste correções com itens valiosos sem antes fazer backup do mundo.

## Licença

Distribuído sob a [Licença MIT](LICENSE).
