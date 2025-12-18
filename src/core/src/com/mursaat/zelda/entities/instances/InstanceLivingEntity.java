package com.mursaat.zelda.entities.instances;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mursaat.zelda.entities.LivingEntity;
import com.mursaat.zelda.entities.Orientation;
import com.mursaat.zelda.items.Item;
import com.mursaat.zelda.map.Chunk;
import com.mursaat.zelda.map.Map;
import com.mursaat.zelda.tiles.Tile;
import com.mursaat.zelda.world.World;

import java.util.HashMap;

/**
 * @author Mursaat
 * Une InstanceEntity spécifique aux LivingEntities
 */
public class InstanceLivingEntity extends InstanceEntity
{
    // La quantité de points de vie de l'entité
    protected float life;
    // Définit si l'entité est vivante ou non
    protected boolean alive;
    // Le vecteur de déplacement à suivre après avoir été frappé
    protected Vector2 damagedVector;

    // Orientation
    protected Orientation orientation;

    // La frame actuellement affichée
    protected TextureRegion currentFrame;

    // Le temps pendant lequel s'est déroulé l'animation courante
    protected float animationTime;

    // Si l'entité est poussée, le temps restant
    protected float remainingPushTime;
    // Le temps total de poussée de l'entité
    protected float totalPushTime;

    // Le temps de récupération de l'entité après avoir subit un coup
    public static float RECOVERY_TIME = 0.4f;
    // Le temps actuel de récuperation
    protected float remainingRecoveryTime;

    // Le temps restant avant de finir l'animation de mort
    public float timeBeforeDeath;

    // Si l'entité doit disparaitre
    public boolean mustBeRemoved = false;

    public InstanceLivingEntity(LivingEntity entity)
    {
        super(entity);
        this.orientation = Orientation.BOTTOM;
        currentFrame = entity.textureFrames[0][0];
        this.animationTime = 0;
        this.timeBeforeDeath = -1;
        this.alive = true;
        this.life = entity.getMaxLife();
    }

    public InstanceLivingEntity(LivingEntity entity, float x, float y)
    {
        super(entity, x, y);
        this.orientation = Orientation.BOTTOM;
        currentFrame = entity.textureFrames[0][0];
        this.animationTime = 0;
    }

    @Override
    public void draw(SpriteBatch batch, float deltaTime)
    {
        float xCentrePixel = Tile.TILE_SIZE * Chunk.CHUNK_TILE_SIZE * Map.MAP_CHUNK_SIZE / 2;
        float yCentrePixel = Tile.TILE_SIZE * Chunk.CHUNK_TILE_SIZE * Map.MAP_CHUNK_SIZE / 2;

        float xPixel = xCentrePixel + (getXRelativeTo(World.getHero()) * Tile.TILE_SIZE);
        float yPixel = yCentrePixel + (getYRelativeTo(World.getHero()) * Tile.TILE_SIZE);
        batch.draw(currentFrame, xPixel, yPixel);
    }

    public void hurt(float damage, InstanceEntity hitter, Item source)
    {
        life -= damage;
        if (life <= 0)
        {
            alive = false;
        }
    }

    public float getLife()
    {
        return life;
    }

    public boolean isAlive()
    {
        return alive;
    }

    public Rectangle getDamageBounds()
    {
        return ((LivingEntity)entity).getDamageBounds(x, y);
    }

    public void push(InstanceEntity pusher, float distance, float time)
    {
        remainingPushTime = time;
        totalPushTime = time;
        float distanceBetweenEntities = Vector2.dst(x, y, pusher.x, pusher.y);
        float ratio = distance/distanceBetweenEntities;
        damagedVector = new Vector2((x-pusher.x)*ratio, (y-pusher.y)*ratio);
    }

    @Override
    public void update()
    {

    }
}
