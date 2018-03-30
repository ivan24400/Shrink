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


public class Decompressor extends AppCompatActivity {
    private static final String TAG="Decompressor";

    private static Button decompress;
    private static String filename;
    public static Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.decompress_activity);
        decompress = (Button)findViewById(R.id.btnDecompress);
        handler = new Handler(Looper.getMainLooper());

        Intent intent = getIntent();
        if(intent.getAction().equals(Intent.ACTION_VIEW)){
            String scheme = intent.getScheme();
            ContentResolver resolver = getContentResolver();
            if(scheme.equals(ContentResolver.SCHEME_FILE)){
                Uri uri = intent.getData();
                Log.d(TAG,"File intent detected uri:"+uri+" datastring:"+intent.getDataString()+" getdata:"+intent.getData());
            }
        }

    }

    public static void setWidgetEnabled(boolean state){
        decompress.setEnabled(state);
    }
    public void onClickDecompress(View view){
       /* Intent intent = new Intent(this,CompressionService.class);
        intent.putExtra(CompressionUtils.cfile,filename);
        startService(intent);
        */
    }
}
