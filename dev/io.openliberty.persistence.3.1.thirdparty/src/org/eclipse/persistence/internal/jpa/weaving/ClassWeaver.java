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
//     Oracle - initial API and implementation from Oracle TopLink
//     dclarke Bug 244124: Enhanced weaving to support extended FetchGroup functionality
//     08/23/2010-2.2 Michael O'Brien
//        - 323043: application.xml module ordering may cause weaving not to occur causing an NPE.
//                       warn if expected "_persistence_//_vh" method not found
//                       instead of throwing NPE during deploy validation.
//     19/04/2014-2.6 Lukas Jungmann
//       - 429992: JavaSE 8/ASM 5.0.1 support (EclipseLink silently ignores Entity classes with lambda expressions)
package org.eclipse.persistence.internal.jpa.weaving;

import java.util.Iterator;

import org.eclipse.persistence.asm.ASMFactory;
import org.eclipse.persistence.asm.AnnotationVisitor;
import org.eclipse.persistence.asm.ClassVisitor;
import org.eclipse.persistence.asm.FieldVisitor;
import org.eclipse.persistence.asm.Label;
import org.eclipse.persistence.asm.MethodVisitor;
import org.eclipse.persistence.asm.Opcodes;
import org.eclipse.persistence.asm.Type;
import org.eclipse.persistence.internal.helper.Helper;

/**
 * INTERNAL: Weaves classes to allow them to support EclipseLink indirection.
 * Classes are weaved to add a variable of type ValueHolderInterface for each
 * attribute that uses indirection. In addition, access methods are added for
 * the new variable. Also, triggers the process of weaving the methods of the
 * class.
 *
 * @see org.eclipse.persistence.internal.jpa.weaving.MethodWeaver
 */

public class ClassWeaver extends ClassVisitor {

    // PersistenceWeaved
    public static final String PERSISTENCE_WEAVED_SHORT_SIGNATURE = "org/eclipse/persistence/internal/weaving/PersistenceWeaved";

    // ValueHolders
    public static final String TW_LAZY_SHORT_SIGNATURE = "org/eclipse/persistence/internal/weaving/PersistenceWeavedLazy";
    public static final String VHI_CLASSNAME = "org.eclipse.persistence.indirection.WeavedAttributeValueHolderInterface";
    public static final String VH_SHORT_SIGNATURE = "org/eclipse/persistence/indirection/ValueHolder";
    public static final String VHI_SHORT_SIGNATURE = "org/eclipse/persistence/indirection/WeavedAttributeValueHolderInterface";
    public static final String VHI_SIGNATURE = "L" + VHI_SHORT_SIGNATURE + ";";

    // Change tracking
    public static final String TW_CT_SHORT_SIGNATURE = "org/eclipse/persistence/internal/weaving/PersistenceWeavedChangeTracking";
    public static final String PCL_SHORT_SIGNATURE = "java/beans/PropertyChangeListener";
    public static final String PCL_SIGNATURE = "L" + PCL_SHORT_SIGNATURE + ";";
    public static final String CT_SHORT_SIGNATURE = "org/eclipse/persistence/descriptors/changetracking/ChangeTracker";
    public static final String PCE_SHORT_SIGNATURE = "java/beans/PropertyChangeEvent";
    public static final String PCE_SIGNATURE = "L" + PCE_SHORT_SIGNATURE + ";";

    // PersistenceEntity
    public static final String PERSISTENCE_ENTITY_SHORT_SIGNATURE = "org/eclipse/persistence/internal/descriptors/PersistenceEntity";
    public static final String PERSISTENCE_OBJECT_SHORT_SIGNATURE = "org/eclipse/persistence/internal/descriptors/PersistenceObject";
    public static final String PERSISTENCE_OBJECT_SIGNATURE = "L" + PERSISTENCE_OBJECT_SHORT_SIGNATURE + ";";
    public static final String VECTOR_SIGNATURE = "Ljava/util/Vector;";
    public static final String OBJECT_SIGNATURE = "Ljava/lang/Object;";
    public static final String STRING_SIGNATURE = "Ljava/lang/String;";
    public static final String CACHEKEY_SIGNATURE = "Lorg/eclipse/persistence/internal/identitymaps/CacheKey;";

    // Fetch groups
    public static final String WEAVED_FETCHGROUPS_SHORT_SIGNATURE = "org/eclipse/persistence/internal/weaving/PersistenceWeavedFetchGroups";
    public static final String FETCHGROUP_TRACKER_SIGNATURE = "Lorg/eclipse/persistence/queries/FetchGroupTracker;";
    public static final String FETCHGROUP_TRACKER_SHORT_SIGNATURE = "org/eclipse/persistence/queries/FetchGroupTracker";
    public static final String FETCHGROUP_SHORT_SIGNATURE = "org/eclipse/persistence/queries/FetchGroup";
    public static final String FETCHGROUP_SIGNATURE = "Lorg/eclipse/persistence/queries/FetchGroup;";
    public static final String SESSION_SIGNATURE = "Lorg/eclipse/persistence/sessions/Session;";
    public static final String ENTITY_MANAGER_IMPL_SHORT_SIGNATURE = "org/eclipse/persistence/internal/jpa/EntityManagerImpl";
    public static final String PBOOLEAN_SIGNATURE = "Z";
    public static final String LONG_SIGNATURE = "J";

    // REST
    public static final String WEAVED_REST_LAZY_SHORT_SIGNATURE = "org/eclipse/persistence/internal/jpa/rs/weaving/PersistenceWeavedRest";
    public static final String LIST_RELATIONSHIP_INFO_SIGNATURE = "Ljava/util/List;";
    public static final String LIST_RELATIONSHIP_INFO_GENERIC_SIGNATURE = "Ljava/util/List<Lorg/eclipse/persistence/internal/jpa/rs/weaving/RelationshipInfo;>;";
    public static final String LINK_SIGNATURE = "Lorg/eclipse/persistence/internal/jpa/rs/metadata/model/Link;";
    public static final String ITEM_LINKS_SIGNATURE = "Lorg/eclipse/persistence/internal/jpa/rs/metadata/model/ItemLinks;";

    // Cloneable
    public static final String CLONEABLE_SHORT_SIGNATURE = "java/lang/Cloneable";

    // Transient
    public static final String JPA_TRANSIENT_DESCRIPTION = "Ljakarta/persistence/Transient;";
    public static final String XML_TRANSIENT_DESCRIPTION = "Ljakarta/xml/bind/annotation/XmlTransient;";

    public static final String PERSISTENCE_SET = Helper.PERSISTENCE_SET;
    public static final String PERSISTENCE_GET = Helper.PERSISTENCE_GET;

    // 323403: These constants are used to search for missing weaved functions -
    // a copy is in the foundation project under internal.Helper
    public static final String PERSISTENCE_FIELDNAME_PREFIX = "_persistence_";
    public static final String PERSISTENCE_FIELDNAME_POSTFIX = "_vh";

    public static final String VIRTUAL_GETTER_SIGNATURE = "(" + ClassWeaver.STRING_SIGNATURE + ")" + ClassWeaver.OBJECT_SIGNATURE;
    public static final String VIRTUAL_SETTER_SIGNATURE = "(" + ClassWeaver.STRING_SIGNATURE + ClassWeaver.OBJECT_SIGNATURE + ")" + ClassWeaver.OBJECT_SIGNATURE;

    /** Store if JAXB is on the classpath, true in Java SE 6 - 8, maybe true from 9 */
    private static Boolean isJAXBOnPath = null;

    /**
     * Stores information on the class gathered from the temp class loader and
     * descriptor.
     */
    protected ClassDetails classDetails;

    // Keep track of what was weaved.
    protected boolean alreadyWeaved = false;
    public boolean weaved = false;
    public boolean weavedLazy = false;
    public boolean weavedPersistenceEntity = false;
    public boolean weavedChangeTracker = false;
    public boolean weavedFetchGroups = false;
    public boolean weavedRest = false;

    private ClassVisitor cw;

    /**
     * Used for primitive conversion. Returns the name of the class that wraps a
     * given type.
     */
    public static String wrapperFor(int sort) {
        if (sort == Type.BOOLEAN) {
            return "java/lang/Boolean";
        } else if (sort == Type.BYTE) {
            return "java/lang/Byte";
        } else if (sort == Type.CHAR) {
            return "java/lang/Character";
        } else if (sort == Type.SHORT) {
            return "java/lang/Short";
        } else if (sort == Type.INT) {
            return "java/lang/Integer";
        } else if (sort == Type.FLOAT) {
            return "java/lang/Float";
        } else if (sort == Type.LONG) {
            return "java/lang/Long";
        } else if (sort == Type.DOUBLE) {
            return "java/lang/Double";
        }
        return null;
    }

    /**
     * Used for primitive conversion. Returns the name conversion method for the
     * given type.
     */
    public static void unwrapPrimitive(AttributeDetails attribute, MethodVisitor visitor) {
        String wrapper = wrapperFor(attribute.getReferenceClassType().getSort());
        int sort = attribute.getReferenceClassType().getSort();
        if (sort == Type.BOOLEAN) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "booleanValue", "()Z", false);
            return;
        } else if (sort == Type.BYTE) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "byteValue", "()B", false);
            return;
        } else if (sort == Type.CHAR) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "charValue", "()C", false);
            return;
        } else if (sort == Type.SHORT) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "shortValue", "()S", false);
            return;
        } else if (sort == Type.INT) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "intValue", "()I", false);
            return;
        } else if (sort == Type.FLOAT) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "floatValue", "()F", false);
            return;
        } else if (sort == Type.LONG) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "longValue", "()J", false);
            return;
        } else if (sort == Type.DOUBLE) {
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, "doubleValue", "()D", false);
            return;
        }
    }

    /**
     * Return the get method name weaved for a value-holder attribute.
     */
    public static String getWeavedValueHolderGetMethodName(String attributeName) {
        return Helper.getWeavedValueHolderGetMethodName(attributeName);
    }

    /**
     * Return the set method name weaved for a value-holder attribute.
     */
    public static String getWeavedValueHolderSetMethodName(String attributeName) {
        return Helper.getWeavedValueHolderSetMethodName(attributeName);
    }

    /**
     * Return if the JAXB classes are on the classpath (if they are the
     * XmlTransient annotation is added).
     */
    public static boolean isJAXBOnPath() {
        if (isJAXBOnPath == null) {
            try {
                Class.forName("jakarta.xml.bind.annotation.XmlTransient");
                isJAXBOnPath = true;
            } catch (Exception notThere) {
                isJAXBOnPath = false;
            }
        }
        return isJAXBOnPath;
    }

    public ClassWeaver(ClassVisitor classWriter, ClassDetails classDetails) {
        super(ASMFactory.ASM_API_SELECTED, classWriter);
        super.setCustomClassVisitor(this);
        this.cw = classWriter;
        this.classDetails = classDetails;
    }

    /**
     * Add a variable of type ValueHolderInterface to the class. When this
     * method has been run, the class will contain a variable declaration
     * similar to the following:
     *
     * private ValueHolderInterface _persistence_variableName_vh;
     */
    public void addValueHolder(AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();
        FieldVisitor fv = cv.visitField(Opcodes.ACC_PROTECTED, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE, null, null);

        // only mark @Transient if this is property access. Otherwise, the
        // @Transient annotation could mistakenly
        // cause the class to use attribute access.
        if (attributeDetails.getGetterMethodName() == null || attributeDetails.getGetterMethodName().equals("") || attributeDetails.weaveTransientFieldValueHolders()) {
            fv.visitAnnotation(JPA_TRANSIENT_DESCRIPTION, true).visitEnd();
            if (isJAXBOnPath()) {
                fv.visitAnnotation(XML_TRANSIENT_DESCRIPTION, true).visitEnd();
            }
        }
        fv.visitEnd();
    }

    /**
     * Add a variable of type PropertyChangeListener to the class. When this
     * method has been run, the class will contain a variable declaration
     * similar to the following
     *
     * private transient _persistence_listener;
     */
    public void addPropertyChangeListener(boolean attributeAccess) {
        cv.visitField(Opcodes.ACC_PROTECTED + Opcodes.ACC_TRANSIENT, "_persistence_listener", PCL_SIGNATURE, null, null);
    }

    /**
     * Add the implementation of the changeTracker_getPropertyChangeListener
     * method to the class. The result is a method that looks as follows:
     *
     * public PropertyChangeListener _persistence_getPropertyChangeListener() {
     * return _persistence_listener; }
     */
    public void addGetPropertyChangeListener(ClassDetails classDetails) {
        MethodVisitor cv_getPCL = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_getPropertyChangeListener", "()" + PCL_SIGNATURE, null, null);
        cv_getPCL.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getPCL.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_listener", PCL_SIGNATURE);
        cv_getPCL.visitInsn(Opcodes.ARETURN);
        cv_getPCL.visitMaxs(0, 0);
    }

    /**
     * Add the implementation of the changeTracker_setPropertyChangeListener
     * method to the class. The result is a method that looks as follows:
     *
     * public void _persistence_setPropertyChangeListener(PropertyChangeListener
     * propertychangelistener){ _persistence_listener = propertychangelistener;
     * }
     */
    public void addSetPropertyChangeListener(ClassDetails classDetails) {
        MethodVisitor cv_setPCL = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setPropertyChangeListener", "(" + PCL_SIGNATURE + ")V", null, null);
        cv_setPCL.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setPCL.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setPCL.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_listener", PCL_SIGNATURE);
        cv_setPCL.visitInsn(Opcodes.RETURN);
        cv_setPCL.visitMaxs(0, 0);
    }

    /**
     * Add a method to track property changes. The method will look as follows:
     *
     * public void _toplink_propertyChange(String s, Object obj, Object obj1){
     * if(_persistence_listener != null {@literal &&} obj != obj1){
     * _persistence_listener.propertyChange(new PropertyChangeEvent(this, s,
     * obj, obj1)); } }
     */
    public void addPropertyChange(ClassDetails classDetails) {
        // create the _toplink_propertyChange() method
        MethodVisitor cv_addPC = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_propertyChange", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", null, null);

        // if (_toplink_Listener != null)
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 0);
        cv_addPC.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_listener", PCL_SIGNATURE);
        Label l0 = ASMFactory.createLabel();
        cv_addPC.visitJumpInsn(Opcodes.IFNULL, l0);

        // if (obj != obj1)
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 2);
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 3);
        cv_addPC.visitJumpInsn(Opcodes.IF_ACMPEQ, l0);

        // _toplink_listener.propertyChange(...);
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 0);
        cv_addPC.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_listener", PCL_SIGNATURE);
        cv_addPC.visitTypeInsn(Opcodes.NEW, PCE_SHORT_SIGNATURE);
        cv_addPC.visitInsn(Opcodes.DUP);

        // new PropertyChangeEvent(this, s, obj, obj1)
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 0);
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 1);
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 2);
        cv_addPC.visitVarInsn(Opcodes.ALOAD, 3);
        cv_addPC.visitMethodInsn(Opcodes.INVOKESPECIAL, PCE_SHORT_SIGNATURE, "<init>", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false);
        cv_addPC.visitMethodInsn(Opcodes.INVOKEINTERFACE, PCL_SHORT_SIGNATURE, "propertyChange", "(" + PCE_SIGNATURE + ")V", true);

        // }
        cv_addPC.visitLabel(l0);

        cv_addPC.visitInsn(Opcodes.RETURN);
        cv_addPC.visitMaxs(0, 0);
    }

    /**
     * Add a method that allows us to lazily initialize a valueholder we have
     * woven in This allows us to avoid initializing valueholders in the
     * constructor.
     *
     * protected void _persistence_initialize_attribute_vh(){
     * if(_persistence_attribute_vh == null){ _persistence_attribute_vh = new
     * ValueHolder(this.attribute); // or new ValueHolder() if property access.
     * _persistence_attribute_vh.setIsNewlyWeavedValueHolder(true); } }
     */
    public void addInitializerForValueHolder(ClassDetails classDetails, AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();
        String className = classDetails.getClassName();

        // Create a getter method for the new valueholder
        // protected void _persistence_initialize_attribute_vh(){
        MethodVisitor cv_init_VH = cv.visitMethod(Opcodes.ACC_PROTECTED, "_persistence_initialize_" + attribute + PERSISTENCE_FIELDNAME_POSTFIX, "()V", null, null);

        // if(_persistence_attribute_vh == null){
        cv_init_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_init_VH.visitFieldInsn(Opcodes.GETFIELD, className, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        Label l0 = ASMFactory.createLabel();
        cv_init_VH.visitJumpInsn(Opcodes.IFNONNULL, l0);

        // _persistence_attribute_vh = new ValueHolder(this.attribute);
        cv_init_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_init_VH.visitTypeInsn(Opcodes.NEW, VH_SHORT_SIGNATURE);
        cv_init_VH.visitInsn(Opcodes.DUP);
        if (attributeDetails.hasField()) {
            cv_init_VH.visitVarInsn(Opcodes.ALOAD, 0);
            cv_init_VH.visitFieldInsn(Opcodes.GETFIELD, className, attribute, attributeDetails.getReferenceClassType().getDescriptor());
            cv_init_VH.visitMethodInsn(Opcodes.INVOKESPECIAL, VH_SHORT_SIGNATURE, "<init>", "(Ljava/lang/Object;)V", false);
        } else {
            cv_init_VH.visitMethodInsn(Opcodes.INVOKESPECIAL, VH_SHORT_SIGNATURE, "<init>", "()V", false);
        }
        cv_init_VH.visitFieldInsn(Opcodes.PUTFIELD, className, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);

        // _persistence_attribute_vh.setIsNewlyWeavedValueHolder(true);
        cv_init_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_init_VH.visitFieldInsn(Opcodes.GETFIELD, className, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        cv_init_VH.visitInsn(Opcodes.ICONST_1);
        cv_init_VH.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "setIsNewlyWeavedValueHolder", "(Z)V", true);

        // }
        cv_init_VH.visitLabel(l0);

        cv_init_VH.visitInsn(Opcodes.RETURN);
        cv_init_VH.visitMaxs(0, 0);
    }

    /**
     * Add a get method for the newly added valueholder. Adds a method of the
     * following form:
     *
     * public WeavedAttributeValueHolderInterface _persistence_getfoo_vh(){
     * _persistence_initialize_attributeName_vh(); if
     * (_persistence_vh.isCoordinatedWithProperty() ||
     * _persistence_foo_vh.isNewlyWeavedValueHolder()){ EntityC object =
     * (EntityC)getFoo(); if (object != _persistence_foo_vh.getValue()){
     * setFoo(object); } } return _persistence_foo_vh; }
     */
    public void addGetterMethodForValueHolder(ClassDetails classDetails, AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();
        String className = classDetails.getClassName();
        // Create a getter method for the new valueholder
        MethodVisitor cv_get_VH = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_GET + attribute + PERSISTENCE_FIELDNAME_POSTFIX, "()" + VHI_SIGNATURE, null, null);

        // _persistence_initialize_attributeName_vh();
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_initialize_" + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

        // if (_toplink_foo_vh.isCoordinatedWithProperty() ||
        // _toplink_foo_vh.isNewlyWeavedValueHolder()){
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        cv_get_VH.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "isCoordinatedWithProperty", "()Z", true);
        Label l0 = ASMFactory.createLabel();
        cv_get_VH.visitJumpInsn(Opcodes.IFNE, l0);
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        cv_get_VH.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "isNewlyWeavedValueHolder", "()Z", true);
        Label l1 = ASMFactory.createLabel();
        cv_get_VH.visitJumpInsn(Opcodes.IFEQ, l1);
        cv_get_VH.visitLabel(l0);
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);

        // EntityC object = (EntityC)getFoo();
        if (attributeDetails.getGetterMethodName() != null) {
            cv_get_VH.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), attributeDetails.getGetterMethodName(), "()L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";", false);
            cv_get_VH.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));
        } else {
            cv_get_VH.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), PERSISTENCE_GET + attributeDetails.attributeName, "()L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";", false);
        }
        cv_get_VH.visitVarInsn(Opcodes.ASTORE, 1);

        // if (object != _toplink_foo_vh.getValue()){
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 1);
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        cv_get_VH.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);
        cv_get_VH.visitJumpInsn(Opcodes.IF_ACMPEQ, l1);

        // setFoo(object);
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 1);
        if (attributeDetails.getSetterMethodName() != null) {
            cv_get_VH.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), attributeDetails.getSetterMethodName(), "(L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";)V", false);
        } else {
            cv_get_VH.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), PERSISTENCE_SET + attributeDetails.getAttributeName(), "(L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";)V", false);
        }

        // }
        cv_get_VH.visitLabel(l1);

        // return _toplink_foo_vh;
        cv_get_VH.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get_VH.visitFieldInsn(Opcodes.GETFIELD, className, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);
        cv_get_VH.visitInsn(Opcodes.ARETURN);

        cv_get_VH.visitMaxs(0, 0);
    }

    /**
     * Add a set method for the newly added ValueHolder. Adds a method of this
     * form:
     *
     * public void _persistence_setfoo_vh(WeavedAttributeValueHolderInterface
     * valueholderinterface){ _persistence_foo_vh = valueholderinterface; if
     * (valueholderinterface.isInstantiated()){ Object object = getFoo(); Object
     * value = valueholderinterface.getValue(); if (object != value){
     * setFoo((EntityC)value); } } else { foo = null; } }
     */
    public void addSetterMethodForValueHolder(ClassDetails classDetails, AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();
        String className = classDetails.getClassName();
        // create a setter method for the new valueholder
        MethodVisitor cv_set_value = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_SET + attribute + PERSISTENCE_FIELDNAME_POSTFIX, "(" + VHI_SIGNATURE + ")V", null, null);

        // _toplink_foo_vh = valueholderinterface;
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 0);
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 1);
        cv_set_value.visitFieldInsn(Opcodes.PUTFIELD, className, PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, VHI_SIGNATURE);

        // if (valueholderinterface.isInstantiated()){
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 1);
        cv_set_value.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "isInstantiated", "()Z", true);
        Label l0 = ASMFactory.createLabel();
        cv_set_value.visitJumpInsn(Opcodes.IFEQ, l0);

        // Object object = getFoo();
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 0);
        if (attributeDetails.getGetterMethodName() != null) {
            cv_set_value.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, attributeDetails.getGetterMethodName(), "()L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";", false);
        } else {
            cv_set_value.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, PERSISTENCE_GET + attributeDetails.attributeName, "()L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";", false);
        }
        cv_set_value.visitVarInsn(Opcodes.ASTORE, 2);

        // Object value = valueholderinterface.getValue();
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 1);
        cv_set_value.visitMethodInsn(Opcodes.INVOKEINTERFACE, VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);
        cv_set_value.visitVarInsn(Opcodes.ASTORE, 3);

        // if (object != value){
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 2);
        cv_set_value.visitVarInsn(Opcodes.ALOAD, 3);
        if (attributeDetails.getSetterMethodName() != null) {
            cv_set_value.visitJumpInsn(Opcodes.IF_ACMPEQ, l0);
            // setFoo((EntityC)value);
            cv_set_value.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set_value.visitVarInsn(Opcodes.ALOAD, 3);
            cv_set_value.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));
            cv_set_value.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, attributeDetails.getSetterMethodName(), "(L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";)V", false);
            //}
            cv_set_value.visitLabel(l0);
        } else {
            Label l1 = ASMFactory.createLabel();
            cv_set_value.visitJumpInsn(Opcodes.IF_ACMPEQ, l1);
            // _persistence_setFoo((EntityC)value);
            cv_set_value.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set_value.visitVarInsn(Opcodes.ALOAD, 3);
            cv_set_value.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));
            cv_set_value.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, PERSISTENCE_SET + attributeDetails.getAttributeName(), "(L" + attributeDetails.getReferenceClassName().replace('.', '/') + ";)V", false);
            // }
            cv_set_value.visitLabel(l1);
            cv_set_value.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            Label l2 = ASMFactory.createLabel();
            cv_set_value.visitJumpInsn(Opcodes.GOTO, l2);
            // }
            cv_set_value.visitLabel(l0);
            // else {
            cv_set_value.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            // foo = null;
            cv_set_value.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set_value.visitInsn(Opcodes.ACONST_NULL);
            cv_set_value.visitFieldInsn(Opcodes.PUTFIELD, className, attributeDetails.attributeName, attributeDetails.getReferenceClassType().getDescriptor());
            //}
            cv_set_value.visitLabel(l2);
            cv_set_value.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        cv_set_value.visitInsn(Opcodes.RETURN);
        cv_set_value.visitMaxs(0, 0);
    }

    /**
     * Adds a convenience method used to replace a PUTFIELD when field access is
     * used. The method follows the following form:
     *
     * public void _persistence_set_variableName((VariableClas) argument) {
     * _persistence_checkFetchedForSet("variableName");
     * _persistence_initialize_variableName_vh();
     * _persistence_propertyChange("variableName", this.variableName, argument);
     * // if change tracking enabled, wrapping primitives, i.e. Long.valueOf(item)
     * this.variableName = argument;
     * _persistence_variableName_vh.setValue(variableName); // if lazy enabled }
     */
    public void addSetterMethodForFieldAccess(ClassDetails classDetails, AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();

        // create _persistence_set_variableName
        MethodVisitor cv_set = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_SET + attribute, "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")V", null, null);

        // Get the opcode for the load instruction. This may be different
        // depending on the type
        int opcode = attributeDetails.getReferenceClassType().getOpcode(Opcodes.ILOAD);

        if (classDetails.shouldWeaveFetchGroups()) {
            cv_set.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set.visitLdcInsn(attribute);
            // _persistence_checkFetchedForSet("variableName");
            cv_set.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_checkFetchedForSet", "(Ljava/lang/String;)V", false);
        }

        if (classDetails.shouldWeaveChangeTracking()) {
            if (attributeDetails.weaveValueHolders()) {
                // _persistence_initialize_variableName_vh();
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_initialize_" + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

                // _persistenc_variableName_vh.getValue();
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                cv_set.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);

                // Add the cast:
                // (<VariableClass>)_persistenc_variableName_vh.getValue()
                cv_set.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));

                // add the assignment: this.variableName =
                // (<VariableClass>)_persistenc_variableName_vh.getValue();
                cv_set.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());
            }

            // load the string attribute name as the first argument of the
            // property change call
            cv_set.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set.visitLdcInsn(attribute);

            // if the attribute is a primitive, wrap it
            // e.g. if it is an integer: Integer.valueOf(attribute)
            // This is the first part of the wrapping
            String wrapper = ClassWeaver.wrapperFor(attributeDetails.getReferenceClassType().getSort());
            if (wrapper != null) {
                cv_set.visitTypeInsn(Opcodes.NEW, wrapper);
                cv_set.visitInsn(Opcodes.DUP);
            }

            // load the method argument
            cv_set.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());

            if (wrapper != null) {
                // invoke the constructor for wrapping
                // e.g. Integer.valueOf(variableName)
                cv_set.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")V", false);

                // wrap the method argument
                // e.g. Integer.valueOf(argument)
                cv_set.visitTypeInsn(Opcodes.NEW, wrapper);
                cv_set.visitInsn(Opcodes.DUP);
                cv_set.visitVarInsn(opcode, 1);
                cv_set.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")V", false);
            } else {
                // if we are not wrapping the argument, just load it
                cv_set.visitVarInsn(Opcodes.ALOAD, 1);
            }
            // _persistence_propertyChange("variableName", variableName,
            // argument);
            cv_set.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_propertyChange", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false);
        } else {
            if (attributeDetails.weaveValueHolders()) {
                // _persistence_initialize_variableName_vh();
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_initialize_" + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

                // _persistenc_variableName_vh.getValue();
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                cv_set.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);

                // Add the cast:
                // (<VariableClass>)_persistenc_variableName_vh.getValue()
                cv_set.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));

                // add the assignment: this.variableName =
                // (<VariableClass>)_persistenc_variableName_vh.getValue();
                cv_set.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());
            }
        }

        // Must set variable after raising change event, so event has old and
        // new value.
        // variableName = argument
        cv_set.visitVarInsn(Opcodes.ALOAD, 0);
        cv_set.visitVarInsn(opcode, 1);
        cv_set.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());

        if (attributeDetails.weaveValueHolders()) {
            // _persistence_variableName_vh.setValue(argument);
            cv_set.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
            cv_set.visitVarInsn(Opcodes.ALOAD, 1);
            cv_set.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "setValue", "(Ljava/lang/Object;)V", true);
        }

        cv_set.visitInsn(Opcodes.RETURN);
        cv_set.visitMaxs(0, 0);
    }

    /**
     * Adds a convenience method used to replace a GETFIELD when field access is
     * used. The method follows the following form:
     *
     * public (VariableClass) _persistence_get_variableName() {
     * _persistence_checkFetched("variableName");
     * _persistence_initialize_variableName_vh(); this.variableName =
     * ((VariableClass))_persistence_variableName_vh.getValue(); return
     * this.variableName; }
     */
    public void addGetterMethodForFieldAccess(ClassDetails classDetails, AttributeDetails attributeDetails) {
        String attribute = attributeDetails.getAttributeName();

        // create the _persistenc_getvariableName method
        MethodVisitor cv_get = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_GET + attribute, "()" + attributeDetails.getReferenceClassType().getDescriptor(), null, null);

        if (classDetails.shouldWeaveFetchGroups()) {
            cv_get.visitVarInsn(Opcodes.ALOAD, 0);
            cv_get.visitLdcInsn(attribute);
            // _persistence_checkFetched("variableName");
            cv_get.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_checkFetched", "(Ljava/lang/String;)V", false);
        }

        if (attributeDetails.weaveValueHolders()) {
            // _persistence_initialize_variableName_vh();
            cv_get.visitVarInsn(Opcodes.ALOAD, 0);
            cv_get.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_initialize_" + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

            // _persistenc_variableName_vh.getValue();
            cv_get.visitVarInsn(Opcodes.ALOAD, 0);
            cv_get.visitVarInsn(Opcodes.ALOAD, 0);
            cv_get.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attribute + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
            cv_get.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);

            // Add the cast:
            // (<VariableClass>)_persistenc_variableName_vh.getValue()
            cv_get.visitTypeInsn(Opcodes.CHECKCAST, attributeDetails.getReferenceClassName().replace('.', '/'));

            // add the assignment: this.variableName =
            // (<VariableClass>)_persistenc_variableName_vh.getValue();
            cv_get.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());
        }

        // return this.variableName;
        cv_get.visitVarInsn(Opcodes.ALOAD, 0);
        cv_get.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), attribute, attributeDetails.getReferenceClassType().getDescriptor());
        // Get the opcode for the return insturction. This may be different
        // depending on the type.
        int opcode = attributeDetails.getReferenceClassType().getOpcode(Opcodes.IRETURN);
        cv_get.visitInsn(opcode);
        cv_get.visitMaxs(0, 0);
    }

    /**
     * Add a variable of type Object to the class. When this method has been
     * run, the class will contain a variable declarations similar to the
     * following:
     *
     * private Object _persistence_primaryKey;
     */
    public void addPersistenceEntityVariables() {
        cv.visitField(Opcodes.ACC_PROTECTED + Opcodes.ACC_TRANSIENT, "_persistence_primaryKey", OBJECT_SIGNATURE, null, null);
        cv.visitField(Opcodes.ACC_PROTECTED + Opcodes.ACC_TRANSIENT, "_persistence_cacheKey", CACHEKEY_SIGNATURE, null, null);
    }

    /**
     * Add an internal post clone method. This will clone value holders to avoid
     * change original/clone to effect the other.
     *
     * public Object _persistence_post_clone() { this._attribute_vh =
     * this._attribute_vh.clone(); ... this._persistence_listener = null; return
     * this; }
     */
    public void addPersistencePostClone(ClassDetails classDetails) {
        // create the _persistence_post_clone() method
        MethodVisitor cv_clone = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_post_clone", "()Ljava/lang/Object;", null, null);

        // if there is a weaved superclass, it will implement
        // _persistence_post_clone. Call that method
        // super._persistence_post_clone()
        if (classDetails.getSuperClassDetails() != null && classDetails.getSuperClassDetails().shouldWeaveInternal()) {
            cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
            cv_clone.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getSuperClassName(), "_persistence_post_clone", "()Ljava/lang/Object;", false);
        }

        if (classDetails.shouldWeaveValueHolders()) {
            for (Iterator<AttributeDetails> iterator = classDetails.getAttributesMap().values().iterator(); iterator.hasNext();) {
                AttributeDetails attributeDetails = iterator.next();
                if (attributeDetails.weaveValueHolders()) { // &&
                                                            // !attributeDetails.isAttributeOnSuperClass())
                                                            // {
                    // clone._attribute_vh = this._attribute_vh.clone();
                    cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
                    cv_clone.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                    Label label = ASMFactory.createLabel();
                    cv_clone.visitJumpInsn(Opcodes.IFNULL, label);
                    cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
                    cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
                    cv_clone.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                    cv_clone.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "clone", "()Ljava/lang/Object;", true);
                    cv_clone.visitTypeInsn(Opcodes.CHECKCAST, ClassWeaver.VHI_SHORT_SIGNATURE);
                    cv_clone.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + attributeDetails.getAttributeName() + PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                    cv_clone.visitLabel(label);
                }
            }
        }
        if (classDetails.shouldWeaveChangeTracking()) {
            // clone._persistence_listener = null;
            cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
            cv_clone.visitInsn(Opcodes.ACONST_NULL);
            cv_clone.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_listener", PCL_SIGNATURE);
        }
        if (classDetails.shouldWeaveFetchGroups()) {
            // clone._persistence_fetchGroup = null;
            // clone._persistence_session = null;
            cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
            cv_clone.visitInsn(Opcodes.ACONST_NULL);
            cv_clone.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_fetchGroup", FETCHGROUP_SIGNATURE);
            cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
            cv_clone.visitInsn(Opcodes.ACONST_NULL);
            cv_clone.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_session", SESSION_SIGNATURE);
        }

        if (!classDetails.isEmbedable()) {
            // clone._persistence_primaryKey = null;
            cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
            cv_clone.visitInsn(Opcodes.ACONST_NULL);
            cv_clone.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_primaryKey", OBJECT_SIGNATURE);
        }

        // return clone;
        cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
        cv_clone.visitInsn(Opcodes.ARETURN);
        cv_clone.visitMaxs(0, 0);
    }

    public void addPersistenceRestMethods(ClassDetails classDetails) {
        // public List<RelationshipInfo> _persistence_getRelationships() {
        //   return this._persistence_relationshipInfo;
        // }
        MethodVisitor cv_getPKVector = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "getRelationships", "()" + LIST_RELATIONSHIP_INFO_SIGNATURE, "()" + LIST_RELATIONSHIP_INFO_GENERIC_SIGNATURE, null);
        cv_getPKVector.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getPKVector.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_relationshipInfo", LIST_RELATIONSHIP_INFO_SIGNATURE);
        cv_getPKVector.visitInsn(Opcodes.ARETURN);
        cv_getPKVector.visitMaxs(0, 0);

        // public void _persistence_setRelationships(List<RelationshipInfo> paramList) {
        //   this._persistence_relationshipInfo = paramList;
        // }
        MethodVisitor cv_setPKVector = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "setRelationships", "(" + LIST_RELATIONSHIP_INFO_SIGNATURE + ")V", "(" + LIST_RELATIONSHIP_INFO_GENERIC_SIGNATURE + ")V", null);
        cv_setPKVector.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setPKVector.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setPKVector.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + "relationshipInfo", LIST_RELATIONSHIP_INFO_SIGNATURE);
        cv_setPKVector.visitInsn(Opcodes.RETURN);
        cv_setPKVector.visitMaxs(0, 0);

        // public Link _persistence_getHref() {
        //   return this._persistence_href;
        // }
        MethodVisitor cv_getHref = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "getHref", "()" + LINK_SIGNATURE, null, null);
        cv_getHref.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getHref.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + "href", LINK_SIGNATURE);
        cv_getHref.visitInsn(Opcodes.ARETURN);
        cv_getHref.visitMaxs(0, 0);

        // public void _persistence_setHref(Link paramLink)
        //   this._persistence_href = paramLink;
        // }
        MethodVisitor cv_setHref = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "setHref", "(" + LINK_SIGNATURE + ")V", null, null);
        cv_setHref.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setHref.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setHref.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + "href", LINK_SIGNATURE);
        cv_setHref.visitInsn(Opcodes.RETURN);
        cv_setHref.visitMaxs(0, 0);

        // public ItemLinks _persistence_getLinks() {
        //   return this._persistence_links;
        // }
        MethodVisitor cv_getLinks = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "getLinks", "()" + ITEM_LINKS_SIGNATURE, null, null);
        cv_getLinks.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getLinks.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + "links", ITEM_LINKS_SIGNATURE);
        cv_getLinks.visitInsn(Opcodes.ARETURN);
        cv_getLinks.visitMaxs(0, 0);

        // public void _persistence_setLinks(ItemLinks paramItemLinks) {
        //   this._persistence_links = paramItemLinks;
        // }
        MethodVisitor cv_setLinks = cv.visitMethod(Opcodes.ACC_PUBLIC, PERSISTENCE_FIELDNAME_PREFIX + "setLinks", "(" + ITEM_LINKS_SIGNATURE + ")V", null, null);
        cv_setLinks.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setLinks.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setLinks.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), PERSISTENCE_FIELDNAME_PREFIX + "links", ITEM_LINKS_SIGNATURE);
        cv_setLinks.visitInsn(Opcodes.RETURN);
        cv_setLinks.visitMaxs(0, 0);

    }

    public void addPersistenceRestVariables() {
        // protected transient List<RelationshipInfo> _persistence_relationshipInfo;
        cv.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT, PERSISTENCE_FIELDNAME_PREFIX + "relationshipInfo", LIST_RELATIONSHIP_INFO_SIGNATURE, LIST_RELATIONSHIP_INFO_GENERIC_SIGNATURE, null);
        // protected transient Link _persistence_href;
        cv.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT, PERSISTENCE_FIELDNAME_PREFIX + "href", LINK_SIGNATURE, null, null);
        // protected transient ItemLinks _persistence_links;
        cv.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT, PERSISTENCE_FIELDNAME_PREFIX + "links", ITEM_LINKS_SIGNATURE, null, null);
    }

    /**
     * Add an internal shallow clone method. This can be used to optimize uow
     * cloning.
     *
     * public Object _persistence_shallow_clone() { return super.clone(); }
     */
    public void addShallowClone(ClassDetails classDetails) {
        // create the _persistence_shallow_clone() method
        MethodVisitor cv_clone = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_shallow_clone", "()Ljava/lang/Object;", null, null);

        // return super.clone();
        cv_clone.visitVarInsn(Opcodes.ALOAD, 0);
        cv_clone.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "clone", "()Ljava/lang/Object;", false);

        cv_clone.visitInsn(Opcodes.ARETURN);
        cv_clone.visitMaxs(0, 0);
    }

    /**
     * Add an internal empty constructor, and new method. This is used to avoid
     * unnecessary initialization and avoid reflection.
     *
     * public void _persistence_new(PersistenceObject factory) { return new
     * ClassType(factory); }
     *
     * public ClassType(PersistenceObject factory) { super(); }
     */
    public void addPersistenceNew(ClassDetails classDetails) {
        // create the _persistence_new() method
        MethodVisitor cv_new = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_new", "(" + PERSISTENCE_OBJECT_SIGNATURE + ")Ljava/lang/Object;", null, null);

        // return new ClassType(factory);
        cv_new.visitTypeInsn(Opcodes.NEW, classDetails.getClassName());
        cv_new.visitInsn(Opcodes.DUP);
        if (!classDetails.canWeaveConstructorOptimization()) {
            cv_new.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getClassName(), "<init>", "()V", false);
            cv_new.visitInsn(Opcodes.ARETURN);
            cv_new.visitMaxs(0, 0);
            return;
        } else {
            cv_new.visitVarInsn(Opcodes.ALOAD, 1);
            cv_new.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getClassName(), "<init>", "(" + PERSISTENCE_OBJECT_SIGNATURE + ")V", false);
        }
        cv_new.visitInsn(Opcodes.ARETURN);
        cv_new.visitMaxs(0, 0);

        // create the ClassType() method
        MethodVisitor cv_constructor = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + PERSISTENCE_OBJECT_SIGNATURE + ")V", null, null);

        cv_constructor.visitVarInsn(Opcodes.ALOAD, 0);
        if (classDetails.getSuperClassDetails() == null) {
            // super();
            cv_constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getSuperClassName(), "<init>", "()V", false);
        } else {
            // super(factory);
            cv_constructor.visitVarInsn(Opcodes.ALOAD, 1);
            cv_constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getSuperClassName(), "<init>", "(" + PERSISTENCE_OBJECT_SIGNATURE + ")V", false);
        }
        cv_constructor.visitInsn(Opcodes.RETURN);
        cv_constructor.visitMaxs(0, 0);
    }

    /**
     * Add an internal generic get and set method. This is used to avoid
     * reflection.
     *
     * public Object _persistence_get(String attribute) { if (attribute ==
     * "address") { return this.address; } if (attribute == "city") { return
     * this.city; } return null; }
     *
     * public void _persistence_set(int index, Object value) { if (attribute ==
     * "address") { this.address = (String)value; } else if (attribute ==
     * "city") { this.city = (String)city; } }
     */
    public void addPersistenceGetSet(ClassDetails classDetails) {
        // create the _persistence_get() method
        MethodVisitor cv_get = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_get", "(Ljava/lang/String;)Ljava/lang/Object;", null, null);

        Label label = null;
        for (AttributeDetails attributeDetails : classDetails.getAttributesMap().values()) {
            if (!attributeDetails.isAttributeOnSuperClass() && !attributeDetails.isVirtualProperty()) {
                if (label != null) {
                    cv_get.visitLabel(label);
                }
                // else if (attribute == "address")
                cv_get.visitVarInsn(Opcodes.ALOAD, 1);
                cv_get.visitLdcInsn(attributeDetails.getAttributeName().intern());
                label = ASMFactory.createLabel();
                cv_get.visitJumpInsn(Opcodes.IF_ACMPNE, label);
                // return this.address
                cv_get.visitVarInsn(Opcodes.ALOAD, 0);
                cv_get.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), attributeDetails.getAttributeName(), attributeDetails.getReferenceClassType().getDescriptor());
                // if this is a primitive, get the wrapper class
                String wrapper = ClassWeaver.wrapperFor(attributeDetails.getReferenceClassType().getSort());
                if (wrapper != null) {
                    // Call valueOf on the wrapper (more optimal than
                    // constructor).
                    cv_get.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")L" + wrapper + ";", false);
                }

                cv_get.visitInsn(Opcodes.ARETURN);
            }
        }
        if (label != null) {
            cv_get.visitLabel(label);
        }
        // call super, or return null
        if (classDetails.getSuperClassDetails() == null) {
            // return null;
            cv_get.visitInsn(Opcodes.ACONST_NULL);
        } else {
            cv_get.visitVarInsn(Opcodes.ALOAD, 0);
            cv_get.visitVarInsn(Opcodes.ALOAD, 1);
            cv_get.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getSuperClassName(), "_persistence_get", "(Ljava/lang/String;)Ljava/lang/Object;", false);
        }

        cv_get.visitInsn(Opcodes.ARETURN);
        cv_get.visitMaxs(0, 0);

        // create the _persistence_set() method
        MethodVisitor cv_set = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_set", "(Ljava/lang/String;Ljava/lang/Object;)V", null, null);

        label = null;
        for (AttributeDetails attribute : classDetails.getAttributesMap().values()) {
            if (!attribute.isAttributeOnSuperClass() && !attribute.isVirtualProperty()) {
                if (label != null) {
                    cv_set.visitLabel(label);
                }
                // else if (attribute == "address")
                cv_set.visitVarInsn(Opcodes.ALOAD, 1);
                cv_set.visitLdcInsn(attribute.getAttributeName().intern());
                label = ASMFactory.createLabel();
                cv_set.visitJumpInsn(Opcodes.IF_ACMPNE, label);
                // this.address = (String)value;
                cv_set.visitVarInsn(Opcodes.ALOAD, 0);
                cv_set.visitVarInsn(Opcodes.ALOAD, 2);
                String wrapper = wrapperFor(attribute.getReferenceClassType().getSort());
                if (wrapper == null) {
                    wrapper = attribute.getReferenceClassName().replace('.', '/');
                }
                cv_set.visitTypeInsn(Opcodes.CHECKCAST, wrapper);
                // Unwrap any primitive wrapper to its value.
                unwrapPrimitive(attribute, cv_set);
                cv_set.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), attribute.getAttributeName(), attribute.getReferenceClassType().getDescriptor());
                // return;
                cv_set.visitInsn(Opcodes.RETURN);
            }
        }
        if (label != null) {
            cv_set.visitLabel(label);
        }
        // call super, or return null
        if (classDetails.getSuperClassDetails() != null) {
            cv_set.visitVarInsn(Opcodes.ALOAD, 0);
            cv_set.visitVarInsn(Opcodes.ALOAD, 1);
            cv_set.visitVarInsn(Opcodes.ALOAD, 2);
            cv_set.visitMethodInsn(Opcodes.INVOKESPECIAL, classDetails.getSuperClassName(), "_persistence_set", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
        }

        cv_set.visitInsn(Opcodes.RETURN);
        cv_set.visitMaxs(0, 0);
    }

    /**
     * Adds get/set method for PersistenceEntity interface. This adds the
     * following methods:
     *
     * public Object _persistence_getId() { return _persistence_primaryKey; }
     * public void _persistence_setId(Object primaryKey) {
     * this._persistence_primaryKey = primaryKey; }
     */
    public void addPersistenceEntityMethods(ClassDetails classDetails) {
        MethodVisitor cv_getPKVector = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_getId", "()" + OBJECT_SIGNATURE, null, null);
        cv_getPKVector.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getPKVector.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_primaryKey", OBJECT_SIGNATURE);
        cv_getPKVector.visitInsn(Opcodes.ARETURN);
        cv_getPKVector.visitMaxs(0, 0);

        MethodVisitor cv_setPKVector = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setId", "(" + OBJECT_SIGNATURE + ")V", null, null);
        cv_setPKVector.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setPKVector.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setPKVector.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_primaryKey", OBJECT_SIGNATURE);
        cv_setPKVector.visitInsn(Opcodes.RETURN);
        cv_setPKVector.visitMaxs(0, 0);

        MethodVisitor cv_getCacheKey = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_getCacheKey", "()" + CACHEKEY_SIGNATURE, null, null);
        cv_getCacheKey.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getCacheKey.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_cacheKey", CACHEKEY_SIGNATURE);
        cv_getCacheKey.visitInsn(Opcodes.ARETURN);
        cv_getCacheKey.visitMaxs(0, 0);

        MethodVisitor cv_setCacheKey = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setCacheKey", "(" + CACHEKEY_SIGNATURE + ")V", null, null);
        cv_setCacheKey.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setCacheKey.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setCacheKey.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_cacheKey", CACHEKEY_SIGNATURE);
        cv_setCacheKey.visitInsn(Opcodes.RETURN);
        cv_setCacheKey.visitMaxs(0, 0);
    }

    /**
     * Add a variable of type FetchGroup, Session to the class. When this method
     * has been run, the class will contain a variable declarations similar to
     * the following:
     *
     * private FetchGroup _persistence_fetchGroup; private boolean
     * _persistence_shouldRefreshFetchGroup; private Session
     * _persistence_session;
     */
    public void addFetchGroupVariables() {
        FieldVisitor fv = cv.visitField(Opcodes.ACC_PROTECTED, "_persistence_fetchGroup", FETCHGROUP_SIGNATURE, null, null);
        // Only add jakarta.persistence.Transient annotation if attribute access
        // is being used
        if (classDetails.usesAttributeAccess()) {
            fv.visitAnnotation(JPA_TRANSIENT_DESCRIPTION, true).visitEnd();
        }
        if (isJAXBOnPath()) {
            fv.visitAnnotation(XML_TRANSIENT_DESCRIPTION, true).visitEnd();
        }
        fv.visitEnd();

        cv.visitField(Opcodes.ACC_PROTECTED + Opcodes.ACC_TRANSIENT, "_persistence_shouldRefreshFetchGroup", PBOOLEAN_SIGNATURE, null, null).visitEnd();
        cv.visitField(Opcodes.ACC_PROTECTED + Opcodes.ACC_TRANSIENT, "_persistence_session", SESSION_SIGNATURE, null, null).visitEnd();
    }

    /**
     * Adds get/set method for FetchGroupTracker interface. This adds the
     * following methods:
     *
     * public Session _persistence_getSession() { return _persistence_session; }
     * public void _persistence_setSession(Session session) {
     * this._persistence_session = session; }
     *
     * public FetchGroup _persistence_getFetchGroup() { return
     * _persistence_fetchGroup; } public void
     * _persistence_setFetchGroup(FetchGroup fetchGroup) {
     * this._persistence_fetchGroup = fetchGroup; }
     *
     * public boolean _persistence_shouldRefreshFetchGroup() { return
     * _persistence_shouldRefreshFetchGroup; } public void
     * _persistence_setShouldRefreshFetchGroup(boolean shouldRefreshFetchGroup)
     * { this._persistence_shouldRefreshFetchGroup = shouldRefreshFetchGroup; }
     *
     * public void _persistence_resetFetchGroup() { }
     *
     * public void _persistence_isAttributeFetched(String attribute) { return
     * this._persistence_fetchGroup == null ||
     * _persistence_fetchGroup.containsAttribute(attribute); }
     *
     * public void _persistence_checkFetched(String attribute) { if
     * (this._persistence_fetchGroup != null) {
     * EntityManagerImpl.processUnfetchedAttribute(this, attribute); } }
     *
     *
     * public void _persistence_checkSetFetched(String attribute) { if
     * (this._persistence_fetchGroup != null) {
     * EntityManagerImpl.processUnfetchedAttributeForSet(this, attribute); } }
     */
    public void addFetchGroupMethods(ClassDetails classDetails) {
        MethodVisitor cv_getSession = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_getSession", "()" + SESSION_SIGNATURE, null, null);
        cv_getSession.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getSession.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_session", SESSION_SIGNATURE);
        cv_getSession.visitInsn(Opcodes.ARETURN);
        cv_getSession.visitMaxs(0, 0);

        MethodVisitor cv_setSession = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setSession", "(" + SESSION_SIGNATURE + ")V", null, null);
        cv_setSession.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setSession.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setSession.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_session", SESSION_SIGNATURE);
        cv_setSession.visitInsn(Opcodes.RETURN);
        cv_setSession.visitMaxs(0, 0);

        MethodVisitor cv_getFetchGroup = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_getFetchGroup", "()" + FETCHGROUP_SIGNATURE, null, null);
        cv_getFetchGroup.visitVarInsn(Opcodes.ALOAD, 0);
        cv_getFetchGroup.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_fetchGroup", FETCHGROUP_SIGNATURE);
        cv_getFetchGroup.visitInsn(Opcodes.ARETURN);
        cv_getFetchGroup.visitMaxs(0, 0);

        MethodVisitor cv_setFetchGroup = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setFetchGroup", "(" + FETCHGROUP_SIGNATURE + ")V", null, null);
        cv_setFetchGroup.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setFetchGroup.visitVarInsn(Opcodes.ALOAD, 1);
        cv_setFetchGroup.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_fetchGroup", FETCHGROUP_SIGNATURE);
        cv_setFetchGroup.visitInsn(Opcodes.RETURN);
        cv_setFetchGroup.visitMaxs(0, 0);

        MethodVisitor cv_shouldRefreshFetchGroup = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_shouldRefreshFetchGroup", "()" + PBOOLEAN_SIGNATURE, null, null);
        cv_shouldRefreshFetchGroup.visitVarInsn(Opcodes.ALOAD, 0);
        cv_shouldRefreshFetchGroup.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_shouldRefreshFetchGroup", PBOOLEAN_SIGNATURE);
        cv_shouldRefreshFetchGroup.visitInsn(Opcodes.IRETURN);
        cv_shouldRefreshFetchGroup.visitMaxs(0, 0);

        MethodVisitor cv_setShouldRefreshFetchGroup = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_setShouldRefreshFetchGroup", "(" + PBOOLEAN_SIGNATURE + ")V", null, null);
        cv_setShouldRefreshFetchGroup.visitVarInsn(Opcodes.ALOAD, 0);
        cv_setShouldRefreshFetchGroup.visitVarInsn(Opcodes.ILOAD, 1);
        cv_setShouldRefreshFetchGroup.visitFieldInsn(Opcodes.PUTFIELD, classDetails.getClassName(), "_persistence_shouldRefreshFetchGroup", PBOOLEAN_SIGNATURE);
        cv_setShouldRefreshFetchGroup.visitInsn(Opcodes.RETURN);
        cv_setShouldRefreshFetchGroup.visitMaxs(0, 0);

        MethodVisitor cv_resetFetchGroup = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_resetFetchGroup", "()V", null, null);
        cv_resetFetchGroup.visitInsn(Opcodes.RETURN);
        cv_resetFetchGroup.visitMaxs(0, 0);

        MethodVisitor cv_isAttributeFetched = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_isAttributeFetched", "(Ljava/lang/String;)Z", null, null);
        cv_isAttributeFetched.visitVarInsn(Opcodes.ALOAD, 0);
        cv_isAttributeFetched.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_fetchGroup", FETCHGROUP_SIGNATURE);
        Label gotoTrue = ASMFactory.createLabel();
        cv_isAttributeFetched.visitJumpInsn(Opcodes.IFNULL, gotoTrue);
        cv_isAttributeFetched.visitVarInsn(Opcodes.ALOAD, 0);
        cv_isAttributeFetched.visitFieldInsn(Opcodes.GETFIELD, classDetails.getClassName(), "_persistence_fetchGroup", FETCHGROUP_SIGNATURE);
        cv_isAttributeFetched.visitVarInsn(Opcodes.ALOAD, 1);
        cv_isAttributeFetched.visitMethodInsn(Opcodes.INVOKEVIRTUAL, FETCHGROUP_SHORT_SIGNATURE, "containsAttributeInternal", "(Ljava/lang/String;)Z", false);
        Label gotoFalse = ASMFactory.createLabel();
        cv_isAttributeFetched.visitJumpInsn(Opcodes.IFEQ, gotoFalse);
        cv_isAttributeFetched.visitLabel(gotoTrue);
        cv_isAttributeFetched.visitInsn(Opcodes.ICONST_1);
        Label gotoReturn = ASMFactory.createLabel();
        cv_isAttributeFetched.visitJumpInsn(Opcodes.GOTO, gotoReturn);
        cv_isAttributeFetched.visitLabel(gotoFalse);
        cv_isAttributeFetched.visitInsn(Opcodes.ICONST_0);
        cv_isAttributeFetched.visitLabel(gotoReturn);
        cv_isAttributeFetched.visitInsn(Opcodes.IRETURN);
        cv_isAttributeFetched.visitMaxs(0, 0);

        MethodVisitor cv_checkFetched = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_checkFetched", "(Ljava/lang/String;)V", null, null);
        cv_checkFetched.visitVarInsn(Opcodes.ALOAD, 0);
        cv_checkFetched.visitVarInsn(Opcodes.ALOAD, 1);
        cv_checkFetched.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_isAttributeFetched", "(Ljava/lang/String;)Z", false);
        gotoReturn = ASMFactory.createLabel();
        cv_checkFetched.visitJumpInsn(Opcodes.IFNE, gotoReturn);
        cv_checkFetched.visitVarInsn(Opcodes.ALOAD, 0);
        cv_checkFetched.visitTypeInsn(Opcodes.CHECKCAST, FETCHGROUP_TRACKER_SHORT_SIGNATURE);
        cv_checkFetched.visitVarInsn(Opcodes.ALOAD, 1);
        cv_checkFetched.visitMethodInsn(Opcodes.INVOKESTATIC, ENTITY_MANAGER_IMPL_SHORT_SIGNATURE, "processUnfetchedAttribute", "(" + FETCHGROUP_TRACKER_SIGNATURE + "Ljava/lang/String;)V", false);
        cv_checkFetched.visitLabel(gotoReturn);
        cv_checkFetched.visitInsn(Opcodes.RETURN);
        cv_checkFetched.visitMaxs(0, 0);

        MethodVisitor cv_checkFetchedForSet = cv.visitMethod(Opcodes.ACC_PUBLIC, "_persistence_checkFetchedForSet", "(Ljava/lang/String;)V", null, null);
        cv_checkFetchedForSet.visitVarInsn(Opcodes.ALOAD, 0);
        cv_checkFetchedForSet.visitVarInsn(Opcodes.ALOAD, 1);
        cv_checkFetchedForSet.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classDetails.getClassName(), "_persistence_isAttributeFetched", "(Ljava/lang/String;)Z", false);
        gotoReturn = ASMFactory.createLabel();
        cv_checkFetchedForSet.visitJumpInsn(Opcodes.IFNE, gotoReturn);
        cv_checkFetchedForSet.visitVarInsn(Opcodes.ALOAD, 0);
        cv_checkFetchedForSet.visitTypeInsn(Opcodes.CHECKCAST, FETCHGROUP_TRACKER_SHORT_SIGNATURE);
        cv_checkFetchedForSet.visitVarInsn(Opcodes.ALOAD, 1);
        cv_checkFetchedForSet.visitMethodInsn(Opcodes.INVOKESTATIC, ENTITY_MANAGER_IMPL_SHORT_SIGNATURE, "processUnfetchedAttributeForSet", "(" + FETCHGROUP_TRACKER_SIGNATURE + "Ljava/lang/String;)V", false);
        cv_checkFetchedForSet.visitLabel(gotoReturn);
        cv_checkFetchedForSet.visitInsn(Opcodes.RETURN);
        cv_checkFetchedForSet.visitMaxs(0, 0);
    }

    /**
     * Visit the class byte-codes and modify to weave Persistence interfaces.
     * This add PersistenceWeaved, PersistenceWeavedLazy,
     * PersistenceWeavedChangeTracking, PersistenceEntity, ChangeTracker. The
     * new interfaces are pass to the super weaver.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        boolean weaveCloneable = true;
        // To prevent 'double' weaving: scan for PersistenceWeaved interface.
        for (int index = 0; index < interfaces.length; index++) {
            String existingInterface = interfaces[index];
            if (PERSISTENCE_WEAVED_SHORT_SIGNATURE.equals(existingInterface)) {
                this.alreadyWeaved = true;
                super.visitSuper(version, access, name, signature, superName, interfaces);
                return;
            } else if (CT_SHORT_SIGNATURE.equals(existingInterface)) {
                // Disable weaving of change tracking if already implemented
                // (such as by user).
                classDetails.setShouldWeaveChangeTracking(false);
            } else if (CLONEABLE_SHORT_SIGNATURE.equals(existingInterface)) {
                weaveCloneable = false;
            }
        }
        int newInterfacesLength = interfaces.length;
        // Cloneable
        int cloneableIndex = 0;
        weaveCloneable = classDetails.shouldWeaveInternal() && weaveCloneable && (classDetails.getSuperClassDetails() == null);
        if (weaveCloneable) {
            cloneableIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        // PersistenceWeaved
        int persistenceWeavedIndex = newInterfacesLength;
        newInterfacesLength++;
        // PersistenceEntity
        int persistenceEntityIndex = 0;
        boolean persistenceEntity = classDetails.shouldWeaveInternal() && (classDetails.getSuperClassDetails() == null) && (!classDetails.isEmbedable());
        if (persistenceEntity) {
            persistenceEntityIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        // PersistenceObject
        int persistenceObjectIndex = 0;
        boolean persistenceObject = classDetails.shouldWeaveInternal();
        if (persistenceObject) {
            persistenceObjectIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        // FetchGroupTracker
        int fetchGroupTrackerIndex = 0;
        boolean fetchGroupTracker = classDetails.shouldWeaveFetchGroups() && (classDetails.getSuperClassDetails() == null);
        if (fetchGroupTracker) {
            fetchGroupTrackerIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        int persistenceWeavedFetchGroupsIndex = 0;
        if (classDetails.shouldWeaveFetchGroups()) {
            persistenceWeavedFetchGroupsIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        // PersistenceWeavedLazy
        int persistenceWeavedLazyIndex = 0;
        if (classDetails.shouldWeaveValueHolders()) {
            persistenceWeavedLazyIndex = newInterfacesLength;
            newInterfacesLength++;
        }

        // ChangeTracker
        boolean changeTracker = !classDetails.doesSuperclassWeaveChangeTracking() && classDetails.shouldWeaveChangeTracking();
        int persistenceWeavedChangeTrackingIndex = 0;
        int changeTrackerIndex = 0;
        if (changeTracker) {
            changeTrackerIndex = newInterfacesLength;
            newInterfacesLength++;
        }
        if (classDetails.shouldWeaveChangeTracking()) {
            persistenceWeavedChangeTrackingIndex = newInterfacesLength;
            newInterfacesLength++;
        }

        int persistenceWeavedRestIndex = 0;
        boolean weaveRest = classDetails.shouldWeaveREST() && classDetails.getSuperClassDetails() == null;
        if (weaveRest) {
            persistenceWeavedRestIndex = newInterfacesLength;
            newInterfacesLength++;
        }

        String[] newInterfaces = new String[newInterfacesLength];
        System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
        // Add 'marker'
        // org.eclipse.persistence.internal.weaving.PersistenceWeaved interface.
        newInterfaces[persistenceWeavedIndex] = PERSISTENCE_WEAVED_SHORT_SIGNATURE;
        weaved = true;
        // Add Cloneable interface.
        if (weaveCloneable) {
            newInterfaces[cloneableIndex] = CLONEABLE_SHORT_SIGNATURE;
        }
        // Add org.eclipse.persistence.internal.descriptors.PersistenceEntity
        // interface.
        if (persistenceEntity) {
            newInterfaces[persistenceEntityIndex] = PERSISTENCE_ENTITY_SHORT_SIGNATURE;
        }
        // Add org.eclipse.persistence.internal.descriptors.PersistenceObject
        // interface.
        if (persistenceObject) {
            newInterfaces[persistenceObjectIndex] = PERSISTENCE_OBJECT_SHORT_SIGNATURE;
        }
        // Add org.eclipse.persistence.queries.FetchGroupTracker interface.
        if (fetchGroupTracker) {
            newInterfaces[fetchGroupTrackerIndex] = FETCHGROUP_TRACKER_SHORT_SIGNATURE;
        }
        if (classDetails.shouldWeaveFetchGroups()) {
            newInterfaces[persistenceWeavedFetchGroupsIndex] = WEAVED_FETCHGROUPS_SHORT_SIGNATURE;
        }
        // Add marker interface for LAZY.
        if (classDetails.shouldWeaveValueHolders()) {
            newInterfaces[persistenceWeavedLazyIndex] = TW_LAZY_SHORT_SIGNATURE;
        }
        // Add marker interface and change tracker interface for change
        // tracking.
        if (changeTracker) {
            newInterfaces[changeTrackerIndex] = CT_SHORT_SIGNATURE;
        }
        if (classDetails.shouldWeaveChangeTracking()) {
            newInterfaces[persistenceWeavedChangeTrackingIndex] = TW_CT_SHORT_SIGNATURE;
        }

        if (weaveRest) {
            newInterfaces[persistenceWeavedRestIndex] = WEAVED_REST_LAZY_SHORT_SIGNATURE;
        }

        String newSignature = null;
        // fix the signature to include any new methods we weave
        if (signature != null) {
            StringBuffer newSignatureBuf = new StringBuffer();
            newSignatureBuf.append(signature);

            for (int i = interfaces.length; i < newInterfaces.length; i++) {
                newSignatureBuf.append("L" + newInterfaces[i] + ";");
            }
            newSignature = newSignatureBuf.toString();
        }

        if (cw != null) {
            cv = cw;
        }
        cv.visit(version, access, name, newSignature, superName, newInterfaces);
    }

    /**
     * Construct a MethodWeaver and allow it to process the method.
     */
    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethodSuper(access, methodName, desc, signature, exceptions);
        if (!alreadyWeaved) {
            // skip constructors, they will not changed
            if (!"<init>".equals(methodName) && !"<cinit>".equals(methodName)) {
                // remaining modifications to the 'body' of the class are
                // delegated to MethodWeaver
                mv = new MethodWeaver(this, methodName, desc, mv);
            }
        }
        return mv;
    }

    /**
     * Visit the end of the class byte codes. Add any new methods or variables to the end.
     */
    @Override
    public void visitEnd() {
        if (!alreadyWeaved) {
            if (this.classDetails.shouldWeaveInternal()) {

                // Add a persistence and shallow clone method.
                addPersistencePostClone(this.classDetails);
                if (this.classDetails.getSuperClassDetails() == null) {
                    addShallowClone(this.classDetails);
                    if (!this.classDetails.isEmbedable()) {
                        // Add PersistenceEntity variables and methods.
                        addPersistenceEntityVariables();
                        addPersistenceEntityMethods(this.classDetails);
                        this.weavedPersistenceEntity = true;
                    }
                }
                // Add empty new method and generic get/set methods.
                addPersistenceNew(this.classDetails);
                addPersistenceGetSet(this.classDetails);
            }

            boolean attributeAccess = false;
            // For each attribute we need to check what methods and variables to
            // add.
            for (Iterator<AttributeDetails> iterator = this.classDetails.getAttributesMap().values().iterator(); iterator.hasNext();) {
                AttributeDetails attributeDetails = iterator.next();
                // Only add to classes that actually contain the attribute we
                // are
                // processing
                // an attribute could be in the classDetails but not actually in
                // the
                // class
                // if it is owned by a MappedSuperClass.
                if (!attributeDetails.isAttributeOnSuperClass()) {
                    if (attributeDetails.weaveValueHolders()) {
                        // We will add valueholders and methods to classes that
                        // have
                        // not already been weaved
                        // and classes that actually contain the attribute we
                        // are
                        // processing
                        // an attribute could be in the classDetails but not
                        // actually in the class
                        // if it is owned by a MappedSuperClass.
                        if (!attributeDetails.isAttributeOnSuperClass()) {
                            weaved = true;
                            weavedLazy = true;
                            addValueHolder(attributeDetails);
                            addInitializerForValueHolder(classDetails, attributeDetails);
                            addGetterMethodForValueHolder(classDetails, attributeDetails);
                            addSetterMethodForValueHolder(classDetails, attributeDetails);
                        }
                    }
                    if (classDetails.shouldWeaveChangeTracking() || classDetails.shouldWeaveFetchGroups() || attributeDetails.weaveValueHolders()) {
                        if (attributeDetails.hasField()) {
                            weaved = true;
                            addGetterMethodForFieldAccess(classDetails, attributeDetails);
                            addSetterMethodForFieldAccess(classDetails, attributeDetails);
                            attributeAccess = true;
                        }
                    }
                }
            }
            if (classDetails.shouldWeaveChangeTracking()) {
                weaved = true;
                weavedChangeTracker = true;
                if ((classDetails.getSuperClassDetails() == null) || (!classDetails.doesSuperclassWeaveChangeTracking())) {
                    addPropertyChangeListener(attributeAccess);
                    addGetPropertyChangeListener(classDetails);
                    addSetPropertyChangeListener(classDetails);
                    addPropertyChange(classDetails);
                }
            }
            if (classDetails.shouldWeaveFetchGroups()) {
                weaved = true;
                weavedFetchGroups = true;
                if (classDetails.getSuperClassDetails() == null) {
                    addFetchGroupVariables();
                    addFetchGroupMethods(this.classDetails);
                }
            }
        }
        if (classDetails.shouldWeaveREST() && classDetails.getSuperClassDetails() == null) {
            weavedRest = true;
            addPersistenceRestVariables();
            addPersistenceRestMethods(classDetails);
        }
        if (cw != null) {
            cw.visitEnd();
        } else {
            cv.visitEnd();
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotationSuper(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitFieldSuper(access, name, desc, signature, value);
    }

}
