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
package com.ibm.ws.javaee.ddmodel.clientbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ApplicationClientBndType extends com.ibm.ws.javaee.ddmodel.clientbnd.ClientRefBindingsGroupType implements com.ibm.ws.javaee.dd.clientbnd.ApplicationClientBnd, DDParser.RootParsable {
    public ApplicationClientBndType(String ddPath) {
        this(ddPath, false);
    }

    public ApplicationClientBndType(String ddPath, boolean xmi) {
        super(xmi);
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType, com.ibm.ws.javaee.dd.commonbnd.MessageDestination> message_destination;
    com.ibm.ws.javaee.ddmodel.StringType appName;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return deploymentDescriptorPath;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        super.finish(parser);
        this.idMap = parser.idMap;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "version".equals(localName)) {
                this.version = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "appName".equals(localName)) {
                this.appName = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("version".equals(localName)) {
                // Allowed but ignored.
                return true;
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "applicationClient".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("applicationClient", com.ibm.ws.javaee.dd.client.ApplicationClient.class);
            parser.parse(xmiRef);
            return true;
        }
        if (!xmi && "message-destination".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType message_destination = new com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType();
            parser.parse(message_destination);
            this.addMessageDestination(message_destination);
            return true;
        }
        return super.handleChild(parser, localName);
    }

    void addMessageDestination(com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType message_destination) {
        if (this.message_destination == null) {
            this.message_destination = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType, com.ibm.ws.javaee.dd.commonbnd.MessageDestination>();
        }
        this.message_destination.add(message_destination);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("applicationClient", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeIfSet("appName", appName);
        diag.describeIfSet("message-destination", message_destination);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
