/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import java.util.EventListener;

/**
 * This interface provides a way for InstallKernel
 * to feedback information back to the caller.
 */
public interface InstallEventListener extends EventListener {

    /**
     * This interface is used by the listener to handle the event
     * which is notified by the InstallKernel.
     *
     * @param event Install progress event
     * @throws CancelException
     */
    public void handleInstallEvent(InstallProgressEvent event) throws Exception;
}
