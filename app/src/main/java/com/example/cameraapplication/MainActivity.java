//package com.example.cameraapplication;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.content.ContentResolver;
//import android.content.ContentValues;
//import android.content.Context;
//import android.content.res.Resources;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.ImageFormat;
//import android.graphics.Matrix;
//import android.graphics.Rect;
//import android.media.Image;
//import android.media.ImageReader;
//import android.media.MediaScannerConnection;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.SurfaceTexture;
//import android.hardware.camera2.*;
//import android.os.Environment;
//import android.provider.MediaStore;
//import android.util.DisplayMetrics;
//import android.util.Size;
//import android.util.TypedValue;
//import android.view.Surface;
//import android.view.TextureView;
//import android.view.View;
//import android.widget.Button;
//import android.widget.RelativeLayout;
//import android.widget.Toast;
//import android.util.SparseIntArray;
//import android.view.Surface;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import org.w3c.dom.Text;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.ByteBuffer;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Objects;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final int REQUEST_CAMERA_PERMISSION = 200;
//    private TextureView textureView;
//
//    private View cropView;
//    private CameraDevice cameraDevice;
//    private CameraCaptureSession cameraCaptureSessions;
//    private CaptureRequest.Builder captureRequestBuilder;
//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
//
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 0);
//        ORIENTATIONS.append(Surface.ROTATION_90, 90);
//        ORIENTATIONS.append(Surface.ROTATION_180, 180);
//        ORIENTATIONS.append(Surface.ROTATION_270, 270);
//    }
//
//    private String cameraId;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        textureView = findViewById(R.id.textureView);
//        cropView = findViewById(R.id.cropView);
//        Bitmap textureViewBitmap = textureView.getBitmap();
//        Button takePictureButton = findViewById(R.id.button_capture);
//        takePictureButton.setOnClickListener(v -> takePicture());
//
//        textureView.setSurfaceTextureListener(textureListener);
//    }
//
//    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            // Open your camera here
//            openCamera();
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//            // Transform your image captured size according to the surface width and height
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            return false;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        }
//    };
//
//    private void openCamera() {
//        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
//        try {
//            cameraId = manager.getCameraIdList()[0];
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
//            // Add checks for camera capabilities here
//
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{
//                        Manifest.permission.CAMERA,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE
//                }, REQUEST_CAMERA_PERMISSION);
//                return;
//            }
//            manager.openCamera(cameraId, stateCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
//        @Override
//        public void onOpened(@NonNull CameraDevice camera) {
//            //This is called when the camera is open
//            cameraDevice = camera;
//            createCameraPreview();
//        }
//
//        @Override
//        public void onDisconnected(@NonNull CameraDevice camera) {
//            cameraDevice.close();
//        }
//
//        @Override
//        public void onError(@NonNull CameraDevice camera, int error) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//    };
//
//    protected void createCameraPreview() {
//        try {
//            SurfaceTexture texture = textureView.getSurfaceTexture();
//            assert texture != null;
//            texture.setDefaultBufferSize(1920, 1080);
//            Surface surface = new Surface(texture);
//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.addTarget(surface);
//            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    // The camera is already closed
//                    if (null == cameraDevice) {
//                        return;
//                    }
//                    // When the session is ready, we start displaying the preview.
//                    cameraCaptureSessions = cameraCaptureSession;
//                    updatePreview();
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
//                }
//            }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static int getOrientation(int rotation, CameraCharacteristics characteristics) {
//        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//        // Get device rotation in degrees
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0: degrees = 0; break;
//            case Surface.ROTATION_90: degrees = 90; break;
//            case Surface.ROTATION_180: degrees = 180; break;
//            case Surface.ROTATION_270: degrees = 270; break;
//        }
//
//        int result;
//        // If the camera is front-facing, flip the image horizontally
//        if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
//            result = (sensorOrientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate for the mirror
//        } else {  // back-facing
//            result = (sensorOrientation - degrees + 360) % 360;
//        }
//        return result;
//    }
//
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
//
//        // Save a file: path for use with ACTION_VIEW intents
//        return image;
//    }
//
//    protected void updatePreview() {
//        if (null == cameraDevice) {
//            return;
//        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        try {
//            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private int[] translateCoordinates(Rect overlayRect, View textureView, Bitmap bitmap) {
//        int left = (int)((overlayRect.left / (float)textureView.getWidth()) * bitmap.getWidth());
//        int top = (int)((overlayRect.top / (float)textureView.getHeight()) * bitmap.getHeight());
//        int right = (int)((overlayRect.right / (float)textureView.getWidth()) * bitmap.getWidth());
//        int bottom = (int)((overlayRect.bottom / (float)textureView.getHeight()) * bitmap.getHeight());
//
//        return new int[]{left, top, right, bottom};
//    }
//
//    private Rect getOverlayRect(View textureView, View cropView) {
//        // Get the margins of the cropView
//        RelativeLayout.LayoutParams cropParams = (RelativeLayout.LayoutParams) cropView.getLayoutParams();
//        int marginTop = cropParams.topMargin;
//        int marginBottom = cropParams.bottomMargin;
//        int marginLeft = cropParams.leftMargin;
//        int marginRight = cropParams.rightMargin;
//
//
//        // Calculate the size and position of the cropView based on the margins
//        int left = marginLeft;
//        int top = marginTop;
//        int right = textureView.getWidth() - marginRight;
//        int bottom = textureView.getHeight() - marginBottom;
//
//        // Create a Rect with these coordinates
//        Rect overlayRect = new Rect(left, top, right, bottom);
//
//        return overlayRect;
//    }
//
//    private void takePicture() {
//        if (null == cameraDevice) {
//            return;
//        }
//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        try {
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
//            Size[] jpegSizes = null;
//            if (characteristics != null) {
//                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                        .getOutputSizes(ImageFormat.JPEG);
//            }
//            int width = 580;
//            int height = 600;
//            if (jpegSizes != null && jpegSizes.length > 0) {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//            }
//            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
//            List<Surface> outputSurfaces = new ArrayList<>(2);
//            outputSurfaces.add(reader.getSurface());
//            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//
//            final CaptureRequest.Builder captureBuilder =
//                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(reader.getSurface());
//            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//
//            // Orientation
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            int jpegOrientation = getOrientation(rotation, characteristics);
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
//
//            // Use a listener to get the photo data and save it
//            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                    Image image = null;
//                    try {
//                        image = reader.acquireLatestImage();
//                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                        byte[] bytes = new byte[buffer.capacity()];
//                        buffer.get(bytes);
//                        Bitmap fullBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        // Assume getBoxCoordinates() returns the coordinates of the box [left, top, right, bottom]
//                        Rect overlayRect = getOverlayRect(textureView, cropView);
//                        int[] boxCoordinates = translateCoordinates(overlayRect, textureView, fullBitmap);
//
//                        Matrix matrix = new Matrix();
//                        matrix.postRotate(jpegOrientation);
//
//                        Bitmap rotatedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);
//
//                        int cropWidth = Math.min(boxCoordinates[2] - boxCoordinates[0], rotatedBitmap.getWidth() - boxCoordinates[0]);
//                        int cropHeight = Math.min(boxCoordinates[3] - boxCoordinates[1], rotatedBitmap.getHeight() - boxCoordinates[1]);
//                        Bitmap croppedBitmap = Bitmap.createBitmap(rotatedBitmap,
//                                boxCoordinates[0],
//                                boxCoordinates[1],
//                                cropWidth,
//                                cropHeight);
//
//                        // Convert cropped Bitmap back to byte array
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                        byte[] croppedBytes = stream.toByteArray();
//
//                        save(croppedBytes);                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } finally {
//                        if (image != null) {
//                            image.close();
//                        }
//                    }
//                }
//
//                private void save(byte[] bytes) throws IOException {
//                    OutputStream output = null;
//                    Uri imageUri = null;
//                    try {
//                        ContentResolver contentResolver = getContentResolver();
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                            ContentValues contentValues = new ContentValues();
//                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "JPEG_" + System.currentTimeMillis());
//                            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + "YourAppName");
//
//                            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//                            output = contentResolver.openOutputStream(Objects.requireNonNull(imageUri));
//                        } else {
//                            // Save image for API < 29 with WRITE_EXTERNAL_STORAGE permission
//                            File image = createImageFile();
//                            imageUri = Uri.fromFile(image);
//                            output = new FileOutputStream(image);
//                        }
//
//                        if (output != null) {
//                            output.write(bytes);
//                        }
//                    } finally {
//                        if (output != null) {
//                            output.close();
//                        }
//                    }
//
//                    // Optional: scan the file so it appears in the gallery right away
//                    MediaScannerConnection.scanFile(MainActivity.this,
//                            new String[]{imageUri.toString()}, null,
//                            (path, uri) -> {
//                                // Log or handle scanned image uri if needed
//                            });
//                }
//            };
//
//            reader.setOnImageAvailableListener(readerListener, null);
//
//            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
//                @Override
//                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
//                                               @NonNull CaptureRequest request,
//                                               @NonNull TotalCaptureResult result) {
//                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
//                    createCameraPreview();
//                }
//            };
//
//            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession session) {
//                    try {
//                        session.capture(captureBuilder.build(), captureListener, null);
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                }
//            }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (null != cameraDevice) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                // close the app
//                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
//                finish();
//            }
//        }
//    }
//
//}