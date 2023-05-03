package com.example.thatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.thatsapp.databinding.ActivityMainBinding;
import com.example.thatsapp.databinding.ActivityUploadBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class UploadActivity extends AppCompatActivity {
    protected Interpreter tflite;

    public Bitmap imgBitmap;
    public static Bitmap faceBitmap;

    private String currentPhotoPath;
    private final int CAMERA_REQ_CODE = 100;
    private final int GALLERY_REQ_CODE = 200;

    ActivityUploadBinding binding;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        currentUser=auth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        initComponents();

        binding.openUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UploadActivity.this, MainActivity.class);
                startActivity(intent);
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
                    Uri imageUri = FileProvider.getUriForFile(UploadActivity.this, "com.example.thatsapp.fileprovider", imageFile);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CAMERA_REQ_CODE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent iGallery = new Intent(Intent.ACTION_PICK);
                iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(iGallery, GALLERY_REQ_CODE);
            }
        });
    }

    private void initComponents(){
        ProgressDialog progressDialog = new ProgressDialog(UploadActivity.this);
        progressDialog.setTitle("Uploading...");
        progressDialog.setMessage("Image is being uploaded...");
        binding.btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                // Load the FaceNet TFLite model
                try {
                    tflite = new Interpreter(loadModelFile());
                }catch(Exception e){
                    e.printStackTrace();
                }

// Preprocess the two input face images
                Bitmap face1 = faceBitmap; // load first face image
                Bitmap resizedFace1 = Bitmap.createScaledBitmap(face1, 160, 160, true);
                ByteBuffer input1 = preprocessImage(resizedFace1);

                int outputSize = 128;
                ByteBuffer output1 = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder());

// Pass the preprocessed face images through the FaceNet model

                tflite.run(input1, output1);

                float[] embedding1 = postProcessResult(output1);

                List<Float> list = new ArrayList<Float>(embedding1.length);
                for (float value : embedding1) {
                    list.add(value);
                }
                String jsonData = new Gson().toJson(list);

                String id = currentUser.getUid();
                database.getReference().child("Users").child(id).child("faceArray").setValue(jsonData);
                progressDialog.dismiss();
                binding.faceRecResult.setText("Image uploaded successfully");
//                Toast.makeText(getApplicationContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
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
        final InputImage image = InputImage.fromBitmap(bitmap,0);
        com.google.mlkit.vision.face.FaceDetector detector = FaceDetection.getClient();
        detector.process(image).addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                // Task completed successfully
                                for (Face face : faces) {
                                    Rect bounds = face.getBoundingBox();
                                    try{
                                        faceBitmap=Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
                                    }
                                    catch (Exception e){
                                        Toast.makeText(UploadActivity.this, "Please click the photo again!", Toast.LENGTH_SHORT).show();
                                    }
//                                    faceBitmap=Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
//                                    binding.imgCameraTest.setImageBitmap(faceBitmap);
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
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==CAMERA_REQ_CODE){
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                binding.imgCamera.setImageBitmap(bitmap);
                imgBitmap = bitmap;
                faceDetect(imgBitmap);

//                Bitmap img = (Bitmap) data.getExtras().get("data");
//                imgCamera.setImageBitmap(img);
            }
            if(requestCode==GALLERY_REQ_CODE){
                binding.imgCamera.setImageURI(data.getData());
                Uri imageuri = data.getData();
                try {
                    imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageuri);
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
                Intent intent = new Intent(UploadActivity.this, SignInActivity.class);
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
}