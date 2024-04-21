package ru.project.waygo.ar;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
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
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import ru.project.waygo.R;
import ru.project.waygo.dto.ar.ArMetaInfoDTO;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.PointService;

public class ArActivity extends AppCompatActivity {

    private RetrofitConfiguration retrofit;

    ModelRenderable lampPostRenderable;
    ModelRenderable copy;
    MaterialButton clearButton;
    MaterialButton takePict;

    MaterialButton fixButton;

    List<AnchorNode> nodeList = new ArrayList<>();

    Session session;

    private boolean isPlaced;
    private boolean isFixed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


        retrofit = new RetrofitConfiguration();
        setContentView(R.layout.activity_ar);

        Intent intent = getIntent();
        long id = 1l;
        PointService pointService = retrofit.createService(PointService.class);

        Call<ArMetaInfoDTO> ar = pointService.getArMetaInfo(id);
        ar.enqueue(new Callback<ArMetaInfoDTO>() {
            @Override
            public void onResponse(Call<ArMetaInfoDTO> call, Response<ArMetaInfoDTO> response) {


                String[] parts = response.body().getKey().split("\\.");
                try {
                    final File localFile = File.createTempFile(parts[0], parts[1]);
                    StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://waygodb-9ccc6.appspot.com/").child("ar/pistol.glb");
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    AtomicLong size = new AtomicLong();
                    DownloadConfirmDialog dialog = new DownloadConfirmDialog(ArActivity.this);
                    reference.getMetadata().addOnSuccessListener(storageMetadata -> {
                        size.set(storageMetadata.getSizeBytes() / (1024 * 1024));

                        dialog.setText("\n" + reference.getName() + " (" + size + "MB).");
                        dialog.preBuild(creteOnOkListener(reference, localFile), createOnNoListener(), getApplicationContext());

                        dialog.show(getSupportFragmentManager(), " ");
                        Log.i("ARR", "onResponse: OK= " + storageMetadata.getSizeBytes());
                    }).addOnFailureListener(e -> Log.i("ARR", "onFailure: ." + e.getLocalizedMessage()));


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Call<ArMetaInfoDTO> call, Throwable t) {

            }
        });
        configCamera();

    }


    private void buildModel(File file) {
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.setMenuVisibility(true);

        ModelRenderable.builder().setSource(this,
                RenderableSource.builder().setSource(this, Uri.parse(file.getPath()),
                RenderableSource.SourceType.GLB).setScale(10).build()).build().thenAccept(renderable -> {
            lampPostRenderable= renderable;
            copy = lampPostRenderable.makeCopy();
            copy.getMaterial().setFloat3("baseColorTint",
                    new Color(android.graphics.Color.rgb(128,128,128)));;
        });
        ArSceneView arSceneView = arFragment.getArSceneView();
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

            if (lampPostRenderable == null) {
                return;
            }
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            nodeList.add(anchorNode);

            TransformableNode lamp = new TransformableNode(arFragment.getTransformationSystem());
            lamp.getScaleController().setMaxScale(15f);
            lamp.setParent(anchorNode);
            lamp.setRenderable(copy);
            lamp.select();
            Frame frame = arFragment.getArSceneView().getArFrame();
            Camera camera = frame.getCamera();
            lamp.setOnTouchListener(new Node.OnTouchListener() {
                @Override
                public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
                    Log.i("ARR", "onTouch: ");
                    return true;
                }
            });


            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if(isFixed){
                    return;
                }
                Log.i("ARR", "onUpdate: " + camera.getDisplayOrientedPose());
                float tx = camera.getPose().tx() ;
                float ty = 0;
                float tz = camera.getPose().tz() ;

                float qx = camera.getPose().qx();
                float qz = camera.getPose().qz();
                lamp.setLocalPosition(new Vector3(tx,0,tz));
                lamp.setLocalPosition(lamp.getLocalPosition().scaled(5));
                Quaternion quaternion = new Quaternion(new Vector3(tx,0f, tz).scaled(5));
                lamp.setLocalRotation(quaternion);

            });

        });

        clearButton = findViewById(R.id.button_clean);
        clearButton.setOnClickListener(s -> {
            Scene scene = arFragment.getArSceneView().getScene();
            for (Node node : nodeList) {
                scene.removeChild(node);
            }
        });
        fixButton = findViewById(R.id.button_fix);
        fixButton.setOnClickListener(s->{
            if(isFixed == true){
                isFixed = false;
                return;
            }
            isFixed = false;
        });
    }

    private void extracted(ArFragment arFragment) {
        arFragment.getArSceneView().getScene().addOnUpdateListener(s->{
            if(isPlaced){
                return;
            }
            Frame frame = arFragment.getArSceneView().getArFrame();
            float x = frame.getCamera().getPose().qx() ;
            float y = frame.getCamera().getPose().qy();
            float z = frame.getCamera().getPose().qz() ;
            Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
            for(Plane plane : planes){
                if(plane.getTrackingState() == TrackingState.TRACKING){
                    Anchor anchor = plane.createAnchor(frame.getCamera().getPose());
                    AnchorNode node = new AnchorNode(anchor);
                    TransformableNode lamp = new TransformableNode(arFragment.getTransformationSystem());
                    lamp.getScaleController().setMaxScale(15f);

                    lamp.setParent(node);
                    lamp.setRenderable(copy);
                    lamp.select();
                    arFragment.getArSceneView().getScene().addChild(node);
                    isPlaced = true;
                }
            }
        });



    }

    public void createSession() throws UnavailableDeviceNotCompatibleException, UnavailableSdkTooOldException, UnavailableArcoreNotInstalledException, UnavailableApkTooOldException, CameraNotAvailableException {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        session = new Session(this);
        Config config = new Config(session);
        config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
        session.configure(config);
    }

    public DialogInterface.OnClickListener creteOnOkListener(StorageReference reference, File localFile) {
        return (dialog, which) -> reference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(ArActivity.this, "OK!", Toast.LENGTH_SHORT).show();
            buildModel(localFile);
        }).addOnFailureListener(e -> Toast.makeText(ArActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
    }

    public DialogInterface.OnClickListener createOnNoListener() {
        return (dialog, which) -> {
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

        File out = new File(filename);
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
        ;
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
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Photo saved", Snackbar.LENGTH_LONG);
                snackbar.setAction("Open in Photos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(ArActivity.this, ArActivity.this.getPackageName() + ".ar.codelab.name.provider", photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.show();
            } else {
                Log.d("DrawAR", "Failed to copyPixels: " + copyResult);
                Toast toast = Toast.makeText(ArActivity.this, "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }


}
