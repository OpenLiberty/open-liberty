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
package org.apache.cxf.ws.security.wss4j;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.cache.ReplayCacheFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.cache.ReplayCache;

/**
 * Some common functionality that can be shared between the WSS4JInInterceptor and the
 * UsernameTokenInterceptor.
 */
public final class WSS4JUtils {

    // FAULT error messages
    public static final String UNSUPPORTED_TOKEN_ERR = "An unsupported token was provided";
    public static final String UNSUPPORTED_ALGORITHM_ERR = 
        "An unsupported signature or encryption algorithm was used";
    public static final String INVALID_SECURITY_ERR = 
        "An error was discovered processing the <wsse:Security> header.";
    public static final String INVALID_SECURITY_TOKEN_ERR = 
        "An invalid security token was provided";
    public static final String FAILED_AUTHENTICATION_ERR = 
        "The security token could not be authenticated or authorized";
    public static final String FAILED_CHECK_ERR = "The signature or decryption was invalid";
    public static final String SECURITY_TOKEN_UNAVAILABLE_ERR = 
        "Referenced security token could not be retrieved";
    public static final String MESSAGE_EXPIRED_ERR = "The message has expired";


    private WSS4JUtils() {
        // complete
    }

    /**
     * Get a ReplayCache instance. It first checks to see whether caching has been explicitly 
     * enabled or disabled via the booleanKey argument. If it has been set to false then no
     * replay caching is done (for this booleanKey). If it has not been specified, then caching
     * is enabled only if we are not the initiator of the exchange. If it has been specified, then
     * caching is enabled.
     * 
     * It tries to get an instance of ReplayCache via the instanceKey argument from a 
     * contextual property, and failing that the message exchange. If it can't find any, then it
     * defaults to using an EH-Cache instance and stores that on the message exchange.
     */
    public static ReplayCache getReplayCache(
        SoapMessage message, String booleanKey, String instanceKey
    ) {
        boolean specified = false;
        Object o = message.getContextualProperty(booleanKey);
        if (o != null) {
            if (!MessageUtils.isTrue(o)) {
                return null;
            }
            specified = true;
        }

        if (!specified && MessageUtils.isRequestor(message)) {
            return null;
        }
        Endpoint ep = message.getExchange().get(Endpoint.class);
        if (ep != null && ep.getEndpointInfo() != null) {
            EndpointInfo info = ep.getEndpointInfo();
            synchronized (info) {
                ReplayCache replayCache = 
                        (ReplayCache)message.getContextualProperty(instanceKey);
                if (replayCache == null) {
                    replayCache = (ReplayCache)info.getProperty(instanceKey);
                }
                if (replayCache == null) {
                    ReplayCacheFactory replayCacheFactory = ReplayCacheFactory.newInstance();
                    String cacheKey = instanceKey;
                    if (info.getName() != null) {
                        int hashcode = info.getName().toString().hashCode();
                        if (hashcode < 0) {
                            cacheKey += hashcode;
                        } else {
                            cacheKey += "-" + hashcode;
                        }
                    }
                    replayCache = replayCacheFactory.newReplayCache(cacheKey, message);
                    info.setProperty(instanceKey, replayCache);
                }
                return replayCache;
            }
        }
        return null;
    }

    /**
     * Map a WSSecurityException FaultCode to a standard error String, so as not to leak
     * internal configuration to an attacker.
     */
    public static String getSafeExceptionMessage(WSSecurityException ex) {
        // Allow a Replay Attack message to be returned, otherwise it could be confusing
        // for clients who don't understand the default caching functionality of WSS4J/CXF
        if (ex.getMessage() != null && ex.getMessage().contains("replay attack")) {
            return ex.getMessage();
        }
        
        String errorMessage = null;
        QName faultCode = ex.getFaultCode();
        if (WSConstants.UNSUPPORTED_SECURITY_TOKEN.equals(faultCode)) {
            errorMessage = UNSUPPORTED_TOKEN_ERR;
        } else if (WSConstants.UNSUPPORTED_ALGORITHM.equals(faultCode)) {
            errorMessage = UNSUPPORTED_ALGORITHM_ERR;
        } else if (WSConstants.INVALID_SECURITY.equals(faultCode)) {
            errorMessage = INVALID_SECURITY_ERR;
        } else if (WSConstants.INVALID_SECURITY_TOKEN.equals(faultCode)) {
            errorMessage = INVALID_SECURITY_TOKEN_ERR;
        } else if (WSConstants.FAILED_AUTHENTICATION.equals(faultCode)) {
            errorMessage = FAILED_AUTHENTICATION_ERR;
        } else if (WSConstants.FAILED_CHECK.equals(faultCode)) {
            errorMessage = FAILED_CHECK_ERR;
        } else if (WSConstants.SECURITY_TOKEN_UNAVAILABLE.equals(faultCode)) {
            errorMessage = SECURITY_TOKEN_UNAVAILABLE_ERR;
        } else if (WSConstants.MESSAGE_EXPIRED.equals(faultCode)) {
            errorMessage = MESSAGE_EXPIRED_ERR;
        } else {
            // Default
            errorMessage = INVALID_SECURITY_ERR;
        }
        return errorMessage;
        
    }
}