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

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:group name="resourceGroup">
 <xsd:sequence>
 <xsd:group ref="javaee:resourceBaseGroup"/>
 <xsd:element name="lookup-name"
 type="javaee:xsdStringType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 </xsd:group>
 */

public class ResourceGroup extends ResourceBaseGroup implements com.ibm.ws.javaee.dd.common.ResourceGroup {

    @Override
    public String getLookupName() {
        return lookup_name != null ? lookup_name.getValue() : null;
    }

    // ResourceBaseGroup fields appear here in sequence
    XSDStringType lookup_name;

    protected ResourceGroup(String element_local_name) {
        super(element_local_name);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("lookup-name".equals(localName)) {
            XSDStringType lookup_name = new XSDStringType();
            parser.parse(lookup_name);
            this.lookup_name = lookup_name;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("lookup-name", lookup_name);
    }
}
