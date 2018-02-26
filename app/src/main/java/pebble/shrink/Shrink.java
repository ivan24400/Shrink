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
    private AlertDialog.Builder choice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shrink_activity);

        Log.d(TAG, Build.DEVICE + "\n" + Build.HARDWARE + "\n" + Build.BRAND + "\n" + Build.MODEL + "\n");
    }

    public void onClickCompressFile(View view){
        Intent intent = new Intent(Shrink.this, CompressFile.class);
        startActivity(intent);
    }

    public void onClickShareDevice(View view){
        Intent intent = new Intent(Shrink.this, ShareResource.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                Intent intent = new Intent(this, Help.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
