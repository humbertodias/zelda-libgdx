package com.mursaat.zelda.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.mursaat.zelda.entities.instances.InstanceEntity;
import com.mursaat.zelda.map.Chunk;
import com.mursaat.zelda.map.Map;
import com.mursaat.zelda.structures.InstanceStructure;
import com.mursaat.zelda.world.World;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

        FileHandle playerData = Gdx.files.external(saveName + "/player/player.data");

        try
        {
            if (playerData.exists())
            {
                // Lire les données du joueur
                InputStream in = playerData.read();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line = reader.readLine();
                reader.close();

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
                        FileHandle chunkData = Gdx.files.external(filename);

                        if (chunkData.exists())
                        {
                            World.getCurrentMap().loadChunkFile(x, y, chunkData);
                        }
                        else
                        {
                            saveChunk(World.getCurrentMap().getChunk(x, y), false);
                        }
                    }
                }
            }
            else
            {
                // Créer la sauvegarde
                playerData.writeString("0 0", false);
            }
        }
        catch (Exception ex)
        {
            Gdx.app.error("Game", "Erreur sauvegarde", ex);
        }
    }

    public static void saveChunk(Chunk chunk, boolean destroyEntities)
    {
        String filename = saveName + "/world/" + chunk.x + "." + chunk.y + ".chunk";
        FileHandle chunkData = Gdx.files.external(filename);

        if (chunkData.exists())
        {
            chunkData.delete();
        }

        // -------- Tiles --------
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Chunk.CHUNK_TILE_SIZE; i++)
        {
            for (int j = 0; j < Chunk.CHUNK_TILE_SIZE; j++)
            {
                if (i > 0 || j > 0) sb.append(" ");
                sb.append(chunk.getTile(i, j).getId());
            }
        }
        chunkData.writeString(sb.append("\n").toString(), false);

        // -------- Structures --------
        sb.setLength(0);
        for (InstanceStructure structure : chunk.structures)
        {
            sb.append(structure.getStructureId()).append(" ")
                    .append(structure.x).append(" ")
                    .append(structure.y).append(" ");
        }
        chunkData.writeString(sb.append("\n").toString(), true);

        // -------- Entités --------
        sb.setLength(0);
        Iterator<InstanceEntity> it = World.getCurrentMap().entities.iterator();

        while (it.hasNext())
        {
            InstanceEntity entity = it.next();
            if (entity.getXChunk() == chunk.x && entity.getYChunk() == chunk.y)
            {
                sb.append(entity.getEntityId()).append(" ")
                        .append(entity.x).append(" ")
                        .append(entity.y).append(" ");

                if (destroyEntities)
                {
                    it.remove();
                }
            }
        }

        chunkData.writeString(sb.toString(), true);
    }

    public static FileHandle getChunkFile(int chunkX, int chunkY)
    {
        String filename = saveName + "/world/" + chunkX + "." + chunkY + ".chunk";
        FileHandle chunkData = Gdx.files.external(filename);
        return chunkData.exists() ? chunkData : null;
    }
}
