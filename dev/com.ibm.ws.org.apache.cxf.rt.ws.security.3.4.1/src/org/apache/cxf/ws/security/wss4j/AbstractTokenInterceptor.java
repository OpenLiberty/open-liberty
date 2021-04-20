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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractToken;

/**
 * An abstract interceptor that can be used to form the basis of an interceptor to add and process
 * a specific type of security token.
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public abstract class AbstractTokenInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSoapInterceptor.class);
    private static final Set<QName> HEADERS =
        Collections.singleton(new QName(WSConstants.WSSE_NS, "Security"));

    public AbstractTokenInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(PolicyBasedWSS4JOutInterceptor.class.getName());
        addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        addAfter(PolicyBasedWSS4JStaxInInterceptor.class.getName());
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public void handleMessage(SoapMessage message) throws Fault {

        boolean enableStax =
            MessageUtils.getContextualBoolean(message, SecurityConstants.ENABLE_STREAMING_SECURITY, false);
        if (enableStax) {
            return;
        }

        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);

        if (isReq != isOut) {
            //outbound on server side and inbound on client side doesn't need
            //any specific token stuff, assert policies and return
            assertTokens(message);
            return;
        }
        if (isReq) {
            if (message.containsKey(PolicyBasedWSS4JOutInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            addToken(message);
        } else {
            if (message.containsKey(WSS4JInInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            processToken(message);
        }
    }

    protected abstract void processToken(SoapMessage message);

    protected abstract void addToken(SoapMessage message);

    protected abstract AbstractToken assertTokens(SoapMessage message);

    protected AbstractToken assertTokens(SoapMessage message, String localname, boolean signed) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = PolicyUtils.getAllAssertionsByLocalname(aim, localname);
        AbstractToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (AbstractToken)ai.getAssertion();
            ai.setAsserted(true);
        }

        PolicyUtils.assertPolicy(aim, SPConstants.SUPPORTING_TOKENS);

        if (signed || isTLSInUse(message)) {
            PolicyUtils.assertPolicy(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        }
        return tok;
    }

    protected boolean isTLSInUse(SoapMessage message) {
        // See whether TLS is in use or not
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        return tlsInfo != null;
    }

    protected TokenStore getTokenStore(SoapMessage message) {
        EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore =
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            return tokenStore;
        }
    }

    protected Header findSecurityHeader(SoapMessage message, boolean create) {
        String actor = (String)message.getContextualProperty(SecurityConstants.ACTOR);
        for (Header h : message.getHeaders()) {
            QName n = h.getName();
            if ("Security".equals(n.getLocalPart())
                && (n.getNamespaceURI().equals(WSS4JConstants.WSSE_NS)
                    || n.getNamespaceURI().equals(WSS4JConstants.WSSE11_NS))) {
                String receivedActor = ((SoapHeader)h).getActor();
                if (actor == null || actor.equalsIgnoreCase(receivedActor)) {
                    return h;
                }
            }
        }
        if (!create) {
            return null;
        }
        Document doc = DOMUtils.getEmptyDocument();
        Element el = doc.createElementNS(WSS4JConstants.WSSE_NS, "wsse:Security");
        el.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:wsse", WSS4JConstants.WSSE_NS);

        SoapHeader sh = new SoapHeader(new QName(WSS4JConstants.WSSE_NS, "Security"), el);
        sh.setMustUnderstand(true);
        if (actor != null && actor.length() > 0) {
            sh.setActor(actor);
        }
        message.getHeaders().add(sh);
        return sh;
    }

    protected String getPassword(String userName, AbstractToken info,
                                 int usage, SoapMessage message) {
        //Then try to get the password from the given callback handler
        CallbackHandler handler = null;
        try {
            Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
            handler = SecurityUtils.getCallbackHandler(o);
            if (handler == null) {
                policyNotAsserted(info, "No callback handler and no password available", message);
                return null;
            }
        } catch (Exception ex) {
            policyNotAsserted(info, "No callback handler and no password available", message);
            return null;
        }

        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, usage)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            policyNotAsserted(info, e, message);
        }

        //get the password
        return cb[0].getPassword();
    }

    protected void policyNotAsserted(AbstractToken assertion, String reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        Collection<AssertionInfo> ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason);
                }
            }
        }
        if (!assertion.isOptional()) {
            throw new PolicyException(new Message(reason, LOG));
        }
    }

    protected void policyNotAsserted(AbstractToken assertion, Exception reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason.getMessage());
                }
            }
        }
        throw new PolicyException(reason);
    }


}
