/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.texture;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.color.ColorSpace;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Hashtable;

import org.lwjgl.BufferUtils;

/*
 * @author labramson
 */
public class Image {

    private final int width;
    private final int height;
    private BufferedImage image;
    private ByteBuffer buffer;

    public Image(String imgFile) {
        System.out.println("Made image");
        BufferedImage img = this.getImageFile(imgFile);
        ByteBuffer buff = this.createByteBuff(img);
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.image = img;
        this.buffer = buff;
    }

    public BufferedImage getImageFile(String imageFile) {
        System.out.println("read image file");
        try {
            BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuffer createByteBuff(BufferedImage img) {
        System.out.println("Made image buffer");
        try {
            int pixels[] = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

            ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int pixel = pixels[y * img.getWidth() + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                }
            }

            buffer.flip();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    public BufferedImage getImg() {
        return image;
    }
}
