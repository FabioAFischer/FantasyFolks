import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Deterministically remaps the working v0.1 texture to the compact v0.2 UV islands. */
public final class PrepareGoblinV02Texture {
    record Rect(int x, int y, int width, int height) {}
    record Cube(int u, int v, int width, int height, int depth) {}
    record Mapping(String name, Cube source, Cube target) {}

    private static List<Rect> faces(Cube cube) {
        int u = cube.u();
        int v = cube.v();
        int width = cube.width();
        int height = cube.height();
        int depth = cube.depth();
        return List.of(
                new Rect(u + depth, v, width, depth),
                new Rect(u + depth + width, v, width, depth),
                new Rect(u, v + depth, depth, height),
                new Rect(u + depth, v + depth, width, height),
                new Rect(u + depth + width, v + depth, depth, height),
                new Rect(u + depth + width + depth, v + depth, width, height)
        );
    }

    private static void copyScaled(BufferedImage source, BufferedImage target, Rect from, Rect to) {
        for (int y = 0; y < to.height(); y++) {
            int sourceY = from.y() + Math.min(from.height() - 1, y * from.height() / to.height());
            for (int x = 0; x < to.width(); x++) {
                int sourceX = from.x() + Math.min(from.width() - 1, x * from.width() / to.width());
                target.setRGB(to.x() + x, to.y() + y, source.getRGB(sourceX, sourceY));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PrepareGoblinV02Texture <source.png> <target.png>");
        }

        Path sourcePath = Path.of(args[0]);
        Path targetPath = Path.of(args[1]);
        BufferedImage source = ImageIO.read(sourcePath.toFile());
        if (source == null || source.getWidth() != 64 || source.getHeight() != 64) {
            throw new IllegalArgumentException("Expected a 64x64 PNG: " + sourcePath);
        }

        List<Mapping> mappings = new ArrayList<>();
        mappings.add(new Mapping("head", new Cube(0, 0, 10, 8, 8), new Cube(0, 0, 10, 8, 8)));
        mappings.add(new Mapping("nose", new Cube(31, 1, 4, 4, 1), new Cube(31, 1, 4, 4, 1)));
        mappings.add(new Mapping("left_tusk", new Cube(2, 4, 1, 2, 1), new Cube(2, 4, 1, 2, 1)));
        mappings.add(new Mapping("right_tusk", new Cube(2, 0, 1, 2, 1), new Cube(2, 0, 1, 2, 1)));
        mappings.add(new Mapping("left_ear", new Cube(51, 6, 1, 5, 4), new Cube(51, 6, 1, 5, 4)));
        mappings.add(new Mapping("right_ear", new Cube(39, 6, 1, 5, 4), new Cube(39, 6, 1, 5, 4)));
        mappings.add(new Mapping("body", new Cube(16, 16, 8, 12, 4), new Cube(16, 32, 8, 10, 5)));
        mappings.add(new Mapping("right_arm", new Cube(40, 16, 4, 12, 4), new Cube(40, 16, 3, 10, 3)));
        mappings.add(new Mapping("left_arm", new Cube(32, 48, 4, 12, 4), new Cube(32, 48, 3, 10, 3)));
        mappings.add(new Mapping("right_leg", new Cube(0, 16, 4, 12, 4), new Cube(0, 16, 4, 8, 4)));
        mappings.add(new Mapping("left_leg", new Cube(16, 48, 4, 12, 4), new Cube(16, 48, 4, 8, 4)));

        BufferedImage target = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        boolean[][] occupied = new boolean[64][64];

        for (Mapping mapping : mappings) {
            List<Rect> sourceFaces = faces(mapping.source());
            List<Rect> targetFaces = faces(mapping.target());
            for (int face = 0; face < sourceFaces.size(); face++) {
                Rect destination = targetFaces.get(face);
                if (destination.x() < 0 || destination.y() < 0
                        || destination.x() + destination.width() > 64
                        || destination.y() + destination.height() > 64) {
                    throw new AssertionError("Out-of-bounds UV for " + mapping.name());
                }
                for (int y = destination.y(); y < destination.y() + destination.height(); y++) {
                    for (int x = destination.x(); x < destination.x() + destination.width(); x++) {
                        if (occupied[y][x]) {
                            throw new AssertionError("Overlapping UV at " + x + "," + y);
                        }
                        occupied[y][x] = true;
                    }
                }
                copyScaled(source, target, sourceFaces.get(face), destination);
            }
        }

        for (Mapping mapping : mappings) {
            for (Rect face : faces(mapping.target())) {
                for (int y = face.y(); y < face.y() + face.height(); y++) {
                    for (int x = face.x(); x < face.x() + face.width(); x++) {
                        if ((target.getRGB(x, y) >>> 24) == 0) {
                            throw new AssertionError("Transparent model pixel for " + mapping.name()
                                    + " at " + x + "," + y);
                        }
                    }
                }
            }
        }

        Files.createDirectories(targetPath.toAbsolutePath().getParent());
        ImageIO.write(target, "png", targetPath.toFile());
        System.out.println("PASS: remapped " + mappings.size()
                + " base cubes to a non-overlapping 64x64 v0.2 atlas: " + targetPath);
    }
}
