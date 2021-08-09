/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import com.ibm.ws.javaee.dd.ws.ServiceImplBean;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="service-impl-beanType">
 <xsd:choice>
 <xsd:element name="ejb-link"
 type="javaee:ejb-linkType"/>
 <xsd:element name="servlet-link"
 type="javaee:string"/>
 </xsd:choice>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class ServiceImplBeanType extends ElementContentParsable implements
                ServiceImplBean {

    @Override
    public String getEJBLink() {
        return ejb_link != null ? ejb_link.getValue() : null;
    }

    @Override
    public String getServletLink() {
        return servlet_link != null ? servlet_link.getValue() : null;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean handleChild(DDParser parser, String localName)
                    throws ParseException {

        if ("ejb-link".equals(localName)) {
            XSDTokenType ejb_link = new XSDTokenType();
            parser.parse(ejb_link);
            this.ejb_link = ejb_link;
            return true;
        }
        if ("servlet-link".equals(localName)) {
            XSDTokenType servlet_link = new XSDTokenType();
            parser.parse(servlet_link);
            this.servlet_link = servlet_link;
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void describe(Diagnostics diag) {
        diag.describeIfSet("ejb-link", ejb_link);
        diag.describeIfSet("servlet-link", servlet_link);
    }

    // elements
    XSDTokenType ejb_link;
    XSDTokenType servlet_link;
}
