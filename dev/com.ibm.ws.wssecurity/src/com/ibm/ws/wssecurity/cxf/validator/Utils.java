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
package com.ibm.ws.wssecurity.cxf.validator;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.cache.ReplayCacheFactory;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.cache.ReplayCache;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class Utils {
    private static final TraceComponent tc = Tr.register(Utils.class,
                                                         WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

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
    public static ReplayCache getReplayCache(@Sensitive SoapMessage message, String booleanKey, String instanceKey) {
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
                                (ReplayCache) message.getContextualProperty(instanceKey);
                if (replayCache == null) {
                    replayCache = (ReplayCache) info.getProperty(instanceKey);
                }
                if (replayCache == null) {
                    ReplayCacheFactory replayCacheFactory = ReplayCacheFactory.newInstance();
                    String cacheKey = instanceKey;
                    if (info.getName() != null) {
                        cacheKey += "-" + info.getName().toString().hashCode();
                    }
                    replayCache = replayCacheFactory.newReplayCache(cacheKey, message);
                    info.setProperty(instanceKey, replayCache);
                }
                return replayCache;
            }
        }
        return null;
    }

    public static boolean checkPolicyNoPassword(@Sensitive final SoapMessage message) throws WSSecurityException {
        boolean bRet = false;
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken) ai.getAssertion();
            if (tok.isNoPassword())
                bRet = true;
        }

        return bRet;
    }

    /**
     * The cryptoProps may include the sensitive keystore password
     * 
     * @param cryptoProps
     * @return
     */
    @Sensitive
    public static Crypto getCrypto(Properties cryptoProps) {
        Crypto crypto = null;
        try {
            if (cryptoProps != null) {
                crypto = CryptoFactory.getInstance(cryptoProps);
            }
        } catch (WSSecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cannot create a Crypto, please double check the key properties (" + e + ")");
            }
        }
        return crypto;
    }

    @Sensitive
    public static String changePasswordType(SerializableProtectedString protectedString) {
        String clearString = null;
        if (protectedString != null) {
            char[] password_array = protectedString.getChars();
            if (password_array.length > 0) {
                StringBuilder buf = new StringBuilder();

                for (int i = 0; i < password_array.length; i++) {
                    buf.append(password_array[i]);
                }

                // decode 
                clearString = PasswordUtil.passwordDecode(buf.toString());

            }
        }

        return clearString;
    }

    /*
     * @Sensitive
     * public boolean checkForSignatureProps(@Sensitive Map<String, Object> map) {
     * boolean isSigConfig = false;
     * if (map.containsKey(WSSecurityConstants.CXF_SIG_PROPS))
     * isSigConfig = true;
     * return isSigConfig;
     * }
     * 
     * public boolean checkForEncryptionProps(Map<String, Object> map) {
     * boolean isEncConfig = false;
     * if (map.containsKey(WSSecurityConstants.CXF_ENC_PROPS))
     * isEncConfig = true;
     * return isEncConfig;
     * }
     */
    @Sensitive
    static public void modifyConfigMap(Map<String, Object> configMap) {
        if (configMap.containsKey(WSSecurityConstants.CXF_USER_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
            configMap.put(WSSecurityConstants.CXF_USER_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_KEY_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.WSS4J_KEY_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_KEY_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_KS_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.WSS4J_KS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_KS_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_TS_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.WSS4J_TS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_TS_PASSWORD, pwd);
        }
    }

}
