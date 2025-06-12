package me.deecaad.weaponmechanics.weapon.shoot.spread;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.ProbabilityMap;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SpreadImage implements Serializer<SpreadImage> {

    private ProbabilityMap<Point> points;
    private int height, width;

    /**
     * Default constructor for serializer
     */
    public SpreadImage() {
    }

    public SpreadImage(Sprite sprite, double fovWidth, double fovHeight) {
        this.points = new ProbabilityMap<>();
        this.width = sprite.getWidth();
        this.height = sprite.getHeight();

        double maxYaw = Math.toRadians(fovWidth);
        double maxPitch = Math.toRadians(fovHeight);

        double xMiddle = width / 2.0;
        double yMiddle = height / 2.0;

        int[][] pixels = sprite.getPixels();
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {

                // First, we need to strip away color/alpha data. We only want
                // a number [0, 255] (which represents a grayscale shade).
                int r = (pixels[y][x] >> 16) & 0xFF;
                int g = (pixels[y][x] >> 8) & 0xFF;
                int b = (pixels[y][x]) & 0xFF;

                if (r != g || r != b) {
                    throw new IllegalArgumentException("Found an image that used colors instead of a shade " +
                        "of gray at pixel x:" + x + " y:" + y);
                }

                int grayScale = 255 - r;
                if (grayScale != 0) {
                    double yaw = (x - xMiddle) / sprite.getWidth() * maxYaw;
                    double pitch = (y - yMiddle) / sprite.getHeight() * maxPitch;

                    Point point = new Point(yaw, pitch);
                    double chance = grayScale / 255.0;
                    points.add(point, chance);
                }
            }
        }
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    /**
     * Gets a given number of random points (x, y) from <code>this.points</code> using the value of that
     * point as the chance for that point to be selected.
     * <p>
     * If not enough points are found in the allowed number of checks, then random points are selected,
     * not taking the point's value into account
     *
     * @param amount Number of points to get
     * @return The found points
     */
    public Set<Point> getLocations(int amount) {
        Set<Point> points = new HashSet<>();

        while (points.size() < amount) {
            points.add(this.points.get());
        }

        return points;
    }

    public Point getLocation() {
        return points.get();
    }

    @Override
    public String getKeyword() {
        return "Spread_Image";
    }

    @Override
    @NotNull public SpreadImage serialize(@NotNull SerializeData data) throws SerializerException {
        String imageName = data.of("Name").assertExists().get(String.class).get();

        // 9/10, people will be using a png. If no file extension is provided,
        // we will default to png.
        if (!imageName.contains("\\."))
            imageName += ".png";

        double FOVWidth = data.of("Field_Of_View_Width").assertExists().assertRange(0.0, 360.0).getDouble().getAsDouble();
        double FOVHeight = data.of("Field_Of_View_Height").assertExists().assertRange(0.0, 360.0).getDouble().getAsDouble();

        File dataFolder = WeaponMechanics.getInstance().getDataFolder();
        File spritesFolder = new File(dataFolder, "spread_patterns");
        File spriteFile = new File(spritesFolder, imageName);

        if (!spritesFolder.exists()) {
            spritesFolder.mkdirs();
            WeaponMechanics.getInstance().debugger.warning("The spread image folder seems to be missing",
                "Perhaps you accidentally deleted it? Or perhaps the plugin failed on first load?",
                "We'll add one back for you, make sure you drag and drop your images into it.",
                "You are going to see an error on the next line. This is because the folder is empty.",
                ""); // + empty line for spacing
        }

        if (!spriteFile.exists()) {
            throw SerializerException.builder()
                .locationRaw(data.of("Name").getLocation())
                .buildInvalidOption(imageName, Arrays.asList(spritesFolder.list()));
        }

        Sprite sprite;
        try {
            sprite = new Sprite(spriteFile);
        } catch (IOException ex) {
            throw data.exception("Name", "There was an error while reading the file '" + spriteFile + "'",
                "Are you sure it is an image? Was it corrupted? Can you open the image normally?",
                ex.getClass() + ": " + ex.getMessage());
        }

        SpreadImage image = new SpreadImage(sprite, FOVWidth, FOVHeight);
        if (image.points.isEmpty()) {
            throw data.exception("Name", "The spread image '" + imageName + "' is either blank, or colored",
                "Make sure '" + imageName + "' is black and white");
        }

        return image;
    }
}
