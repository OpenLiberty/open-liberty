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
import java.util.List;

import com.ibm.ws.javaee.ddmetadata.model.ModelAttribute;
import com.ibm.ws.javaee.ddmetadata.model.ModelElement;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;
import com.ibm.ws.javaee.ddmetadata.model.ModelNode;

public class MetatypeInterfaceGenerator extends ModelClassGenerator {
    final ModelInterfaceType type;

    public MetatypeInterfaceGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, getClassName(type.implClassName));
        this.type = type;
    }

    /**
     * The model interface type has a name ending in 'Type'. For the moment, modify it here.
     */
    private static String getClassName(String implClassName) {
        implClassName = implClassName.substring(0, implClassName.length() - 4);
        return implClassName + "Metatype";
    }

    public void generate() {
        if (this.type.isLibertyNotInUse()) {
            return;
        }
        PrintWriter out = open();
        out.println("import org.osgi.service.metatype.annotations.AttributeDefinition;");
        out.println("import org.osgi.service.metatype.annotations.ObjectClassDefinition;");
        out.println("import com.ibm.ws.bnd.metatype.annotation.Ext;");
        out.println();
        writeClass(out, "", type, simpleName, false);
        out.close();
    }

    public static String getTypeName(Class<?> klass) {
        return klass.getName().replace('$', '.');
    }

    private class OCD {
        private final ModelInterfaceType type;
        private final String simpleName;

        OCD(ModelInterfaceType type, String simpleName) {
            this.type = type;
            this.simpleName = simpleName;
        }

        private String getFactoryPid() {
            return type.getJavaTypeName();
        }

        private String getNLSName() {
            return "%" + simpleName.toLowerCase() + ".name";
        }

        private String getNLSDescription() {
            return "%" + simpleName.toLowerCase() + ".desc";
        }

        @Override
        public String toString() {
            StringBuffer ocd = new StringBuffer();
            ocd.append("@ObjectClassDefinition(factoryPid = \"");
            ocd.append(getFactoryPid());
            ocd.append("\", name = \"");
            ocd.append(getNLSName());
            ocd.append("\", description = \"");
            ocd.append(getNLSDescription());
            ocd.append("\", localization = \"OSGI-INF/l10n/metatype\")");
            ocd.append("\n");

            return ocd.toString();
        }
    }

    private void writeClass(PrintWriter out, String indent, ModelInterfaceType type, String simpleName, boolean inner) {

        OCD ocd = new OCD(type, simpleName);
        out.append(ocd.toString());
        out.append(indent).append("public ");

        out.append("interface ").append(simpleName);

        List<String> interfaceNames = new ArrayList<String>();
        if (type.interfaceName != null) {
            interfaceNames.add(type.interfaceName);
        }

//        if (!interfaceNames.isEmpty()) {
//            out.append(" extends ");
//            for (int i = 0; i < interfaceNames.size(); i++) {
//                if (i != 0) {
//                    out.append(", ");
//                }
//                out.append(interfaceNames.get(i));
//            }
//        }
        out.append(" {").println();

        ModelInterfaceType currentType = type;
        while (currentType != null) {
            // Getters for elements and attributes
            for (ModelElement element : currentType.elements) {
                if (element.isLibertyNotInUse()) {
                    continue;
                }
                out.println();

                try {
                    AD attributeDefinition = new AD(element, indent);
                    out.append(indent).append(attributeDefinition.toString()).println();
                    out.println();
                } catch (IgnoredAttributeException e) {
                    // Expected. This is part of the XMI but isn't used in the model
                }

            }

            for (ModelAttribute attr : currentType.attributes) {
                if (attr.isLibertyNotInUse()) {
                    continue;
                }
                out.println();
                try {
                    AD attributeDefinition = new AD(attr, indent);
                    out.append(indent).append(attributeDefinition.toString()).println();
                    out.println();
                } catch (IgnoredAttributeException e) {
                    // Expected. This is part of the XMI but isn't used in the model
                }

            }

            currentType = currentType.getExtendsSupertype();

        }

        // Extra methods as required
        writeExtra(out, indent);

        out.append(indent).append("}").println();
    }

    private static class AD {
        private final String methodName;
        private final boolean isElement;
        private final String fieldTypeName;
        private final String indent;
        private final int cardinality;

        AD(ModelElement element, String indent) throws IgnoredAttributeException {
            this(element, indent, true);
        }

        AD(ModelAttribute attr, String indent) throws IgnoredAttributeException {
            this(attr, indent, false);
        }

        /**
         * @param element
         * @param indent2
         * @param b
         * @throws IgnoredAttributeException
         */
        private AD(ModelNode element, String indent, boolean isElement) throws IgnoredAttributeException {
            this.methodName = getBndFriendlyMethodName(element);
            if (element.method.isList()) {
                cardinality = Integer.MAX_VALUE;
            } else {
                cardinality = 0;
            }
            this.isElement = isElement;
            this.fieldTypeName = element.method.field.type.getJavaTypeName();
            this.indent = indent;
        }

        /**
         * @param method
         * @return
         * @throws IgnoredAttributeException
         */
        private String getBndFriendlyMethodName(ModelNode field) throws IgnoredAttributeException {
            String name = field.name;
            if (name == null)
                throw new IgnoredAttributeException();
            name = name.replace("_", "__");
            // TEMP - The metatype generator needs to handle - chars
            name = name.replace("-", "__");
            name = name.replace("$", "$$");

            // Java keywords
            if (name.equals("class"))
                name = "$class";
            if (name.equals("interface"))
                name = "$interface";

            return name;
        }

        @Override
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("    ");
            buffer.append("@AttributeDefinition(");
            buffer.append("name = \"%");
            buffer.append(methodName);
            buffer.append(".name\",");
            buffer.append(" description = \"%");
            buffer.append(methodName);
            buffer.append(".desc\")");

            if (isElement) {
                buffer.append("\n");
                buffer.append("    ");
                buffer.append("@Ext.FlatReferencePid(\"");
                buffer.append(fieldTypeName);
                buffer.append("\")");
            }

            buffer.append(getMethodDeclaration());

            return buffer.toString();
        }

        /**
         * @return
         */
        private String getMethodDeclaration() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("\n");
            buffer.append(indent);
            buffer.append("    public ");
            buffer.append(getReturnType());
            buffer.append(' ');
            buffer.append(methodName);
            buffer.append("();");

            return buffer.toString();
        }

        /**
         * @return
         */
        private String getReturnType() {
            StringBuffer returnType = new StringBuffer();
            if (isElement) {
                returnType.append("java.lang.String");
            } else {
                returnType.append(fieldTypeName);
            }

            if (cardinality > 0)
                returnType.append("[]");

            return returnType.toString();
        }
    }

    protected static class IgnoredAttributeException extends Exception {
        private static final long serialVersionUID = 1L;
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
