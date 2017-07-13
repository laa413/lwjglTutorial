/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/*
 * @author labramson
 */
public class ObjectModel {
    private int drawCount;
    private int vertexId;
    private int textureId;
    
    public ObjectModel(float[] vertices, float[] texCoords){
        drawCount = vertices.length/2;
        vertexId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexId);
        glBufferData(GL_ARRAY_BUFFER, createFloatBuff(vertices), GL_STATIC_DRAW);
        
        textureId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, textureId);
        glBufferData(GL_ARRAY_BUFFER, createFloatBuff(texCoords), GL_STATIC_DRAW);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public void renderVBO(){
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER, vertexId);
        glVertexPointer(2, GL_FLOAT, 0, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, textureId);
        glTexCoordPointer(2, GL_FLOAT, 0, 0);
        
        glDrawArrays(GL_TRIANGLES, 0, drawCount);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
    }
    
    private FloatBuffer createFloatBuff(float[] data){
        FloatBuffer vFloatBuff = BufferUtils.createFloatBuffer(data.length);
        vFloatBuff.put(data);
        vFloatBuff.flip();
        return vFloatBuff;
    }
}
