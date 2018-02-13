/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.artifact.fat_bvt.test.utils;

/**
 * Feature definition.  Used when preparing liberty servers.
 */
public class FATBundleDef {
    public final String sourceJarPath;

    public FATBundleDef(String sourceJarPath) {
        this.sourceJarPath = sourceJarPath;
    }
}
