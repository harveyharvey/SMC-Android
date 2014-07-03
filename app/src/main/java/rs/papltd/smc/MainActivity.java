package rs.papltd.smc;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import android.view.View;

/**
 * Created by pedja on 2/27/14.
 */
public class MainActivity extends AndroidApplication
{
    MaryoGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		View decorView = getWindow().getDecorView(); 
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;
        //config.useGL20 = true;
        game = new MaryoGame();
        initialize(game, config);
    }

    @Override
    public void onBackPressed()
    {
        if(game.onBackPressed())
        {
            super.onBackPressed();
        }
    }
}
