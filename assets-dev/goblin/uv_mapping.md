# Mapeamento UV — Goblin v0.1 / Piglin adulto 26.3

## Fonte e reprodutibilidade

Atlas real: **64 × 64 pixels**, RGBA na candidata. Origem UV no canto superior esquerdo. Retângulos em pixels, `(u,v) largura×altura`; limite final exclusivo. UV normalizado = coordenada / 64.

Textura original de referência: `assets/minecraft/textures/entity/piglin/piglin.png`, dentro de `/home/fabio/.gradle/caches/fabric-loom/26.3/minecraft-client.jar`. Lida em memória para conferir dimensões/hash; não foi extraída para o projeto.

SHA-256 da textura vanilla: `6ba539fa4ae905fd95f95984aec2845992fa92c748765ac9da0c01800d8ce69f`.

O Loom fornece classes em `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-1b0498e74c/26.3/` e no cache global `~/.gradle/caches/fabric-loom/26.3/`. O probe usa o classpath **client.runtimeClasspath** do próprio projeto.

`LayerDefinitions.createRoots()` associa `ModelLayers.PIGLIN` à definição produzida por `AdultPiglinModel.createBodyLayer()`, verificado no bytecode 26.3 com `javap -c`. O probe invoca essa definição e percorre os cubos já baked: vértices, normais, UV, dimensões, poses e flags reais estão em `uv_geometry.json`. Não foram deduzidos de um template antigo.

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle exportPiglinUv
./gradlew build
```

O probe cria `goblin_original.png` somente se não existir e recusa sobrescrever um backup diferente. Nenhum arquivo em `src/` é escrito pelas ferramentas. Os arquivos `assets-dev/` não pertencem aos source sets e não entram no JAR.

## Correspondência com o renderer

`GoblinRenderer` usa `new AdultPiglinModel(context.bakeLayer(ModelLayers.PIGLIN))`: mesma malha e UV do Piglin **adulto**, inclusive seus filhos de roupa. Não usa o modelo do bebê. Usa `PiglinRenderState` apenas para compatibilidade visual.

A escala não é idêntica: o Goblin aplica `(0.85,0.85,0.85)`; `PiglinRenderer` não sobrescreve o `scale` vazio de `LivingEntityRenderer`. A redução geométrica não muda as coordenadas UV nem exige reduzir a imagem. A sombra 0.42 do Goblin e 0.5 do Piglin também não mudam UV. Camadas de equipamento do renderer vanilla não fazem parte deste atlas.

## Cubos e retângulos das faces

Direita/esquerda são anatômicas. IDs correspondem às cores e painéis do guia PNG. Todas as 96 faces ficam no atlas e não há sobreposição de ilhas.

| ID / peça | Offset | Dimensões X×Y×Z | Topo | Base | Direita | Frente | Esquerda | Costas |
|---|---|---|---|---|---|---|---|---|
| 01 Tronco | [16.0,16.0] | [8.0,12.0,4.0] | (20,16) 8×4 | (28,16) 8×4 | (16,20) 4×12 | (20,20) 8×12 | (28,20) 4×12 | (32,20) 8×12 | 
| 02 Casaco externo | [16.0,32.0] | [8.0,12.0,4.0] | (20,32) 8×4 | (28,32) 8×4 | (16,36) 4×12 | (20,36) 8×12 | (28,36) 4×12 | (32,36) 8×12 | 
| 03 Cabeça | [0.0,0.0] | [10.0,8.0,8.0] | (8,0) 10×8 | (18,0) 10×8 | (0,8) 8×8 | (8,8) 10×8 | (18,8) 8×8 | (26,8) 10×8 | 
| 04 Focinho / nariz | [31.0,1.0] | [4.0,4.0,1.0] | (32,1) 4×1 | (36,1) 4×1 | (31,2) 1×4 | (32,2) 4×4 | (36,2) 1×4 | (37,2) 4×4 | 
| 05 Presa esquerda | [2.0,4.0] | [1.0,2.0,1.0] | (3,4) 1×1 | (4,4) 1×1 | (2,5) 1×2 | (3,5) 1×2 | (4,5) 1×2 | (5,5) 1×2 | 
| 06 Presa direita | [2.0,0.0] | [1.0,2.0,1.0] | (3,0) 1×1 | (4,0) 1×1 | (2,1) 1×2 | (3,1) 1×2 | (4,1) 1×2 | (5,1) 1×2 | 
| 07 Orelha esquerda | [51.0,6.0] | [1.0,5.0,4.0] | (55,6) 1×4 | (56,6) 1×4 | (51,10) 4×5 | (55,10) 1×5 | (56,10) 4×5 | (60,10) 1×5 | 
| 08 Orelha direita | [39.0,6.0] | [1.0,5.0,4.0] | (43,6) 1×4 | (44,6) 1×4 | (39,10) 4×5 | (43,10) 1×5 | (44,10) 4×5 | (48,10) 1×5 | 
| 09 Braço esquerdo | [32.0,48.0] | [4.0,12.0,4.0] | (36,48) 4×4 | (40,48) 4×4 | (32,52) 4×12 | (36,52) 4×12 | (40,52) 4×12 | (44,52) 4×12 | 
| 10 Manga externa esquerda | [48.0,48.0] | [4.0,12.0,4.0] | (52,48) 4×4 | (56,48) 4×4 | (48,52) 4×12 | (52,52) 4×12 | (56,52) 4×12 | (60,52) 4×12 | 
| 11 Perna esquerda | [16.0,48.0] | [4.0,12.0,4.0] | (20,48) 4×4 | (24,48) 4×4 | (16,52) 4×12 | (20,52) 4×12 | (24,52) 4×12 | (28,52) 4×12 | 
| 12 Calça externa esquerda | [0.0,48.0] | [4.0,12.0,4.0] | (4,48) 4×4 | (8,48) 4×4 | (0,52) 4×12 | (4,52) 4×12 | (8,52) 4×12 | (12,52) 4×12 | 
| 13 Braço direito | [40.0,16.0] | [4.0,12.0,4.0] | (44,16) 4×4 | (48,16) 4×4 | (40,20) 4×12 | (44,20) 4×12 | (48,20) 4×12 | (52,20) 4×12 | 
| 14 Manga externa direita | [40.0,32.0] | [4.0,12.0,4.0] | (44,32) 4×4 | (48,32) 4×4 | (40,36) 4×12 | (44,36) 4×12 | (48,36) 4×12 | (52,36) 4×12 | 
| 15 Perna direita | [0.0,16.0] | [4.0,12.0,4.0] | (4,16) 4×4 | (8,16) 4×4 | (0,20) 4×12 | (4,20) 4×12 | (8,20) 4×12 | (12,20) 4×12 | 
| 16 Calça externa direita | [0.0,32.0] | [4.0,12.0,4.0] | (4,32) 4×4 | (8,32) 4×4 | (0,36) 4×12 | (4,36) 4×12 | (8,36) 4×12 | (12,36) 4×12 | 

O focinho/nariz é **um único cubo 4×4×1**, não duas peças separadas; narinas são pintura nessa face. As duas presas têm geometria própria 1×2×1. Orelhas têm 1×5×4; não são planos. `head/hat` é removido pela definição do adulto e não possui cubos. `jacket`, `sleeve` e `pants` são os cinco cubos externos herdados da malha do jogador; não são bolsas ou armaduras separadas.

## Espelhamento e orientação

Todos os 16 cubos reportam `mirror=false`, inclusive os membros esquerdos. Portanto o lado esquerdo usa suas próprias ilhas na parte inferior do atlas; não reutiliza automaticamente o lado direito.

Eixos locais: X positivo = esquerda do personagem, Y positivo = baixo, Z negativo = frente. A tabela foi calculada comparando os vértices em UV crescente, e vale para todos os cubos (checado automaticamente).

| Face | Normal local | U crescente | V crescente |
|---|---|---|---|
| Topo | −Y | +X | -Z |
| Base | +Y | +X | -Z |
| Direita | −X | -Z | +Y |
| Frente | −Z | +X | +Y |
| Esquerda | +X | +Z | +Y |
| Costas | +Z | -X | +Y |

Topo e base não seguem a orientação de uma face frontal. Observe especialmente que V cresce para −Z nos dois, e a região inferior foi obtida dos vértices, não pela ordem de um desenho de cubo genérico. Para cada face, `uv_geometry.json` conserva a associação exata `[x,y,z,u,v]` dos quatro cantos.

As orelhas usam poses locais Z de −30° (esquerda) e +30° (direita), com pivôs X ±4.5, Y −6. Isso gira a geometria no mundo, não espelha sua ilha UV. Animações de orelha também preservam o UV.

## Diagnóstico da textura de produção

Backup: `goblin_original.png`. SHA-256: `0cca7563db9b13c629644a79e149fa5e3dbcdce7a3117e8929efbf8ae710fd35`.

Cobertura medida por face/cubo (pixel com alpha > 0):

| Peça | Pixels cobertos / área UV |
|---|---|
| Tronco | 352 / 352 |
| Casaco externo | 0 / 352 |
| Cabeça | 448 / 448 |
| Focinho / nariz | 48 / 48 |
| Presa esquerda | 10 / 10 |
| Presa direita | 10 / 10 |
| Orelha esquerda | 58 / 58 |
| Orelha direita | 58 / 58 |
| Braço esquerdo | 0 / 224 |
| Manga externa esquerda | 0 / 224 |
| Perna esquerda | 0 / 224 |
| Calça externa esquerda | 0 / 224 |
| Braço direito | 224 / 224 |
| Manga externa direita | 0 / 224 |
| Perna direita | 224 / 224 |
| Calça externa direita | 0 / 224 |

A dimensão 64×64 já está correta. O problema principal é o conteúdo: as ilhas inferiores dos membros esquerdos estão vazias. A camisa possui uma faixa diagonal de alto contraste que cruza regiões do tronco, enquanto a disposição de faixas e botas precisa obedecer à altura local de cada membro, não à posição global da linha no PNG. A ausência de pixels nos cubos-base pode tornar superfícies invisíveis no render type com recorte de alpha. Transparência nas camadas externas é intencional e válida; não deve ser confundida com falta de pintura no corpo-base.

## Candidata

`goblin_candidate.png`: 64×64 RGBA. Preserva os pixels opacos já pintados da cabeça, focinho, presas e orelhas; completa eventuais lacunas com pele. Tronco e membros recebem materiais próprios, produzidos com imagegen a partir da referência do usuário (`materials.png`, prompt em `material_prompt.md`) e montados deterministicamente nos retângulos extraídos, sem usar pixels vanilla.

Túnica marrom com gola central; cinto contínuo nas linhas locais 8–9 do tronco (V=28–29), fivela plana discreta. Braços: manga nas linhas 0–2, pele 3–5, faixas 6–8, mãos 9–11. Pernas: tecido 0–2, pele 3–4, faixas 5–7, botas 8–11. Topos/bases também são pintados. Membros esquerdos recebem o mesmo padrão em suas ilhas próprias. As cinco camadas externas ficam transparentes: o traje nesta candidata está pintado na superfície-base. Nenhuma bolsa ou volume extra é simulado.

`goblin_comparison.png` amplia as duas versões sem interpolação. `piglin_uv_template.png` é um mapa de cores técnico, não uma textura de personagem. `piglin_uv_guide.png` contém atlas ampliado, legenda e painel de cada cubo.

## Validação no Minecraft sem trocar a produção

1. Crie um resource pack local de teste em `run/resourcepacks/goblin-uv-test/`. Copie **apenas a candidata** para `assets/fantasyfolk/textures/entity/goblin.png` dentro dele. Use o formato de pack de recursos declarado em `version.json` do JAR 26.3, ou siga o exemplo de `pack.mcmeta` abaixo.
2. Execute `./gradlew runClient`, habilite esse pack em Opções → Pacotes de recursos e recarregue com F3+T. Isso sobrepõe o recurso durante o teste e não modifica `src/`.
3. Em um mundo de teste com comandos, use `/summon fantasyfolk:goblin ~ ~ ~ {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}`. Observe frente, costas, os dois lados e topo/base; confira continuidade do cinto, mãos, botas e as faixas dos dois lados.
4. Invoque também um Goblin sem `NoAI` para conferir articulações durante a caminhada. Orelhas têm animação própria, então faça também a inspeção em movimento.
5. Para conferir orientação, substitua a imagem **no pack de teste** pelo template colorido e recarregue; compare com os IDs do guia.
6. Desative o pack para retornar imediatamente à textura de produção. A aprovação visual dentro do jogo ainda é necessária; o build não verifica a aparência 3D.

### Pacote de teste: metadados e comandos

O `version.json` do JAR 26.3 declara `resource_major=97`, `resource_minor=1`. O codec local `PackFormat` aceita os limites abaixo (não confundir com formato de datapack 121). Conteúdo próprio de `pack.mcmeta.example`:

```json
{
  "pack": {
    "description": "Fantasy Folk - teste local da textura candidata do Goblin",
    "min_format": [97, 1],
    "max_format": [97, 1]
  }
}
```

Comandos opcionais para o usuário preparar o pack (não foram executados nesta tarefa; a candidata não foi ativada):

```bash
mkdir -p run/resourcepacks/goblin-uv-test/assets/fantasyfolk/textures/entity
cp assets-dev/goblin/pack.mcmeta.example run/resourcepacks/goblin-uv-test/pack.mcmeta
cp assets-dev/goblin/goblin_candidate.png run/resourcepacks/goblin-uv-test/assets/fantasyfolk/textures/entity/goblin.png
./gradlew runClient
```

### Regenerar os arquivos de desenvolvimento

Com Java 25 selecionado como no início deste documento, as ferramentas próprias podem ser executadas pelo classpath do Loom:

```bash
./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle exportPiglinUv
./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle prepareGoblinUv
```

O segundo comando usa a folha já salva `materials.png` e não chama serviços de geração de imagem. Ele recalcula template, guia, candidata, comparativo e este documento.

## Resultado da validação desta entrega

- `./gradlew build` executado com Java 25: **BUILD SUCCESSFUL**; tarefas de teste `NO-SOURCE`.
- 96 faces com coordenadas dentro de 64×64; nenhuma sobreposição de ilhas; orientação conferida pelos vértices reais.
- Candidata: todas as 11 peças-base opacas; 5 camadas externas transparentes.
- Backup e produção têm o mesmo SHA-256; renderer, modelo, entidade, atributos e registros não foram alterados.
- Inspeção do JAR: somente a textura de produção é distribuída; nenhum arquivo de `assets-dev/`, textura vanilla ou código vanilla foi incluído.
- A candidata e o guia foram inspecionados como imagens; não houve execução de Minecraft com a candidata nesta etapa. A validação visual em jogo fica para o teste com resource pack acima.

Referência de contexto: [Fabric — conceitos de renderização](https://docs.fabricmc.net/develop/rendering/basic-concepts). Os números UV deste documento vêm das classes locais 26.3, não dessa página.
