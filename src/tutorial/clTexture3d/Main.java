/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture3d;

import com.sun.prism.impl.BufferUtil;
import java.nio.IntBuffer;
import java.util.List;
import org.lwjgl.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CL;
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
import org.lwjgl.opencl.api.Filter;
import org.lwjgl.opengl.*;
import static java.lang.Math.min;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueWaitForEvents;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10GL.clEnqueueAcquireGLObjects;
import static org.lwjgl.opencl.KHRGLEvent.clCreateEventFromGLsyncKHR;
import static org.lwjgl.opengl.ARBCLEvent.glCreateSyncFromCLeventARB;
import static org.lwjgl.opengl.ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.ARBSync.glFenceSync;
import static org.lwjgl.opengl.ARBSync.glWaitSync;
import static org.lwjgl.opengl.ARBTextureRg.GL_R16;
import static org.lwjgl.opengl.ARBTextureRg.GL_R16F;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
//import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
//import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;

/*
 * @author labramson
 */
public class Main {
    //public static Image img;
    //public static Image img2;
    private Texture inTexture, outTexture;

    static final String kernel_Sample
            = "kernel void imgTest(__read_only image2d_t inputTexture){\n"
            + "    int2 imgCoords = (int2)(get_global_id(0), get_global_id(1));\n"
            + "    //printf(\"Coord x:%d Coord y:%d \", imgCoords.x, imgCoords.y);\n\n"
            + "    const sampler_t smp =  CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;\n\n"
            + "    float4 imgVal = read_imagef(inputTexture, smp, imgCoords); \n"
            + "    printf(\"(%d, %d) RGBA(%f, %f, %f, %f)\\n\", imgCoords.x, imgCoords.y, imgVal.x, imgVal.y, imgVal.z, imgVal.w);\n"
            + "}";
    static final String kernel_3DImage
            = "kernel void imgTest2(__read_only image3d_t inTexture, __write_only image3d_t outTexture){\n"
            + "    #pragma OPENCL EXTENSION cl_khr_3d_image_writes : enable\n"
            + "    const sampler_t smp =  CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;\n\n"
            + "    int4 coord = (int4)(get_global_id(0), get_global_id(1), get_global_id(2), 0);\n"
            + "    int4 coordL = (int4)(get_global_id(0)-1, get_global_id(1), get_global_id(2), 0);\n"
            + "    int4 coordR = (int4)(get_global_id(0)+1, get_global_id(1), get_global_id(2), 0);\n"
            + "    int4 coordT = (int4)(get_global_id(0), get_global_id(1)-1, get_global_id(2), 0);\n"
            + "    int4 coordBo = (int4)(get_global_id(0), get_global_id(1)+1, get_global_id(2), 0);\n"
            + "    int4 coordF = (int4)(get_global_id(0), get_global_id(1), get_global_id(2)-1, 0);\n"
            + "    int4 coordBa = (int4)(get_global_id(0), get_global_id(1), get_global_id(2)-1, 0);\n"
            + "    float4 pixel = read_imagef(inTexture, smp, coord);\n"
            + "    float4 pixelL = read_imagef(inTexture, smp, coordL);\n"
            + "    float4 pixelR = read_imagef(inTexture, smp, coordR);\n"
            + "    float4 pixelT = read_imagef(inTexture, smp, coordT);\n"
            + "    float4 pixelBo = read_imagef(inTexture, smp, coordBo);\n"
            + "    float4 pixelF = read_imagef(inTexture, smp, coordF);\n"
            + "    float4 pixelBa = read_imagef(inTexture, smp, coordBa);\n"
            + "    pixel.x = (pixelR.x - pixelL.x); \n"
            + "    if (pixel.x  < 0.0) {pixel.x = -pixel.x;} \n"
            + "    pixel.y = pixelT.x - pixelBo.x; \n"
            + "    if (pixel.y  < 0.0) {pixel.y = -pixel.y;} \n"
            + "    pixel.z = pixelF.x - pixelBa.x; \n"
            + "    if (pixel.z  < 0.0) {pixel.z = -pixel.z;} \n"
            + "    write_imagef(outTexture, coord, pixel);\n"
            + "}";

    //VARS FOR CL CONVERSION
    private CLCommandQueue[] queues;  //array of cl command queues
    private CLContext context;  //CL context
    private CLEvent glEvent;  //cl event 
    private CLEvent[] clEvents;  //array of cl events for 
    private CLKernel[] kernels;  //array of cl kernels for 
    private CLMem[] glBuffers;  //array of clm for 
    private CLMem[] glBuffersOut;  //array of clm for 
    private CLProgram[] programs;  //array of cl programs for 
    private GLSync glSync; //glsync so that cl & gl dont cause race condition
    private GLSync[] clSyncs; //array of gl sync objects for 
    private IntBuffer glIDs;  //int buffer for 
    private IntBuffer glIDs2;  //int buffer for 
    private boolean buffersInitialized;  //buffers for something initialized
    private boolean drawSeparator;  //no idea what this is
    private boolean rebuild;  //boolean for rerendering
    private boolean syncCLtoGL; // true if we can make CL wait on sync objects generated from GL.
    private boolean syncGLtoCL; // true if we can make GL wait on events generated from CL queues.
    private final boolean doublePrecision = true;  //doubles used instead of floats
    private final boolean useTextures = true;  //for something...
    private final PointerBuffer kernel2DGlobalWorkSize = BufferUtils.createPointerBuffer(3);  //the global work size of the kernel
    private final PointerBuffer syncBuffer = BufferUtils.createPointerBuffer(1);  //buffer for dealing with gl cl sync
    private int deviceType = CL10.CL_DEVICE_TYPE_GPU;
    private int slices;  //dividing up the image for faster processing
    private static final int MAX_GPUS = 8; //Max GPUs used at once

    //CONSTRUCTOR
    public Main() {
    }

    public static void main(String... args) throws Exception {
        //INIT DISPLAY & GL CONTEXT
        initDisplay();
        initGL();

        //INSTANCE OF MAIN TO RUN FUNCTIONS
        Main run = new Main();

        //SET UP CL
        run.initCL();

        //CREATE THE TEXTURE IN GL CONTEXT
        run.initGLTexture();
        run.initWritableTexture();

        //COMPLETE ALL GL
        glFinish();

        //SET THE KERNEL PARAMS FOR CL COMPUTAIONS
        run.setKernelParams();

        //DISPLAY LOOP
        while (!Display.isCloseRequested()) {
            //ALLOWS DISPLAYING ON SCREEN
            run.display();

            //IF ESC THEN CLOSE
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
                Display.destroy();
                System.exit(0);
            }

            //UPDATES THE DISPLAY WITH 60 FPS
            Display.update();
            Display.sync(60);
        }

        //DESTROYS THE DISPLAY
        Display.destroy();
    }

    public static void initDisplay() {
        try {
            Display.setDisplayMode(new DisplayMode(300, 300));
            Display.setTitle("Texture Demo");
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        Display.update();
    }

    public static void initGL() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 300, 300, 0, 10, -300);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    //GL SETTINGS FOR THE TEXTURE
    public static void glSettings() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //CLEARS SCREEN EACH LOOP
        glEnable(GL_DEPTH_TEST); //DISABLES DEPTH TEST
        glEnable(GL_TEXTURE_3D); //ENABLES GL_TEXTURE_3D
        glPixelStorei(GL_PACK_ALIGNMENT, 4);
    }

    //INITIALIZE THE GL TEXTURE
    public void initGLTexture() throws Exception {
        if (glBuffers == null) {
            glBuffers = new CLMem[slices];

            glIDs = BufferUtils.createIntBuffer(slices);
        } else {
            for (CLMem mem : glBuffers) {
                clReleaseMemObject(mem);
            }

            if (useTextures) {
                glDeleteTextures(glIDs);
            } else {
                glDeleteBuffers(glIDs);
            }
        }

        if (useTextures) {
            GL11.glGenTextures(glIDs);
            for (int i = 0; i < slices; i++) {
                ByteBuffer bbuf = BufferUtil.newByteBuffer(64 * 64 * 64 * 2);
                ShortBuffer sbuf = bbuf.asShortBuffer();
                for (int z=0; z<64; z++) {
                    for (int y=0; y<64; y++) {
                        for (int x=0; x<64; x++) {
                            short value = 0;
                            if ( x>16 && x<50 && y>16 && y<50 && z>16 && z<50) {
                                value = 32767;
                            }
                            else if ( x>18 && x<48 && y>18 && y<48 && z>18 && z<48) {
                                value = 128;
                            }
                            sbuf.put(value);
                        }
                    }
                }
               
             
                bbuf.flip();
                bbuf.order(ByteOrder.LITTLE_ENDIAN);
                
                Texture texture = new Texture(64, 64, 64, GL_R16F, bbuf);
                glBuffers[i] = CL10GL.clCreateFromGLTexture3D(context, CL10.CL_MEM_READ_ONLY, texture.getTarget(), 0, texture.getId(), null);
                inTexture = texture;
            }
            //UNBINDS THE TEXTURE
            glBindTexture(GL_TEXTURE_3D, 0);
        }
    }

    public void initWritableTexture() throws Exception {
        if (glBuffersOut == null) {
            glBuffersOut = new CLMem[slices];

            glIDs2 = BufferUtils.createIntBuffer(slices);
        } else {
            for (CLMem mem : glBuffersOut) {
                clReleaseMemObject(mem);
            }

            if (useTextures) {
                glDeleteTextures(glIDs2);
            } else {
                glDeleteBuffers(glIDs2);
            }
        }

        if (useTextures) {
            GL11.glGenTextures(glIDs2);
            for (int i = 0; i < slices; i++) {
                Texture texture = new Texture(64, 64, 64, GL_RGBA16F, null);
                glBuffersOut[i] = CL10GL.clCreateFromGLTexture3D(context, CL10.CL_MEM_WRITE_ONLY, texture.getTarget(), 0, texture.getId(), null);
                outTexture = texture;
            }
            glBindTexture(GL_TEXTURE_3D, 0);
        }
        buffersInitialized = true;
    }

    //INITIALIZE CL PLATFORM, DEVICES, CONTEXT, COMMAND QUEUE, KERNEL
    public void initCL() {
        try {
            // Initialize OpenCL
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
                System.out.println(abilities.CL_KHR_3d_image_writes);
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

            slices = min(devices.size(), MAX_GPUS);
            System.out.println("Slices: " + slices);

            // Create the OpenCL context using the patform, devices, and the OpenGL drawable
            context = CLContext.create(platform, devices, null, drawable, null);
            System.out.println("Context created");

            // Create an command queue using our OpenCL context
            queues = new CLCommandQueue[slices];
            fillQueue(context);
            System.out.println("Command Queue created");

            buildProgram(context);
            System.out.println("Program created");

            kernels = new CLKernel[slices];
            initKernel();
            System.out.println("Kernel initialized");

            //CHECK SYNC STATUS BTWN GL & CL
            syncStatus(context);

        } catch (RuntimeException | LWJGLException e) {
            System.out.println("*** Problem creating CL context");
            e.printStackTrace();
        }
    }

    //CREATES THE CL PROGRAM WITH CORRECT OPTIONS
    private void buildProgram(CLContext context) {
        programs = new CLProgram[slices];
        if (programs[0] != null) {
            for (CLProgram program : programs) {
                CL10.clReleaseProgram(program);
            }
        }

        for (int i = 0; i < programs.length; i++) {
            programs[i] = CL10.clCreateProgramWithSource(context, kernel_3DImage, null);
        }

        for (int i = 0; i < programs.length; i++) {
            final CLDevice device = queues[i].getCLDevice();
            final StringBuilder options = new StringBuilder(useTextures ? "-D USE_TEXTURE" : "");
            final CLDeviceCapabilities caps = CLCapabilities.getDeviceCapabilities(device);

            if (doublePrecision) {
                //cl_khr_fp64
                options.append(" -D DOUBLE_FP");

                //amd's verson of double precision floating point math
                if (!caps.CL_KHR_fp64 && caps.CL_AMD_fp64) {
                    options.append(" -D AMD_FP");
                }
            }
            System.out.println("\nOpenCL COMPILER OPTIONS: " + options);

            try {
                CL10.clBuildProgram(programs[i], device, options, null);
            } finally {
                System.out.println("BUILD LOG: " + programs[i].getBuildInfoString(device, CL_PROGRAM_BUILD_LOG));
            }
        }
    }

    //FILLS THE COMMAND QUEUE APPROPRIATELY
    private void fillQueue(CLContext context) {
        for (int i = 0; i < slices; i++) {
            // create command queue on each used device
            queues[i] = CL10.clCreateCommandQueue(context, context.getInfoDevices().get(i), CL_QUEUE_PROFILING_ENABLE, null);
            queues[i].checkValid();
        }
    }

    //INITIALIZES THE KERNEL
    private void initKernel() {
        for (int i = 0; i < kernels.length; i++) {
            kernels[i] = CL10.clCreateKernel(programs[min(i, programs.length)], "imgTest2", null);
        }
    }

    //PASSES IN THE ARGUMENTS TO THE KERNEL
    private void setKernelParams() {
        for (int i = 0; i < slices; i++) {
            kernels[i].setArg(0, glBuffers[i])
                    .setArg(1, glBuffersOut[i]);
        }
    }

    //CHECKS THE SYNC STATUS BTWN GL & CL AND VISE VERSA
    private void syncStatus(CLContext context) {
        final ContextCapabilities abilities = GLContext.getCapabilities();

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
    }

    //DISPLAYS THE RESULTS
    public void display() throws Exception {
        //CHECKS TO MAKE SURE ALL GL EVENTS HAVE COMPLETED
        if (syncCLtoGL && glEvent != null) {
            for (final CLCommandQueue queue : queues) {
                clEnqueueWaitForEvents(queue, glEvent);
            }
        } else {
            glFinish();
        }

        //IF THE GL TEXTURE BUFFERS HAVE NOT BEEN INITIALIZED
        if (!buffersInitialized) {
            initGLTexture();
            setKernelParams();
        }

        //IF CHANGES OCCURED, AND NEEDS TO REBUILD PROGRAM & KERNEL
        if (rebuild) {
            buildProgram(context);
            setKernelParams();
        }

        //SETS THE WORKSIZE OF THE KERNEL
        kernel2DGlobalWorkSize.put(0, 64).put(1, 64).put(2, 64);

        //GETS THE GL OBJECTS 
        for (int i = 0; i < slices; i++) {
            // acquire GL objects, and enqueue a kernel with a probe from the list
            clEnqueueAcquireGLObjects(queues[i], glBuffers[i], null, null);
            clEnqueueAcquireGLObjects(queues[i], glBuffersOut[i], null, null);

            clEnqueueNDRangeKernel(queues[i], kernels[i], 3,
                    null,
                    kernel2DGlobalWorkSize,
                    null,
                    null, null);

            CL10GL.clEnqueueReleaseGLObjects(queues[i], glBuffers[i], null, syncGLtoCL ? syncBuffer : null);
            CL10GL.clEnqueueReleaseGLObjects(queues[i], glBuffersOut[i], null, syncGLtoCL ? syncBuffer : null);
            if (syncGLtoCL) {
                clEvents[i] = queues[i].getCLEvent(syncBuffer.get(0));
                clSyncs[i] = glCreateSyncFromCLeventARB(queues[i].getParent(), clEvents[i], 0);
            }
        }

        // block until done (important: finish before doing further gl work)
        if (!syncGLtoCL) {
            for (int i = 0; i < slices; i++) {
                clFinish(queues[i]);
            }
        }

        //RENDER THE TEXTURE
        render();
    }

    private void render() {
        if (syncGLtoCL) {
            for (int i = 0; i < slices; i++) {
                glWaitSync(clSyncs[i], 0, 0);
            }
        }

        //draw slices
        //int sliceWidth = texture.getWidth() / slices;

        for (int i = 0; i < slices; i++) {
            //int seperatorOffset = drawSeparator ? i : 0;
            
            //SETS GL SETTINGS
            glSettings();

            //BIND THE TEXTURE
            glEnable(GL_TEXTURE_3D);
            glBindTexture(GL_TEXTURE_3D, outTexture.getId());

            //DRAW A CUBE WITH MAPPED TEXTURE
            glBegin(GL_QUADS);
              //System.out.println("Drawing front");
            glTexCoord3f(0f, 0f, 0.5f);
            glVertex3f(100, 100, 0); //upper left
            glTexCoord3f(0f, 1.0f, 0.5f);
            glVertex3f(100, 200, 0);  //upper right
            glTexCoord3f(1.0f, 1.0f, 0.5f);
            glVertex3f(200, 200, 0); //bottom right
            glTexCoord3f(1.0f, 0f, 0.5f);
            glVertex3f(200, 100, 0); //bottom left
            
//            // Top-face
//            System.out.println("Drawing top");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(1.0f, 1.0f, -1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(-1.0f, 1.0f, -1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(-1.0f, 1.0f, 1.0f);
//            glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(1.0f, 1.0f, 1.0f);
//             
//            // Bottom-face
//            System.out.println("Drawing bottom");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(1.0f, -1.0f, 1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(-1.0f, -1.0f, 1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(-1.0f, -1.0f, -1.0f);
//            glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(1.0f, -1.0f, -1.0f);
//
//            // Front-face
//            System.out.println("Drawing front");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(1.0f, 1.0f, 1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(-1.0f, 1.0f, 1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(-1.0f, -1.0f, 1.0f);
//            glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(1.0f, -1.0f, 1.0f);
//
//            // Back-face
//            System.out.println("Drawing back");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(1.0f, -1.0f, -1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(-1.0f, -1.0f, -1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(-1.0f, 1.0f, -1.0f);
//             glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(1.0f, 1.0f, -1.0f);
//
//            // Left-face
//            System.out.println("Drawing left");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(-1.0f, 1.0f, 1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(-1.0f, 1.0f, -1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(-1.0f, -1.0f, -1.0f);
//            glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(-1.0f, -1.0f, 1.0f);
//
//            // Right-face
//            System.out.println("Drawing right");
//            glTexCoord3f(0f, 0f, 0f);
//            glVertex3f(1.0f, 1.0f, -1.0f);
//            glTexCoord3f(0f, 1.0f, 0f);
//            glVertex3f(1.0f, 1.0f, 1.0f);
//            glTexCoord3f(1.0f, 1.0f, 0f);
//            glVertex3f(1.0f, -1.0f, 1.0f);
//            glTexCoord3f(1.0f, 0f, 0f);
//            glVertex3f(1.0f, -1.0f, -1.0f);

            glEnd();
            
            glBindTexture(GL_TEXTURE_3D, 0);
            glDisable(GL_TEXTURE_3D);
        }

        //CHECKING SYNC
        if (syncCLtoGL) {
            glSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            glEvent = clCreateEventFromGLsyncKHR(context, glSync, null);
        }
    }
}