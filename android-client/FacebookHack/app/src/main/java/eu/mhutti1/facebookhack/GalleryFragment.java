package eu.mhutti1.facebookhack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GalleryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryFragment extends Fragment {

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;
  ArrayList<GalleryItem> galleryItems;

  GalleryAdapter adapter;
  private OnFragmentInteractionListener mListener;

  public GalleryFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment GalleryFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static GalleryFragment newInstance(String param1, String param2) {
    GalleryFragment fragment = new GalleryFragment();
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // Inflate the layout for this fragment

    View layout = inflater.inflate(R.layout.fragment_gallery, container, false);

    RecyclerView recyclerView = layout.findViewById(R.id.imagegallery);
    recyclerView.setHasFixedSize(true);

    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(),2);
    recyclerView.setLayoutManager(layoutManager);
    galleryItems = getFiles();
    adapter = new GalleryAdapter(getContext(), galleryItems);
    recyclerView.setAdapter(adapter);

    return layout;
  }

  // TODO: Rename method, update argument and hook method into UI event
  public void onButtonPressed(Uri uri) {
    if (mListener != null) {
      mListener.onFragmentInteraction(uri);
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

  public ArrayList<GalleryItem> getFiles() {
    File[] files = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES).listFiles();
    ArrayList<GalleryItem> result = new ArrayList<>();
    for (int i = files.length - 1; i > 0; i--) {
      Bitmap b = BitmapFactory.decodeFile(files[i].getPath());
      if (b != null) {
        result.add(new GalleryItem(files[i].getPath(), b));
            //Bitmap.createScaledBitmap(b, b.getWidth() / 10, b.getHeight() / 10, false)));
      }
    }
    return result;
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

  public static class GalleryItem {

    private String path;
    private Bitmap image;

    public GalleryItem(String path, Bitmap image) {
      this.path = path;
      this.image = image;
    }

  }


  public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private ArrayList<GalleryItem> galleryList;
    private Context context;

    public GalleryAdapter(Context context, ArrayList<GalleryItem> galleryList) {
      this.context = context;
      this.galleryList = galleryList;
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GalleryAdapter.ViewHolder holder, final int position) {
      holder.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
      holder.img.setImageBitmap(galleryList.get(position).image);
      holder.img.setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
          new File(galleryList.get(position).path).delete();
          galleryList.remove(position);
          notifyDataSetChanged();
          return false;
        }
      });
    }

    @Override
    public int getItemCount() {
      return galleryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
      private ImageView img;
      public ViewHolder(View view) {
        super(view);
        img = (ImageView) view.findViewById(R.id.img);
      }
    }
  }
}
