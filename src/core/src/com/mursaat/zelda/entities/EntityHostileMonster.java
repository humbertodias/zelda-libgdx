package com.mursaat.zelda.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.mursaat.zelda.animations.Animations;
import com.mursaat.zelda.entities.instances.InstanceEntity;
import com.mursaat.zelda.entities.instances.InstanceEntityHostileMonster;
import com.mursaat.zelda.tiles.Tile;

/**
 * Created by Aurelien on 23/12/2015.
 */
public class EntityHostileMonster extends LivingEntity
{
    public static final int regionHeight = 16;
    public static final int regionWidth = 16;

    public Animation<TextureRegion> deathAnimation;

    public EntityHostileMonster(String textureName, String name, int id, int maxLife, float damage, float moveSpeed)
    {
        super(textureName, name, id, maxLife, damage, moveSpeed);
        this.textureFrames = TextureRegion.split(texture, regionWidth, regionHeight);

        makeAnimations();
    }

    protected void makeAnimations()
    {
        // MOVE
        this.animMoveBottom = new Animation<TextureRegion>(0.1f, this.textureFrames[0][0], this.textureFrames[0][1]);
        this.animMoveBottom.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveTop = new Animation<TextureRegion>(0.1f, this.textureFrames[1][0], this.textureFrames[1][1]);
        this.animMoveTop.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveLeft = new Animation<TextureRegion>(0.1f, this.textureFrames[2][0], this.textureFrames[2][1]);
        this.animMoveLeft.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveRight = new Animation<TextureRegion>(0.1f, this.textureFrames[3][0], this.textureFrames[3][1]);
        this.animMoveRight.setPlayMode(Animation.PlayMode.LOOP);

        // MOVE DAMAGED
        this.animMoveBottomDamaged = new Animation<TextureRegion>(0.1f, this.textureFrames[0][2], this.textureFrames[0][3]);
        this.animMoveBottomDamaged.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveTopDamaged = new Animation<TextureRegion>(0.1f, this.textureFrames[1][2], this.textureFrames[1][3]);
        this.animMoveTopDamaged.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveLeftDamaged = new Animation<TextureRegion>(0.1f, this.textureFrames[2][2], this.textureFrames[2][3]);
        this.animMoveLeftDamaged.setPlayMode(Animation.PlayMode.LOOP);

        this.animMoveRightDamaged = new Animation<TextureRegion>(0.1f, this.textureFrames[3][2], this.textureFrames[3][3]);
        this.animMoveRightDamaged.setPlayMode(Animation.PlayMode.LOOP);

        // DEAD ANIMATION
        this.deathAnimation = Animations.monsterDeathAnimation;
    }

    @Override
    public Rectangle getCollisionBounds(float posX, float posY)
    {
        return new Rectangle(posX * Tile.TILE_SIZE + 3, posY * Tile.TILE_SIZE + 3, 11, 11);
    }

    /**
     * Définit la hitbox du monstre
     */
    public Rectangle getDamageBounds(float posX, float posY)
    {
        return new Rectangle(posX * Tile.TILE_SIZE + 1, posY * Tile.TILE_SIZE + 1, 14, 14);
    }
}
