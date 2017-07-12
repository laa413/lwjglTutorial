/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial.clTexture;

import org.lwjgl.opengl.GL11;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
//"C:\\Users\\labramson\\Documents\\Tutorial\\src\\tutorial\\smileTexture.png"
public class Texture {

    public int target, textureID, height, width, texWidth, texHeight;
    private float widthRatio, heightRatio;

    public Texture(int target, int textureID) {

        this.target = target;
        this.textureID = textureID;
    }

    public void bind() {

        GL11.glBindTexture(target, textureID);
    }

    public void setWidth(int width) {

        this.width = width;
        setWidth();
    }

    public void setHeight(int height) {

        this.height = height;
        setHeight();
    }

    public int getImageWidth() {

        return width;
    }

    public int getImageHeight() {

        return height;
    }

    public float getWidth() {

        return widthRatio;
    }

    public float getHeight() {

        return heightRatio;
    }

    public void setTextureWidth(int texWidth) {

        this.texWidth = texWidth;
        setWidth();
    }

    public void setTextureHeight(int texHeight) {

        this.texHeight = texHeight;
        setHeight();
    }

    private void setWidth() {

        if (texWidth != 0) {
            widthRatio = ((float) width) / texWidth;
        }
    }

    private void setHeight() {

        if (texHeight != 0) {
            heightRatio = ((float) height) / texHeight;
        }
    }
}
