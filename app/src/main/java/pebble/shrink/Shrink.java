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
            if (ActivityCompat.checkSelfPermission(Shrink.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(Shrink.this,Manifest.permission_group.STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                    Log.d(TAG,"permission not granted");
                ActivityCompat.requestPermissions(Shrink.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission_group.STORAGE},MULTI_PERMISSION_GROUP_ID);
            }
        }
    }

    public void onClickCompressFile(View view) {
        Intent intent = new Intent(Shrink.this, CompressFile.class);
        startActivity(intent);
    }

    public void onClickShareDevice(View view) {
        Log.d(TAG, "onclicksharedevice " + view.getId());
        Intent intent = new Intent(Shrink.this, ShareResource.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch(requestCode){
    case MULTI_PERMISSION_GROUP_ID:
        Log.d(TAG,"granted permissions: "+Arrays.toString(grantResults));
        if(grantResults.length == 2){

        }else{
            finish();
        }
}
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

}
