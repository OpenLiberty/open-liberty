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

public class ModuleExtensionType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.appext.ModuleExtension {
    public ModuleExtensionType() {
        this(false);
    }

    public ModuleExtensionType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType module;
    com.ibm.ws.javaee.ddmodel.StringType alt_bindings_uri;
    com.ibm.ws.javaee.ddmodel.StringType alt_extensions_uri;
    com.ibm.ws.javaee.ddmodel.StringType alt_root_uri;
    com.ibm.ws.javaee.ddmodel.StringType absolute_path_path;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType applicationExtension;
    com.ibm.ws.javaee.ddmodel.StringType dependentClasspath;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public boolean isSetAltBindings() {
        return alt_bindings_uri != null;
    }

    @Override
    public java.lang.String getAltBindings() {
        return alt_bindings_uri != null ? alt_bindings_uri.getValue() : null;
    }

    @Override
    public boolean isSetAltExtensions() {
        return alt_extensions_uri != null;
    }

    @Override
    public java.lang.String getAltExtensions() {
        return alt_extensions_uri != null ? alt_extensions_uri.getValue() : null;
    }

    @Override
    public boolean isSetAltRoot() {
        return alt_root_uri != null;
    }

    @Override
    public java.lang.String getAltRoot() {
        return alt_root_uri != null ? alt_root_uri.getValue() : null;
    }

    @Override
    public boolean isSetAbsolutePath() {
        return absolute_path_path != null;
    }

    @Override
    public java.lang.String getAbsolutePath() {
        return absolute_path_path != null ? absolute_path_path.getValue() : null;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "dependentClasspath".equals(localName)) {
                this.dependentClasspath = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "altBindings".equals(localName)) {
                this.alt_bindings_uri = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "altExtensions".equals(localName)) {
                this.alt_extensions_uri = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "altRoot".equals(localName)) {
                this.alt_root_uri = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "absolutePath".equals(localName)) {
                this.absolute_path_path = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("type".equals(localName)) {
                String type = parser.getAttributeValue(index);
                if ((type.endsWith(":ConnectorModuleExtension") && "applicationext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":ConnectorModuleExtension".length())))) || (type.endsWith(":EjbModuleExtension") && "applicationext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":EjbModuleExtension".length())))) || (type.endsWith(":JavaClientModuleExtension") && "applicationext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":JavaClientModuleExtension".length())))) || (type.endsWith(":WebModuleExtension") && "applicationext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":WebModuleExtension".length()))))) {
                    // Allowed but ignored.
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "module".equals(localName)) {
            this.module = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("module", parser.crossComponentDocumentType);
            parser.parse(module);
            com.ibm.ws.javaee.dd.app.Module referent = this.module.resolveReferent(parser, com.ibm.ws.javaee.dd.app.Module.class);
            if (referent == null) {
                DDParser.unresolvedReference("module", this.module.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getModulePath());
            }
            return true;
        }
        if (xmi && "applicationExtension".equals(localName)) {
            this.applicationExtension = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("applicationExtension", parser.crossComponentDocumentType);
            parser.parse(applicationExtension);
            // The referent is unused.
            return true;
        }
        if (!xmi && "alt-bindings".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType alt_bindings_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            alt_bindings_uri.obtainValueFromAttribute("uri");
            parser.parse(alt_bindings_uri);
            this.alt_bindings_uri = alt_bindings_uri;
            return true;
        }
        if (!xmi && "alt-extensions".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType alt_extensions_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            alt_extensions_uri.obtainValueFromAttribute("uri");
            parser.parse(alt_extensions_uri);
            this.alt_extensions_uri = alt_extensions_uri;
            return true;
        }
        if (!xmi && "alt-root".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType alt_root_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            alt_root_uri.obtainValueFromAttribute("uri");
            parser.parse(alt_root_uri);
            this.alt_root_uri = alt_root_uri;
            return true;
        }
        if (!xmi && "absolute-path".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType absolute_path_path = new com.ibm.ws.javaee.ddmodel.StringType();
            absolute_path_path.obtainValueFromAttribute("path");
            parser.parse(absolute_path_path);
            this.absolute_path_path = absolute_path_path;
            return true;
        }
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("module", module);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet("applicationExtension", applicationExtension);
        diag.describeIfSet("dependentClasspath", dependentClasspath);
        diag.describeIfSet(xmi ? "altBindings" : "alt-bindings[@uri]", alt_bindings_uri);
        diag.describeIfSet(xmi ? "altExtensions" : "alt-extensions[@uri]", alt_extensions_uri);
        diag.describeIfSet(xmi ? "altRoot" : "alt-root[@uri]", alt_root_uri);
        diag.describeIfSet(xmi ? "absolutePath" : "absolute-path[@path]", absolute_path_path);
    }
}
