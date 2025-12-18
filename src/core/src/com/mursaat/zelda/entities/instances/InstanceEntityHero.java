package com.mursaat.zelda.entities.instances;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.mursaat.zelda.entities.Entities;
import com.mursaat.zelda.entities.EntityHero;
import com.mursaat.zelda.entities.Orientation;
import com.mursaat.zelda.items.Item;
import com.mursaat.zelda.items.ItemSword;
import com.mursaat.zelda.items.Items;
import com.mursaat.zelda.map.Map;
import com.mursaat.zelda.sound.Sounds;
import com.mursaat.zelda.structures.InstanceStructure;
import com.mursaat.zelda.tiles.Tile;
import com.mursaat.zelda.world.World;

import java.util.ArrayList;

/**
 * Created by Aurelien on 19/12/2015.
 */
public class InstanceEntityHero extends InstanceLivingEntity implements InputProcessor
{
    // Les positions de l'épée relative au héros (en Pixels)
    protected float swordPosX, swordPosY;

    // L'epee tenue
    ItemSword sword;

    // Le restant avant de finir l'animation de coup d'epee
    public float remainingSwordTime;
    public TextureRegion currentSwordFrame;

    // Définit si le héros est immobilisé
    public boolean immobilized;

    // Détermine si le héros brandit son épée ou non
    public boolean brandishingSword;
    // Détermine si le héros vient juste de finir de donner un coup d'épée (sans arrêter de la bandir)
    public boolean justEndSwordRotation;

    public boolean swordEnd;

    public InstanceEntityHero()
    {
        super(Entities.hero);
        this.remainingRecoveryTime = 0;
        this.remainingSwordTime = 0;
        this.sword = Items.sword;
        this.immobilized = false;
        this.brandishingSword = false;
        this.justEndSwordRotation = false;
    }

    @Override
    public void draw(SpriteBatch batch, float deltaTime)
    {
        if (brandishingSword || remainingSwordTime != 0)
        {
            batch.draw(currentSwordFrame, this.x * Tile.TILE_SIZE + swordPosX, this.y * Tile.TILE_SIZE + swordPosY);
        }
        batch.draw(currentFrame, this.x * Tile.TILE_SIZE, this.y * Tile.TILE_SIZE);
    }

    @Override
    public void update()
    {
        super.update();

        // Ici on met à jour le recovery time
        if (remainingRecoveryTime != 0)
        {
            remainingRecoveryTime -= Gdx.graphics.getDeltaTime();
            if (remainingRecoveryTime < 0)
            {
                remainingRecoveryTime = 0;
            }
        }

        // Gerer les inputs clavier de déplacement sauf si le héros est poussé ou si il est immobilisé
        remainingPushTime -= Gdx.graphics.getDeltaTime();
        if (remainingPushTime > 0)
        {
            float ratioTimeRemainingPushTime = Gdx.graphics.getDeltaTime() / totalPushTime;

            float oldX = this.x;
            float oldY = this.y;

            this.x += damagedVector.x * ratioTimeRemainingPushTime;
            this.y += damagedVector.y * ratioTimeRemainingPushTime;

            float newX = this.x;


            ArrayList<Rectangle> collisions = new ArrayList<Rectangle>();
            for (InstanceEntity entity : World.getCurrentMap().entities)
            {
                collisions.add(entity.getCollisionBounds());
            }

            int xChunk = getXChunk();
            int yChunk = getYChunk();
            for (int i = xChunk - 1; i <= xChunk + 1; i++)
            {
                for (int j = yChunk - 1; j <= yChunk + 1; j++)
                {
                    for (InstanceStructure structure : World.getCurrentMap().chunks.get(i).get(j).structures)
                    {
                        for (Rectangle rectangle : structure.collisions)
                        {
                            collisions.add(rectangle);
                        }
                    }
                }
            }

            // On gère maintenant les collisions
            for (Rectangle rectangle : collisions)
            {
                if (Intersector.overlaps(rectangle, getCollisionBounds()))
                {
                    // On teste si c'est la coordonnée x ou y ou les deux qui provoquent la collision
                    this.x = oldX;
                    if (Intersector.overlaps(rectangle, getCollisionBounds()))
                    {
                        this.x = newX;
                        this.y = oldY;
                        if (Intersector.overlaps(rectangle, getCollisionBounds()))
                        {
                            this.x = oldX;
                        }
                    }
                }
            }

            updateAnimation(orientation, false);
        }
        else if (!immobilized)
        {
            remainingPushTime = 0;
            handleMoveInputs();
        }

        // Gérer l'épée
        if (brandishingSword || remainingSwordTime != 0)
        {
            handleSwordAnimation();
        }
    }

    public InstanceEntity handleSwordRectangleHit()
    {
        Rectangle swordRectangle = null;
        switch (orientation)
        {
            case BOTTOM:
                swordRectangle = new Rectangle(x * Tile.TILE_SIZE + 9, y * Tile.TILE_SIZE - Tile.TILE_SIZE + 3, 4, sword.allonge);
                break;
            case TOP:
                swordRectangle = new Rectangle(x * Tile.TILE_SIZE + 2, y * Tile.TILE_SIZE + 5, 4, sword.allonge);
                break;
            case LEFT:
                swordRectangle = new Rectangle(x * Tile.TILE_SIZE - 10, y * Tile.TILE_SIZE + 2, sword.allonge, 4);
                break;
            case RIGHT:
                swordRectangle = new Rectangle(x * Tile.TILE_SIZE + 5, y * Tile.TILE_SIZE + 2, sword.allonge, 4);
                break;
        }

        InstanceEntity entityTouched = null;
        for (InstanceEntity entity : World.getCurrentMap().entities)
        {
            if (entity instanceof InstanceEntityHostileMonster && ((InstanceEntityHostileMonster) entity).alive)
            {
                Rectangle collisionEntity = ((InstanceEntityHostileMonster) entity).getDamageBounds();
                if (Intersector.overlaps(collisionEntity, swordRectangle))
                {
                    ((InstanceEntityHostileMonster) entity).hurt(2f, this, sword);
                    entityTouched = entity;
                }
            }
        }
        return entityTouched;
    }

    public void handleSwordArcHit()
    {
        Rectangle c = getCollisionBounds();
        Rectangle swordRect = new Rectangle();
        switch (orientation)
        {
            case BOTTOM:
                swordRect.set(c.x - sword.allonge / 2, c.y - sword.allonge, c.width + sword.allonge, sword.allonge + c.height / 2);
                break;
            case TOP:
                swordRect.set(c.x - sword.allonge / 2, c.y + c.height / 2, c.width + sword.allonge, sword.allonge + c.height / 2);
                break;
            case LEFT:
                swordRect.set(c.x - sword.allonge, c.y - sword.allonge / 2, sword.allonge + c.width / 2, c.height + sword.allonge);
                break;
            case RIGHT:
                swordRect.set(c.x + c.width / 2, c.y - sword.allonge / 2, sword.allonge + c.width / 2, c.height + sword.allonge);
                break;
        }

        for (InstanceEntity entity : World.getCurrentMap().entities)
        {
            if (entity instanceof InstanceEntityHostileMonster && ((InstanceEntityHostileMonster) entity).alive)
            {
                Rectangle r = ((InstanceEntityHostileMonster) entity).getDamageBounds();
                if (swordRect.overlaps(r))
                {
                    ((InstanceEntityHostileMonster) entity).hurt(2f, this, sword);
                }
            }
        }
    }

    public void handleSwordAnimation()
    {
        // Ici on met à jour le swipe time
        if (remainingSwordTime != 0)
        {
            remainingSwordTime -= Gdx.graphics.getDeltaTime();
        }
        if (remainingSwordTime <= 0)
        {
            if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) || swordEnd)
            {
                remainingSwordTime = 0;
                immobilized = false;
                brandishingSword = false;
                justEndSwordRotation = false;
                return;
            }

            // Ici on vérifie si on a touché quelqu'un
            InstanceEntity entityTouched = handleSwordRectangleHit();
            if (entityTouched != null)
            {
                remainingSwordTime = sword.slashTime / 3;
                swordEnd = true;
                immobilized = true;
            }

            if (justEndSwordRotation)
            {
                remainingSwordTime = 0;
                immobilized = false;
                switch (orientation)
                {
                    case BOTTOM:
                        swordPosX = -2;
                        swordPosY = -Tile.TILE_SIZE + 2;
                        break;
                    case TOP:
                        swordPosX = 1;
                        swordPosY = Tile.TILE_SIZE - 4;
                        break;
                    case LEFT:
                        swordPosX = -Tile.TILE_SIZE + 5;
                        swordPosY = 0;
                        break;
                    case RIGHT:
                        swordPosX = Tile.TILE_SIZE - 5;
                        swordPosY = 0;
                        break;
                }
                justEndSwordRotation = false;
            }
        }
        else
        {
            switch (orientation)
            {
                case BOTTOM:
                    if (remainingSwordTime > (2 * sword.slashTime / 3))
                    {
                        swordPosX = -Tile.TILE_SIZE + 3;
                        swordPosY = -1;
                        currentFrame = ((EntityHero) entity).animSwipeSwordBottom.getKeyFrames()[0];
                        currentSwordFrame = sword.animSlashBottom.getKeyFrames()[0];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordRectangleHit();
                    }
                    else if (remainingSwordTime > sword.slashTime / 3)
                    {
                        swordPosX = -Tile.TILE_SIZE + 5;
                        swordPosY = -Tile.TILE_SIZE + 2;
                        currentFrame = ((EntityHero) entity).animSwipeSwordBottom.getKeyFrames()[1];
                        currentSwordFrame = sword.animSlashBottom.getKeyFrames()[1];

                        // Ici on vérifie si on a touché quelqu'un

                    }
                    else
                    {
                        swordPosX = -1;
                        swordPosY = -Tile.TILE_SIZE + 1;
                        currentFrame = ((EntityHero) entity).animSwipeSwordBottom.getKeyFrames()[2];
                        currentSwordFrame = sword.animSlashBottom.getKeyFrames()[2];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordArcHit();
                    }
                    break;
                case TOP:
                    if (remainingSwordTime > (2 * sword.slashTime / 3))
                    {
                        swordPosX = Tile.TILE_SIZE - 1;
                        swordPosY = 3;
                        currentFrame = ((EntityHero) entity).animSwipeSwordTop.getKeyFrames()[0];
                        currentSwordFrame = sword.animSlashTop.getKeyFrames()[0];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordRectangleHit();
                    }
                    else if (remainingSwordTime > sword.slashTime / 3)
                    {
                        swordPosX = Tile.TILE_SIZE - 4;
                        swordPosY = Tile.TILE_SIZE - 2;
                        currentFrame = ((EntityHero) entity).animSwipeSwordTop.getKeyFrames()[1];
                        currentSwordFrame = sword.animSlashTop.getKeyFrames()[1];

                        // Ici on vérifie si on a touché quelqu'un
                    }
                    else
                    {
                        swordPosX = 1;
                        swordPosY = Tile.TILE_SIZE;
                        currentFrame = ((EntityHero) entity).animSwipeSwordTop.getKeyFrames()[2];
                        currentSwordFrame = sword.animSlashTop.getKeyFrames()[2];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordArcHit();
                    }
                    break;
                case LEFT:
                    if (remainingSwordTime > (2 * sword.slashTime / 3))
                    {
                        swordPosX = -2;
                        swordPosY = Tile.TILE_SIZE;
                        currentFrame = ((EntityHero) entity).animSwipeSwordLeft.getKeyFrames()[0];
                        currentSwordFrame = sword.animSlashLeft.getKeyFrames()[0];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordRectangleHit();
                    }
                    else if (remainingSwordTime > sword.slashTime / 3)
                    {
                        swordPosX = -Tile.TILE_SIZE + 3;
                        swordPosY = Tile.TILE_SIZE - 5;
                        currentFrame = ((EntityHero) entity).animSwipeSwordLeft.getKeyFrames()[1];
                        currentSwordFrame = sword.animSlashLeft.getKeyFrames()[1];

                        // Ici on vérifie si on a touché quelqu'un
                    }
                    else
                    {
                        swordPosX = -Tile.TILE_SIZE + 1;
                        swordPosY = 0;
                        currentFrame = ((EntityHero) entity).animSwipeSwordLeft.getKeyFrames()[2];
                        currentSwordFrame = sword.animSlashLeft.getKeyFrames()[2];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordArcHit();
                    }
                    break;
                case RIGHT:
                    if (remainingSwordTime > (2 * sword.slashTime / 3))
                    {
                        swordPosX = 2;
                        swordPosY = Tile.TILE_SIZE;
                        currentFrame = ((EntityHero) entity).animSwipeSwordRight.getKeyFrames()[0];
                        currentSwordFrame = sword.animSlashRight.getKeyFrames()[0];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordRectangleHit();
                    }
                    else if (remainingSwordTime > sword.slashTime / 3)
                    {
                        swordPosX = Tile.TILE_SIZE - 2;
                        swordPosY = Tile.TILE_SIZE - 5;
                        currentFrame = ((EntityHero) entity).animSwipeSwordRight.getKeyFrames()[1];
                        currentSwordFrame = sword.animSlashRight.getKeyFrames()[1];

                        // Ici on vérifie si on a touché quelqu'un
                    }
                    else
                    {
                        swordPosX = Tile.TILE_SIZE - 1;
                        swordPosY = 0;
                        currentFrame = ((EntityHero) entity).animSwipeSwordRight.getKeyFrames()[2];
                        currentSwordFrame = sword.animSlashRight.getKeyFrames()[2];

                        // Ici on vérifie si on a touché quelqu'un
                        handleSwordArcHit();
                    }
                    break;
            }
        }
    }

    public void stopMove()
    {
        this.animationTime = 0;
        updateAnimation(orientation, false);
    }

    public void handleMoveInputs()
    {
        int nbFleches = numberOfArrowKeysPressed();
        Orientation oldOrientation = orientation;
        Orientation directionUpdated = null;

        // On garde la position de départ au cas ou il y est collision
        float oldX = x;
        float oldY = y;

        // Si on a 0 ou au moins 3 touche d'appuyé, le personnage s'arrête
        if (nbFleches == 0 || nbFleches >= 3)
        {
            stopMove();
        }
        // Si on a une touche d'appuyé, le personnage se déplace dans une direction
        else if (nbFleches == 1)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
            {
                this.y -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed();
                directionUpdated = Orientation.BOTTOM;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.UP))
            {
                this.y += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed();
                directionUpdated = Orientation.TOP;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
            {
                this.x -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed();
                directionUpdated = Orientation.LEFT;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            {
                this.x += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed();
                directionUpdated = Orientation.RIGHT;
            }
        }
        // Si on a deux touches d'appuyé, le personnage se déplace en diagonale
        else if (nbFleches == 2)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.UP) && Gdx.input.isKeyPressed(Input.Keys.LEFT))
            {
                if (this.orientation == Orientation.BOTTOM || orientation == Orientation.RIGHT)
                {
                    orientation = Orientation.TOP;
                }
                this.y += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                this.x -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                directionUpdated = orientation;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.UP) && Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            {
                if (orientation == Orientation.BOTTOM || orientation == Orientation.LEFT)
                {
                    orientation = Orientation.TOP;
                }
                this.y += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                this.x += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                directionUpdated = orientation;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) && Gdx.input.isKeyPressed(Input.Keys.LEFT))
            {
                if (orientation == Orientation.TOP || orientation == Orientation.RIGHT)
                {
                    orientation = Orientation.BOTTOM;
                }
                this.y -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                this.x -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                directionUpdated = orientation;
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) && Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            {
                if (orientation == Orientation.TOP || orientation == Orientation.LEFT)
                {
                    orientation = Orientation.BOTTOM;
                }
                this.y -= Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                this.x += Gdx.graphics.getDeltaTime() * ((EntityHero) entity).getMoveSpeed() / 1.35;
                directionUpdated = orientation;
            }
            else
            {
                stopMove();
            }
        }

        // On gère maintenant les collisions avec les entités
        boolean hasVerticalCollision = false;
        boolean hasHorizontalCollison = false;

        ArrayList<Rectangle> collisions = new ArrayList<Rectangle>();
        for (InstanceEntity entity : World.getCurrentMap().entities)
        {
            collisions.add(entity.getCollisionBounds());
        }

        int xChunk = getXChunk();
        int yChunk = getYChunk();
        for (int i = xChunk - 1; i <= xChunk + 1; i++)
        {
            for (int j = yChunk - 1; j <= yChunk + 1; j++)
            {
                for (InstanceStructure structure : World.getCurrentMap().chunks.get(i).get(j).structures)
                {
                    for (Rectangle rectangle : structure.collisions)
                    {
                        collisions.add(rectangle);
                    }
                }
            }
        }

        float newY = this.y;
        // Les collisions avec les objets placés sur le chunk
        for (Rectangle object : collisions)
        {
            if (Intersector.overlaps(object, getCollisionBounds()))
            {
                // On teste si c'est la coordonnée x ou y ou les deux qui provoquent la collision
                this.y = oldY;
                if (Intersector.overlaps(object, getCollisionBounds()))
                {
                    this.y = newY;
                    this.x = oldX;
                    if (Intersector.overlaps(object, getCollisionBounds()))
                    {
                        this.x = oldX;
                        hasHorizontalCollison = true;
                        hasVerticalCollision = true;
                    }
                    else
                    {
                        hasHorizontalCollison = true;
                    }
                }
                else
                {
                    hasVerticalCollision = true;
                }
            }
        }

        if (directionUpdated != null)
        {
            // On affiche la direction de poussée que si la collision se produit dans le même sens que la direction d'avancement du personnage
            boolean isPushing = false;
            if (((directionUpdated == Orientation.LEFT || directionUpdated == Orientation.RIGHT) && hasHorizontalCollison)
                    || ((directionUpdated == Orientation.TOP || directionUpdated == Orientation.BOTTOM) && hasVerticalCollision))
            {
                isPushing = true;
            }
            if (brandishingSword)
            {
                isPushing = false;
                this.orientation = oldOrientation;
                directionUpdated = oldOrientation;
            }
            updateAnimation(directionUpdated, isPushing);
        }
    }

    public void updateAnimation(Orientation orientation, boolean isPushing)
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
            if (isPushing)
            {
                updatePushingDamagedAnimation();
            }
            else
            {
                updateMoveDamagedAnimation();
            }
        }
        // Sinon on affiche l'animation normale
        else
        {
            if (isPushing)
            {
                updatePushingAnimation();
            }
            else
            {
                updateMoveAnimation();
            }
        }
    }

    public void updatePushingAnimation()
    {
        switch (orientation)
        {
            case BOTTOM:
                currentFrame = ((EntityHero) entity).animPushBottom.getKeyFrame(animationTime);
                break;
            case TOP:
                currentFrame = ((EntityHero) entity).animPushTop.getKeyFrame(animationTime);
                break;
            case LEFT:
                currentFrame = ((EntityHero) entity).animPushLeft.getKeyFrame(animationTime);
                break;
            case RIGHT:
                currentFrame = ((EntityHero) entity).animPushRight.getKeyFrame(animationTime);
                break;
        }
    }

    public void updatePushingDamagedAnimation()
    {
        switch (orientation)
        {
            case BOTTOM:
                currentFrame = ((EntityHero) entity).animPushBottomDamaged.getKeyFrame(animationTime);
                break;
            case TOP:
                currentFrame = ((EntityHero) entity).animPushTopDamaged.getKeyFrame(animationTime);
                break;
            case LEFT:
                currentFrame = ((EntityHero) entity).animPushLeftDamaged.getKeyFrame(animationTime);
                break;
            case RIGHT:
                currentFrame = ((EntityHero) entity).animPushRightDamaged.getKeyFrame(animationTime);
                break;
        }
    }

    public void updateMoveAnimation()
    {
        switch (orientation)
        {
            case BOTTOM:
                currentFrame = ((EntityHero) entity).animMoveBottom.getKeyFrame(animationTime);
                break;
            case TOP:
                currentFrame = ((EntityHero) entity).animMoveTop.getKeyFrame(animationTime);
                break;
            case LEFT:
                currentFrame = ((EntityHero) entity).animMoveLeft.getKeyFrame(animationTime);
                break;
            case RIGHT:
                currentFrame = ((EntityHero) entity).animMoveRight.getKeyFrame(animationTime);
                break;
        }
    }

    public void updateMoveDamagedAnimation()
    {
        switch (orientation)
        {
            case BOTTOM:
                currentFrame = ((EntityHero) entity).animMoveBottomDamaged.getKeyFrame(animationTime);
                break;
            case TOP:
                currentFrame = ((EntityHero) entity).animMoveTopDamaged.getKeyFrame(animationTime);
                break;
            case LEFT:
                currentFrame = ((EntityHero) entity).animMoveLeftDamaged.getKeyFrame(animationTime);
                break;
            case RIGHT:
                currentFrame = ((EntityHero) entity).animMoveRightDamaged.getKeyFrame(animationTime);
                break;
        }
    }

    public int numberOfArrowKeysPressed()
    {
        int nbFleches = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
        {
            nbFleches++;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP))
        {
            nbFleches++;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
        {
            nbFleches++;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
        {
            nbFleches++;
        }
        return nbFleches;
    }

    @Override
    public void hurt(float damage, InstanceEntity hitter, Item source)
    {
        // Si on est pas en recovery time on peut appliquer les dégats
        if (remainingRecoveryTime == 0)
        {
            if (hitter instanceof InstanceEntityHostileMonster)
            {
                // Pousser le heros
                World.getHero().push(hitter, 2f, 0.2f);
            }

            life -= damage;
            if (life <= 0)
            {
                alive = false;
            }
            // Si il est toujours vivant, on affiche l'animation de dégats subit et on démarre le recovery time
            else
            {
                remainingRecoveryTime = RECOVERY_TIME;
                Sounds.heroHurt.play();
            }
        }
    }

    @Override
    public boolean keyDown(int keycode)
    {
        return false;
    }

    @Override
    public boolean keyUp(int keycode)
    {
        return false;
    }

    @Override
    public boolean keyTyped(char character)
    {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        if (button == Input.Buttons.LEFT && remainingSwordTime == 0)
        {
            remainingSwordTime = sword.slashTime;
            immobilized = true;
            swordEnd = false;
            brandishingSword = true;
            justEndSwordRotation = true;
            sword.playRandomSlashSound();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY)
    {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY)
    {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }
}
