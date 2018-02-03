package eu.mhutti1.facebookhack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import eu.mhutti1.facebookhack.CameraFragment.OnFragmentInteractionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPostHC4;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class MainActivity extends AppCompatActivity implements CameraFragment.OnFragmentInteractionListener,
    GalleryFragment.OnFragmentInteractionListener {

  static final int REQUEST_IMAGE_CAPTURE = 1;
  static final int PICK_IMAGE = 2;


  CameraFragment cameraFragment;

  GalleryFragment galleryFragment;


  BottomNavigationView navigation;

  PhotoPageAdapter photoPageAdapter;
  ViewPager mViewPager;

  private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
      = new BottomNavigationView.OnNavigationItemSelectedListener() {

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
      switch (item.getItemId()) {
        case R.id.navigation_camera:
          mViewPager.setCurrentItem(0);
          return true;
        case R.id.navigation_gallery:
          mViewPager.setCurrentItem(1);
          return true;
      }
      return false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    photoPageAdapter =
        new PhotoPageAdapter(
            getSupportFragmentManager());
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(photoPageAdapter);
    mViewPager.addOnPageChangeListener(new OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        if (position == 0) {
          navigation.setSelectedItemId(R.id.navigation_camera);
        } else {
          navigation.setSelectedItemId(R.id.navigation_gallery);
        }
      }

      @Override
      public void onPageScrollStateChanged(int state) {

      }
    });


    navigation = (BottomNavigationView) findViewById(R.id.navigation);
    navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    cameraFragment = new CameraFragment();
    galleryFragment = new GalleryFragment();


  }










  @Override
  public void onFragmentInteraction(Uri uri) {

  }



  public class PhotoPageAdapter extends FragmentStatePagerAdapter {
    public PhotoPageAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      if (i == 0) {
        return cameraFragment;
      } else {
        return galleryFragment;
      }
    }

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return "OBJECT " + (position + 1);
    }
  }
}
