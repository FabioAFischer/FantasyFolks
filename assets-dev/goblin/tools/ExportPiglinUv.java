import com.google.gson.GsonBuilder;
import net.minecraft.client.model.monster.piglin.AdultPiglinModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import org.joml.Vector3fc;
import javax.imageio.ImageIO;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Reads the actual 26.3 model through its API; contains no copied model definitions. */
public class ExportPiglinUv {
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
    static List<Float> vector(Vector3fc v) { return List.of(v.x(), v.y(), v.z()); }
    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static void walk(String path, PartDefinition definition, ModelPart part,
                     List<Object> output, int width, int height) throws Exception {
        List<?> definitions = (List<?>) field(definition, "cubes");
        List<?> cubes = (List<?>) field(part, "cubes");
        for (int i = 0; i < cubes.size(); i++) {
            Object def = definitions.get(i);
            ModelPart.Cube cube = (ModelPart.Cube) cubes.get(i);
            var row = new LinkedHashMap<String, Object>();
            row.put("part", path); row.put("cube", i);
            row.put("origin", vector((Vector3fc) field(def, "origin")));
            row.put("size", vector((Vector3fc) field(def, "dimensions")));
            row.put("mirror", field(def, "mirror"));
            UVPair offset = (UVPair) field(def, "texCoord");
            row.put("offset", List.of(offset.u(), offset.v()));
            row.put("pose", List.of(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot));
            var faces = new ArrayList<Object>();
            for (var polygon : cube.polygons) {
                var face = new LinkedHashMap<String, Object>();
                face.put("normal", vector(polygon.normal()));
                var vertices = new ArrayList<Object>();
                for (var vertex : polygon.vertices()) {
                    vertices.add(List.of(vertex.x(), vertex.y(), vertex.z(), vertex.u() * width, vertex.v() * height));
                }
                face.put("vertices", vertices); faces.add(face);
            }
            row.put("faces", faces); output.add(row);
        }
        var children = new TreeMap<String, PartDefinition>();
        for (var entry : definition.getChildren()) children.put(entry.getKey(), entry.getValue());
        for (var entry : children.entrySet())
            walk(path.isEmpty() ? entry.getKey() : path + "/" + entry.getKey(), entry.getValue(),
                    part.getChild(entry.getKey()), output, width, height);
    }
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        var layer = AdultPiglinModel.createBodyLayer();
        var material = field(layer, "material");
        int width = (int) field(material, "xTexSize"), height = (int) field(material, "yTexSize");
        var mesh = (MeshDefinition) field(layer, "mesh");
        var cubes = new ArrayList<Object>();
        walk("", mesh.getRoot(), layer.bakeRoot(), cubes, width, height);
        var data = new LinkedHashMap<String, Object>();
        data.put("minecraft", "26.3"); data.put("atlas", List.of(width, height));
        data.put("model", "AdultPiglinModel.createBodyLayer / ModelLayers.PIGLIN");
        data.put("rendererScale", 0.85);
        String texture = "assets/minecraft/textures/entity/piglin/piglin.png";
        try (var jar = new ZipFile(args[1])) {
            byte[] bytes = jar.getInputStream(jar.getEntry(texture)).readAllBytes();
            var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            data.put("vanillaTexture", texture); data.put("vanillaJar", args[1]);
            data.put("vanillaTextureSize", List.of(image.getWidth(), image.getHeight()));
            data.put("vanillaTextureSha256", sha(bytes));
            if (image.getWidth() != width || image.getHeight() != height) throw new AssertionError("Atlas mismatch");
        }
        var production = Path.of("src/main/resources/assets/fantasyfolk/textures/entity/goblin.png");
        byte[] original = Files.readAllBytes(production);
        Path backup = out.resolve("goblin_original.png");
        if (!Files.exists(backup)) Files.copy(production, backup);
        if (!Arrays.equals(original, Files.readAllBytes(backup))) throw new AssertionError("Existing backup differs");
        data.put("productionSha256", sha(original)); data.put("cubes", cubes);
        Files.writeString(out.resolve("uv_geometry.json"), new GsonBuilder().setPrettyPrinting().create().toJson(data) + "\n");
        System.out.println("Exported " + cubes.size() + " cubes, atlas " + width + "x" + height + "; production backup verified.");
    }
}
