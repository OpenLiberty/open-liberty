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
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.common.Listener;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="listenerType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="listener-class"
 type="javaee:fully-qualified-classType">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ListenerType extends DescriptionGroup implements Listener {

    public static class ListType extends ParsableListImplements<ListenerType, Listener> {
        @Override
        public ListenerType newInstance(DDParser parser) {
            return new ListenerType();
        }
    }

    @Override
    public String getListenerClassName() {
        return listener_class.getValue();
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    XSDTokenType listener_class = new XSDTokenType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("listener-class".equals(localName)) {
            parser.parse(listener_class);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("listener-class", listener_class);
    }
}
