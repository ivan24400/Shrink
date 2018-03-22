package pebble.shrink;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
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

public class Shrink extends AppCompatActivity {

    private final String TAG = Shrink.this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shrink_activity);
    }

    public void onClickCompressFile(View view) {
        Intent intent = new Intent(Shrink.this, CompressFile.class);
        startActivity(intent);
    }

    public void onClickShareDevice(View view) {
        Log.d(TAG,"onclicksharedevice "+view.getId());
        Intent intent = new Intent(Shrink.this, ShareResource.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"on destroy");
        super.onDestroy();
    }

}
