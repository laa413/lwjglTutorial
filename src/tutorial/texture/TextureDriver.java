/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.texture;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.*;
import org.lwjgl.*;
import org.lwjgl.input.Keyboard;

/*
 * @author labramson
 */
public class TextureDriver {

    //IMAGE FOR THE TEXTURE
    private static final String imgDir = "C:\\Users\\labramson\\Documents\\Tutorial\\res\\";
    public static String imgName = "smileTexture2.jpg";

    //CONSTRUCTOR
    public TextureDriver() {

    }

    public static void main(String... args) throws Exception {
        initDisplay();
        initGL();

        Image img = new Image(imgDir + "" + imgName);
        Texture texture = new Texture(GL_TEXTURE_2D, GL11.glGenTextures());

        //OBJECT USED TO MAKE THE TEXTURE
        TextureMaker tex = new TextureMaker(img.getImg(), texture);
        texture = tex.MakeTexture();

        while (!Display.isCloseRequested()) {
            //CLEARS SCREEN EACH LOOP
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            //ENABLES GL_TEXTURE_2D
            glEnable(GL_TEXTURE_2D);

            //BIND THE TEXTURE
            texture.bind();

            glPixelStorei(GL_PACK_ALIGNMENT, 4);
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
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 300, 300, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
    }
}