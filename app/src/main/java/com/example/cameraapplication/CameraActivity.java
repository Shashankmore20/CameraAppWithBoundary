package com.example.cameraapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    private PreviewView textureView;
    private View overlayView;
    private Button captureButton;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private Vibrator vibrator;

    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        overlayView = findViewById(R.id.overlayView);
        captureButton = findViewById(R.id.captureButton);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);



        startCamera();

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate for 100 milliseconds with default amplitude
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // For older devices
                    vibrator.vibrate(100);
                }
                capturePhoto();
                Toast.makeText(CameraActivity.this,"Image Saved", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // ON RESUME AFTER PERMISSIONS ARE GRANTED
    @Override
    protected void onResume() {
        super.onResume();
        // Start camera if permissions are granted, otherwise request permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // CHECK FOR PERMISSIONS

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
//                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // FUNCTION TO START THE CAMERA
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // FUNCTION TO START THE CAMERA
    private void bindCameraUseCases() {
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(textureView.getSurfaceProvider());

        // Release any previously bound use cases
        cameraProvider.unbindAll();

        imageCapture = new ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build();
        // BIND CAMERA TO LIFECYCLE
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    // CAPTURE THE IMAGE/PHOTO
    private void capturePhoto() {
        File photoFile = createImageFile();

        if (photoFile != null) {
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputOptions, executor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    // Photo saved successfully
                    Log.d(TAG, "Photo saved: " + photoFile.getAbsolutePath());

                    // Crop the captured image to the rectangle overlay area
                    cropImageToOverlay(photoFile);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    // Photo capture failed
                    Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                }
            });
        } else {
            Log.e(TAG, "Unable to create a file for the captured photo.");
        }
    }

    // Create the Full Image File. THIS IS THE FULL IMAGE AND NOT THE CROPPED.
    private File createImageFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String fileName = "IMG_" + sdf.format(System.currentTimeMillis()) + ".jpg";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                return null; // Failed to create directory
            }
        }

        File imageFile = new File(storageDir, fileName);
        return imageFile;
    }

    // Function to Crop the Image once it is taken.
    private void cropImageToOverlay(File photoFile) {
        Bitmap fullBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

        // Get the image orientation from the Exif data (if available)
        int imageOrientation = getImageOrientation(photoFile.getAbsolutePath());

        // Rotate the image to the correct orientation
        fullBitmap = rotateBitmap(fullBitmap, imageOrientation);

        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        int overlayLeft = overlayView.getLeft();
        int overlayTop = overlayView.getTop();
        int overlayWidth = overlayView.getWidth();
        int overlayHeight = overlayView.getHeight();

        Log.d(TAG, "View dimensions: width=" + viewWidth + ", height=" + viewHeight);
        Log.d(TAG, "Overlay coordinates: left=" + overlayLeft + ", top=" + overlayTop);
        Log.d(TAG, "Overlay dimensions: width=" + overlayWidth + ", height=" + overlayHeight);

        // Calculate the cropping coordinates
        int cropLeft = (int) ((overlayLeft / (float) viewWidth) * fullBitmap.getWidth());
        int cropTop = (int) ((overlayTop / (float) viewHeight) * fullBitmap.getHeight());
        int cropWidth = (int) ((overlayWidth / (float) viewWidth) * fullBitmap.getWidth());
        int cropHeight = (int) ((overlayHeight / (float) viewHeight) * fullBitmap.getHeight());

        Log.d(TAG, "Cropping coordinates: left=" + cropLeft + ", top=" + cropTop + ", width=" + cropWidth + ", height=" + cropHeight);

        // Crop the image
        Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, cropLeft, cropTop, cropWidth, cropHeight);

        //Save the cropped Image
        saveCroppedImageToGallery(croppedBitmap);
    }

    // Function to get the Image Orientation to check the orientation of it.
    private int getImageOrientation(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Since the Image was in 90 degree rotation, We are using this function to rotate the image back to 0 Degree
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);

            try {
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true
                );

                if (rotatedBitmap != bitmap) {
                    bitmap.recycle();
                }

                return rotatedBitmap;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    // Function to save the Cropped Image to a specific Location, Here to Gallery
    private void saveCroppedImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "cropped_image.jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + "CameraApp");

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream os = getContentResolver().openOutputStream(imageUri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.d(TAG, "Cropped image saved to gallery.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving cropped image to gallery: " + e.getMessage());
        }
    }
}
