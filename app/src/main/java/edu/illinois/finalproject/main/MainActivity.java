package edu.illinois.finalproject.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import edu.illinois.finalproject.R;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static edu.illinois.finalproject.authentication.AuthenticationActivity.mGoogleApiClient;

/**
 * This is the main activity and users will see when using the app. It consists of three main
 * Fragments that are controlled by a ViewPager. The three fragments are the map, camera, and
 * profile fragment. The MainActivity is also in charge of asking the user for permissions.
 */
public class MainActivity extends AppCompatActivity {
    // app defined constant to determine the permission that was requested
    public static final int PERMISSIONS_ALL = 1;
    private ViewPager mViewPager;
    private Button mapButton;
    private Button profileButton;

    public static final int MAP_PAGE = 0;
    public static final int CAMERA_PAGE = 1;
    public static final int PROFILE_PAGE = 2;

    // permissions
    public static boolean fineLocationPermission;
    public static boolean coarseLocationPermission;

    private FirebaseAuth mAuth;
    public static final String USER_ID_REF = "user_ids";
    private DatabaseReference userIdsRef;
    private HashMap<String, String> allUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // configure buttons
        mapButton = (Button) findViewById(R.id.map_menu_button);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(MAP_PAGE, true);
            }
        });

        profileButton = (Button) findViewById(R.id.profile_menu_button);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(PROFILE_PAGE, true);
            }
        });

        // configure viewpager
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mViewPager.setAdapter(new PageSwipeAdapter(getSupportFragmentManager()));
        mViewPager.setPageTransformer(false, new MainPageTransformer());
        mViewPager.setCurrentItem(1);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) {
                float absolutePos = pos + posOffset;
                float posRelativeToCamera = Math.abs(absolutePos - CAMERA_PAGE);
                adjustButtonMargins(posRelativeToCamera);
            }

            @Override
            public void onPageSelected(int position) {
                // need to implement but no functionality needed
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // need to implement but no functionality needed
            }
        });

        askForPermissions();

        // firebase operations
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        addUserToFirebase(user);

        // set up sign out button
        setupSignOutButton();
    }

    private void addUserToFirebase(final FirebaseUser user) {
        allUsers = new HashMap<>();
        userIdsRef = FirebaseDatabase.getInstance().getReference(USER_ID_REF);
        userIdsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<HashMap<String, String>>() {
                };
                HashMap<String, String> list = dataSnapshot.getValue(t);
                if (list != null) {
                    for (String uid : list.keySet()) {
                        allUsers.put(uid, list.get(uid));
                    }

                    allUsers.put(user.getUid(), user.getDisplayName());
                    userIdsRef.setValue(allUsers);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // need to implement but no functionality needed
            }
        });
    }

    /**
     * Asks for the permissions that this apps needs: camera, writing external storage, and
     * accessing location data
     */
    private void askForPermissions() {
        String[] permissionsToAsk = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (!hasPermissions(permissionsToAsk)) {
            ActivityCompat.requestPermissions(this, permissionsToAsk, PERMISSIONS_ALL);
        }

        fineLocationPermission = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
        coarseLocationPermission = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks to see if application has a list of permissions. If not, the app will request them.
     * Source: https://stackoverflow.com/questions/34342816/android-6-0-multiple-permissions
     *
     * @param permissions the list of permissions to check
     * @return false if one permissions is not granted. true if all permissions are granted
     */
    public boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method enables the library used to assign fonts to text on runtime
     * Source: https://github.com/chrisjenx/Calligraphy
     *
     * @param newBase
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    /**
     * move the menu buttons off the screen
     *
     * @param posRelativeToCamera
     */
    public void adjustButtonMargins(float posRelativeToCamera) {
        float displacementMult = 0.5f;
        float initialDisplacement = 0.5f;
        posRelativeToCamera = posRelativeToCamera * displacementMult + initialDisplacement;

        int margin = (int) getResources().getDimension(R.dimen.menu_button_margin);
        int percentMargin = (int) (margin * posRelativeToCamera);

        RelativeLayout.LayoutParams mapButtonLayout =
                (RelativeLayout.LayoutParams) mapButton.getLayoutParams();
        mapButtonLayout.setMarginStart(percentMargin);
        mapButton.setLayoutParams(mapButtonLayout);

        RelativeLayout.LayoutParams profileButtonLayout =
                (RelativeLayout.LayoutParams) profileButton.getLayoutParams();
        profileButtonLayout.setMarginEnd(percentMargin);
        profileButton.setLayoutParams(profileButtonLayout);
    }

    private void setupSignOutButton() {
        findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog.show(MainActivity.this, getResources().getString(R.string.signing_out));
                // connects to google api client, then signs out of Google
                // source: https://stackoverflow.com/questions/38039320/googleapiclient-is-not-
                // connected-yet-on-logout-when-using-firebase-auth-with-g

                mGoogleApiClient.connect();
                mGoogleApiClient.registerConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(@Nullable Bundle bundle) {
                                FirebaseAuth.getInstance().signOut();

                                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                                        new ResultCallback<Status>() {
                                            @Override
                                            public void onResult(@NonNull Status status) {
                                                if (status.isSuccess()) {
                                                    finish();
                                                }
                                            }

                                        }
                                );
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // implementation needed but no functionality needed
                            }
                        }
                );
            }
        });
    }
}
