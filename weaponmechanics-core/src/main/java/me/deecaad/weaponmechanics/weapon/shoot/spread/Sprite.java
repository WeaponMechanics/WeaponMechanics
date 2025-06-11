package me.deecaad.weaponmechanics.weapon.shoot.spread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This class parses an image input (Usually a .png file) and saves each pixel in the image into a
 * buffer. Each pixel can then be accessed, assuming you have an x and y coordinate to use.
 */
public class Sprite {

    private final int[][] pixels;
    private final int height, width;

    public Sprite(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);

        height = img.getHeight();
        width = img.getWidth();

        // Get the image raster (an int[] where each int represents 1 pixel)
        // then split the int[] into a int[][] that holds the pixel value of a
        // given set of (y, x) coordinates.
        int[] array = img.getRGB(0, 0, width, height, null, 0, width);
        pixels = new int[array.length / width][width];
        for (int y = 0; y < pixels.length; y++) {
            System.arraycopy(array, y * width, pixels[y], 0, pixels[y].length);
        }
    }

    public int[][] getPixels() {
        return pixels;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}
