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

import java.security.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import javax.xml.soap.SOAPMessage;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.action.Action;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;

public class WSS4JOutInterceptor extends AbstractWSS4JInterceptor {

    /**
     * Property name for a map of action IDs ({@link Integer}) to action
     * class names. Values can be either {@link Class}) or Objects
-    * implementing {@link Action}.
     */
    public static final String WSS4J_ACTION_MAP = "wss4j.action.map";

    private static final Logger LOG = LogUtils
            .getL7dLogger(WSS4JOutInterceptor.class);

    private WSS4JOutInterceptorInternal ending;
    private SAAJOutInterceptor saajOut = new SAAJOutInterceptor();

    public WSS4JOutInterceptor() {
        super();
        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJOutInterceptor.class.getName());

        ending = createEndingInterceptor();
    }

    public WSS4JOutInterceptor(Map<String, Object> props) {
        this();
        setProperties(props);
    }

    @Override
    public Object getProperty(Object msgContext, String key) {
        // use the superclass first
        Object result = super.getProperty(msgContext, key);

        // handle the special case of the RECV_RESULTS
        if (result == null
            && WSHandlerConstants.RECV_RESULTS.equals(key)
            && !this.isRequestor((SoapMessage)msgContext)) {
            result = ((SoapMessage)msgContext).getExchange().getInMessage().get(key);
        }
        return result;
    }

    public void handleMessage(SoapMessage mc) throws Fault {
        if (mc.getContent(SOAPMessage.class) == null) {
            saajOut.handleMessage(mc);
        }

        // If a custom Id has been set, then change the Id for the internal interceptor as well, as otherwise
        // we can't add two WSS4JOutInterceptor instances to the interceptor chain.
        if (!WSS4JOutInterceptor.class.getName().equals(getId())) {
            Random random = new Random();
            int randomInt = random.nextInt();
            ending.setId(WSS4JOutInterceptorInternal.class.getName() + "_" + randomInt);
        }
        mc.getInterceptorChain().add(ending);
    }
    public void handleFault(SoapMessage message) {
        saajOut.handleFault(message);
    }

    public final WSS4JOutInterceptorInternal createEndingInterceptor() {
        return new WSS4JOutInterceptorInternal();
    }

    final class WSS4JOutInterceptorInternal
        implements PhaseInterceptor<SoapMessage> {
        private String id = WSS4JOutInterceptorInternal.class.getName();

        WSS4JOutInterceptorInternal() {
            super();
        }

        public void handleMessage(SoapMessage message) throws Fault {
            Object provider = message.getExchange().get(Provider.class);
            final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
            try {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.setProvider((Provider)provider);
                }
                handleMessageInternal(message);
            } finally {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.unsetProvider();
                }
            }
        }

        private void handleMessageInternal(SoapMessage mc) throws Fault {

            boolean doDebug = LOG.isLoggable(Level.FINE);

            if (doDebug) {
                LOG.fine("WSS4JOutInterceptor: enter handleMessage()");
            }
            /**
             * There is nothing to send...Usually happens when the provider
             * needs to send a HTTP 202 message (with no content)
             */
            if (mc == null) {
                return;
            }
            SoapVersion version = mc.getVersion();

            /*
             * The overall try, just to have a finally at the end to perform some
             * housekeeping.
             */
            try {
                RequestData reqData = new RequestData();

                WSSConfig config = WSSConfig.getNewInstance();
                reqData.setWssConfig(config);
                reqData.setEncryptionSerializer(new StaxSerializer());

                /*
                 * Setup any custom actions first by processing the input properties
                 * and reconfiguring the WSSConfig with the user defined properties.
                 */
                this.configureActions(mc, doDebug, version, config);

                /*
                 * Get the action first.
                 */
                List<HandlerAction> actions =
                    CastUtils.cast((List<?>)getProperty(mc, WSHandlerConstants.HANDLER_ACTIONS));
                if (actions == null) {
                    // If null then just fall back to the "action" String
                    String action = getString(ConfigurationConstants.ACTION, mc);
                    if (action == null) {
                        throw new SoapFault(new Message("NO_ACTION", LOG), version
                                .getReceiver());
                    }

                    actions = WSSecurityUtil.decodeHandlerAction(action, config);
                }
                if (actions.isEmpty()) {
                    return;
                }

                translateProperties(mc);
                reqData.setMsgContext(mc);
                reqData.setAttachmentCallbackHandler(new AttachmentCallbackHandler(mc));

                // Enable XOP Include unless the user has explicitly configured it
                if (getString(ConfigurationConstants.EXPAND_XOP_INCLUDE, mc) == null) {
                    reqData.setExpandXopInclude(AttachmentUtil.isMtomEnabled(mc));
                }
                if (getString(ConfigurationConstants.STORE_BYTES_IN_ATTACHMENT, mc) == null) {
                    reqData.setStoreBytesInAttachment(AttachmentUtil.isMtomEnabled(mc));
                }

                /*
                 * For every action we need a username, so get this now. The
                 * username defined in the deployment descriptor takes precedence.
                 */
                reqData.setUsername((String) getOption(ConfigurationConstants.USER));
                if (reqData.getUsername() == null || reqData.getUsername().isEmpty()) {
                    String username = (String) getProperty(reqData.getMsgContext(),
                            ConfigurationConstants.USER);
                    if (username != null) {
                        reqData.setUsername(username);
                    }
                }

                // Check to see if we require a username (+ if it's missing)
                boolean userNameRequired = false;
                for (HandlerAction handlerAction : actions) {
                    if ((handlerAction.getAction() == WSConstants.SIGN
                        || handlerAction.getAction() == WSConstants.UT
                        || handlerAction.getAction() == WSConstants.UT_NOPASSWORD
                        || handlerAction.getAction() == WSConstants.UT_SIGN)
                        && (handlerAction.getActionToken() == null
                            || handlerAction.getActionToken().getUser() == null)) {
                        userNameRequired = true;
                        break;
                    }
                }
                if (userNameRequired && (reqData.getUsername() == null || reqData.getUsername().isEmpty())
                        && (String)getOption(ConfigurationConstants.SIGNATURE_USER) == null) {
                    throw new SoapFault(new Message("NO_USERNAME", LOG), version
                            .getReceiver());
                }

                if (doDebug) {
                    LOG.fine("Actor: " + reqData.getActor());
                }
                /*
                 * Now get the SOAP part from the request message and convert it
                 * into a Document. This forces CXF to serialize the SOAP request
                 * into FORM_STRING. This string is converted into a document.
                 * During the FORM_STRING serialization CXF performs multi-ref of
                 * complex data types (if requested), generates and inserts
                 * references for attachments and so on. The resulting Document
                 * MUST be the complete and final SOAP request as CXF would send it
                 * over the wire. Therefore this must shall be the last (or only)
                 * handler in a chain. Now we can perform our security operations on
                 * this request.
                 */

                SOAPMessage saaj = mc.getContent(SOAPMessage.class);

                if (saaj == null) {
                    LOG.warning("SAAJOutHandler must be enabled for WS-Security!");
                    throw new SoapFault(new Message("NO_SAAJ_DOC", LOG), version
                            .getReceiver());
                }

                Document doc = saaj.getSOAPPart();

                doSenderAction(doc, reqData, actions, Boolean.TRUE
                        .equals(getProperty(mc, org.apache.cxf.message.Message.REQUESTOR_ROLE)));

                if (doDebug) {
                    LOG.fine("WSS4JOutInterceptor: exit handleMessage()");
                }
            } catch (InvalidCanonicalizerException | WSSecurityException e) {
                throw new SoapFault(new Message("SECURITY_FAILED", LOG), e, version
                        .getSender());
            }
        }

        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        public Set<String> getBefore() {
            return Collections.emptySet();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPhase() {
            return Phase.POST_PROTOCOL;
        }

        public void handleFault(SoapMessage message) {
            //nothing
        }

        private void configureActions(SoapMessage mc, boolean doDebug,
                SoapVersion version, WSSConfig config) {

            final Map<Integer, Object> actionMap = CastUtils.cast(
                (Map<?, ?>)getProperty(mc, WSS4J_ACTION_MAP));
            if (actionMap != null && !actionMap.isEmpty()) {
                for (Map.Entry<Integer, Object> entry : actionMap.entrySet()) {
                    final Class<?> removedAction;

                    // Be defensive here since the cast above is slightly risky
                    // with the handler config options not being strongly typed.
                    try {
                        if (entry.getValue() instanceof Class<?>) {
                            removedAction = config.setAction(
                                    entry.getKey().intValue(),
                                    (Class<?>)entry.getValue());
                        } else if (entry.getValue() instanceof Action) {
                            removedAction = config.setAction(
                                    entry.getKey().intValue(),
                                    (Action)entry.getValue());
                        } else {
                            throw new SoapFault(new Message("BAD_ACTION", LOG), version
                                    .getReceiver());
                        }
                    } catch (ClassCastException e) {
                        throw new SoapFault(new Message("BAD_ACTION", LOG), version
                                .getReceiver());
                    }

                    if (doDebug) {
                        if (removedAction != null) {
                            LOG.fine("Replaced Action: " + removedAction.getName()
                                    + " with Action: " + entry.getValue()
                                    + " for ID: " + entry.getKey());
                        } else {
                            LOG.fine("Added Action: " + entry.getValue()
                                    + " with ID: " + entry.getKey());
                        }
                    }
                }
            }
        }

        public Collection<PhaseInterceptor<? extends org.apache.cxf.message.Message>> getAdditionalInterceptors() {
            return null;
        }
    }
}
