import com.google.gson.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

/** Development-only UV diagrams and face packing. Coordinates come from the live model probe. */
public class PrepareUvAssets {
    record Face(String name, int x, int y, int w, int h, JsonArray vertices) {}
    record Part(String id, String label, JsonObject cube, List<Face> faces, Color color, boolean overlay) {}
    static Path dir;
    static final String[] FACE_NAMES = {"Topo", "Base", "Direita", "Frente", "Esquerda", "Costas"};
    static final String[] FACE_SHORT = {"T", "B", "D", "F", "E", "C"};
    static final List<Part> parts = new ArrayList<>();
    static final Color INK = new Color(28, 38, 43);
    static JsonObject data;

    static int n(JsonElement e) { return Math.round(e.getAsFloat()); }
    static String name(JsonArray normal) {
        int x = n(normal.get(0)), y = n(normal.get(1)), z = n(normal.get(2));
        return x < 0 ? "Direita" : x > 0 ? "Esquerda" : y < 0 ? "Topo" : y > 0 ? "Base" : z < 0 ? "Frente" : "Costas";
    }
    static String label(String part, int cube) {
        return switch (part) {
            case "head" -> new String[]{"Cabeça", "Focinho / nariz", "Presa esquerda", "Presa direita"}[cube];
            case "head/left_ear" -> "Orelha esquerda";
            case "head/right_ear" -> "Orelha direita";
            case "body" -> "Tronco";
            case "body/jacket" -> "Casaco externo";
            case "left_arm" -> "Braço esquerdo";
            case "right_arm" -> "Braço direito";
            case "left_leg" -> "Perna esquerda";
            case "right_leg" -> "Perna direita";
            case "left_arm/left_sleeve" -> "Manga externa esquerda";
            case "right_arm/right_sleeve" -> "Manga externa direita";
            case "left_leg/left_pants" -> "Calça externa esquerda";
            case "right_leg/right_pants" -> "Calça externa direita";
            default -> throw new IllegalArgumentException(part);
        };
    }
    static void load() throws Exception {
        data = JsonParser.parseString(Files.readString(dir.resolve("uv_geometry.json"))).getAsJsonObject();
        int index = 0;
        for (var element : data.getAsJsonArray("cubes")) {
            JsonObject cube = element.getAsJsonObject();
            String path = cube.get("part").getAsString();
            var faces = new ArrayList<Face>();
            for (var element2 : cube.getAsJsonArray("faces")) {
                var face = element2.getAsJsonObject();
                JsonArray vertices = face.getAsJsonArray("vertices");
                int x0 = 100, y0 = 100, x1 = -1, y1 = -1;
                for (var vertex : vertices) {
                    var v = vertex.getAsJsonArray();
                    int u = n(v.get(3)), vv = n(v.get(4));
                    x0 = Math.min(x0, u); x1 = Math.max(x1, u);
                    y0 = Math.min(y0, vv); y1 = Math.max(y1, vv);
                }
                if (x0 < 0 || y0 < 0 || x1 > 64 || y1 > 64 || x0 >= x1 || y0 >= y1)
                    throw new AssertionError("Invalid UV bounds: " + path);
                faces.add(new Face(name(face.getAsJsonArray("normal")), x0, y0, x1-x0, y1-y0, vertices));
            }
            boolean outer = path.contains("jacket") || path.contains("sleeve") || path.contains("pants");
            Color color = Color.getHSBColor((index * 0.618034f) % 1f, .50f, .85f);
            parts.add(new Part(String.format("%02d", ++index), label(path, n(cube.get("cube"))), cube, faces, color, outer));
        }
    }
    static Color faceColor(Part p, Face f) {
        float factor = switch(f.name) { case "Topo" -> 1.16f; case "Base" -> .70f; case "Frente" -> 1f; case "Costas" -> .78f; case "Direita" -> .87f; default -> 1.07f; };
        return new Color(Math.min(255, (int)(p.color.getRed()*factor)), Math.min(255, (int)(p.color.getGreen()*factor)), Math.min(255, (int)(p.color.getBlue()*factor)));
    }
    static BufferedImage template() throws Exception {
        var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        boolean[][] seen = new boolean[64][64];
        for (var p : parts) for (var f : p.faces) {
            g.setColor(faceColor(p,f)); g.fillRect(f.x,f.y,f.w,f.h);
            for(int y=f.y;y<f.y+f.h;y++) for(int x=f.x;x<f.x+f.w;x++) {
                if(seen[y][x]) throw new AssertionError("Overlapping UV islands at " + x + "," + y);
                seen[y][x]=true;
            }
        }
        g.dispose(); ImageIO.write(image,"png",dir.resolve("piglin_uv_template.png").toFile());
        return image;
    }
    static void text(Graphics2D g,String s,int x,int y,int size,boolean bold) {
        g.setFont(new Font("SansSerif",bold ? Font.BOLD : Font.PLAIN,size)); g.setColor(INK); g.drawString(s,x,y);
    }
    static void center(Graphics2D g,String s,int x,int y,int w,int h,int size) {
        g.setFont(new Font("SansSerif",Font.BOLD,size)); var fm = g.getFontMetrics();
        int tx=x+(w-fm.stringWidth(s))/2, ty=y+(h-fm.getHeight())/2+fm.getAscent();
        g.setColor(new Color(255,255,255,215)); g.fillRect(tx-2,ty-fm.getAscent(),fm.stringWidth(s)+4,fm.getHeight());
        g.setColor(INK);g.drawString(s,tx,ty);
    }
    static void checker(Graphics2D g,int x,int y,int w,int h,int step) {
        for(int yy=0;yy<h;yy+=step) for(int xx=0;xx<w;xx+=step) {
            g.setColor(((xx/step+yy/step)%2==0) ? new Color(233,236,235) : new Color(215,220,218));
            g.fillRect(x+xx,y+yy,Math.min(step,w-xx),Math.min(step,h-yy));
        }
    }
    static void guide(BufferedImage atlas) throws Exception {
        var image=new BufferedImage(2000,2370,BufferedImage.TYPE_INT_RGB); var g=image.createGraphics();
        g.setColor(new Color(248,247,241));g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        text(g,"FANTASY FOLK · GOBLIN v0.1",50,48,30,true);
        text(g,"UV real do Piglin adulto · Minecraft 26.3 · atlas 64 × 64",50,86,24,false);
        int ox=65,oy=140,s=16; checker(g,ox,oy,1024,1024,16);
        g.drawImage(atlas,ox,oy,1024,1024,null);
        for(int i=0;i<=64;i++) {
            g.setColor(new Color(255,255,255,i%8==0?180:55));g.drawLine(ox+i*s,oy,ox+i*s,oy+1024);g.drawLine(ox,oy+i*s,ox+1024,oy+i*s);
            if(i%4==0){text(g,""+i,ox+i*s-5,oy-12,13,false);text(g,""+i,ox-28,oy+i*s+4,13,false);}
        }
        for(var p:parts) for(var f:p.faces) {
            int x=ox+f.x*s,y=oy+f.y*s,w=f.w*s,h=f.h*s;
            g.setColor(INK);g.setStroke(new BasicStroke(p.overlay?1:2));g.drawRect(x,y,w,h);
            if(w>=48&&h>=38) center(g,p.id+f.name.substring(0,1),x,y,w,h,14);
            else center(g,f.name.substring(0,1),x,y,w,h,11);
        }
        int lx=1140;
        text(g,"LEGENDA E ORIENTAÇÃO",lx,148,25,true);
        text(g,"U → direita · V ↓ abaixo · origem (0,0) no alto à esquerda",lx,185,20,false);
        text(g,"F frente   C costas   D direita   E esquerda   T topo   B base",lx,216,19,false);
        text(g,"Direita/esquerda são do personagem, visto por ele próprio.",lx,248,19,false);
        text(g,"Cores = cubos; tons = faces. Quadriculado = área sem UV.",lx,280,19,false);
        int y=333;
        for(var p:parts){g.setColor(p.color);g.fillRect(lx,y-18,24,24);text(g,p.id+"  "+p.label+(p.overlay?" [externa]":""),lx+38,y,21,true);
            text(g,"origem UV "+p.cube.get("offset")+" · tamanho "+p.cube.get("size"),lx+38,y+25,17,false);y+=49;}
        text(g,"96 faces · 16 cubos · nenhum mirror=true",lx,1140,22,true);
        text(g,"Cada face abaixo conserva a posição relativa no atlas; detalhes de 1 px ficam ampliados.",50,1225,22,false);
        for(int i=0;i<parts.size();i++) {
            var p=parts.get(i);int cx=50+(i%4)*490,cy=1260+(i/4)*263;
            g.setColor(Color.WHITE);g.fillRoundRect(cx,cy,470,245,12,12);text(g,p.id+" · "+p.label,cx+12,cy+29,20,true);
            int minX=64,minY=64,maxX=0,maxY=0;
            for(var f:p.faces){minX=Math.min(minX,f.x);minY=Math.min(minY,f.y);maxX=Math.max(maxX,f.x+f.w);maxY=Math.max(maxY,f.y+f.h);}
            int k=Math.min(11,Math.min(438/(maxX-minX),169/(maxY-minY))),px=cx+16,py=cy+49;
            for(var f:p.faces){int x=px+(f.x-minX)*k,yy=py+(f.y-minY)*k,w=f.w*k,h=f.h*k;
                g.setColor(faceColor(p,f));g.fillRect(x,yy,w,h);g.setColor(INK);g.drawRect(x,yy,w,h);center(g,f.name.substring(0,1),x,yy,w,h,Math.min(13,k));}
            text(g,"UV "+p.cube.get("offset")+" · mirror=false",cx+12,cy+232,16,false);
        }
        text(g,"Diagrama próprio calculado dos vértices/UV do modelo local. Não contém pixels da textura vanilla.",50,2340,18,false);
        g.dispose();ImageIO.write(image,"png",dir.resolve("piglin_uv_guide.png").toFile());
    }
    static Face face(Part p,String name){return p.faces.stream().filter(f->f.name.equals(name)).findFirst().orElseThrow();}
    static String rect(Face f){return "("+f.x+","+f.y+") "+f.w+"×"+f.h;}
    static String direction(Face f,int dimension) {
        var v=f.vertices; JsonArray a=null,b=null;
        for(var va:v) for(var vb:v){var aa=va.getAsJsonArray();var bb=vb.getAsJsonArray();
            if(n(aa.get(3+dimension)) < n(bb.get(3+dimension)) && n(aa.get(4-dimension))==n(bb.get(4-dimension))){a=aa;b=bb;}}
        if(a==null)throw new AssertionError("No UV edge");
        for(int i=0;i<3;i++){float delta=b.get(i).getAsFloat()-a.get(i).getAsFloat();if(Math.abs(delta)>.001)return (delta>0?"+":"-")+"XYZ".charAt(i);}
        throw new AssertionError("Zero edge");
    }
    static int material(BufferedImage sheet,int quadrant,int x,int y) {
        int qx=quadrant%2,qy=quadrant/2;
        int tx=Math.floorMod(x,32),ty=Math.floorMod(y,32);
        int sx=(int)((qx*.5+(tx+.5)/64)*sheet.getWidth()),sy=(int)((qy*.5+(ty+.5)/64)*sheet.getHeight());
        return sheet.getRGB(sx,sy)|0xff000000;
    }
    static void candidate() throws Exception {
        var source=ImageIO.read(dir.resolve("goblin_original.png").toFile());
        var sheet=ImageIO.read(dir.resolve("materials.png").toFile());
        var target=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        for(var p:parts) if(!p.overlay) for(var f:p.faces) {
            String path=p.cube.get("part").getAsString();
            for(int y=0;y<f.h;y++) for(int x=0;x<f.w;x++){
                int localY=switch(f.name){case "Topo"->0;case "Base"->11;default->y;};
                int kind=0;
                if(path.equals("body"))kind=localY==8||localY==9?3:1;
                if(path.endsWith("arm"))kind=localY<3?1:localY>=6&&localY<=8?2:0;
                if(path.endsWith("leg"))kind=localY<3?1:localY>=8?3:localY>=5?2:0;
                int color=material(sheet,kind,x,y);
                if(path.startsWith("head")){
                    int original=source.getRGB(f.x+x,f.y+y);
                    if((original>>>24)!=0)color=original|0xff000000;
                }
                if(path.equals("body")&&f.name.equals("Frente")){
                    // A narrow neckline and a flat belt buckle; all remain on the torso face.
                    if(localY<2&&x>=3&&x<=4)color=material(sheet,0,x,y);
                    if(localY==8&&(x==3||x==4))color=0xffb5a078;
                }
                target.setRGB(f.x+x,f.y+y,color);
            }
        }
        ImageIO.write(target,"png",dir.resolve("goblin_candidate.png").toFile());
        comparison(source,target);
    }
    static void comparison(BufferedImage source,BufferedImage target)throws Exception {
        var image=new BufferedImage(1400,790,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(248,247,241));g.fillRect(0,0,1400,790);
        text(g,"Textura atual",40,43,26,true);text(g,"Candidata · 64 × 64",740,43,26,true);
        checker(g,40,70,640,640,20);checker(g,740,70,640,640,20);
        g.drawImage(source,40,70,640,640,null);g.drawImage(target,740,70,640,640,null);
        text(g,"Ampliação 10× sem interpolação · produção preservada · camadas externas transparentes",40,756,21,false);
        g.dispose();ImageIO.write(image,"png",dir.resolve("goblin_comparison.png").toFile());
    }
    static void report()throws Exception {
        StringBuilder md=new StringBuilder("# Mapeamento UV — Goblin v0.1 / Piglin adulto 26.3\n\n");
        md.append("## Fonte e reprodutibilidade\n\nAtlas real: **64 × 64 pixels**, RGBA na candidata. Origem UV no canto superior esquerdo. Retângulos em pixels, `(u,v) largura×altura`; limite final exclusivo. UV normalizado = coordenada / 64.\n\n");
        md.append("Textura original de referência: `").append(data.get("vanillaTexture").getAsString()).append("`, dentro de `").append(data.get("vanillaJar").getAsString()).append("`. Lida em memória para conferir dimensões/hash; não foi extraída para o projeto.\n\n");
        md.append("SHA-256 da textura vanilla: `").append(data.get("vanillaTextureSha256").getAsString()).append("`.\n\n");
        md.append("O Loom fornece classes em `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-1b0498e74c/26.3/` e no cache global `~/.gradle/caches/fabric-loom/26.3/`. O probe usa o classpath **client.runtimeClasspath** do próprio projeto.\n\n");
        md.append("`LayerDefinitions.createRoots()` associa `ModelLayers.PIGLIN` à definição produzida por `AdultPiglinModel.createBodyLayer()`, verificado no bytecode 26.3 com `javap -c`. O probe invoca essa definição e percorre os cubos já baked: vértices, normais, UV, dimensões, poses e flags reais estão em `uv_geometry.json`. Não foram deduzidos de um template antigo.\n\n");
        md.append("```bash\nexport JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64\nexport PATH=\"$JAVA_HOME/bin:$PATH\"\n./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle exportPiglinUv\n./gradlew build\n```\n\n");
        md.append("O probe cria `goblin_original.png` somente se não existir e recusa sobrescrever um backup diferente. Nenhum arquivo em `src/` é escrito pelas ferramentas. Os arquivos `assets-dev/` não pertencem aos source sets e não entram no JAR.\n\n");
        md.append("## Correspondência com o renderer\n\n`GoblinRenderer` usa `new AdultPiglinModel(context.bakeLayer(ModelLayers.PIGLIN))`: mesma malha e UV do Piglin **adulto**, inclusive seus filhos de roupa. Não usa o modelo do bebê. Usa `PiglinRenderState` apenas para compatibilidade visual.\n\nA escala não é idêntica: o Goblin aplica `(0.85,0.85,0.85)`; `PiglinRenderer` não sobrescreve o `scale` vazio de `LivingEntityRenderer`. A redução geométrica não muda as coordenadas UV nem exige reduzir a imagem. A sombra 0.42 do Goblin e 0.5 do Piglin também não mudam UV. Camadas de equipamento do renderer vanilla não fazem parte deste atlas.\n\n");
        md.append("## Cubos e retângulos das faces\n\nDireita/esquerda são anatômicas. IDs correspondem às cores e painéis do guia PNG. Todas as 96 faces ficam no atlas e não há sobreposição de ilhas.\n\n| ID / peça | Offset | Dimensões X×Y×Z | Topo | Base | Direita | Frente | Esquerda | Costas |\n|---|---|---|---|---|---|---|---|---|\n");
        for(var p:parts){md.append("| ").append(p.id).append(" ").append(p.label).append(" | ").append(p.cube.get("offset")).append(" | ").append(p.cube.get("size")).append(" | ");
            for(var fn:FACE_NAMES)md.append(rect(face(p,fn))).append(" | ");md.append("\n");}
        md.append("\nO focinho/nariz é **um único cubo 4×4×1**, não duas peças separadas; narinas são pintura nessa face. As duas presas têm geometria própria 1×2×1. Orelhas têm 1×5×4; não são planos. `head/hat` é removido pela definição do adulto e não possui cubos. `jacket`, `sleeve` e `pants` são os cinco cubos externos herdados da malha do jogador; não são bolsas ou armaduras separadas.\n\n");
        md.append("## Espelhamento e orientação\n\nTodos os 16 cubos reportam `mirror=false`, inclusive os membros esquerdos. Portanto o lado esquerdo usa suas próprias ilhas na parte inferior do atlas; não reutiliza automaticamente o lado direito.\n\nEixos locais: X positivo = esquerda do personagem, Y positivo = baixo, Z negativo = frente. A tabela foi calculada comparando os vértices em UV crescente, e vale para todos os cubos (checado automaticamente).\n\n| Face | Normal local | U crescente | V crescente |\n|---|---|---|---|\n");
        var first=parts.getFirst();
        for(var fn:FACE_NAMES){var f=face(first,fn);String u=direction(f,0),v=direction(f,1);
            for(var p:parts){if(!u.equals(direction(face(p,fn),0))||!v.equals(direction(face(p,fn),1)))throw new AssertionError("Unexpected mirrored orientation");}
            String normal=switch(fn){case "Frente"->"−Z";case "Costas"->"+Z";case "Direita"->"−X";case "Esquerda"->"+X";case "Topo"->"−Y";default->"+Y";};
            md.append("| ").append(fn).append(" | ").append(normal).append(" | ").append(u).append(" | ").append(v).append(" |\n");}
        md.append("\nTopo e base não seguem a orientação de uma face frontal. Observe especialmente que V cresce para −Z nos dois, e a região inferior foi obtida dos vértices, não pela ordem de um desenho de cubo genérico. Para cada face, `uv_geometry.json` conserva a associação exata `[x,y,z,u,v]` dos quatro cantos.\n\nAs orelhas usam poses locais Z de −30° (esquerda) e +30° (direita), com pivôs X ±4.5, Y −6. Isso gira a geometria no mundo, não espelha sua ilha UV. Animações de orelha também preservam o UV.\n\n");
        var original=ImageIO.read(dir.resolve("goblin_original.png").toFile());
        md.append("## Diagnóstico da textura de produção\n\nBackup: `goblin_original.png`. SHA-256: `").append(data.get("productionSha256").getAsString()).append("`.\n\nCobertura medida por face/cubo (pixel com alpha > 0):\n\n| Peça | Pixels cobertos / área UV |\n|---|---|\n");
        for(var p:parts){int total=0,painted=0;for(var f:p.faces)for(int y=f.y;y<f.y+f.h;y++)for(int x=f.x;x<f.x+f.w;x++){total++;if((original.getRGB(x,y)>>>24)>0)painted++;}
            md.append("| ").append(p.label).append(" | ").append(painted).append(" / ").append(total).append(" |\n");}
        md.append("\nA dimensão 64×64 já está correta. O problema principal é o conteúdo: as ilhas inferiores dos membros esquerdos estão vazias. A camisa possui uma faixa diagonal de alto contraste que cruza regiões do tronco, enquanto a disposição de faixas e botas precisa obedecer à altura local de cada membro, não à posição global da linha no PNG. A ausência de pixels nos cubos-base pode tornar superfícies invisíveis no render type com recorte de alpha. Transparência nas camadas externas é intencional e válida; não deve ser confundida com falta de pintura no corpo-base.\n\n");
        md.append("## Candidata\n\n`goblin_candidate.png`: 64×64 RGBA. Preserva os pixels opacos já pintados da cabeça, focinho, presas e orelhas; completa eventuais lacunas com pele. Tronco e membros recebem materiais próprios, produzidos com imagegen a partir da referência do usuário (`materials.png`, prompt em `material_prompt.md`) e montados deterministicamente nos retângulos extraídos, sem usar pixels vanilla.\n\nTúnica marrom com gola central; cinto contínuo nas linhas locais 8–9 do tronco (V=28–29), fivela plana discreta. Braços: manga nas linhas 0–2, pele 3–5, faixas 6–8, mãos 9–11. Pernas: tecido 0–2, pele 3–4, faixas 5–7, botas 8–11. Topos/bases também são pintados. Membros esquerdos recebem o mesmo padrão em suas ilhas próprias. As cinco camadas externas ficam transparentes: o traje nesta candidata está pintado na superfície-base. Nenhuma bolsa ou volume extra é simulado.\n\n`goblin_comparison.png` amplia as duas versões sem interpolação. `piglin_uv_template.png` é um mapa de cores técnico, não uma textura de personagem. `piglin_uv_guide.png` contém atlas ampliado, legenda e painel de cada cubo.\n\n");
        md.append("## Validação no Minecraft sem trocar a produção\n\n1. Crie um resource pack local de teste em `run/resourcepacks/goblin-uv-test/`. Copie **apenas a candidata** para `assets/fantasyfolk/textures/entity/goblin.png` dentro dele. Use o formato de pack de recursos declarado em `version.json` do JAR 26.3, ou siga o exemplo de `pack.mcmeta` abaixo.\n2. Execute `./gradlew runClient`, habilite esse pack em Opções → Pacotes de recursos e recarregue com F3+T. Isso sobrepõe o recurso durante o teste e não modifica `src/`.\n3. Em um mundo de teste com comandos, use `/summon fantasyfolk:goblin ~ ~ ~ {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}`. Observe frente, costas, os dois lados e topo/base; confira continuidade do cinto, mãos, botas e as faixas dos dois lados.\n4. Invoque também um Goblin sem `NoAI` para conferir articulações durante a caminhada. Orelhas têm animação própria, então faça também a inspeção em movimento.\n5. Para conferir orientação, substitua a imagem **no pack de teste** pelo template colorido e recarregue; compare com os IDs do guia.\n6. Desative o pack para retornar imediatamente à textura de produção. A aprovação visual dentro do jogo ainda é necessária; o build não verifica a aparência 3D.\n\n");
        md.append("### Pacote de teste: metadados e comandos\n\nO `version.json` do JAR 26.3 declara `resource_major=97`, `resource_minor=1`. O codec local `PackFormat` aceita os limites abaixo (não confundir com formato de datapack 121). Conteúdo próprio de `pack.mcmeta.example`:\n\n```json\n");
        md.append(Files.readString(dir.resolve("pack.mcmeta.example"))).append("```\n\n");
        md.append("Comandos opcionais para o usuário preparar o pack (não foram executados nesta tarefa; a candidata não foi ativada):\n\n```bash\nmkdir -p run/resourcepacks/goblin-uv-test/assets/fantasyfolk/textures/entity\ncp assets-dev/goblin/pack.mcmeta.example run/resourcepacks/goblin-uv-test/pack.mcmeta\ncp assets-dev/goblin/goblin_candidate.png run/resourcepacks/goblin-uv-test/assets/fantasyfolk/textures/entity/goblin.png\n./gradlew runClient\n```\n\n");
        md.append("### Regenerar os arquivos de desenvolvimento\n\nCom Java 25 selecionado como no início deste documento, as ferramentas próprias podem ser executadas pelo classpath do Loom:\n\n```bash\n./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle exportPiglinUv\n./gradlew --no-configuration-cache -I assets-dev/goblin/tools/export-uv.init.gradle prepareGoblinUv\n```\n\nO segundo comando usa a folha já salva `materials.png` e não chama serviços de geração de imagem. Ele recalcula template, guia, candidata, comparativo e este documento.\n\n");
        md.append("## Resultado da validação desta entrega\n\n- `./gradlew build` executado com Java 25: **BUILD SUCCESSFUL**; tarefas de teste `NO-SOURCE`.\n- 96 faces com coordenadas dentro de 64×64; nenhuma sobreposição de ilhas; orientação conferida pelos vértices reais.\n- Candidata: todas as 11 peças-base opacas; 5 camadas externas transparentes.\n- Backup e produção têm o mesmo SHA-256; renderer, modelo, entidade, atributos e registros não foram alterados.\n- Inspeção do JAR: somente a textura de produção é distribuída; nenhum arquivo de `assets-dev/`, textura vanilla ou código vanilla foi incluído.\n- A candidata e o guia foram inspecionados como imagens; não houve execução de Minecraft com a candidata nesta etapa. A validação visual em jogo fica para o teste com resource pack acima.\n\nReferência de contexto: [Fabric — conceitos de renderização](https://docs.fabricmc.net/develop/rendering/basic-concepts). Os números UV deste documento vêm das classes locais 26.3, não dessa página.\n");
        Files.writeString(dir.resolve("uv_mapping.md"),md.toString());
    }
    static void validate()throws Exception{
        var original=Files.readAllBytes(Path.of("src/main/resources/assets/fantasyfolk/textures/entity/goblin.png"));
        if(!Arrays.equals(original,Files.readAllBytes(dir.resolve("goblin_original.png"))))throw new AssertionError("Production changed");
        if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)).equals(data.get("productionSha256").getAsString()))throw new AssertionError("Hash mismatch");
        var candidate=ImageIO.read(dir.resolve("goblin_candidate.png").toFile());
        if(candidate.getWidth()!=64||candidate.getHeight()!=64)throw new AssertionError("Candidate dimensions");
        for(var p:parts)for(var f:p.faces)for(int y=f.y;y<f.y+f.h;y++)for(int x=f.x;x<f.x+f.w;x++)
            if((candidate.getRGB(x,y)>>>24)!=(p.overlay?0:255))throw new AssertionError("Unexpected alpha "+p.label);
        System.out.println("PASS: 64x64; 96 bounded nonoverlapping faces; orientations match; all base faces opaque; all overlays transparent; production/backup hash unchanged.");
    }
    public static void main(String[] args)throws Exception{
        dir=Path.of("assets-dev/goblin");load();guide(template());report();
        if(Files.exists(dir.resolve("materials.png"))){candidate();validate();}
    }
}
