package pebble.shrink;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskAllocation {

    private static long fileSize,fileSize_t;
    private static List<MasterDevice> list;
    private static int index = 0;

    public static void setFileSize(long size){
        fileSize = size;
    }

    public List<MasterDevice> getDeviceList(){
        return list;
    }

    public long getBase(int rank){
        fileSize_t = 0;
        if(list != null){
            for(int i=0; i<list.size(); i++){
                if(list.get(i).getRank() == rank){
                    for(int j=0; j<i; j++){
                        fileSize_t = fileSize_t + list.get(j).getAllocatedSpace();
                    }
                    return fileSize_t+1;
                }
            }
        }
        return -1;
    }

    public boolean allocate(List<MasterDevice> tmp){
        list = tmp;
        Collections.sort(list,new SortDevices());

        for(int i=0; i< list.size(); i++){
            list.get(i).setRank(i);
            fileSize_t = fileSize_t + list.get(i).getFreeSpace();
        }

        if(fileSize_t < fileSize){ return false; }

        for(;index < list.size(); index++){
            if((fileSize - list.get(index).getFreeSpace()) <= 0){
                list.get(index).setAllocatedSpace(fileSize);
                return true;
            }else{
                list.get(index).setAllocatedSpace(fileSize - list.get(index).getFreeSpace());
                fileSize = fileSize - list.get(index).getFreeSpace();
            }
        }
        return false;
    }

    public boolean reallocate(int rank){
        for(MasterDevice device : list){
            if(rank == device.getRank()){
                if(list.get(++index).getFreeSpace() >= device.getAllocatedSpace()){
                    list.get(index).setAllocatedSpace(device.getAllocatedSpace());
                    list.get(index).setRank(rank);
                    return true;
                }
            }
        }
        return false;
    }

    private class SortDevices implements Comparator<MasterDevice> {

        @Override
        public int compare(MasterDevice o1, MasterDevice o2) {

            if(o1.getBattery() < o2.getBattery()){
                return -1;
            }else if(o1.getBattery() > o2.getBattery()){
                return 1;
            }else{
                if(o1.getFreeSpace() < o2.getFreeSpace()){
                    return 1;
                }else if(o1.getFreeSpace() > o2.getFreeSpace()){
                    return -1;
                }else{
                    return 0;
                }
            }
        }
    }
}
