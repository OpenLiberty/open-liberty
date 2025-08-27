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
package org.apache.wss4j.stax.impl.processor.output;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.SecurityHeaderOrder;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.AbstractOutputProcessor;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.processor.output.FinalOutputProcessor;

/**
 * The basic ordering (token dependencies) is given through the processor order
 * but we have more ordering criterias e.g. signed timestamp and strict header ordering ws-policy.
 * To be able to sign a timestamp the processor must be inserted before the signature processor but that
 * means that the timestamp is below the signature in the sec-header. Because of the highly dynamic nature
 * of the processor chain (and encryption makes it far more worse) we have to order the headers afterwards.
 * So that is what this processor does, the final header reordering...
 */
public class SecurityHeaderReorderProcessor extends AbstractOutputProcessor {

    private final Map<XMLSecurityConstants.Action, Map<SecurityHeaderOrder, Deque<XMLSecEvent>>> actionEventMap =
            new LinkedHashMap<>();

    private int securityHeaderIndex;
    private Deque<XMLSecEvent> currentDeque;

    public SecurityHeaderReorderProcessor() throws XMLSecurityException {
        super();
        setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        addBeforeProcessor(FinalOutputProcessor.class);
    }

    @Override
    public void init(OutputProcessorChain outputProcessorChain) throws XMLSecurityException {
        super.init(outputProcessorChain);

        List<XMLSecurityConstants.Action> outActions = getSecurityProperties().getActions();
        for (int i = outActions.size() - 1; i >= 0; i--) {
            XMLSecurityConstants.Action outAction = outActions.get(i);
            actionEventMap.put(outAction, new TreeMap<SecurityHeaderOrder, Deque<XMLSecEvent>>(new Comparator<SecurityHeaderOrder>() {
                @Override
                public int compare(SecurityHeaderOrder o1, SecurityHeaderOrder o2) {
                    if (WSSConstants.TAG_dsig_Signature.equals(o1.getSecurityHeaderElementName())) {
                        return 1;
                    } else if (WSSConstants.TAG_dsig_Signature.equals(o2.getSecurityHeaderElementName())) {
                        return -1;
                    }
                    return 1;
                }
            }));
        }
    }

    @Override
    public void processEvent(XMLSecEvent xmlSecEvent, OutputProcessorChain outputProcessorChain)
        throws XMLStreamException, XMLSecurityException {

        int documentLevel = xmlSecEvent.getDocumentLevel();
        if (documentLevel < 3
            || !WSSUtils.isInSecurityHeader(xmlSecEvent, ((WSSSecurityProperties) getSecurityProperties()).getActor())) {
            outputProcessorChain.processEvent(xmlSecEvent);
            return;
        }

        //now we are in our security header

        if (documentLevel == 3) {
            if (xmlSecEvent.isEndElement() && xmlSecEvent.asEndElement().getName().equals(WSSConstants.TAG_WSSE_SECURITY)) {
                OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);

                Iterator<Map.Entry<XMLSecurityConstants.Action, Map<SecurityHeaderOrder, Deque<XMLSecEvent>>>> iterator =
                    actionEventMap.entrySet().iterator();
                loop:
                while (iterator.hasNext()) {
                    Map.Entry<XMLSecurityConstants.Action, Map<SecurityHeaderOrder, Deque<XMLSecEvent>>> next = iterator.next();

                    boolean encryptAction = false;
                    Iterator<Map.Entry<SecurityHeaderOrder, Deque<XMLSecEvent>>> entryIterator = next.getValue().entrySet().iterator();
                    while (entryIterator.hasNext()) {
                        Map.Entry<SecurityHeaderOrder, Deque<XMLSecEvent>> entry = entryIterator.next();
                        //output all non encrypted headers until...
                        if (!entry.getKey().isEncrypted()) {
                            Deque<XMLSecEvent> xmlSecEvents = entry.getValue();
                            while (!xmlSecEvents.isEmpty()) {
                                XMLSecEvent event = xmlSecEvents.pop();
                                subOutputProcessorChain.reset();
                                subOutputProcessorChain.processEvent(event);
                            }
                            //remove the actual header so that it won't be output twice in the loop below
                            entryIterator.remove();
                        }
                        //... the action is encryption and...
                        if (entry.getKey().getAction().getName().contains("Encrypt")) {
                            encryptAction = true;
                        }
                    }
                    //...output the rest of the encrypt action and...
                    if (encryptAction) {
                        break loop;
                    }
                }
                //...loop again over the headers and output the leftover headers
                iterator = actionEventMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<XMLSecurityConstants.Action, Map<SecurityHeaderOrder, Deque<XMLSecEvent>>> next = iterator.next();
                    Iterator<Map.Entry<SecurityHeaderOrder, Deque<XMLSecEvent>>> entryIterator = next.getValue().entrySet().iterator();
                    while (entryIterator.hasNext()) {
                        Map.Entry<SecurityHeaderOrder, Deque<XMLSecEvent>> entry = entryIterator.next();
                        Deque<XMLSecEvent> xmlSecEvents = entry.getValue();
                        while (!xmlSecEvents.isEmpty()) {
                            XMLSecEvent event = xmlSecEvents.pop();
                            subOutputProcessorChain.reset();
                            subOutputProcessorChain.processEvent(event);
                        }
                    }
                }
                outputProcessorChain.removeProcessor(this);
            }
            outputProcessorChain.processEvent(xmlSecEvent);
            return;
        } else if (documentLevel == 4 && XMLStreamConstants.START_ELEMENT == xmlSecEvent.getEventType()) {
            XMLSecStartElement xmlSecStartElement = xmlSecEvent.asStartElement();

            List<SecurityHeaderOrder> securityHeaderOrderList =
                    outputProcessorChain.getSecurityContext().getAsList(SecurityHeaderOrder.class);
            SecurityHeaderOrder securityHeaderOrder = securityHeaderOrderList.get(securityHeaderIndex);
            if (!xmlSecStartElement.getName().equals(WSSConstants.TAG_xenc_EncryptedData)
                    && !xmlSecStartElement.getName().equals(securityHeaderOrder.getSecurityHeaderElementName())) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE, "empty",
                        new Object[]{"Invalid security header order. Expected "
                                + securityHeaderOrder.getSecurityHeaderElementName()
                                + " but got " + xmlSecStartElement.getName()});
            }

            Map<SecurityHeaderOrder, Deque<XMLSecEvent>> map = actionEventMap.get(securityHeaderOrder.getAction());
            currentDeque = new ArrayDeque<>();
            map.put(securityHeaderOrder, currentDeque);

            securityHeaderIndex++;
        }
        currentDeque.offer(xmlSecEvent);
    }
}
