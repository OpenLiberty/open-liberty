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
package com.ibm.ws.artifact.fat_bvt.bundle.custom;

import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * Simple NO-OP notifier used by custom containers.
 */
public class CustomNotifier implements ArtifactNotifier {
    protected static final CustomNotifier INSTANCE = new CustomNotifier();

    @Override
    public boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject)
        throws IllegalArgumentException {
        return false;
    }

    @Override
    public boolean removeListener(ArtifactListener listenerToRemove) {
        return false;
    }

    @Override
    public boolean setNotificationOptions(long interval, boolean useMBean) {
        return false;
    }
}
