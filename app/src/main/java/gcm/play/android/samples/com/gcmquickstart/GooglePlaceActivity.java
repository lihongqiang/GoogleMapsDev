package gcm.play.android.samples.com.gcmquickstart;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.PlacePhotoResult;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GooglePlaceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    public static final int PLACE_PICKER_REQUEST = 1;

    private GoogleApiClient mGoogleApiClient;
    private Button btn_choose_position;
    private ImageView mImageView;
    private TextView mText;
    private Bitmap mBitmap;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == 0){
                mImageView.setImageBitmap(mBitmap);
                Log.e("handler", "ok");
            }
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_place);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        btn_choose_position = (Button) findViewById(R.id.choose_position);

        //点击按钮之后，开启地点选择器，获取当前位置
        btn_choose_position.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(GooglePlaceActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
        mImageView = (ImageView) findViewById(R.id.photo);
        mText = (TextView) findViewById(R.id.text);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                LatLng latLng = place.getLatLng();
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();

//                获取google static map picture
                if(latLng != null){
                    getGoogleMapPhoto(latLng);
                }
            }
        }
    }

    // 通过调用google static maps 获取图片
    public void getGoogleMapPhoto(LatLng latLng) {
        Log.e("getGoogleMapPhoto", "getGoogleMapPhoto");
        int width = mImageView.getWidth()/2;
        int height = mImageView.getHeight()/2;

        Log.e("mImageView", width + ", " + height);

        String uriString = "https://maps.googleapis.com/maps/api/staticmap?" +
                "markers=size:big%7Ccolor:red%7C" + latLng.latitude + "," + latLng.longitude +
                "&zoom=15" +
                "&size=" + width + "x" + height +
                "&scale=2" +
                "&format=png" +
                "&maptype=roadmap" +
                "&key=AIzaSyBdw0JQ7MvKHky8m0bEQCk1kDJpzUpb6AI";
        Log.e("uriString", uriString);
        startPhotosTask(uriString);
    }

    private void startPhotosTask(String uri) {

        // Create a new AsyncTask that displays the bitmap and attribution once loaded.
        new PhotoTask() {
            @Override
            protected void onPreExecute() {
                // Display a temporary image to show while bitmap is loading.
                mImageView.setImageResource(R.drawable.empty_photo);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    // Photo has been loaded, display it.
//                    mImageView.setImageBitmap(bitmap);
                    mBitmap = bitmap;
                    handler.sendEmptyMessage(0);
                }
            }
        }.execute(uri);
    }

    //    方式一 异步实现图片获取
//    private void initPlacePhotoMetadata(String placeId) {
//
//        // Get a PlacePhotoMetadataResult containing metadata for the first 10 photos.
//        PlacePhotoMetadataResult result = Places.GeoDataApi
//                .getPlacePhotos(mGoogleApiClient, placeId).await();
//
//        // Get a PhotoMetadataBuffer instance containing a list of photos (PhotoMetadata).
//        if (result != null && result.getStatus().isSuccess()) {
//
//            PlacePhotoMetadataBuffer photoMetadataBuffer = result.getPhotoMetadata();
//
//            // Get the first photo in the list.
//            PlacePhotoMetadata photo = photoMetadataBuffer.get(0);
//
//            // Get a full-size bitmap for the photo.
//            Bitmap image = photo.getPhoto(mGoogleApiClient).await()
//                    .getBitmap();
//
//            // Get the attribution text.
//            CharSequence attribution = photo.getAttributions();
//        }
//    }

    //方式二 获取位置点的相关图片
    /**
     * Load a bitmap from the photos API asynchronously
     * by using buffers and result callbacks.
     */
//    private void placePhotosAsync(String placeId) {
//        if(placeId.isEmpty() || placeId.equals("")){
//            placeId = "ChIJrTLr-GyuEmsRBfy61i59si0"; // Australian Cruise Group
//        }
//        Log.e("placeId", placeId);
//        Places.GeoDataApi.getPlacePhotos(mGoogleApiClient, placeId)
//                .setResultCallback(new ResultCallback<PlacePhotoMetadataResult>() {
//
//                    @Override
//                    public void onResult(PlacePhotoMetadataResult photos) {
//                        if (!photos.getStatus().isSuccess()) {
//                            return;
//                        }
//
//                        PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
//                        if (photoMetadataBuffer.getCount() > 0) {
//                            // Display the first bitmap in an ImageView in the size of the view
//                            photoMetadataBuffer.get(0)
//                                    .getScaledPhoto(mGoogleApiClient, mImageView.getWidth(),
//                                            mImageView.getHeight())
//                                    .setResultCallback(mDisplayPhotoResultCallback);
//                        }
//                        photoMetadataBuffer.release();
//                    }
//                });
//    }

}
