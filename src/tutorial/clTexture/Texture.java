/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

import static tutorial.clTexture.Main.toFloatBuffer;

/*
 * @author LAA
 */
public class Texture {

    private final int width, height, target, id;
    private final Image image;

    static final String source
            = "kernel void sum(global const float *a, global const float *b, global float *answer)"
            + "{ unsigned int xid = get_global_id(0);  answer[xid] = a[xid] + b[xid];}";
    static final String kernel
            = "kernel void imgTest()"
            + "{ //Do stuff here }";
    static final FloatBuffer a = toFloatBuffer(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    static final FloatBuffer b = toFloatBuffer(new float[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0});
    static final FloatBuffer answer = BufferUtils.createFloatBuffer(a.capacity());

    public ByteBuffer buf, ptrbufs;
    public PointerBuffer pointers;
    

    static PointerBuffer ptrbuff;

    public Texture(Image image, int target, int id) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.target = target;
        this.id = id;

        //byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
       buf = ByteBuffer.allocateDirect(16);
       buf.put(0, )
      
        // Create a direct (memory-mapped) ByteBuffer with a 10 byte capacity.
       // buf = ByteBuffer.wrap(bytes);
        pointers = new PointerBuffer(buf);
         
        byte[] ptrs = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        ByteBuffer ptrbufs = ByteBuffer.allocateDirect(10);
        // Create a direct (memory-mapped) ByteBuffer with a 10 byte capacity.
        ptrbufs = ByteBuffer.wrap(ptrs);
//        // Get the ByteBuffer's capacity
        int capacity = buf.capacity(); // 10
        
        System.out.println(capacity);

        
        
        
        // bind this texture 
        GL11.glBindTexture(target, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, image.getWidth() + 1, image.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, image.getByeBuff());
    }

    public void convertToCL() {
        try {
            CLContext context = createCLContext();

            // Create an command queue using our OpenCL context and the first device in our list of devices
            CLCommandQueue queue = CL10.clCreateCommandQueue(context, context.getInfoDevices().get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);
            System.out.println("Command Queue created");

            CLMem mem = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_READ_ONLY, target, 0, id, null);

            System.out.println(CL10.clGetImageInfo(mem, CL10.CL_IMAGE_WIDTH, buf, pointers));
            GL11.glFinish();

            kernelFunction(context, queue);
        } catch (Exception e) {
            System.out.println("*** Problem creating CL texture");
            e.printStackTrace();
        }
    }

    private CLContext createCLContext() {
        try {
            // Initialize OpenCL and create a context and command queue
            CL.create();
            System.out.println("\nCL created");

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

            return context;

        } catch (LWJGLException e) {
            System.out.println("*** Problem initializing OpenCL");
            e.printStackTrace();
            return null;
        }
    }

    private void kernelFunction(CLContext context, CLCommandQueue queue) {
        //BELOW IS CODE TO TRY TO WORK WITH A KERNEL; KERNEL WILL NEED TO STUFF WITH TEXTURE
        //THIS CODE IS FROM MY HelloWorld.java
        //needs to be passed the texture

        //Allocates memory for input buffers
        CLMem aMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, null);
        clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);

        CLMem bMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, b, null);
        clEnqueueWriteBuffer(queue, bMem, 1, 0, b, null, null);

        //Allocates memory four output buffer
        CLMem answerMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, answer, null);
        clFinish(queue);

        //Creates the program using context & source
        CLProgram program = clCreateProgramWithSource(context, source, null);
        //Checks if there was an error in creation
        Util.checkCLError(clBuildProgram(program, context.getInfoDevices().get(0), "", null));

        //Creates the kernel; function name passed in must match kernel method in OCL source
        //sum method is defined in String source used in creating the program
        CLKernel kernel = clCreateKernel(program, "sum", null);

        //Execute the kernel
        PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
        kernel1DGlobalWorkSize.put(0, a.capacity());
        //Set arguments for the kernel; inputs & output
        kernel.setArg(0, aMem);
        kernel.setArg(1, bMem);
        kernel.setArg(2, answerMem);
        //Enqueue the kernel
        clEnqueueNDRangeKernel(queue, kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);

        //Read results from memory back into buffer
        clEnqueueReadBuffer(queue, answerMem, 1, 0, answer, null, null);
        clFinish(queue);

        //END OF KERNEL WORK; PRINTS OUT ANSWER FROM FLOAT BUFFER WITH ANSWER
        for (int i = 0; i < a.capacity(); i++) {
            System.out.print(a.get(i) + " ");
        }
        System.out.println("+");
        for (int i = 0; i < b.capacity(); i++) {
            System.out.print(b.get(i) + " ");
        }
        System.out.println("=");
        for (int i = 0; i < answer.capacity(); i++) {
            System.out.print(answer.get(i) + " ");
        }
    }

    public void bind() {
        GL11.glBindTexture(target, id);
    }

    public int getTarget() {
        return target;
    }

    public Image getImage() {
        return image;
    }

    public int getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
