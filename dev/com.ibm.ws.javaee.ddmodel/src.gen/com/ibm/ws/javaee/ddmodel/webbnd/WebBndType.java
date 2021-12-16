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
package com.ibm.ws.javaee.ddmodel.webbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class WebBndType extends com.ibm.ws.javaee.ddmodel.commonbnd.RefBindingsGroupType implements com.ibm.ws.javaee.dd.webbnd.WebBnd, DDParser.RootParsable {
    public static class ServiceRefBindingXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public ServiceRefBindingXMIIgnoredType() {
            this(false);
        }

        public ServiceRefBindingXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;
        com.ibm.ws.javaee.ddmodel.StringType jndiName;
        private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType bindingServiceRef;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if (xmi && "jndiName".equals(localName)) {
                    this.jndiName = parser.parseStringAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
            diag.describeIfSet("jndiName", jndiName);
            diag.describeIfSet("bindingServiceRef", bindingServiceRef);
        }
    }

    public static class MessageDestinationXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public MessageDestinationXMIIgnoredType() {
            this(false);
        }

        public MessageDestinationXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;
        com.ibm.ws.javaee.ddmodel.StringType name;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if (xmi && "name".equals(localName)) {
                    this.name = parser.parseStringAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
        }
    }

    public WebBndType(String ddPath) {
        this(ddPath, false);
    }

    public WebBndType(String ddPath, boolean xmi) {
        super(xmi);
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    com.ibm.ws.javaee.ddmodel.webbnd.VirtualHostType virtual_host;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType, com.ibm.ws.javaee.dd.commonbnd.MessageDestination> message_destination;
    com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType jaspi_ref;
    DDParser.ParsableListImplements<ServiceRefBindingXMIIgnoredType, ServiceRefBindingXMIIgnoredType> serviceRefBindings;
    DDParser.ParsableListImplements<MessageDestinationXMIIgnoredType, MessageDestinationXMIIgnoredType> messageDestinations;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.webbnd.VirtualHost getVirtualHost() {
        return virtual_host;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public com.ibm.ws.javaee.dd.commonbnd.JASPIRef getJASPIRef() {
        return jaspi_ref;
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
            if (xmi && "virtualHostName".equals(localName)) {
                if (this.virtual_host == null) {
                    this.virtual_host = new com.ibm.ws.javaee.ddmodel.webbnd.VirtualHostType(true);
                }
                this.virtual_host.name = parser.parseStringAttributeValue(index);
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
        if (xmi && "webapp".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("webapp", com.ibm.ws.javaee.dd.web.WebApp.class);
            parser.parse(xmiRef);
            return true;
        }
        if (!xmi && "virtual-host".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webbnd.VirtualHostType virtual_host = new com.ibm.ws.javaee.ddmodel.webbnd.VirtualHostType();
            parser.parse(virtual_host);
            this.virtual_host = virtual_host;
            return true;
        }
        if (xmi && "virtualHostName".equals(localName)) {
            if (this.virtual_host == null) {
                this.virtual_host = new com.ibm.ws.javaee.ddmodel.webbnd.VirtualHostType(true);
            }
            com.ibm.ws.javaee.ddmodel.StringType name = new com.ibm.ws.javaee.ddmodel.StringType();
            parser.parse(name);
            if (!name.isNil()) {
                this.virtual_host.name = name;
            }
            return true;
        }
        if (!xmi && "message-destination".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType message_destination = new com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType();
            parser.parse(message_destination);
            this.addMessageDestination(message_destination);
            return true;
        }
        if ((xmi ? "jaspiRefBinding" : "jaspi-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType jaspi_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType(xmi);
            parser.parse(jaspi_ref);
            this.jaspi_ref = jaspi_ref;
            return true;
        }
        if (xmi && "serviceRefBindings".equals(localName)) {
            ServiceRefBindingXMIIgnoredType serviceRefBindings = new ServiceRefBindingXMIIgnoredType(xmi);
            parser.parse(serviceRefBindings);
            this.addServiceRefBinding(serviceRefBindings);
            return true;
        }
        if (xmi && "messageDestinations".equals(localName)) {
            MessageDestinationXMIIgnoredType messageDestinations = new MessageDestinationXMIIgnoredType(xmi);
            parser.parse(messageDestinations);
            this.addMessageDestination(messageDestinations);
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

    void addServiceRefBinding(ServiceRefBindingXMIIgnoredType serviceRefBindings) {
        if (this.serviceRefBindings == null) {
            this.serviceRefBindings = new DDParser.ParsableListImplements<ServiceRefBindingXMIIgnoredType, ServiceRefBindingXMIIgnoredType>();
        }
        this.serviceRefBindings.add(serviceRefBindings);
    }

    void addMessageDestination(MessageDestinationXMIIgnoredType messageDestinations) {
        if (this.messageDestinations == null) {
            this.messageDestinations = new DDParser.ParsableListImplements<MessageDestinationXMIIgnoredType, MessageDestinationXMIIgnoredType>();
        }
        this.messageDestinations.add(messageDestinations);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("webapp", xmiRef);
        diag.describeIfSet("version", version);
        if (xmi) {
            if (virtual_host != null) {
                virtual_host.describe(diag);
            }
        } else {
            diag.describeIfSet("virtual-host", virtual_host);
        }
        diag.describeIfSet("message-destination", message_destination);
        diag.describeIfSet(xmi ? "jaspiRefBinding" : "jaspi-ref", jaspi_ref);
        diag.describeIfSet("serviceRefBindings", serviceRefBindings);
        diag.describeIfSet("messageDestinations", messageDestinations);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
