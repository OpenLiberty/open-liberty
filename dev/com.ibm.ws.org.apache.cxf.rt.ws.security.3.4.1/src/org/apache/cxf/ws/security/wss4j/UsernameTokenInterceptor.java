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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.dom.processor.UsernameTokenProcessor;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.XMLUtils;

/**
 *
 */
public class UsernameTokenInterceptor extends AbstractTokenInterceptor {
    //Liberty code change start, for debug only
    private static final Logger LOG = LogUtils.getL7dLogger(UsernameTokenInterceptor.class);
    //Liberty code change end
    public UsernameTokenInterceptor() {
        super();
    }

    protected void processToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        boolean utWithCallbacks =
            MessageUtils.getContextualBoolean(message, SecurityConstants.VALIDATE_TOKEN, true);

        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (SPConstants.USERNAME_TOKEN.equals(child.getLocalName())
                && WSS4JConstants.WSSE_NS.equals(child.getNamespaceURI())) {
                try {
                    boolean bspCompliant = isWsiBSPCompliant(message);
                    boolean allowNSPasswdTypes = allowNamespaceQualifiedPWDTypes(message);
                    Principal principal = null;
                    Subject subject = null;
                    Object transformedToken = null;

                    if (utWithCallbacks) {
                        final WSSecurityEngineResult result = validateToken(child, message);
                        subject = (Subject)result.get(WSSecurityEngineResult.TAG_SUBJECT);
                        transformedToken = result.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
                        principal = (Principal)result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                        if (principal == null) {
                            principal = parseTokenAndCreatePrincipal(child, bspCompliant, allowNSPasswdTypes);
                        }
                    } else {
                        principal = parseTokenAndCreatePrincipal(child, bspCompliant, allowNSPasswdTypes);
                        WSS4JTokenConverter.convertToken(message, principal);
                    }

                    SecurityContext sc = message.get(SecurityContext.class);
                    if (sc == null || sc.getUserPrincipal() == null) {
                        if (transformedToken instanceof SamlAssertionWrapper) {
                            message.put(SecurityContext.class,
                                        createSecurityContext(message,
                                                              (SamlAssertionWrapper)transformedToken));
                        } else if (subject != null && principal != null) {
                            message.put(SecurityContext.class,
                                    createSecurityContext(principal, subject));
                        } else {
                            UsernameTokenPrincipal utPrincipal = (UsernameTokenPrincipal)principal;
                            String nonce = null;
                            if (utPrincipal.getNonce() != null) {
                                nonce = XMLUtils.encodeToString(utPrincipal.getNonce());
                            }
                            subject = createSubject(utPrincipal.getName(), utPrincipal.getPassword(),
                                    utPrincipal.isPasswordDigest(), nonce, utPrincipal.getCreatedTime());
                            message.put(SecurityContext.class,
                                    createSecurityContext(utPrincipal, subject));
                        }
                    }

                    if (principal instanceof UsernameTokenPrincipal) {
                        storeResults((UsernameTokenPrincipal)principal, subject, message);
                    }
                } catch (WSSecurityException | Base64DecodingException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    private SecurityContext createSecurityContext(Message msg,
                                                  SamlAssertionWrapper samlAssertion) {
        String roleAttributeName =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_ROLE_ATTRIBUTENAME, msg);
        if (roleAttributeName == null || roleAttributeName.length() == 0) {
            roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
        }

        ClaimCollection claims =
            SAMLUtils.getClaims(samlAssertion);
        Set<Principal> roles =
            SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

        SAMLSecurityContext context =
            new SAMLSecurityContext(new SAMLTokenPrincipalImpl(samlAssertion), roles, claims);
        context.setIssuer(SAMLUtils.getIssuer(samlAssertion));
        context.setAssertionElement(SAMLUtils.getAssertionElement(samlAssertion));
        return context;
    }

    private void storeResults(UsernameTokenPrincipal principal, Subject subject, SoapMessage message) {
        List<WSSecurityEngineResult> v = new ArrayList<>();
        int action = WSConstants.UT;
        if (principal.getPassword() == null) {
            action = WSConstants.UT_NOPASSWORD;
        }

        WSSecurityEngineResult result = new WSSecurityEngineResult(action, principal, null, null, null);
        if (subject != null) {
            result.put(WSSecurityEngineResult.TAG_SUBJECT, subject);
        }
        v.add(0, result);
        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                                  .get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            results = new ArrayList<>();
            message.put(WSHandlerConstants.RECV_RESULTS, results);
        }

        WSHandlerResult rResult =
            new WSHandlerResult(null, v, Collections.singletonMap(action, v));
        results.add(0, rResult);

        assertTokens(message, principal, false);
    }

    protected WSSecurityEngineResult validateToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException, Base64DecodingException {

        boolean bspCompliant = isWsiBSPCompliant(message);
        boolean allowNoPassword = isAllowNoPassword(message.get(AssertionInfoMap.class));
        UsernameTokenProcessor p = new UsernameTokenProcessor();

        RequestData data = new CXFRequestData();
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
        try {
            data.setCallbackHandler(SecurityUtils.getCallbackHandler(o));
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        data.setMsgContext(message);

        // Configure replay caching
        ReplayCache nonceCache =
            WSS4JUtils.getReplayCache(
                message, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
            );
        data.setNonceReplayCache(nonceCache);

        data.setAllowUsernameTokenNoPassword(allowNoPassword);
        data.setWssConfig(WSSConfig.getNewInstance());
        if (!bspCompliant) {
            data.setDisableBSPEnforcement(true);
        }
        data.setMsgContext(message);

        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        data.setWsDocInfo(wsDocInfo);
        try {
            List<WSSecurityEngineResult> results = p.handleToken(tokenElement, data);
            return results.get(0);
        } catch (WSSecurityException ex) {
            throw WSS4JUtils.createSoapFault(message, message.getVersion(), ex);
        }
    }

    protected UsernameTokenPrincipal parseTokenAndCreatePrincipal(Element tokenElement, boolean bspCompliant,
                                                                  boolean allowNamespaceQualifiedPWDTypes)
        throws WSSecurityException, Base64DecodingException {
        BSPEnforcer bspEnforcer = new org.apache.wss4j.common.bsp.BSPEnforcer(!bspCompliant);
        org.apache.wss4j.dom.message.token.UsernameToken ut =
            new org.apache.wss4j.dom.message.token.UsernameToken(tokenElement, allowNamespaceQualifiedPWDTypes,
                                                                 bspEnforcer);

        WSUsernameTokenPrincipalImpl principal = new WSUsernameTokenPrincipalImpl(ut.getName(), ut.isHashed());
        if (ut.getNonce() != null) {
            principal.setNonce(XMLUtils.decode(ut.getNonce()));
        }
        principal.setPassword(ut.getPassword());
        principal.setCreatedTime(ut.getCreated());
        principal.setPasswordType(ut.getPasswordType());

        return principal;
    }

    protected boolean isWsiBSPCompliant(final SoapMessage message) {
        String bspc = (String)message.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        // Default to WSI-BSP compliance enabled
        return !("false".equals(bspc) || "0".equals(bspc));
    }
    private boolean allowNamespaceQualifiedPWDTypes(final SoapMessage message) {
        String allow = (String)message
            .getContextualProperty(ConfigurationConstants.ALLOW_NAMESPACE_QUALIFIED_PASSWORD_TYPES);
        return "true".equals(allow) || "1".equals(allow);
    }
    private boolean isAllowNoPassword(AssertionInfoMap aim) throws WSSecurityException {
        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);

        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                UsernameToken policy = (UsernameToken)ai.getAssertion();
                if (policy.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
                    return true;
                }
            }
        }

        return false;
    }

    protected SecurityContext createSecurityContext(final Principal p, Subject subject) {
        return new DefaultSecurityContext(p, subject);
    }

    /**
     * Create a Subject representing a current user and its roles.
     * This Subject is expected to contain at least one Principal representing a user
     * and optionally followed by one or more principal Groups this user is a member of.
     * @param name username
     * @param password password
     * @param isDigest true if a password digest is used
     * @param nonce optional nonce
     * @param created optional timestamp
     * @return subject
     * @throws SecurityException
     */
    protected Subject createSubject(String name,
                                    String password,
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException {
        return null;
    }

    protected UsernameToken assertTokens(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN10);
        PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN11);
        PolicyUtils.assertPolicy(aim, SPConstants.HASH_PASSWORD);
        PolicyUtils.assertPolicy(aim, SPConstants.NO_PASSWORD);
        PolicyUtils.assertPolicy(aim, SP13Constants.NONCE);
        PolicyUtils.assertPolicy(aim, SP13Constants.CREATED);

        return (UsernameToken)assertTokens(message, SPConstants.USERNAME_TOKEN, true);
    }

    private UsernameToken assertTokens(
        SoapMessage message,
        UsernameTokenPrincipal princ,
        boolean signed
    ) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken)ai.getAssertion();
            ai.setAsserted(true);
            if ((tok.getPasswordType() == UsernameToken.PasswordType.HashPassword)
                && (princ == null || !princ.isPasswordDigest())) {
                ai.setNotAsserted("Password hashing policy not enforced");
            } else {
                PolicyUtils.assertPolicy(aim, SPConstants.HASH_PASSWORD);
            }

            if ((tok.getPasswordType() != UsernameToken.PasswordType.NoPassword)
                && isNonEndorsingSupportingToken(tok)
                && (princ == null || princ.getPassword() == null)) {
                ai.setNotAsserted("Username Token No Password supplied");
            } else {
                PolicyUtils.assertPolicy(aim, SPConstants.NO_PASSWORD);
            }

            if (tok.isCreated() && (princ == null || princ.getCreatedTime() == null)) {
                ai.setNotAsserted("No Created Time");
            } else {
                PolicyUtils.assertPolicy(aim, SP13Constants.CREATED);
            }

            if (tok.isNonce() && princ.getNonce() == null) {
                ai.setNotAsserted("No Nonce");
            } else {
                PolicyUtils.assertPolicy(aim, SP13Constants.NONCE);
            }
        }

        PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN10);
        PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN11);
        PolicyUtils.assertPolicy(aim, SPConstants.SUPPORTING_TOKENS);

        if (signed || isTLSInUse(message)) {
            PolicyUtils.assertPolicy(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        }
        return tok;
    }

    /**
     * Return true if this UsernameToken policy is a (non-endorsing)SupportingToken. If this is
     * true then the corresponding UsernameToken must have a password element.
     */
    private boolean isNonEndorsingSupportingToken(
        org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy
    ) {
        AbstractSecurityAssertion supportingToken = usernameTokenPolicy.getParentAssertion();
        return !(supportingToken instanceof SupportingTokens
            && ((SupportingTokens)supportingToken).isEndorsing());
    }

    protected void addToken(SoapMessage message) {
        UsernameToken tok = assertTokens(message);
        Header h = findSecurityHeader(message, true);
        Element el = (Element)h.getObject();
        Document doc = el.getOwnerDocument();

        WSSecUsernameToken utBuilder =
            addUsernameToken(message, doc, tok);
        if (utBuilder == null) {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            Collection<AssertionInfo> ais =
                PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
            for (AssertionInfo ai : ais) {
                if (ai.isAsserted()) {
                    ai.setAsserted(false);
                }
            }
            return;
        }
        utBuilder.prepare();
        el.appendChild(utBuilder.getUsernameTokenElement());
    }


    protected WSSecUsernameToken addUsernameToken(SoapMessage message, Document doc, UsernameToken token) {
        String userName =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);
        WSSConfig wssConfig = (WSSConfig)message.getContextualProperty(WSSConfig.class.getName());
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
        }

        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            if (token.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(doc);
                utBuilder.setIdAllocator(wssConfig.getIdAllocator());
                utBuilder.setWsTimeSource(wssConfig.getCurrentTime());
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
                return utBuilder;
            }

            String password =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.PASSWORD, message);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN, message);
            }

            if (!StringUtils.isEmpty(password)) {
                //If the password is available then build the token
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(doc);
                utBuilder.setIdAllocator(wssConfig.getIdAllocator());
                utBuilder.setWsTimeSource(wssConfig.getCurrentTime());
                if (token.getPasswordType() == UsernameToken.PasswordType.HashPassword) {
                    utBuilder.setPasswordType(WSS4JConstants.PASSWORD_DIGEST);
                } else {
                    utBuilder.setPasswordType(WSS4JConstants.PASSWORD_TEXT);
                }

                if (token.isCreated()) {
                    utBuilder.addCreated();
                }
                if (token.isNonce()) {
                    utBuilder.addNonce();
                }

                utBuilder.setUserInfo(userName, password);
                return utBuilder;
            }
            policyNotAsserted(token, "No username available", message);
        } else {
            policyNotAsserted(token, "No username available", message);
        }
        return null;
    }


}
