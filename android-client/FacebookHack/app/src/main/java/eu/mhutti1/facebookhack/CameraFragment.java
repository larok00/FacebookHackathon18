package eu.mhutti1.facebookhack;

import static android.app.Activity.RESULT_OK;
import static eu.mhutti1.facebookhack.MainActivity.REQUEST_IMAGE_CAPTURE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


public class CameraFragment extends Fragment implements OnClickListener {

  private Button button;

  private OnFragmentInteractionListener mListener;

  ImageView photoView;

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
    button.setOnClickListener(this);

    // Inflate the layout for this fragment
    return layout;
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
    takePicture();
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
      Bundle extras = data.getExtras();
      Bitmap imageBitmap = BitmapFactory.decodeFile(lastImage);
      photoView.setImageBitmap(imageBitmap);

      process();


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


    protected void onPreExecute() {
    }

    protected String doInBackground(String... urls) {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();

      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

      FileBody fileBody = new FileBody(new File(lastImage)); //image should be a String
      builder.addPart("my_file", fileBody);

      HttpEntity entity = builder.build();
      HttpPost httpPost = new HttpPost(EndPoints.ROOT + EndPoints.PROCESS);
      httpPost.setEntity(entity);
      CloseableHttpClient client = HttpClients.createDefault();
      try {
        httpResponse = client.execute(httpPost);
        httpResponse.getEntity().writeTo(baos);
      } catch (IOException e) {
        e.printStackTrace();
      }

      return "hello";
    }

    protected void onPostExecute(String feed) {
      Bitmap result = BitmapFactory.decodeByteArray(baos.toByteArray(), 0,
          (int) httpResponse.getEntity().getContentLength());
      photoView.setImageBitmap(result);

    }
  }
}
