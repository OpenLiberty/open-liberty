/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The purpose of this class is to track SSL handshake errors as they occur
 * in inbound chains. The goal is to print an error message to the log, but
 * only when necessary. Configuration is provided to control whether the
 * messages are printed as these messages can occur during normal server
 * operation; these messages can be useful when SSL is behaving unexpectedly
 * and is therefore by default enabled. These message can be disabled when
 * the SSL configuration is considered stable and correct.
 * 
 * By default the errors are logged as the error path is taken in multiple
 * circumstances. The one we care about is when the admin has not set up the
 * security setings / repertoire well and might have an expired certificate
 * or an incorrect protocol setting (or other issues). For that case, we want
 * to print something to the error log as an indicator. However, this code
 * path is also taken when everything is configured correctly, but an
 * unauthorized attempt to access is made. In those cases, it may be
 * undesirable to have the errors printed as we don't want to spam the error
 * log. In those cases, the configuraiton can be changed to disable logging.
 * 
 * Note, this class is not used in when a the SSL channel's Discriminator is
 * used to handle the initial connection. We don't want to send SSL handshake
 * error messages to the log in cases where the traffic isn't necessarily SSL.
 * The Discriminator is used in cases where port sharing is involved.
 */
class SSLHandshakeErrorTracker {

    /** Trace component for WAS */
    private static final TraceComponent tc =
                    Tr.register(SSLHandshakeErrorTracker.class,
                                SSLChannelConstants.SSL_TRACE_NAME,
                                SSLChannelConstants.SSL_BUNDLE);
    private final boolean shouldLogError;
    private final long maxLogEntries;
    private long numberOfLogEntries = 0;
    private boolean loggingStopped = false;

    /**
     * Constructor. Each instance of the SSLChannel includes a unique instance
     * of this class.
     * 
     * @param shouldLogError control if handshake error messages should be logged
     * @param maxLogEntries number of times the handshake failure should be logged
     */
    SSLHandshakeErrorTracker(boolean shouldLogError, long maxLogEntries) {
        this.shouldLogError = shouldLogError;
        this.maxLogEntries = maxLogEntries;
    }

    /**
     * This method should be called when an SSL handshake error occurs. It will
     * determine if an error messages should be logged, and log it if necessary.
     * <p>
     * Note that a debug message containing the failure will be issued if debug
     * is enabled, regardless of the error message being printed.
     * 
     * @param failure Exception which resulted from the JSSE during the SSL handshake. {@code null} is not supported.
     */
    void noteHandshakeError(Exception failure) {
        numberOfLogEntries++;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "noteHandshakeError (" + numberOfLogEntries + "): " + failure.getMessage(), failure);
        }
        if (shouldLogError) {
            if (numberOfLogEntries <= maxLogEntries) {
                Tr.error(tc, SSLChannelConstants.HANDSHAKE_FAILURE, failure);
            } else if (!loggingStopped && (numberOfLogEntries > maxLogEntries)) {
                loggingStopped = true;
                Tr.info(tc, SSLChannelConstants.HANDSHAKE_FAILURE_STOP_LOGGING);
            }
        }
    }
}
