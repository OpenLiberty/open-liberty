/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.ws.javaee.ddmetadata.model.ModelAttribute;
import com.ibm.ws.javaee.ddmetadata.model.ModelBasicType;
import com.ibm.ws.javaee.ddmetadata.model.ModelElement;
import com.ibm.ws.javaee.ddmetadata.model.ModelEnumType;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;
import com.ibm.ws.javaee.ddmetadata.model.ModelMethod;
import com.ibm.ws.javaee.ddmetadata.model.ModelNode;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class ComponentImplGenerator extends ModelClassGenerator {
    final ModelInterfaceType type;
    private final static String INDENT = "     ";
    private final static String QUOTE = "\"";

    public ComponentImplGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, getClassName(type.implClassName));
        this.type = type;
    }

    /**
     * The model interface type has a name ending in 'Type'. For the moment, modify it here.
     */
    private static String getClassName(String implClassName) {
        implClassName = implClassName.substring(0, implClassName.length() - 4);
        return implClassName + "ComponentImpl";
    }

    public void generate() {
        if (this.type.isLibertyNotInUse())
            return;

        PrintWriter out = open();
        out.println("import org.osgi.service.component.annotations.Activate;");
        out.println("import org.osgi.service.component.annotations.Component;");
        out.println("import org.osgi.service.component.annotations.ConfigurationPolicy;");
        out.println("import org.osgi.service.component.annotations.Reference;");
        out.println("import org.osgi.service.component.annotations.ReferenceCardinality;");
        out.println("import org.osgi.service.component.annotations.ReferencePolicy;");

        out.println("import java.util.Map;");
        out.println("import java.util.ArrayList;");
        out.println("import java.util.List;");
        out.println();
        writeClass(out, "", type, simpleName, false);
        out.close();
    }

    private class ComponentDefinition {
        private final ModelInterfaceType type;

        ComponentDefinition(ModelInterfaceType type) {
            this.type = type;
        }

        private String getFactoryPid() {
            return type.getJavaTypeName();
        }

        @Override
        public String toString() {
            StringBuffer ocd = new StringBuffer();
            ocd.append("@Component(configurationPid = \"");
            ocd.append(getFactoryPid());
            ocd.append("\",\n");
            ocd.append(INDENT).append("configurationPolicy = ConfigurationPolicy.REQUIRE,\n");
            ocd.append(INDENT).append("immediate=true,\n");
            ocd.append(INDENT).append("property = \"service.vendor = IBM\")\n");

            return ocd.toString();
        }
    }

    private void writeClass(PrintWriter out, String indent, ModelInterfaceType type, String simpleName, boolean inner) {

        ComponentDefinition component = new ComponentDefinition(type);
        out.append(component.toString());
        out.append(indent).append("public ");

        out.append("class ").append(simpleName);

        ModelInterfaceType extendsSupertype = type.getExtendsSupertype();
        if (extendsSupertype != null) {
            out.append(" extends ").append(extendsSupertype.implClassName);
        }

        List<String> interfaceNames = new ArrayList<String>();
        if (type.interfaceName != null) {
            interfaceNames.add(type.interfaceName);
        }

        if (!interfaceNames.isEmpty()) {
            out.append(" implements ");
            for (int i = 0; i < interfaceNames.size(); i++) {
                if (i != 0) {
                    out.append(", ");
                }
                out.append(interfaceNames.get(i));
            }
        }
        out.append(" {").println();

        out.append("private Map<String,Object> configAdminProperties;").println();
        out.append("private " + type.interfaceName + " delegate;").println();

        Map<ModelMethod, Set<String>> methodsInUse = new HashMap<ModelMethod, Set<String>>();

        // Loop through the hierarchy and create field declarations. We also keep track of the element name
        // because in some cases multiple levels declare the same element.
        ModelInterfaceType currentType = type;
        Set<String> elementNames = new HashSet<String>();
        while (currentType != null) {
            for (ModelElement element : currentType.elements) {
                if (!element.isLibertyNotInUse() && elementNames.add(element.name)) {
                    if (element.inlineAttribute == null)
                        addMethodInUse(methodsInUse, element.method, element.name);
                    else
                        addMethodInUse(methodsInUse, element.inlineAttribute.method, element.inlineAttribute.name);
                    FieldDeclaration fd = new FieldDeclaration(element);
                    out.append(fd.toString());
                }

            }

            for (ModelAttribute attr : currentType.attributes) {
                if (!attr.isLibertyNotInUse() && elementNames.add(attr.name)) {
                    addMethodInUse(methodsInUse, attr.method, attr.name);
                    FieldDeclaration fd = new FieldDeclaration(attr);
                    out.append(fd.toString());
                }
            }
            currentType = currentType.getExtendsSupertype();
        }

        ActivateMethod activateMethod = new ActivateMethod(type);
        out.println();
        out.append(activateMethod.toString());

        // Loop through hierarchy and generate methods at each level. Also keep a map of method names (some types strangely
        // include the same method at two levels.)
        currentType = type;
        Set<String> methodNames = new HashSet<String>();
        while (currentType != null) {
            for (ModelMethod method : currentType.methods) {
                if (methodNames.add(method.name)) {
                    out.println();

                    MethodDeclaration methodDeclaration = new MethodDeclaration(method, methodsInUse);
                    out.append(methodDeclaration.toString());
                    out.println();
                }
            }
            currentType = currentType.getExtendsSupertype();
        }

        // Special methods
        if (type.ddSupertype) {
            out.println();
            out.println("// Methods required to implement DeploymentDescriptor -- Not used in Liberty");
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public String getDeploymentDescriptorPath() {").println();
            out.append(indent).append("        return null;").println();
            out.append(indent).append("    }").println();
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public Object getComponentForId(String id) {").println();
            out.append(indent).append("        return null;").println();
            out.append(indent).append("    }").println();
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public String getIdForComponent(Object ddComponent) {").println();
            out.append(indent).append("        return null;").println();
            out.append(indent).append("    }").println();
            out.println("// End of DeploymentDescriptor Methods -- Not used in Liberty");
        }

        out.append(INDENT).append("public Map<String,Object> getConfigAdminProperties() {").println();
        out.append(INDENT).append(INDENT).append("return this.configAdminProperties;").println();
        out.append(INDENT).append("}").println();
        out.println();
        out.append(INDENT).append("public void setDelegate(").append(type.interfaceName).append(" delegate) {").println();
        out.append(INDENT).append(INDENT).append("this.delegate = delegate;").println();
        out.append(INDENT).append("}").println();

        // Extra methods as required
        writeExtra(out, indent);

        out.append(indent).append("}").println();
    }

    /**
     * @param methodsInUse
     * @param method
     * @param name
     */
    private void addMethodInUse(Map<ModelMethod, Set<String>> methodsInUse, ModelMethod method, String name) {
        Set<String> elementNames = methodsInUse.get(method);
        if (elementNames == null) {
            elementNames = new HashSet<String>();
            methodsInUse.put(method, elementNames);
        }
        elementNames.add(name);

    }

    private static class ActivateMethod {

        private final Map<String, ModelAttribute> attributes = new HashMap<String, ModelAttribute>();

        ActivateMethod(ModelInterfaceType type) {
            // Using a map to associate the attribute name with the attribute because inline attributes need the name from the element.
            // It also helps to remove duplicates from the hierarchy.
            ModelInterfaceType currentType = type;
            while (currentType != null) {
                for (ModelAttribute attr : currentType.attributes) {
                    attributes.put(attr.name, attr);
                }

                for (ModelElement element : currentType.elements) {
                    if (element.inlineAttribute != null) {
                        attributes.put(element.name, element.inlineAttribute);
                    }
                }
                currentType = currentType.getExtendsSupertype();
            }
        }

        @Override
        public String toString() {
            boolean hasReference = false;

            StringBuffer buffer = new StringBuffer();
            buffer.append(INDENT).append("@Activate\n");
            buffer.append(INDENT).append("protected void activate(Map<String, Object> config) {\n");
            buffer.append(INDENT).append(INDENT).append("this.configAdminProperties = config;\n");

            for (Entry<String, ModelAttribute> entry : attributes.entrySet()) {
                String attributeName = entry.getKey();
                ModelAttribute attr = entry.getValue();
                if (!attr.isLibertyNotInUse()) {

                    String typeName = getTypeName(attr.method);
                    if (attr.method.field.getLibertyReference() != null) {
                        hasReference = true;
                    }
                    if (attr.method.getType() instanceof ModelEnumType) {
                        buffer.append(INDENT).append(INDENT).append("if (config.get(").append(QUOTE).append(attributeName).append(QUOTE).append(") != null)\n");
                        buffer.append(INDENT).append(INDENT).append(INDENT).append(attr.method.field.name).append(" = ");
                        buffer.append(typeName).append(".valueOf((String) config.get(\"").append(attributeName).append("\"));\n");
                        //  client_mode = com.ibm.ws.javaee.dd.appext.ApplicationExt.ClientModeEnum.valueOf((String)config.get("client-mode"));
                    } else if (attr.method.field.getDurationTimeUnit() != null) {
                        buffer.append(INDENT).append(INDENT).append(attr.method.field.name).append(" = ");
                        buffer.append("((Long) config.get(\"").append(attributeName).append("\")).intValue();\n");
                    } else {
                        buffer.append(INDENT).append(INDENT).append(attr.method.field.name).append(" = ");
                        buffer.append("(").append(typeName).append(") config.get(\"").append(attributeName).append("\");\n");
                    }

                }
            }
            buffer.append(INDENT).append("}\n");

            if (hasReference) {
                buffer.append(INDENT).append("@Reference\n");
                buffer.append(INDENT).append("org.osgi.service.cm.ConfigurationAdmin configAdmin;\n");

                buffer.append("\n");
                buffer.append(INDENT).append("private String getIDForPID(String pid) {\n");
                buffer.append(INDENT).append(INDENT).append("try {\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("String filter = com.ibm.wsspi.kernel.service.utils.FilterUtils.createPropertyFilter(org.osgi.framework.Constants.SERVICE_PID, name);\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations(filter);\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("if (configs == null || configs.length == 0)\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("return null;\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("return (String) configs[0].getProperties().get(\"id\");\n");
                buffer.append(INDENT).append(INDENT).append("} catch (java.io.IOException e) {\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("e.getCause();\n");
                buffer.append(INDENT).append(INDENT).append("} catch (org.osgi.framework.InvalidSyntaxException e) {\n");
                buffer.append(INDENT).append(INDENT).append(INDENT).append("e.getCause();\n");
                buffer.append(INDENT).append(INDENT).append("}\n");
                buffer.append(INDENT).append(INDENT).append("return null;\n");
                buffer.append(INDENT).append(INDENT).append("}\n");
            }

            return buffer.toString();
        }
    }

    private static String getTypeName(ModelMethod method) {
        String typeName = method.getJavaTypeName();
        if (method.getType() == ModelBasicType.Boolean) {
            typeName = "Boolean";
        } else if (method.getType() == ModelBasicType.Int) {
            typeName = "Integer";
        } else if (method.getType() == ModelBasicType.Long) {
            typeName = "Long";
        } else if (method.getType() == ModelBasicType.ProtectedString) {
            typeName = SerializableProtectedString.class.getName();
        }
        return typeName;
    }

    private static class FieldDeclaration {
        private String methodTypeName;
        private boolean isElement;
        private final boolean isList;
        private String fieldName;
        private final String nodeName;

        FieldDeclaration(ModelElement element) {
            this(element, true);

            if (element.inlineAttribute != null) {
                ModelAttribute inline = element.inlineAttribute;
                this.methodTypeName = getTypeName(inline.method);
                this.isElement = false;
                this.fieldName = inline.method.field.name;
            } else {
                this.methodTypeName = element.getType().getJavaTypeName();
            }

        }

        FieldDeclaration(ModelAttribute attr) {
            this(attr, false);
        }

        FieldDeclaration(ModelNode node, boolean isElement) {
            // The xml node name (virtual-host)
            this.nodeName = node.name;
            // The java friendly field name (virtual_host)
            this.fieldName = modifyFieldName(node.name);
            this.methodTypeName = getTypeName(node.method);
            this.isElement = isElement;
            this.isList = node.method.isList();
        }

        @Override
        public String toString() {
            if (fieldName == null)
                return "";

            StringBuffer buffer = new StringBuffer();
            if (isElement) {

                buffer.append("\n").append(INDENT).append("@Reference(cardinality = ReferenceCardinality.");
                if (isList) {
                    buffer.append("MULTIPLE");
                } else {
                    buffer.append("OPTIONAL");
                }
                buffer.append(", policy = ReferencePolicy.DYNAMIC");
                buffer.append(", name = ").append(QUOTE).append(nodeName).append(QUOTE);
                buffer.append(", target = \"(id=unbound)\")\n");
                if (isList) {
                    String toUpper = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    //     Reference annotations on multiple cardinality fields seem to be broken. For now, we'll generate bind/unbind methods here.
                    buffer.append(INDENT).append("protected void set").append(toUpper).append("(").append(methodTypeName).append(" value) {\n");
                    buffer.append(INDENT).append(INDENT).append("this.").append(fieldName).append(".add(value);\n");
                    buffer.append(INDENT).append("}\n\n");
                    buffer.append(INDENT).append("protected void unset").append(toUpper).append("(").append(methodTypeName).append(" value) {\n");
                    buffer.append(INDENT).append(INDENT).append("this.").append(fieldName).append(".remove(value);\n");
                    buffer.append(INDENT).append("}\n\n");
                }
            }
            buffer.append(INDENT).append("protected ");
            if (isElement)
                buffer.append("volatile ");
            if (isList)
                buffer.append("List<").append(methodTypeName).append("> ");
            else
                buffer.append(methodTypeName).append(" ");
            buffer.append(fieldName);
            if (isElement && isList) {
                buffer.append(" = new ArrayList<").append(methodTypeName).append(">()");
            }
            buffer.append(";\n");

            return buffer.toString();
        }
    }

    private static String modifyFieldName(String fieldName) {
        if (fieldName == null)
            return null;

        fieldName = fieldName.replace("-", "_");
        if (("class".equals(fieldName)) || ("interface".equals(fieldName)))
            fieldName = fieldName + "_";

        return fieldName;

    }

    private static class MethodDeclaration {
        private final ModelMethod method;
        private final boolean inUse;
        private final Set<String> fieldNames;

        MethodDeclaration(ModelMethod method, Map<ModelMethod, Set<String>> methodsInUse) {
            this.method = method;

            this.fieldNames = methodsInUse.get(method);
            boolean inUse = (this.fieldNames != null);
            // If this is an "isSet" method, consider it in use
            if (inUse == false && methodsInUse.containsKey(method.isSetAccessorMethod))
                inUse = true;

            this.inUse = inUse;
        }

        @Override
        public String toString() {
            if (method.name == null)
                return "";

            StringBuffer buffer = new StringBuffer();
            buffer.append("");
            buffer.append(INDENT).append("@Override\n");
            if (method.getType() == ModelBasicType.ProtectedString) {
                buffer.append(INDENT).append("@com.ibm.websphere.ras.annotation.Sensitive\n");
            }
            buffer.append(INDENT).append("public ");
            buffer.append(method.getJavaTypeName());
            buffer.append(' ');
            buffer.append(method.name);
            buffer.append("() {\n");
            if (!inUse) {
                buffer.append(INDENT).append(INDENT).append("// Not Used In Liberty -- returning default value or app configuration\n");
                if (method.isList()) {
                    buffer.append(INDENT).append(INDENT).append(method.getJavaTypeName()).append(" returnValue = delegate == null ? new ArrayList<").append(method.field.type.getJavaTypeName()).append(">() : new ArrayList<").append(method.field.type.getJavaTypeName()).append(">(delegate.").append(method.name).append("());\n");
                    buffer.append(INDENT).append(INDENT).append("return returnValue;\n");
                } else {
                    buffer.append(INDENT).append(INDENT).append("return delegate == null ? ").append(method.getDefaultValue());
                    buffer.append(" : delegate.").append(method.name).append("();\n");
                }
            } else if (method.isList()) {
                buffer.append(INDENT).append(INDENT).append(method.getJavaTypeName()).append(" returnValue = delegate == null ? new ArrayList<").append(method.field.type.getJavaTypeName()).append(">() : new ArrayList<").append(method.field.type.getJavaTypeName()).append(">(delegate.").append(method.name).append("());\n");
                for (String fieldName : fieldNames) {
                    if (fieldName != null) {
                        String modifiedFieldName = modifyFieldName(fieldName);
                        buffer.append(INDENT).append(INDENT).append("returnValue.addAll(").append(modifiedFieldName).append(");\n");
                    }
                }
                buffer.append(INDENT).append(INDENT).append("return returnValue;\n");
            } else {
                if (method.isSetAccessorMethod != null) {
                    buffer.append(INDENT).append(INDENT).append("return (").append(method.isSetAccessorMethod.field.name).append("!= null);\n");
                } else if (method.field.getLibertyReference() != null) {
                    buffer.append(INDENT).append(INDENT).append("String id = getIDForPID(").append(method.field.name).append(");\n");
                    buffer.append(INDENT).append(INDENT).append("if (delegate == null) {\n");
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("return id == null ? null : id;\n");
                    buffer.append(INDENT).append(INDENT).append("} else {\n");
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("return id == null ? delegate.").append(method.name).append("() : id;\n");
                    buffer.append(INDENT).append(INDENT).append("}\n");
                } else {
                    String returnValue = method.field.name;
                    if (method.getType() == ModelBasicType.ProtectedString) {
                        returnValue = "new String(" + method.field.name + ".getChars())";
                    }
                    buffer.append(INDENT).append(INDENT).append("if (delegate == null) {\n");
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("return ").append(method.field.name).append(" == null ? ").append(method.getDefaultValue()).append(" : ").append(returnValue).append(";\n");
                    buffer.append(INDENT).append(INDENT).append("} else {\n");
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("return ").append(method.field.name).append(" == null ? delegate.").append(method.name).append("() : ").append(returnValue).append(";\n");
                    buffer.append(INDENT).append(INDENT).append("}\n");
                }

            }
            buffer.append(INDENT).append("}");

            return buffer.toString();
        }
/*
 *
 * returnValue.addAll(security_role);
 * return returnValue;
 */
    }

    protected void writeFieldsExtra(PrintWriter out, String indent) {}

    protected void writeExtra(PrintWriter out, String indent) {}

    protected boolean isHandleChildExtraNeeded() {
        return false;
    }

    protected void writeHandleChildExtra(PrintWriter out, String indent) {}

    protected boolean isXMISuperHandleChild() {
        return true;
    }

    protected boolean isFinishExtraNeeded() {
        return false;
    }

    protected void writeFinishExtra(PrintWriter out, String indent) {}
}
