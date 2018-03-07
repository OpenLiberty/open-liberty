package com.ibm.ws.collector.manager.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ibm.wsspi.collector.manager.BufferManager;

public class BufferManagerEMQHelper {

    protected volatile static boolean EMQRemovedFlag = false;
    private static final int EMQ_TIMER = 60 * 5 * 1000; //5 Minute timer
    protected static List<BufferManager> bufferManagerList= Collections.synchronizedList(new ArrayList<BufferManager>());

    public BufferManagerEMQHelper() {
    }

    public static void removeEMQByTimer(){
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        BufferManagerEMQHelper.removeEMQTrigger();
                    }
                },
                BufferManagerEMQHelper.EMQ_TIMER);
    }
    
    public synchronized static void removeEMQTrigger(){
            BufferManagerEMQHelper.EMQRemovedFlag=true;
            for(BufferManager i: BufferManagerEMQHelper.bufferManagerList) {
                ((BufferManagerImpl) i).removeEMQ();
            }
    }
    
    public static boolean getEMQRemovedFlag() {
        return EMQRemovedFlag;
    }

    public synchronized static void addBufferManagerList(BufferManager bufferManager) {
        bufferManagerList.add(bufferManager);
    }
}
