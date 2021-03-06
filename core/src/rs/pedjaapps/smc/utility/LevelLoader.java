package rs.pedjaapps.smc.utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

import rs.pedjaapps.smc.assets.Assets;
import rs.pedjaapps.smc.object.Box;
import rs.pedjaapps.smc.object.GameObject;
import rs.pedjaapps.smc.object.Level;
import rs.pedjaapps.smc.object.LevelEntry;
import rs.pedjaapps.smc.object.LevelExit;
import rs.pedjaapps.smc.object.MovingPlatform;
import rs.pedjaapps.smc.object.Sprite;
import rs.pedjaapps.smc.object.World;
import rs.pedjaapps.smc.object.enemy.Enemy;
import rs.pedjaapps.smc.object.enemy.EnemyStopper;
import rs.pedjaapps.smc.object.items.Item;
import rs.pedjaapps.smc.object.maryo.Maryo;
import rs.pedjaapps.smc.view.Background;

import static rs.pedjaapps.smc.view.Background.BG_GR_HOR;
import static rs.pedjaapps.smc.view.Background.BG_GR_VER;
import static rs.pedjaapps.smc.view.Background.BG_IMG_ALL;
import static rs.pedjaapps.smc.view.Background.BG_IMG_BOTTOM;
import static rs.pedjaapps.smc.view.Background.BG_IMG_TOP;

/**
 * Created by pedja on 2/2/14.
 */

/**
 * This class loads level from json
 */
public class LevelLoader
{
    public static final Pattern TXT_NAME_IN_ATLAS = Pattern.compile(".+\\.pack:.+");
    public Level level;
    private boolean levelParsed = false;

    private enum ObjectClass
    {
        sprite, item, box, player, enemy, moving_platform, enemy_stopper, level_entry, level_exit,
    }

    public static final float m_pos_z_passive_start = 0.01f;
    private static final float m_pos_z_massive_start = 0.08f;
    private static final float m_pos_z_front_passive_start = 0.1f;
    private static final float m_pos_z_halfmassive_start = 0.04f;

    /**
     * Use this constructor only from pc when you want to automatically fix assets dependencies
     */
    public LevelLoader(String levelName)
    {
        level = new Level(levelName);
    }

    public synchronized void parseLevel(World world)
    {
        JSONObject jLevel;
        try
        {
            jLevel = new JSONObject(Gdx.files.internal("data/levels/" + level.levelName + Level.LEVEL_EXT).readString());
            parseInfo(jLevel, world.screen.game.assets);
            parseParticleEffect(jLevel, world.screen.game.assets);
            parseBg(jLevel, world.screen.game.assets);
            parseGameObjects(world, jLevel, world.screen.game.assets);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Unable to load level! " + e.getMessage());
        }
        levelParsed = true;
    }

    private void parseParticleEffect(JSONObject jLevel, Assets assets)
    {
        JSONObject jParticleEffect = jLevel.optJSONObject("particle_effect");
        if(jParticleEffect != null)
        {
            String effect = jParticleEffect.optString("effect");
            if(!TextUtils.isEmpty(effect))
            {
                assets.manager.load(effect, ParticleEffect.class, assets.particleEffectParameter);
                level.particleEffect = effect;
            }
        }
    }

    private void parseGameObjects(World world, JSONObject level, Assets assets) throws JSONException
    {
        JSONArray jObjects = level.getJSONArray("objects");
        for (int i = 0; i < jObjects.length(); i++)
        {
            JSONObject jObject = jObjects.getJSONObject(i);
            switch (ObjectClass.valueOf(jObject.getString("obj_class")))
            {
                case sprite:
                    parseSprite(world, jObject, assets);
                    break;
                case player:
                    parsePlayer(jObject, world);
                    break;
                case item:
                    parseItem(world, jObject, assets);
                    break;
                case enemy:
                    parseEnemy(world, jObject, assets);
                    break;
                case enemy_stopper:
                    parseEnemyStopper(world, jObject);
                    break;
                case box:
                    parseBox(world, jObject, assets);
                    break;
                case level_entry:
                    /*{"direction":"up","posy":-228,"name":"1","posx":8074,"type":1,"obj_class":"level_entry"}*/
                    parseLevelEntry(world, jObject);
                    break;
                case level_exit:
                    parseLevelExit(world, jObject);
                    break;
                case moving_platform:
                    parseMovingPlatform(world, jObject, assets);
                    break;
            }
        }
        //this.level.gameObjects.sort(new ZSpriteComparator());
        Collections.sort(this.level.gameObjects, new ZSpriteComparator());
    }

    private void parseInfo(JSONObject jLevel, Assets assets) throws JSONException
    {
        JSONObject jInfo = jLevel.getJSONObject("info");
        float width = (float) jInfo.getDouble("level_width");
        float height = (float) jInfo.getDouble("level_height");
        level.width = width;
        level.height = height;
        if (jInfo.has("level_music"))
        {
            JSONArray jMusic = jInfo.getJSONArray("level_music");
            Array<String> music = new Array<>();
            for (int i = 0; i < jMusic.length(); i++)
            {
                String tmp = jMusic.getString(i);
                assets.manager.load(tmp, Music.class);
                if (!levelParsed) music.add(jMusic.getString(i));
            }
            if (!levelParsed) level.music = music;
        }
    }

    private void parseBg(JSONObject jLevel, Assets assets) throws JSONException
    {
        JSONArray jBgs = jLevel.optJSONArray("backgrounds");
        if (jBgs != null)
        {
            for (int i = 0; i < jBgs.length(); i++)
            {
                JSONObject jBg = jBgs.optJSONObject(i);

                int type = jBg.optInt("type");
                if (type == BG_IMG_ALL || type == BG_IMG_BOTTOM || type == BG_IMG_TOP)
                {
                    String textureName = jBg.optString("texture_name", null);
                    if (textureName != null)
                        assets.manager.load(textureName, Texture.class, assets.textureParameter);
                    if (levelParsed) return;

                    Vector2 speed = new Vector2();

                    speed.x = (float) jBg.optDouble("speedx");
                    speed.y = (float) jBg.optDouble("speedy");

                    Vector2 pos = new Vector2();

                    pos.x = (float) jBg.optDouble("posx");
                    pos.y = (float) jBg.optDouble("posy");

                    float width = (float) jBg.optDouble("width");
                    float height = (float) jBg.optDouble("height");

                    Background bg = new Background(pos, speed, textureName, width, height, level.width, level.height, type);

                    bg.width = (float) jBg.optDouble("width");
                    bg.height = (float) jBg.optDouble("height");
                    level.backgrounds.add(bg);
                }
                else if (type == BG_GR_VER || type == BG_GR_HOR)
                {
                    Background bg = new Background(type);
                    float r1 = (float) jBg.getDouble("r_1") / 255;//convert from 0-255 range to 0-1 range
                    float r2 = (float) jBg.getDouble("r_2") / 255;
                    float g1 = (float) jBg.getDouble("g_1") / 255;
                    float g2 = (float) jBg.getDouble("g_2") / 255;
                    float b1 = (float) jBg.getDouble("b_1") / 255;
                    float b2 = (float) jBg.getDouble("b_2") / 255;

                    Color color1 = new Color(r1, g1, b1, 0f);//color is 0-1 range where 1 = 255
                    Color color2 = new Color(r2, g2, b2, 0f);

                    bg.setColors(color1, color2);
                    level.backgrounds.add(bg);
                }
            }
        }
    }

    private void parsePlayer(JSONObject jPlayer, World world) throws JSONException
    {
        if (levelParsed) return;
        float x = (float) jPlayer.getDouble("posx");
        float y = (float) jPlayer.getDouble("posy");
        level.spanPosition = new Vector3(x, y, Maryo.POSITION_Z);
        Maryo maryo = new Maryo(world, level.spanPosition, new Vector2(0.9f, 0.9f));
        world.maryo = maryo;
        level.gameObjects.add(maryo);
    }

    private void parseSprite(World world, JSONObject jSprite, Assets assets) throws JSONException
    {
        Vector3 position = new Vector3((float) jSprite.getDouble("posx"), (float) jSprite.getDouble("posy"), 0);
        Sprite.Type sType = null;
        if (jSprite.has("massive_type"))
        {
            sType = Sprite.Type.valueOf(jSprite.getString("massive_type"));
            switch (sType)
            {
                case massive:
                    position.z = m_pos_z_massive_start;
                    break;
                case passive:
                    position.z = m_pos_z_passive_start;
                    break;
                case halfmassive:
                    position.z = m_pos_z_halfmassive_start;
                    break;
                case front_passive:
                    position.z = m_pos_z_front_passive_start;
                    break;
                case climbable:
                    position.z = m_pos_z_halfmassive_start;
                    break;
            }
        }
        else
        {
            position.z = m_pos_z_front_passive_start;
        }
        Vector2 size = new Vector2((float) jSprite.getDouble("width"), (float) jSprite.getDouble("height"));

        Rectangle rectangle = new Rectangle();
        rectangle.x = (float) jSprite.optDouble("c_posx", 0);
        rectangle.y = (float) jSprite.optDouble("c_posy", 0);
        rectangle.width = (float) jSprite.optDouble("c_width", size.x);
        rectangle.height = (float) jSprite.optDouble("c_height", size.y);
        Sprite sprite = new Sprite(world, size, position, rectangle);
        sprite.type = sType;
        sprite.groundType = jSprite.optInt("ground_type", Sprite.GROUND_NORMAL);

        sprite.textureName = jSprite.getString("texture_name");
        sprite.textureAtlas = jSprite.optString("texture_atlas", null);

        if (TextUtils.isEmpty(sprite.textureName) && TextUtils.isEmpty(sprite.textureAtlas))
        {
            throw new GdxRuntimeException("Both textureName and textureAtlas are null");
        }

        if (TextUtils.isEmpty(sprite.textureName))
        {
            throw new IllegalArgumentException("texture name is invalid: \"" + sprite.textureName + "\"");
        }

        if (!TXT_NAME_IN_ATLAS.matcher(sprite.textureName).matches())
        {
            assets.manager.load(sprite.textureName, Texture.class, assets.textureParameter);
        }

        if (!TextUtils.isEmpty(sprite.textureAtlas))
        {
            assets.manager.load(sprite.textureAtlas, TextureAtlas.class);
        }

        sprite.mRotationX = jSprite.optInt("rotationX");
        sprite.mRotationY = jSprite.optInt("rotationY");
        sprite.mRotationZ = jSprite.optInt("rotationZ");
        if (sprite.mRotationZ == 270)
        {
            sprite.mRotationZ = -sprite.mRotationZ;
        }
        if (!levelParsed) level.gameObjects.add(sprite);

    }

    private void parseEnemy(World world, JSONObject jEnemy, Assets assets) throws JSONException
    {
        Enemy enemy = Enemy.initEnemy(world, jEnemy);
        if (enemy == null) return;
        if (jEnemy.has("texture_atlas"))
        {
            enemy.textureAtlas = jEnemy.getString("texture_atlas");
            assets.manager.load(enemy.textureAtlas, TextureAtlas.class);
        }
        if (jEnemy.has("texture_name"))
        {
            enemy.textureName = jEnemy.getString("texture_name");
            assets.manager.load(enemy.textureName, Texture.class, assets.textureParameter);
        }
        if (!levelParsed) level.gameObjects.add(enemy);
    }

    private void parseEnemyStopper(World world, JSONObject jEnemyStopper) throws JSONException
    {
        if (levelParsed) return;
        Vector3 position = new Vector3((float) jEnemyStopper.getDouble("posx"), (float) jEnemyStopper.getDouble("posy"), 0);
        float width = (float) jEnemyStopper.getDouble("width");
        float height = (float) jEnemyStopper.getDouble("height");

        EnemyStopper stopper = new EnemyStopper(world, new Vector2(width, height), position);

        level.gameObjects.add(stopper);
    }

    private void parseLevelEntry(World world, JSONObject jEntry) throws JSONException
    {
        if (levelParsed) return;
        Vector3 position = new Vector3((float) jEntry.getDouble("posx"), (float) jEntry.getDouble("posy"), 0);
        float width = (float) jEntry.getDouble("width");
        float height = (float) jEntry.getDouble("height");

        LevelEntry entry = new LevelEntry(world, new Vector2(width, height), position);
        entry.direction = jEntry.optString("direction");
        entry.type = jEntry.optInt("type");
        entry.name = jEntry.optString("name");

        level.gameObjects.add(entry);
    }

    private void parseLevelExit(World world, JSONObject jExit) throws JSONException
    {
        if (levelParsed) return;
        Vector3 position = new Vector3((float) jExit.getDouble("posx"), (float) jExit.getDouble("posy"), 0);
        float width = (float) jExit.getDouble("width");
        float height = (float) jExit.getDouble("height");
        LevelExit exit = new LevelExit(world, new Vector2(width, height), position, jExit.optInt("type"), jExit.optString("direction"));
        exit.cameraMotion = jExit.optInt("camera_motion");
        exit.levelName = jExit.optString("level_name", null);
        exit.entry = jExit.optString("entry");

        level.gameObjects.add(exit);
    }

    private void parseMovingPlatform(World world, JSONObject jMovingPlatform, Assets assets) throws JSONException
    {
        if (levelParsed) return;
        Vector3 position = new Vector3((float) jMovingPlatform.getDouble("posx"), (float) jMovingPlatform.getDouble("posy"), 0);
        float width = (float) jMovingPlatform.getDouble("width");
        float height = (float) jMovingPlatform.getDouble("height");
        MovingPlatform platform = new MovingPlatform(world, new Vector2(width, height), position, null);
        platform.max_distance = jMovingPlatform.optInt("max_distance");
        platform.speed = (float) jMovingPlatform.optDouble("speed");
        platform.touch_time = (float) jMovingPlatform.optDouble("touch_time");
        platform.shake_time = (float) jMovingPlatform.optDouble("shake_time");
        platform.touch_move_time = (float) jMovingPlatform.optDouble("touch_move_time");
        platform.move_type = jMovingPlatform.optInt("move_type");
        platform.middle_img_count = jMovingPlatform.optInt("middle_img_count");
        platform.direction = jMovingPlatform.optString("direction");
        platform.image_top_left = jMovingPlatform.optString("image_top_left");
        platform.image_top_middle = jMovingPlatform.optString("image_top_middle");
        platform.image_top_right = jMovingPlatform.optString("image_top_right");
        platform.textureAtlas = jMovingPlatform.optString("texture_atlas");
        if (platform.textureAtlas != null && !platform.textureAtlas.trim().isEmpty())
        {
            assets.manager.load(platform.textureAtlas, TextureAtlas.class);
        }
        else
        {
            assets.manager.load(platform.image_top_left, Texture.class);
            assets.manager.load(platform.image_top_middle, Texture.class);
            assets.manager.load(platform.image_top_right, Texture.class);
        }

        Sprite.Type sType = null;
        if (jMovingPlatform.has("massive_type"))
        {
            sType = Sprite.Type.valueOf(jMovingPlatform.getString("massive_type"));
            switch (sType)
            {
                case massive:
                    position.z = m_pos_z_massive_start;
                    break;
                case passive:
                    position.z = m_pos_z_passive_start;
                    break;
                case halfmassive:
                    position.z = m_pos_z_halfmassive_start;
                    break;
                case front_passive:
                    position.z = m_pos_z_front_passive_start;
                    break;
                case climbable:
                    position.z = m_pos_z_halfmassive_start;
                    break;
            }
        }
        else
        {
            position.z = m_pos_z_front_passive_start;
        }
        platform.type = sType;

        JSONObject jPath = jMovingPlatform.optJSONObject("path");
        if (platform.move_type == MovingPlatform.MOVING_PLATFORM_TYPE_PATH && jPath == null)
        {
            throw new GdxRuntimeException("MovingPlatform type is 'path' but no path defined");
        }
        if (jPath != null)
        {
            MovingPlatform.Path path = new MovingPlatform.Path();
            path.posx = (float) jPath.optDouble("posx");
            path.posy = (float) jPath.optDouble("posy");
            path.rewind = jPath.optInt("rewind");

            JSONArray jSegments = jPath.optJSONArray("segments");
            if (jSegments == null || jSegments.length() == 0)
            {
                throw new GdxRuntimeException("Path doesn't contain segments. Level: " + level.levelName);
            }
            for (int i = 0; i < jSegments.length(); i++)
            {
                JSONObject jSegment = jSegments.optJSONObject(i);
                MovingPlatform.Path.Segment segment = new MovingPlatform.Path.Segment();
                segment.start.x = (float) jSegment.optDouble("startx");
                segment.start.y = (float) jSegment.optDouble("starty");
                segment.end.x = (float) jSegment.optDouble("endx");
                segment.end.y = (float) jSegment.optDouble("endy");
                path.segments.add(segment);
            }
            platform.path = path;
        }

        level.gameObjects.add(platform);
    }

    private void parseItem(World world, JSONObject jItem, Assets assets) throws JSONException
    {
        Vector3 position = new Vector3((float) jItem.getDouble("posx"), (float) jItem.getDouble("posy"), 0);

        Item item = Item.createObject(world, assets, jItem.optInt("mushroom_type"), jItem.getString("type"), new Vector2((float) jItem.getDouble("width"), (float) jItem.getDouble("height")), position);
        if (item == null) return;
        if (jItem.has("texture_atlas"))
        {
            item.textureAtlas = jItem.getString("texture_atlas");
            assets.manager.load(item.textureAtlas, TextureAtlas.class);
        }
        if (!levelParsed) level.gameObjects.add(item);
    }

    private void parseBox(World world, JSONObject jBox, Assets assets) throws JSONException
    {
        Box box = Box.initBox(world, jBox, assets);
        if (!levelParsed) level.gameObjects.add(box);
    }

    /**
     * Comparator used for sorting, sorts in ascending order (biggset z to smallest z).
     *
     * @author mzechner
     */
    public static class ZSpriteComparator implements Comparator<GameObject>
    {
        @Override
        public int compare(GameObject sprite1, GameObject sprite2)
        {
            if (sprite1.position.z > sprite2.position.z) return 1;
            if (sprite1.position.z < sprite2.position.z) return -1;
            return 0;
        }
    }

}
