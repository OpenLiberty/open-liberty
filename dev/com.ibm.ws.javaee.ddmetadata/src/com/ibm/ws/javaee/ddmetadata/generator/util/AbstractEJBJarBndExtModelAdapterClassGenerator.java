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
package com.ibm.ws.javaee.ddmetadata.generator.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;

import com.ibm.ws.javaee.ddmetadata.generator.ModelAdapterClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.Model;

public abstract class AbstractEJBJarBndExtModelAdapterClassGenerator extends ModelAdapterClassGenerator {
    public AbstractEJBJarBndExtModelAdapterClassGenerator(File destdir, Model model) {
        super(destdir, model);
    }

    protected abstract String getType();

    private String getConstantFileType() {
        return getType().toUpperCase(Locale.ROOT);
    }

    @Override
    protected void writeStatics(PrintWriter out) {
        String type = getType();
        String constantFileType = getConstantFileType();
        out.append("    public static final String XMI_").append(constantFileType).append("_IN_EJB_MOD_NAME = \"META-INF/ibm-ejb-jar-").append(type).append(".xmi\";").println();
        out.append("    public static final String XML_").append(constantFileType).append("_IN_EJB_MOD_NAME = \"META-INF/ibm-ejb-jar-").append(type).append(".xml\";").println();
        out.append("    public static final String XMI_").append(constantFileType).append("_IN_WEB_MOD_NAME = \"WEB-INF/ibm-ejb-jar-").append(type).append(".xmi\";").println();
        out.append("    public static final String XML_").append(constantFileType).append("_IN_WEB_MOD_NAME = \"WEB-INF/ibm-ejb-jar-").append(type).append(".xml\";").println();
        out.println();
    }

    @Override
    protected void writeInitializeEntryName(PrintWriter out, String ddEntryNameVar, String xmiVar) {
        String constantFileType = getConstantFileType();
        out.append("        ").append(model.xmiPrimaryDDTypeName)
                        .append(" primary = containerToAdapt.adapt(").append(model.xmiPrimaryDDTypeName)
                        .append(".class);").println();
        out.append("        boolean xmi = primary != null && primary.getVersionID() < ").append(model.xmiPrimaryDDTypeName).append(".VERSION_3_0;").println();
        out.append("        String ").append(ddEntryNameVar).append(';').println();
        out.append("        if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), com.ibm.ws.container.service.app.deploy.WebModuleInfo.class) == null) {").println();
        out.append("            ").append(ddEntryNameVar)
                        .append(" = xmi ? XMI_").append(constantFileType).append("_IN_EJB_MOD_NAME : XML_").append(constantFileType).append("_IN_EJB_MOD_NAME;").println();
        out.append("        } else {").println();
        out.append("            ").append(ddEntryNameVar)
                        .append(" = xmi ? XMI_").append(constantFileType).append("_IN_WEB_MOD_NAME : XML_").append(constantFileType).append("_IN_WEB_MOD_NAME;").println();
        out.append("        }").println();
    }
}
