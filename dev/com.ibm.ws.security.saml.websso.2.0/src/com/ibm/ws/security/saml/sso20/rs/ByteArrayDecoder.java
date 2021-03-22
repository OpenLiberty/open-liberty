/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.rs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SAMLObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.QNameSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/** Message decoder implementing the SAML 2.0 HTTP POST binding. */
public class ByteArrayDecoder {
    private static final TraceComponent tc = Tr.register(ByteArrayDecoder.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    
    ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool(); //v3

    /** Constructor. */
    public ByteArrayDecoder() {
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    protected void doDecode(/* MessageContext messageContext */BasicMessageContext<?, ?> messageContext,
                            ByteArrayInputStream byteArrayInputStream) throws MessageDecodingException {
        InputStream base64DecodedMessage = byteArrayInputStream;
        SAMLObject inboundMessage = (SAMLObject) unmarshallMessage(base64DecodedMessage);
        messageContext.getMessageContext().setMessage(inboundMessage); //v3
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
                Tr.debug(tc, "Resultant DOM message was:\n{}", messageElem == null ? "null" : SerializeSupport.nodeToString(messageElem));
                Tr.debug(tc, "Unmarshalling message");
            }

            //Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(messageElem)
            Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(messageElem);
            if (unmarshaller == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to unmarshall message, no unmarshaller registered for message element "
                                 + QNameSupport.getNodeQName(messageElem));
                }
                throw new MessageDecodingException("Unable to unmarshall message, no unmarshaller registered for message element "
                                                   + QNameSupport.getNodeQName(messageElem));
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