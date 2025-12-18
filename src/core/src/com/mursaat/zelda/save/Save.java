package com.mursaat.zelda.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.mursaat.zelda.entities.instances.InstanceEntity;
import com.mursaat.zelda.entities.instances.InstanceLivingEntity;
import com.mursaat.zelda.map.Chunk;
import com.mursaat.zelda.map.Map;
import com.mursaat.zelda.structures.InstanceStructure;
import com.mursaat.zelda.world.World;

import java.io.*;
import java.util.Iterator;

/**
 * Created by Aurelien on 19/12/2015.
 */
public class Save
{
    public static String saveName = "save";

    public static void loadOrCreateSave()
    {
        World.initHero();
        World.setCurrentMap(new Map());
        World.getCurrentMap().initMap();

        FileHandle playerData = Gdx.files.local(saveName + "/player/player.data");
        try
        {
            if (playerData.exists())
            {
                // Charger la partie

                // Lire le fichier des données du joueur
                InputStream in = playerData.read();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                line = reader.readLine();
                String[] coords = line.split(" ");
                World.getHero().x = Integer.parseInt(coords[0]);
                World.getHero().y = Integer.parseInt(coords[1]);

                int xChunk = World.getHero().getXChunk();
                int yChunk = World.getHero().getYChunk();
                for (int x = xChunk - 1; x <= xChunk + 1; x++)
                {
                    for (int y = yChunk - 1; y <= yChunk + 1; y++)
                    {
                        String filename = saveName + "/world/" + x + "." + y + ".chunk";
                        FileHandle chunkData = Gdx.files.local(filename);
                        if (chunkData.exists())
                        {
                            // Si le fichier chunk existe, on le charge en mémoire
                            World.getCurrentMap().loadChunkFile(x + xChunk, y + yChunk, chunkData);
                        }
                        else
                        {
                            saveChunk(World.getCurrentMap().getChunk(x + xChunk, y + yChunk), false);
                        }
                    }
                }
            }
            else
            {
                // Créer la partie
                playerData.writeString("0 0", false);
            }
        }
        catch (Exception ex)
        {
            Gdx.app.error("Game", ex.toString());
        }
    }

    public static void saveChunk(Chunk chunk, boolean destroyEntities)
    {
        String filename = saveName + "/world/" + chunk.x + "." + chunk.y + ".chunk";
        FileHandle chunkData = Gdx.files.local(filename);
        // Créer le fichier
        try
        {
            if (chunkData.exists())
            {
                chunkData.delete();
            }
        }
        catch (Exception ex)
        {
            Gdx.app.error("Game", ex.toString());
        }

        // Ecrire dans le fichier
        // Sur la premiere ligne on met les ids des Tiles
        String chaine = "";
        for (int i = 0; i < Chunk.CHUNK_TILE_SIZE; i++)
        {
            for (int j = 0; j < Chunk.CHUNK_TILE_SIZE; j++)
            {
                if (i > 0 || j > 0)
                {
                    chaine += " ";
                }
                chaine += chunk.getTile(i, j).getId();
            }
        }
        chunkData.writeString(chaine + "\n", false);

        // Sur la deuxieme ligne on met les structures
        chaine = "";
        for (InstanceStructure structure : chunk.structures)
        {
            chaine += structure.getStructureId() + " " + structure.x + " " + structure.y + " ";
        }
        chunkData.writeString(chaine + "\n", true);

        // Sur la troisième ligne on met les entités
        chaine = "";
        for(Iterator<InstanceEntity> it = World.getCurrentMap().entities.iterator(); it.hasNext();)
        {
            InstanceEntity entity = it.next();
            if (entity.getXChunk() == chunk.x && entity.getYChunk() == chunk.y)
            {
                chaine += entity.getEntityId() + " " + entity.x + " " + entity.y + " ";
                if(destroyEntities)
                {
                    World.getCurrentMap().entities.remove(entity);
                }
            }
        }
        chunkData.writeString(chaine, true);
    }

    public static FileHandle getChunkFile(int chunkX, int chunkY)
    {
        String filename = saveName + "/world/" + chunkX + "." + chunkY + ".chunk";
        FileHandle chunkData = Gdx.files.local(filename);
        return chunkData.exists() ? chunkData : null;
    }
}
