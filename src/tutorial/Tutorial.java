/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.*;
import org.lwjgl.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * @author labramson
 */
public class Tutorial {
    public Tutorial(){
        //CREATES THE DISPLAY
        try{
            Display.setDisplayMode(new DisplayMode(640,480));
            Display.setTitle("Hello World");
            Display.create();
        }catch(LWJGLException e){
            e.printStackTrace();
        }
        
        //init OpenGL
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 640, 480, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        
        //RENDER LOOP
        while(!Display.isCloseRequested()){
            //CLEARS SCREEN EACH LOOP
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            //DRAW A LINE
            glBegin(GL_LINES);
            glVertex2i(100, 100);
            glVertex2i(200, 200);
            glEnd();
            
            //DRAW A SQUARE
            glBegin(GL_QUADS);
            glVertex2i(400, 400); //upper left
            glVertex2i(450, 400); //upper right
            glVertex2i(450, 450); //bottom right
            glVertex2i(400, 450); //bottom left
            glEnd();
            
            //GET MOUSE COORDINATES
            int mouseX = Mouse.getX(); 
            int mouseY = Display.getHeight()-Mouse.getY()-1;
            
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
    
    /** @param args the command line arguments */
    public static void main(String[] args) {
        new Tutorial();
    }
}