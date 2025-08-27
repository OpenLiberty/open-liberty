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
package org.apache.xml.security.stax.ext;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.xml.security.stax.impl.DocumentContextImpl;
import org.apache.xml.security.stax.impl.InboundSecurityContextImpl;
import org.apache.xml.security.stax.impl.InputProcessorChainImpl;
import org.apache.xml.security.stax.impl.XMLSecurityStreamReader;
import org.apache.xml.security.stax.impl.processor.input.LogInputProcessor;
import org.apache.xml.security.stax.impl.processor.input.XMLEventReaderInputProcessor;
import org.apache.xml.security.stax.impl.processor.input.XMLSecurityInputProcessor;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound Streaming-XML-Security
 * An instance of this class can be retrieved over the XMLSec class
 *
 */
public class InboundXMLSec {

    protected static final transient Logger LOG = LoggerFactory.getLogger(InboundXMLSec.class);

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            xmlInputFactory.setProperty("org.codehaus.stax2.internNames", true);
            xmlInputFactory.setProperty("org.codehaus.stax2.internNsUris", true);
            xmlInputFactory.setProperty("org.codehaus.stax2.preserveLocation", false);
        } catch (IllegalArgumentException e) {
            LOG.debug(e.getMessage(), e);
            //ignore
        }
    }

    private final XMLSecurityProperties securityProperties;

    public InboundXMLSec(XMLSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     * <p></p>
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     *
     * @param xmlStreamReader The original XMLStreamReader
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException  thrown when a streaming error occurs
     */
    public XMLStreamReader processInMessage(XMLStreamReader xmlStreamReader) throws XMLStreamException {
        return processInMessage(xmlStreamReader, null, null);
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     * <p></p>
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     *
     * @param xmlStreamReader The original XMLStreamReader
     * @param requestSecurityEvents A List of requested SecurityEvents
     * @param securityEventListener A SecurityEventListener to receive security-relevant events.
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException  thrown when a streaming error occurs
     */
    public XMLStreamReader processInMessage(
            XMLStreamReader xmlStreamReader, List<SecurityEvent> requestSecurityEvents,
            SecurityEventListener securityEventListener) throws XMLStreamException {

        if (requestSecurityEvents == null) {
            requestSecurityEvents = Collections.emptyList();
        }

        final InboundSecurityContextImpl inboundSecurityContext = new InboundSecurityContextImpl();
        inboundSecurityContext.putList(SecurityEvent.class, requestSecurityEvents);
        inboundSecurityContext.addSecurityEventListener(securityEventListener);

        inboundSecurityContext.put(XMLSecurityConstants.XMLINPUTFACTORY, xmlInputFactory);

        DocumentContextImpl documentContext = new DocumentContextImpl();
        documentContext.setEncoding(xmlStreamReader.getEncoding() != null ? xmlStreamReader.getEncoding() : java.nio.charset.StandardCharsets.UTF_8.name());
        //woodstox 3.2.9 returns null when used with a DOMSource
        Location location = xmlStreamReader.getLocation();
        if (location != null) {
            documentContext.setBaseURI(location.getSystemId());
        }

        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(inboundSecurityContext, documentContext);
        inputProcessorChain.addProcessor(new XMLEventReaderInputProcessor(securityProperties, xmlStreamReader));

        List<InputProcessor> additionalInputProcessors = securityProperties.getInputProcessorList();
        if (!additionalInputProcessors.isEmpty()) {
            for (InputProcessor inputProcessor : additionalInputProcessors) {
                inputProcessorChain.addProcessor(inputProcessor);
            }
        }

        inputProcessorChain.addProcessor(new XMLSecurityInputProcessor(securityProperties));

        if (LOG.isTraceEnabled()) {
            LogInputProcessor logInputProcessor = new LogInputProcessor(securityProperties);
            logInputProcessor.addAfterProcessor(XMLSecurityInputProcessor.class.getName());
            inputProcessorChain.addProcessor(logInputProcessor);
        }

        return new XMLSecurityStreamReader(inputProcessorChain, securityProperties);
    }
}
