/*******************************************************************************
 * Copyright (c) 2014. 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.upgrade.handlers;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.WebConnection;

/*
 * CDI test write listener implementation.
 */
public class CDITestWriteListener implements WriteListener {
    public static final String LOG_CLASS_NAME = "CDITestWriteListener";

    private void logEntry(String methodName) {
        // System.out.println(LOG_CLASS_NAME + " " + methodName + " " + "ENTRY");
        CDITestHttpUpgradeHandler.logEntry(LOG_CLASS_NAME, methodName);
    }

    private void logExit(String methodName) {
        // System.out.println(LOG_CLASS_NAME + " " + methodName + " " + "EXIT");
        CDITestHttpUpgradeHandler.logExit(LOG_CLASS_NAME, methodName);
    }

    private void logInfo(String methodName, String text) {
        // System.out.println(LOG_CLASS_NAME + " " + methodName + " " + text);
        CDITestHttpUpgradeHandler.logInfo(LOG_CLASS_NAME, methodName, text);
    }

    private void logException(String methodName, Exception e) {
        // System.out.println(LOG_CLASS_NAME + " " + methodName + " " + "Exception");
        // e.printStackTrace(System.out);

        CDITestHttpUpgradeHandler.logException(LOG_CLASS_NAME, methodName, e);
    }

    //

    protected CDITestWriteListener(CDITestHttpUpgradeHandler upgradeHandler,
                                   WebConnection webConnection,
                                   List<String> queue) throws IOException {

        this.upgradeHandler = upgradeHandler;
        this.queue = queue;

        this.webConnection = webConnection;
        this.requestOutput = this.webConnection.getOutputStream(); // throws IOException

        // Assigning the write listener spawns a new thread,
        // which may run before or after this method exits.
        this.requestOutput.setWriteListener(this);
    }

    //

    private final CDITestHttpUpgradeHandler upgradeHandler;
    private final List<String> queue;

    private void logBeanActivity(String methodName, String text) {
        upgradeHandler.logBeanActivity(LOG_CLASS_NAME, methodName, text);
    }

    private void logBeanState(String methodName) {
        upgradeHandler.logBeanState(LOG_CLASS_NAME, methodName);
    }

    private void appendBeanData(String appendData) {
        upgradeHandler.appendBeanData(appendData);
    }

    //

    private final WebConnection webConnection;
    private final ServletOutputStream requestOutput;

    //

    // Called once initially, then again only if the output stream
    // becomes unready then ready again.  The tests expect only
    // the initial call.

    @Override
    public void onWritePossible() throws IOException {
        String methodName = "onWritePossible";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");
        appendBeanData("WP"); // 'W' for "WriteListener"; 'P' for "Possible"

        boolean nextIsNumbers = false;
        boolean isTerminated = false;

        String nextData;
        while (!isTerminated && (nextData = poll()) != null) {
            if (nextIsNumbers) {
                logInfo(methodName, "Send back numbers [ " + nextData + " ]");
                requestOutput.println(nextData);
                nextIsNumbers = false;

            } else if (nextData.equals(CDITestHttpUpgradeHandler.NUMBERS_TAG)) {
                logInfo(methodName, "Detected numbers tag [ " + nextData + " ]");
                nextIsNumbers = true;

            } else if (nextData.equals(CDITestHttpUpgradeHandler.TERMINATION_TAG)) {
                logInfo(methodName, "Detected termination tag [ " + nextData + " ]");
                isTerminated = true;

            } else {
                requestOutput.println("ERROR: Unexpected data [ " + nextData + " ]");

                onError(new IllegalStateException("Protocol Error [ " + nextData + " ]"));

                logExit(methodName);
                return;
            }
        }

        try {
            webConnection.close(); // throws Exception
        } catch (Exception e) {
            throw new IOException("Failed to close web connection", e);
        }

        logBeanActivity(methodName, "Exit");
        logBeanState(methodName);

        logExit(methodName);
    }

    private String poll() {
        String methodName = "poll";

        if (!requestOutput.isReady()) {
            logInfo(methodName, "Request output is not ready [ null ]");
            return null;

        } else if (queue.isEmpty()) {
            logInfo(methodName, "No queue data [ null ]");
            return null;

        } else {
            String data = queue.remove(0);
            logInfo(methodName, "Queue data [ " + data + " ] Remaining [ " + queue.size() + " ]");
            return data;
        }
    }

    @Override
    public void onError(Throwable t) {
        String methodName = "onError";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");
        appendBeanData("WE"); // 'W' for "WriteListener"; 'E' for "Error"

        try {
            webConnection.close(); // throws Exception
        } catch (Exception e) {
            logException(methodName, e);
        }

        logBeanActivity(methodName, "Exit");
        logBeanState(methodName);

        logExit(methodName);
    }
}
