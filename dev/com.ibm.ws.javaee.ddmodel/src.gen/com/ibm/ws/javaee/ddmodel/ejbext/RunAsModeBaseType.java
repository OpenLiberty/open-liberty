/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.ejbext;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class RunAsModeBaseType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbext.RunAsModeBase {
    private static final TraceComponent tc = Tr.register(RunAsModeBaseType.class);

    public RunAsModeBaseType() {
        this(false);
    }

    public RunAsModeBaseType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.ejbext.SpecifiedIdentityType specified_identity;
    com.ibm.ws.javaee.dd.ejbext.RunAsModeBase.ModeTypeEnum mode;
    com.ibm.ws.javaee.ddmodel.StringType description;

    @Override
    public com.ibm.ws.javaee.dd.ejbext.SpecifiedIdentity getSpecifiedIdentity() {
        return specified_identity;
    }

    @Override
    public com.ibm.ws.javaee.dd.ejbext.RunAsModeBase.ModeTypeEnum getModeType() {
        return mode;
    }

    @Override
    public java.lang.String getDescription() {
        return description != null ? description.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        if (mode == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("mode"));
        }

        if (specified_identity == null && mode == ModeTypeEnum.SPECIFIED_IDENTITY) {
            throw new DDParser.ParseException(Tr.formatMessage(tc, "runasmode.missing.specifiedID.element", parser.getPath(), parser.getLineNumber()));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "mode".equals(localName)) {
                this.mode = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.ejbext.RunAsModeBase.ModeTypeEnum.class);
                return true;
            }
            // "description" is the same for XML and XMI.
            if ("description".equals(localName)) {
                this.description = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (!xmi && "specified-identity".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.SpecifiedIdentityType specified_identity = new com.ibm.ws.javaee.ddmodel.ejbext.SpecifiedIdentityType();
            parser.parse(specified_identity);
            this.specified_identity = specified_identity;
            return true;
        }
        if (xmi && "runAsMode".equals(localName)) {
            RunAsModeXMIType runAsMode = new RunAsModeXMIType(this);
            parser.parse(runAsMode);
            return true;
        }
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeEnumIfSet("mode", mode);
        diag.describeIfSet("description", description);
        diag.describeIfSet("specified-identity", specified_identity);
    }
}
