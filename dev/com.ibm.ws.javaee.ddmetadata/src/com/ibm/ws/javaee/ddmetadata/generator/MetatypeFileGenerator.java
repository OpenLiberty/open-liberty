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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.javaee.ddmetadata.model.Model;
import com.ibm.ws.javaee.ddmetadata.model.ModelAttribute;
import com.ibm.ws.javaee.ddmetadata.model.ModelBasicType;
import com.ibm.ws.javaee.ddmetadata.model.ModelElement;
import com.ibm.ws.javaee.ddmetadata.model.ModelEnumType;
import com.ibm.ws.javaee.ddmetadata.model.ModelEnumType.Constant;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;
import com.ibm.ws.javaee.ddmetadata.model.ModelNode;

public class MetatypeFileGenerator extends ModelClassGenerator {
    final ModelInterfaceType type;
    final static String QUOTE = "\"";
    final static String SPACE = " ";
    final static String END_ELEMENT = "/>\n";

    final static String HEADER = "<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.1.0\"\n" +
                                 "xmlns:ibm=\"http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0\"\n" +
                                 "localization=\"OSGI-INF/l10n/metatype\">";

    final static String FOOTER = "</metatype:MetaData>";

    public MetatypeFileGenerator(File destdir, ModelInterfaceType type) {
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
        out.println(HEADER);
        out.println();
        OCD ocd = buildModel(type, simpleName);
        out.println(ocd.toString());
        out.println(FOOTER);
        out.close();
    }

    @Override
    PrintWriter open() {
        File packageDir = new File(destdir, "OSGI-INF/metatype");

        if (!packageDir.mkdirs() && !packageDir.isDirectory()) {
            throw new IllegalStateException("Unable to create directory: " + packageDir);
        }

        File classFile = new File(packageDir, simpleName + ".xml");

        // Avoid having two SessionMetatype.xml, etc files
        for (int i = 2; classFile.exists(); i++) {
            classFile = new File(packageDir, simpleName + i + ".xml");
        }

        try {
            PrintWriter out = new PrintWriter(classFile, "UTF-8");
            out.println("<!-- NOTE: This is a generated file. Do not edit it directly. -->");
            out.println();
            return out;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getTypeName(Class<?> klass) {
        return klass.getName().replace('$', '.');
    }

    private class OCD {
        private final ModelInterfaceType type;
        private final String simpleName;
        private final List<AD> attributes = new ArrayList<AD>();
        private String extendedOcd;
        private final String childAlias;
        private final boolean needsModuleNameAttribute;

        OCD(ModelInterfaceType type, String simpleName) {
            this.type = type;
            this.simpleName = simpleName;
            this.needsModuleNameAttribute = type.isLibertyModule();

            Model model = type.rootElementModel;
            if (model != null) {
                childAlias = model.root.name;
            } else {
                childAlias = null;
            }
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
            ocd.append("<OCD id=").append(QUOTE).append(getFactoryPid()).append(QUOTE);
            ocd.append(" name=").append(QUOTE).append(getNLSName()).append(QUOTE);
            ocd.append(" description=").append(QUOTE).append(getNLSDescription()).append(QUOTE);
            if (extendedOcd != null) {
                ocd.append(" ibm:extends=").append(QUOTE).append(extendedOcd).append(QUOTE);
            }
            if (childAlias != null) {
                ocd.append(" ibm:parentPid=\"com.ibm.ws.app.manager\" ibm:childAlias=").append(QUOTE).append(childAlias).append(QUOTE);
            }

            ocd.append(">\n");

            if (needsModuleNameAttribute) {
                ocd.append("<AD id=\"moduleName\" name=\"%moduleNameAttr.name\" description=\"%moduleNameAttr.desc\" type=\"String\" required=\"false\" cardinality=\"0\" />\n");
            }
            for (AD ad : attributes) {
                ocd.append(ad.toString());
            }
            ocd.append("</OCD>\n\n");
            ocd.append("<Designate factoryPid=\"");
            ocd.append(getFactoryPid());
            ocd.append("\">\n");
            ocd.append("     <Object ocdref=\"");
            ocd.append(getFactoryPid());
            ocd.append("\"/>\n");
            ocd.append("</Designate>\n");

            return ocd.toString();
        }

        /**
         * @param attributeDefinition
         */
        public void addAttributeDefinition(AD attributeDefinition) {
            this.attributes.add(attributeDefinition);
            attributeDefinition.setOCDName(simpleName);

        }

        /**
         * @param javaTypeName
         */
        public void setExtends(String javaTypeName) {
            this.extendedOcd = javaTypeName;

        }
    }

    private OCD buildModel(ModelInterfaceType type, String simpleName) {
        OCD ocd = new OCD(type, simpleName);

        Set<String> attributeNames = new HashSet<String>();
        Map<String, Set<ModelElement>> methodNameToElements = new HashMap<String, Set<ModelElement>>();
        ModelInterfaceType currentType = type;

        for (ModelElement element : currentType.elements) {
            // Ignore non-Liberty, XMI only (element.name == null), and previously added names
            if (element.isLibertyNotInUse() || element.name == null || !attributeNames.add(element.name)) {
                continue;
            }

            AD attributeDefinition = new AD(element);
            ocd.addAttributeDefinition(attributeDefinition);

            if (element.inlineAttribute == null) {
                // The elementsForMethod map is used to collect different elements that have a single method (eg, session-bean and message-driven
                // are both retrieved by getEnterpriseBeans.) After they're collected we produce DS target attributes with a filter that references
                // both elements

                // If the element has an inlineAttribute, it will be generated as a simple attribute rather than an element, so we don't
                // generate a target method.
                Set<ModelElement> elementsForMethod = methodNameToElements.get(element.method.name);
                if (elementsForMethod == null) {
                    elementsForMethod = new HashSet<ModelElement>();
                    methodNameToElements.put(element.method.name, elementsForMethod);
                }
                elementsForMethod.add(element);
            }
        }

        // Add .target filter attributes
        for (ModelElement element : currentType.elements) {
            if (!element.isLibertyNotInUse()) {
                TargetAD target = new TargetAD(element);
                ocd.addAttributeDefinition(target);
            }
        }

        // Add simple attributes
        for (ModelAttribute attr : currentType.attributes) {
            if (attr.isLibertyNotInUse() || !attributeNames.add(attr.name)) {
                continue;
            }

            AD attributeDefinition = new AD(attr);
            ocd.addAttributeDefinition(attributeDefinition);

        }

        if (currentType.getExtendsSupertype() != null) {
            ocd.setExtends(currentType.getExtendsSupertype().getJavaTypeName());
        }

        return ocd;
    }

    private static class TargetAD extends AD {
        private static final String INTERNAL = "internal";
        private final String elementName;

        TargetAD(ModelElement element) {
            super(element, false);
            this.elementName = element.name;
        }

        @Override
        protected String getMetatypeType() {
            return STRING;
        }

        @Override
        protected String getIdAttribute() {
            return elementName + ".target";
        }

        @Override
        protected String getNameAttribute() {
            return INTERNAL;
        }

        @Override
        protected String getDescriptionAttribute() {
            return INTERNAL;
        }

        @Override
        protected String getCardinality() {
            return "0";
        }

        @Override
        protected String getRequired() {
            return "true";
        }

        @Override
        protected String getDefaultValue() {
            return "${servicePidOrFilter(" + elementName + ")}";
        }
    }

    private static class Option {

        private final String name;

        /**
         * @param c
         */
        public Option(Constant c) {
            this.name = c.name;
        }

        @Override
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("        <Option label=").append(QUOTE).append(name).append(QUOTE).append(" value=").append(QUOTE).append(name).append(QUOTE).append("/>\n");
            return buffer.toString();
        }
    }

    /**
     * Represents an attribute definition in metatype.xml. This has gotten a bit ugly as more and more things have been added and could really
     * use a refactoring.
     */
    private static class AD {
        private static final String IBM_REFERENCE = "ibm:reference";
        private static final String IBM_TYPE = "ibm:type";

        static final String STRING = "String";
        private final String elementName;
        private boolean isElement;
        private String fieldTypeName;
        private final Integer cardinality;
        private final boolean isEnum;
        private final String defaultValue;
        private final boolean required;
        private String ocdName;
        private String referenceName;
        private final boolean isPassword;
        private final Set<Option> options = new HashSet<Option>();
        private final TimeUnit durationTimeUnit;
        private final String reference;

        AD(ModelElement element) {
            this(element, true);
            if (element.inlineAttribute != null) {
                this.isElement = false;
                this.fieldTypeName = element.inlineAttribute.method.field.type.getJavaTypeName();
            }
            this.referenceName = element.getType().getJavaTypeName();

        }

        public void setOCDName(String simpleName) {
            this.ocdName = simpleName;

        }

        AD(ModelAttribute attr) {
            this(attr, false);
        }

        private AD(ModelNode element, boolean isElement) {
            this.elementName = element.name;
            if (element.method.isList()) {
                cardinality = Integer.MAX_VALUE;
            } else {
                cardinality = 0;
            }
            this.isElement = isElement;
            this.durationTimeUnit = element.method.field.getDurationTimeUnit();
            this.reference = element.method.field.getLibertyReference();
            this.fieldTypeName = element.method.field.type.getJavaTypeName();
            if (element.method.field.type instanceof ModelEnumType) {
                this.isEnum = true;
                ModelEnumType type = (ModelEnumType) element.method.field.type;
                for (ModelEnumType.Constant c : type.constants) {
                    if (!c.isLibertyNotInUse()) {
                        this.options.add(new Option(c));
                    }
                }
            } else {
                this.isEnum = false;
            }

            this.defaultValue = element.method.getDefaultValue();
            this.required = element.required;

            this.isPassword = (element.method.getType() == ModelBasicType.ProtectedString);

        }

        protected String getRequired() {
            return Boolean.toString(required);
        }

        protected String getDefaultValue() {
            return defaultValue;
        }

        protected String getIdAttribute() {
            return elementName;
        }

        protected String getNameAttribute() {
            return "%" + ocdName + "." + elementName + ".name";
        }

        protected String getDescriptionAttribute() {
            return "%" + ocdName + "." + elementName + ".desc";
        }

        protected String getCardinality() {
            return cardinality.toString();
        }

        protected String getMetatypeType() {
            if (isElement)
                return STRING;
            if (String.class.getName().equals(fieldTypeName)) {
                return STRING;
            } else if (reference != null) {
                return STRING;
            } else if (isEnum) {
                return STRING;
            } else if (durationTimeUnit != null) {
                return STRING;
            } else if (fieldTypeName.equals("int")) {
                return "Integer";
            } else if (fieldTypeName.equals("boolean")) {
                return "Boolean";
            } else {
                return fieldTypeName;
            }
        }

        @Override
        public String toString() {
            if (elementName == null) {
                return "";
            }
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("id", getIdAttribute());
            attributes.put("name", getNameAttribute());
            attributes.put("description", getDescriptionAttribute());
            attributes.put("cardinality", getCardinality());
            attributes.put("required", getRequired());

            if (getDefaultValue() != null) {
                attributes.put("default", getDefaultValue());
            }

            // "referenceName" comes from a referenced model element in the bindings/extensions config.
            // "reference" comes from a @LibertyReference annotation, meaning that it points to something in server.xml outside of
            // bindings/extensions config. 

            if (isElement) {
                attributes.put(IBM_TYPE, "pid");
                attributes.put(IBM_REFERENCE, referenceName);
            } else if (reference != null) {
                attributes.put(IBM_TYPE, "pid");
                attributes.put(IBM_REFERENCE, reference);
            }

            attributes.put("type", getMetatypeType());

            if (durationTimeUnit != null) {
                switch (durationTimeUnit) {
                    case SECONDS:
                        attributes.put(IBM_TYPE, "duration(s)");
                        break;
                    case MILLISECONDS:
                        attributes.put(IBM_TYPE, "duration(ms)");
                        break;
                    case MINUTES:
                        attributes.put(IBM_TYPE, "duration(m)");
                        break;
                    case HOURS:
                        attributes.put(IBM_TYPE, "duration(h)");
                    default:
                        attributes.put(IBM_TYPE, "duration(ms)");
                        break;
                }
            }

            if (isPassword) {
                attributes.put(IBM_TYPE, "password");
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append("    ");
            buffer.append("<AD");
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                buffer.append(SPACE).append(attr.getKey()).append("=").append(QUOTE).append(attr.getValue()).append(QUOTE);
            }
            if (!isEnum) {
                buffer.append(END_ELEMENT);
            } else {
                buffer.append(">\n");
                for (Option o : options) {
                    buffer.append(o.toString());
                }
                buffer.append("    </AD>\n");
            }

            // TODO <Option> elements for enums
            return buffer.toString();
        }

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
