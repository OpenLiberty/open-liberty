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

import org.apache.wss4j.binding.wss11.SignatureConfirmationType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.AbstractInputProcessor;
import org.apache.xml.security.stax.ext.InputProcessorChain;
import org.apache.xml.security.stax.ext.stax.XMLSecEndElement;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SignatureValueSecurityEvent;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Arrays;
import java.util.List;

public class SignatureConfirmationInputProcessor extends AbstractInputProcessor {

    public SignatureConfirmationInputProcessor(WSSSecurityProperties securityProperties) {
        super(securityProperties);
    }

    @Override
    public XMLSecEvent processHeaderEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {

        XMLSecEvent xmlSecEvent = inputProcessorChain.processHeaderEvent();
        if (xmlSecEvent.getEventType() == XMLStreamConstants.END_ELEMENT) {
            XMLSecEndElement xmlSecEndElement = xmlSecEvent.asEndElement();
            if (xmlSecEndElement.getName().equals(WSSConstants.TAG_WSSE_SECURITY)) {
                inputProcessorChain.removeProcessor(this);

                List<SignatureValueSecurityEvent> signatureValueSecurityEventList =
                        inputProcessorChain.getSecurityContext().getAsList(SecurityEvent.class);
                List<SignatureConfirmationType> signatureConfirmationTypeList =
                        inputProcessorChain.getSecurityContext().getAsList(SignatureConfirmationType.class);

                //when no signature was sent, we expect an empty SignatureConfirmation in the response
                if (signatureValueSecurityEventList == null || signatureValueSecurityEventList.isEmpty()) {
                    if (signatureConfirmationTypeList == null || signatureConfirmationTypeList.size() != 1) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                    } else if (signatureConfirmationTypeList.get(0).getValue() != null) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                    }
                }

                if (signatureConfirmationTypeList == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                }

                for (int i = 0; i < signatureValueSecurityEventList.size(); i++) {
                    SignatureValueSecurityEvent signatureValueSecurityEvent = signatureValueSecurityEventList.get(i);
                    byte[] signatureValue = signatureValueSecurityEvent.getSignatureValue();

                    boolean found = false;

                    for (int j = 0; j < signatureConfirmationTypeList.size(); j++) {
                        SignatureConfirmationType signatureConfirmationType = signatureConfirmationTypeList.get(j);
                        byte[] sigConfValue = signatureConfirmationType.getValue();
                        if (Arrays.equals(signatureValue, sigConfValue)) {
                            found = true;
                        }
                    }

                    if (!found) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                    }
                }
            }
        }
        return xmlSecEvent;
    }

    @Override
    public XMLSecEvent processEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {
        //should never be called
        return null;
    }
}
