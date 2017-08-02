/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture3d;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import static org.lwjgl.opengl.ARBTextureRg.GL_R16;
import static org.lwjgl.opengl.ARBTextureRg.GL_R16F;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SHORT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

/*
 * @author LAA
 */
public class Texture {
    private int width, height, depth, target, id;

    public Texture(int width, int height, int depth, int colorFormat, ByteBuffer buff) throws Exception {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.target = GL_TEXTURE_3D;
        this.id = GL11.glGenTextures();
        
        // bind this texture 
        GL11.glBindTexture(target, id);

        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

       // glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, image.getWidth() + 1, image.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, image.getByeBuff());
       switch (colorFormat){
           case GL_RGB8:
               //glTexImage3D();
               glTexImage3D(target, 0, colorFormat, width, height, depth, 0, GL_RGB, GL_UNSIGNED_BYTE, buff);
               break;
           case GL_RGBA16F:
               glTexImage3D(target, 0, colorFormat, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, buff);
               break;
           case GL_R16F:
               glTexImage3D(target, 0, colorFormat, width, height, depth, 0, GL_RED, GL_SHORT, buff);
               break;
           case GL_R16:
               glTexImage3D(target, 0, colorFormat, width, height, depth, 0, GL_RED, GL_SHORT, buff);
               break;
           default:
               throw new Exception("Unrecognized texture color format: " + colorFormat);
       }
    }

    public void bind() {
        GL11.glBindTexture(target, id);
    }

    public int getTarget() {
        return target;
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
    
    public int getDepth(){
        return depth;
    }
}
