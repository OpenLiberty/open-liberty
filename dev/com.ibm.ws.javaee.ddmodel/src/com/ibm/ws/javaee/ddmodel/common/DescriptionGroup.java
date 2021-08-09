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

import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:group name="descriptionGroup">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="display-name"
 type="javaee:display-nameType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="icon"
 type="javaee:iconType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 </xsd:group>
 */

public class DescriptionGroup extends DescribableType implements com.ibm.ws.javaee.dd.common.DescriptionGroup {

    @Override
    public List<DisplayName> getDisplayNames() {
        if (display_name != null) {
            return display_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Icon> getIcons() {
        if (icon != null) {
            return icon.getList();
        } else {
            return Collections.emptyList();
        }
    }

    DisplayNameType.ListType display_name;
    IconType.ListType icon;

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            addDisplayName(display_name);
            return true;
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            addIcon(icon);
            return true;
        }
        return false;
    }

    private void addDisplayName(DisplayNameType display_name) {
        if (this.display_name == null) {
            this.display_name = new DisplayNameType.ListType();
        }
        this.display_name.add(display_name);
    }

    protected void addIcon(IconType icon) {
        if (this.icon == null) {
            this.icon = new IconType.ListType();
        }
        this.icon.add(icon);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
    }
}
