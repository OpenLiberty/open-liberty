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

public class ModelAdapterClassGenerator extends ModelClassGenerator {
    protected final Model model;

    public ModelAdapterClassGenerator(File destdir, Model model) {
        super(destdir, model.adapterImplClassName);
        this.model = model;
    }

    public void generate() {
        ModelInterfaceType rootType = model.getRootType();

        PrintWriter out = open();
        out.append("import java.util.List;").println();
        out.append("import java.util.Set;").println();
        out.append("import java.util.HashSet;").println();
        out.append("import com.ibm.ws.javaee.dd.app.Application;").println();
        out.append("import com.ibm.ws.javaee.dd.app.Module;").println();
        out.append("import org.osgi.service.component.annotations.*;").println();
        out.append("import com.ibm.websphere.ras.Tr;").println();
        out.append("import com.ibm.websphere.ras.TraceComponent;").println();
        out.append("import com.ibm.ws.container.service.app.deploy.ApplicationInfo;").println();
        out.append("import com.ibm.ws.container.service.app.deploy.ModuleInfo;").println();
        out.append("import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;").println();
        out.append("import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;").println();
        out.append("import com.ibm.ws.ffdc.annotation.FFDCIgnore;").println();
        out.append("import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;").println();
        out.append("import com.ibm.wsspi.adaptable.module.Container;").println();
        out.append("import com.ibm.wsspi.adaptable.module.Entry;").println();
        out.append("import com.ibm.wsspi.adaptable.module.UnableToAdaptException;").println();
        out.append("import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;").println();
        out.append("import com.ibm.wsspi.artifact.ArtifactContainer;").println();
        out.append("import com.ibm.wsspi.artifact.overlay.OverlayContainer;").println();
        out.println();

        out.append("@Component(configurationPolicy = ConfigurationPolicy.IGNORE,").println();
        out.append("    service = ContainerAdapter.class,").println();
        out.append("    property = { \"service.vendor=IBM\", \"toType=").append(rootType.interfaceName).append("\" })").println();
        out.append("public class ").append(simpleName).append(" implements ContainerAdapter<").append(rootType.interfaceName).append("> {").println();
        writeStatics(out);

        out.println();
        out.append(INDENT).append("private static final String MODULE_NAME_INVALID = \"module.name.invalid\";").println();
        out.append(INDENT).append("private static final String MODULE_NAME_NOT_SPECIFIED = \"module.name.not.specified\";").println();
        out.append(INDENT).append("private static final TraceComponent tc = Tr.register(").append(simpleName).append(".class);").println();
        out.println();

        out.append("    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)").println();
        out.append("volatile List<").append(rootType.interfaceName).append("> configurations;").println();
        out.println();

        out.append("    @Override").println();
        out.append("    @FFDCIgnore(ParseException.class)").println();
        out.append("    public ").append(rootType.interfaceName).append(" adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {").println();
        writeInitializeEntryName(out, "ddEntryName", "xmi");
        out.println();
        out.append("        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);").println();

        String componentImplName = getComponentImplClassName(rootType.implClassName);
        out.append(componentImplName).append(" fromConfig = getConfigOverrides(rootOverlay, artifactContainer);").println();
        out.append("if (ddEntry == null && fromConfig == null)").println();
        out.append("    return null;").println();

        out.append("        if (ddEntry != null) {").println();
        out.append("            try {").println();
        out.append("                ").append(rootType.interfaceName).append(" fromApp = ").println();

        out.append("              new ").append(model.parserImplClassName).append("(containerToAdapt, ddEntry");
        if (model.xmiPrimaryDDTypeName != null) {
            out.append(", xmi");
        }
        out.append(").parse();").println();

        out.append("               if (fromConfig == null) {").println();
        out.append("                   return fromApp;").println();
        out.append("                } else {  ").println();
        out.append("                   fromConfig.setDelegate(fromApp);").println();
        out.append("                    return fromConfig;").println();
        out.append("                }").println();
        out.append("            } catch (ParseException e) {").println();
        out.append("                throw new UnableToAdaptException(e);").println();
        out.append("            }").println();
        out.append("        }").println();
        out.println();
        out.append("        return fromConfig;").println();
        out.append("    }").println();

        out.append(getConfigOverridesMethod(componentImplName, rootType));
        out.append("}").println();
        out.close();
    }

    private String getComponentImplClassName(String implClassName) {
        String name = implClassName.substring(0, implClassName.length() - 4);
        return name + "ComponentImpl";

    }

    private static final String INDENT = "     ";

    private String getConfigOverridesMethod(String componentImplName, ModelInterfaceType rootType) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("private ").append(componentImplName).append(" getConfigOverrides(OverlayContainer rootOverlay, ArtifactContainer artifactContainer)");
        if (rootType.isLibertyModule()) {
            buffer.append(" throws UnableToAdaptException");
        }
        buffer.append(" {\n");
        buffer.append(INDENT).append("if (configurations == null || configurations.isEmpty())\n");
        buffer.append(INDENT).append(INDENT).append("return null;\n\n");
        buffer.append(INDENT).append("ApplicationInfo appInfo = (ApplicationInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);\n");

        if (rootType.isLibertyModule()) {
            buffer.append(INDENT).append("ModuleInfo moduleInfo = null;\n");
            buffer.append(INDENT).append("if (appInfo == null && rootOverlay.getParentOverlay() != null) {\n");
            buffer.append(INDENT).append(INDENT).append("moduleInfo = (ModuleInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ModuleInfo.class);\n");
            buffer.append(INDENT).append(INDENT).append("if (moduleInfo == null)\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("return null;\n");
            buffer.append(INDENT).append(INDENT).append("appInfo = moduleInfo.getApplicationInfo();\n");
            buffer.append(INDENT).append("}\n");
        }
        buffer.append(INDENT).append("NestedConfigHelper configHelper = null;\n");
        buffer.append(INDENT).append("if (appInfo != null && appInfo instanceof ExtendedApplicationInfo)\n");
        buffer.append(INDENT).append(INDENT).append("configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();\n");
        buffer.append(INDENT).append(" if (configHelper == null)\n");
        buffer.append(INDENT).append(INDENT).append("return null;\n\n");
        if (rootType.isLibertyModule()) {
            buffer.append(INDENT).append("Set<String> configuredModuleNames = new HashSet<String>();\n");
        }
        buffer.append(INDENT).append("String servicePid = (String) configHelper.get(\"service.pid\");\n");
        buffer.append(INDENT).append("String extendsPid = (String) configHelper.get(\"ibm.extends.source.pid\");\n");
        buffer.append(INDENT).append("for (").append(rootType.interfaceName).append(" config : configurations) {\n");
        buffer.append(INDENT).append(INDENT).append(componentImplName).append(" configImpl = (").append(componentImplName).append(")").append(" config;\n");
        buffer.append(INDENT).append(INDENT).append("String parentPid = (String) configImpl.getConfigAdminProperties().get(\"config.parentPID\");\n");
        buffer.append(INDENT).append(INDENT).append("if ( servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {\n");
        if (rootType.isLibertyModule()) {
            buffer.append(INDENT).append(INDENT).append(INDENT).append("if (moduleInfo == null)\n");
        }
        buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("return configImpl;\n");

        if (rootType.isLibertyModule()) {
            buffer.append(INDENT).append(INDENT).append(INDENT).append("String moduleName = (String) configImpl.getConfigAdminProperties().get(\"moduleName\");\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("if (moduleName == null) {\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, ").append(simpleName).append(".class) == null) {\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("Tr.error(tc, \"module.name.not.specified\", \"").append(rootType.rootElementModel.root.name).append("\" );\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, ").append(simpleName).append(".class, MODULE_NAME_NOT_SPECIFIED);\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("}\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("continue;\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("}\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("moduleName = stripExtension(moduleName);\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("configuredModuleNames.add(moduleName);\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("if (moduleInfo.getName().equals(moduleName))\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("return configImpl;\n");
            buffer.append(INDENT).append("}\n");
            buffer.append(INDENT).append("}\n");
            buffer.append(INDENT).append("if (moduleInfo != null && !configuredModuleNames.isEmpty()) {\n");
            buffer.append(INDENT).append(" if (rootOverlay.getParentOverlay().getFromNonPersistentCache(MODULE_NAME_INVALID, ").append(simpleName).append(".class) == null) {\n");
            buffer.append(INDENT).append(INDENT).append("HashSet<String> moduleNames = new HashSet<String>();\n");
            buffer.append(INDENT).append(INDENT).append("Application app = appInfo.getContainer().adapt(Application.class);\n");
            buffer.append(INDENT).append(INDENT).append("for (Module m : app.getModules()) {\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("moduleNames.add(stripExtension(m.getModulePath()));\n");
            buffer.append(INDENT).append(INDENT).append("}\n");
            buffer.append(INDENT).append(INDENT).append("configuredModuleNames.removeAll(moduleNames);\n");
            buffer.append(INDENT).append(INDENT).append("if ( !configuredModuleNames.isEmpty() )\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("Tr.error(tc, \"module.name.invalid\", configuredModuleNames, \"").append(rootType.rootElementModel.root.name).append("\");\n");
            buffer.append(INDENT).append(INDENT).append("rootOverlay.getParentOverlay().addToNonPersistentCache(MODULE_NAME_INVALID, ").append(simpleName).append(".class, MODULE_NAME_INVALID);\n");
            buffer.append(INDENT).append(INDENT).append("}\n");
        } else {
            buffer.append(INDENT).append(INDENT).append("}\n");
        }
        buffer.append(INDENT).append("}\n");
        buffer.append(INDENT).append("return null;\n");
        buffer.append("}\n");

        if (rootType.isLibertyModule()) {
            buffer.append(INDENT).append("private String stripExtension(String moduleName) {\n");
            buffer.append(INDENT).append(INDENT).append("if (moduleName.endsWith(\".war\") || moduleName.endsWith(\".jar\")) {\n");
            buffer.append(INDENT).append(INDENT).append(INDENT).append("return moduleName.substring(0, moduleName.length() - 4);\n");
            buffer.append(INDENT).append(INDENT).append("}\n");
            buffer.append(INDENT).append(INDENT).append("return moduleName;\n");
            buffer.append(INDENT).append("}\n");
        }

        return buffer.toString();
    }

    protected void writeStatics(PrintWriter out) {}

    protected void writeInitializeEntryName(PrintWriter out, String ddEntryNameVar, String xmiVar) {
        String interfaceName = model.getRootType().interfaceName;
        String constantFileType = interfaceName.endsWith("Bnd") ? "BND" : "EXT";

        if (model.xmiPrimaryDDTypeName == null) {
            throw new UnsupportedOperationException();
        } else {
            out.append("        ").append(model.xmiPrimaryDDTypeName).append(" primary = containerToAdapt.adapt(").append(model.xmiPrimaryDDTypeName).append(".class);").println();
            out.append("        String primaryVersion = primary == null ? null : primary.getVersion();").println();
            out.append("        String ").append(ddEntryNameVar).append(';').println();
            out.append("        boolean ").append(xmiVar).append(" = ");
            for (int i = 0; i < model.xmiPrimaryDDVersions.size(); i++) {
                if (i != 0) {
                    out.append(" || ");
                }
                out.append('"').append(model.xmiPrimaryDDVersions.get(i)).append("\".equals(primaryVersion)");
            }
            out.append(";").println();
            out.append("        if (").append(xmiVar).append(") {").println();
            out.append("            ").append(ddEntryNameVar).append(" = ").append(interfaceName).append(".XMI_").append(constantFileType).append("_NAME;").println();
            out.append("        } else {").println();
            out.append("            ").append(ddEntryNameVar).append(" = ").append(interfaceName).append(".XML_").append(constantFileType).append("_NAME;").println();
            out.append("        }").println();
        }
    }
}
