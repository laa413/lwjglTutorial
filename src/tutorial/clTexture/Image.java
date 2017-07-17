package tutorial.clTexture;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import javax.imageio.ImageIO;

/*
 * @author LAA
 */
public class Image {
    private final int width, height;
    private BufferedImage img;

    /* The color model including alpha for the GL image */
    private ColorModel glAlphaColorModel;
    /* The color model for the GL image */
    private ColorModel glColorModel;

    public Image(String imgFile) {
        this.img = getBuffImage(imgFile);
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[]{8, 8, 8, 8},
                true,
                false,
                ComponentColorModel.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);

        this.glColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[]{8, 8, 8, 0},
                false,
                false,
                ComponentColorModel.OPAQUE,
                DataBuffer.TYPE_BYTE);
    }

    private BufferedImage getBuffImage(String imageFile) {
        try {
            BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ByteBuffer getByeBuff() {
        ByteBuffer byteBuffer;
        WritableRaster raster;
        BufferedImage texImage;

        // create a raster that can be used by OpenGL as a source for a texture
        if (this.img.getColorModel().hasAlpha()) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, this.img.getWidth() + 1, this.img.getHeight(), 4, null);
            texImage = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, this.img.getWidth() + 1, this.img.getHeight(), 3, null);
            texImage = new BufferedImage(glColorModel, raster, false, new Hashtable());
        }

        // copy the source image into the produced image
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0f, 0f, 0f, 0f));
        g.fillRect(0, 0, this.img.getWidth() + 1, this.img.getHeight());
        g.drawImage(this.img, 0, 0, null);

        // build a byte buffer from the temporary image to produce a texture
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

        byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(data, 0, data.length);
        byteBuffer.flip();

        return byteBuffer;
    }
    
    public int getWidth(){
        return width;
    }
    
    public int getHeight(){
        return height;
    }
    
    public BufferedImage getImg(){
        return img;
    }
}
