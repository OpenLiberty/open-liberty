/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.targets.cache;

import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataApps;

public interface TargetCache_Factory {
    TargetCache_Options createOptions();

    TargetCache_Options getCacheOptions();
    void setOptions(TargetCache_Options options);

    TargetCacheImpl_DataApps getCache();
    void clearCache();
}
