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
package com.ibm.ws.anno.util.internal;

import com.ibm.wsspi.anno.util.Util_RelativePath;

/**
 *
 */
public class UtilImpl_RelativePath implements Util_RelativePath {

    public UtilImpl_RelativePath(String n_basePath, String n_relativePath, String n_fullPath) {
        this.n_basePath = n_basePath;
        this.n_relativePath = n_relativePath;
        this.n_fullPath = n_fullPath;
    }

    //

    protected final String n_basePath;
    protected final String n_relativePath;
    protected final String n_fullPath;

    @Override
    public String n_getBasePath() {
        return n_basePath;
    }

    @Override
    public String n_getRelativePath() {
        return n_relativePath;
    }

    @Override
    public String n_getFullPath() {
        return n_fullPath;
    }
}
