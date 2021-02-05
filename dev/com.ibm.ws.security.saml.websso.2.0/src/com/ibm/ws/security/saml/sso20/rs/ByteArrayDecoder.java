/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.rs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

//import org.opensaml.xml.Configuration;
import org.opensaml.core.config.Configuration; //@AV999
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
//import org.opensaml.common.binding.SAMLMessageContext;
//import org.opensaml.ws.message.MessageContext;
import org.opensaml.messaging.context.MessageContext; //@AV999
//import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.MessageDecodingException; //@AV999
import org.opensaml.saml.common.SAMLObject; //@AV999
//import org.opensaml.xml.parse.ParserPool; //@AV999
//import org.opensaml.xml.parse.XMLParserException;
//import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.QNameSupport;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/** Message decoder implementing the SAML 2.0 HTTP POST binding. */
public class ByteArrayDecoder {
    private static final TraceComponent tc = Tr.register(ByteArrayDecoder.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    // ParserPool parserPool = Configuration.getParserPool(); //@AV999
    ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool(); //@AV999

    /** Constructor. */
    public ByteArrayDecoder() {}

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    protected void doDecode(/*MessageContext messageContext*/BasicMessageContext<?, ?> messageContext, ByteArrayInputStream byteArrayInputStream) throws MessageDecodingException {
        //SAMLMessageContext samlMsgCtx = (SAMLMessageContext) messageContext; //@AV999
        InputStream base64DecodedMessage = byteArrayInputStream;
        SAMLObject inboundMessage = (SAMLObject) unmarshallMessage(base64DecodedMessage);
        messageContext.getMessageContext().setMessage(inboundMessage); //@AV999 major change
        //samlMsgCtx.setInboundMessage(inboundMessage); //@AV999 TODO
        //samlMsgCtx.setInboundSAMLMessage(inboundMessage);
        // Should only happen during testing the installation
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Decoded SAML message");
        }
    }

    /**
     * Helper method that deserializes and unmarshalls the message from the given stream.
     *
     * @param messageStream input stream containing the message
     *
     * @return the inbound message
     *
     * @throws MessageDecodingException thrown if there is a problem deserializing and unmarshalling the message
     */
    protected XMLObject unmarshallMessage(InputStream messageStream) throws MessageDecodingException {

        try {
            Document messageDoc = parserPool.parse(messageStream);
            Element messageElem = messageDoc.getDocumentElement();
            if (tc.isDebugEnabled()) {
                //Tr.debug(tc, "Resultant DOM message was:\n{}", messageElem == null ? "null" : XMLHelper.nodeToString(messageElem)); //@AV999
                Tr.debug(tc, "Resultant DOM message was:\n{}", messageElem == null ? "null" : SerializeSupport.nodeToString(messageElem)); //@AV999
                Tr.debug(tc, "Unmarshalling message DOM");
            }

            //Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(messageElem); //@AV999
            Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(messageElem); //@AV999
            if (unmarshaller == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to unmarshall message, no unmarshaller registered for message element "
                                 + QNameSupport.getNodeQName(messageElem)); //@AV999
                }
                throw new MessageDecodingException("Unable to unmarshall message, no unmarshaller registered for message element "
                                                   + QNameSupport.getNodeQName(messageElem)); //@AV999
            }

            XMLObject message = unmarshaller.unmarshall(messageElem);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Message succesfully unmarshalled");
            }
            return message;
        } catch (XMLParserException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "XMLParserException e:" + e);
            }
            throw new MessageDecodingException("Encountered error parsing message into its DOM representation", e);
        } catch (UnmarshallingException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "UnmarshallingException e:" + e);
            }
            throw new MessageDecodingException("Encountered error unmarshalling message from its DOM representation", e);
        }
    }

}