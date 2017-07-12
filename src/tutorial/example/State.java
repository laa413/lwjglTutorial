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
public class State {

    private static enum States {
        INTRO, MAIN, GAME;
    }

    private States state = States.INTRO;

    public State() {
        //CREATES THE DISPLAY
        try {
            Display.setDisplayMode(new DisplayMode(640, 480));
            Display.setTitle("Hello World");
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
        }

        //init OpenGL
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 640, 480, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        //RENDER LOOP
        while (!Display.isCloseRequested()) {
            //CLEARS SCREEN EACH LOOP
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            checkInput();
            render();

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

    private void checkInput() {
        switch (state) {
            case INTRO:
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                    state = States.MAIN;
                }
                break;
            case MAIN:
                if (Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
                    state = States.INTRO;
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
                    state = States.GAME;
                }
                break;
            case GAME:
                if (Keyboard.isKeyDown(Keyboard.KEY_DELETE)) {
                    state = States.MAIN;
                }
                break;
        }
    }

    private void render() {
        switch (state) {
            case INTRO:
                glColor3f(1.0f, 0f, 0f);
                glRectf(0, 0, 640, 480);
                break;
            case MAIN:
                glColor3f(0f, 1.0f, 0f);
                glRectf(0, 0, 640, 480);
                break;
            case GAME:
                glColor3f(0f, 0f, 1.0f);
                glRectf(0, 0, 640, 480);
                break;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new State();
    }
}
