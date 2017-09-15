/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class is used by disk cache to determine the disk cache size and disk cache size in GB limit.
 */
public class DiskCacheSizeInfo {

    private static TraceComponent tc = Tr.register(DiskCacheSizeInfo.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private static final boolean IS_UNIT_TEST             = false;

    // max file size
    public final static long GB_SIZE = 1024l * 1024l * 1024l;
    public final static long MB_SIZE = 1024l * 1024l;

    public static final int TYPE_CACHE_DATA              = 1;
    public static final int TYPE_DEPENDENCY_ID_DATA      = 2;
    public static final int TYPE_TEMPLATE_DATA           = 3;

    private String cacheName;

    protected int diskCacheSizeLimit = 0;
    protected int diskCacheSizeHighLimit = 0;
    protected int diskCacheSizeLowLimit  = 0;
    protected int highThreshold = 0;
    protected int lowThreshold = 0;

    protected long diskCacheEntrySizeInBytesLimit = 0;
    protected int diskCacheSizeInGBLimit = 0;
    private long diskCacheSizeInBytesLimit = 0;
    private long diskCacheSizeInBytesHighLimit = 0;
    private long diskCacheSizeInBytesLowLimit  = 0;

    protected int currentDataGB = 0;
    protected int currentDependencyIdGB = 0;
    protected int currentTemplateGB = 0;
    protected boolean allowOverflow = false;

    public DiskCacheSizeInfo(String cacheName) {
        this.cacheName = cacheName;
    }

    public void initialize(int diskCacheSizeLimit, int diskCacheEntrySizeInMBLimit, int diskCacheSizeInGBLimit, int dataGB, int dependencyIdGB, int templateGB, int highThreshold, int lowThreshold) {
        final String methodName = "initialize()";

        this.diskCacheSizeLimit = diskCacheSizeLimit;
        this.diskCacheSizeInGBLimit = diskCacheSizeInGBLimit;
        this.diskCacheEntrySizeInBytesLimit = diskCacheEntrySizeInMBLimit * MB_SIZE;
        this.highThreshold = highThreshold;
        this.lowThreshold = lowThreshold;
        this.currentDataGB = dataGB;
        this.currentDependencyIdGB = dependencyIdGB;
        this.currentTemplateGB = templateGB;

        if (this.diskCacheSizeLimit > 0) {
            this.diskCacheSizeHighLimit = (this.diskCacheSizeLimit * this.highThreshold) / 100;
            this.diskCacheSizeLowLimit  = (this.diskCacheSizeLimit * this.lowThreshold)  / 100;
            traceDebug(methodName, "cacheName=" + this.cacheName + " diskCacheSizeLimit=" + this.diskCacheSizeLimit + " diskCacheSizeHighLimit=" + this.diskCacheSizeHighLimit + " diskCacheSizeLowLimit=" + this.diskCacheSizeLowLimit);
        }
        if (this.diskCacheSizeInGBLimit > 0) {
            this.diskCacheSizeInBytesLimit = (diskCacheSizeInGBLimit - this.currentDependencyIdGB - this.currentTemplateGB) * GB_SIZE;
            this.diskCacheSizeInBytesHighLimit = (this.diskCacheSizeInBytesLimit * (long)this.highThreshold) / 100l;
            this.diskCacheSizeInBytesLowLimit  = (this.diskCacheSizeInBytesLimit * (long)this.lowThreshold ) / 100l;
            traceDebug(methodName, "cacheName=" + this.cacheName + " diskCacheSizeInBytesLimit=" + this.diskCacheSizeInBytesLimit + " diskCacheSizeInBytesHighLimit=" + this.diskCacheSizeInBytesHighLimit + " diskCacheSizeInBytesLowLimit=" + this.diskCacheSizeInBytesLowLimit + " currentDataGB=" + currentDataGB + " currentDependencyIdGB=" + currentDependencyIdGB + " currentTemplateGB=" + currentTemplateGB);
        }
    }

    /**
     * Call this method to check and add volume. If current disk cache size in GB plus request of new volume is over limit,
     * it will return false. 
     *
     * @param type - type of volume (TYPE_CACHE_DATA, TYPE_DEPENDENCY_ID_DATA, TYPE_TEMPLATE_DATA)
     * @param vol  - volume
     * @return boolean false means no more volume can be added.
     */
    public synchronized boolean checkAddVolume(int type, int vol) {
        final String methodName = "checkAddVolume()";
        if (this.diskCacheSizeInGBLimit > 0) {
            boolean bCalculateHighAndLow = false;
            int minGB = 0;
            switch (type) {
                case TYPE_CACHE_DATA:
                    if (this.currentDataGB >= (vol + 1)) {
                        return true;
                    }
                    if (this.allowOverflow) {
                        traceDebug(methodName, "cacheName=" + this.cacheName + " allow overflow for data file vol=" + vol);
                        return true;
                    }
                    minGB = this.currentDataGB + this.currentDependencyIdGB + this.currentTemplateGB + 1;
                    if (this.diskCacheSizeInGBLimit >= minGB) {
                        this.currentDataGB++;
                    } else {
                        traceDebug(methodName, "data over limit cacheName=" + this.cacheName + " add type=" + type + " diskCacheSizeInGBLimit=" + this.diskCacheSizeInGBLimit + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB); 
                        return false;
                    }
                    break;
                case TYPE_DEPENDENCY_ID_DATA:
                    if (this.currentDependencyIdGB >= (vol + 1)) {
                        return true;
                    }
                    if (this.allowOverflow) {
                        traceDebug(methodName, "cacheName=" + this.cacheName + " allow overflow for dependency file id vol=" + vol);
                        return true;
                    }
                    minGB = this.currentDataGB + this.currentDependencyIdGB + this.currentTemplateGB + 1;
                    if (this.diskCacheSizeInGBLimit >= minGB) {
                        this.currentDependencyIdGB++;
                        bCalculateHighAndLow = true;
                    } else {
                        traceDebug(methodName, "depId over limit cacheName=" + this.cacheName + " add type=" + type + " diskCacheSizeInGBLimit=" + this.diskCacheSizeInGBLimit + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB); 
                        return false;
                    }
                    break;
                case TYPE_TEMPLATE_DATA:
                    if (this.currentTemplateGB >= (vol + 1)) {
                        return true;
                    }
                    if (this.allowOverflow) {
                        traceDebug(methodName, "cacheName=" + this.cacheName + " allow overflow for template file vol=" + vol);
                        return true;
                    }
                    minGB = this.currentDataGB + this.currentDependencyIdGB + this.currentTemplateGB + 1;
                    if (this.diskCacheSizeInGBLimit >= minGB) {
                        this.currentTemplateGB++;
                        bCalculateHighAndLow = true;
                    } else {
                        traceDebug(methodName, "template over limit cacheName=" + this.cacheName + " add type=" + type + " diskCacheSizeInGBLimit=" + this.diskCacheSizeInGBLimit + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB); 
                        return false;
                    }
                    break;
            }
            if (bCalculateHighAndLow) {
                this.diskCacheSizeInBytesLimit = (diskCacheSizeInGBLimit - this.currentDependencyIdGB - this.currentTemplateGB) * GB_SIZE;
                this.diskCacheSizeInBytesHighLimit = (this.diskCacheSizeInBytesLimit * (long)this.highThreshold) / 100l;
                this.diskCacheSizeInBytesLowLimit  = (this.diskCacheSizeInBytesLimit * (long)this.lowThreshold)  / 100l;
                traceDebug(methodName, "new limit: cacheName=" + this.cacheName + " add type=" + type + " diskCacheSizeInBytesLimit=" + this.diskCacheSizeInBytesLimit + " diskCacheSizeInBytesHighLimit=" + this.diskCacheSizeInBytesHighLimit + " diskCacheSizeInBytesLowLimit=" + this.diskCacheSizeInBytesLowLimit);
            }
            traceDebug(methodName, "cacheName=" + this.cacheName + " add volume=" + vol + " type=" + type + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB);
        }
        return true;
    }

    /**
     * Call this method to reset the disk cache size info after disk clear
     */
    public synchronized void reset() {
        final String methodName = "reset()";
        this.currentDataGB = 1;
        if (this.currentDependencyIdGB > 0) {
            this.currentDependencyIdGB = 1;
        }
        if (this.currentTemplateGB > 0) {
            this.currentTemplateGB = 1;
        }
        if (this.diskCacheSizeInGBLimit > 0) {
            this.diskCacheSizeInBytesLimit = (diskCacheSizeInGBLimit - this.currentDependencyIdGB - this.currentTemplateGB) * GB_SIZE;
            this.diskCacheSizeInBytesHighLimit = (this.diskCacheSizeInBytesLimit * (long)this.highThreshold) / 100l;
            this.diskCacheSizeInBytesLowLimit  = (this.diskCacheSizeInBytesLimit * (long)this.lowThreshold ) / 100l;
            traceDebug(methodName, "cacheName=" + this.cacheName + " diskCacheSizeInBytesLimit=" + this.diskCacheSizeInBytesLimit + " diskCacheSizeInBytesHighLimit=" + this.diskCacheSizeInBytesHighLimit + " diskCacheSizeInBytesLowLimit=" + this.diskCacheSizeInBytesLowLimit  + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB);
        }
    }

    public synchronized long getDiskCacheSizeInBytesHighLimit() {
        return this.diskCacheSizeInBytesHighLimit;
    }

    public synchronized long getDiskCacheSizeInBytesLowLimit() {
        return this.diskCacheSizeInBytesLowLimit;
    }

    public synchronized long getDiskCacheSizeInBytesLimit() {
        return this.diskCacheSizeInBytesLimit;
    }

    public long getDiskCacheSizeHighLimit() {
        return this.diskCacheSizeHighLimit;
    }

    public long getDiskCacheSizeLowLimit() {
        return this.diskCacheSizeLowLimit;
    }

    public long getDiskCacheSizeLimit() {
        return this.diskCacheSizeLimit;
    }

    public long getDiskCacheEntrySizeInBytesLimit() {
        return this.diskCacheEntrySizeInBytesLimit;
    }

    public void displayDiskCacheInfo() {
        final String methodName = "displayDiskCacheInfo()";
        if (this.diskCacheSizeLimit > 0) {
            traceDebug(methodName, "cacheName=" + this.cacheName + " diskCacheSizeLimit=" + this.diskCacheSizeLimit + " diskCacheSizeHighLimit=" + this.diskCacheSizeHighLimit + " diskCacheSizeLowLimit=" + this.diskCacheSizeLowLimit);
        }
        if (this.diskCacheSizeInGBLimit > 0) {
            traceDebug(methodName, "cacheName=" + this.cacheName + " diskCacheSizeInBytesLimit=" + this.diskCacheSizeInBytesLimit + " diskCacheSizeInBytesHighLimit=" + this.diskCacheSizeInBytesHighLimit + " diskCacheSizeInBytesLowLimit=" + this.diskCacheSizeInBytesLowLimit  + " currentDataGB=" + this.currentDataGB + " currentDependencyIdGB=" + this.currentDependencyIdGB + " currentTemplateGB=" + this.currentTemplateGB);
        }
    }

    public boolean doYield(int currentDiskCacheSize, long currentDiskCacheSizeInBytes) {
    	boolean rc = true;
        if (this.diskCacheSizeLimit > 0 && currentDiskCacheSize > this.diskCacheSizeHighLimit) {
        	rc = false;
        }
    	
        if (this.diskCacheSizeInGBLimit > 0 && currentDiskCacheSizeInBytes > this.diskCacheSizeInBytesHighLimit) {
        	rc = false;
        }
    	return rc;
    }
    
    private void traceDebug(String methodName, String message){
        if (IS_UNIT_TEST) {
            System.out.println(this.getClass().getName() + "." + methodName + " " + message);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " " + message);
            }
        }
    }
}