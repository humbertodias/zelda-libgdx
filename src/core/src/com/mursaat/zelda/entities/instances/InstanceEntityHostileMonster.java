package com.mursaat.zelda.entities.instances;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mursaat.zelda.entities.*;
import com.mursaat.zelda.items.Item;
import com.mursaat.zelda.items.ItemSword;
import com.mursaat.zelda.map.Chunk;
import com.mursaat.zelda.sound.Sounds;
import com.mursaat.zelda.structures.InstanceStructure;
import com.mursaat.zelda.tiles.Tile;
import com.mursaat.zelda.world.World;

import java.util.HashMap;

/**
 * Created by Aurelien on 23/12/2015.
 */
public abstract class InstanceEntityHostileMonster extends InstanceLivingEntity
{
    // La durée d'un déplacement avec l'AI passive
    private static final float ANIM_TIME = 1.5f;
    // Le temps restant avant d'effectuer un nouveau mouvement
    private float timeBeforeNextMove;

    private static final float PASSIVE_SPEED_MODIFICATOR = 0.7f;
    // Le temps de récupération de l'entité après avoir subit un coup
    public static float RECOVERY_TIME = 0.2f;

    public enum State
    {
        PASSIVE,
        AGGRESSIVE,
    }

    private State currentState;

    public InstanceEntityHostileMonster(float x, float y)
    {
        super(Entities.blueMoblin);
        this.x = x;
        this.y = y;
        this.timeBeforeNextMove = 0f;
        this.timeBeforeDeath = -1f;
        this.currentState = State.PASSIVE;
    }

    @Override
    public void draw(SpriteBatch batch, float deltaTime)
    {
        batch.draw(currentFrame, this.x * Tile.TILE_SIZE, this.y * Tile.TILE_SIZE);
    }

    /**
     * A chaque tick on redefinit l'AI du monstre
     */
    @Override
    public void update()
    {
        super.update();

        // Si le monstre est sur le point de mourir
        if (!alive)
        {
            updateDeath();
        }
        else
        {
            // Si la distance Mob-Héros est supérieure à 16 (dst2 = distance au carré, plus rapide de pas faire la racine)
            if (Vector2.dst2(x, y, World.getHero().x, World.getHero().y) > 40f)
            {
                this.updateMovePassif();
            }
            else
            {
                this.updateMoveAgressif();
            }
        }

        // Ici on met à jour le recovery time
        if (remainingRecoveryTime != 0)
        {
            remainingRecoveryTime -= Gdx.graphics.getDeltaTime();
            if (remainingRecoveryTime < 0)
            {
                remainingRecoveryTime = 0;
            }
        }

        // Si le monstre est poussé, on le déplace
        remainingPushTime -= Gdx.graphics.getDeltaTime();
        if (remainingPushTime <= 0)
        {
            remainingPushTime = 0;
        }
        else
        {
            handlePush();
        }

        // Teste si on cogne le héros
        if (alive && World.getHero().getDamageBounds().overlaps(getDamageBounds()))
        {
            // Si le héros n'est plus en recovery time
            if (World.getHero().remainingRecoveryTime == 0)
            {
                // Infliger des dégats
                World.getHero().hurt(((LivingEntity) entity).getDamage(), this, null);
            }
        }
    }

    public void updateDeath()
    {
        /*
        if (remainingPushTime == totalPushTime)
        {

            System.out.println((float)(((EntityHostileMonster) entity).deathAnimation.getKeyFrames()[0].getRegionWidth() - EntityHostileMonster.regionWidth) / (2 * Tile.TILE_SIZE));
        }
        */
        if (timeBeforeDeath > 0)
        {
            timeBeforeDeath -= Gdx.graphics.getDeltaTime();
            if (timeBeforeDeath < 0)
            {
                timeBeforeDeath = 0;
                mustBeRemoved = true;
            }
            else
            {
                currentFrame = ((EntityHostileMonster) entity).deathAnimation.getKeyFrame(timeBeforeDeath);
            }
        }
        else if (remainingPushTime == 0)
        {
            timeBeforeDeath = ((EntityHostileMonster) entity).deathAnimation.getAnimationDuration();

            x -= (float) (((EntityHostileMonster) entity).deathAnimation.getKeyFrames()[0].getRegionWidth() - EntityHostileMonster.regionWidth) / (2 * Tile.TILE_SIZE);
            y -= (float) (((EntityHostileMonster) entity).deathAnimation.getKeyFrames()[0].getRegionHeight() - EntityHostileMonster.regionHeight) / (2 * Tile.TILE_SIZE);
        }
        if (remainingPushTime == totalPushTime)
        {
            Sounds.enemyDie.play();
        }
    }

    @Override
    public void hurt(float damage, InstanceEntity hitter, Item source)
    {
        // Si on est pas en recovery time on peut appliquer les dégats
        if (remainingRecoveryTime == 0)
        {
            if (source instanceof ItemSword)
            {
                this.push(hitter, 2f, 0.1f);
            }
            life -= damage;
            remainingRecoveryTime = RECOVERY_TIME;
            if (life <= 0)
            {
                alive = false;
            }
            // Si il est toujours vivant, on affiche l'animation de dégats subit et on démarre le recovery time
            else
            {
                Sounds.enemyHurt.play();
            }
        }
    }

    private void handlePush()
    {
        float ratioTimeRemainingPushTime = Gdx.graphics.getDeltaTime() / totalPushTime;

        float oldX = this.x;
        float oldY = this.y;

        this.x += damagedVector.x * ratioTimeRemainingPushTime;
        this.y += damagedVector.y * ratioTimeRemainingPushTime;

        // On gère maintenant les collisions
        handleCollisions(oldX, oldY, this.x, this.y);
    }

    private void updateMovePassif()
    {
        float oldX = this.x;
        float oldY = this.y;

        if (currentState != State.PASSIVE)
        {
            timeBeforeNextMove = 0;
            currentState = State.PASSIVE;
        }
        if (timeBeforeNextMove == 0f)
        {
            orientation = Orientation.randomOrientation();
            timeBeforeNextMove = ANIM_TIME;
        }

        final float delta = Gdx.graphics.getDeltaTime();
        float elapsedTime;
        if (timeBeforeNextMove < delta)
        {
            elapsedTime = timeBeforeNextMove;
            timeBeforeNextMove = 0f;
        }
        else
        {
            elapsedTime = delta;
            timeBeforeNextMove -= delta;
        }

        if (this.orientation == Orientation.TOP)
        {
            y += elapsedTime * ((LivingEntity) entity).getMoveSpeed() * PASSIVE_SPEED_MODIFICATOR;
        }
        else if (this.orientation == Orientation.BOTTOM)
        {
            y -= elapsedTime * ((LivingEntity) entity).getMoveSpeed() * PASSIVE_SPEED_MODIFICATOR;
        }
        else if (this.orientation == Orientation.LEFT)
        {
            x -= elapsedTime * ((LivingEntity) entity).getMoveSpeed() * PASSIVE_SPEED_MODIFICATOR;
        }
        else if (this.orientation == Orientation.RIGHT)
        {
            x += elapsedTime * ((LivingEntity) entity).getMoveSpeed() * PASSIVE_SPEED_MODIFICATOR;
        }
        updateAnimation(this.orientation);

        // On gère maintenant les collisions, si on en a une on change de sens
        if (handleCollisions(oldX, oldY, this.x, this.y))
        {
            timeBeforeNextMove = 0f;
        }
    }

    public void updateAnimation(Orientation orientation)
    {
        if (this.orientation != orientation)
        {
            animationTime = 0;
        }
        else
        {
            animationTime += Gdx.graphics.getDeltaTime();
        }
        this.orientation = orientation;

        // Si l'entité est en recovery time et que on doit afficher l'animation des dégats (on switch toutes les 0.05 secondes)
        if (remainingRecoveryTime != 0 && ((int) (remainingRecoveryTime * 20)) % 2 == 0)
        {
            switch (orientation)
            {
                case BOTTOM:
                    currentFrame = ((EntityHostileMonster) entity).animMoveBottomDamaged.getKeyFrame(animationTime);
                    break;
                case TOP:
                    currentFrame = ((EntityHostileMonster) entity).animMoveTopDamaged.getKeyFrame(animationTime);
                    break;
                case LEFT:
                    currentFrame = ((EntityHostileMonster) entity).animMoveLeftDamaged.getKeyFrame(animationTime);
                    break;
                case RIGHT:
                    currentFrame = ((EntityHostileMonster) entity).animMoveRightDamaged.getKeyFrame(animationTime);
                    break;
            }
        }
        else
        {
            switch (orientation)
            {
                case BOTTOM:
                    currentFrame = ((EntityHostileMonster) entity).animMoveBottom.getKeyFrame(animationTime);
                    break;
                case TOP:
                    currentFrame = ((EntityHostileMonster) entity).animMoveTop.getKeyFrame(animationTime);
                    break;
                case LEFT:
                    currentFrame = ((EntityHostileMonster) entity).animMoveLeft.getKeyFrame(animationTime);
                    break;
                case RIGHT:
                    currentFrame = ((EntityHostileMonster) entity).animMoveRight.getKeyFrame(animationTime);
                    break;
            }
        }
    }

    private void updateMoveAgressif()
    {
        currentState = State.AGGRESSIVE;
        timeBeforeNextMove = 0f;

        float oldX = x;
        float oldY = y;

        // On veut que le mob se déplace vers le héros
        // On calcule de combien le mob doit se déplacer
        float dist = Gdx.graphics.getDeltaTime() * ((LivingEntity) entity).getMoveSpeed();

        // On prend la distance mob-héros
        float distToHero = Vector2.dst(x, y, World.getHero().x, World.getHero().y);

        // On fait le rapport des deux distances
        float rapport = distToHero / dist;

        // On fait la différence des position x et y
        Vector2 diff = new Vector2(x - World.getHero().x, y - World.getHero().y);

        // On applique ce rapport sur la différence
        x -= diff.x / rapport;
        y -= diff.y / rapport;

        if (Math.abs(diff.x) > Math.abs(diff.y))
        {
            if (diff.x > 0)
            {
                this.updateAnimation(Orientation.LEFT);
            }
            else
            {
                this.updateAnimation(Orientation.RIGHT);
            }
        }
        else
        {
            if (diff.y > 0)
            {
                this.updateAnimation(Orientation.BOTTOM);
            }
            else
            {
                this.updateAnimation(Orientation.TOP);
            }
        }

        // On gère maintenant les collisions
        handleCollisions(oldX, oldY, this.x, this.y);
    }

    public boolean handleCollisions(float oldX, float oldY, float newX, float newY)
    {
        boolean collision = false;
        handleCollisionWithEntity(World.getHero(), oldX, oldY, newX, newY);
        for (InstanceEntity entity : World.getCurrentMap().entities)
        {
            if (handleCollisionWithEntity(entity, oldX, oldY, newX, newY))
            {
                collision = true;
            }
        }

        int xChunk = getXChunk();
        int yChunk = getYChunk();
        for (int i = xChunk - 1; i <= xChunk + 1; i++)
        {
            for (int j = yChunk - 1; j <= yChunk + 1; j++)
            {
                HashMap<Integer, Chunk> map = World.getCurrentMap().chunks.get(i);
                if (map != null)
                {
                    Chunk chunk = map.get(j);
                    if (chunk != null)
                    {
                        for (InstanceStructure structure : chunk.structures)
                        {
                            for (Rectangle rectangle : structure.collisions)
                            {
                                if (handleCollisionWithRectangle(rectangle, oldX, oldY, newX, newY))
                                {
                                    collision = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return collision;
    }

    public boolean handleCollisionWithEntity(InstanceEntity entity, float oldX, float oldY, float newX, float newY)
    {
        boolean collision = false;
        if (entity != this && Intersector.overlaps(entity.getCollisionBounds(), getCollisionBounds()))
        {
            collision = true;
            // On teste si c'est la coordonnée x ou y ou les deux qui provoquent la collision
            this.y = newY;
            this.x = oldX;
            if (Intersector.overlaps(entity.getCollisionBounds(), getCollisionBounds()))
            {
                this.x = newX;
                this.y = oldY;
                if (Intersector.overlaps(entity.getCollisionBounds(), getCollisionBounds()))
                {
                    this.x = oldX;
                    this.y = oldY;
                }
            }
        }
        return collision;
    }

    public boolean handleCollisionWithRectangle(Rectangle rectangle, float oldX, float oldY, float newX, float newY)
    {
        boolean collision = false;
        if (Intersector.overlaps(rectangle, getCollisionBounds()))
        {
            collision = true;
            // On teste si c'est la coordonnée x ou y ou les deux qui provoquent la collision
            this.y = newY;
            this.x = oldX;
            if (Intersector.overlaps(rectangle, getCollisionBounds()))
            {
                this.x = newX;
                this.y = oldY;
                if (Intersector.overlaps(rectangle, getCollisionBounds()))
                {
                    this.x = oldX;
                    this.y = oldY;
                }
            }
        }
        return collision;
    }
}
