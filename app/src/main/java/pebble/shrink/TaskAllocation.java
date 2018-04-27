package pebble.shrink;

import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskAllocation {

    private static long fileSize, fileSize_t;
    static List<MasterDevice> list;

    private static final String TAG = "TaskAllocation";

    public static void setFileSize(long size) {
        Log.d(TAG, "filesize " + size);
        fileSize = size;
    }

    /**
     * Allocate file chunk to slave devices
     * @return allocate error status
     */
    public boolean allocate() {
        list = DistributorService.deviceList;

        Collections.sort(list, new SortDevices());
        Log.d(TAG,"AFter sorting: "+list.toString());
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i);
            fileSize_t = fileSize_t + list.get(i).getFreeSpace();
        }

        if (fileSize_t < fileSize) {
            return false;
        }

        DistributorService.incrWorker();
        for (int index=0; index < list.size(); index++) {
            if ((fileSize - list.get(index).getFreeSpace()) <= 0) {
                Log.d(TAG, "index " + index + " final set allocated space " + fileSize);
                list.get(index).setAllocatedSpace(fileSize);
                list.get(index).setLastChunk(true);
                return true;
            } else {
                DistributorService.incrWorker();
                Log.d(TAG, "index " + index + " set allocated space " + (list.get(index).getFreeSpace()));
                list.get(index).setAllocatedSpace(list.get(index).getFreeSpace());
                fileSize = fileSize - list.get(index).getFreeSpace();
            }
        }
        return false;
    }

    /**
     * This class sorts devices in ascending battery and descending free space.
     */
    private class SortDevices implements Comparator<MasterDevice> {

        @Override
        public int compare(MasterDevice o1, MasterDevice o2) {

            if (o1.getBattery() < o2.getBattery()) {
                return -1;
            } else if (o1.getBattery() > o2.getBattery()) {
                return 1;
            } else {
                if (o1.getFreeSpace() < o2.getFreeSpace()) {
                    return 1;
                } else if (o1.getFreeSpace() > o2.getFreeSpace()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }
}
