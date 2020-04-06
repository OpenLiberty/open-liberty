/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.upgrade.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.WebConnection;

/*
 * CDI test read listener implementation.
 */
public class CDITestReadListener implements ReadListener {
    private static final String LOG_CLASS_NAME = "CDITestReadListener";

    private void logEntry(String methodName) {
        CDITestHttpUpgradeHandler.logEntry(LOG_CLASS_NAME, methodName);
    }

    private void logExit(String methodName) {
        CDITestHttpUpgradeHandler.logExit(LOG_CLASS_NAME, methodName);
    }

    private void logInfo(String methodName, String text) {
        CDITestHttpUpgradeHandler.logInfo(LOG_CLASS_NAME, methodName, text);
    }

    private void logException(String methodName, Exception e) {
        CDITestHttpUpgradeHandler.logException(LOG_CLASS_NAME, methodName, e);
    }

    //

    protected CDITestReadListener(CDITestHttpUpgradeHandler upgradeHandler,
                                  WebConnection webConnection,
                                  List<String> queue) throws IOException {

        this.upgradeHandler = upgradeHandler;
        this.queue = queue;

        this.webConnection = webConnection;
        this.servletInput = this.webConnection.getInputStream(); // throws IOException

        // Assigning the read listener spawns a new thread,
        // which may run before or after this method exits.

        this.servletInput.setReadListener(this);
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
    private final ServletInputStream servletInput;

    //

    private final ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
    private boolean isTerminated = false;

    //

    @Override
    public void onDataAvailable() throws IOException {
        String methodName = "onDataAvailable";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");
        appendBeanData("RV"); // 'R' for "ReadListener"; 'V' for "Available"

        byte[] buffer = new byte[1024];
        int bytesRead;
        while (!isTerminated && ((bytesRead = doRead(buffer)) != -1)) { // throws IOException
            logInfo(methodName, "Read [ " + new String(buffer, 0, bytesRead) + " ]");

            if (bytesRead > 0) {
                if (buffer[bytesRead - 1] == CDITestHttpUpgradeHandler.TERMINATION_CHAR) {
                    logInfo(methodName, "Termination character detected");
                    isTerminated = true;
                    bytesRead--;
                }

                accumulator.write(buffer, 0, bytesRead);
            }
        }

        if (isTerminated) {
            String numbers = accumulator.toString();
            accumulator.reset();

            queue.add(CDITestHttpUpgradeHandler.NUMBERS_TAG);
            queue.add(numbers);
            queue.add(CDITestHttpUpgradeHandler.TERMINATION_TAG);

            logInfo(methodName, "Queue size [ " + queue.size() + " ]");

            upgradeHandler.setWriteListener(webConnection, queue);
        }

        logBeanActivity(methodName, "Exit");
        logBeanState(methodName);

        logExit(methodName);
    }

    private int doRead(byte[] buffer) throws IOException {
        String methodName = "doRead";
        if (!servletInput.isReady()) {
            logInfo(methodName, "Servlet input is not ready; read [ -1 ]");
            return -1;
        } else {
            int bytesRead = servletInput.read(buffer); // throws IOException
            logInfo(methodName, "Servlet input is ready; read [ " + bytesRead + " ]");
            return bytesRead;
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        String methodName = "onAllDataRead";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");

        appendBeanData("RA"); // 'R' for "ReadListener"; 'A' for "AllData"
        logBeanState(methodName);

        logBeanActivity(methodName, "Exit");

        logExit(methodName);
    }

    //

    @Override
    public void onError(final Throwable t) {
        String methodName = "onError";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");
        appendBeanData("RE"); // 'R' for "ReadListener"; 'E' for "Error"

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