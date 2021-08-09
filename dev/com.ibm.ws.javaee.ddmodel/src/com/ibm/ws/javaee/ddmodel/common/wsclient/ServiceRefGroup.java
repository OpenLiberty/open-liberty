/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common.wsclient;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:group name="service-refGroup">
 <xsd:sequence>
 <xsd:element name="service-ref"
 type="javaee:service-refType"
 minOccurs="0"
 maxOccurs="unbounded">
 <xsd:key name="service-ref_handler-name-key">
 <xsd:selector xpath="javaee:handler"/>
 <xsd:field xpath="javaee:handler-name"/>
 </xsd:key>
 </xsd:element>
 </xsd:sequence>
 </xsd:group>
 */

public class ServiceRefGroup extends DDParser.ElementContentParsable {
    ServiceRefType.ListType service_ref;

    public List<ServiceRef> getServiceRefs() {
        if (service_ref != null) {
            return service_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("service-ref".equals(localName)) {
            ServiceRefType service_ref = new ServiceRefType();
            parser.parse(service_ref);
            addServiceRef(service_ref);
            return true;
        }
        return false;
    }

    private void addServiceRef(ServiceRefType service_ref) {
        if (this.service_ref == null) {
            this.service_ref = new ServiceRefType.ListType();
        }
        this.service_ref.add(service_ref);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("service-ref", service_ref);
    }
}
