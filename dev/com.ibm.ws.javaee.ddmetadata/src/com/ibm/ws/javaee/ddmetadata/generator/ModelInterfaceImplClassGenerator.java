/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
import java.util.Set;

import com.ibm.ws.javaee.ddmetadata.model.ModelAttribute;
import com.ibm.ws.javaee.ddmetadata.model.ModelBasicType;
import com.ibm.ws.javaee.ddmetadata.model.ModelElement;
import com.ibm.ws.javaee.ddmetadata.model.ModelEnumType;
import com.ibm.ws.javaee.ddmetadata.model.ModelField;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;
import com.ibm.ws.javaee.ddmetadata.model.ModelMethod;
import com.ibm.ws.javaee.ddmetadata.model.ModelNode;
import com.ibm.ws.javaee.ddmetadata.model.ModelType;
import com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable;

public class ModelInterfaceImplClassGenerator extends ModelClassGenerator {
    final ModelInterfaceType type;

    public ModelInterfaceImplClassGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, type.implClassName);
        this.type = type;
    }

    public void generate() {
        PrintWriter out = open();
        out.println("import com.ibm.ws.javaee.ddmodel.DDParser;");
        if (isTraceComponentNeeded()) {
            out.println("import com.ibm.websphere.ras.Tr;");
            out.println("import com.ibm.websphere.ras.TraceComponent;");
        }
        out.println();
        writeClass(out, "", type, simpleName, false);
        out.close();
    }

    protected boolean isTraceComponentNeeded() {
        return false;
    }

    public static String getTypeName(Class<?> klass) {
        return klass.getName().replace('$', '.');
    }

    private void writeClass(PrintWriter out, String indent, ModelInterfaceType type, String simpleName, boolean inner) {
        out.append(indent).append("public ");
        if (inner) {
            out.append("static ");
        }
        out.append("class ").append(simpleName);
        ModelInterfaceType extendsSupertype = type.getExtendsSupertype();
        if (extendsSupertype == null) {
            out.append(" extends ").append(getTypeName(ElementContentParsable.class));
        } else {
            out.append(" extends ").append(extendsSupertype.implClassName);
        }
        List<String> interfaceNames = new ArrayList<String>();
        if (type.interfaceName != null) {
            interfaceNames.add(type.interfaceName);
        }
        if (type.ddSupertype) {
            interfaceNames.add("DDParser.RootParsable");
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

        // TraceComponent
        if (!inner && isTraceComponentNeeded()) {
            out.append(indent).append("    private static final TraceComponent tc = Tr.register(").append(simpleName).append(".class);").println();
            out.println();
        }

        // Inner classes
        for (ModelInterfaceType anonymousType : type.anonymousTypes) {
            writeClass(out, indent + "    ", anonymousType, anonymousType.implClassName, true);
            out.println();
        }

        // Constructors
        if (type.ddSupertype) {
            if (type.xmi) {
                out.append(indent).append("    public ").append(simpleName).append("(String ddPath) {").println();
                out.append(indent).append("        this(ddPath, false);").println();
                out.append(indent).append("    }").println();
                out.println();
                out.append(indent).append("    public ").append(simpleName).append("(String ddPath, boolean xmi) {").println();
                if (extendsSupertype == null) {
                    out.append(indent).append("        this.xmi = xmi;").println();
                } else {
                    out.append(indent).append("        super(xmi);").println();
                }
            } else {
                out.append(indent).append("    public ").append(simpleName).append("(String ddPath) {").println();
            }

            out.append(indent).append("        this.deploymentDescriptorPath = ddPath;").println();
            out.append(indent).append("    }").println();
            out.println();
        } else {
            if (type.xmi) {
                out.append(indent).append("    public ").append(simpleName).append("() {").println();
                out.append(indent).append("        this(false);").println();
                out.append(indent).append("    }").println();
                out.println();
                out.append(indent).append("    public ").append(simpleName).append("(boolean xmi) {").println();
                if (extendsSupertype == null) {
                    out.append(indent).append("        this.xmi = xmi;").println();
                } else {
                    out.append(indent).append("        super(xmi);").println();
                }
                out.append(indent).append("    }").println();
                out.println();
            }
        }

        // Fields
        if (type.ddSupertype) {
            out.append(indent).append("    private final String deploymentDescriptorPath;").println();
            out.append(indent).append("    private DDParser.ComponentIDMap idMap;").println();
        }
        if (type.xmi && extendsSupertype == null) {
            out.append(indent).append("    protected final boolean xmi;").println();
        }
        if (type.rootElementModel != null && type.rootElementModel.xmiRefElementName != null) {
            out.append(indent).append("    private ").append(CrossComponentReferenceType.class.getName()).append(" xmiRef;").println();
        }

        for (ModelField field : type.fields) {
            out.append(indent).append("    ");
            if (field.privateAccess) {
                out.append("private ");
            }
            out.append(field.getJavaTypeName()).append(' ').append(field.name).append(';').println();
        }

        writeFieldsExtra(out, indent);

        // Getters
        for (ModelMethod method : type.methods) {
            out.println();
            writeInterfaceMethod(out, indent, method);
        }

        // Special methods
        if (type.ddSupertype) {
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public String getDeploymentDescriptorPath() {").println();
            out.append(indent).append("        return deploymentDescriptorPath;").println();
            out.append(indent).append("    }").println();
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public Object getComponentForId(String id) {").println();
            out.append(indent).append("        return idMap.getComponentForId(id);").println();
            out.append(indent).append("    }").println();
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public String getIdForComponent(Object ddComponent) {").println();
            out.append(indent).append("        return idMap.getIdForComponent(ddComponent);").println();
            out.append(indent).append("    }").println();
        }

        if (type.ddSupertype || type.hasRequiredNodes() || (!inner && isFinishExtraNeeded())) {
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public void finish(DDParser parser) throws DDParser.ParseException {").println();
            if (extendsSupertype != null) {
                out.append(indent).append("        super.finish(parser);").println();
            }
            for (int whichNodes = 0; whichNodes < 2; whichNodes++) {
                for (ModelNode node : whichNodes == 0 ? type.attributes : type.elements) {
                    String missingMethodName = whichNodes == 0 ? "requiredAttributeMissing" : "missingElement";
                    if (node.required) {
                        out.append(indent).append("        if (").append(node.method.field.name).append(" == null) {").println();
                        out.append(indent).append("            throw new DDParser.ParseException(parser.").append(missingMethodName)
                                        .append("(\"").append(node.name).append("\"));").println();
                        out.append(indent).append("        }").println();
                    }
                }
            }
            if (type.ddSupertype) {
                out.append(indent).append("        this.idMap = parser.idMap;").println();
            }
            if (!inner) {
                writeFinishExtra(out, indent);
            }
            out.append(indent).append("    }").println();
        }

        if (type.idAttribute || type.xmi) {
            boolean methodNeeded = true;
            if (extendsSupertype != null) {
                if (extendsSupertype.isIdAllowed()) {
                    // Supertype already has 'return true'.
                    methodNeeded = false;
                } else if (!type.idAttribute && extendsSupertype.xmi) {
                    // Supertype already has 'return xmi'.
                    methodNeeded = false;
                }
            }

            if (methodNeeded) {
                out.println();
                out.append(indent).append("    @Override").println();
                out.append(indent).append("    public boolean isIdAllowed() {").println();
                out.append(indent).append("        return ").append(type.idAttribute ? "true" : "xmi").append(';').println();
                out.append(indent).append("    }").println();
            }
        }

        // Extra methods as required
        writeExtra(out, indent);

        // handleAttribute
        out.println();
        out.append(indent).append("    @Override").println();
        out.append(indent).append("    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {").println();
        //If DDXXMIIgnoredElement and no attributes specified, just return true and ignore any possible attributes
        if (type.xmiIgnored && !type.hasAttributes()) {
            out.append(indent).append("        return true;").println();
            out.append(indent).append("    }").println();
        } else {
            if (type.hasAttributes()) {
                out.append(indent).append("        if (nsURI == null) {").println();
                for (ModelAttribute attribute : type.attributes) {
                    if (writeIfNameEquals(out, indent + "    ", type, attribute, false, "localName")) {
                        writeHandleAttribute(out, indent, type, attribute, "this");
                        out.append(indent).append("            }").println();
                    }
                }
                for (ModelElement element : type.elements) {
                    writeHandleXMIFlattenedNodes(out, indent, element, false, false, "localName");

                    if (element.xmiName == null && element.inlineAttribute != null && element.inlineAttribute.xmiName != null) {
                        out.append("            if (xmi && \"").append(element.inlineAttribute.xmiName).append("\".equals(localName)) {").println();
                        writeHandleAttribute(out, indent, type, element.inlineAttribute, "this");
                        out.append("            }").println();
                    }
                }
                out.append(indent).append("        }").println();
            }
            if (type.rootElementModel != null && type.rootElementModel.xmiRefElementName != null) {
                // Ignore xmi:version since it's unrelated to our schema versions.
                out.append(indent).append("        if (xmi && \"http://www.omg.org/XMI\".equals(nsURI)) {").println();
                out.append(indent).append("            if (\"version\".equals(localName)) {").println();
                out.append(indent).append("                // Allowed but ignored.").println();
                out.append(indent).append("                return true;").println();
                out.append(indent).append("            }").println();
                out.append(indent).append("        }").println();
            } else if (type.xmiTypes != null) {
                out.append(indent).append("        if (xmi && \"http://www.omg.org/XMI\".equals(nsURI)) {").println();
                out.append(indent).append("            if (\"type\".equals(localName)) {").println();
                out.append(indent).append("                String type = parser.getAttributeValue(index);").println();
                out.append(indent).append("                if (");
                writeIfXMITypeMatchesExpression(out, indent, "type", type);
                out.append(") {").println();
                out.append(indent).append("                    // Allowed but ignored.").println();
                out.append(indent).append("                    return true;").println();
                out.append(indent).append("                }").println();
                out.append(indent).append("            }").println();
                out.append(indent).append("        }").println();
            }
            out.append(indent).append("        return ");
            if (extendsSupertype == null) {
                out.append("false");
            } else {
                out.append("super.handleAttribute(parser, nsURI, localName, index)");
            }
            out.append(';').println();
            out.append(indent).append("    }").println();
        }

        // handleChild
        out.println();
        out.append(indent).append("    @Override").println();
        out.append(indent).append("    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {").println();
        //If DDXXMIIgnoredElement, just return true and ignore any possible child elements
        if (type.xmiIgnored) {
            out.append(indent).append("        return true;").println();
            out.append(indent).append("    }").println();
        } else {
            if (type.rootElementModel != null && type.rootElementModel.xmiRefElementName != null) {
                out.append(indent).append("        if (xmi && \"").append(type.rootElementModel.xmiRefElementName).append("\".equals(localName)) {").println();
                out.append(indent).append("            xmiRef = new ").append(CrossComponentReferenceType.class.getName())
                                .append("(\"").append(type.rootElementModel.xmiRefElementName)
                                .append("\", ").append(type.rootElementModel.xmiPrimaryDDTypeName)
                                .append(".class);").println();
                out.append(indent).append("            parser.parse(xmiRef);").println();
                out.append(indent).append("            return true;").println();
                out.append(indent).append("        }").println();
            }
            for (ModelAttribute attribute : type.attributes) {
                if (attribute.xmiNillable) {
                    writeHandleXMINillableAttributeIfNameEquals(out, indent, attribute, "this");
                }

                ModelMethod method = attribute.method;
                if (method.xmiRefField != null) {
                    String xmiRefFieldName = method.xmiRefField.name;
                    out.append(indent).append("        if (xmi && \"").append(attribute.xmiName).append("\".equals(localName)) {").println();
                    out.append(indent).append("            this.").append(xmiRefFieldName).append(" = new ").append(CrossComponentReferenceType.class.getName())
                                    .append("(\"").append(attribute.xmiName).append("\", parser.crossComponentDocumentType);").println();
                    out.append(indent).append("            parser.parse(").append(xmiRefFieldName).append(");").println();
                    if (method.xmiRefValueGetter == null) {
                        out.append(indent).append("            // The referent is unused.").println();
                    } else {
                        out.append(indent).append("            ").append(method.xmiRefReferentTypeName)
                                        .append(" referent = this.").append(xmiRefFieldName).append(".resolveReferent(parser, ").append(method.xmiRefReferentTypeName).append(".class);").println();
                        out.append(indent).append("            if (referent == null) {").println();
                        out.append(indent).append("                DDParser.unresolvedReference(\"").append(attribute.xmiName)
                                        .append("\", this.").append(xmiRefFieldName).append(".getReferenceString());").println();
                        out.append(indent).append("            } else {").println();
                        out.append(indent).append("                this.").append(method.field.name)
                                        .append(" = parser.parseString(referent.").append(method.xmiRefValueGetter).append("());").println();
                        out.append(indent).append("            }").println();
                    }
                    out.append(indent).append("            return true;").println();
                    out.append(indent).append("        }").println();
                }
            }
            for (ModelElement element : type.elements) {
                writeIfNameEquals(out, indent, type, element, true, "localName");
                writeHandleElement(out, indent, element, "this");
                out.append(indent).append("        }").println();

                writeHandleXMIFlattenedNodes(out, indent, element, true, false, "localName");
                writeHandleXMIFlattenedNodes(out, indent, element, false, true, "localName");

                if (element.xmiName == null && element.inlineAttribute != null && element.inlineAttribute.xmiNillable) {
                    writeHandleXMINillableAttributeIfNameEquals(out, indent, element.inlineAttribute, "this");
                }
            }
            if (!inner && isHandleChildExtraNeeded()) {
                writeHandleChildExtra(out, indent);
            }
            out.append(indent).append("        return ");
            if (extendsSupertype == null) {
                out.append("false");
            } else {
                if (type.xmi && !isXMISuperHandleChild()) {
                    out.append("!xmi && ");
                }
                out.append("super.handleChild(parser, localName)");
            }
            out.append(';').println();
            out.append(indent).append("    }").println();
        }

        // parseXMI${Enum}AttributeValue methods
        if (type.xmi) {
            for (ModelAttribute attribute : type.attributes) {
                ModelType attributeType = attribute.method.getType();
                if (attributeType instanceof ModelEnumType) {
                    ModelEnumType attributeEnumType = (ModelEnumType) attributeType;
                    if (attributeEnumType.hasXMIConstantName()) {
                        out.println();
                        writeParseXMIEnumAttributeValue(out, indent, (ModelEnumType) attributeType);
                    }
                }
            }
        }

        // addX methods for list fields
        for (ModelField field : type.fields) {
            if (field.listAddMethodName != null) {
                out.println();
                out.append(indent).append("    void ").append(field.listAddMethodName).append("(").append(field.type.getJavaImplTypeName())
                                .append(" ").append(field.name).append(") {").println();
                out.append(indent).append("        if (this.").append(field.name).append(" == null) {").println();
                out.append(indent).append("            this.").append(field.name)
                                .append(" = new ").append(field.getJavaImplTypeName()).append("();").println();
                out.append(indent).append("        }").println();
                out.append(indent).append("        this.").append(field.name).append(".add(").append(field.name).append(");").println();
                out.append(indent).append("    }").println();
            }
        }

        // describe
        out.println();
        out.append(indent).append("    @Override").println();
        out.append(indent).append("    public void describe(").append(getTypeName(Diagnostics.class)).append(" diag) {").println();
        if (type.rootElementModel != null && type.rootElementModel.xmiRefElementName != null) {
            out.append(indent).append("        diag.describeIfSet(\"").append(type.rootElementModel.xmiRefElementName).append("\", xmiRef);").println();
        }
        Map<ModelMethod, ModelNode> methodNodes = new HashMap<ModelMethod, ModelNode>();
        Set<ModelMethod> sharedMethods = new HashSet<ModelMethod>();
        for (int whichNodes = 0; whichNodes < 2; whichNodes++) {
            for (ModelNode node : whichNodes == 0 ? type.attributes : type.elements) {
                if (methodNodes.put(node.method, node) != null) {
                    sharedMethods.add(node.method);
                }
            }
        }
        for (int whichNodes = 0; whichNodes < 2; whichNodes++) {
            for (ModelNode node : whichNodes == 0 ? type.attributes : type.elements) {
                if (methodNodes.get(node.method) == node) {
                    String name = sharedMethods.contains(node.method) ? node.method.field.name : node.name;

                    if (node instanceof ModelElement && ((ModelElement) node).inlineAttribute != null) {
                        node = ((ModelElement) node).inlineAttribute;
                        name += "[@" + node.name + ']';
                    }

                    ModelMethod method = node.method;
                    ModelType methodType = method.getType();
                    String describe = methodType instanceof ModelEnumType ? "describeEnumIfSet" : "describeIfSet";
                    boolean xmiSpecific = (name != null && method.xmiRefField != null) ||
                                          (node instanceof ModelElement && ((ModelElement) node).xmiFlattenType != null);
                    if (xmiSpecific) {
                        out.append(indent).append("        if (xmi) {").println();
                        if (method.xmiRefField != null) {
                            out.append(indent).append("            diag.describeIfSet(\"").append(node.xmiName).append("\", ")
                                            .append(method.xmiRefField.name).append(");").println();
                        } else {
                            out.append(indent).append("            if (").append(method.field.name).append(" != null) {").println();
                            out.append(indent).append("                ").append(method.field.name).append(".describe(diag);").println();
                            out.append(indent).append("            }").println();
                        }
                        out.append(indent).append("        } else {").println();
                        out.append("    ");
                    }
                    out.append(indent).append("        diag.").append(describe).append('(');
                    if (!xmiSpecific && node.xmiName != null && !node.xmiName.equals(name)) {
                        if (name == null) {
                            out.append('"').append(node.xmiName).append('"');
                        } else {
                            out.append("xmi ? \"").append(node.xmiName).append("\" : ");
                        }
                    }
                    if (name != null) {
                        out.append('"').append(name).append("\"");
                    }
                    out.append(", ").append(method.field.name).append(");").println();
                    if (xmiSpecific) {
                        out.append(indent).append("        }").println();
                    }
                }
            }
        }
        out.append(indent).append("    }").println();
        if (type.ddSupertype) {
            out.println();
            out.append(indent).append("    @Override").println();
            out.append(indent).append("    public void describe(StringBuilder sb) {").println();
            out.append(indent).append("        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);").println();
            out.append(indent).append("        diag.describe(toTracingSafeString(), this);").println();
            out.append(indent).append("    }").println();
        }

        out.append(indent).append("}").println();
    }

    private void writeInterfaceMethod(PrintWriter out, String indent, ModelMethod method) {
        ModelType methodType = method.getType();

        ModelMethod accessorMethod = method.isSetAccessorMethod;
        if (accessorMethod == null) {
            accessorMethod = method;
        }

        out.append(indent).append("    @Override").println();
        if (methodType == ModelBasicType.ProtectedString) {
            out.append("    @com.ibm.websphere.ras.annotation.Sensitive").println();
        }
        out.append(indent).append("    public ").append(method.getJavaTypeName()).append(' ').append(method.name).append("() {").println();

        String fieldAccessorNullCheck = "";
        String fieldAccessor = accessorMethod.field.name;
        if (accessorMethod.intermediateField != null) {
            String intermediateFieldName = accessorMethod.intermediateField.name;
            fieldAccessorNullCheck = intermediateFieldName + " != null && ";
            fieldAccessor = intermediateFieldName + '.' + fieldAccessor;
        }

        if (method.isList()) {
            out.append(indent).append("        if (").append(fieldAccessorNullCheck).append(fieldAccessor).append(" != null) {").println();
            out.append(indent).append("            return ").append(fieldAccessor).append(".getList();").println();
            out.append(indent).append("        }").println();
            out.append(indent).append("        return java.util.Collections.emptyList();").println();
            out.append(indent).append("    }").println();
        } else {
            out.append(indent).append("        return ");
            if (method.xmiVersion) {
                out.append(indent).append("xmi ? \"XMI\" : ");
            }
            out.append(fieldAccessorNullCheck).append(fieldAccessor);

            String defaultValue = method.getDefaultValue();
            if (methodType instanceof ModelBasicType || defaultValue != null) {
                out.append(" != null");
                if (method.isSetAccessorMethod == null) {
                    out.append(" ? ").append(fieldAccessor);
                    if (methodType instanceof ModelBasicType) {
                        ModelBasicType methodBasicType = (ModelBasicType) methodType;
                        out.append('.').append(methodBasicType.valueMethodName).append("()");
                    }
                    out.append(" : ").append(method.getDefaultValue());
                }
            } else {
                if (method.isSetAccessorMethod != null) {
                    throw new IllegalStateException();
                }
            }

            out.append(";").println();
            out.append(indent).append("    }").println();
        }
    }

    private void writeIfXMITypeMatchesExpression(PrintWriter out, String indent, String typeVar, ModelInterfaceType type) {
        for (int i = 0; i < type.xmiTypes.size(); i++) {
            if (i != 0) {
                out.append(" || ");
            }

            String xmiType = type.xmiTypes.get(i);
            if (type.xmiTypes.size() > 1) {
                out.append('(');
            }
            out.append(typeVar).append(".endsWith(\":").append(xmiType).append("\") && \"")
                            .append(type.xmiTypeNamespace).append("\".equals(parser.getNamespaceURI(")
                            .append(typeVar).append(".substring(0, ")
                            .append(typeVar).append(".length() - \":")
                            .append(xmiType).append("\".length())))");
            if (type.xmiTypes.size() > 1) {
                out.append(')');
            }
        }
    }

    private boolean writeIfNameEquals(PrintWriter out, String indent, ModelInterfaceType type, ModelNode node, boolean element, String nameVar) {
        boolean hasXMIName = node.xmiName != null && (element || node.hasXMIAttribute());
        if (node.name == null && !hasXMIName) {
            return false;
        }

        if (hasXMIName && node.xmiName.equals(node.name)) {
            out.append(indent).append("        // \"").append(node.name).append("\" is the same for XML and XMI.").println();
        }
        out.append(indent).append("        if (");
        if (!hasXMIName) {
            if (type.xmi) {
                out.append("!xmi && ");
            }
            out.append('"').append(node.name).append('"');
        } else {
            if (node.name == null) {
                out.append("xmi && \"").append(node.xmiName).append('"');
            } else if (node.xmiName.equals(node.name)) {
                out.append('"').append(node.name).append('"');
            } else {
                out.append("(xmi ? \"").append(node.xmiName).append("\" : \"").append(node.name).append("\")");
            }
        }
        out.append(".equals(localName)) {").println();
        return true;
    }

    private void writeHandleAttribute(PrintWriter out, String indent, ModelInterfaceType type, ModelAttribute attribute, String object) {
        ModelMethod method = attribute.method;

        out.append(indent).append("                ").append(object).append('.').append(method.field.name).append(" = ");

        ModelType methodType = method.getType();
        if (methodType instanceof ModelBasicType) {
            ModelBasicType basicType = (ModelBasicType) methodType;
            String parseMethodName = method.field.listAddMethodName != null ? basicType.getParseListAttributeMethodName() : basicType.getParseAttributeMethodName();
            out.append("parser.").append(parseMethodName).append("(index)");
        } else if (methodType instanceof ModelEnumType) {
            ModelEnumType enumType = (ModelEnumType) methodType;
            boolean hasConstantName = enumType.hasConstantName();
            if (type.xmi && enumType.hasXMIConstantName()) {
                if (hasConstantName) {
                    out.append("xmi ? ");
                }
                out.append(enumType.getParseXMIAttributeValueMethodName()).append("(parser, index)");
                if (hasConstantName) {
                    out.append(" : ");
                }
            }
            if (hasConstantName) {
                out.append("parser.parseEnumAttributeValue(index, ").append(enumType.getJavaTypeName()).append(".class)");
            }
        } else {
            throw new UnsupportedOperationException(methodType.toString());
        }

        out.append(';').println();
        out.append(indent).append("                return true;").println();
    }

    private void writeParseXMIEnumAttributeValue(PrintWriter out, String indent, ModelEnumType enumType) {
        String typeName = enumType.getJavaTypeName();

        out.append(indent).append("    private static ").append(typeName)
                        .append(' ').append(enumType.getParseXMIAttributeValueMethodName())
                        .append("(DDParser parser, int index) throws DDParser.ParseException {").println();
        out.append(indent).append("        String value = parser.getAttributeValue(index);").println();

        for (ModelEnumType.Constant constant : enumType.constants) {
            out.append(indent).append("        if (\"").append(constant.xmiName).append("\".equals(value)) {").println();
            out.append(indent).append("            return ");
            if (typeName.equals(String.class.getName())) {
                out.append("value");
            } else {
                out.append(typeName).append('.').append(constant.name);
            }
            out.append(';').println();
            out.append(indent).append("        }").println();
        }

        out.append(indent).append("        throw new DDParser.ParseException(parser.invalidEnumValue(value, ");
        boolean any = false;
        for (ModelEnumType.Constant constant : enumType.constants) {
            if (any) {
                out.append(", ");
            }
            any = true;
            out.append('"').append(constant.xmiName).append('"');
        }
        out.append("));").println();

        out.append(indent).append("    }").println();
    }

    private void writeHandleElement(PrintWriter out, String indent, ModelElement element, String object) {
        ModelMethod method = element.method;
        String fieldName = method.field.name;
        String typeName = element.getType().getJavaImplTypeName();

        if (element.xmiTypes.isEmpty()) {
            if (element.inlineAttribute != null) {
                if (element.xmiName != null) {
                    throw new UnsupportedOperationException(element.toString());
                }

                method = element.inlineAttribute.method;
                fieldName = method.field.name;
                typeName = method.field.type.getJavaImplTypeName();
            }

            out.append(indent).append("            ").append(typeName).append(' ').append(fieldName)
                            .append(" = new ").append(typeName).append('(')
                            .append(element.xmiName != null ? "xmi" : "")
                            .append(");").println();

            if (element.inlineAttribute != null) {
                out.append(indent).append("            ").append(fieldName)
                                .append(".obtainValueFromAttribute(\"").append(element.inlineAttribute.name)
                                .append("\");").println();
            }
        } else {
            if (element.inlineAttribute != null) {
                throw new UnsupportedOperationException();
            }

            out.append(indent).append("            ").append(typeName).append(' ').append(fieldName).append(';').println();

            String xindent = indent;
            if (element.name != null) {
                out.append(indent).append("            if (xmi) {").println();
                xindent += "    ";
            }

            out.append(xindent).append("            String xmiType = parser.getAttributeValue(\"http://www.omg.org/XMI\", \"type\");").println();
            out.append(xindent).append("            if (xmiType == null) {").println();
            if (element.xmiDefaultType == null) {
                out.append(xindent).append("                throw new DDParser.ParseException(parser.requiredAttributeMissing(\"xmi:type\"));").println();
            } else {
                out.append(xindent).append("                ").append(fieldName)
                                .append(" = new ").append(element.xmiDefaultType.getJavaImplTypeName()).append("(true);").println();
            }
            for (ModelInterfaceType xmiType : element.xmiTypes) {
                out.append(xindent).append("            } else if (");
                writeIfXMITypeMatchesExpression(out, indent, "xmiType", xmiType);
                out.append(") {").println();
                out.append(xindent).append("                ").append(fieldName)
                                .append(" = new ").append(xmiType.getJavaImplTypeName()).append("(true);").println();
            }
            out.append(xindent).append("            } else {").println();
            out.append(xindent).append("                return false;").println();
            out.append(xindent).append("            }").println();

            if (element.name != null) {
                out.append(indent).append("            } else {").println();
                out.append(indent).append("                ").append(fieldName).append(" = new ").append(typeName).append("();").println();
                out.append(indent).append("            }").println();
            }
        }

        out.append(indent).append("            parser.parse(").append(fieldName).append(");").println();
        if (method.isList()) {
            out.append(indent).append("            ").append(object).append('.').append(method.field.listAddMethodName)
                            .append('(').append(fieldName).append(");").println();
        } else {
            out.append(indent).append("            ").append(object).append('.').append(fieldName).append(" = ").append(fieldName).append(';').println();
        }
        out.append(indent).append("            return true;").println();
    }

    private void writeHandleXMINillableAttributeIfNameEquals(PrintWriter out, String indent, ModelAttribute attribute, String object) {
        out.append(indent).append("        if (xmi && \"").append(attribute.xmiName).append("\".equals(localName)) {").println();
        writeHandleXMINillableAttribute(out, indent, attribute, "this");
        out.append(indent).append("        }").println();
    }

    /**
     * Handle a nillable XMI attribute as an element with an xsi:nil="true".
     */
    private void writeHandleXMINillableAttribute(PrintWriter out, String indent, ModelAttribute attribute, String object) {
        ModelMethod method = attribute.method;
        String fieldName = method.field.name;
        String typeName = method.getType().getJavaImplTypeName();
        out.append(indent).append("            ").append(typeName).append(' ').append(fieldName).append(" = new ").append(typeName).append("();").println();
        out.append(indent).append("            parser.parse(").append(fieldName).append(");").println();
        out.append(indent).append("            if (!").append(fieldName).append(".isNil()) {").println();
        out.append(indent).append("                ").append(object).append('.').append(fieldName).append(" = ").append(fieldName).append(';').println();
        out.append(indent).append("            }").println();
        out.append(indent).append("            return true;").println();
    }

    private void writeHandleXMIFlattenedNodes(PrintWriter out, String indent, ModelElement element, boolean elements, boolean nillable, String nameVar) {
        if (element.xmiFlattenType != null) {
            // Attribute indentation has an extra level.  writeHandleAttribute
            // already does that, so we only need to update the indentation
            // written by this method.
            String xindent = elements || nillable ? indent : indent + "    ";

            for (ModelNode node : elements ? element.xmiFlattenType.elements : element.xmiFlattenType.attributes) {
                if (node.xmiName != null && (elements || !nillable || ((ModelAttribute) node).xmiNillable)) {
                    String fieldName = element.method.field.name;
                    out.append(xindent).append("        if (xmi && \"").append(node.xmiName).append("\".equals(").append(nameVar).append(")) {").println();
                    out.append(xindent).append("            if (this.").append(fieldName).append(" == null) {").println();
                    out.append(xindent).append("                this.").append(fieldName)
                                    .append(" = new ").append(element.method.getType().getJavaImplTypeName()).append("(true);").println();
                    out.append(xindent).append("            }").println();

                    String object = "this." + fieldName;
                    if (elements) {
                        writeHandleElement(out, indent, (ModelElement) node, object);
                    } else {
                        ModelAttribute attr = (ModelAttribute) node;
                        if (nillable) {
                            writeHandleXMINillableAttribute(out, indent, attr, object);
                        } else {
                            writeHandleAttribute(out, indent, type, attr, object);
                        }
                    }

                    out.append(xindent).append("        }").println();
                }
            }
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
