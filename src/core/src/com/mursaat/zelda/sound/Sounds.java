package com.mursaat.zelda.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

/**
 * Created by Aurelien on 22/12/2015.
 */
public class Sounds
{
    public static Sound heroHurt;
    public static Sound enemyHurt;
    public static Sound enemyDie;

    public static Sound swordSlash1;
    public static Sound swordSlash2;
    public static Sound swordSlash3;

    public static void registerSounds()
    {
        heroHurt = Gdx.audio.newSound(Gdx.files.internal("sounds/hero_hurt.ogg"));
        enemyHurt = Gdx.audio.newSound(Gdx.files.internal("sounds/enemy_hurt.ogg"));
        enemyDie = Gdx.audio.newSound(Gdx.files.internal("sounds/enemy_die.ogg"));

        swordSlash1 = Gdx.audio.newSound(Gdx.files.internal("sounds/sword_slash_1.ogg"));
        swordSlash2 = Gdx.audio.newSound(Gdx.files.internal("sounds/sword_slash_2.ogg"));
        swordSlash3 = Gdx.audio.newSound(Gdx.files.internal("sounds/sword_slash_3.ogg"));
    }
}
