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
package com.ibm.ws.javaee.ddmodel.commonext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class LocalTransactionType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonext.LocalTransaction {
    public LocalTransactionType() {
        this(false);
    }

    public LocalTransactionType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum boundary;
    com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum resolver;
    com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum unresolved_action;
    com.ibm.ws.javaee.ddmodel.BooleanType shareable;

    @Override
    public boolean isSetBoundary() {
        return boundary != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum getBoundary() {
        return boundary;
    }

    @Override
    public boolean isSetResolver() {
        return resolver != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum getResolver() {
        return resolver;
    }

    @Override
    public boolean isSetUnresolvedAction() {
        return unresolved_action != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum getUnresolvedAction() {
        return unresolved_action;
    }

    @Override
    public boolean isSetShareable() {
        return shareable != null;
    }

    @Override
    public boolean isShareable() {
        return shareable != null ? shareable.getBooleanValue() : false;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            // "boundary" is the same for XML and XMI.
            if ("boundary".equals(localName)) {
                this.boundary = xmi ? parseXMIBoundaryEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum.class);
                return true;
            }
            // "resolver" is the same for XML and XMI.
            if ("resolver".equals(localName)) {
                this.resolver = xmi ? parseXMIResolverEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum.class);
                return true;
            }
            if ((xmi ? "unresolvedAction" : "unresolved-action").equals(localName)) {
                this.unresolved_action = xmi ? parseXMIUnresolvedActionEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum.class);
                return true;
            }
            // "shareable" is the same for XML and XMI.
            if ("shareable".equals(localName)) {
                this.shareable = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return false;
    }

    private static com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum parseXMIBoundaryEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("ActivitySession".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum.ACTIVITY_SESSION;
        }
        if ("BeanMethod".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum.BEAN_METHOD;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "ActivitySession", "BeanMethod"));
    }

    private static com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum parseXMIResolverEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("Application".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum.APPLICATION;
        }
        if ("ContainerAtBoundary".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "Application", "ContainerAtBoundary"));
    }

    private static com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum parseXMIUnresolvedActionEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("Rollback".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum.ROLLBACK;
        }
        if ("Commit".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum.COMMIT;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "Rollback", "Commit"));
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeEnumIfSet("boundary", boundary);
        diag.describeEnumIfSet("resolver", resolver);
        diag.describeEnumIfSet(xmi ? "unresolvedAction" : "unresolved-action", unresolved_action);
        diag.describeIfSet("shareable", shareable);
    }
}
