/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;


/**
 *
 * An implementation of a LogHandler that routes a message to the operator console.
 *
 */
public class MsgLogLogHandler implements WsLogHandler {

    /**
     * vector of errors
     */
    private final LoggingHandlerDiagnosticsVector savedDiagnostics = new LoggingHandlerDiagnosticsVector();

    /**
     * Used to track and handle registration of this service
     */
    private volatile ServiceRegistration<WsLogHandler> serviceRegistration;

    /**
     * Contains the native methods.
     */
    private final ZosLoggingBundleActivator zosLoggingBundleActivator;

    /**
     * Native FILE * to MSGLOG DD.
     * "volatile" because http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
     */
    private volatile long msgLogFilePtr = 0;

    /**
     * CTOR
     */
    public MsgLogLogHandler(ZosLoggingBundleActivator zosLoggingBundleActivator) {
        this.zosLoggingBundleActivator = zosLoggingBundleActivator;
    }

    /**
     * Helper to perform registration of this service.
     * Relies on synchronized caller.
     */
    protected synchronized void register(BundleContext bundleContext) {
        if (serviceRegistration != null) {
            return; // Already registered.
        }

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_VENDOR, "IBM");
        props.put("id", "MSGLOG");
        serviceRegistration = bundleContext.registerService(WsLogHandler.class, this, props);
    }

    /**
     * Helper to perform de/un-registration of this service
     * Relies on synchronized caller.
     */
    protected synchronized void unregister() {
        if (serviceRegistration != null) {

            // Close the MSGLOG file if opened
            if (msgLogFilePtr != 0) {
                try {
                    zosLoggingBundleActivator.closeFile(msgLogFilePtr);
                } catch (Exception e) {
                    // FFDC
                    savedDiagnostics.insertElementAtBegining(e.getMessage(), -1);
                } finally {
                    msgLogFilePtr = 0;
                }
            }

            serviceRegistration.unregister();
            serviceRegistration = null; // setting to null in case we want turn it back on later and check for null
        }
    }

    /**
     * @return the header-line (timestamp, threadId, etc) portion of the msg
     */
    protected String parseHeaderLine(String msg) {
        return (msg.length() > 96) ? msg.substring(0, 96) : msg;
    }

    /**
     * @return the message portion of the msg (minus the header line)
     */
    protected String parseMessage(String msg) {
        return (msg.length() > 96) ? msg.substring(96) : "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(RoutedMessage routedMessage) {

        String msg = routedMessage.getMessageLogFormat();
        if (msg != null) {

            try {
                publishMessage(msg);
            } catch (IOException ioe) {
                // FFDC
                savedDiagnostics.insertElementAtBegining(ioe.getMessage(), -1);
            }
        }

    }

    /**
     * @throws IOException
     */
    protected synchronized void publishMessage(String msg) throws IOException {

        // TODO: convert to english first?  will EBDIC conversion be messed up by other langs?
        // Split the msg across two lines.
        zosLoggingBundleActivator.writeFile(getMsgLogFilePtr(), parseHeaderLine(msg) + "\n");

        zosLoggingBundleActivator.writeFile(getMsgLogFilePtr(), "    " + parseMessage(msg) + "\n");
    }

    /**
     * @return DD:MSGLOG native FILE ptr. The FILE is opened on first call.
     */
    protected long getMsgLogFilePtr() throws IOException {
        // Caller must synchronize.
        if (msgLogFilePtr == 0) {
            msgLogFilePtr = zosLoggingBundleActivator.openFile();
        }

        return msgLogFilePtr;
    }

}
