package pebble.shrink;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class Decompressor extends AppCompatActivity {
    private static final String TAG = "Decompressor";

    public static final String ACTION_MAIN = "decompressor.main";
    private static Button decompress,chooseFile;
    private static final int FILE_CHOOSE_REQUEST = 53;
    private static String filename;
    private static TextView tvFileName;
    public static Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.decompress_activity);
        handler = new Handler(Looper.getMainLooper());
        filename = null;
        decompress = (Button) findViewById(R.id.btDFdecompress);
        chooseFile = (Button) findViewById(R.id.btDFchooseFile);
        tvFileName = (TextView) findViewById(R.id.tvDFfileName);

        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            String scheme = intent.getScheme();
            if (scheme.equals(ContentResolver.SCHEME_FILE)) {
                Uri uri = intent.getData();
                Log.d(TAG, "File intent detected uri:" + uri.getPath());
                filename = uri.getPath();
                if(filename.matches(".*\\.dcrz") && (new File(filename)).exists()) {
                    tvFileName.setText(this.getString(R.string.df_filename, filename));
                }else{
                    NotificationUtils.errorDialog(this,getString(R.string.err_invalid_file));
                }
            }
        }

        chooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Decompressor.this, FileChooser.class);
                intent.setAction(FileChooser.FILE_CHOOSER_DCRZ);
                startActivityForResult(intent, FILE_CHOOSE_REQUEST);

            }
        });
    }

    /**
     * When user optionally selects a file
     * from the file chooser
     * @param requestCode custom code to verify file choose operation
     * @param resultCode is a file selected
     * @param intent result of file chooser activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CHOOSE_REQUEST) {
            if (resultCode == RESULT_OK) {
                filename = intent.getStringExtra(FileChooser.EXTRA_FILE_PATH);
                File tmp = new File(filename);
                if(tmp.exists()) {
                    tvFileName.setText(getString(R.string.df_filename, filename));
                }else{
                    Toast.makeText(this,getString(R.string.err_file_not_found),Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.d(TAG, "Invalid file");
        }
    }

    /**
     * Enable or disable widget
     * @param state enable or disable
     */
    public static void setWidgetEnabled(boolean state) {
        decompress.setEnabled(state);
    }

    /**
     * Start decompression of dcrz file
     * @param view Current view
     */
    public void onClickDecompress(View view) {
        if(filename != null) {
            Intent intent = new Intent(this, CompressionService.class);
            intent.setAction(CompressionUtils.ACTION_DECOMPRESS_LOCAL);
            intent.putExtra(CompressionUtils.cfile, filename);
            startService(intent);
        }else {
            Toast.makeText(this, "First choose a file !", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy(){
        NotificationUtils.removeNotification();
        super.onDestroy();
    }
}
