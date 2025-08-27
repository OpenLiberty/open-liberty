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
package org.apache.wss4j.stax.impl.processor.input;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSInboundSecurityContext;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.NoSecuritySecurityEvent;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.config.SecurityHeaderHandlerMapper;
import org.apache.xml.security.stax.ext.AbstractInputProcessor;
import org.apache.xml.security.stax.ext.InputProcessorChain;
import org.apache.xml.security.stax.ext.XMLSecurityHeaderHandler;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.ext.stax.XMLSecEndElement;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.processor.input.XMLEventReaderInputProcessor;
import org.apache.xml.security.stax.impl.util.IDGenerator;

/**
 * Processor for the Security-Header XML Structure.
 * This processor instantiates more processors on demand
 */
public class SecurityHeaderInputProcessor extends AbstractInputProcessor {

    protected static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SecurityHeaderInputProcessor.class);

    private final ArrayDeque<XMLSecEvent> xmlSecEventList = new ArrayDeque<>();
    private int startIndexForProcessor;

    public SecurityHeaderInputProcessor(WSSSecurityProperties securityProperties) {
        super(securityProperties);
        setPhase(WSSConstants.Phase.POSTPROCESSING);
    }

    @Override
    public XMLSecEvent processHeaderEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {
        return null;
    }

    @Override
    public XMLSecEvent processEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {

        //buffer all events until the end of the security header
        final InputProcessorChain subInputProcessorChain = inputProcessorChain.createSubChain(this);
        final InternalSecurityHeaderBufferProcessor internalSecurityHeaderBufferProcessor
                = new InternalSecurityHeaderBufferProcessor(getSecurityProperties());
        subInputProcessorChain.addProcessor(internalSecurityHeaderBufferProcessor);

        boolean responsibleSecurityHeaderFound = false;
        boolean timestampFound = false;

        XMLSecEvent xmlSecEvent;
        do {
            subInputProcessorChain.reset();
            xmlSecEvent = subInputProcessorChain.processHeaderEvent();

            switch (xmlSecEvent.getEventType()) {   //NOPMD
                case XMLStreamConstants.START_ELEMENT:
                    XMLSecStartElement xmlSecStartElement = xmlSecEvent.asStartElement();
                    int documentLevel = xmlSecStartElement.getDocumentLevel();

                    if (documentLevel == 1) {
                        if (WSSUtils.getSOAPMessageVersionNamespace(xmlSecStartElement) == null) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "notASOAPMessage");
                        }
                    } else if (documentLevel == 3
                            && xmlSecStartElement.getName().equals(WSSConstants.TAG_WSSE_SECURITY)
                            && WSSUtils.isInSOAPHeader(xmlSecStartElement)) {

                        if (!WSSUtils.isResponsibleActorOrRole(xmlSecStartElement,
                                ((WSSSecurityProperties) getSecurityProperties()).getActor())) {
                            continue;
                        }
                        responsibleSecurityHeaderFound = true;

                    } else if (documentLevel == 4 && responsibleSecurityHeaderFound
                            && WSSUtils.isInSecurityHeader(xmlSecStartElement,
                            ((WSSSecurityProperties) getSecurityProperties()).getActor())) {
                        startIndexForProcessor = xmlSecEventList.size() - 1;

                        //special handling for EncryptedData in the SecurityHeader. This way, if for example
                        // a token was encrypted we have the possibility to decrypt it before so that we
                        // are able to engage the appropriate processor for the token.
                        if (WSSConstants.TAG_xenc_EncryptedData.equals(xmlSecStartElement.getName())) {
                            engageSecurityHeaderHandler(subInputProcessorChain, getSecurityProperties(),
                                    xmlSecEventList, startIndexForProcessor, xmlSecStartElement.getName());
                        }
                    } else if (documentLevel == 5 && responsibleSecurityHeaderFound
                            && WSSUtils.isInSecurityHeader(xmlSecStartElement,
                            ((WSSSecurityProperties) getSecurityProperties()).getActor())
                            && WSSConstants.TAG_xenc_EncryptedData.equals(xmlSecStartElement.getName())) {
                        startIndexForProcessor = xmlSecEventList.size() - 1;

                        // Same goes as per EncryptedData above. This is when a child of a security header
                        // element is encrypted (e.g. EncryptedAssertion)
                        engageSecurityHeaderHandler(subInputProcessorChain, getSecurityProperties(),
                                xmlSecEventList, startIndexForProcessor, xmlSecStartElement.getName());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    XMLSecEndElement xmlSecEndElement = xmlSecEvent.asEndElement();
                    documentLevel = xmlSecEndElement.getDocumentLevel();
                    if (documentLevel == 3 && responsibleSecurityHeaderFound
                            && xmlSecEndElement.getName().equals(WSSConstants.TAG_WSSE_SECURITY)) {

                        return finalizeHeaderProcessing(
                                inputProcessorChain, subInputProcessorChain,
                                internalSecurityHeaderBufferProcessor, xmlSecEventList);

                    } else if (documentLevel == 4 && responsibleSecurityHeaderFound
                            && WSSUtils.isInSecurityHeader(xmlSecEndElement,
                            ((WSSSecurityProperties) getSecurityProperties()).getActor())) {
                        //we are in the security header and the depth is +1, so every child
                        //element should have a responsible handler with the exception of an EncryptedData SecurityHeader
                        //which is already handled in the above StartElement Logic (@see comment above).
                        if (!WSSConstants.TAG_xenc_EncryptedData.equals(xmlSecEndElement.getName())) {
                            engageSecurityHeaderHandler(subInputProcessorChain, getSecurityProperties(),
                                    xmlSecEventList, startIndexForProcessor, xmlSecEndElement.getName());
                        }

                        // Check for multiple timestamps
                        if (xmlSecEndElement.getName().equals(WSSConstants.TAG_WSU_TIMESTAMP)) {
                            if (timestampFound) {
                                WSInboundSecurityContext context =
                                    (WSInboundSecurityContext)subInputProcessorChain.getSecurityContext();
                                context.handleBSPRule(BSPRule.R3227);
                            }
                            timestampFound = true;
                        }
                    }
                    break;
            }

        } while (!(xmlSecEvent.getEventType() == XMLStreamConstants.START_ELEMENT
                && xmlSecEvent.asStartElement().getName().getLocalPart().equals(WSSConstants.TAG_SOAP_BODY_LN)
                && xmlSecEvent.asStartElement().getName().getNamespaceURI().equals(
                WSSUtils.getSOAPMessageVersionNamespace(xmlSecEvent.asStartElement()))
        ));
        //if we reach this state we didn't find a security header
        //issue a security event to notify about this fact:
        NoSecuritySecurityEvent noSecuritySecurityEvent = new NoSecuritySecurityEvent();
        noSecuritySecurityEvent.setCorrelationID(IDGenerator.generateID(null));
        inputProcessorChain.getSecurityContext().registerSecurityEvent(noSecuritySecurityEvent);

        return finalizeHeaderProcessing(
                inputProcessorChain, subInputProcessorChain,
                internalSecurityHeaderBufferProcessor, xmlSecEventList);
    }

    private XMLSecEvent finalizeHeaderProcessing(
            InputProcessorChain originalInputProcessorChain,
            InputProcessorChain subInputProcessorChain,
            InternalSecurityHeaderBufferProcessor internalSecurityHeaderBufferProcessor,
            Deque<XMLSecEvent> xmlSecEventList) {

        subInputProcessorChain.removeProcessor(internalSecurityHeaderBufferProcessor);
        subInputProcessorChain.addProcessor(
                new InternalSecurityHeaderReplayProcessor(getSecurityProperties()));

        //remove this processor from chain now. the next events will go directly to the other processors
        subInputProcessorChain.removeProcessor(this);
        //since we cloned the inputProcessor list we have to add the processors from
        //the subChain to the main chain.
        originalInputProcessorChain.getProcessors().clear();
        originalInputProcessorChain.getProcessors().addAll(subInputProcessorChain.getProcessors());

        //return first event now;
        return xmlSecEventList.pollLast();
    }

    @SuppressWarnings("unchecked")
    private void engageSecurityHeaderHandler(InputProcessorChain inputProcessorChain,
                                             XMLSecurityProperties securityProperties,
                                             Deque<XMLSecEvent> eventQueue,
                                             Integer index,
                                             QName elementName)
            throws WSSecurityException, XMLStreamException {

        Class<XMLSecurityHeaderHandler> clazz =
            (Class<XMLSecurityHeaderHandler>)SecurityHeaderHandlerMapper.getSecurityHeaderHandler(elementName);
        if (clazz == null) {
            LOG.warn("No matching handler found for " + elementName);
            return;
        }
        try {
            XMLSecurityHeaderHandler xmlSecurityHeaderHandler = clazz.getDeclaredConstructor().newInstance();
            xmlSecurityHeaderHandler.handle(inputProcessorChain, securityProperties, eventQueue, index);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
        } catch (WSSecurityException e) {
            throw e;
        } catch (XMLSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
        }
    }

    /**
     * Temporary Processor to buffer all events until the end of the security header
     */
    public class InternalSecurityHeaderBufferProcessor extends AbstractInputProcessor {

        InternalSecurityHeaderBufferProcessor(XMLSecurityProperties securityProperties) {
            super(securityProperties);
            setPhase(WSSConstants.Phase.POSTPROCESSING);
            addBeforeProcessor(SecurityHeaderInputProcessor.class.getName());
        }

        @Override
        public XMLSecEvent processHeaderEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            XMLSecEvent xmlSecEvent = inputProcessorChain.processHeaderEvent();
            xmlSecEventList.push(xmlSecEvent);
            return xmlSecEvent;
        }

        @Override
        public XMLSecEvent processEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            //should never be called because we remove this processor before
            return null;
        }
    }

    /**
     * Temporary processor to replay the buffered events
     */
    public class InternalSecurityHeaderReplayProcessor extends AbstractInputProcessor {

        public InternalSecurityHeaderReplayProcessor(XMLSecurityProperties securityProperties) {
            super(securityProperties);
            setPhase(WSSConstants.Phase.PREPROCESSING);
            addBeforeProcessor(SecurityHeaderInputProcessor.class.getName());
            addAfterProcessor(XMLEventReaderInputProcessor.class.getName());
        }

        @Override
        public XMLSecEvent processHeaderEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            return null;
        }

        @Override
        public XMLSecEvent processEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {

            if (!xmlSecEventList.isEmpty()) {
                return xmlSecEventList.pollLast();
            } else {
                inputProcessorChain.removeProcessor(this);
                return inputProcessorChain.processEvent();
            }
        }
    }
}
