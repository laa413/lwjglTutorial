/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

import static java.lang.Math.min;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.*;
import org.lwjgl.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
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
import org.lwjgl.opencl.api.Filter;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;

/*
 * @author labramson
 */
public class Main {

    //IMAGE FOR THE TEXTURE
    private static final String imgDir = "C:\\Users\\labramson\\Documents\\Tutorial\\res\\";
    public static String imgName = "smileTexture2.jpg";

    static final String kernel
            = "kernel void imgTest(read_only image2d_t srcImage, write_only image2d_t finalImage){\n"
            + "    sampler_t sampler = CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;\n"
            + "    uint offset = get_global_id(1) * 0x4000 + get_global_id(0)*0x1000;\n"
            + "    int2 coord = (int2)(get_global_id(0), get_global_id(1));\n"
            + "    uint4 pixel = read_imageui(srcImage, sampler, coord);\n"
            + "    pixel.x -= offset;\n"
            + "    write_imageui(finalImage, coord, pixel);\n"
            + "}";

    //VARS FOR CL CONVERSION
    private static final int MAX_GPUS = 8; //Max GPUs used at once
    private CLContext context;  //CL context
    private CLCommandQueue[] queues;  //array of cl command queues
    private CLKernel[] kernels;  //array of cl kernels for 
    private CLProgram[] programs;  //array of cl programs for 
    private int program;  //array of cl programs for 
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

    //CONSTRUCTOR
    public Main() {
    }

    public static void main(String... args) throws Exception {
        initDisplay();
        initGL();

        //GET THE IMAGE AND GREATE THE GL TEXTURE
        Image img = new Image(imgDir + "" + imgName);
        Texture texture = new Texture(img, GL_TEXTURE_2D, GL11.glGenTextures());

        //init
        Main run = new Main();
        run.initCL();

        CLMem mem = run.createTexture(texture);

        //CONVERT GL TEXTURE TO CL TEXTURE
        while (!Display.isCloseRequested()) {
            //SETS GL SETTINGS
            glSettings();

            //DRAW A SQUARE WITH MAPPED TEXTURE
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex2i(100, 100); //upper left

            glTexCoord2f(0, 1);
            glVertex2i(100, 200); //upper right

            glTexCoord2f(1, 1);
            glVertex2i(200, 200); //bottom right

            glTexCoord2f(1, 0);
            glVertex2i(200, 100); //bottom left
            glEnd();

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
        glViewport(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 300, 300, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    public static void glSettings() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //CLEARS SCREEN EACH LOOP
        glDisable(GL_DEPTH_TEST); //DISABLES DEPTH TEST
        glEnable(GL_TEXTURE_2D); //ENABLES GL_TEXTURE_2D
        glPixelStorei(GL_PACK_ALIGNMENT, 4);
    }

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
            System.out.println("Slices:" + slices);

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

            //CLMem mem = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_READ_ONLY, target, 0, id, null);
            //CLMem mem2 = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_WRITE_ONLY, target, 0, id, null);
        } catch (Exception e) {
            System.out.println("*** Problem creating CL texture");
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
            programs[i] = CL10.clCreateProgramWithSource(context, kernel, null);
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

    private void fillQueue(CLContext context) {
        for (int i = 0; i < slices; i++) {
            // create command queue and upload color map buffer on each used device
            queues[i] = CL10.clCreateCommandQueue(context, context.getInfoDevices().get(i), CL_QUEUE_PROFILING_ENABLE, null);
            queues[i].checkValid();
        }
    }

    private void initKernel() {
        // init kernel with constants
        for (int i = 0; i < kernels.length; i++) {
            kernels[i] = CL10.clCreateKernel(programs[min(i, programs.length)], "imgTest", null);
        }
    }

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

    private CLMem createTexture(Texture texture) {
        return CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_READ_ONLY, texture.getTarget(), 0, texture.getId(), null);
    }
}
