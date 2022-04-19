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

package org.apache.cxf.binding.soap.jms.interceptor;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;

/**
 *
 */
public class SoapFaultFactory  {

    private SoapVersion version;

    public SoapFaultFactory(Binding binding) {
        version = ((SoapBinding)binding).getSoapVersion();
    }

    public Fault createFault(JMSFault jmsFault) {
        Fault f = null;
        if (version == Soap11.getInstance()) {
            f = createSoap11Fault(jmsFault);
            // so we can encode the SequenceFault as header
            f.initCause(jmsFault);
        } else {
            f = createSoap12Fault(jmsFault);
        }
        return f;
    }

    Fault createSoap11Fault(JMSFault jmsFault) {
        return new SoapFault(jmsFault.getReason(),
                             jmsFault.getSubCode());
    }

    Fault createSoap12Fault(JMSFault jmsFault) {
        SoapFault fault = new SoapFault(jmsFault.getReason(),
            jmsFault.isSender() ? version.getSender() : version.getReceiver());
        QName subCode = jmsFault.getSubCode();
        fault.setSubCode(subCode);
        Object detail = jmsFault.getDetail();
        if (null == detail) {
            return fault;
        }
        setDetail(fault, detail);
        return fault;
    }

    void setDetail(SoapFault fault, Object detail) {
        Element el = null;
        if (detail instanceof Element) {
            el = (Element)detail;
        } else if (detail instanceof Document) {
            el = ((Document)detail).getDocumentElement();
        }
        if (el != null) {
            fault.setDetail(el);
        }
    }

    public String toString(Fault f) {
        return f.toString();
    }


}
