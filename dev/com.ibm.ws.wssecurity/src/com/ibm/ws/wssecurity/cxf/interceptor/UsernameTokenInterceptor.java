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
package com.ibm.ws.wssecurity.cxf.interceptor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.wss4j.WSS4JTokenConverter;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.cache.ReplayCache;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.processor.UsernameTokenProcessor;
import org.apache.ws.security.validate.Validator;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wssecurity.cxf.validator.Utils;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

public class UsernameTokenInterceptor extends org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor {
    protected static final TraceComponent tc = Tr.register(UsernameTokenInterceptor.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);

    @Override
    protected WSUsernameTokenPrincipal getPrincipal(@Sensitive Element tokenElement, @Sensitive final SoapMessage message)
                    throws WSSecurityException {

        final ReplayCache replayCache = Utils.getReplayCache(message, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE);
        boolean bspCompliant = isWsiBSPCompliant(message);

        boolean utWithCallbacks =
                        MessageUtils.getContextualBoolean(message, SecurityConstants.VALIDATE_TOKEN, true);
        if (utWithCallbacks) {
            UsernameTokenProcessor p = new UsernameTokenProcessor();
            WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
            RequestData data = new RequestData() {
                @Override
                public CallbackHandler getCallbackHandler() {
                    return getCallback(message);
                }

                @Override
                public Validator getValidator(QName qName) throws WSSecurityException {
                    Object validator =
                                    message.getContextualProperty(SecurityConstants.USERNAME_TOKEN_VALIDATOR);
                    if (validator == null) {
                        return super.getValidator(qName);
                    }
                    return (Validator) validator;
                }
            };
            data.setWssConfig(WSSConfig.getNewInstance());
            data.setNonceReplayCache(replayCache);
            data.setMsgContext(message);
            translateSettingsFromMsgContext(data, message); // This EMULATES CXF HTTPS behavior, but "MAY or MAY NOT be exactly CXF HTTPS behavior"
            List<WSSecurityEngineResult> results =
                            p.handleToken(tokenElement, data, wsDocInfo);

            checkTokens(message, results);

            return (WSUsernameTokenPrincipal) results.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
        } else {
            WSUsernameTokenPrincipal principal = parseTokenAndCreatePrincipal(tokenElement, bspCompliant);
            WSS4JTokenConverter.convertToken(message, principal);
            return principal;
        }
    }

    private CallbackHandler getCallback(@Sensitive SoapMessage message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler) o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler) ClassLoaderUtils
                                .loadClass((String) o, this.getClass()).newInstance();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception while getCallback handler :" + e);
                }
                handler = null;
            }
        }
        return handler;
    }

    @Override
    protected WSSecUsernameToken addUsernameToken(@Sensitive SoapMessage message, @Sensitive UsernameToken token) {
        String userName = (String) message.getContextualProperty(SecurityConstants.USERNAME);
        WSSConfig wssConfig = (WSSConfig) message.getContextualProperty(WSSConfig.class.getName());
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
        }

        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            if (token.isNoPassword()) {
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(wssConfig);
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
                if (token.isRequireCreated() && !token.isHashPassword()) {
                    utBuilder.addCreated();
                }
                if (token.isRequireNonce() && !token.isHashPassword()) {
                    utBuilder.addNonce();
                }
                return utBuilder;
            }

            String password = (String) message.getContextualProperty(SecurityConstants.PASSWORD);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN, message);
            }

            if (!StringUtils.isEmpty(password)) {
                //If the password is available then build the token
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(wssConfig);
                if (token.isHashPassword()) {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_DIGEST);
                } else {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_TEXT);
                }

                utBuilder.setUserInfo(userName, password);
                if (token.isRequireCreated() && !token.isHashPassword()) {
                    utBuilder.addCreated();
                }
                if (token.isRequireNonce() && !token.isHashPassword()) {
                    utBuilder.addNonce();
                }
                return utBuilder;
            } else {
                policyNotAsserted(token, "No username available", message);
            }
        } else {
            policyNotAsserted(token, "No username available", message);
        }
        return null;
    }

    /**
     * All UsernameTokens must conform to the policy
     */
    public boolean checkTokens(@Sensitive SoapMessage message,
                               List<WSSecurityEngineResult> utResults) throws WSSecurityException {

        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ais in checkTokens is '" + ais + "'");
        }
        UsernameToken usernameTokenPolicy = null;
        //for (AssertionInfo ai : ais) { // TBD
        //    tok = (UsernameToken) ai.getAssertion();
        //
        //}
        Object[] aiArray = ais.toArray();
        AssertionInfo ai = null;
        if (aiArray.length > 0) {
            ai = (AssertionInfo) aiArray[0];
            usernameTokenPolicy = (UsernameToken) ai.getAssertion();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ai in checkTokens is '" + ai + "'");
            Tr.debug(tc, "usernameTokenPolicy is '" + usernameTokenPolicy + "'");
        }

        boolean bOk = true;
        Set<String> msgs = new LinkedHashSet<String>();

        for (WSSecurityEngineResult result : utResults) {

            org.apache.ws.security.message.token.UsernameToken usernameToken =
                            (org.apache.ws.security.message.token.UsernameToken) result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "usernameToken is '" + usernameToken + "'");
            }

            if (usernameTokenPolicy.isHashPassword() != usernameToken.isHashed()) {
                String strErr = "Password hashing policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            if (usernameTokenPolicy.isNoPassword() && usernameToken.getPassword() != null) {
                String strErr = "Username Token NoPassword policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            if (usernameTokenPolicy.isRequireCreated()
                && (usernameToken.getCreated() == null || usernameToken.isHashed())) {
                String strErr = "Username Token Created policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            if (usernameTokenPolicy.isRequireNonce()
                && (usernameToken.getNonce() == null || usernameToken.isHashed())) {
                String strErr = "Username Token Nonce policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
        }
        if (!bOk) {
            throw new WSSecurityException(msgs.toString());
        }

        return true;
    }

    void translateSettingsFromMsgContext(RequestData reqData, @Sensitive SoapMessage message) {
        // This emulates the code from doReceiverAction() in org.apache.ws.security.WSHandler
        WSSConfig wssConfig = reqData.getWssConfig();
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
        }
        //boolean enableSigConf = decodeEnableSignatureConfirmation(reqData);
        //wssConfig.setEnableSignatureConfirmation(
        //    enableSigConf || ((doAction & WSConstants.SC) != 0)
        //);
        //wssConfig.setTimeStampStrict(decodeTimestampStrict(reqData));
        //if (decodePasswordTypeStrict(reqData)) {
        //    String passwordType = decodePasswordType(reqData);
        //    wssConfig.setRequiredPasswordType(passwordType);
        //}
        // wssConfig.setTimeStampTTL(decodeTimeToLive(reqData, message));
        wssConfig.setUtTTL(decodeTimeToLive(reqData, message));
        // wssConfig.setTimeStampFutureTTL(decodeFutureTimeToLive(reqData, message));
        wssConfig.setUtFutureTTL(decodeFutureTimeToLive(reqData, message));
        //wssConfig.setHandleCustomPasswordTypes(decodeCustomPasswordTypes(reqData));
        //wssConfig.setPasswordsAreEncoded(decodeUseEncodedPasswords(reqData));
        reqData.setWssConfig(wssConfig);
    }

    public int decodeTimeToLive(RequestData reqData, @Sensitive SoapMessage message) {
        String ttl = (String) message.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        //System.out.println("gkuo:ttl string in decode is:" + ttl);
        int ttlI = 0;
        if (ttl != null) {
            try {
                ttlI = Integer.parseInt(ttl);
            } catch (NumberFormatException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ttl string is malformat '" + ttl + "'" + e.getMessage());
                }
                ttlI = reqData.getTimeToLive();
            }
        }
        if (ttlI <= 0) {
            ttlI = reqData.getTimeToLive();
        }
        return ttlI;
    }

    protected int decodeFutureTimeToLive(RequestData reqData, @Sensitive SoapMessage message) {
        String ttl = (String) message.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        //System.out.println("gkuo:ttl string in decode is:" + ttl);
        int defaultFutureTimeToLive = 60;
        if (ttl != null) {
            try {
                int ttlI = Integer.parseInt(ttl);
                if (ttlI < 0) {
                    return defaultFutureTimeToLive;
                }
                return ttlI;
            } catch (NumberFormatException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "future ttl string is malformat '" + ttl + "'" + e.getMessage());
                }
                return defaultFutureTimeToLive;
            }
        }
        return defaultFutureTimeToLive;
    }

}
