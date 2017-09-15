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

import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="injection-targetType">
 <xsd:sequence>
 <xsd:element name="injection-target-class"
 type="javaee:fully-qualified-classType"/>
 <xsd:element name="injection-target-name"
 type="javaee:java-identifierType"/>
 </xsd:sequence>
 </xsd:complexType>
 */

public class InjectionTargetType extends DDParser.ElementContentParsable implements InjectionTarget {

    public static class ListType extends ParsableListImplements<InjectionTargetType, InjectionTarget> {
        @Override
        public InjectionTargetType newInstance(DDParser parser) {
            return new InjectionTargetType();
        }
    }

    @Override
    public String getInjectionTargetClassName() {
        return injection_target_class.getValue();
    }

    @Override
    public String getInjectionTargetName() {
        return injection_target_name.getValue();
    }

    XSDTokenType injection_target_class = new XSDTokenType();
    XSDTokenType injection_target_name = new XSDTokenType();

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("injection-target-class".equals(localName)) {
            parser.parse(injection_target_class);
            return true;
        }
        if ("injection-target-name".equals(localName)) {
            parser.parse(injection_target_name);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("injection-target-class", injection_target_class);
        diag.describe("injection-target-name", injection_target_name);
    }
}
