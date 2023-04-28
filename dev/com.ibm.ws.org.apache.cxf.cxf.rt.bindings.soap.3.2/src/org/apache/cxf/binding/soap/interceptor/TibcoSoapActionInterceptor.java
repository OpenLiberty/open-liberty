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
package org.apache.cxf.binding.soap.interceptor;

import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import com.ibm.websphere.ras.annotation.Sensitive; // Liberty Change
/**
 * Tibco Business Works uses SoapAction instead of the standard spelling SOAPAction.
 * So this interceptor adds a SoapAction header if SOAPAction is set in protocol header
 */
 // Liberty Change; This class has no Liberty specific changes other than the Sensitive annotation 
// It is required as an overlay because of Liberty specific changes to MessageImpl.put(). Any call
// to SoapMessage.put() will cause a NoSuchMethodException in the calling class if the class is not recompiled.
// If a solution to this compilation issue can be found, this class should be removed as an overlay. 
public class TibcoSoapActionInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private static final String SOAPACTION_TIBCO = "SoapAction";

    public TibcoSoapActionInterceptor() {
        super(Phase.PREPARE_SEND);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(@Sensitive SoapMessage soapMessage) throws Fault { // Liberty Change
        Map<String, Object> headers = (Map<String, Object>)soapMessage.get(Message.PROTOCOL_HEADERS);
        if (headers != null && headers.containsKey(SoapBindingConstants.SOAP_ACTION)) {
            //need to flip to a case sensitive map.  The default
            //is a case insensitive map, but in this case, we need
            //to use a case sensitive map to make sure both versions go out
            headers = new TreeMap<>(headers);
            soapMessage.put(Message.PROTOCOL_HEADERS, headers);
            headers.put(SOAPACTION_TIBCO, headers.get(SoapBindingConstants.SOAP_ACTION));
        }
    }

}
