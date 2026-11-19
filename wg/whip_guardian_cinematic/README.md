# Whip Guardian — World Breaker

**Projeto-fonte de addon para Minecraft Java 1.21.1 + NeoForge 21.1.x + RimuLib 1.3.0 + GeckoLib 4.8.2 + The Shooting Star [Demo] 1.6.0.**

O objetivo é acrescentar `world_breaker` ao **Stellar Remote** usando `SkillApi.whenRegistered("shooting_star_demo", ...)` / `SkillApi.extend(...)`, sem substituir o remote, nem copiar o código proprietário do The Shooting Star. O addon instala uma entidade real (observável por outros jogadores), a anima com GeckoLib, desenha um shader de mundo pela RimuLib, cria planos de câmera com FilmApi e executa fisicamente o golpe no servidor, mesmo com a câmera pulada.

> **Status:** pacote de **código-fonte e recursos preparado**, gerado e revisado estaticamente. **Não é um `.jar` compilado/testado**, pois os JARs necessários, wrapper Gradle e ambiente Minecraft gráfico não foram anexados. Os nomes / assinaturas das APIs públicas foram consultados nos guias RimuLib e no código público do SA Series; ainda é necessário compilar e testar no jogo. O conversor Python é experimental: reexportar pelo plugin GeckoLib do Blockbench é preferível se as animações saírem deslocadas/invertidas.

## Storyboard original (20 ticks = 1 segundo)

| Tempo / tick | Acontecimento | Câmera |
|---|---|---|
| 0–20 (0–1s) | Selo geométrico no chão, céu escurece, linhas de leitura da abertura | Estabelecimento lateral |
| 20–60 (1–3s) | Portal elíptico ~38 blocos de altura, halo cromático, raios fractais, modelo colossal cresce de escala quase zero durante a animação extra `portal_arrival` | Revelação com travelling |
| 60–140 (3–7s) | `aura_farm` começa. Halo, arcos concêntricos, símbolos luminosos iluminam a superfície real | Órbita de 3/4 |
| 140–205 (7–10,25s) | Aos **4 s da aura**, surgem 5 tornados volumétricos e forças de impulso. O giro desacelera próximo dos 7,25 s locais | Órbita aérea sobre os tornados |
| 205–220 (10,25–11s) | Tornados perdem energia, pequena pausa de contraste, compressão da aura | Câmera baixa, foco nos braços |
| 220–263 (11–13,15s) | `ground_slam` começa. Ondas antecipatórias e exposição reduzida | Plano fechado no golpe |
| **263 (13,15s)** | **Impacto exato aos 2,15 s do slam**: flash limitado, impact frame monocromático, tremor, dano e craterização no servidor | Corte de choque e afastamento |
| 264–310 (13,2–15,5s) | Fragmentos, anéis expansivos, fissuras e destruição progressiva do terreno | Grande angular recuando |
| 310–350 (15,5–17,5s) | Névoa luminosa e anéis se dissipam, câmera devolvida | Visão geral do resultado |

**Observação crucial:** a `aura_farm` dura exatamente 160 ticks e o `ground_slam` 60 ticks. As duas animações originais mantêm seus nomes completos, durações e tempos de keyframes originais, incluindo as 4.023 interpolações Catmull-Rom presentes no `.bbmodel`. A escala de renderização foi escolhida como `11.0`, que transforma a altura da geometria de ~2,84 blocos em **~31,2 blocos**. Não precisa ampliar o `.bbmodel` original; mantenha o rig e as animações intactos.

## Estrutura entregue

- `whip_guardian.bbmodel` — original completo editável.
- `scripts/export_bbmodel.py` — exporta 30 ossos, 113 cubos, textura embutida 512×512 e 2 animações originais GeckoLib com interpolação Catmull-Rom + 1 transição de invocação.
- `src/main/resources/assets/whip_cinematic/geo/whip_guardian.geo.json` — geometria GeckoLib.
- `src/main/resources/assets/whip_cinematic/animations/whip_guardian.animation.json` — `ground_slam` / `aura_farm` sem retemporização.
- `src/main/resources/assets/whip_cinematic/textures/entity/whip_guardian.png` — textura original.
- `src/main/resources/assets/whip_cinematic/shaders/fx/whip_world.fsh` — portal procedural, chão energético por profundidade, 5 tornados analíticos, céu estilizado, ondas, fissuras/impact frames.
- `src/main/java/.../WhipCinematic.java` — mod bootstrap e extensão de skill no Remote.
- `.../entity/WhipGuardian.java` — entidade sincronizada e relógio servidor/cliente.
- `.../client/WhipClient.java` — passes RimuLib, bloom, shake, flash, planos FilmApi.
- `.../server/WhipServer.java` — alvo, invocação, impulso, dano, craterização progressiva com limites.

## Como compilar / instalar

1. Instale **Java 21**, Gradle (ou copie o wrapper da MDK NeoForge 1.21.1 para este diretório) e acesso às dependências Maven.
2. Baixe **RimuLib 1.3.0 NeoForge** oficial e coloque em `libs/rimulib-1.3.0-neoforge.jar`. O script `build.gradle` referencia essa cópia local; não redistribua o JAR de terceiros.
3. O `build.gradle` obtém **GeckoLib 4.8.2** do Maven GeckoLib; no Minecraft, instale GeckoLib, RimuLib e **The Shooting Star [Demo] 1.6.0 NeoForge** juntos com este addon.
4. Execute `gradle build` (ou `./gradlew build`, após adicionar wrapper). O resultado pretendido é `build/libs/whip_cinematic-0.1.0.jar` **somente se a compilação terminar sem erros**.
5. Entre num mundo de testes, segure o **Stellar Remote**. Na lista de habilidades, encontre **Whip Guardian — World Breaker**. A tecla padrão é **J**, alterável pelo menu do remote, quando a skill for reconhecida.
6. Faça testes de `aura_farm`, `ground_slam` e câmera com `F3`, gravando os ticks 60/140/205/220/263/280. Confira orientação e escala do modelo exportado. Confira também outro cliente vendo o poder sem assistir à cutscene.

## Controle de destruição

**AVISO: esse poder pode destruir blocos permanentes! Faça backup do mundo.** No código `WhipServer.java` há dois interruptores:

```java
public static final boolean REQUIRE_OP_FOR_TERRAIN = true;
public static final boolean DESTROY_TERRAIN = true;
```

- Sem permissão de operador: VFX, dano e knockback funcionam; a **craterização real** não é autorizada.
- A craterização exige também `mobGriefing=true` e carrega somente chunks já presentes (nunca gera terreno novo).
- `CRATER_RADIUS = 18` limita a área; o corte percorre anéis de 2 blocos por tick e limita substituições a `3800` por tick.
- Bedrock e outros blocos indestrutíveis, além de blocos com block entities, são preservados. Para proteger áreas de claims, adicione uma consulta ao sistema de proteção do servidor antes de `setBlock`.
- Dano e arremesso atingem criaturas e jogadores perto, exceto o conjurador. **Altere o dano conforme regras do seu servidor**.

## Pontos de validação obrigatórios

- **Integração:** `SkillApi.whenRegistered` efetivamente encontra o set do Remote na versão instalada. O código filtra sets visíveis com habilidade enum, como o SA Series faz. Se o nome ou forma do set mudar, revise esse filtro.
- **Controles de filme:** `FilmApi.automatic()` respeita a preferência do jogador e o filme padrão é pulável. O impacto no servidor continua com filme desligado; todas as entidades enxergam o mesmo evento.
- **GeckoLib:** teste o posicionamento do `whip_root` e do `root`. A exportação bruta de Blockbench pode exigir ajustes de sinal das rotações/UVs conforme a versão exata do plugin; **reexporte** os três arquivos com GeckoLib Animated Java Entity no Blockbench se houver diferenças.
- **Shaders:** a RimuLib 1.3.0 injeta o próprio `common.glsl`. Não adicione outro `#version` ou `#include` do common. Teste em cliente com qualidade baixa/alta, redimensionamento de janela, sombras/oclusão e Iris.
- **Escala:** o `EntityType` tem caixa de rastreio 16×36 blocos para não desaparecer por frustum culling; ajuste a caixa se aumentar a escala do renderer.
- **Desempenho:** 5 volumes × 4 amostras são ~20 testes de volume/pixel nas áreas que interceptam; em GPUs fracas, reduza para 3 tornados ou 2 amostras. O arquivo shader é original e não foi medido em cliente gráfico.
- **Acessibilidade:** `reduceFlashes` e `disableCamera` são variáveis no arquivo `WhipClient.java` (ainda não são configurações em menu). O projeto precisa expor esses toggles em config antes de publicar a modificação.

## Fontes técnicas consultadas

- RimuLib 1.3.0: https://docs.rimuru.cyou/rimulib/skills , https://docs.rimuru.cyou/rimulib/films , https://docs.rimuru.cyou/rimulib/fx , https://docs.rimuru.cyou/rimulib/reference
- Exemplo de addon para o Stellar Remote: https://github.com/Adamko-10/the-shooting-star-addition (referência de integração, não foi copiado seu código proprietário)
- GeckoLib 4: https://github.com/bernie-g/geckolib/wiki/Geckolib-4-Changes

Os efeitos visuais são autorais; o modelo e textura são os anexos que você forneceu. A ausência de um projeto Gradle seu impede editar diretamente sua instalação existente; este pacote é um **addon novo para colocar ao lado dos mods existentes**, não uma edição binária do JAR original.
