package com.mursaat.zelda.save;

import com.badlogic.gdx.Application;
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

    /**
     * Get a FileHandle appropriate for the current platform.
     * Desktop: uses local() for read-write access
     * Web/GWT: returns null as file system operations are not supported
     */
    private static FileHandle getFileHandle(String path) {
        if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
            // Web platform doesn't support file system operations
            return null;
        }
        return Gdx.files.local(path);
    }

    /**
     * Check if save operations are supported on the current platform.
     */
    private static boolean isSaveSupported() {
        return Gdx.app.getType() != Application.ApplicationType.WebGL;
    }

    public static void loadOrCreateSave()
    {
        World.initHero();
        World.setCurrentMap(new Map());
        World.getCurrentMap().initMap();

        // Skip save operations on web platform
        if (!isSaveSupported()) {
            Gdx.app.log("Save", "Save operations not supported on web platform - starting new game");
            return;
        }

        FileHandle playerData = getFileHandle(saveName + "/player/player.data");
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
                        FileHandle chunkData = getFileHandle(filename);
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
        // Skip save operations on web platform
        if (!isSaveSupported()) {
            return;
        }

        String filename = saveName + "/world/" + chunk.x + "." + chunk.y + ".chunk";
        FileHandle chunkData = getFileHandle(filename);

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
        if (!isSaveSupported()) {
            return null;
        }

        String filename = saveName + "/world/" + chunkX + "." + chunkY + ".chunk";
        FileHandle chunkData = getFileHandle(filename);
        return chunkData.exists() ? chunkData : null;
    }
}
