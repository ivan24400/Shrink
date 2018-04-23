package pebble.shrink;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;


import android.view.View;
import android.widget.Button;

import java.util.Arrays;

public class Shrink extends AppCompatActivity {

    private final String TAG = Shrink.this.getClass().getSimpleName();
    private static final int MULTI_PERMISSION_GROUP_ID = 475;
    private static Button btCompress;

    static {
        System.loadLibrary("dcrz");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shrink_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[3];
            int i = 0;
            if (ActivityCompat.checkSelfPermission(Shrink.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions[i++] = Manifest.permission.ACCESS_COARSE_LOCATION;
            }
            if (ActivityCompat.checkSelfPermission(Shrink.this, Manifest.permission_group.STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions[i++] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                permissions[i++] = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            if (permissions.length > 0) {
                ActivityCompat.requestPermissions(Shrink.this, permissions, MULTI_PERMISSION_GROUP_ID);
            }
        }
        btCompress = (Button) findViewById(R.id.btScompress);
        btCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btCompress.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Shrink.this);
                        builder.setTitle("Choose mode");
                        builder.setItems(R.array.operation_mode, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    onClickCompressFile();
                                } else {
                                    onClickDecompressFile();
                                }
                            }
                        });
                        builder.show();
                    }
                });
            }
        });
    }

    /**
     * Compress or decompress a file. (Master mode.)
     */
    private void onClickCompressFile() {
        Intent intent = new Intent(Shrink.this, CompressFile.class);
        startActivity(intent);
    }

    private void onClickDecompressFile() {
        Intent intent = new Intent(Shrink.this, Decompressor.class);
        intent.setAction(Decompressor.ACTION_MAIN);
        startActivity(intent);
    }

    /**
     * Slave mode.
     *
     * @param view Current view
     */
    public void onClickShareDevice(View view) {
        Log.d(TAG, "onclicksharedevice " + view.getId());
        Intent intent = new Intent(Shrink.this, ShareResource.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTI_PERMISSION_GROUP_ID:
                Log.d(TAG, "granted permissions: " + Arrays.toString(grantResults));
                for (int perm : grantResults) {
                    if (perm == PackageManager.PERMISSION_DENIED) {
                        NotificationUtils.errorDialog(Shrink.this, getString(R.string.err_permission_denied));
                    }
                }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

}
