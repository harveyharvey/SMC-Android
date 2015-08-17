package rs.pedjaapps.smc.utility;

import com.badlogic.gdx.Gdx;

/**
 * Created by pedja on 2/15/14.
 */
public class Constants
{
    public static final String DEFAULT_FONT_FILE_NAME = "data/fonts/Roboto-Regular.ttf";
    public static final String DEFAULT_FONT_BOLD_FILE_NAME = "data/fonts/Roboto-Bold.ttf";


    public static float CAMERA_WIDTH/* = 10f*/;
    public static float MENU_CAMERA_WIDTH/* = 10f*/;

    public static final float MENU_CAMERA_HEIGHT = 7f;
    public static final float CAMERA_HEIGHT = 9f;

    public static float ASPECT_RATIO;

    public static final float MENU_DRAW_WIDTH = 12.444444444f;
    public static final float DRAW_WIDTH = 16f;

    public static final float BACKGROUND_SCROLL_SPEED = 0.12f;
    public static final int GRAVITY = -20;
    static
    {
        initCamera();
    }

    public static void initCamera()
    {
        ASPECT_RATIO = (float)Gdx.graphics.getWidth()/(float)Gdx.graphics.getHeight();
        CAMERA_WIDTH = CAMERA_HEIGHT * ASPECT_RATIO;
        MENU_CAMERA_WIDTH = MENU_CAMERA_HEIGHT * ASPECT_RATIO;
    }
}
