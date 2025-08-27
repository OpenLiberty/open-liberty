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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.SecurityHeaderOrder;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.impl.EncryptionPartDef;
import org.apache.xml.security.stax.impl.processor.output.AbstractEncryptEndingOutputProcessor;

/**
 * Processor buffers encrypted XMLEvents and forwards them when final is called
 */
public class EncryptEndingOutputProcessor extends AbstractEncryptEndingOutputProcessor {

    public EncryptEndingOutputProcessor() throws XMLSecurityException {
        super();
        this.addAfterProcessor(EncryptOutputProcessor.class);
        this.addAfterProcessor(UsernameTokenOutputProcessor.class);
    }

    @Override
    public void processHeaderEvent(OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
        OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);
        if (attachmentCount(outputProcessorChain) > 0) {
            WSSUtils.createEncryptedDataStructureForAttachments(this, subOutputProcessorChain);
        }
    }

    @Override
    public void flushBufferAndCallbackAfterHeader(OutputProcessorChain outputProcessorChain,
                                                   Deque<XMLSecEvent> xmlSecEventDeque)
            throws XMLStreamException, XMLSecurityException {

        final String actor = ((WSSSecurityProperties) getSecurityProperties()).getActor();

        //loop until we reach our security header
        loop:
        while (!xmlSecEventDeque.isEmpty()) {
            XMLSecEvent xmlSecEvent = xmlSecEventDeque.pop();
            if (XMLStreamConstants.START_ELEMENT == xmlSecEvent.getEventType()
                    && WSSUtils.isSecurityHeaderElement(xmlSecEvent, actor)) {
                int attachmentCount = attachmentCount(outputProcessorChain);
                for (int i = 0; i < attachmentCount; i++) {
                    OutputProcessorUtils.updateSecurityHeaderOrder(
                            outputProcessorChain, WSSConstants.TAG_xenc_EncryptedData, getAction(), true);
                }
                List<SecurityHeaderOrder> securityHeaderOrderList =
                        outputProcessorChain.getSecurityContext().getAsList(SecurityHeaderOrder.class);
                List<SecurityHeaderOrder> tmpList = null;
                if (securityHeaderOrderList != null) {
                    tmpList = new ArrayList<>(securityHeaderOrderList);
                    securityHeaderOrderList.clear();
                }

                outputProcessorChain.reset();
                outputProcessorChain.processEvent(xmlSecEvent);

                if (securityHeaderOrderList != null) {
                    securityHeaderOrderList.addAll(tmpList);
                }
                break loop;
            }
            outputProcessorChain.reset();
            outputProcessorChain.processEvent(xmlSecEvent);
        }
        super.flushBufferAndCallbackAfterHeader(outputProcessorChain, xmlSecEventDeque);
    }

    private int attachmentCount(OutputProcessorChain outputProcessorChain) {
        List<EncryptionPartDef> encryptionPartDefs =
                outputProcessorChain.getSecurityContext().getAsList(EncryptionPartDef.class);
        if (encryptionPartDefs == null) {
            return 0;
        }

        int count = 0;
        Iterator<EncryptionPartDef> encryptionPartDefIterator = encryptionPartDefs.iterator();
        while (encryptionPartDefIterator.hasNext()) {
            EncryptionPartDef encryptionPartDef = encryptionPartDefIterator.next();

            if (encryptionPartDef.getCipherReferenceId() != null) {
                count++;
            }
        }
        return count;
    }
}
