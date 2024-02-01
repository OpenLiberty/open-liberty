/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1998, 2018 Hans Harz, Andrew Rustleund, IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     James Sutherland - initial impl
//     05/14/2010-2.1 Guy Pelletier
//       - 253083: Add support for dynamic persistence using ORM.xml/eclipselink-orm.xml
//     Hans Harz, Andrew Rustleund - Bug 324862 - IndexOutOfBoundsException in
//           DatabaseSessionImpl.initializeDescriptors because @MapKey Annotation is not found.
//     04/21/2011-2.3 dclarke: Upgraded to support ASM 3.3.1
//     08/10/2011-2.3 Lloyd Fernandes : Bug 336133 - Validation error during processing on parameterized generic OneToMany Entity relationship from MappedSuperclass
//     10/05/2012-2.4.1 Guy Pelletier
//       - 373092: Exceptions using generics, embedded key and entity inheritance
//     19/04/2014-2.6 Lukas Jungmann
//       - 429992: JavaSE 8/ASM 5.0.1 support (EclipseLink silently ignores Entity classes with lambda expressions)
//     11/05/2015-2.6 Dalia Abo Sheasha
//       - 480787 : Wrap several privileged method calls with a doPrivileged block
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.asm.ASMFactory;
import org.eclipse.persistence.asm.AnnotationVisitor;
import org.eclipse.persistence.asm.EclipseLinkAnnotationVisitor;
import org.eclipse.persistence.asm.Attribute;
import org.eclipse.persistence.asm.ClassReader;
import org.eclipse.persistence.asm.EclipseLinkClassVisitor;
import org.eclipse.persistence.asm.EclipseLinkFieldVisitor;
import org.eclipse.persistence.asm.EclipseLinkClassReader;
import org.eclipse.persistence.asm.FieldVisitor;
import org.eclipse.persistence.asm.EclipseLinkMethodVisitor;
import org.eclipse.persistence.asm.MethodVisitor;
import org.eclipse.persistence.asm.Type;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;

/**
 * INTERNAL: A metadata factory that uses ASM technology and no reflection
 * whatsoever to process the metadata model.
 *
 * @author James Sutherland
 * @since EclipseLink 1.2
 */
public class MetadataAsmFactory extends MetadataFactory {
    /** Set of primitive type codes. */
    public static final String PRIMITIVES = "VJIBZCSFD";
    /** Set of desc token characters. */
    public static final String TOKENS = "()<>;";

    /**
     * INTERNAL:
     */
    public MetadataAsmFactory(MetadataLogger logger, ClassLoader loader) {
        super(logger, loader);

        addMetadataClass("I", new MetadataClass(this, int.class));
        addMetadataClass("J", new MetadataClass(this, long.class));
        addMetadataClass("S", new MetadataClass(this, short.class));
        addMetadataClass("Z", new MetadataClass(this, boolean.class));
        addMetadataClass("F", new MetadataClass(this, float.class));
        addMetadataClass("D", new MetadataClass(this, double.class));
        addMetadataClass("C", new MetadataClass(this, char.class));
        addMetadataClass("B", new MetadataClass(this, byte.class));
    }

    /**
     * Build the class metadata for the class name using ASM to read the class
     * byte codes.
     */
    protected void buildClassMetadata(MetadataClass metadataClass, String className, boolean isLazy) {
        ClassMetadataVisitor visitor = new ClassMetadataVisitor(metadataClass, isLazy);
        InputStream stream = null;
        String resourceString = className.replace('.', '/') + ".class";
        boolean markSupported = false;
        try {
            stream = readResource(resourceString);
            if (markSupported = stream.markSupported()) {
                stream.mark(Integer.MAX_VALUE);
            }

            ClassReader reader = ASMFactory.createClassReader(stream);
            Attribute[] attributes = new Attribute[0];
            reader.accept(visitor, attributes, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IllegalArgumentException iae) {
            // class was probably compiled with some newer than officially
            // supported and tested JDK
            // in such case log a warning and try to re-read the class
            // without class version check
            if (stream != null) {
                if (markSupported) {
                    try {
                        stream.reset();
                    } catch (IOException e) {
                        try {
                            stream.close();
                        } catch (IOException ex) {
                            //ignore
                        }
                        stream = readResource(resourceString);
                    }
                } else {
                    try {
                        stream.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                    stream = readResource(resourceString);
                }
                try {
                    //Second argument checkClassVersion=false means, that EclipseLink ASM implementation is used
                    //as checkClassVersion is not visible by public constructor
                    ClassReader reader = ASMFactory.createClassReader(stream, false);
                    Attribute[] attributes = new Attribute[0];
                    reader.accept(visitor, attributes, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (Exception e) {
                    SessionLog log = getLogger().getSession() != null
                            ? getLogger().getSession().getSessionLog() : AbstractSessionLog.getLog();
                    // our fall-back failed, this is severe
                    if (log.shouldLog(SessionLog.SEVERE, SessionLog.METADATA)) {
                        SessionLogEntry entry = new SessionLogEntry(getLogger().getSession(), SessionLog.SEVERE, SessionLog.METADATA, e);
                        entry.setMessage(ExceptionLocalization.buildMessage("unsupported_classfile_version", new Object[] { className }));
                        log.log(entry);
                    }
                    addMetadataClass(getVirtualMetadataClass(className));
                }
            } else {
                addMetadataClass(getVirtualMetadataClass(className));
            }
        } catch (Exception exception) {
            addMetadataClass(getVirtualMetadataClass(className));
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignore) {
                // Ignore.
            }
        }
    }

    /**
     * Return the class metadata for the class name.
     */
    @Override
    public MetadataClass getMetadataClass(String className) {
        return getMetadataClass(className, false);
    }

    /**
     * Return the class metadata for the class name.
     */
    @Override
    public MetadataClass getMetadataClass(String className, boolean isLazy) {
        if (className == null) {
            return null;
        }

        MetadataClass metaClass = m_metadataClasses.get(className);
        if ((metaClass == null) || (!isLazy && metaClass.isLazy())) {
            if (metaClass != null) {
                metaClass.setIsLazy(false);
            }
            buildClassMetadata(metaClass, className, isLazy);
            metaClass = m_metadataClasses.get(className);
        }

        return metaClass;
    }

    /**
     * INTERNAL: This method resolves generic types based on the ASM class
     * metadata. Unless every other factory (e.g. APT mirror factory) respects
     * the generic format as built from ASM this method will not work since it
     * is very tied to it.
     */
    @Override
    public void resolveGenericTypes(MetadataClass child, List<String> genericTypes, MetadataClass parent, MetadataDescriptor descriptor) {
        // If we have a generic parent we need to grab our generic types
        // that may be used (and therefore need to be resolved) to map
        // accessors correctly.
        if (genericTypes != null) {
            // The generic types provided map to its parents generic types. The
            // generics also include the superclass, and interfaces. The parent
            // generics include the type and ":" and class.

            List<String> parentGenericTypes = parent.getGenericType();
            if (parentGenericTypes != null) {
                List<String> genericParentTemp = new ArrayList<>(genericTypes);
                genericParentTemp.removeAll(child.getInterfaces());

                int size = genericParentTemp.size();
                int parentIndex = 0;

                for (int index = genericTypes.indexOf(parent.getName()) + 1; index < size; index++) {
                    String actualTypeArgument = genericTypes.get(index);
                    // Ignore extra types on the end of the child, such as
                    // interface generics.
                    if (parentIndex >= parentGenericTypes.size()) {
                        break;
                    }
                    String variable = parentGenericTypes.get(parentIndex);

                    // if we get as far as the superclass name in the parent generic type list,
                    // there is nothing more to process.  We have processed all the generics in the type definition
                    if (variable.equals(parent.getSuperclassName())){
                        break;
                    }
                    parentIndex = parentIndex + 3;

                    // We are building bottom up and need to link up any
                    // TypeVariables with the actual class from the originating
                    // entity.
                    if (actualTypeArgument.length() == 1) {
                        index++;
                        actualTypeArgument = genericTypes.get(index);
                        descriptor.addGenericType(variable, descriptor.getGenericType(actualTypeArgument));
                    } else {
                        descriptor.addGenericType(variable, actualTypeArgument);
                    }
                }
            }
        }
    }

    private InputStream readResource(String name) {
        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
            return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    return m_loader.getResourceAsStream(name);
                }
            });
        } else {
            return m_loader.getResourceAsStream(name);
        }
    }

    /**
     * Walk the class byte codes and collect the class info.
     */
    public class ClassMetadataVisitor extends EclipseLinkClassVisitor {

        private boolean isLazy;
        private boolean processedMemeber;
        private MetadataClass classMetadata;

        ClassMetadataVisitor(MetadataClass metadataClass, boolean isLazy) {
            super();
            super.setCustomClassVisitor(this);
            this.isLazy = isLazy;
            this.classMetadata = metadataClass;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            String className = toClassName(name);
            if ((this.classMetadata == null) || !this.classMetadata.getName().equals(className)) {
                this.classMetadata = new MetadataClass(MetadataAsmFactory.this, className, isLazy);
                addMetadataClass(this.classMetadata);
            }
            this.classMetadata.setName(className);
            this.classMetadata.setSuperclassName(toClassName(superName));
            this.classMetadata.setModifiers(access);
            this.classMetadata.setGenericType(processDescription(signature, true));

            for (String interfaceName : interfaces) {
                this.classMetadata.addInterface(toClassName(interfaceName));
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            this.processedMemeber = true;
            if (this.classMetadata.isLazy()) {
                return null;
            }
            return new MetadataFieldVisitor(this.classMetadata, access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            this.processedMemeber = true;
            if (this.classMetadata.isLazy() || name.indexOf("init>") != -1) {
                return null;
            }
            return new MetadataMethodVisitor(this.classMetadata, access, name, signature, desc, exceptions);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            boolean isJPA = false;
            if (desc.startsWith("Lkotlin")) {
                //ignore kotlin annotations
                return null;
            }
            if (desc.startsWith("Ljava")) {
                char c = desc.charAt(5);
                //ignore annotations from 'java' namespace
                if (c == '/') {
                    return null;
                }
                //ignore annotations from other then 'javax/persistence' namespace
                if (desc.regionMatches(5, "x/", 0, 2)) {
                    if (desc.regionMatches(7, "persistence", 0, "persistence".length())) {
                        isJPA = true;
                    } else {
                        return null;
                    }
                }
            }
            if (desc.startsWith("Ljakarta")) {
                //ignore annotations from other then 'jakarta/persistence' namespace
                    if (desc.regionMatches(9, "persistence", 0, "persistence".length())) {
                        isJPA = true;
                    } else {
                        return null;
                    }
            }
            if (!this.processedMemeber && this.classMetadata.isLazy()) {
                this.classMetadata.setIsLazy(false);
            }
            //this currently forbids us to use meta-annotations defined in EclipseLink packages
            return new MetadataAnnotationVisitor(this.classMetadata, desc, isJPA || desc.startsWith("Lorg/eclipse/persistence"));
        }

        @Override
        public void visitEnd() {
            //TODO should lead into infinite loop if visitEnd() is not implemented here
        }
    }

    /**
     * {@link AnnotationVisitor} used to process class, field , and method
     * annotations populating a {@link MetadataAnnotation} and its nested state.
     *
     * @see MetadataAnnotationArrayVisitor for population of array attributes
     */
    class MetadataAnnotationVisitor extends EclipseLinkAnnotationVisitor {

        /**
         * Element the annotation is being applied to. If this is null the
         * {@link MetadataAnnotation} being constructed is a nested annotation
         * and is already referenced from its parent.
         */
        private MetadataAnnotatedElement element;

        /**
         * {@link MetadataAnnotation} being populated
         */
        private MetadataAnnotation annotation;

        MetadataAnnotationVisitor(MetadataAnnotatedElement element, String name) {
            this(element, name, true);
        }

        MetadataAnnotationVisitor(MetadataAnnotatedElement element, String name, boolean isRegular) {
            super();
            super.setCustomAnnotationVisitor(this);
            this.element = element;
            this.annotation = new MetadataAnnotation();
            this.annotation.setName(processDescription(name, false).get(0));
            this.annotation.setIsMeta(!isRegular);
        }

        public MetadataAnnotationVisitor(MetadataAnnotation annotation) {
            super();
            super.setCustomAnnotationVisitor(this);
            this.annotation = annotation;
        }

        @Override
        public void visit(String name, Object value) {
            this.annotation.addAttribute(name, annotationValue(null, value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            this.annotation.addAttribute(name, annotationValue(desc, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            MetadataAnnotation mda = new MetadataAnnotation();
            mda.setName(processDescription(desc, false).get(0));
            this.annotation.addAttribute(name, mda);
            return new MetadataAnnotationVisitor(mda);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new MetadataAnnotationArrayVisitor(this.annotation, name);
        }

        @Override
        public void visitEnd() {
            if (this.element != null) {
                if (this.annotation.isMeta()) {
                    this.element.addMetaAnnotation(this.annotation);
                } else {
                    this.element.addAnnotation(this.annotation);
                }
            }
        }
    }

    /**
     * Specialized visitor to handle the population of arrays of annotation
     * values.
     */
    class MetadataAnnotationArrayVisitor extends EclipseLinkAnnotationVisitor {

        private MetadataAnnotation annotation;

        private String attributeName;

        private List<Object> values;

        public MetadataAnnotationArrayVisitor(MetadataAnnotation annotation, String name) {
            super();
            super.setCustomAnnotationVisitor(this);
            this.annotation = annotation;
            this.attributeName = name;
            this.values = new ArrayList<Object>();
        }

        @Override
        public void visit(String name, Object value) {
            this.values.add(annotationValue(null, value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            this.values.add(annotationValue(desc, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            MetadataAnnotation mda = new MetadataAnnotation();
            mda.setName(processDescription(desc, false).get(0));
            this.values.add(mda);
            return new MetadataAnnotationVisitor(mda);
        }

        @Override
        public void visitEnd() {
            this.annotation.addAttribute(this.attributeName, this.values.toArray());
        }
    }

    /**
     * Factory for the creation of {@link MetadataField} handling basic type,
     * generics, and annotations.
     */
    class MetadataFieldVisitor extends EclipseLinkFieldVisitor {

        private MetadataField field;

        public MetadataFieldVisitor(MetadataClass classMetadata, int access, String name, String desc, String signature, Object value) {
            super();
            super.setCustomFieldVisitor(this);
            this.field = new MetadataField(classMetadata);
            this.field.setModifiers(access);
            this.field.setName(name);
            this.field.setAttributeName(name);
            this.field.setGenericType(processDescription(signature, true));
            this.field.setType(processDescription(desc, false).get(0));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.startsWith("Ljavax/persistence") || desc.startsWith("Ljakarta/persistence")
                    || desc.startsWith("Lorg/eclipse/persistence")) {
                return new MetadataAnnotationVisitor(this.field, desc);
            }
            return null;
        }

        @Override
        public void visitEnd() {
            this.field.getDeclaringClass().addField(this.field);
        }
    }

    /**
     * Factory for the creation of {@link MetadataMethod} handling basic type,
     * generics, and annotations.
     */
    // Note: Subclassed EmptyListener to minimize signature requirements for
    // ignored MethodVisitor API
    class MetadataMethodVisitor extends EclipseLinkMethodVisitor {

        private MetadataMethod method;

        public MetadataMethodVisitor(MetadataClass classMetadata, int access, String name, String desc, String signature, String[] exceptions) {
            super();
            super.setCustomMethodVisitor(this);
            this.method = new MetadataMethod(MetadataAsmFactory.this, classMetadata);

            this.method.setName(name);
            this.method.setAttributeName(Helper.getAttributeNameFromMethodName(name));
            this.method.setModifiers(access);

            this.method.setGenericType(processDescription(desc, true));

            List<String> argumentNames = processDescription(signature, false);
            if (argumentNames != null && !argumentNames.isEmpty()) {
                this.method.setReturnType(argumentNames.get(argumentNames.size() - 1));
                argumentNames.remove(argumentNames.size() - 1);
                this.method.setParameters(argumentNames);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.startsWith("Ljavax/persistence") || desc.startsWith("Ljakarta/persistence")
                    || desc.startsWith("Lorg/eclipse/persistence")) {
                return new MetadataAnnotationVisitor(this.method, desc);
            }
            return null;
        }

        // Change from 8ac1935575, Fixes ECL #1832 -- released in 4.0.2
        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttributeSuper(attr);
        }

        /**
         * At the end of visiting this method add it to the
         * {@link MetadataClass} and handle duplicate method names by chaining
         * them.
         */
        @Override
        public void visitEnd() {
            MetadataClass classMetadata = this.method.getMetadataClass();

            MetadataMethod existing = classMetadata.getMethods().get(this.method.getName());
            if (existing == null) {
                classMetadata.getMethods().put(this.method.getName(), this.method);
            } else {
                // Handle methods with the same name.
                while (existing.getNext() != null) {
                    existing = existing.getNext();
                }
                existing.setNext(this.method);
            }
        }

    }

    /**
     * Get MetadataClass for a class which can not be found
     * @param className class which has not been found
     * @return MetadataClass
     */
    private MetadataClass getVirtualMetadataClass(String className) {
        // Some basic types can't be found, so can just be registered
        // (i.e. arrays). Also, VIRTUAL classes may also not exist,
        // therefore, tag the MetadataClass as loadable false. This will be
        // used to determine if a class will be dynamically created or not.
        MetadataClass metadataClass = new MetadataClass(this, className, false);
        // If the class is a JDK class, then maybe there is a class loader
        // issues,
        // since it is a JDK class, just use reflection.
        if ((className.length() > 5) && className.substring(0, 5).equals("java.")) {
            try {
                Class<?> reflectClass = Class.forName(className);
                if (reflectClass.getSuperclass() != null) {
                    metadataClass.setSuperclassName(reflectClass.getSuperclass().getName());
                }
                for (Class<?> reflectInterface : reflectClass.getInterfaces()) {
                    metadataClass.addInterface(reflectInterface.getName());
                }
            } catch (Exception failed) {
                //ignore
                metadataClass.setIsAccessible(false);
            }
        } else {
            metadataClass.setIsAccessible(false);
        }
        return metadataClass;
    }

    /**
     * Process the byte-code argument description and return the array of Java
     * class names. i.e.
     * "(Lorg/foo/Bar;Z)Ljava/lang/Boolean;"={@literal >}[org.foo.Bar,boolean
     * ,java.lang.Boolean]
     */
    private static List<String> processDescription(String desc, boolean isGeneric) {
        if (desc == null) {
            return null;
        }
        List<String> arguments = new ArrayList<String>();
        int index = 0;
        int length = desc.length();
        boolean isGenericTyped=false;
        // PERF: Use char array to make char index faster (note this is a heavily optimized method, be very careful on changes)
        char[] chars = desc.toCharArray();
        while (index < length) {
            char next = chars[index];
            if (('(' != next) && (')' != next) && ('<' != next) && ('>' != next) && (';' != next)) {
                if (next == 'L') {
                    index++;
                    int start = index;
                    next = chars[index];
                    while (('(' != next) && (')' != next) && ('<' != next) && ('>' != next) && (';' != next)) {
                        index++;
                        next = chars[index];
                    }
                    arguments.add(toClassName(desc.substring(start, index)));
                    if(isGenericTyped) {
                        isGenericTyped=false;
                        if(next == '<') {
                            int cnt = 1;
                            while((cnt > 0) && (++index<desc.length())) {
                               switch (desc.charAt(index)) {
                                    case '<': cnt ++; break;
                                    case '>': cnt --; break;
                               }
                            }
                         }
                     }
                } else if (!isGeneric && (PRIMITIVES.indexOf(next) != -1)) {
                    // Primitives.
                    arguments.add(getPrimitiveName(next));
                } else if (next == '[') {
                    // Arrays.
                    int start = index;
                    index++;
                    next = chars[index];
                    // Nested arrays.
                    while (next == '[') {
                        index++;
                        next = chars[index];
                    }
                    if (PRIMITIVES.indexOf(next) == -1) {
                        while (next != ';') {
                            index++;
                            next = chars[index];
                        }
                        arguments.add(toClassName(desc.substring(start, index + 1)));
                    } else {
                        arguments.add(desc.substring(start, index + 1));
                    }
                } else {
                    // Is a generic type variable.
                    int start = index;
                    int end = start;

                    char myNext = next;

                    while (':' != myNext && '(' != myNext && ')' != myNext && '<' != myNext && '>' != myNext && ';' != myNext && end < length - 1) {
                        end++;
                        myNext = chars[end];
                    }

                    if (myNext == ':') {
                        arguments.add(desc.substring(start, end));
                        isGenericTyped=true;
                        index = end;
                        arguments.add(":");
                        if(desc.charAt(index+1)==':') {
                            index ++;
                        }
                    } else if (myNext == ';' && next == 'T') {
                        arguments.add(String.valueOf(next));
                        arguments.add(desc.substring(start + 1, end));
                        index = end - 1;
                    } else {
                        arguments.add(String.valueOf(next));
                    }
                }
            }
            index++;
        }
        return arguments;

    }

    /**
     * Return the Java type name for the primitive code.
     */
    private static String getPrimitiveName(char primitive) {
        if (primitive == 'V') {
            return "void";
        } else if (primitive == 'I') {
            return "int";
        } else if (primitive == 'Z') {
            return "boolean";
        } else if (primitive == 'J') {
            return "long";
        } else if (primitive == 'F') {
            return "float";
        } else if (primitive == 'D') {
            return "double";
        } else if (primitive == 'B') {
            return "byte";
        } else if (primitive == 'C') {
            return "char";
        } else if (primitive == 'S') {
            return "short";
        } else {
            return String.valueOf(primitive);
        }
    }

    private static String toClassName(String classDescription) {
        if (classDescription == null) {
            return "void";
        }
        return classDescription.replace('/', '.');
    }

    /**
     * Convert the annotation value into the value used in the meta model
     */
    private static Object annotationValue(String description, Object value) {
        Object className = Type.getTypeClassName(value);
        return (className != null) ? className: value;
    }
}
