package ru.project.waygo.ar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.DownloaadConfirmDialogActivity;
import ru.project.waygo.ProgressDialog;
import ru.project.waygo.R;
import ru.project.waygo.dto.ar.ArMetaInfoDTO;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.PointService;
import ru.project.waygo.utils.CacheUtils;

public class ArActivity extends BaseActivity {

    private RetrofitConfiguration retrofit;

    ModelRenderable lampPostRenderable;
    ModelRenderable copy;
    MaterialButton clearButton;
    MaterialButton takePict;


    MaterialButton fixButton;

    List<AnchorNode> nodeList = new ArrayList<>();

    Session session;
    Config config;
    private DownloaadConfirmDialogActivity dialog;
    private ProgressDialog progressDialog;

    private boolean isPlaced;
    private boolean isFixed;

    private float scale ;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        try {
            createSession();
        } catch (UnavailableDeviceNotCompatibleException e) {
            throw new RuntimeException(e);
        } catch (UnavailableSdkTooOldException e) {
            throw new RuntimeException(e);
        } catch (UnavailableArcoreNotInstalledException e) {
            throw new RuntimeException(e);
        } catch (UnavailableApkTooOldException e) {
            throw new RuntimeException(e);
        } catch (CameraNotAvailableException e) {
            throw new RuntimeException(e);
        }

        dialog = new DownloaadConfirmDialogActivity(ArActivity.this);
        progressDialog = new ProgressDialog(ArActivity.this);

        retrofit = new RetrofitConfiguration();

        Intent intent = getIntent();
        scale = intent.getFloatExtra("scale",0);
        String nameModel = intent.getStringExtra("model");
        String[] parts = nameModel.split("\\.");
        long id = 1l;

        configCamera();
        if(CacheUtils.getObjectFileCache(getApplicationContext(),parts[0]) != null){
            File file = null;
            try {
                file = File.createTempFile(parts[0],parts[1]);
                FileOutputStream fos = new FileOutputStream(file.getPath());
                fos.write(CacheUtils.getObjectFileCache(getApplicationContext(),parts[0]));
                fos.flush();
                fos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                buildModel(file,scale);
                Toast.makeText(getApplicationContext(),"Ok!",Toast.LENGTH_SHORT).show();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            final File localFile = File.createTempFile(parts[0], parts[1]);
            StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://waygodb-9ccc6.appspot.com/")
                    .child("ar/" +  nameModel);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            AtomicLong size = new AtomicLong();
            reference.getMetadata().addOnSuccessListener(storageMetadata -> {
                size.set(storageMetadata.getSizeBytes() / (1024 * 1024));
                Log.i("ARR", "onResponse: "+storageMetadata.getName());
                dialog.setText("\n" + storageMetadata.getName() + " (" + size + "MB).");
                dialog.preBuild(creteOnOkListener(reference, localFile,parts[0],storageMetadata.getSizeBytes()));
                dialog.show();
            }).addOnFailureListener(e -> Log.i("ARR", "onFailure: ." + e.getLocalizedMessage()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void buildModel(File file, float scale) throws IOException {
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.setMenuVisibility(true);
        

        ModelRenderable.builder().setSource(this,
                RenderableSource.builder().setSource(this, Uri.parse(file.getPath()),
                        RenderableSource.SourceType.GLB).setScale(scale).build()).build().thenAccept(renderable -> {
            lampPostRenderable = renderable;
        });
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (lampPostRenderable == null) {
                return;
            }

            if(nodeList.size() == 1){
                return;
            }

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            nodeList.add(anchorNode);

            TransformableNode lamp = new TransformableNode(arFragment.getTransformationSystem());
            lamp.getScaleController().setMaxScale(100f);

            lamp.setParent(anchorNode);
            lamp.setRenderable(lampPostRenderable);
            lamp.select();
            Frame frame = arFragment.getArSceneView().getArFrame();
            Camera camera = frame.getCamera();
            lamp.setOnTouchListener(new Node.OnTouchListener() {
                @Override
                public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
                    return true;
                }
            });


            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (isFixed) {
                    Log.i("arr", "buildModel: ");
                    return;
                }
                float tx = camera.getPose().tx();
                float ty = 0;
                float tz = camera.getPose().tz();

                lamp.setLocalPosition(new Vector3(tx, 0, tz));
                lamp.setLocalPosition(lamp.getLocalPosition().scaled(5));
                Quaternion quaternion = new Quaternion(new Vector3(tx, 0f, tz).scaled(5));
                lamp.setLocalRotation(quaternion);

            });

        });

        clearButton = findViewById(R.id.button_clean);
        clearButton.setOnClickListener(s -> {
            Scene scene = arFragment.getArSceneView().getScene();
            for (Node node : nodeList) {
                scene.removeChild(node);
            }
            nodeList = new ArrayList<>();
            isFixed = false;
        });
        fixButton = findViewById(R.id.button_fix);
        fixButton.setOnClickListener(s -> {
            Config configTemp = new Config(session);
            if (isFixed == true) {
                arFragment.getArSceneView().getScene().getView();
                configTemp.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                session.configure(configTemp);
                isFixed = false;
                return;
            }
            arFragment.getPlaneDiscoveryController().hide();
            configTemp.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
            session.configure(configTemp);
            isFixed = true;
        });
    }

    public void createSession() throws UnavailableDeviceNotCompatibleException, UnavailableSdkTooOldException, UnavailableArcoreNotInstalledException, UnavailableApkTooOldException, CameraNotAvailableException {
        session = new Session(this);
        config = new Config(session);
        config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
        session.configure(config);
    }

    public View.OnClickListener creteOnOkListener(StorageReference reference, File localFile, String name,long total) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDownloadTask task = reference.getFile(localFile);
                FileDownloadTask.TaskSnapshot snapshot = task.getSnapshot();
                progressDialog.preBuild(snapshot, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("ARR", "onClick: ");
                        snapshot.getTask().cancel();
                        progressDialog.close();
                    }
                });
                progressDialog.show();
                task.addOnSuccessListener(taskSnapshot -> {
                            progressDialog.close();
                            Toast.makeText(ArActivity.this, "OK!", Toast.LENGTH_SHORT).show();
                            try {
                                CacheUtils.cacheObjectFiles(getApplicationContext(),name, Files.readAllBytes(Paths.get(localFile.getPath())));
                                buildModel(localFile,scale);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(ArActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show())
                        .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                                long atMoment = snapshot.getBytesTransferred();
                                progressDialog.setProgress((int) ((100* ((float) (atMoment)/(float) (total)))));

                            }
                        });
                dialog.close();
            }
        };
    }

    private void configCamera() {
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        takePict = findViewById(R.id.button_photo);
        takePict.setOnClickListener(s -> {
            takePhoto();
        });
    }

    private String generateFilename() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File out = new File(appDir, filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename); ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    private void takePhoto() {
        final String filename = generateFilename();
        /*ArSceneView view = fragment.getArSceneView();*/
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        ArSceneView mSurfaceView = arFragment.getArSceneView();
        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(mSurfaceView, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(ArActivity.this, e.toString(), Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

            } else {
                Log.d("DrawAR", "Failed to copyPixels: " + copyResult);
                Toast toast = Toast.makeText(ArActivity.this, "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }


}
