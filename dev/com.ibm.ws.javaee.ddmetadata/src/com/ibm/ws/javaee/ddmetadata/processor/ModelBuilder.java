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
package com.ibm.ws.javaee.ddmetadata.processor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDVersion;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIEnumConstant;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIFlatten;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttributes;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredRefElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredRefElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRefElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIVersionAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyDurationType;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyModule;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyReference;
import com.ibm.ws.javaee.ddmetadata.model.Model;
import com.ibm.ws.javaee.ddmetadata.model.ModelAttribute;
import com.ibm.ws.javaee.ddmetadata.model.ModelBasicType;
import com.ibm.ws.javaee.ddmetadata.model.ModelClassType;
import com.ibm.ws.javaee.ddmetadata.model.ModelElement;
import com.ibm.ws.javaee.ddmetadata.model.ModelEnumType;
import com.ibm.ws.javaee.ddmetadata.model.ModelField;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;
import com.ibm.ws.javaee.ddmetadata.model.ModelMethod;
import com.ibm.ws.javaee.ddmetadata.model.ModelType;
import com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType;

/**
 * Builds {@link Model} and {@link ModelInterface} from annotations.
 */
public class ModelBuilder {
    private final ProcessingEnvironment processingEnv;
    public final List<Model> models = new ArrayList<Model>();
    public final Map<Name, ModelInterfaceType> interfaceTypes = new LinkedHashMap<Name, ModelInterfaceType>();

    public ModelBuilder(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void process(Set<? extends Element> elements) {
        for (Element klass : elements) {
            //           System.out.println("klass: " + klass.toString());
            DDRootElement root = klass.getAnnotation(DDRootElement.class);

            TypeElement typeElement = (TypeElement) klass;
            TypeMirror typeMirror = typeElement.asType();
            ModelInterfaceType type = getModelInterfaceType(typeMirror);
            ModelField field = new ModelField(null, type, null, false);
            ModelMethod method = new ModelMethod(null, field, null);
            ModelElement element = new ModelElement(root.name(), method, false, null);

            Model model = new Model(element, getAdapterClassName(type.interfaceName), getParserClassName(type.interfaceName));
            type.rootElementModel = model;

            Map<String, Model.Namespace> nss = new LinkedHashMap<String, Model.Namespace>();
            for (DDVersion version : root.versions()) {
                if (version.namespace().isEmpty()) {
                    throw new UnsupportedOperationException();
                }

                Model.Namespace ns = nss.get(version.namespace());
                if (ns == null) {
                    ns = new Model.Namespace(version.namespace());
                    nss.put(ns.namespace, ns);
                    model.namespaces.add(ns);
                }

                if (version.versionString().isEmpty()) {
                    throw new UnsupportedOperationException();
                }

                ns.versions.add(new Model.Namespace.Version(version.versionString(), version.version()));
            }

            DDXMIRootElement xmiRoot = klass.getAnnotation(DDXMIRootElement.class);
            if (xmiRoot != null) {
                element.xmiName = xmiRoot.name();

                model.xmiNamespace = xmiRoot.namespace();
                model.xmiVersion = xmiRoot.version();
                try {
                    model.xmiPrimaryDDTypeName = xmiRoot.primaryDDType().getName();
                } catch (MirroredTypeException e) {
                    model.xmiPrimaryDDTypeName = asElement(e.getTypeMirror()).getQualifiedName().toString();
                }
                model.xmiPrimaryDDVersions = Arrays.asList(xmiRoot.primaryDDVersions());
                model.xmiRefElementName = xmiRoot.refElementName();
            }

            models.add(model);
        }
    }

    private String replaceImplPackage(String className) {
        return className.replace(".dd.", ".ddmodel.");
    }

    private String getImplClassName(String className) {
        return replaceImplPackage(className) + "Type";
    }

    private String getAdapterClassName(String className) {
        return replaceImplPackage(className) + "Adapter";
    }

    private String getParserClassName(String className) {
        return replaceImplPackage(className) + "DDParser";
    }

    private String getFieldName(String name) {
        if (name.equals("class") || name.equals("interface")) {
            return name + '_';
        }
        return name.replace('-', '_');
    }

    private String getListAddMethodName(String name) {
        return "add" + depluralize(hyphenatedToCamelCase(name));
    }

    private ModelInterfaceType getModelInterfaceType(TypeMirror typeMirror) {
        TypeElement typeElement = asElement(typeMirror);
        Name name = typeElement.getQualifiedName();
        ModelInterfaceType type = interfaceTypes.get(name);
        if (type == null) {
            List<? extends TypeMirror> interfaceMirrors = typeElement.getInterfaces();
            List<ModelInterfaceType> supertypes = new ArrayList<ModelInterfaceType>(interfaceMirrors.size());
            boolean ddSupertype = false;
            boolean xmi = false;

            for (TypeMirror supertypeMirror : interfaceMirrors) {
                Name supertypeName = asElement(supertypeMirror).getQualifiedName();
                if (supertypeName.contentEquals(DeploymentDescriptor.class.getName())) {
                    ddSupertype = true;
                } else {
                    ModelInterfaceType supertype = getModelInterfaceType(supertypeMirror);
                    supertypes.add(supertype);
                    xmi |= supertype.xmi;
                }
            }

            List<ModelField> fields = new ArrayList<ModelField>();
            List<ModelMethod> methods = new ArrayList<ModelMethod>();
            List<ModelAttribute> attributes = new ArrayList<ModelAttribute>();
            List<ModelElement> elements = new ArrayList<ModelElement>();
            List<ModelInterfaceType> anonymousTypes = new ArrayList<ModelInterfaceType>();
            processChildren(typeMirror, fields, methods, attributes, elements, anonymousTypes);

            DDXMIRootElement xmiRoot = typeElement.getAnnotation(DDXMIRootElement.class);
            if (xmiRoot != null) {
                xmi = true;
            } else {
                for (ModelAttribute attribute : attributes) {
                    xmi |= attribute.xmiName != null | attribute.method.xmiVersion;
                }
                for (ModelElement element : elements) {
                    xmi |= element.xmiName != null;
                }
            }

            String className = name.toString();
            boolean idAttribute = typeElement.getAnnotation(DDIdAttribute.class) != null;
            type = new ModelInterfaceType(className, getImplClassName(className), supertypes, ddSupertype, fields, methods, attributes, idAttribute, elements, anonymousTypes, xmi);

            DDXMIType xmiType = typeElement.getAnnotation(DDXMIType.class);
            if (xmiType != null) {
                type.xmiTypes = Arrays.asList(xmiType.name());
                type.xmiTypeNamespace = xmiType.namespace();
            }

            if (typeElement.getAnnotation(LibertyModule.class) != null)
                type.setLibertyModule(true);

            if (typeElement.getAnnotation(LibertyNotInUse.class) != null)
                type.setLibertyNotInUse(true);

            interfaceTypes.put(name, type);
        }
        return type;
    }

    private TypeElement asElement(TypeMirror typeMirror) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
    }

    private ModelType getModelType(DDAttributeType attrType, TypeMirror mirror) {
        if (attrType != null) {
            switch (attrType) {
                case Enum: {
                    if (mirror != null) {
                        TypeElement element = asElement(mirror);
                        String name = element.getQualifiedName().toString();
                        if (name.startsWith("com.ibm.")) {
                            List<ModelEnumType.Constant> constants = new ArrayList<ModelEnumType.Constant>();
                            for (Element childElement : element.getEnclosedElements()) {
                                if (childElement.getKind() == ElementKind.ENUM_CONSTANT) {
                                    String constantName = childElement.getSimpleName().toString();
                                    DDXMIEnumConstant enumConstant = childElement.getAnnotation(DDXMIEnumConstant.class);
                                    String constantXMIName = enumConstant == null ? null : enumConstant.name();
                                    ModelEnumType.Constant c = new ModelEnumType.Constant(constantName, constantXMIName);
                                    if (childElement.getAnnotation(LibertyNotInUse.class) != null)
                                        c.setLibertyNotInUse(true);
                                    constants.add(c);
                                }
                            }

                            return new ModelEnumType(name, constants);
                        }
                    }
                    break;
                }
                case Boolean:
                    return ModelBasicType.Boolean;
                case Int:
                    return ModelBasicType.Int;
                case Long:
                    return ModelBasicType.Long;
                case String:
                    return ModelBasicType.String;
                case ProtectedString:
                    return ModelBasicType.ProtectedString;
            }
        } else if (mirror != null && mirror.getKind() == TypeKind.DECLARED) {
            TypeElement element = asElement(mirror);
            ElementKind kind = element.getKind();
            if (kind == ElementKind.INTERFACE) {
                String name = element.getQualifiedName().toString();
                if (name.startsWith("com.ibm.")) {
                    ModelInterfaceType type = getModelInterfaceType(mirror);
                    if (element.getAnnotation(LibertyNotInUse.class) != null)
                        type.setLibertyNotInUse(true);

                    return type;
                }
            }
        }

        if (mirror == null) {
            throw new UnsupportedOperationException(attrType.toString());
        }
        throw new UnsupportedOperationException(mirror.getKind() + " " + mirror.toString());
    }

    /**
     * Convert a string like "abcs" to "abc".
     */
    private static String depluralize(String s) {
        if (s.endsWith("ies")) {
            return s.substring(0, s.length() - 3) + 'y';
        }
        if (s.endsWith("s")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Convert a string like "abc-def-ghi" to "AbcDefGhi".
     */
    private static String hyphenatedToCamelCase(String s) {
        Matcher m = Pattern.compile("(?:^|-)([a-z])").matcher(s);
        StringBuilder b = new StringBuilder();
        int last = 0;
        for (; m.find(); last = m.end()) {
            b.append(s, last, m.start()).append(Character.toUpperCase(m.group(1).charAt(0)));
        }
        return b.append(s, last, s.length()).toString();
    }

    /**
     * Convert a string like "abcDef" to "AbcDef".
     */
    private static String upperCaseFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Convert a string like "AbcDef" to "abcDef".
     */
    private static String lowerCaseFirstChar(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Return a List of TypeMirror for an annotation on a member. This is a
     * workaround for JDK-6519115, which causes MirroredTypeException to be
     * thrown rather than MirroredTypesException.
     */
    private static List<TypeMirror> getAnnotationClassValues(Element member, Annotation annotation, String annotationMemberName) {
        for (AnnotationMirror annotationMirror : member.getAnnotationMirrors()) {
            if (((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(annotation.annotationType().getName())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().contentEquals(annotationMemberName)) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        List<AnnotationValue> types = (List) entry.getValue().getValue();
                        TypeMirror[] result = new TypeMirror[types.size()];
                        for (int i = 0; i < types.size(); i++) {
                            result[i] = (TypeMirror) types.get(i).getValue();
                        }
                        return Arrays.asList(result);
                    }
                }
                return Collections.emptyList();
            }
        }

        throw new IllegalStateException(annotation.annotationType().getName() + " not found in " + member);
    }

    private static class IntermediateElement {
        final String name;
        String xmiName;
        String xmiType;
        String xmiTypeNamespace;
        boolean libertyNotInUse = false;

        /**
         * Fields for the type of this element.
         */
        final List<ModelField> fields = new ArrayList<ModelField>();

        /**
         * The attributes for this element.
         */
        final List<ModelAttribute> attributes = new ArrayList<ModelAttribute>();

        /**
         * Methods for the type of the parent of this element that need to be
         * updated when the field for this element is created.
         */
        final List<ModelMethod> parentMethods = new ArrayList<ModelMethod>();

        boolean inlineRequired;

        IntermediateElement(String name) {
            this.name = name;
        }

    }

    private static class IsSetMethod {
        final ExecutableElement executable;
        final ModelMethod method;
        final String accessorMethodBaseName;

        IsSetMethod(ExecutableElement executable, ModelMethod method, String getterMethodName) {
            this.executable = executable;
            this.method = method;
            this.accessorMethodBaseName = getterMethodName;
        }
    }

    private void processChildren(TypeMirror typeMirror,
                                 List<ModelField> fields,
                                 List<ModelMethod> methods,
                                 List<ModelAttribute> attributes,
                                 List<ModelElement> elements,
                                 List<ModelInterfaceType> anonymousTypes) {
        Map<String, IntermediateElement> intermediateElements = new LinkedHashMap<String, IntermediateElement>();
        List<IsSetMethod> isSetMethods = new ArrayList<IsSetMethod>();

        TypeElement typeElement = asElement(typeMirror);
        for (Element member : typeElement.getEnclosedElements()) {
            //      System.out.println("Processing member " + member.getSimpleName() + " of type " + member.getKind());
            boolean libertyNotInUse = false;
            if (member.getAnnotation(LibertyNotInUse.class) != null) {
                libertyNotInUse = true;
            }
            if (member.getKind() == ElementKind.METHOD) {
                ExecutableElement executable = (ExecutableElement) member;
                String methodName = executable.getSimpleName().toString();

                DDAttribute attr = member.getAnnotation(DDAttribute.class);
                IntermediateElement intermediateElement = null;
                if (attr != null) {
                    List<ModelField> effectiveFields = fields;
                    List<ModelAttribute> effectiveAttributes = attributes;

                    String name = attr.name();

                    String intermediateElementName = attr.elementName();
                    if (!intermediateElementName.isEmpty()) {
                        intermediateElement = intermediateElements.get(intermediateElementName);
                        if (intermediateElement == null) {
                            intermediateElement = new IntermediateElement(intermediateElementName);
                            intermediateElement.libertyNotInUse = libertyNotInUse;
                            intermediateElements.put(intermediateElementName, intermediateElement);
                        }

                        effectiveFields = intermediateElement.fields;
                        effectiveAttributes = intermediateElement.attributes;
                    }

                    String defaultValue = attr.defaultValue();
                    if (defaultValue.isEmpty()) {
                        defaultValue = null;
                    }

                    DDXMIAttribute xmiAttr = member.getAnnotation(DDXMIAttribute.class);
                    String xmiName = xmiAttr == null ? null : xmiAttr.name();

                    ModelField childField = createModelField(name, executable, attr.type());
                    LibertyDurationType duration = member.getAnnotation(LibertyDurationType.class);
                    if (duration != null)
                        childField.setDuration(duration.timeUnit());
                    LibertyReference reference = member.getAnnotation(LibertyReference.class);
                    if (reference != null)
                        childField.setLibertyReference(reference.name());

                    effectiveFields.add(childField);

                    ModelMethod childMethod = new ModelMethod(executable.getSimpleName().toString(), childField, defaultValue);
                    methods.add(childMethod);
                    if (intermediateElement != null) {
                        intermediateElement.parentMethods.add(childMethod);
                    }

                    boolean required = attr.required();
                    ModelAttribute childAttr = new ModelAttribute(name, childMethod, required);
                    childAttr.setLibertyNotInUse(libertyNotInUse);
                    effectiveAttributes.add(childAttr);

                    if (member.getAnnotation(DDXMIVersionAttribute.class) != null) {
                        childMethod.xmiVersion = true;
                    }

                    if (xmiAttr != null) {
                        childAttr.xmiName = xmiName;
                        childAttr.xmiNillable = xmiAttr.nillable();

                        String xmiElementName = xmiAttr.elementName();
                        if (xmiElementName.isEmpty()) {
                            if (intermediateElement != null) {
                                intermediateElement.inlineRequired = true;
                            }
                        } else {
                            if (intermediateElement == null) {
                                throw new UnsupportedOperationException("XMI element " + xmiElementName +
                                                                        " specified for " + member +
                                                                        ", but no intermediate XML element specified");
                            }

                            if (intermediateElement.xmiName == null) {
                                intermediateElement.xmiName = xmiElementName;
                            } else if (!intermediateElement.xmiName.equals(xmiElementName)) {
                                throw new UnsupportedOperationException();
                            }
                        }

                        String elementXMIType = xmiAttr.elementXMIType();
                        if (!elementXMIType.isEmpty()) {
                            String elementXMITypeNamespace = xmiAttr.elementXMITypeNamespace();
                            if (elementXMITypeNamespace.isEmpty()) {
                                throw new UnsupportedOperationException();
                            }

                            if (intermediateElement.xmiType == null) {
                                intermediateElement.xmiType = elementXMIType;
                                intermediateElement.xmiTypeNamespace = elementXMITypeNamespace;
                            } else if (!intermediateElement.xmiType.equals(elementXMIType) ||
                                       !intermediateElement.xmiTypeNamespace.equals(elementXMITypeNamespace)) {
                                throw new UnsupportedOperationException();
                            }
                        }
                    }

                    DDXMIRefElement xmiRefElem = member.getAnnotation(DDXMIRefElement.class);
                    if (xmiRefElem != null) {
                        if (childAttr.xmiName != null) {
                            throw new UnsupportedOperationException();
                        }

                        String xmiRefElemName = xmiRefElem.name();

                        childAttr.method.xmiRefField = new ModelField(xmiRefElemName, new ModelClassType(CrossComponentReferenceType.class.getName()), null, true);
                        effectiveFields.add(childAttr.method.xmiRefField);

                        childAttr.xmiName = xmiRefElemName;
                        try {
                            childMethod.xmiRefReferentTypeName = xmiRefElem.referentType().getName();
                        } catch (MirroredTypeException e) {
                            childMethod.xmiRefReferentTypeName = asElement(e.getTypeMirror()).getQualifiedName().toString();
                        }
                        childMethod.xmiRefValueGetter = xmiRefElem.getter();
                    }

                    continue;
                }

                DDElement elem = member.getAnnotation(DDElement.class);
                DDChoiceElements choiceElements = member.getAnnotation(DDChoiceElements.class);
                if (elem != null || choiceElements != null) {
                    DDXMIElement xmiElem = member.getAnnotation(DDXMIElement.class);
                    String xmiName = xmiElem == null ? null : xmiElem.name();
                    boolean xmiFlatten = member.getAnnotation(DDXMIFlatten.class) != null;

                    String fieldBaseName;
                    if (elem != null) {
                        fieldBaseName = elem.name();
                    } else {
                        fieldBaseName = methodName.startsWith("get") ? lowerCaseFirstChar(methodName.substring(3)) : methodName;
                    }

                    ModelField childField = createModelField(fieldBaseName, executable, null);
                    fields.add(childField);

                    ModelMethod childMethod = new ModelMethod(methodName, childField, null);
                    methods.add(childMethod);

                    ModelElement xmiChildElement;
                    if (elem != null) {
                        String name = elem.name();
                        boolean required = elem.required();
                        ModelElement childElement = new ModelElement(name, childMethod, required, null);
                        childElement.setLibertyNotInUse(libertyNotInUse);

                        elements.add(childElement);
                        xmiChildElement = childElement;
                    } else {
                        for (DDChoiceElement choiceElement : choiceElements.value()) {
                            String name = choiceElement.name();
                            ModelInterfaceType choiceType;
                            try {
                                choiceElement.type();
                                throw new IllegalStateException();
                            } catch (MirroredTypeException e) {
                                choiceType = (ModelInterfaceType) getModelType(null, e.getTypeMirror());
                            }
                            ModelElement childElement = new ModelElement(name, childMethod, false, choiceType);

                            elements.add(childElement);
                        }

                        if (xmiName == null) {
                            xmiChildElement = null;
                        } else {
                            xmiChildElement = new ModelElement(null, childMethod, false, null);
                            elements.add(xmiChildElement);
                        }
                    }

                    if (xmiChildElement != null) {
                        xmiChildElement.xmiName = xmiName;
                        if (xmiFlatten) {
                            xmiChildElement.xmiFlattenType = (ModelInterfaceType) childField.type;
                        }

                        if (xmiElem != null) {
                            try {
                                xmiElem.defaultType();
                                throw new IllegalStateException();
                            } catch (MirroredTypeException e) {
                                if (!asElement(e.getTypeMirror()).getQualifiedName().toString().equals(Object.class.getName())) {
                                    xmiChildElement.xmiDefaultType = getModelInterfaceType(e.getTypeMirror());
                                }
                            }

                            for (TypeMirror xmiTypeMirror : getAnnotationClassValues(member, xmiElem, "types")) {
                                ModelInterfaceType xmiType = getModelInterfaceType(xmiTypeMirror);
                                if (xmiType.xmiTypes == null) {
                                    throw new UnsupportedOperationException(xmiType.interfaceName + " is not annotated @DDXMIType");
                                }
                                xmiChildElement.xmiTypes.add(xmiType);
                            }
                        }
                    }

                    continue;
                }

                if (methodName.startsWith("isSet")) {
                    ModelMethod method = new ModelMethod(methodName, null, null);
                    methods.add(method);

                    String accessorBaseMethodName = methodName.substring("isSet".length());
                    isSetMethods.add(new IsSetMethod(executable, method, accessorBaseMethodName));
                    continue;
                }

                throw new IllegalStateException("unhandled " + member + " in " + typeMirror);
            }
        }

        if (!isSetMethods.isEmpty()) {
            Map<String, ModelMethod> methodsByName = new HashMap<String, ModelMethod>();
            for (ModelMethod method : methods) {
                methodsByName.put(method.name, method);
            }

            for (IsSetMethod isSetMethod : isSetMethods) {
                String getMethodName = "get" + isSetMethod.accessorMethodBaseName;
                ModelMethod getMethod = methodsByName.get(getMethodName);
                String isMethodName = "is" + isSetMethod.accessorMethodBaseName;
                ModelMethod isMethod = methodsByName.get(isMethodName);

                ModelMethod accessorMethod;
                if (getMethod == null) {
                    if (isMethod == null) {
                        throw new IllegalStateException("missing " + getMethodName + " and " + isMethodName + " for " + isSetMethod.executable + " in " + typeMirror);
                    }
                    accessorMethod = isMethod;
                } else {
                    if (isMethod != null) {
                        throw new IllegalStateException("conflicting " + getMethodName + " and " + isMethodName + " for " + isSetMethod.executable + " in " + typeMirror);
                    }
                    accessorMethod = getMethod;
                }

                isSetMethod.method.isSetAccessorMethod = accessorMethod;
            }
        }

        for (IntermediateElement intermediateElement : intermediateElements.values()) {
            String implName = hyphenatedToCamelCase(intermediateElement.name) + "Type";
            ModelInterfaceType type = new ModelInterfaceType(null, implName, Collections.<ModelInterfaceType> emptyList(), false, intermediateElement.fields, Collections.<ModelMethod> emptyList(), intermediateElement.attributes, false, Collections.<ModelElement> emptyList(), Collections.<ModelInterfaceType> emptyList(), intermediateElement.xmiName != null);
            ModelField field = new ModelField(getFieldName(intermediateElement.name), type, null, false);
            ModelMethod method = new ModelMethod(null, field, null);
            ModelElement element = new ModelElement(intermediateElement.name, method, false, null);

            element.setLibertyNotInUse(intermediateElement.libertyNotInUse);

            if (intermediateElement.xmiName != null) {
                element.xmiName = intermediateElement.xmiName;
                type.xmiTypes = Arrays.asList(intermediateElement.xmiType);
                type.xmiTypeNamespace = intermediateElement.xmiTypeNamespace;
            }

            if (intermediateElement.attributes.size() == 1) {
                // There's only one attribute/field, so the handling can be
                // "inlined" without needing an inner class.
                element.inlineAttribute = intermediateElement.attributes.get(0);
                ModelField singleField = intermediateElement.fields.get(0);

                // The field name should be qualified with the element name to
                // avoid conflicts.
                String fieldBaseName = intermediateElement.name + '-' + element.inlineAttribute.name;
                singleField.name = getFieldName(fieldBaseName);
                if (singleField.listAddMethodName != null) {
                    singleField.listAddMethodName = getListAddMethodName(fieldBaseName);
                }

                fields.add(singleField);
            } else {
                if (intermediateElement.inlineRequired) {
                    throw new UnsupportedOperationException();
                }

                // Update all the methods in the parent element to use the field for
                // this element.
                for (ModelMethod parentMethod : intermediateElement.parentMethods) {
                    parentMethod.intermediateField = field;
                }

                fields.add(field);
                anonymousTypes.add(type);
            }

            elements.add(element);
        }

        DDXMIIgnoredElements xmiIgnoredElements = typeElement.getAnnotation(DDXMIIgnoredElements.class);
        if (xmiIgnoredElements != null) {
            for (DDXMIIgnoredElement xmiIgnoredElement : xmiIgnoredElements.value()) {
                ModelElement element = processXMIIgnoredElement(xmiIgnoredElement, xmiIgnoredElement.list());
                // The data in ignored elements is kept for diagnostics.
                fields.add(element.method.field);
                elements.add(element);
                anonymousTypes.add((ModelInterfaceType) element.method.field.type);
            }
        }

        DDXMIIgnoredRefElements xmiIgnoredRefElements = typeElement.getAnnotation(DDXMIIgnoredRefElements.class);
        if (xmiIgnoredRefElements != null) {
            for (DDXMIIgnoredRefElement xmiIgnoredRefElement : xmiIgnoredRefElements.value()) {
                processXMIIgnoredRefElement(xmiIgnoredRefElement, fields, attributes);
            }
        }

        DDXMIIgnoredAttributes xmiIgnoredAttributes = typeElement.getAnnotation(DDXMIIgnoredAttributes.class);
        if (xmiIgnoredAttributes != null) {
            for (DDXMIIgnoredAttribute xmiIgnoredAttribute : xmiIgnoredAttributes.value()) {
                processXMIIgnoredAttribute(xmiIgnoredAttribute, fields, attributes);
            }
        }
    }

    private ModelField createModelField(String fieldBaseName, ExecutableElement executable, DDAttributeType attrType) {
        TypeMirror typeMirror = executable.getReturnType();
        boolean list = false;

        // Unwrap List<X> if needed.
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            Element typeElement = declaredType.asElement();
            if (typeElement.getKind() == ElementKind.INTERFACE) {
                Name typeElementName = ((TypeElement) typeElement).getQualifiedName();
                if (typeElementName.contentEquals(List.class.getName())) {
                    List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
                    if (!typeArgs.isEmpty()) {
                        typeMirror = typeArgs.get(0);
                        list = true;
                    }
                }
            }
        }

        ModelType type = getModelType(attrType, typeMirror);
        String fieldName = getFieldName(fieldBaseName);
        String listAddMethodName = list ? getListAddMethodName(fieldBaseName) : null;

        return new ModelField(fieldName, type, listAddMethodName, false);
    }

    private ModelElement processXMIIgnoredElement(DDXMIIgnoredElement xmiIgnoredElement, boolean list) {
        String name = xmiIgnoredElement.name();

        List<ModelField> fields = new ArrayList<ModelField>();
        List<ModelMethod> methods = new ArrayList<ModelMethod>();

        List<ModelAttribute> attributes = new ArrayList<ModelAttribute>();
        for (DDXMIIgnoredAttribute ignoredAttribute : xmiIgnoredElement.attributes()) {
            processXMIIgnoredAttribute(ignoredAttribute, fields, attributes);
        }

        List<ModelElement> elements = new ArrayList<ModelElement>();
        for (DDXMIIgnoredRefElement xmiRefElem : xmiIgnoredElement.refElements()) {
            processXMIIgnoredRefElement(xmiRefElem, fields, attributes);
        }

        String implName = upperCaseFirstChar(name);
        if (xmiIgnoredElement.list() && implName.endsWith("s")) {
            implName = implName.substring(0, implName.length() - 1);
        }
        implName += "XMIIgnoredType";

        ModelInterfaceType type = new ModelInterfaceType(null, implName, Collections.<ModelInterfaceType> emptyList(), false, fields, methods, attributes, false, elements, Collections.<ModelInterfaceType> emptyList(), true);

        type.xmiIgnored = true;
        String listAddMethodName = list ? getListAddMethodName(name) : null;
        ModelField field = new ModelField(name, type, listAddMethodName, false);
        ModelMethod method = new ModelMethod(null, field, null);
        ModelElement element = new ModelElement(null, method, false, null);
        element.xmiName = name;

        // XMI ignored elements are not used in Liberty
        element.setLibertyNotInUse(true);
        return element;
    }

    private void processXMIIgnoredAttribute(DDXMIIgnoredAttribute ignoredAttribute, List<ModelField> fields, List<ModelAttribute> attributes) {
        String attrName = ignoredAttribute.name();

        ModelType childType = getXMIIgnoredAttributeModelType(ignoredAttribute);
        ModelField childField = new ModelField(attrName, childType, null, false);

        ModelMethod childMethod = new ModelMethod(null, childField, null);

        ModelAttribute childAttr = new ModelAttribute(null, childMethod, false);
        childAttr.xmiName = attrName;
        childAttr.xmiNillable = ignoredAttribute.nillable();
        // XMI ignored attributes are not in use in Liberty
        childAttr.setLibertyNotInUse(true);

        fields.add(childField);
        attributes.add(childAttr);
    }

    private ModelType getXMIIgnoredAttributeModelType(DDXMIIgnoredAttribute ignoredAttribute) {
        DDAttributeType attrType = ignoredAttribute.type();
        String[] enumConstantNames = ignoredAttribute.enumConstants();

        if (attrType == DDAttributeType.Enum) {
            List<ModelEnumType.Constant> constants = new ArrayList<ModelEnumType.Constant>();
            for (String constantName : enumConstantNames) {
                constants.add(new ModelEnumType.Constant(null, constantName));
            }
            return new ModelEnumType(String.class.getName(), constants);
        }

        if (enumConstantNames.length != 0) {
            throw new UnsupportedOperationException();
        }
        return getModelType(attrType, null);
    }

    private void processXMIIgnoredRefElement(DDXMIIgnoredRefElement xmiRefElem, List<ModelField> fields, List<ModelAttribute> attributes) {
        String elementName = xmiRefElem.name();

        ModelField childField = new ModelField(elementName, ModelBasicType.String, null, false);

        ModelMethod childMethod = new ModelMethod(null, childField, null);
        childMethod.xmiRefField = new ModelField(elementName, new ModelClassType(CrossComponentReferenceType.class.getName()), null, true);
        childMethod.xmiRefReferentTypeName = "";

        ModelAttribute childAttr = new ModelAttribute(null, childMethod, false);
        childAttr.xmiName = elementName;

        fields.add(childMethod.xmiRefField);
        attributes.add(childAttr);
    }
}
