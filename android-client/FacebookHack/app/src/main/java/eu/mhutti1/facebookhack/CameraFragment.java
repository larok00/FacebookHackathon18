package eu.mhutti1.facebookhack;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static eu.mhutti1.facebookhack.MainActivity.PICK_IMAGE;
import static eu.mhutti1.facebookhack.MainActivity.REQUEST_IMAGE_CAPTURE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


public class CameraFragment extends Fragment implements OnClickListener {

  private Button button;

  private Button importButton;

  private Button switchButton;

  private OnFragmentInteractionListener mListener;

  CameraDevice cameraDevice;

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

  public void process() {
    ProcessPicture processPicture = new ProcessPicture();
    processPicture.execute(lastImage);
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
    button = layout.findViewById(R.id.button);
    importButton = layout.findViewById(R.id.importButton);
    progressBar = layout.findViewById(R.id.progressBar);
    textureView = layout.findViewById(R.id.texturView);
    textureView.setSurfaceTextureListener(textureListener);

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
      cameraDevice.close();
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
          //The camera is already closed
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
    if(null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }
    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
      Size[] jpegSizes = null;
      if (characteristics != null) {
        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(
            ImageFormat.JPEG);
      }
      int width = 640;
      int height = 480;
      if (jpegSizes != null && 0 < jpegSizes.length) {
        width = jpegSizes[0].getWidth();
        height = jpegSizes[0].getHeight();
      }
      ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
      List<Surface> outputSurfaces = new ArrayList<Surface>(2);
      outputSurfaces.add(reader.getSurface());
      outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
      final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureBuilder.addTarget(reader.getSurface());
      captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
      // Orientation
      int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
      captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
      final File file = new File(path);
      ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
          Image image = null;
          try {
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            save(bytes);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            if (image != null) {
              image.close();
            }
          }
        }
        private void save(byte[] bytes) throws IOException {
          OutputStream output = null;
          try {
            output = new FileOutputStream(file);
            if (cameraNumber % 2 == 1) {
              Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
              Matrix matrix = new Matrix();
              matrix.postScale(1, -1, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
              Bitmap.createBitmap(bitmap,0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true).compress(CompressFormat.JPEG, 100, output);
            } else {
              output.write(bytes);
            }
          } finally {
            if (null != output) {
              output.close();
            }
            photoView.setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
            lastImage = file.getPath();
           // photoView.setImageBitmap(imageBitmap);
            process();

          }
        }
      };
      reader.setOnImageAvailableListener(readerListener, handler);
      final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);
          createCameraPreview();
        }
      };
      cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
          try {
            session.capture(captureBuilder.build(), captureListener, handler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
      }, handler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
  private void startCamera() {
    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    try {
      cameraId = manager.getCameraIdList()[cameraNumber % manager.getCameraIdList().length];
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      assert map != null;
      imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
      // Add permission for camera and let user grant the permission
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
    File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
      process();
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
        process();
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
      textureView.setVisibility(View.INVISIBLE);
      progressBar.setVisibility(View.VISIBLE);
      button.setEnabled(false);
      importButton.setEnabled(false);
    }

    protected String doInBackground(String... urls) {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();

      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

      File compressedFile = new File(lastImage + ".comp");
      try {
        FileOutputStream fOut = new FileOutputStream(compressedFile);
        BitmapFactory.decodeFile(lastImage).compress(CompressFormat.JPEG, 25, fOut);
        fOut.flush();
        fOut.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }

      FileBody fileBody = new FileBody(compressedFile); //image should be a String
      builder.addPart("my_file", fileBody);

      HttpEntity entity = builder.build();
      HttpPost httpPost = new HttpPost(EndPoints.ROOT + EndPoints.PROCESS);
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
      try {
        File output = createImageFile();
        FileOutputStream fOut = new FileOutputStream(output);

        result.compress(Bitmap.CompressFormat.PNG, 85, fOut);
        fOut.flush();
        fOut.close();
        GalleryFragment galleryFragment = ((MainActivity) getActivity()).galleryFragment;
        galleryFragment.galleryItems.add(new GalleryItem(output.getPath(), result));
        galleryFragment.adapter.notifyDataSetChanged();
      } catch (IOException e) {
        e.printStackTrace();
      }

      progressBar.setVisibility(View.INVISIBLE);

      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          textureView.setVisibility(View.VISIBLE);
          button.setEnabled(true);
          importButton.setEnabled(true);        }
      }, 3000);

    }
  }
}
