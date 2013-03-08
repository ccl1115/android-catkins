package com.simon.catkins.demo.app.image;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import com.simon.catkins.demo.R;
import com.simon.catkins.views.imagebrowser.ImageSurfaceView;
import de.akquinet.android.androlog.Log;

/**
 * @author bb.simon.yu@gmail.com
 */
public class ImageSurfaceViewActivity extends Activity {
  private static final String TAG = "ImageSurfaceViewActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "@onCreate");

    ImageSurfaceView imageSurfaceView = new ImageSurfaceView(this);

    imageSurfaceView.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

    setContentView(imageSurfaceView);
  }
}
