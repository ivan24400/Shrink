package pebble.shrink;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;


public class DeviceListAdapter extends ArrayAdapter<Device> {

    private String TAG = "DeviceListAdapter";
    private List<Device> devices;
    private int resource;

    public DeviceListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Device> objects) {
        super(context, resource, objects);
        this.devices = objects;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resource, null);
        }
        Device device = devices.get(position);
        if (device != null) {
            TextView deviceName = (TextView) view.findViewById(R.id.tvDLdeviceName);
            TextView deviceDetail = (TextView) view.findViewById(R.id.tvDLdeviceDetails);
            if (deviceName != null && deviceDetail != null) {
                deviceName.setText(device.getDeviceName());
                deviceDetail.setText("CPU: " + device.getCpu() + "\nFreeSpace: " + device.getFreeSpace() + "\nBattery: " + device.getBattery());
            } else {
                Log.d(TAG, "textviews are null");
            }
        }
        return view;
    }
}
