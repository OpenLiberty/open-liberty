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
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.jaxrs.impl.PropertyHolderFactory.PropertyHolder;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

public class ServletRequestPropertyHolder implements PropertyHolder {
    private static final String ENDPOINT_ADDRESS_PROPERTY = "org.apache.cxf.transport.endpoint.address";
    private HttpServletRequest request;
    public ServletRequestPropertyHolder(Message m) {
        request = (HttpServletRequest)((MessageImpl) m).getHttpRequest();
    }

    @Override
    public Object getProperty(String name) {
        return request.getAttribute(name);
    }

    @Override
    public void removeProperty(String name) {
        request.removeAttribute(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (value == null) {
            removeProperty(name);
        } else {
            request.setAttribute(name, value);
        }
    }

    @Override
    public Collection<String> getPropertyNames() {
        List<String> list = new LinkedList<>();
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = attrNames.nextElement();
            if (!ENDPOINT_ADDRESS_PROPERTY.equals(name)) {
                list.add(name);
            }
        }
        return list;
    }

}
