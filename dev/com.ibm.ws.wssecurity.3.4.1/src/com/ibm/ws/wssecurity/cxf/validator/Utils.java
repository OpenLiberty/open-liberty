/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.UsernameToken;

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
     * @throws WSSecurityException 
     */
    public static ReplayCache getReplayCache(@Sensitive SoapMessage message, String booleanKey, String instanceKey) throws WSSecurityException {
        
        return WSS4JUtils.getReplayCache(message, booleanKey, instanceKey); //@2020
    }

    /**
     * Get the security property value for the given property. It also checks for the older "ws-"* property
     * values.
     */
    public static Object getSecurityPropertyValue(String property, SoapMessage message) {
        Object value = message.getContextualProperty(property);
        if (value != null) {
            return value;
        }
        return message.getContextualProperty("ws-" + property);
    }

    public static boolean checkPolicyNoPassword(@Sensitive final SoapMessage message) throws WSSecurityException {
        boolean bRet = false;
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken) ai.getAssertion();
            if (UsernameToken.PasswordType.NoPassword.equals(tok.getPasswordType())) {
                bRet = true;
            }
        }

        return bRet;
    }

    /**
     * The cryptoProps may include the sensitive keystore password
     * 
     * @param cryptoProps
     * @return
     */
    //@2020 TODO
//    @Sensitive
//    public static Crypto getCrypto(Properties cryptoProps) {
//        Crypto crypto = null;
//        try {
//            if (cryptoProps != null) {
//                crypto = CryptoFactory.getInstance(cryptoProps);
//            }
//        } catch (WSSecurityException e) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "cannot create a Crypto, please double check the key properties (" + e + ")");
//            }
//        }
//        return crypto;
//    }

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
        if (configMap.containsKey(WSSecurityConstants.WSS4J_2_KEY_PASSWORD)) {
            String pwd = PasswordUtil.passwordDecode((String)configMap.get(WSSecurityConstants.WSS4J_2_KEY_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_2_KEY_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_KS_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.WSS4J_KS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_KS_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_2_KS_PASSWORD)) {
            String pwd = PasswordUtil.passwordDecode((String)configMap.get(WSSecurityConstants.WSS4J_2_KS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_2_KS_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_TS_PASSWORD)) {
            String pwd = changePasswordType((SerializableProtectedString)
                            configMap.get(WSSecurityConstants.WSS4J_TS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_TS_PASSWORD, pwd);
        }
        if (configMap.containsKey(WSSecurityConstants.WSS4J_2_TS_PASSWORD)) {
            String pwd = PasswordUtil.passwordDecode((String)configMap.get(WSSecurityConstants.WSS4J_2_TS_PASSWORD));
            configMap.put(WSSecurityConstants.WSS4J_2_TS_PASSWORD, pwd);
        }
    }

}
