package pebble.shrink;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Ivan on 14-03-2018.
 */

public class FileChooser extends ListActivity{

    private static final String TAG = "FileChooser";
    private File dir;
    private ArrayList<File> files;
    private FileChooserListAdapter adapter;

    private static final String DEFAULT_INITIAL_DIRECTORY="/sdcard";
    public static final String EXTRA_FILE_PATH="pebble.shrink.FileChooser.filepath";

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = layoutInflater.inflate(R.layout.fc_empty_view,null);
        ((ViewGroup)getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);

        dir = Environment.getExternalStorageDirectory();

        files = new ArrayList<>();

        adapter = new FileChooserListAdapter(this, files);
        setListAdapter(adapter);

        if(getIntent().hasExtra(EXTRA_FILE_PATH)){
            dir = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));
        }
    }

    @Override
    public void onResume(){
        refreshFileList();
        super.onResume();
    }

    private void refreshFileList(){
        files.clear();

        File[] tmpFiles = dir.listFiles();

        if(tmpFiles != null && tmpFiles.length > 0) {
            for (File f : tmpFiles) {
                if (f.isHidden()) {
                    continue;
                }
                files.add(f);
            }
            Collections.sort(files, new FileComparator());
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed(){
        if(dir.getParentFile() != null){
            dir = dir.getParentFile();
            refreshFileList();
        }
        super.onBackPressed();
    }

    @Override
    public void onListItemClick(ListView lv, View v, int pos, long id){
        super.onListItemClick(lv,v,pos,id);
        File tmp = (File)lv.getItemAtPosition(pos);
        if(tmp.isFile()){
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FILE_PATH,tmp.getAbsolutePath());
            setResult(RESULT_OK,intent);
            finish();
        } else {
            dir = tmp;
            refreshFileList();
        }
    }
    private static class FileChooserListAdapter extends ArrayAdapter<File>{

        private List<File> fileList;

        public FileChooserListAdapter(Context context, List<File> list){
            super(context,R.layout.fc_list_item,R.id.fc_list_text,list);
            fileList = list;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            View row = view;
            if(row == null){
                LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.fc_list_item,null);
            }

            final File file = fileList.get(position);
            if(file != null) {
                ImageView liImage = (ImageView) row.findViewById(R.id.fc_list_image);
                TextView liText = (TextView) row.findViewById(R.id.fc_list_text);
                Log.d(TAG, "NULL values litext " + liText + " liImage " + liImage);
                if(liText != null) {
                    liText.setText(file.getName());
                }
                if(liImage != null) {
                    if (file.isFile()) {
                        liImage.setImageResource(R.drawable.ic_menu_file);
                    } else {
                        liImage.setImageResource(R.drawable.ic_menu_folder);
                    }
                }
            }
            return row;
        }
    }

    private static class FileComparator implements Comparator<File>{

        public int compare(File f1, File f2){
            if(f1 == f2){
                return 0;
            }

            if(f1.isDirectory() && f2.isFile()){
                return -1;
            }

            if(f1.isFile() && f2.isDirectory()){
                return 1;
            }

            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }
}
