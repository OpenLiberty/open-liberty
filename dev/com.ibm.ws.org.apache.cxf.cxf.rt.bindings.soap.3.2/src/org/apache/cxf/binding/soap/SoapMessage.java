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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

public class SoapMessage extends MessageImpl {
    private static final long serialVersionUID = 6310906412722265578L;
    private SoapVersion version;

    public SoapMessage(Message message) {
        super(message);
        setVersion(Soap11.getInstance());
    }
    public SoapMessage(SoapVersion ver) {
        super();
        setVersion(ver);
    }

    public SoapVersion getVersion() {
        return version;
    }

    public final void setVersion(SoapVersion v) {
        super.put(SoapVersion.class, v);
        this.version = v;
    }

    public List<Header> getHeaders() {
        List<Header> heads = CastUtils.cast((List<?>)get(Header.HEADER_LIST));
        if (heads == null) {
            heads = new ArrayList<>();
            put(Header.HEADER_LIST, heads);
        }
        return heads;
    }

    public boolean hasHeader(QName qn) {
        for (Header head : getHeaders()) {
            if (head.getName().equals(qn)) {
                return true;
            }
        }
        return false;
    }
    public Header getHeader(QName qn) {
        for (Header head : getHeaders()) {
            if (head.getName().equals(qn)) {
                return head;
            }
        }
        return null;
    }

    public boolean hasHeaders() {
        return containsKey(Header.HEADER_LIST) && !getHeaders().isEmpty();
    }

    public Map<String, String> getEnvelopeNs() {
        return CastUtils.cast((Map<?, ?>)getContextualProperty("soap.env.ns.map"));
    }

    public boolean hasAdditionalEnvNs() {
        Map<String, String> ns = getEnvelopeNs();
        return ns != null && !ns.isEmpty();
    }

}
