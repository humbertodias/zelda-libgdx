package com.mursaat.zelda.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mursaat.zelda.entities.Entities;
import com.mursaat.zelda.entities.EntityMoblin;
import com.mursaat.zelda.entities.Orientation;
import com.mursaat.zelda.entities.instances.*;
import com.mursaat.zelda.save.Save;
import com.mursaat.zelda.structures.Structures;
import com.mursaat.zelda.tiles.Tiles;
import com.mursaat.zelda.tiles.tileentities.TileEntity;
import com.mursaat.zelda.util.Pair;
import com.mursaat.zelda.world.World;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Mursaat
 *         La carte qui va pouvoir être affiché dans le GameScreen
 *         Il stocke la portion du monde actuellement affichée
 */
public class Map
{
    // La taille d'un côté de la map en chunks
    public static final int MAP_CHUNK_SIZE = 5;
    // La taille d'un côté de la map - 1 / 2
    public static final int MAP_CHUNK_SIZE_HALF = (MAP_CHUNK_SIZE - 1) / 2;

    // La position x actuelle du chunk central de la map
    public int xChunk;
    // La position y actuelle du chunk central de la map
    public int yChunk;

    // Les entités présentes sur la map
    public ArrayList<InstanceEntity> entities;

    // Les chunks chargés de la map
    public HashMap<Integer, HashMap<Integer, Chunk>> chunks;
    /* Les TileEntities chargées associées à la map. L'HashMap contient en clé les coordonnées
       du TileEntity. */
    private HashMap<Pair<Integer, Integer>, TileEntity> tileEntities;

    public Map()
    {
        entities = new ArrayList<InstanceEntity>();
    }

    public void initMap()
    {
        xChunk = World.getHero().getXChunk();
        yChunk = World.getHero().getYChunk();

        chunks = new HashMap<Integer, HashMap<Integer, Chunk>>();

        for (int i = xChunk - MAP_CHUNK_SIZE_HALF; i <= xChunk + MAP_CHUNK_SIZE_HALF; i++)
        {
            chunks.put(i, new HashMap<Integer, Chunk>());
            for (int j = yChunk - MAP_CHUNK_SIZE_HALF; j <= yChunk + MAP_CHUNK_SIZE_HALF; j++)
            {
                Chunk chunk = new Chunk(i, j);
                FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                if (savedChunk != null)
                {
                    // Charger le chunk depuis le fichier
                    loadChunkFile(chunk, savedChunk);
                }
                chunks.get(i).put(j, chunk);
            }
        }


        /*
        // Test ajout de rochers
        entities.add(new InstanceEntityRock(2, 2));
        entities.add(new InstanceEntityRock(2, 3));
        entities.add(new InstanceEntityRock(2, 4));
        entities.add(new InstanceEntityRock(2, 5));

        // Test ajout des moblins
        entities.add(new InstanceEntityMoblin(-4f, -5f, EntityMoblin.MoblinType.BLUE));
        entities.add(new InstanceEntityMoblin(5f, 7f, EntityMoblin.MoblinType.BLUE));
        entities.add(new InstanceEntityMoblin(-5f, 7f, EntityMoblin.MoblinType.RED));

        // Test ajout de structures
        Structures.red_house.place(this, 0, 0, 3, 3);
        Structures.tree.place(this, 0, 0, 8, 8);
        Structures.tree.place(this, 0, 0, 6, 6);
        Structures.tree.place(this, 0, 0, 6, 3);
        Structures.tree.place(this, -1, 0, 4, 4);
        Structures.tree.place(this, -1, 0, 9, 6);
        Structures.tree.place(this, -1, 0, 6, 4);
        */

        for (int i = xChunk - MAP_CHUNK_SIZE_HALF; i <= xChunk + MAP_CHUNK_SIZE_HALF; i++)
        {
            for (int j = yChunk - MAP_CHUNK_SIZE_HALF; j <= yChunk + MAP_CHUNK_SIZE_HALF; j++)
            {
                Chunk chunk = chunks.get(i).get(j);
                FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                if (savedChunk == null)
                {
                    // Sauvegarder le chunk
                    chunk.initRandomStructures();
                    chunk.placeMonstersRandom();
                    Save.saveChunk(chunk, false);
                }
            }
        }


    }

    public void draw(SpriteBatch batch, float deltaTime)
    {
        // On regarde si le joueur a dépassé une certaine limite
        updateChunksArray();

        // On dessine d'abord tous les tiles
        for (java.util.Map.Entry<Integer, HashMap<Integer, Chunk>> entryX : chunks.entrySet())
        {
            HashMap<Integer, Chunk> map = entryX.getValue();
            for (java.util.Map.Entry<Integer, Chunk> entryY : map.entrySet())
            {
                Chunk chunk = entryY.getValue();
                chunk.drawTiles(batch, deltaTime, chunk.x, chunk.y);
            }
        }
        // Par dessus on dessine les structures
        for (java.util.Map.Entry<Integer, HashMap<Integer, Chunk>> entryX : chunks.entrySet())
        {
            HashMap<Integer, Chunk> map = entryX.getValue();
            for (java.util.Map.Entry<Integer, Chunk> entryY : map.entrySet())
            {
                Chunk chunk = entryY.getValue();
                chunk.drawStructures(batch);
            }
        }

        // Dessin des entités
        for (InstanceEntity entity : entities)
        {
            entity.update();
            entity.draw(batch, deltaTime);
        }

        // Si une créature doit disparaitre on l'enleve
        for(Iterator<InstanceEntity> it = entities.iterator(); it.hasNext();)
        {
            InstanceEntity entity = it.next();
            if (entity instanceof InstanceLivingEntity && ((InstanceLivingEntity) entity).mustBeRemoved)
            {
                entities.remove(entity);
            }
        }
    }

    public synchronized void updateChunksArray()
    {
        final int xChunkHero = World.getHero().getXChunk();
        final int yChunkHero = World.getHero().getYChunk();
        if (xChunkHero != xChunk || yChunkHero != yChunk)
        {
            if (xChunkHero < xChunk)
            {
                xChunk = xChunkHero;
                loadChunks(Orientation.LEFT);
            }
            if (xChunkHero > xChunk)
            {
                xChunk = xChunkHero;
                loadChunks(Orientation.RIGHT);
            }
            if (yChunkHero < yChunk)
            {
                yChunk = yChunkHero;
                loadChunks(Orientation.BOTTOM);
            }
            if (yChunkHero > yChunk)
            {
                yChunk = yChunkHero;
                loadChunks(Orientation.TOP);
            }
        }
    }

    /**
     * Permet de charger des chunks et de decaler les autres dans le tableau des chunks
     *
     * @param direction Définit de quel côté le joueur de déplace
     */
    public void loadChunks(Orientation direction)
    {
        switch (direction)
        {
            case BOTTOM:
                for (int i = xChunk - MAP_CHUNK_SIZE_HALF; i <= xChunk + MAP_CHUNK_SIZE_HALF; i++)
                {
                    if (!chunks.containsKey(i))
                    {
                        chunks.put(i, new HashMap<Integer, Chunk>());
                    }
                    if (!chunks.get(i).containsKey(yChunk - MAP_CHUNK_SIZE_HALF))
                    {
                        Chunk chunk = new Chunk(i, yChunk - MAP_CHUNK_SIZE_HALF);
                        chunks.get(i).put(yChunk - MAP_CHUNK_SIZE_HALF, chunk);
                        FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                        if (savedChunk == null)
                        {
                            // Initialiser le chunk et le sauvegarder
                            chunk.initRandomStructures();
                            chunk.placeMonstersRandom();
                            Save.saveChunk(chunk, false);
                        }
                        else
                        {
                            // Charger le chunk depuis le fichier
                            loadChunkFile(chunk, savedChunk);
                        }
                    }
                    if (chunks.get(i).containsKey(yChunk + MAP_CHUNK_SIZE_HALF + 1))
                    {
                        Save.saveChunk(chunks.get(i).get(yChunk + MAP_CHUNK_SIZE_HALF + 1), true);
                        chunks.get(i).remove(yChunk + MAP_CHUNK_SIZE_HALF + 1);
                    }
                }
                break;
            case TOP:
                for (int i = xChunk - MAP_CHUNK_SIZE_HALF; i <= xChunk + MAP_CHUNK_SIZE_HALF; i++)
                {
                    if (chunks.get(i) == null)
                    {
                        chunks.put(i, new HashMap<Integer, Chunk>());
                    }
                    if (!chunks.get(i).containsKey(yChunk + MAP_CHUNK_SIZE_HALF))
                    {
                        Chunk chunk = new Chunk(i, yChunk + MAP_CHUNK_SIZE_HALF);
                        chunks.get(i).put(yChunk + MAP_CHUNK_SIZE_HALF, chunk);

                        FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                        if (savedChunk == null)
                        {
                            // Initialiser le chunk et le sauvegarder
                            chunk.initRandomStructures();
                            chunk.placeMonstersRandom();
                            Save.saveChunk(chunk, false);
                        }
                        else
                        {
                            // Charger le chunk depuis le fichier
                            loadChunkFile(chunk, savedChunk);
                        }
                    }
                    if (chunks.get(i).containsKey(yChunk - MAP_CHUNK_SIZE_HALF - 1))
                    {
                        Save.saveChunk(chunks.get(i).get(yChunk - MAP_CHUNK_SIZE_HALF - 1), true);
                        chunks.get(i).remove(yChunk - MAP_CHUNK_SIZE_HALF - 1);
                    }
                }
                break;
            case LEFT:
                if (!chunks.containsKey(xChunk - MAP_CHUNK_SIZE_HALF))
                {
                    chunks.put(xChunk - MAP_CHUNK_SIZE_HALF, new HashMap<Integer, Chunk>());
                }
                for (int j = yChunk - MAP_CHUNK_SIZE_HALF; j <= yChunk + MAP_CHUNK_SIZE_HALF; j++)
                {
                    if (!chunks.get(xChunk - MAP_CHUNK_SIZE_HALF).containsKey(j))
                    {
                        Chunk chunk = new Chunk(xChunk - MAP_CHUNK_SIZE_HALF, j);
                        chunks.get(xChunk - MAP_CHUNK_SIZE_HALF).put(j, chunk);

                        FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                        if (savedChunk == null)
                        {
                            // Initialiser le chunk et le sauvegarder
                            chunk.initRandomStructures();
                            chunk.placeMonstersRandom();
                            Save.saveChunk(chunk, false);
                        }
                        else
                        {
                            // Charger le chunk depuis le fichier
                            loadChunkFile(chunk, savedChunk);
                        }
                    }
                    if (chunks.containsKey(xChunk + MAP_CHUNK_SIZE_HALF + 1) && chunks.get(xChunk + MAP_CHUNK_SIZE_HALF + 1).containsKey(j))
                    {
                        Save.saveChunk(chunks.get(xChunk + MAP_CHUNK_SIZE_HALF + 1).get(j), true);
                        chunks.get(xChunk + MAP_CHUNK_SIZE_HALF + 1).remove(j);
                    }
                }
                break;
            case RIGHT:
                if (!chunks.containsKey(xChunk + MAP_CHUNK_SIZE_HALF))
                {
                    chunks.put(xChunk + MAP_CHUNK_SIZE_HALF, new HashMap<Integer, Chunk>());
                }
                for (int j = yChunk - MAP_CHUNK_SIZE_HALF; j <= yChunk + MAP_CHUNK_SIZE_HALF; j++)
                {
                    if (!chunks.get(xChunk + MAP_CHUNK_SIZE_HALF).containsKey(j))
                    {
                        Chunk chunk = new Chunk(xChunk + MAP_CHUNK_SIZE_HALF, j);
                        chunks.get(xChunk + MAP_CHUNK_SIZE_HALF).put(j, chunk);

                        FileHandle savedChunk = Save.getChunkFile(chunk.x, chunk.y);

                        if (savedChunk == null)
                        {
                            // Initialiser le chunk et le sauvegarder
                            chunk.initRandomStructures();
                            chunk.placeMonstersRandom();
                            Save.saveChunk(chunk, false);
                        }
                        else
                        {
                            // Charger le chunk depuis le fichier
                            loadChunkFile(chunk, savedChunk);
                        }
                    }
                    if (chunks.containsKey(xChunk - MAP_CHUNK_SIZE_HALF - 1) && chunks.get(xChunk - MAP_CHUNK_SIZE_HALF - 1).containsKey(j))
                    {
                        Save.saveChunk(chunks.get(xChunk - MAP_CHUNK_SIZE_HALF - 1).get(j), true);
                        chunks.get(xChunk - MAP_CHUNK_SIZE_HALF - 1).remove(j);
                    }
                }
                break;
        }
    }

    /**
     * Charge un chunk en mémoire depuis un fichier
     *
     * @param file Le fichier à lire
     */
    public void loadChunkFile(Chunk chunk, FileHandle file)
    {
        InputStream in = file.read();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try
        {
            // Charger les tiles
            line = reader.readLine();
            String[] lineArray = line.split(" ");
            int currentArrayId = 0;
            for (int i = 0; i < Chunk.CHUNK_TILE_SIZE; i++)
            {
                for (int j = 0; j < Chunk.CHUNK_TILE_SIZE; j++)
                {
                    chunk.setTile(i, j, Tiles.getTile(Integer.parseInt(lineArray[currentArrayId++])));
                }
            }

            // Charger les rectangles de collision
            line = reader.readLine();
            if(line != null && !line.isEmpty())
            {
                lineArray = line.split(" ");
                for (int i = 0; i < lineArray.length; i += 3)
                {
                    int id = Integer.parseInt(lineArray[i]);
                    float x = Float.parseFloat(lineArray[i + 1]);
                    float y = Float.parseFloat(lineArray[i + 2]);
                    chunk.structures.add(Structures.getStructure(id).newInstance(x,y));
                }
            }

            // Charger les entités
            line = reader.readLine();
            if(line != null && !line.isEmpty())
            {
                lineArray = line.split(" ");
                for (int i = 0; i < lineArray.length; i += 3)
                {
                    int id = Integer.parseInt(lineArray[i]);
                    float x = Float.parseFloat(lineArray[i + 1]);
                    float y = Float.parseFloat(lineArray[i + 2]);
                    World.getCurrentMap().entities.add(Entities.getEntity(id).newInstance(x, y));
                }
            }
        }
        catch (Exception ex)
        {
            Gdx.app.error("Game", ex.toString());
        }
    }

    public void loadChunkFile(int x, int y, FileHandle file)
    {
        /*
        loadChunkFile(chunks[x - xChunk + 1][y - yChunk + 1], file);
        */
    }

    public Chunk getChunk(int x, int y)
    {
        /*
        return chunks[x - xChunk + 1][y - yChunk + 1];
        */
        return null;
    }

    /**
     * Retourne la TileEntity associée aux coordonnées passées en paramètres.
     * Renvoie null si il n'y a pas de TileEntity correspondante.
     *
     * @param x La coordonnée x en Tiles
     * @param y La coordonnée y en Tiles
     * @return La TileEntity associée aux coordonées
     */
    public TileEntity getTileEntity(int x, int y)
    {
        /*
        return tileEntities.get(new Pair<Integer, Integer>(x, y));
        */
        return null;
    }
}
