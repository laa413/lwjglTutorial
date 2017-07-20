/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

import java.awt.Color;
import static java.lang.Math.min;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Set;

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
import static org.lwjgl.opencl.CL10.CL_MAP_WRITE;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.CL_TRUE;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLDeviceCapabilities;
import org.lwjgl.opencl.CLEvent;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;
import org.lwjgl.opencl.api.Filter;
import org.lwjgl.opengl.ContextCapabilities;
import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glNewList;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

import static tutorial.clTexture.Main.toFloatBuffer;

/*
 * @author LAA
 */
public class Texture {

    private final int width, height, target, id;
    private final Image image;

    //KERNEL EXAMPLE VARIABLES
    static final String source
            = "kernel void sum(global const float *a, global const float *b, global float *answer)"
            + "{ unsigned int xid = get_global_id(0);  answer[xid] = a[xid] + b[xid];}";
    static final FloatBuffer a = toFloatBuffer(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    static final FloatBuffer b = toFloatBuffer(new float[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0});
    static final FloatBuffer answer = BufferUtils.createFloatBuffer(a.capacity());

    //TRYING TO WORK WITH BUFFERS
    public ByteBuffer buf, ptrbufs;
    public PointerBuffer pointers, ptrbuff;

    //MY KERNEL FUNCTION FROM TEXTBOOK PG
    static final String kernel
            = "kernel void imgTest(read_only image2d_t srcImage, write_only image2d_t finalImage){\n"
            + "    constant sampler_t sampler = CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;\n"
            + "    uint offset get_global_id(1) * 0x4000 + get_global_id(0)*0x1000;\n"
            + "    int2 coord = (int2)(get_global_id(0), get_global_id(1));\n"
            + "    uint4 pixel = read_imageui(srcImage, sampler, coord);\n"
            + "    pixel.x -= offset;\n"
            + "    write_imageui(finalImage, coord, pixel);\n"
            + "}";

    //VARS FOR CL CONVERSION
    private static final int MAX_GPUS = 8; //Max GPUs used at once
    private CLContext clContext;  //CL context
    private CLCommandQueue[] queues;  //array of cl command queues
    private CLKernel[] kernels;  //array of cl kernels for 
    private CLProgram[] programs;  //array of cl programs for 
    private CLMem[] glBuffers;  //array of clm for 
    private CLMem[] colorMap;  //array of cl color maps for 
    private IntBuffer glIDs;  //int buffer for 
    private boolean useTextures;  //for something...
    private final PointerBuffer kernel2DGlobalWorkSize = BufferUtils.createPointerBuffer(2);  //the global work size of the kernel
    private int slices;  //dividing up the image for faster processing
    private boolean drawSeparator;  //no idea what this is
    private boolean doublePrecision = true;  //doubles used instead of floats
    private boolean buffersInitialized;  //buffers for something initialized
    private boolean rebuild;  //boolean for rerendering
    private boolean run = true;  //boolena for running the program
    private final PointerBuffer syncBuffer = BufferUtils.createPointerBuffer(1);  //buffer for dealing with gl cl sync
    private boolean syncGLtoCL; // true if we can make GL wait on events generated from CL queues.
    private boolean syncCLtoGL; // true if we can make CL wait on sync objects generated from GL.
    private CLEvent[] clEvents;  //array of cl events for 
    private CLEvent glEvent;  //cl event 
    private GLSync[] clSyncs; //array of gl sync objects for 
    private GLSync glSync; //glsync so that cl & gl dont cause race condition
    private int deviceType = CL10.CL_DEVICE_TYPE_GPU;

    public Texture(Image image, int target, int id) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.target = target;
        this.id = id;

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

            queues = new CLCommandQueue[slices];
            kernels = new CLKernel[slices];

            // Create an command queue using our OpenCL context and the first device in our list of devices
            for (int i = 0; i < slices; i++) {
                colorMap[i] = clCreateBuffer(clContext, CL_MEM_READ_ONLY, 256, null);
                colorMap[i].checkValid();

                // create command queue and upload color map buffer on each used device
                queues[i] = CL10.clCreateCommandQueue(context, context.getInfoDevices().get(i), CL_QUEUE_PROFILING_ENABLE, null);
                queues[i].checkValid();

                CL10.clEnqueueUnmapMemObject(queues[i], colorMap[i], null, null, null);
            }
            //CLCommandQueue queue = CL10.clCreateCommandQueue(context, context.getInfoDevices().get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);
            System.out.println("Command Queue created");

            CLMem mem = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_READ_ONLY, target, 0, id, null);
            CLMem mem2 = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_WRITE_ONLY, target, 0, id, null);

            GL11.glFinish();

            kernelFunction(context, queues);
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

            final Filter<CLDevice> glSharingFilter;
            glSharingFilter = (final CLDevice device) -> {
                final CLDeviceCapabilities abilities = CLCapabilities.getDeviceCapabilities(device);
                return abilities.CL_KHR_gl_sharing;
            };

            // List of LJWGL CLDevice objects representing hardware or software contexts that OpenCL can use
            List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
            if (devices == null) {
                deviceType = CL10.CL_DEVICE_TYPE_CPU;
                devices = platform.getDevices(CL10.CL_DEVICE_TYPE_CPU, glSharingFilter);
                if (devices == null) {
                    throw new RuntimeException("No OpenCL devices found with KHR_gl_sharing support.");
                }
            }
            System.out.println("Devices obtained");

            // Create the OpenCL context using the patform, devices, and the OpenGL drawable
            CLContext context = CLContext.create(platform, devices, null, drawable, null);
            System.out.println("Context created");

            slices = min(devices.size(), MAX_GPUS);

            return context;

        } catch (LWJGLException e) {
            System.out.println("*** Problem initializing OpenCL");
            e.printStackTrace();
            return null;
        }
    }

    private void kernelFunction(CLContext context, CLCommandQueue[] queues) {
        //BELOW IS CODE TO TRY TO WORK WITH A KERNEL; KERNEL WILL NEED TO STUFF WITH TEXTURE
        //THIS CODE IS FROM MY HelloWorld.java
        //needs to be passed the texture

        /*
        //Allocates memory for input buffers
        CLMem aMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, null);
        clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);

        CLMem bMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, b, null);
        clEnqueueWriteBuffer(queue, bMem, 1, 0, b, null, null);

        //Allocates memory four output buffer
        CLMem answerMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, answer, null);
        clFinish(queue);

        //Creates the program using context & source
        CLProgram program = clCreateProgramWithSource(context, kernel, null);
        
        //Creates the kernel; function name passed in must match kernel method 
        //in OCL sourcesum method is defined in String source used in creating the program
        CLKernel kernel = clCreateKernel(program, "imgTest", null);

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
         */
        //Trying logic from demofractal
        for (int i = 0; i < programs.length; i++) {
            programs[i] = clCreateProgramWithSource(clContext, kernel, null);
            //Checks if there was an error in creation
            Util.checkCLError(clBuildProgram(programs[i], context.getInfoDevices().get(i), "", null));
        }

        for (int i = 0; i < kernels.length; i++) {
            kernels[i] = clCreateKernel(programs[min(i, programs.length)], "imgTest", null);
        }

        final ContextCapabilities abilities = GLContext.getCapabilities();
        // Detect GLtoCL synchronization method
        syncGLtoCL = abilities.GL_ARB_cl_event; // GL3.2 or ARB_sync implied
        if (syncGLtoCL) {
            clEvents = new CLEvent[slices];
            clSyncs = new GLSync[slices];
            System.out.println("\nGL to CL sync: Using OpenCL events");
        } else {
            System.out.println("\nGL to CL sync: Using clFinish");
        }
        // Detect CLtoGL synchronization method
        syncCLtoGL = abilities.OpenGL32 || abilities.GL_ARB_sync;
        if (syncCLtoGL) {
            for (CLDevice device : context.getInfoDevices()) {
                if (!CLCapabilities.getDeviceCapabilities(device).CL_KHR_gl_event) {
                    syncCLtoGL = false;
                    break;
                }
            }
        }
        if (syncCLtoGL) {
            System.out.println("CL to GL sync: Using OpenGL sync objects");
        } else {
            System.out.println("CL to GL sync: Using glFinish");
        }
        
        useTextures = (deviceType == CL10.CL_DEVICE_TYPE_GPU) && (!abilities.OpenGL21);
        
        if (useTextures){
            glNewList(glGenLists(1), GL_COMPILE);
            
            
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
