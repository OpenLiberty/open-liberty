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

package org.apache.cxf.binding.soap;

import javax.xml.soap.SOAPMessage; // Liberty Change

import org.apache.cxf.binding.Binding;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;

import com.ibm.websphere.ras.annotation.Sensitive; // Liberty Change

// Liberty Changes - should try to commit back to CXF 
public class SoapBinding extends AbstractBasicInterceptorProvider implements Binding {

    private SoapVersion version;
    private BindingInfo bindingInfo;

    public SoapBinding(BindingInfo info) {
        this(info, Soap11.getInstance());
    }

    public SoapBinding(BindingInfo info, SoapVersion v) {
        version = v;
        bindingInfo = info;
    }

    public BindingInfo getBindingInfo() {
        return bindingInfo;
    }

    public void setSoapVersion(SoapVersion v) {
        this.version = v;
    }

    public SoapVersion getSoapVersion() {
        return version;
    }

    @Sensitive // Liberty Change
    public Message createMessage() {
        SoapMessage soapMessage = new SoapMessage(version);
        soapMessage.put(Message.CONTENT_TYPE, soapMessage.getVersion().getContentType());
        return soapMessage;
    }

    public Message createMessage(@Sensitive Message m) { // Liberty Change
       
        SoapMessage soapMessage = new SoapMessage(m);
        if (m.getExchange() != null) {
            if (m.getExchange().getInMessage() instanceof SoapMessage) {
                soapMessage.setVersion(((SoapMessage)m.getExchange().getInMessage()).getVersion());                      
            } else {
                soapMessage.setVersion(version);
            }
        } else {
            soapMessage.setVersion(version); 
        }

	    // Liberty Change Start: If Message doesn't contain content-type add the SOAPMessage content-type
        if (!m.containsKey(Message.CONTENT_TYPE)) {
            m.put(Message.CONTENT_TYPE, soapMessage.getVersion().getContentType());
            SoapMessage newMessage = new SoapMessage(m);
            newMessage.setVersion(soapMessage.getVersion());
        }
		// Liberty Change End
        
        if (!soapMessage.containsKey(Message.CONTENT_TYPE)) {
            soapMessage.put(Message.CONTENT_TYPE, soapMessage.getVersion().getContentType());
        }

        return soapMessage;
    }

}
