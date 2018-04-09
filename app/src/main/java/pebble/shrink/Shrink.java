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
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Shrink extends AppCompatActivity {

    private final String TAG = Shrink.this.getClass().getSimpleName();
    private static final int MULTI_PERMISSION_GROUP_ID = 475;

    static {
        System.loadLibrary("dcrz");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shrink_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = null;
            int i = 0;
            if (ActivityCompat.checkSelfPermission(Shrink.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions[i++] = Manifest.permission.ACCESS_COARSE_LOCATION;
            }
            if (ActivityCompat.checkSelfPermission(Shrink.this, Manifest.permission_group.STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions[i++] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                permissions[i++] = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            if(permissions != null){
                ActivityCompat.requestPermissions(Shrink.this, permissions, MULTI_PERMISSION_GROUP_ID);
            }
        }
    }

    /**
     * Compress a file. Master mode.
     * @param view Current view
     */
    public void onClickCompressFile(View view) {
        Intent intent = new Intent(Shrink.this, CompressFile.class);
        startActivity(intent);
    }

    /**
     * Slave mode.
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
                for(int perm : grantResults){
                    if(perm == PackageManager.PERMISSION_DENIED){
                        NotificationUtils.permErrorDialog(Shrink.this);
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
