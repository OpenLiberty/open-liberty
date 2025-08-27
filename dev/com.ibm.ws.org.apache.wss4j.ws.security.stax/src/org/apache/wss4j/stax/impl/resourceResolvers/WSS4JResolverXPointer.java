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
package org.apache.wss4j.stax.impl.resourceResolvers;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;

import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.xml.security.stax.ext.ResourceResolver;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.resourceResolvers.ResolverXPointer;

public class WSS4JResolverXPointer extends ResolverXPointer {

    public WSS4JResolverXPointer() {
        super();
    }

    public WSS4JResolverXPointer(String uri) {
        super(uri);
    }

    @Override
    public ResourceResolver newInstance(String uri, String baseURI) {
        return new WSS4JResolverXPointer(uri);
    }

    @Override
    public boolean matches(XMLSecStartElement xmlSecStartElement) {
        return matches(xmlSecStartElement, XMLSecurityConstants.ATT_NULL_Id);
    }

    @Override
    public boolean matches(XMLSecStartElement xmlSecStartElement, QName idAttributeNS) {
        //when id is null we have #xpointer(/) and then we just return true for the first start-element
        if (getId() == null) {
            if (!isRootNodeOccured()) {
                setRootNodeOccured(true);
                return true;
            }
            return false;
        }
        //case #xpointer(id('ID')):
        Attribute attribute = xmlSecStartElement.getAttributeByName(WSSConstants.ATT_WSU_ID);
        if (attribute != null && attribute.getValue().equals(getId())) {
            return true;
        }
        return super.matches(xmlSecStartElement, idAttributeNS);
    }
}
