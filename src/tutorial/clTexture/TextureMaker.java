/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

//Java imports
import java.awt.Graphics;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

//GL imports
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;

//CL imports
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.Util;

/*
 * @author labramson
 */
public class TextureMaker {

    /* The table of textures that have been loaded in this loader */
    private HashMap table = new HashMap();
    /* The color model including alpha for the GL image */
    private ColorModel glAlphaColorModel;
    /* The color model for the GL image */
    private ColorModel glColorModel;

    private Texture texture;
    private BufferedImage image;
    private ByteBuffer textureBuff;

    public TextureMaker(BufferedImage image, Texture texture) {
        this.image = image;
        this.texture = texture;
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

    public Texture makeTexture() {
        int textureID = this.texture.textureID;
        int textureTarget = this.texture.target;
        Texture texture = new Texture(textureTarget, textureID);
        int srcPixelFormat = 0;
        // bind this texture 
        GL11.glBindTexture(textureTarget, textureID);

        BufferedImage bufferedImage = this.image;
        texture.setWidth(bufferedImage.getWidth());
        texture.setHeight(bufferedImage.getHeight());

        if (bufferedImage.getColorModel().hasAlpha()) {
            srcPixelFormat = GL11.GL_RGBA;
        } else {
            srcPixelFormat = GL11.GL_RGB;
        }

        ByteBuffer texBuff = imgToTexBuff(bufferedImage, texture);
        textureBuff = texBuff;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, bufferedImage.getWidth(), bufferedImage.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, texBuff);

        return texture;
    }

    private ByteBuffer imgToTexBuff(BufferedImage bufferedImage, Texture texture) {
        ByteBuffer imageBuffer = null;
        WritableRaster raster;
        BufferedImage texImage;

        int texWidth = 2;
        int texHeight = 2;

        // find the closest power of 2 for the width and height of the produced texture
        while (texWidth < bufferedImage.getWidth()) {
            texWidth *= 2;
        }
        while (texHeight < bufferedImage.getHeight()) {
            texHeight *= 2;
        }

        texture.setTextureHeight(texHeight);
        texture.setTextureWidth(texWidth);

        // create a raster that can be used by OpenGL as a source for a texture
        if (bufferedImage.getColorModel().hasAlpha()) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
            texImage = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
            texImage = new BufferedImage(glColorModel, raster, false, new Hashtable());
        }

        // copy the source image into the produced image
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0f, 0f, 0f, 0f));
        g.fillRect(0, 0, texWidth, texHeight);
        g.drawImage(bufferedImage, 0, 0, null);

        // build a byte buffer from the temporary image used by OpenGL to produce a texture
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

        imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }

    public void makeCLTexture() {
        try {
            // Initialize OpenCL and create a context and command queue
            CL.create();
            System.out.println("\n****************");
            System.out.println("CL created");

            // Drawable context that OpenCL needs
            Drawable drawable = Display.getDrawable();
            System.out.println("Drawable created");

            // LWJGL CLPlatform object
            CLPlatform platform = CLPlatform.getPlatforms().get(0);
            System.out.println("Platform created");

            // List of LJWGL CLDevice objects representing hardware or software contexts that OpenCL can use
            List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
            System.out.println("Devices obtained");

            // Create the OpenCL context using the patform, devices, and the OpenGL drawable
            CLContext context = CLContext.create(platform, devices, null, drawable, null);
            System.out.println("Context created");

            // Create an command queue using our OpenCL context and the first device in our list of devices
            CLCommandQueue queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);
            
            CLMem clglMem = createSharedMem(context, texture); 
            //CLMem gltoCLTex = CL10GL.clCreateFromGLTexture(context, CL_MEM_READ_WRITE, GL_TEXTURE_BUFFER, 0,texture, errcode_ret);
            System.out.println("Shared mem created");
            
            // use the shared memory in CL
            useSharedMem(clglMem, queue);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("*** Problem initializing OpenCL");
        }
    }

    public CLMem createSharedMem(CLContext context, Texture texture) {
        // Create an OpenGL buffer
        int glBufId = GL15.glGenBuffers();
        // Load the buffer with data using glBufferData();
        // initialize buffer object
        
        GL15.glBindBuffer(GL_ARRAY_BUFFER, glBufId);
        
        //int size = (texture.getImageHeight() * texture.getImageWidth() * 4);
        GL15.glBufferData(GL_ARRAY_BUFFER, textureBuff, GL_DYNAMIC_DRAW);
        // Create the shared OpenCL memory object from the OpenGL buffer
        CLMem glMem = CL10GL.clCreateFromGLBuffer(context, CL10.CL_MEM_READ_WRITE, glBufId, null);

        return glMem;
    }
    
    public void useSharedMem(CLMem clmem, CLCommandQueue queue){
        System.out.println("Acquiring mem lock");
        // Acquire the lock for the 'glMem' memory object
        int error = CL10GL.clEnqueueAcquireGLObjects(queue, clmem, null, null);
        // Remember to check for errors
        if(error != CL10.CL_SUCCESS) { Util.checkCLError(error); }
        
        // Now execute an OpenCL command using the shared memory object,
        // such as uploading data to the memory object using 'CL10.clEnqueueWriteBuffer()'
        // or running a kernel using 'CL10.clEnqueueNDRangeKernel()' with the correct parameters
        // ...
        System.out.println("Doing Stuff");
        // Release the lock on the 'glMem' memory object
        error = CL10GL.clEnqueueReleaseGLObjects(queue, clmem, null, null);
        if(error != CL10.CL_SUCCESS) { Util.checkCLError(error); }

        // Remember to flush the command queue when you are done. 
        // Flushing the queue ensures that all of the OpenCL commands
        // sent to the queue have completed before the program continues. 
        CL10.clFinish(queue);
        deleteSharedMem(clmem);
        System.out.println("Finished using CL & released shared mem");
    }
    
    public void deleteSharedMem(CLMem clmem){
        // Delete/release an OpenCL shared memory object
        CL10.clReleaseMemObject(clmem);
    }
}
