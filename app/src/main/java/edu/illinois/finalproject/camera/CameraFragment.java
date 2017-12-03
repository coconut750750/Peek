package edu.illinois.finalproject.camera;


import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.illinois.finalproject.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * http://www.tutorialspoint.com/android/android_textureview.htm
 */
public class CameraFragment extends Fragment {
    // extension of the saved picture
    public static final String PHOTO_EXTENSION = ".jpg";
    // the format of the date that will be used to give the saved picture a name
    public static final String DATA_FORMAT = "yyyyMMdd_HHmmss";

    private TextureView mTextureView;
    private CameraTextureListener mTextureListener;
    private Camera mCamera;

    /**
     * Picture Callback object the camera will use when taking a photo. When a picture is taken,
     * the data of the picture will be saved onto the phone. This is because the bitmap of the
     * picture will most likely be to big to transfer to another activity through a bundle. So,
     * instead, the location of the saved photo will be passed. After accessing the photo in the
     * next activity, the photo file will be deleted.
     */
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // saves picture data into a file to transfer the data to the next activity
            // source: https://developer.android.com/guide/topics/media/camera.html
            String timeStamp = new SimpleDateFormat(DATA_FORMAT, Locale.ENGLISH)
                    .format(new Date());
            String photoPath = timeStamp + PHOTO_EXTENSION;

            try {
                // creates a file output stream to write the photo data to
                FileOutputStream fileOutStream = getActivity().openFileOutput(photoPath, MODE_PRIVATE);
                fileOutStream.write(data);
                fileOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // moves to the next activity, the photo detail activity
            startCapturedImageActivity(photoPath);
        }
    };

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        mCamera = getCameraInstance();
        mTextureListener = new CameraTextureListener(mCamera);

        mTextureView = (TextureView) view.findViewById(R.id.camera_view);
        mTextureView.setSurfaceTextureListener(mTextureListener);

        Button captureButton = (Button) view.findViewById(R.id.take_reset_picture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, pictureCallback);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTextureView.isAvailable()) {
            mTextureListener.displayCamera(mTextureView.getSurfaceTexture());
        }
    }

    /**
     * Gets an instance of a Camera object. Using a deprecated version of Camera to support older
     * versions of android.
     *
     * @return A camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Starts the captured image activity given a photo name.
     *
     * @param photoName the name of the saved photo that was captured
     */
    private void startCapturedImageActivity(String photoName) {
        Intent pictureIntent = new Intent(getContext(), CapturedImageActivity.class);
        pictureIntent.putExtra(CapturedImageActivity.CAPTURED_PHOTO_NAME, photoName);
        startActivity(pictureIntent);
        getActivity().overridePendingTransition(R.anim.right_slide_in, R.anim.left_slide_out);
    }
}