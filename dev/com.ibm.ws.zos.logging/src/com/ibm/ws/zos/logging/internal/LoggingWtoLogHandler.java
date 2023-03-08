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

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.zos.jni.NativeMethodUtils;

/**
 *
 * An implementation of a LogHandler that routes a message to the operator console.
 *
 */
public class LoggingWtoLogHandler implements WsLogHandler {

    /**
     * vector of errors
     */
    private final LoggingHandlerDiagnosticsVector savedDiagnostics = new LoggingHandlerDiagnosticsVector();

    /**
     * For translating to english.
     */
    private final LocaleHelper localeHelper = new LocaleHelper();

    /**
     * Used to track and handle registration of this service
     */
    private volatile ServiceRegistration<WsLogHandler> serviceRegistration;

    /**
     * Contains the native methods.
     */
    private final ZosLoggingBundleActivator zosLoggingBundleActivator;

    /**
     * CTOR
     */
    public LoggingWtoLogHandler(ZosLoggingBundleActivator zosLoggingBundleActivator) {
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
        props.put("id", "WTO");
        serviceRegistration = bundleContext.registerService(WsLogHandler.class, this, props);
    }

    /**
     * Helper to perform de/un-registration of this service
     * Relies on synchronized caller.
     */
    protected synchronized void unregister() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null; // setting to null in case we want turn it back on later and check for null
        }
    }

    /** {@inheritDoc} */
    @Override
    public void publish(RoutedMessage routedMessage) {
        String englishMsg = localeHelper.translateToEnglish(routedMessage.getFormattedMsg(),
                                                            routedMessage.getLogRecord());
        if (englishMsg != null) {
            int writeReturnCode = writeToOperatorConsole(NativeMethodUtils.convertToEBCDIC(englishMsg, false));
            // if there was an error save it
            if (writeReturnCode != 0) {
                savedDiagnostics.insertElementAtBegining(englishMsg, writeReturnCode);
            }
        }
    }

    /**
     * @param message: English message issue to the WTO.
     *
     *                     Sub function of the Publish() method, directly log message to WTO.
     */
    public void publish_eng(String message) {
        int writeReturnCode = writeToOperatorConsole(NativeMethodUtils.convertToEBCDIC(message, false));
        // if there was an error save it
        if (writeReturnCode != 0) {
            savedDiagnostics.insertElementAtBegining(message, writeReturnCode);
        }
    }

    /**
     * Call to native code to write the message to the operator console.
     *
     * @return 0 on success; non-zero on error
     */
    protected int writeToOperatorConsole(byte[] msg) {
        return zosLoggingBundleActivator.ntv_WriteToOperatorConsole(msg);
    }

}
