# Mapeamento UV — Goblin v0.2 compacto

O modelo v0.2 mantém o atlas RGBA de 64 × 64 e a hierarquia esperada por
`AdultPiglinModel`, mas define todos os cubos visíveis em `GoblinModel`.
`ModelLayers.PIGLIN` não é modificado.

## Proporções

| Parte | v0.1: dimensões / pivô | v0.2: dimensões / pivô | Offset UV v0.2 |
|---|---|---|---|
| Cabeça | 10×8×8 / (0,0,0) | 10×8×8 / (0,6,0) | (0,0) |
| Tronco | 8×12×4 / (0,0,0) | 8×10×5 / (0,6,0) | (16,32) |
| Braço direito | 4×12×4 / (-5,2,0) | 3×10×3 / (-5,8,0) | (40,16) |
| Braço esquerdo | 4×12×4 / (5,2,0) | 3×10×3 / (5,8,0) | (32,48) |
| Perna direita | 4×12×4 / (-1.9,12,0) | 4×8×4 / (-1.9,16,0) | (0,16) |
| Perna esquerda | 4×12×4 / (1.9,12,0) | 4×8×4 / (1.9,16,0) | (16,48) |

A cabeça agora termina em Y=6, o tronco ocupa Y=6..16 e as pernas ocupam
Y=16..24. Os pés continuam no plano vanilla Y=24. A extensão visual vai de
Y=-2 a Y=24: 26 pixels; com a escala do renderer em 0,85, corresponde a
aproximadamente 1,38 bloco.

O nariz mantém 4×4×1 e UV (31,1), recuado de Z=-5 para Z=-4.6. As presas
mantêm 1×2×1 e seus offsets, recuadas para Z=-4.75. As orelhas mantêm 1×5×4,
com inclinação-base alterada de 30° para 40°.

## Faces alteradas

| Parte | Topo | Base | Direita | Frente | Esquerda | Costas |
|---|---|---|---|---|---|---|
| Tronco | (21,32) 8×5 | (29,32) 8×5 | (16,37) 5×10 | (21,37) 8×10 | (29,37) 5×10 | (34,37) 8×10 |
| Braço direito | (43,16) 3×3 | (46,16) 3×3 | (40,19) 3×10 | (43,19) 3×10 | (46,19) 3×10 | (49,19) 3×10 |
| Braço esquerdo | (35,48) 3×3 | (38,48) 3×3 | (32,51) 3×10 | (35,51) 3×10 | (38,51) 3×10 | (41,51) 3×10 |
| Perna direita | (4,16) 4×4 | (8,16) 4×4 | (0,20) 4×8 | (4,20) 4×8 | (8,20) 4×8 | (12,20) 4×8 |
| Perna esquerda | (20,48) 4×4 | (24,48) 4×4 | (16,52) 4×8 | (20,52) 4×8 | (24,52) 4×8 | (28,52) 4×8 |

As camadas externas de casaco, mangas e calças não têm cubos na v0.2. As
roupas já estão pintadas nas peças-base, portanto isso libera a antiga ilha do
casaco para o tronco mais profundo e evita sobreposição UV.

## Texturas

- Referência funcional preservada: `src/main/resources/assets/fantasyfolk/textures/entity/goblin-old.png`.
- Candidata de desenvolvimento: `assets-dev/goblin/goblin_v02_candidate.png`.
- Recurso ativo para teste v0.2: `src/main/resources/assets/fantasyfolk/textures/entity/goblin.png`.

Regeneração determinística, sem interpolação suave:

```bash
java assets-dev/goblin/tools/PrepareGoblinV02Texture.java \
  src/main/resources/assets/fantasyfolk/textures/entity/goblin-old.png \
  assets-dev/goblin/goblin_v02_candidate.png
```
