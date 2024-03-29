package com.abr.quickme.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.abr.quickme.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SETTINGS_ACTIVITY";
    FirebaseUser mCurrentUser;
    CircleImageView settings_profile_image;

    Toolbar mToolbar;
    ProgressDialog mProgress;
    private DatabaseReference mUserDatabase;
    private CircleImageView mDislplayImage;
    private TextView mName, mStatus;
    private StorageReference mStorageRef, mImageStorage;
    private Button btn_statusSettings, btn_changeImage;
    private String ImgURL, ThumbURL;
    private String image;
    private AdView mAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mToolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Profile Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDislplayImage = findViewById(R.id.settings_profile_image);
        mName = findViewById(R.id.settings_displayName);
        mStatus = findViewById(R.id.settings_Status);

        fetchProfile();
        adView();

        btn_changeImage = findViewById(R.id.btn_changeImage);
        btn_changeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMaxCropResultSize(2000, 2000)
                        .setAllowCounterRotation(true)
                        .start(ProfileSettingsActivity.this);
            }
        });

        btn_statusSettings = findViewById(R.id.btn_status_settings);
        btn_statusSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent statusSettingsIntent = new Intent(ProfileSettingsActivity.this, StatusSettingsActivity.class);
                String status_value = mStatus.getText().toString();
                statusSettingsIntent.putExtra("status_value", status_value);
                startActivity(statusSettingsIntent);
            }
        });

        mDislplayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageIntent = new Intent(ProfileSettingsActivity.this, ImageViewActivity.class);
                imageIntent.putExtra("image", image);
                startActivity(imageIntent);
            }
        });
    }

    //show ad
    private void adView() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdViewBottom = findViewById(R.id.adViewBottom);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewBottom.loadAd(adRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final Uri resultUri = result.getUri();
                File thumb_filePath = new File(resultUri.getPath());

                mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
                final String current_uid = mCurrentUser.getUid();

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(80)
                            .setMaxHeight(80)
                            .setQuality(30)
                            .compressToBitmap(thumb_filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumb_byte = baos.toByteArray();

                final StorageReference filePath = mImageStorage.child("profile_images").child(current_uid + ".jpg");
                final StorageReference thumb_filepath = mImageStorage.child("profile_images").child("thumbs").child(current_uid + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                ImgURL = uri.toString();

                                final UploadTask uploadTask = thumb_filepath.putBytes(thumb_byte);
                                uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                ThumbURL = uri.toString();

                                                Map update_hashMap = new HashMap();
                                                update_hashMap.put("image", ImgURL);
                                                update_hashMap.put("thumb_image", ThumbURL);

                                                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
                                                mUserDatabase.updateChildren(update_hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(ProfileSettingsActivity.this, "Hohoo, Image Updated", Toast.LENGTH_SHORT).show();
                                                        settings_profile_image = findViewById(R.id.settings_profile_image);
                                                        settings_profile_image.setImageURI(resultUri);
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(ProfileSettingsActivity.this, "Oops, Error uploading image", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ProfileSettingsActivity.this, "Oops, Error uploading thumbnail", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Oops, Error Cropping Image" + error.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchProfile() {
        mProgress = new ProgressDialog(ProfileSettingsActivity.this);
        mProgress.setTitle("Fetching Profile");
        mProgress.setMessage("Your Toast Is Being Ready!");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                final String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                Picasso.get().load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.profile_sample).into(mDislplayImage);
                mName.setText(name);
                mStatus.setText(status);

                mProgress.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
