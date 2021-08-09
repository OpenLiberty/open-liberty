/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.generator;

import java.io.File;
import java.io.PrintWriter;

import com.ibm.ws.javaee.ddmetadata.model.Model;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;

public class ModelParserClassGenerator extends ModelClassGenerator {
    final Model model;

    public ModelParserClassGenerator(File destdir, Model model) {
        super(destdir, model.parserImplClassName);
        this.model = model;
    }

    public void generate() {
        ModelInterfaceType rootType = model.getRootType();

        PrintWriter out = open();
        out.println("import com.ibm.ws.javaee.ddmodel.DDParser;");
        out.println("import com.ibm.wsspi.adaptable.module.Container;");
        out.println("import com.ibm.wsspi.adaptable.module.Entry;");
        out.println();
        out.append("public class ").append(simpleName).append(" extends DDParser {").println();
        if (model.xmiPrimaryDDTypeName != null) {
            out.append("    private final boolean xmi;").println();
            out.println();
            out.append("    public ").append(simpleName).append("(Container ddRootContainer, Entry ddEntry, boolean xmi) throws DDParser.ParseException {").println();
            out.append("        super(ddRootContainer, ddEntry, ").append(model.xmiPrimaryDDTypeName).append(".class);").println();
            out.append("        this.xmi = xmi;").println();
        } else {
            out.append("    public ").append(simpleName).append("(Container ddRootContainer, Entry ddEntry) throws DDParser.ParseException {").println();
            out.append("        super(ddRootContainer, ddEntry);").println();
        }
        out.append("    }").println();
        out.println();
        out.append("    public ").append(rootType.interfaceName).append(" parse() throws ParseException {").println();
        out.append("        super.parseRootElement();").println();
        out.append("        return (").append(rootType.interfaceName).append(") rootParsable;").println();
        out.append("    }").println();
        out.println();
        out.append("    @Override").println();
        out.append("    protected ParsableElement createRootParsable() throws ParseException {").println();
        out.append("        if (");
        if (model.xmiPrimaryDDTypeName != null) {
            out.append("!xmi && ");
        }
        out.append('"').append(model.root.name).append("\".equals(rootElementLocalName)) {").println();
        out.append("            return createXMLRootParsable();").println();
        out.append("        }").println();
        if (model.xmiPrimaryDDTypeName != null) {
            out.append("        if (xmi && \"").append(model.root.xmiName).append("\".equals(rootElementLocalName)) {").println();
            out.append("            DDParser.ParsableElement rootParsableElement = createXMIRootParsable();").println();
            out.append("            namespace = null;").println();
            out.append("            idNamespace = \"http://www.omg.org/XMI\";").println();
            out.append("            return rootParsableElement;").println();
            out.append("        }").println();
        }
        out.append("        throw new ParseException(invalidRootElement());").println();
        out.append("    }").println();
        out.println();
        out.append("    private ParsableElement createXMLRootParsable() throws ParseException {").println();
        out.append("        if (namespace == null) {").println();
        out.append("            throw new ParseException(missingDeploymentDescriptorNamespace());").println();
        out.append("        }").println();
        out.append("        String versionString = getAttributeValue(\"\", \"version\");").println();
        out.append("        if (versionString == null) {").println();
        out.append("            throw new ParseException(missingDeploymentDescriptorVersion());").println();
        out.append("        }").println();
        for (Model.Namespace ns : model.namespaces) {
            out.append("        if (\"").append(ns.namespace).append("\".equals(namespace)) {").println();
            for (Model.Namespace.Version v : ns.versions) {
                out.append("            if (\"").append(v.string).append("\".equals(versionString)) {").println();
                out.append("                version = ").append(Integer.toString(v.constant)).append(";").println();
                out.append("                return new ").append(rootType.implClassName).append("(getDeploymentDescriptorPath());").println();
                out.append("            }").println();
            }
            out.append("            throw new ParseException(invalidDeploymentDescriptorVersion(versionString));").println();
            out.append("        }").println();
        }
        out.append("        throw new ParseException(invalidDeploymentDescriptorNamespace(versionString));").println();
        out.append("    }").println();
        if (model.root.xmiName != null) {
            out.println();
            out.append("    private DDParser.ParsableElement createXMIRootParsable() throws ParseException {").println();
            out.append("        if (namespace == null) {").println();
            out.append("            throw new ParseException(missingDeploymentDescriptorNamespace());").println();
            out.append("        }").println();
            out.append("        if (\"").append(model.xmiNamespace).append("\".equals(namespace)) {").println();
            out.append("            version = ").append(Integer.toString(model.xmiVersion)).append(";").println();
            out.append("            return new ").append(rootType.implClassName).append("(getDeploymentDescriptorPath(), true);").println();
            out.append("        }").println();
            out.append("        throw new ParseException(missingDeploymentDescriptorVersion());").println();
            out.append("    }").println();
        }
        out.append("}").println();

        out.close();
    }
}
