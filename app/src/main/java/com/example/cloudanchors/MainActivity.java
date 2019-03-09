package com.example.cloudanchors;

import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    public String roomName = "Poker";
    private CustomArFragment arFragment;
    private Anchor anchor;
    private boolean isPlaced = false;
    private StorageManager storageManager;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private AppAnchorState appAnchorState = AppAnchorState.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AnchorId", MODE_PRIVATE);
        editor = prefs.edit();

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {

            if (!isPlaced) {
                anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                appAnchorState = AppAnchorState.HOSTING;
                showToast("Hosting...");
                createModel(anchor);

                isPlaced = true;
            }
        });

        storageManager = new StorageManager(this);

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            if (appAnchorState != AppAnchorState.HOSTING)
                return;
            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();

            if (cloudAnchorState.isError()) {
                showToast(cloudAnchorState.toString());
            } else if (cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
                storageManager.nextShortCode(
                        (shortCode) -> {
                            if (shortCode == null) {
                                showToast("Could not obtain a short code.");
                                return;
                            }
                            storageManager.storeUsingShortCode(shortCode, anchor.getCloudAnchorId());
                            showToast("Anchor hosted successfully! Cloud Short Code: " + shortCode);
                        });
                appAnchorState = AppAnchorState.HOSTED;
            }
        });

        Button roomButton = findViewById(R.id.room_button);
        roomButton.setOnClickListener(
                v -> {
                    EditText editText = new EditText(this);
                    editText.setHint("Enter a text");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setView(editText);
                    builder.setPositiveButton("Show text", (dialog, which) -> {
                        roomName = editText.getText().toString();
                        storageManager.setRoomName(this, roomName);
                        showToast(roomName);
                    }).show();
                });

        Button resolveButton = findViewById(R.id.resolve_button);
        resolveButton.setOnClickListener(
                (unusedView) -> {
                    if (anchor != null) {
                        showToast("Please clear anchor first.");
                        return;
                    }
                    ResolveDialogFragment dialog = new ResolveDialogFragment();
                    dialog.setOkListener(this::onResolveOkPressed);
                    dialog.show(getSupportFragmentManager(), "Resolve");
                });

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(
                (unusedView) -> {

                }
        );
    }

    private void onResolveOkPressed(String dialogValue) {
//        int shortCode = Integer.parseInt(dialogValue);
//        String cloudAnchorId = storageManager.getCloudAnchorID(this, shortCode);
//            Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
//            createModel(resolvedAnchor);
//            showToast( "Now resolving anchor...");
//            appAnchorState = AppAnchorState.RESOLVING;

        int shortCode = Integer.parseInt(dialogValue);
        storageManager.getCloudAnchorID(
                shortCode,
                (cloudAnchorId) -> {
                    Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
                    createModel(resolvedAnchor);
                    showToast("Now resolving anchor...");
                    appAnchorState = AppAnchorState.RESOLVING;
                });
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void createModel(Anchor anchor) {

        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("ArcticFox_Posed.sfb"))
                .build()
                .thenAccept(modelRenderable -> placedModel(anchor, modelRenderable));
    }

    private void placedModel(Anchor anchor, ModelRenderable modelRenderable) {

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

}
