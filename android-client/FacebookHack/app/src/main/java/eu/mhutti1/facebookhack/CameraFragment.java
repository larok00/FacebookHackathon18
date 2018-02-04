package eu.mhutti1.facebookhack;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static eu.mhutti1.facebookhack.MainActivity.PICK_IMAGE;
import static eu.mhutti1.facebookhack.MainActivity.REQUEST_IMAGE_CAPTURE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import eu.mhutti1.facebookhack.GalleryFragment.GalleryItem;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;


public class CameraFragment extends Fragment implements OnClickListener {

  private Button button;

  private Button importButton;

  private Button switchButton;

  private Button happyButton;

  private Button sadButton;

  //private Button neutralButton;

  private Button saveButton;

  private Button discardButton;

  private OnFragmentInteractionListener mListener;

  CameraDevice cameraDevice;

  static int angle = 0;

  ImageView photoView;

  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  ProgressBar progressBar;

  TextureView textureView;

  HandlerThread threadHandler;

  ImageReader imageReader;

  Handler handler;

  private String cameraId;

  int cameraNumber = 0;

  CameraCaptureSession cameraCaptureSessions;

  private Size imageDimension;

  protected CaptureRequest.Builder captureRequestBuilder;



  public static HttpClient client = HttpClients.createDefault();



  String lastImage;


  public CameraFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment CameraFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static CameraFragment newInstance(String param1, String param2) {
    CameraFragment fragment = new CameraFragment();
    return fragment;
  }

  public void choose() {
    textureView.setVisibility(View.INVISIBLE);
    button.setVisibility(View.INVISIBLE);
    importButton.setVisibility(View.INVISIBLE);
    switchButton.setVisibility(View.INVISIBLE);
    happyButton.setVisibility(View.VISIBLE);
    sadButton.setVisibility(View.VISIBLE);
    //neutralButton.setVisibility(View.VISIBLE);
  }

  public void process(String emotion) {

    ProcessPicture processPicture = new ProcessPicture();
    processPicture.execute(lastImage, emotion);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View layout = inflater.inflate(R.layout.fragment_camera, container, false);

    photoView = layout.findViewById(R.id.photoView);
//    photoView.setOnLongClickListener(new OnLongClickListener() {
//      @Override
//      public boolean onLongClick(View view) {
//        Matrix matrix = new Matrix();
//        photoView.setScaleType(ImageView.ScaleType.MATRIX);   //required
//        matrix.postRotate((float) --angle *  90, photoView.getWidth() / 2, photoView.getHeight() / 2);
//        Bitmap original = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
//        Bitmap result = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, false);
//        photoView.setImageBitmap(result);
//
//        FileOutputStream fOut = null;
//        try {
//          fOut = new FileOutputStream(new File(lastImage));
//          result.compress(CompressFormat.JPEG, 100, fOut);
//          fOut.flush();
//          fOut.close();
//        } catch (FileNotFoundException e) {
//          e.printStackTrace();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }x
//
//        return true;
//      }
//    });
    button = layout.findViewById(R.id.captureButton);
    importButton = layout.findViewById(R.id.importButton);
    progressBar = layout.findViewById(R.id.progressBar);
    textureView = layout.findViewById(R.id.texturView);
    textureView.setSurfaceTextureListener(textureListener);
    happyButton = layout.findViewById(R.id.happyButton);
    sadButton = layout.findViewById(R.id.sadButton);
    saveButton = layout.findViewById(R.id.saveButton);
    discardButton = layout.findViewById(R.id.discardButton);
    discardButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        textureView.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        importButton.setVisibility(View.VISIBLE);
        switchButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.INVISIBLE);
        discardButton.setVisibility(View.INVISIBLE);
      }
    });
    saveButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        saveButton.setVisibility(View.INVISIBLE);
        discardButton.setVisibility(View.INVISIBLE);
        try {
          File output = createImageFile();
          FileOutputStream fOut = new FileOutputStream(output);
          BitmapDrawable drawable = (BitmapDrawable) photoView.getDrawable();
          Bitmap bitmap = drawable.getBitmap();
          bitmap.compress(CompressFormat.JPEG, 100, fOut);
          fOut.flush();
          fOut.close();
          GalleryFragment galleryFragment = ((MainActivity) getActivity()).galleryFragment;
          galleryFragment.galleryItems.add(new GalleryItem(output.getPath(), bitmap));
          galleryFragment.adapter.notifyDataSetChanged();
        } catch (IOException e) {
          e.printStackTrace();
        }

        textureView.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        importButton.setVisibility(View.VISIBLE);
        switchButton.setVisibility(View.VISIBLE);
      }
    });
    //neutralButton = layout.findViewById(R.id.neutralButton);
    /*
    neutralButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        happyButton.setVisibility(View.INVISIBLE);
        sadButton.setVisibility(View.INVISIBLE);
        neutralButton.setVisibility(View.INVISIBLE);
        saveDiscard();
      }
    });
    */
    happyButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        process("happy");
      }
    });
    happyButton.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        process("vhappy");
        return true;
      }
    });
    sadButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        process("sad");
      }
    });
    sadButton.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        process("vsad");
        return true;
      }
    });


    switchButton = layout.findViewById(R.id.switchButton);
    switchButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        closeCamera();
        cameraNumber++;
        startCamera();
      }
    });

    importButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
      }
    });
    button.setOnClickListener(this);

   // TextureView textureView = layout.findViewById(R.id.surfaceView);
    // Inflate the layout for this fragment
    return layout;
  }


  TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      startCamera();
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      // Transform you image captured size according to the surface width and height
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
  };

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice camera) {
      cameraDevice = camera;
      createCameraPreview();
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
      if (cameraDevice != null) {
        cameraDevice.close();
      }
    }

    @Override
    public void onError(CameraDevice camera, int error) {
      cameraDevice.close();
      cameraDevice = null;
    }
  };



  final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      super.onCaptureCompleted(session, request, result);
      createCameraPreview();
    }
  };
  protected void startBackgroundThread() {
    threadHandler = new HandlerThread("Camera Background");
    threadHandler.start();
    handler = new Handler(threadHandler.getLooper());
  }
  protected void stopBackgroundThread() {
    threadHandler.quitSafely();
    try {
      threadHandler.join();
      threadHandler = null;
      handler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.e(TAG, "onResume");
    startBackgroundThread();
    if (textureView.isAvailable()) {
      startCamera();
    } else {
      textureView.setSurfaceTextureListener(textureListener);
    }
  }

  @Override
  public void onPause() {
    Log.e(TAG, "onPause");
    //closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  protected void createCameraPreview() {
    try {
      SurfaceTexture texture = textureView.getSurfaceTexture();
      assert texture != null;
      texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
      Surface surface = new Surface(texture);
      captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      captureRequestBuilder.addTarget(surface);

      cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
          //The camera_btn is already closed
          if (null == cameraDevice) {
            return;
          }
          // When the session is ready, we start displaying the preview.
          cameraCaptureSessions = cameraCaptureSession;
          updatePreview();
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
        }
      }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }


  protected void capturePicture(String path) {
    if (null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }
    final File file = new File(path);
    OutputStream output = null;
    try {
      output = new FileOutputStream(file);
      Bitmap bitmap = textureView.getBitmap();
      bitmap.compress(CompressFormat.JPEG, 100, output);

      //output.write(bytes);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      if (null != output) {
        try {
          output.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          photoView.setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
          lastImage = file.getPath();
          choose();
        }
      });
    }
  }
  private void startCamera() {
    Log.d("test", String.valueOf(getResources().getConfiguration().orientation));
    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    try {
      cameraId = manager.getCameraIdList()[cameraNumber % manager.getCameraIdList().length];
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      assert map != null;
      imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
      // Add permission for camera_btn and let user grant the permission
      if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
        return;
      }
      manager.openCamera(cameraId, stateCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }


  protected void updatePreview() {
    if(null == cameraDevice) {
      Log.e(TAG, "updatePreview error, return");
    }
    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    try {
      cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  private void closeCamera() {
    if (null != cameraDevice) {
      cameraDevice.close();
      cameraDevice = null;
    }
    if (null != imageReader) {
      imageReader.close();
      imageReader = null;
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override
  public void onClick(View view) {
    File f = null;
    try {
      f = createImageFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    capturePicture(f.getPath());




  }


  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getContext().getExternalMediaDirs()[0];
    File image = File.createTempFile(
        imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    );
    lastImage = image.getPath();

    return image;
  }

  private void takePicture() {
   /* Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }*/
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


    File photoFile = null;
    try {
      photoFile = createImageFile();
    } catch (IOException ex) {
      // Error occurred while creating the File
    }
    // Continue only if the File was successfully created
    if (photoFile != null) {
      Uri photoURI = FileProvider.getUriForFile(getContext(),
          "eu.mhutti1.facebookhack.fileprovider",
          photoFile);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }


  }

  public void onFragmentResult() {

  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      Bitmap imageBitmap = BitmapFactory.decodeFile(lastImage);
      photoView.setImageBitmap(imageBitmap);
      Log.d("test", "triggered");
      choose();
    }
    if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
      Bitmap imageBitmap = null;
      try {
        imageBitmap = BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(data.getData()));
        lastImage = createImageFile().getPath();
        FileOutputStream fOut = new FileOutputStream(new File(lastImage));
        imageBitmap.compress(CompressFormat.JPEG, 25, fOut);
        fOut.flush();
        fOut.close();
        photoView.setImageBitmap(imageBitmap);
        choose();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {

    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
  }


  class ProcessPicture extends AsyncTask<String, Void, String> {

    HttpResponse httpResponse;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Bitmap result;

    protected void onPreExecute() {
      happyButton.setVisibility(View.INVISIBLE);
      sadButton.setVisibility(View.INVISIBLE);
      progressBar.setVisibility(View.VISIBLE);
      //neutralButton.setVisibility(View.INVISIBLE);
    }

    protected String doInBackground(String... urls) {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();

      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);


      FileBody fileBody = new FileBody(new File(lastImage)); //image should be a String
      builder.addPart("my_file", fileBody);

      HttpEntity entity = builder.build();
      HttpPost httpPost = new HttpPost(EndPoints.ROOT + EndPoints.PROCESS);
      httpPost.setHeader("emotion", urls[1]);
      httpPost.setEntity(entity);
      try {
        httpResponse = client.execute(httpPost);
        httpResponse.getEntity().writeTo(baos);
        result = BitmapFactory.decodeByteArray(baos.toByteArray(), 0,
            (int) httpResponse.getEntity().getContentLength());
        new File(lastImage).delete();
        new File(lastImage + ".comp").delete();
      } catch (IOException e) {
        e.printStackTrace();
      }

      return "hello";
    }

    protected void onPostExecute(String feed) {
      photoView.setImageBitmap(result);
      saveDiscard();

    }
  }
  public void saveDiscard() {
    saveButton.setVisibility(View.VISIBLE);
    discardButton.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.INVISIBLE);
  }

}
