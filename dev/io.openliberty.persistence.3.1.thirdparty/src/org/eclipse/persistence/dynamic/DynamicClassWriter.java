/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
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
//     dclarke, mnorman - Dynamic Persistence
//       http://wiki.eclipse.org/EclipseLink/Development/Dynamic
//       (https://bugs.eclipse.org/bugs/show_bug.cgi?id=200045)
//     dclarke - Bug 387240: added field and method calls to allow extensibility
//
package org.eclipse.persistence.dynamic;

//static imports
import static org.eclipse.persistence.internal.dynamic.DynamicPropertiesManager.PROPERTIES_MANAGER_FIELD;

//javase imports
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

//EclipseLink imports
import org.eclipse.persistence.dynamic.DynamicClassLoader.EnumInfo;
import org.eclipse.persistence.exceptions.DynamicException;
import org.eclipse.persistence.internal.dynamic.DynamicEntityImpl;
import org.eclipse.persistence.internal.dynamic.DynamicPropertiesManager;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.asm.ClassWriter;
import org.eclipse.persistence.asm.EclipseLinkASMClassWriter;
import org.eclipse.persistence.asm.MethodVisitor;
import org.eclipse.persistence.asm.Opcodes;
import org.eclipse.persistence.asm.Type;

/**
 * Write the byte codes of a dynamic entity class. The class writer will create
 * the byte codes for a dynamic class that subclasses any provided class
 * replicating its constructors and writeReplace method (if one exists).
 * <p>
 * The intent is to provide a common writer for dynamic JPA entities but also
 * allow for subclasses of this to be used in more complex writing situations
 * such as SDO and DBWS.
 * <p>
 * Instances of this class and any subclasses are maintained within the
 * {@link DynamicClassLoader#getClassWriters()} and
 * {@link DynamicClassLoader#defaultWriter} for the life of the class loader so
 * it is important that no unnecessary state be maintained that may effect
 * memory usage.
 *
 * @author dclarke, mnorman
 * @since EclipseLink 1.2
 */
public class DynamicClassWriter implements EclipseLinkClassWriter {

    /*
     * Pattern is as follows: <pre> public class Foo extends DynamicEntityImpl {
     *
     * public static DynamicPropertiesManager DPM = new
     * DynamicPropertiesManager();
     *
     * public Foo() { super(); } public DynamicPropertiesManager
     * fetchPropertiesManager() { return DPM; } }
     *
     * later on, the DPM field is populated: Field dpmField =
     * myDynamicClass.getField
     * (DynamicPropertiesManager.PROPERTIES_MANAGER_FIELD);
     * DynamicPropertiesManager dpm =
     * (DynamicPropertiesManager)dpmField.get(null); dpm.setType(...) </pre>
     */

    protected static final String DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES = DynamicPropertiesManager.class.getName().replace('.', '/');
    protected static final String INIT = "<init>";
    protected static final String CLINIT = "<clinit>";

    protected Class<?> parentClass;

    /**
     * Name of parent class. This is used only when the parent class is not
     * known at the time the dynamic class writer is registered. This is
     * generally only required when loading from an XML mapping file where the
     * order of class access is not known.
     */
    protected String parentClassName;

    private List<String> interfaces;

    public DynamicClassWriter() {
        this(DynamicEntityImpl.class);
    }

    public DynamicClassWriter(Class<?> parentClass) {
        this.parentClass = parentClass;
    }

    /**
     * Create using a loader and class name so that the parent class can be
     * lazily loaded when the writer is used to generate a dynamic class.
     * <p>
     * The loader must not be null and the parentClassName must not be null and
     * not an empty String. The parentClassName will be converted to a class
     * using the provided loader lazily.
     *
     * @see #getParentClass()
     * @see DynamicException#illegalDynamicClassWriter(DynamicClassLoader,
     *      String)
     */
    public DynamicClassWriter(String parentClassName) {
        if (parentClassName == null || parentClassName.length() == 0) {
            throw DynamicException.illegalParentClassName(parentClassName);
        }
        this.parentClassName = parentClassName;
    }

    @Override
    public Class<?> getParentClass() {
        return this.parentClass;
    }

    @Override
    public String getParentClassName() {
        return this.parentClassName;
    }

    /**
     * Return the {@link #parentClass} converting the {@link #parentClassName}
     * using the provided loader if required.
     *
     * @throws ClassNotFoundException
     *             if the parentClass is not available.
     */
    private Class<?> getParentClass(ClassLoader loader) throws ClassNotFoundException {
        if (parentClass == null && parentClassName != null) {
            parentClass = loader.loadClass(parentClassName);
        }
        return parentClass;
    }

    @Override
    public byte[] writeClass(DynamicClassLoader loader, String className) throws ClassNotFoundException {

        EnumInfo enumInfo = loader.enumInfoRegistry.get(className);
        if (enumInfo != null) {
            return createEnum(enumInfo);
        }

        Class<?> parent = getParentClass(loader);
        parentClassName = parent.getName();
        if (parent.isPrimitive() || parent.isArray() || parent.isEnum() || parent.isInterface() || Modifier.isFinal(parent.getModifiers())) {
            throw new IllegalArgumentException("Invalid parent class: " + parent);
        }
        String classNameAsSlashes = className.replace('.', '/');
        String parentClassNameAsSlashes = parentClassName.replace('.', '/');

        ClassWriter cw = new EclipseLinkASMClassWriter();

        // public class Foo extends DynamicEntityImpl {
        cw.visit(Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, classNameAsSlashes, null, parentClassNameAsSlashes, interfaces != null ? interfaces.toArray(new String[interfaces.size()]) : null);

        // public static DynamicPropertiesManager DPM = new
        // DynamicPropertiesManager();
        cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, PROPERTIES_MANAGER_FIELD, "L" + DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES + ";", null, null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, CLINIT, "()V", null, null);
        mv.visitTypeInsn(Opcodes.NEW, DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES, INIT, "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameAsSlashes, PROPERTIES_MANAGER_FIELD, "L" + DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES + ";");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);

        // public Foo() {
        // super();
        // }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, INIT, "()V", null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassNameAsSlashes, INIT, "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "fetchPropertiesManager", "()L" + DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES + ";", null, null);
        mv.visitFieldInsn(Opcodes.GETSTATIC, classNameAsSlashes, PROPERTIES_MANAGER_FIELD, "L" + DYNAMIC_PROPERTIES_MANAGER_CLASSNAME_SLASHES + ";");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);

        addFields(cw, parentClassNameAsSlashes);
        addMethods(cw, parentClassNameAsSlashes);

        cw.visitEnd();
        return cw.toByteArray();

    }

    /**
     * Allow subclasses to add additional interfaces to the dynamic entity.
     *
     * @param intf additional interface
     */
    protected void addInterface(String intf) {
        if (interfaces == null) {
            interfaces = new ArrayList<>();
        }
        interfaces.add(intf);
    }

    /**
     * Allow subclasses to add additional state to the dynamic entity.
     *
     */
    protected void addFields(ClassWriter cw, String parentClassType) {
    }

    /**
     * Allow subclasses to add additional methods to the dynamic entity.
     *
     */
    protected void addMethods(ClassWriter cw, String parentClassType) {
    }

    public static int[] ICONST = new int[] { Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 };

    protected byte[] createEnum(EnumInfo enumInfo) {

        String[] enumValues = enumInfo.getLiteralLabels();
        String className = enumInfo.getClassName();

        String internalClassName = className.replace('.', '/');

        ClassWriter cw = new EclipseLinkASMClassWriter();
        cw.visit(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER + Opcodes.ACC_ENUM, internalClassName, null, "java/lang/Enum", null);

        // Add the individual enum values
        for (String enumValue : enumValues) {
            cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC + Opcodes.ACC_ENUM, enumValue, "L" + internalClassName + ";", null, null);
        }

        // add the synthetic "$VALUES" field
        cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC, "$VALUES", "[L" + internalClassName + ";", null, null);

        // Add the "values()" method
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "values", "()[L" + internalClassName + ";", null, null);
        mv.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, "$VALUES", "[L" + internalClassName + ";");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "[L" + internalClassName + ";", "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "[L" + internalClassName + ";");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 0);

        // Add the "valueOf()" method
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + internalClassName + ";", null, null);
        mv.visitLdcInsn(Type.getType("L" + internalClassName + ";").unwrap());
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, internalClassName);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 1);

        // Add constructors
        // SignatureAttribute methodAttrs1 = new SignatureAttribute("()V");
        mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 3);

        // Add enum constants
        mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);

        int lastCount = 0;
        for (int i = 0; i < enumValues.length; i++) {
            String enumValue = enumValues[i];
            mv.visitTypeInsn(Opcodes.NEW, internalClassName);
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(enumValue);
            if (i <= 5) {
                mv.visitInsn(ICONST[i]);
            } else if (i <= 127) {
                mv.visitIntInsn(Opcodes.BIPUSH, i);
            } else {
                mv.visitIntInsn(Opcodes.SIPUSH, i);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalClassName, "<init>", "(Ljava/lang/String;I)V", false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, internalClassName, enumValue, "L" + internalClassName + ";");
            lastCount = i;
        }

        if (lastCount < 5) {
            mv.visitInsn(ICONST[lastCount + 1]);
        } else if (lastCount < 127) {
            mv.visitIntInsn(Opcodes.BIPUSH, lastCount + 1);
        } else {
            mv.visitIntInsn(Opcodes.SIPUSH, lastCount + 1);
        }
        mv.visitTypeInsn(Opcodes.ANEWARRAY, internalClassName);

        for (int i = 0; i < enumValues.length; i++) {
            String enumValue = enumValues[i];
            mv.visitInsn(Opcodes.DUP);
            if (i <= 5) {
                mv.visitInsn(ICONST[i]);
            } else if (i <= 127) {
                mv.visitIntInsn(Opcodes.BIPUSH, i);
            } else {
                mv.visitIntInsn(Opcodes.SIPUSH, i);
            }
            mv.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, enumValue, "L" + internalClassName + ";");
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitFieldInsn(Opcodes.PUTSTATIC, internalClassName, "$VALUES", "[L" + internalClassName + ";");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 0);

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Verify that the provided class meets the requirements of the writer. In
     * the case of {@link DynamicClassWriter} this will ensure that the class is
     * a subclass of the {@link #parentClass}
     *
     */
    protected boolean verify(Class<?> dynamicClass, ClassLoader loader) throws ClassNotFoundException {
        Class<?> parent = getParentClass(loader);
        return dynamicClass != null && parent.isAssignableFrom(dynamicClass);
    }

    /**
     * Interfaces the dynamic entity class implements. By default this is none
     * but in the case of SDO a concrete interface must be implemented.
     * Subclasses should override this as required.
     *
     * @return Interfaces implemented by Dynamic class. May be null
     */
    protected String[] getInterfaces() {
        return null;
    }

    /**
     * Create a copy of this {@link DynamicClassWriter} but with a different
     * parent class.
     *
     * @see DynamicClassLoader#addClass(String, Class)
     */
    protected DynamicClassWriter createCopy(Class<?> parentClass) {
        return new DynamicClassWriter(parentClass);
    }

    /**
     * Verify that the provided writer is compatible with the current writer.
     * Returning true means that the bytes that would be created using this
     * writer are identical with what would come from the provided writer.
     * <p>
     * Used in {@link DynamicClassLoader#addClass(String, EclipseLinkClassWriter)}
     * to verify if a duplicate request of the same className can proceed and
     * return the same class that may already exist.
     */
    @Override
    public boolean isCompatible(EclipseLinkClassWriter writer) {
        if (writer == null) {
            return false;
        }
        // Ensure writers are the exact same class. If subclasses do not alter
        // the bytes created then they must override this method and not return
        // false on this check.
        if (getClass() != writer.getClass()) {
            return false;
        }
        if (getParentClass() == null) {
            return getParentClassName() != null && getParentClassName().equals(writer.getParentClassName());
        }
        return getParentClass() == writer.getParentClass();
    }

    @Override
    public String toString() {
        String parentName = getParentClass() == null ? getParentClassName() : getParentClass().getName();
        return Helper.getShortClassName(getClass()) + "(" + parentName + ")";
    }
}
