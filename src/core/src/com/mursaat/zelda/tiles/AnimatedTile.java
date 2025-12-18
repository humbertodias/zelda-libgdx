package com.mursaat.zelda.tiles;


import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * @author Mursaat
 *         Tile animé, se baser sur le Tile 'fleurs' pour voir comment le contruire.
 */
public class AnimatedTile extends Tile
{
    protected TextureRegion[] frames;
    protected Animation<TextureRegion> animation;

    public AnimatedTile(String name, int id)
    {
        super(name, id);
        TextureRegion[][] tmp = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
        frames = new TextureRegion[tmp[0].length];
        for (int i = 0; i < tmp[0].length; i++)
        {
            frames[i] = tmp[0][i];
        }
        animation = new Animation<TextureRegion>(0.5f, frames);
    }

    /**
     * @param name          Le nom du fichier de texture
     * @param frameDuration La durée entre deux frames de l'animation en seconde
     * @param playMode      Le mode d'animation
     */
    public AnimatedTile(String name, int id, float frameDuration, Animation.PlayMode playMode)
    {
        super(name, id);
        TextureRegion[][] tmp = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
        frames = new TextureRegion[tmp[0].length];
        for (int i = 0; i < tmp[0].length; i++)
        {
            frames[i] = tmp[0][i];
        }
        animation = new Animation<TextureRegion>(0.5f, frames);
        animation.setPlayMode(playMode);
        animation.setFrameDuration(frameDuration);
    }

    // Définit le mode d'animation
    public AnimatedTile setAnimationMode(Animation.PlayMode playMode)
    {
        this.animation.setPlayMode(playMode);
        return this;
    }

    // Définit la durée entre deux frames
    public AnimatedTile setFrameDuration(float frameDuration)
    {
        this.animation.setFrameDuration(frameDuration);
        return this;
    }

    public TextureRegion getKeyFrame(float time)
    {
        return animation.getKeyFrame(time, true);
    }

    @Override
    public void draw(SpriteBatch batch, float time, int xPos, int yPos)
    {
        batch.draw(getKeyFrame(time), xPos, yPos);
    }
}
