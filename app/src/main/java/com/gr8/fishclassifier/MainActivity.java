package com.gr8.fishclassifier;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    ImageButton btn_camera, btn_flash, btn_switch, btn_select;

    LinearLayout layout_loading, layout_camera_cover;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result){
                startCamera(cameraFacing);
            }
        }
    });

    boolean isLoading = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_v2);

        layout_loading = findViewById(R.id.layout_loading);
        layout_camera_cover = findViewById(R.id.layout_camera_cover);

        btn_select = findViewById(R.id.btn_select);
        btn_camera = findViewById(R.id.btn_camera);
        btn_switch = findViewById(R.id.btn_switch);
        btn_flash = findViewById(R.id.btn_flash);

        previewView = findViewById(R.id.cameraView);

        IsLoading(false);
        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLoading)return;
                IsLoading(true);
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(cameraIntent, 1);
            }
        });

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        }else {
            startCamera(cameraFacing);
        }
        btn_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLoading)return;
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });
    }
    public void startCamera(int cameraFacing){
        int aspectRatio = AspectRatio.RATIO_4_3;
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);


                btn_camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isLoading)return;
                        IsLoading(true);
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                        takePicture(imageCapture, new Callback() {
                            @Override
                            public void BeforeEvent() {
                                layout_camera_cover.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void AfterEvent() {
                                layout_camera_cover.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });

                btn_flash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isLoading)return;
                        setFlashIcon(camera);
                    }
                });

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    public void takePicture(ImageCapture imageCapture, Callback callback){
        //final File file = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        //ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        Intent intent = new Intent(this, View_Results.class);
        callback.BeforeEvent();
        imageCapture.takePicture(Executors.newCachedThreadPool(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                ImageProxy.PlaneProxy[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                // Create a Bitmap from the byte array
                Bitmap capturedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                image.close();

                BitmapCrop(capturedBitmap);
//                // Update UI or perform any action with capturedBitmap
//                if (capturedBitmap != null) {
//                    String filename = "bitmap.png";
//                    FileOutputStream stream = null;
//                    try {
//                        stream = MainActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
//                        capturedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                        capturedBitmap.recycle();
//
//                        intent.putExtra("image", filename);
//                        startActivity(intent);
//                        IsLoading(false);
//                    } catch (FileNotFoundException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                }
                callback.AfterEvent();
            }
        });
    }
    private Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        Uri uri = null;
        try {
            File tempFile = File.createTempFile("temp_image", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            uri = Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uri;
    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
               // toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                //toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Flash is not available currently", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    void BitmapCrop(Bitmap image){
        Bitmap paddedImage = resizeWithPadding(image);
        Uri imageUri = getImageUriFromBitmap(getApplicationContext(),paddedImage);

        CropImage.activity(imageUri)
                .setAspectRatio(1, 1)
                .start(MainActivity.this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode != RESULT_CANCELED) {
            if (requestCode == 1) {
                //Toast.makeText(this, "Got", Toast.LENGTH_SHORT).show();
                Uri dat = data.getData();

                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);

                    String filename = "bitmap.png";
                    FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
                    image.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    BitmapCrop(image);

                    stream.close();
                    image.recycle();

//                    Uri imageUri = getImageUriFromBitmap(getApplicationContext(),paddedImage);
//
//                    CropImage.activity(imageUri)
//                            .setAspectRatio(1, 1)
//                            .start(MainActivity.this);

//
//
//                    Intent intent = new Intent(this, View_Results.class);
//                    intent.putExtra("image", filename);
//                    startActivity(intent);
//                    IsLoading(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                    String filename = "bitmap.png";
                    FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
                    image.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    stream.close();
                    image.recycle();


                    Intent intent = new Intent(this, View_Results.class);
                    intent.putExtra("image", filename);
                    startActivity(intent);
                    IsLoading(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        IsLoading(false);
        super.onActivityResult(requestCode, resultCode, data);
    }
    public Bitmap resizeWithPadding(Bitmap src) {

        // Determine the larger dimension (width or height)
        int largerDimension = Math.max(src.getWidth(), src.getHeight());

        // Calculate the padding required for both dimensions
        int paddingX = (largerDimension - src.getWidth()) / 2;
        int paddingY = (largerDimension - src.getHeight()) / 2;

        // Create a square output image with padding
        Bitmap outputImage = Bitmap.createBitmap(largerDimension, largerDimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputImage);

        // Draw a white background
        canvas.drawARGB(255, 0, 0, 0);

        // Draw the original image with padding
        canvas.drawBitmap(src, paddingX, paddingY, null);


        return outputImage;
    }
    void IsLoading(boolean isLoading){
        this.isLoading = isLoading;
        int visibility = View.INVISIBLE;
        if(isLoading) visibility = View.VISIBLE;
        layout_loading.setVisibility(visibility);
    }
    public interface Callback{
        void BeforeEvent();
        void AfterEvent();
    }
}