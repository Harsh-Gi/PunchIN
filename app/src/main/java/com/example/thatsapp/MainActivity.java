package com.example.thatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

//import com.chaquo.python.PyObject;*
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;*
import com.example.thatsapp.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    protected Interpreter tflite;

    public Bitmap imgBitmap;
    public static Bitmap faceBitmap;

    private String currentPhotoPath;
    private final int CAMERA_REQ_CODE = 100;
    private final int GALLERY_REQ_CODE = 200;

    ActivityMainBinding binding;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    FirebaseDatabase database;

    public String sq = "";

    FusedLocationProviderClient mFusedLocationClient;

    Location currentLocation;

    int PERMISSION_ID = 44;
    private DatabaseReference mDatabase;

    boolean isPresent, resultDisplay=true, isUploaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                String currentDate = sdf.format(now);
                String userName = snapshot.child(currentUser.getUid()).child("userName").getValue().toString();

                if(snapshot.child(currentUser.getUid()).child("faceArray").getValue()==null){
                    isUploaded=false;
                }
                else{
                    isUploaded=true;
                }

                mDatabase.child("Attendance").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.child(currentDate).child(userName+"_"+currentUser.getUid()).getValue()==null){
                            isPresent=false;
                        }
                        else{
                            isPresent=true;
                            if(resultDisplay){
                                binding.faceRecResult.setText("Your attendance is already marked");
                            }
//                    Toast.makeText(MainActivity.this, "Your attendance is already marked", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        initComponents();

        binding.openUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUploaded){
                    Toast.makeText(MainActivity.this, "You have already uploaded the image!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Intent intent = new Intent(MainActivity.this, UploadActivity.class);
                    startActivity(intent);
                }
            }
        });

        binding.btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = "photo";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                try {
                    File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
                    currentPhotoPath = imageFile.getAbsolutePath();
                    Uri imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.thatsapp.fileprovider", imageFile);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CAMERA_REQ_CODE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

//        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent iGallery = new Intent(Intent.ACTION_PICK);
//                iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(iGallery, GALLERY_REQ_CODE);
//            }
//        });
    }

    private void initComponents() {
        sq = sq + "initComponents => ";
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Processing...");
        progressDialog.setMessage("Authenticating...");
        binding.btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPresent){
                    binding.faceRecResult.setText("Your attendance is already marked");
                }
                else{
                    progressDialog.show();
                    // Load the FaceNet TFLite model
                    try {
                        tflite = new Interpreter(loadModelFile());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

// Preprocess the two input face images
                    Bitmap face1 = faceBitmap; // load first face image
//                Bitmap face2 = croppedTest; // load second face image
                    Bitmap resizedFace1 = Bitmap.createScaledBitmap(face1, 160, 160, true);
//                Bitmap resizedFace2 = Bitmap.createScaledBitmap(face2, 160, 160, true);
                    ByteBuffer input1 = preprocessImage(resizedFace1);
//                ByteBuffer input2 = preprocessImage(resizedFace2);

                    int outputSize = 128;
//                int[] outputShape = tflite.getOutputTensor(0).shape();
//                int outputDataType = tflite.getOutputTensor(0).dataType();
                    ByteBuffer output1 = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder());
//                ByteBuffer output2 = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder());

// Pass the preprocessed face images through the FaceNet model

                    tflite.run(input1, output1);
//                tflite.run(input2, output2);

                    float[] embedding1 = postProcessResult(output1);
//                float[] embedding2 = postProcessResult(output2);
                    String id = currentUser.getUid();
                    DatabaseReference userRef = database.getReference("Users").child(id);

                    userRef.child("faceArray").addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String json = snapshot.getValue(String.class);
                                List<Float> list = new Gson().fromJson(json, new TypeToken<List<Float>>() {
                                }.getType());
                                float[] embedding2 = new float[128];
                                for (int i = 0; i < list.size(); i++) {
                                    embedding2[i] = list.get(i);
                                }

                                // Calculate the Euclidean distance between the two feature vectors
                                float distance = 0;
                                for (int i = 0; i < 128; i++) {
                                    float diff = embedding1[i] - embedding2[i];
                                    distance += diff * diff;
                                }
                                float distance1 = (float) Math.sqrt(distance);

                                // Compare the similarity score to a threshold value to determine if the faces are the same
                                float threshold = 10f; // set a threshold value based on your requirements
                                boolean isSameFace = distance1 < threshold;

                                if (checkPermissions()) {

                                    // check if location is enabled
                                    if (isLocationEnabled()) {
                                        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Location> task) {
                                                currentLocation = task.getResult();
                                                if (currentLocation == null) {
                                                    requestNewLocationData();
                                                } else {
                                                    mDatabase.child("RequiredLocation").addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            float locationResult = -1;

                                                            Location locationA = new Location("point A");

                                                            locationA.setLatitude(currentLocation.getLatitude());
                                                            locationA.setLongitude(currentLocation.getLongitude());

                                                            Location locationB = new Location("point B");

                                                            locationB.setLatitude((double) snapshot.child("reqLat").getValue());
                                                            locationB.setLongitude((double) snapshot.child("reqLon").getValue());

                                                            locationResult = locationA.distanceTo(locationB);

//                                                        binding.latTextViewE.setText((double) snapshot.child("reqLat").getValue() + "");
//                                                        binding.lonTextViewE.setText((double) snapshot.child("reqLon").getValue() + "");
                                                            if (locationResult >= 0 && locationResult < 100) {
//                                                    binding.locResult.setText("Result: Location is same with distance = " + locationResult);
                                                                if (isSameFace) {
//                                                                binding.faceRecResult.setText("Faces are same "+distance1+"\nLocation is same with distance = " + locationResult);
                                                                    Date now = new Date();
                                                                    SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                                                    String timeString = stf.format(now);
                                                                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                                                                    String currentDate = sdf.format(now);
//                                                                if(isPresent){
////                                                                Toast.makeText(MainActivity.this, "Your attendance is already marked", Toast.LENGTH_SHORT).show();
//                                                                    binding.faceRecResult.setText("Your attendance is already marked");
//                                                                }
//                                                                else{
                                                                    mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                            String userName = snapshot.child(currentUser.getUid()).child("userName").getValue().toString();
                                                                            mDatabase.child("Attendance").child(currentDate).child(userName + "_" + currentUser.getUid()).setValue(timeString);
                                                                            binding.faceRecResult.setText("Your attendance is marked successfully!");
                                                                            isPresent = true;
                                                                            resultDisplay = false;
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                                        }
                                                                    });

//                                                                }
                                                                } else {
                                                                    binding.faceRecResult.setText("Faces are not same");
                                                                }
                                                            } else {
//                                                    binding.locResult.setText("Result: Location is not same with distance = " + locationResult);
                                                                if (isSameFace) {
                                                                    binding.faceRecResult.setText("Location is not same, distance is " + locationResult + "m");
                                                                } else {
                                                                    binding.faceRecResult.setText("Faces are not same and location is not same, distance is " + locationResult + "m");
                                                                }
                                                            }
                                                            progressDialog.dismiss();
//                                                        if(attendanceVerified){
//                                                            Date now = new Date();
//                                                            SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
//                                                            String timeString = stf.format(now);
//                                                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
//                                                            String currentDate = sdf.format(now);
//                                                            if(isPresent){
////                                                                Toast.makeText(MainActivity.this, "Your attendance is already marked", Toast.LENGTH_SHORT).show();
//                                                                binding.faceRecResult.setText("Your attendance is already marked");
//                                                            }
//                                                            else{
//                                                                mDatabase.child("Attendance").child(currentDate).child(currentUser.getUid()).setValue(timeString);
//                                                                binding.faceRecResult.setText("Your attendance is marked successfully!");
//                                                            }
//                                                        }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });


                                                }
                                            }
                                        });
                                    } else {
                                        Toast.makeText(MainActivity.this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(intent);
                                    }
                                } else {
                                    // if permissions aren't available,
                                    // request for permissions
                                    requestPermissions();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle the error
                        }
                    });
                }
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("facenet.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void faceDetect(final Bitmap bitmap){
        sq=sq+"face_detector => ";
        if(bitmap!=null){
            final InputImage image = InputImage.fromBitmap(bitmap,0);
            com.google.mlkit.vision.face.FaceDetector detector = FaceDetection.getClient();
            detector.process(image).addOnSuccessListener(
                            new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(List<Face> faces) {
                                    // Task completed successfully
                                    for (Face face : faces) {
                                        Rect bounds = face.getBoundingBox();
//                                    if(imageType.equals("original")){
                                        try{
                                            faceBitmap=Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
                                        }
                                        catch (Exception e){
                                            Toast.makeText(MainActivity.this, "Please click the photo again!", Toast.LENGTH_SHORT).show();
                                        }
//                                    binding.imgCameraTest.setImageBitmap(faceBitmap);
//                                    }
//                                    else if(imageType.equals("test")){
//                                        croppedTest=Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
//                                        binding.imgGalleryTest.setImageBitmap(croppedTest);
//                                    }
                                    }
                                }
                            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
        }
        else{
            Toast.makeText(MainActivity.this, "Please click the photo again!", Toast.LENGTH_SHORT).show();
        }
    }

    private ByteBuffer preprocessImage(Bitmap bitmap) {
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, 160, 160, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 160 * 160 * 3 * 4)
                .order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 160; x++) {
                int pixel = resizedImage.getPixel(x, y);
                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
            }
        }
        return inputBuffer;
    }

    private float[] postProcessResult(ByteBuffer outputBuffer) {
        float[] embeddings = new float[128];
        outputBuffer.rewind();
        outputBuffer.asFloatBuffer().get(embeddings);
        return embeddings;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        sq=sq+"onActivityResult => ";
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==CAMERA_REQ_CODE){
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                binding.imgCamera.setImageBitmap(bitmap);
                imgBitmap = bitmap;
//                face_detector(oribitmap,"original");
                faceDetect(imgBitmap);

//                Bitmap img = (Bitmap) data.getExtras().get("data");
//                imgCamera.setImageBitmap(img);
            }
            if(requestCode==GALLERY_REQ_CODE){
                binding.imgCamera.setImageURI(data.getData());
                Uri imageuri = data.getData();
                try {
                    imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageuri);
//                    face_detector(testbitmap,"test");
                    faceDetect(imgBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
//            case R.id.settings:
//                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
//                break;
            case R.id.logout:
                auth.signOut();
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }
    private String getStringImage(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        currentLocation = task.getResult();
                        if (currentLocation == null) {
                            requestNewLocationData();
                        } else {
//                            binding.latTextView.setText(currentLocation.getLatitude() + "");
//                            binding.lonTextView.setText(currentLocation.getLongitude() + "");
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
//            binding.latTextView.setText("Latitude: " + mLastLocation.getLatitude() + "");
//            binding.lonTextView.setText("Longitude: " + mLastLocation.getLongitude() + "");
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }
}