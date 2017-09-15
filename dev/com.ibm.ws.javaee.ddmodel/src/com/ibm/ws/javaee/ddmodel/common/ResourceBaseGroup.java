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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:group name="resourceBaseGroup">
 <xsd:sequence>
 <xsd:element name="mapped-name"
 type="javaee:xsdStringType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="injection-target"
 type="javaee:injection-targetType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 </xsd:group>
 */

public class ResourceBaseGroup extends JNDIEnvironmentRefType implements com.ibm.ws.javaee.dd.common.ResourceBaseGroup {

    @Override
    public String getMappedName() {
        return mapped_name != null ? mapped_name.getValue() : null;
    }

    @Override
    public List<InjectionTarget> getInjectionTargets() {
        if (injection_target != null) {
            return injection_target.getList();
        } else {
            return Collections.emptyList();
        }
    }

    XSDStringType mapped_name;
    InjectionTargetType.ListType injection_target;

    protected ResourceBaseGroup(String element_local_name) {
        super(element_local_name);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("mapped-name".equals(localName)) {
            XSDStringType mapped_name = new XSDStringType();
            parser.parse(mapped_name);
            this.mapped_name = mapped_name;
            return true;
        }
        if ("injection-target".equals(localName)) {
            InjectionTargetType injection_target = new InjectionTargetType();
            parser.parse(injection_target);
            addInjectionTarget(injection_target);
            return true;
        }
        return false;
    }

    private void addInjectionTarget(InjectionTargetType injection_target) {
        if (this.injection_target == null) {
            this.injection_target = new InjectionTargetType.ListType();
        }
        this.injection_target.add(injection_target);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("mapped-name", mapped_name);
        diag.describeIfSet("injection-target", injection_target);
    }
}
