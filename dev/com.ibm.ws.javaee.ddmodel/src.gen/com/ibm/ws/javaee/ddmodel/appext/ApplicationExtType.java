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
package com.ibm.ws.javaee.ddmodel.appext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ApplicationExtType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.appext.ApplicationExt, DDParser.RootParsable {
    public ApplicationExtType(String ddPath) {
        this(ddPath, false);
    }

    public ApplicationExtType(String ddPath, boolean xmi) {
        this.xmi = xmi;
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    protected final boolean xmi;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    com.ibm.ws.javaee.dd.appext.ApplicationExt.ClientModeEnum client_mode;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appext.ModuleExtensionType, com.ibm.ws.javaee.dd.appext.ModuleExtension> module_extension;
    com.ibm.ws.javaee.ddmodel.LongType reload_interval_value;
    com.ibm.ws.javaee.ddmodel.BooleanType shared_session_context_value;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public boolean isSetClientMode() {
        return client_mode != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.appext.ApplicationExt.ClientModeEnum getClientMode() {
        return client_mode;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appext.ModuleExtension> getModuleExtensions() {
        if (module_extension != null) {
            return module_extension.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isSetReloadInterval() {
        return reload_interval_value != null;
    }

    @Override
    public long getReloadInterval() {
        return reload_interval_value != null ? reload_interval_value.getLongValue() : 0;
    }

    @Override
    public boolean isSetSharedSessionContext() {
        return shared_session_context_value != null;
    }

    @Override
    public boolean isSharedSessionContext() {
        return shared_session_context_value != null ? shared_session_context_value.getBooleanValue() : false;
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
            if ((xmi ? "clientMode" : "client-mode").equals(localName)) {
                this.client_mode = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.appext.ApplicationExt.ClientModeEnum.class);
                return true;
            }
            if (xmi && "reloadInterval".equals(localName)) {
                this.reload_interval_value = parser.parseLongAttributeValue(index);
                return true;
            }
            if (xmi && "sharedSessionContext".equals(localName)) {
                this.shared_session_context_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("version".equals(localName)) {
                // Allowed but ignored.
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "application".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("application", com.ibm.ws.javaee.dd.app.Application.class);
            parser.parse(xmiRef);
            return true;
        }
        if ((xmi ? "moduleExtensions" : "module-extension").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appext.ModuleExtensionType module_extension = new com.ibm.ws.javaee.ddmodel.appext.ModuleExtensionType(xmi);
            parser.parse(module_extension);
            this.addModuleExtension(module_extension);
            return true;
        }
        if (!xmi && "reload-interval".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.LongType reload_interval_value = new com.ibm.ws.javaee.ddmodel.LongType();
            reload_interval_value.obtainValueFromAttribute("value");
            parser.parse(reload_interval_value);
            this.reload_interval_value = reload_interval_value;
            return true;
        }
        if (xmi && "reloadInterval".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.LongType reload_interval_value = new com.ibm.ws.javaee.ddmodel.LongType();
            parser.parse(reload_interval_value);
            if (!reload_interval_value.isNil()) {
                this.reload_interval_value = reload_interval_value;
            }
            return true;
        }
        if (!xmi && "shared-session-context".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType shared_session_context_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            shared_session_context_value.obtainValueFromAttribute("value");
            parser.parse(shared_session_context_value);
            this.shared_session_context_value = shared_session_context_value;
            return true;
        }
        return false;
    }

    void addModuleExtension(com.ibm.ws.javaee.ddmodel.appext.ModuleExtensionType module_extension) {
        if (this.module_extension == null) {
            this.module_extension = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appext.ModuleExtensionType, com.ibm.ws.javaee.dd.appext.ModuleExtension>();
        }
        this.module_extension.add(module_extension);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("application", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeEnumIfSet(xmi ? "clientMode" : "client-mode", client_mode);
        diag.describeIfSet(xmi ? "moduleExtensions" : "module-extension", module_extension);
        diag.describeIfSet(xmi ? "reloadInterval" : "reload-interval[@value]", reload_interval_value);
        diag.describeIfSet(xmi ? "sharedSessionContext" : "shared-session-context[@value]", shared_session_context_value);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
