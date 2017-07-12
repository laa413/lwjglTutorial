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

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * @author labramson
 */
public class Boxes {
    private List<Box> shapes = new ArrayList<Box>(16); //an array of boxes
    private boolean boxIsSelected = false; //is a box selected
    private volatile boolean randColorChange = false;  //should random color change 
    
    public Boxes(){
        //CREATES THE DISPLAY
        try{
            Display.setDisplayMode(new DisplayMode(640,480));
            Display.setTitle("Hello World");
            Display.create();
        }catch(LWJGLException e){
            e.printStackTrace();
        }
        
        //ADDS 2 BOXES TO LIST
        shapes.add(new Box(15,15)); 
        shapes.add(new Box(100,150));
        
        //init OpenGL
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 640, 480, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        
        //RENDER LOOP
        while(!Display.isCloseRequested()){
            //CLEARS SCREEN EACH LOOP
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            //IF ESC THEN CLOSE
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)){
                Display.destroy();
                System.exit(0);
            }
            
            //IF C PRESSED THEN MAKE NEW BOX
            while (Keyboard.next()){
                if (Keyboard.getEventKey()== Keyboard.KEY_C && Keyboard.getEventKeyState()){
                    shapes.add(new Box(15, 15));
                }
            }
            
            //FOR EACH BOX IN THE LIST
            for (Box box : shapes){
                if (Mouse.isButtonDown(0) && box.inWindow(Mouse.getX(), Display.getHeight()-Mouse.getY()-1) && !boxIsSelected){
                    boxIsSelected = true;
                    box.selected = true;
                }else if (Mouse.isButtonDown(1)){
                    boxIsSelected = false;
                    box.selected = false;
                }else if (Mouse.isButtonDown(2) && box.inWindow(Mouse.getX(), Display.getHeight()-Mouse.getY()-1) && !boxIsSelected){
                    box.randomColors();
                    randColorChange = true;
                    //ALLOWS FOR LONGER TIME BETWEEN COLOR CHANGES
                    new Thread(new Runnable(){
                      @Override
                      public void run(){
                          try{
                              Thread.sleep(200);
                          }catch(InterruptedException e){
                              e.printStackTrace();
                          }finally{
                              randColorChange = false;
                          }
                      }
                    }).run();
                }
                
                //ALLOWS BOX TO MOVE IF SELECTED
                if (box.selected){
                    box.changeXY(Mouse.getDX(), -Mouse.getDY());
                }
                
                //DRAWS THE BOX
                box.drawBox();
            }
            
            //UPDATES THE DISPLAY WITH 60 FPS
            Display.update();
            Display.sync(60);
        }
        
        //DESTROYS THE DISPLAY
        Display.destroy();
    }
    
    private static class Box{
        public int x, y; //x & y origin of box
        public boolean selected = false; //is box selected
        private float colorR, colorG, colorB; //color values of box
        
        //CONSTRUCTOR
        Box(int x, int y){
            this.x = x;
            this.y = y;
            Random generator = new Random();
            colorR = generator.nextFloat();
            colorG = generator.nextFloat();
            colorB = generator.nextFloat();
        }
        
        //GENERATES A RANDOM COLOR
        void randomColors(){
            Random generator = new Random();
            colorR = generator.nextFloat();
            colorG = generator.nextFloat();
            colorB = generator.nextFloat();
        }
        
        //CHECKS IF THE MOUSE IS IN THE WINDOW
        boolean inWindow(int mouseX, int mouseY){
            if (mouseX > x && mouseX < x+50 && mouseY > y && mouseY < y+50){
                return true;
            }else{
                return false;
            }
        }
        
        //CHANGES THE POSITION OF THE BOX
        void changeXY(int dx, int dy){
            x+=dx;
            y+=dy;
        }
        
        //DRAWS THE BOX WITH THE COLORS
        void drawBox(){
            glColor3f(colorR, colorG, colorB);
            
            glBegin(GL_QUADS);
            glVertex2i(x, y); //upper left
            glVertex2i(x+50, y); //upper right
            glVertex2i(x+50, y+50); //bottom right
            glVertex2i(x, y+50); //bottom left
            glEnd();
        }
    }
    
    /** @param args the command line arguments */
    public static void main(String[] args) {
        new Boxes();
    }
}