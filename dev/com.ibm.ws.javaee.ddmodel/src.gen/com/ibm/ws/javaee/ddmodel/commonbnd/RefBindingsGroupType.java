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
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class RefBindingsGroupType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup {
    public RefBindingsGroupType() {
        this(false);
    }

    public RefBindingsGroupType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.EJBRefType, com.ibm.ws.javaee.dd.commonbnd.EJBRef> ejb_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.ResourceRefType, com.ibm.ws.javaee.dd.commonbnd.ResourceRef> resource_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.ResourceEnvRefType, com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> resource_env_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationRefType, com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> message_destination_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.DataSourceType, com.ibm.ws.javaee.dd.commonbnd.DataSource> data_source;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.EnvEntryType, com.ibm.ws.javaee.dd.commonbnd.EnvEntry> env_entry;

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> getEJBRefs() {
        if (ejb_ref != null) {
            return ejb_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> getResourceRefs() {
        if (resource_ref != null) {
            return resource_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> getResourceEnvRefs() {
        if (resource_env_ref != null) {
            return resource_env_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> getMessageDestinationRefs() {
        if (message_destination_ref != null) {
            return message_destination_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.DataSource> getDataSources() {
        if (data_source != null) {
            return data_source.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> getEnvEntries() {
        if (env_entry != null) {
            return env_entry.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isIdAllowed() {
        return xmi;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ((xmi ? "ejbRefBindings" : "ejb-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.EJBRefType ejb_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.EJBRefType(xmi);
            parser.parse(ejb_ref);
            this.addEjbRef(ejb_ref);
            return true;
        }
        if ((xmi ? "resRefBindings" : "resource-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.ResourceRefType resource_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.ResourceRefType(xmi);
            parser.parse(resource_ref);
            this.addResourceRef(resource_ref);
            return true;
        }
        if ((xmi ? "resourceEnvRefBindings" : "resource-env-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.ResourceEnvRefType resource_env_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.ResourceEnvRefType(xmi);
            parser.parse(resource_env_ref);
            this.addResourceEnvRef(resource_env_ref);
            return true;
        }
        if ((xmi ? "messageDestinationRefBindings" : "message-destination-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationRefType message_destination_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationRefType(xmi);
            parser.parse(message_destination_ref);
            this.addMessageDestinationRef(message_destination_ref);
            return true;
        }
        if (!xmi && "data-source".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.DataSourceType data_source = new com.ibm.ws.javaee.ddmodel.commonbnd.DataSourceType();
            parser.parse(data_source);
            this.addDataSource(data_source);
            return true;
        }
        if (!xmi && "env-entry".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.EnvEntryType env_entry = new com.ibm.ws.javaee.ddmodel.commonbnd.EnvEntryType();
            parser.parse(env_entry);
            this.addEnvEntry(env_entry);
            return true;
        }
        return false;
    }

    void addEjbRef(com.ibm.ws.javaee.ddmodel.commonbnd.EJBRefType ejb_ref) {
        if (this.ejb_ref == null) {
            this.ejb_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.EJBRefType, com.ibm.ws.javaee.dd.commonbnd.EJBRef>();
        }
        this.ejb_ref.add(ejb_ref);
    }

    void addResourceRef(com.ibm.ws.javaee.ddmodel.commonbnd.ResourceRefType resource_ref) {
        if (this.resource_ref == null) {
            this.resource_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.ResourceRefType, com.ibm.ws.javaee.dd.commonbnd.ResourceRef>();
        }
        this.resource_ref.add(resource_ref);
    }

    void addResourceEnvRef(com.ibm.ws.javaee.ddmodel.commonbnd.ResourceEnvRefType resource_env_ref) {
        if (this.resource_env_ref == null) {
            this.resource_env_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.ResourceEnvRefType, com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>();
        }
        this.resource_env_ref.add(resource_env_ref);
    }

    void addMessageDestinationRef(com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationRefType message_destination_ref) {
        if (this.message_destination_ref == null) {
            this.message_destination_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationRefType, com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>();
        }
        this.message_destination_ref.add(message_destination_ref);
    }

    void addDataSource(com.ibm.ws.javaee.ddmodel.commonbnd.DataSourceType data_source) {
        if (this.data_source == null) {
            this.data_source = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.DataSourceType, com.ibm.ws.javaee.dd.commonbnd.DataSource>();
        }
        this.data_source.add(data_source);
    }

    void addEnvEntry(com.ibm.ws.javaee.ddmodel.commonbnd.EnvEntryType env_entry) {
        if (this.env_entry == null) {
            this.env_entry = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.EnvEntryType, com.ibm.ws.javaee.dd.commonbnd.EnvEntry>();
        }
        this.env_entry.add(env_entry);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "ejbRefBindings" : "ejb-ref", ejb_ref);
        diag.describeIfSet(xmi ? "resRefBindings" : "resource-ref", resource_ref);
        diag.describeIfSet(xmi ? "resourceEnvRefBindings" : "resource-env-ref", resource_env_ref);
        diag.describeIfSet(xmi ? "messageDestinationRefBindings" : "message-destination-ref", message_destination_ref);
        diag.describeIfSet("data-source", data_source);
        diag.describeIfSet("env-entry", env_entry);
    }
}
