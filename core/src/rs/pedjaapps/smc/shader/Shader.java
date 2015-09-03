package rs.pedjaapps.smc.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by pedja on 28.8.15..
 */
public class Shader
{
    public static ShaderProgram FREEZE_SHADER = new ShaderProgram(Gdx.files.internal("data/shaders/freeze.vert"), Gdx.files.internal("data/shaders/freeze.frag"));
    public static ShaderProgram NORMAL_BLEND_SHADER = new ShaderProgram(Gdx.files.internal("data/shaders/normal_blend.vert"), Gdx.files.internal("data/shaders/normal_blend.frag"));

    static
    {
        ShaderProgram.pedantic = false;
    }
}