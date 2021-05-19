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
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.dom.processor.UsernameTokenProcessor;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wssecurity.WSSecurityPolicyException;
import com.ibm.ws.wssecurity.cxf.validator.Utils;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
/**
 *
 */
public class UsernameTokenInterceptor extends org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor {

    protected static final TraceComponent tc = Tr.register(UsernameTokenInterceptor.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);

    @Override
    protected WSSecurityEngineResult validateToken(Element tokenElement, final SoapMessage message) throws WSSecurityException, Base64DecodingException {

        boolean bspCompliant = isWsiBSPCompliant(message);
        boolean allowNoPassword = isAllowNoPassword(message.get(AssertionInfoMap.class));
        UsernameTokenProcessor p = new UsernameTokenProcessor();
        org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor nt = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, " validateToken" + tokenElement.toString());
        }
        RequestData data = new RequestData() {
            @Override
            public CallbackHandler getCallbackHandler() {
                return getCallback(message);
            }

            @Override
            public Validator getValidator(QName qName) throws WSSecurityException {
                Object validator = message.getContextualProperty(SecurityConstants.USERNAME_TOKEN_VALIDATOR);
                if (validator == null) {
                    return super.getValidator(qName);
                }
                return (Validator) validator;
            }
        };

        // Configure replay caching
        ReplayCache nonceCache = WSS4JUtils.getReplayCache(
                                                           message, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE);
        //final ReplayCache replayCache = Utils.getReplayCache(message, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE);
        data.setNonceReplayCache(nonceCache);

        data.setAllowUsernameTokenNoPassword(allowNoPassword);
        data.setWssConfig(WSSConfig.getNewInstance());
        if (!bspCompliant) {
            data.setDisableBSPEnforcement(true);
        }
        data.setMsgContext(message);

        translateSettingsFromMsgContext(data, message); // This EMULATES CXF HTTPS behavior, but "MAY or MAY NOT be exactly CXF HTTPS behavior" //TODO
        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        data.setWsDocInfo(wsDocInfo);
        //try {
            List<WSSecurityEngineResult> results = p.handleToken(tokenElement, data);
            checkTokens(message, results);
            return results.get(0);
        //} catch (WSSecurityException ex) {
        //    throw WSS4JUtils.createSoapFault(message, message.getVersion(), ex);
        //}
    }

    private boolean isAllowNoPassword(AssertionInfoMap aim) throws WSSecurityException {
        Collection<AssertionInfo> ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);

        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                UsernameToken policy = (UsernameToken) ai.getAssertion();
                if (policy.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
                    return true;
                }
            }
        }

        return false;
    }


    private CallbackHandler getCallback(@Sensitive SoapMessage message) {
        //Then try to get the password from the given callback handler
        Object o = Utils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);//message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER); //v3

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler) o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler) ClassLoaderUtils.loadClass((String) o, this.getClass()).newInstance();
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
    protected WSSecUsernameToken addUsernameToken(@Sensitive SoapMessage message, Document doc, @Sensitive UsernameToken token) {
        String userName = null;
        Object o = Utils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);//(String) message.getContextualProperty(SecurityConstants.USERNAME); //v3
        if (o != null) {
            userName = (String) o;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "add usernameToken" + userName);
        }
        WSSConfig wssConfig = (WSSConfig) message.getContextualProperty(WSSConfig.class.getName());
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
        }

        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            boolean isNoPassword = UsernameToken.PasswordType.NoPassword.equals(token.getPasswordType());
            boolean isPasswordHashed = UsernameToken.PasswordType.HashPassword.equals(token.getPasswordType());
            if (isNoPassword) { 
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(doc);//new WSSecUsernameToken(wssConfig); //v3
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
                if (token.isCreated() && !isPasswordHashed) {
                    utBuilder.addCreated();
                }
                if (token.isNonce() && !isPasswordHashed) {
                    utBuilder.addNonce();
                }
                return utBuilder;
            }
            o = Utils.getSecurityPropertyValue(SecurityConstants.PASSWORD, message);
            //String password = (String) message.getContextualProperty(SecurityConstants.PASSWORD);
            String password = null;
            if (o != null) {
                password = (String) o;
            }
            if (StringUtils.isEmpty(password)) {
                password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN, message);
            }

            if (!StringUtils.isEmpty(password)) {
                //If the password is available then build the token
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(doc);
                if (isPasswordHashed) {
                    utBuilder.setPasswordType(WSS4JConstants.PASSWORD_DIGEST);
                } else {
                    utBuilder.setPasswordType(WSS4JConstants.PASSWORD_TEXT);
                }

                utBuilder.setUserInfo(userName, password);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "addUsernameToken, " + userName);
                }
                if (token.isCreated() && !isPasswordHashed) {
                    utBuilder.addCreated();
                }
                if (token.isNonce() && !isPasswordHashed) {
                    utBuilder.addNonce();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "addUsernameToken returns, " + utBuilder);
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

            //org.apache.ws.security.message.token.UsernameToken usernameToken = (org.apache.ws.security.message.token.UsernameToken) result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);

            org.apache.wss4j.dom.message.token.UsernameToken usernameToken = (org.apache.wss4j.dom.message.token.UsernameToken) result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "usernameToken is '" + usernameToken + "'");
            }
            boolean isPasswordHashed = UsernameToken.PasswordType.HashPassword.equals(usernameTokenPolicy.getPasswordType());
            if (isPasswordHashed != usernameToken.isHashed()) {
                String strErr = "Password hashing policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            boolean isNoPassword = UsernameToken.PasswordType.NoPassword.equals(usernameTokenPolicy.getPasswordType());
            if (isNoPassword && usernameToken.getPassword() != null) {
                String strErr = "Username Token NoPassword policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            if (usernameTokenPolicy.isCreated()
                && (usernameToken.getCreated() == null || usernameToken.isHashed())) {
                String strErr = "Username Token Created policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
            if (usernameTokenPolicy./* isRequireNonce() */isNonce()
                && (usernameToken.getNonce() == null || usernameToken.isHashed())) {
                String strErr = "Username Token Nonce policy not enforced";
                //ai.setNotAsserted(strErr);
                msgs.add(strErr);
                bOk = false;
            }
        }
        if (!bOk) {
            WSSecurityPolicyException wsse = new WSSecurityPolicyException(msgs.toString());
            throw new WSSecurityException(ErrorCode.FAILED_CHECK,wsse); 
        }

        return true;
    }

    void translateSettingsFromMsgContext(RequestData reqData, @Sensitive SoapMessage message) {
        // This emulates the code from doReceiverAction() in org.apache.ws.security.WSHandler
        WSSConfig wssConfig = reqData.getWssConfig();
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
            reqData.setWssConfig(wssConfig);
        }
        //v3
        Object mc = reqData.getMsgContext();

        reqData.setUtTTL(decodeTimeToLive(reqData, message, false));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unt TTL '" + reqData.getUtTTL() + "'");
        }
        // wssConfig.setTimeStampFutureTTL(decodeFutureTimeToLive(reqData, message));
        //wssConfig.setUtFutureTTL(decodeFutureTimeToLive(reqData, message));
        reqData.setUtFutureTTL(decodeFutureTimeToLive(reqData, message, false));
        //wssConfig.setHandleCustomPasswordTypes(decodeCustomPasswordTypes(reqData));
        //wssConfig.setPasswordsAreEncoded(decodeUseEncodedPasswords(reqData));
        reqData.setWssConfig(wssConfig);
    }

    public int decodeTimeToLive(RequestData reqData, @Sensitive SoapMessage message, boolean timestamp) {
        String tag = SecurityConstants.TIMESTAMP_TTL;
        if (!timestamp) {
            tag = SecurityConstants.USERNAMETOKEN_TTL;
        }
        String ttl = (String) message.getContextualProperty(tag);
        int ttlI = 0;
        if (ttl != null) {
            try {
                ttlI = Integer.parseInt(ttl);
            } catch (NumberFormatException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ttl string is malformat '" + ttl + "'" + e.getMessage());
                }
                if (timestamp) {
                    ttlI = reqData.getTimeStampTTL();
                } else {
                    ttlI = reqData.getUtTTL();
                }    
            }
        }
        if (ttlI <= 0) {
            if (timestamp) {
                ttlI = reqData.getTimeStampTTL();
            } else {
                ttlI = reqData.getUtTTL();
            }    
        }
        return ttlI;
    }

    protected int decodeFutureTimeToLive(RequestData reqData, @Sensitive SoapMessage message, boolean timestamp) {
        String tag = SecurityConstants.TIMESTAMP_FUTURE_TTL;
        if (!timestamp) {
            tag = SecurityConstants.USERNAMETOKEN_FUTURE_TTL;
        }
        String ttl = (String) message.getContextualProperty(tag);
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
