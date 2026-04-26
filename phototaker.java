
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;

public class PhotoTaker {

    private static final String TAG = "PhotoTaker";
    // IMPORTANT: Replace with your computer's IP address on the local network.
    // Do not use "localhost" or "127.0.0.1" as the Android device has its own network interface.
    private static final String SERVER_URL = "ws://192.168.1.100:8080"; // <-- CHANGE THIS

    private Camera cam;
    private final Context context;
    private WebSocketClient wsClient;

    /**
     * The callback that receives the raw image data from the camera.
     */
    private final PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "Picture taken, data size: " + data.length + " bytes");
            
            // Send the image data back to the server
            if (wsClient != null && wsClient.isOpen()) {
                // We send the raw byte data
                wsClient.send(data);

                // For logging purposes in the web UI, send a confirmation message
                String logMessage = "log:Sent image data (" + data.length + " bytes)";
                wsClient.send(logMessage);
            }

            // Release the camera resources
            if (cam != null) {
                cam.release();
                cam = null;
            }
        }
    };

    /**
     * Constructor for the PhotoTaker.
     * @param context The Android application context.
     */
    public PhotoTaker(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Initializes the WebSocket client and connects to the server.
     */
    public void connectToRemote() {
        try {
            URI serverUri = new URI(SERVER_URL);
            wsClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "WebSocket connected to " + SERVER_URL);
                    // Inform the UI that the Android client is connected
                    sendToUI("log:Android PhotoTaker client connected.");
                }

                @Override
                public void onMessage(String message) {
                    Log.i(TAG, "Received command: " + message);
                    if ("take_photo".equalsIgnoreCase(message.trim())) {
                        // The UI thread is required for some camera operations,
                        // but takePhoto() should be safe if called from a background thread.
                        // For a real app, you would post this to the main thread handler.
                        takePhoto();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.w(TAG, "WebSocket disconnected. Code: " + code + ", Reason: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                }
            };
            
            Log.i(TAG, "Attempting to connect to WebSocket...");
            wsClient.connect();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebSocket connection", e);
        }
    }

    /**
     * Captures a photo using the device's camera.
     * @return true if the capture process was initiated successfully, false otherwise.
     */
    public boolean takePhoto() {
        // Check if a camera is available
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Log.e(TAG, "No camera feature found on this device.");
            sendToUI("log:E/PhotoTaker: No camera feature found.");
            return false;
        }

        // If a capture is already in progress, do nothing.
        if (cam != null) {
            Log.w(TAG, "Camera is already active.");
            return false;
        }

        try {
            Log.i(TAG, "Opening camera...");
            cam = Camera.open(0); // Open the first back-facing camera
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera.", e);
            sendToUI("log:E/PhotoTaker: Failed to open camera.");
            return false;
        }

        if (cam == null) {
            Log.e(TAG, "Camera.open() returned null.");
            return false;
        }

        try {
            // A surface texture or view is required for the preview, even if not shown.
            // Using a dummy SurfaceView for compatibility with the legacy Camera API.
            SurfaceView dummyView = new SurfaceView(context);
            cam.setPreviewDisplay(dummyView.getHolder());
            cam.startPreview();
            Log.i(TAG, "Camera preview started.");
            
            // Wait a moment for autofocus/autoexposure to stabilize before taking the picture
            cam.autoFocus((success, camera) -> {
                Log.i(TAG, "Autofocus completed: " + success);
                sendToUI("log:I/PhotoTaker: Taking picture...");
                camera.takePicture(null, null, pictureCallback);
            });

        } catch (IOException e) {
            Log.e(TAG, "Failed to set up camera preview.", e);
            if (cam != null) {
                cam.release();
                cam = null;
            }
            return false;
        }

        return true;
    }
    
    /**
     * Sends a log message back to the Web UI.
     * @param message The message to send.
     */
    private void sendToUI(String message) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(message);
        }
    }

    /**
     * Disconnects the WebSocket client and releases resources.
     */
    public void cleanup() {
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
        if (cam != null) {
            cam.release();
            cam = null;
        }
    }
}
