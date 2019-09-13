/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;

final class TrustDecisionUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(TrustDecisionUtil.class);
    
    private TrustDecisionUtil() {
    }

    /**
     * This call must take place before anything is written to the 
     * URLConnection. The URLConnection.connect() will be called in order 
     * to get the connection information. 
     * 
     * This method is invoked just after setURLRequestHeaders() from the 
     * WrappedOutputStream before it writes data to the URLConnection.
     * 
     * If trust cannot be established the Trust Decider implemenation
     * throws an IOException.
     * 
     * @param message      The message being sent.
     * @throws IOException This exception is thrown if trust cannot be
     *                     established by the configured MessageTrustDecider.
     * @see MessageTrustDecider
     */
    static void makeTrustDecision(
            MessageTrustDecider trustDecider, 
            Message message,
            HttpURLConnection connection, 
            String conduitName) throws IOException {
    
        MessageTrustDecider decider2 = message.get(MessageTrustDecider.class);
        if (trustDecider != null || decider2 != null) {
            try {
                // We must connect or we will not get the credentials.
                // The call is (said to be) ingored internally if
                // already connected.
                connection.connect();
                HttpsURLConnectionInfo info = new HttpsURLConnectionInfo(connection);
                if (trustDecider != null) {
                    trustDecider.establishTrust(
                            conduitName, 
                        info,
                        message);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Trust Decider "
                            + trustDecider.getLogicalName()
                            + " considers Conduit "
                            + conduitName 
                            + " trusted.");
                    }
                }
                if (decider2 != null) {
                    decider2.establishTrust(conduitName, 
                                            info,
                                            message);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Trust Decider "
                            + decider2.getLogicalName()
                            + " considers Conduit "
                            + conduitName 
                            + " trusted.");
                    }
                }
            } catch (UntrustedURLConnectionIOException untrustedEx) {
                connection.disconnect();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Trust Decider "
                        + (trustDecider != null ? trustDecider.getLogicalName() : decider2.getLogicalName())
                        + " considers Conduit "
                        + conduitName 
                        + " untrusted.", untrustedEx);
                }
                throw untrustedEx;
            }
        } else {
            // This case, when there is no trust decider, a trust
            // decision should be a matter of policy.
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "No Trust Decider for Conduit '"
                    + conduitName
                    + "'. An afirmative Trust Decision is assumed.");
            }
        }
    }
}
