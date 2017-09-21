/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2013
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.session.store.db;

/**
 *
 */
public class DatabaseMRHelper {


    public static final int SMALL=0;
    public static final int MEDIUM=1;
    public static final int LARGE=2;
    
    int size;
    String id;
    String propId;
    byte[] object;
    boolean useStream;
    String appName;
    
    public DatabaseMRHelper() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPropId() {
        return propId;
    }

    public void setPropId(String propId) {
        this.propId = propId;
    }

    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }

    public boolean isUseStream() {
        return useStream;
    }

    public void setUseStream(boolean useStream) {
        this.useStream = useStream;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
}
