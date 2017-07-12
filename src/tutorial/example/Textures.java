/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.example;
 
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.*;
import org.lwjgl.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author labramson
 */
public class Textures {
    private Texture wood;
    
    public Textures(){
        //CREATES THE DISPLAY
        try{
            Display.setDisplayMode(new DisplayMode(640,480));
            Display.setTitle("Hello World");
            Display.create();
        }catch(LWJGLException e){
            e.printStackTrace();
        }
        
        wood = loadTexture("wood.png");
      
        //init OpenGL
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 640, 480, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_TEXTURE_2D);
        
        //RENDER LOOP
         while(!Display.isCloseRequested()){
            //CLEARS SCREEN EACH LOOP
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            //BIND THE TEXTURE
            wood.bind();
            
            //DRAW A SQUARE WITH MAPPED TEXTURE
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex2i(400, 400); //upper left
            
            glTexCoord2f(1, 0);
            glVertex2i(450, 400); //upper right
            
            glTexCoord2f(1, 1);
            glVertex2i(450, 450); //bottom right
            
            glTexCoord2f(0, 1);
            glVertex2i(400, 450); //bottom left
            glEnd();
            
            //IF ESC THEN CLOSE
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)){
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
    
    private Texture loadTexture(String fileName){
         try {
            return TextureLoader.getTexture("PNG", new FileInputStream(new File("res/" + fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
         return null;
    }
    
    /** @param args the command line arguments */
    public static void main(String[] args) {
        new Textures();
    }
}