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
//     19/04/2014-2.6 Lukas Jungmann
//       - 429992: JavaSE 8/ASM 5.0.1 support (EclipseLink silently ignores Entity classes with lambda expressions)
 package org.eclipse.persistence.internal.jpa.weaving;

//ASM imports
import org.eclipse.persistence.asm.ASMFactory;
import org.eclipse.persistence.asm.AnnotationVisitor;
import org.eclipse.persistence.asm.Attribute;
import org.eclipse.persistence.asm.Label;
import org.eclipse.persistence.asm.EclipseLinkMethodVisitor;
import org.eclipse.persistence.asm.MethodVisitor;
import org.eclipse.persistence.asm.Opcodes;
import org.eclipse.persistence.asm.Type;

import org.eclipse.persistence.internal.descriptors.VirtualAttributeMethodInfo;

/**
 * Processes all the methods of a class to weave in persistence code such as,
 * lazy value holder, change tracking and fetch groups.
 *
 * For FIELD access, changes references to GETFIELD and PUTFIELD to call weaved get/set methods.
 *
 * For Property access, modifies the getters and setters.
 *
 */

public class MethodWeaver extends EclipseLinkMethodVisitor {

    protected ClassWeaver tcw;
    protected String methodName;
    protected String methodDescriptor = null;

    /** Determines if we are at the first line of a method. */
    protected boolean methodStarted = false;

    public MethodWeaver(ClassWeaver tcw, String methodName, String methodDescriptor, MethodVisitor mv) {
        super(mv);
        this.setCustomMethodVisitor(this);
        this.tcw = tcw;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void visitInsn (final int opcode) {
        weaveBeginningOfMethodIfRequired();
        if (opcode == Opcodes.RETURN) {
            weaveEndOfMethodIfRequired();
        }
        super.visitInsnSuper(opcode);
    }

    @Override
    public void visitIntInsn (final int opcode, final int operand) {
        weaveBeginningOfMethodIfRequired();
        super.visitIntInsnSuper(opcode, operand);
    }

    @Override
    public void visitVarInsn (final int opcode, final int var) {
        weaveBeginningOfMethodIfRequired();
        super.visitVarInsnSuper(opcode, var);
    }

    @Override
    public void visitTypeInsn (final int opcode, final String desc) {
        weaveBeginningOfMethodIfRequired();
        super.visitTypeInsnSuper(opcode, desc);
    }

    @Override
    public void visitFieldInsn (final int opcode, final String owner, final String name, final String desc) {
        weaveBeginningOfMethodIfRequired();
        weaveAttributesIfRequired(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn (final int opcode, final String owner, final String name, final String desc, boolean intf) {
        weaveBeginningOfMethodIfRequired();
        String descClassName = "";
        if (desc.length() > 3){
            descClassName = desc.substring(3, desc.length()-1);
        }
        // Need to find super.clone and add _persistence_post_clone(clone).
        if (this.tcw.classDetails.shouldWeaveInternal() && name.equals("clone") &&
                /* the following will return true if we are calling a method stored on our direct superclass or one of its superclasses
                 * that is involved in our metadata hierarchy and if there are no classes farther up the hierarchy that implement a clone method
                 * The goal is to call _persistence_post_clone() at the highest level in the hierarchy possible
                 * For completeness, we check to ensure the return type is in that same hierarchy */
                this.tcw.classDetails.isInSuperclassHierarchy(owner) && this.tcw.classDetails.isInMetadataHierarchy(descClassName) &&
                (this.tcw.classDetails.getNameOfSuperclassImplementingCloneMethod() == null)) {
            super.visitMethodInsnSuper(opcode, owner, name, desc, intf);
            super.visitTypeInsnSuper(Opcodes.CHECKCAST, this.tcw.classDetails.getClassName());
            super.visitMethodInsnSuper(Opcodes.INVOKEVIRTUAL, this.tcw.classDetails.getClassName(), "_persistence_post_clone", "()Ljava/lang/Object;", false);
        } else {
            super.visitMethodInsnSuper(opcode, owner, name, desc, intf);
        }
    }

    @Override
    public void visitJumpInsn (final int opcode, final Label label) {
        weaveBeginningOfMethodIfRequired();
        super.visitJumpInsnSuper(opcode, label);
    }

    @Override
    public void visitLabel (final Label label) {
        weaveBeginningOfMethodIfRequired();
        super.visitLabelSuper(label);
    }

    @Override
    public void visitLdcInsn (final Object cst) {
        weaveBeginningOfMethodIfRequired();
        super.visitLdcInsnSuper(cst);
    }

    @Override
    public void visitIincInsn (final int var, final int increment) {
        weaveBeginningOfMethodIfRequired();
        super.visitIincInsnSuper(var, increment);
    }

    @Override
    public void visitTableSwitchInsn (final int min, final int max, final Label dflt, final Label... labels) {
        weaveBeginningOfMethodIfRequired();
        super.visitTableSwitchInsnSuper(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn (final Label dflt, final int keys[], final Label labels[]) {
        weaveBeginningOfMethodIfRequired();
        super.visitLookupSwitchInsnSuper(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn (final String desc, final int dims) {
        weaveBeginningOfMethodIfRequired();
        super.visitMultiANewArrayInsnSuper(desc, dims);
    }

    @Override
    public void visitTryCatchBlock (final Label start, final Label end,final Label handler, final String type) {
        weaveBeginningOfMethodIfRequired();
        super.visitTryCatchBlockSuper(start, end, handler, type);
    }

    @Override
    public void visitMaxs (final int maxStack, final int maxLocals) {
        weaveBeginningOfMethodIfRequired();
        super.visitMaxsSuper(maxStack, maxLocals);
    }

    @Override
    public void visitLocalVariable (final String name, final String desc, String signature, final Label start, final Label end, final int index) {
        weaveBeginningOfMethodIfRequired();
        super.visitLocalVariableSuper(name, desc, signature, start, end, index);
    }

    @Override
    public void visitLineNumber (final int line, final Label start) {
        weaveBeginningOfMethodIfRequired();
        super.visitLineNumberSuper(line, start);
    }

    @Override
    public void visitAttribute (final Attribute attr) {
        weaveBeginningOfMethodIfRequired();
        super.visitAttributeSuper(attr);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotationSuper(desc, visible);
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }

    /**
     * Change GETFIELD and PUTFIELD for fields that use attribute access to make use of new convenience methods.
     *
     * A GETFIELD for an attribute named 'variableName' will be replaced by a call to:
     *
     * _persistence_get_variableName()
     *
     * A PUTFIELD for an attribute named 'variableName' will be replaced by a call to:
     *
     * _persistence_set_variableName(variableName)
     */
    public void weaveAttributesIfRequired(int opcode, String owner, String name, String desc) {
        AttributeDetails attributeDetails = tcw.classDetails.getAttributeDetailsFromClassOrSuperClass(name);
        if ((attributeDetails == null) || (!attributeDetails.hasField()) || (!this.tcw.classDetails.isInMetadataHierarchy(owner))) {
            super.visitFieldInsnSuper(opcode, owner, name, desc);
            return;
        }
        if (opcode == Opcodes.GETFIELD) {
            if (attributeDetails.weaveValueHolders() || tcw.classDetails.shouldWeaveFetchGroups()) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_GET + name, "()" + attributeDetails.getReferenceClassType().getDescriptor(), false);
            } else {
                super.visitFieldInsnSuper(opcode, owner, name, desc);
            }
        } else if (opcode == Opcodes.PUTFIELD) {
            if ((attributeDetails.weaveValueHolders()) || (tcw.classDetails.shouldWeaveChangeTracking()) || (tcw.classDetails.shouldWeaveFetchGroups())) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_SET + name, "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")V", false);
            } else {
                super.visitFieldInsnSuper(opcode, owner, name, desc);
            }
        }  else {
            super.visitFieldInsnSuper(opcode, owner, name, desc);
        }
    }

    /**
     * Makes modifications to the beginning of a method.
     *
     * 1. Modifies getter method for attributes using property access
     *
     * In a getter method for 'attributeName', the following lines are added at the beginning of the method
     *
     *  _persistence_checkFetched("attributeName");
     *  _persistence_initialize_attributeName_vh();
     *  if (!_persistence_attributeName_vh.isInstantiated()) {
     *      PropertyChangeListener temp_persistence_listener = _persistence_listener;
     *      _persistence_listener = null;
     *      setAttributeName((AttributeType)_persistence_attributeName_vh.getValue());
     *      _persistence_listener = temp_persistence_listener;
     *  }
     *
     *  2. Modifies setter methods to store old value of attribute
     *  If weaving for fetch groups:
     *
     *  // if weaving for change tracking:
     *  if(_persistence_listener != null)
     *      // for Objects
     *      AttributeType oldAttribute = getAttribute()
     *      // for primitives
     *      AttributeWrapperType oldAttribute = new AttributeWrapperType(getAttribute());
     *          e.g. Double oldAttribute = Double.valueOf(getAttribute());
     *  else
     *      _persistence_checkFetchedForSet("attributeName");
     *  _persistence_propertyChange("attributeName", oldAttribute, argument);
     *
     *  otherwise (not weaving for fetch groups):
     *
     *      // for Objects
     *      AttributeType oldAttribute = getAttribute()
     *      // for primitives
     *      AttributeWrapperType oldAttribute = new AttributeWrapperType(getAttribute());
     *          e.g. Double oldAttribute = Double.valueOf(getAttribute());
     *  _persistence_propertyChange("attributeName", oldAttribute, argument);
     *
     *  // if not weaving for change tracking, but for fetch groups only:
     *  _persistence_checkFetchedForSet("attributeName");
     *
     *  3. Modifies getter Method for attributes using virtual access
     *
     *  add: _persistence_checkFetched(name);
     *
     *  4. Modifies setter Method for attributes using virtual access
     *
     *  add code of the following form:
     *
     *   Object obj = null;
     *   if(_persistence_listener != null){
     *      obj = get(name);
     *   } else {
     *       _persistence_checkFetchedForSet(name);
     *   }
     *   _persistence_propertyChange(name, obj, value);
     *
     *   _persistence_checkFetchedForSet(name) call will be excluded if weaving of fetch groups is not enabled
     *
     *   _persistence_propertyChange(name, obj, value); will be excluded if weaving of change tracking is not enabled
     */
    public void weaveBeginningOfMethodIfRequired() {
        if (this.methodStarted){
            return;
        }
        // Must set immediately, as weaving can trigger this method.
        this.methodStarted = true;
        boolean isVirtual = false;
        AttributeDetails attributeDetails = tcw.classDetails.getGetterMethodToAttributeDetails().get(methodName);
        boolean isGetMethod = (attributeDetails != null) && (this.methodDescriptor.startsWith("()") ||
                (attributeDetails.isVirtualProperty() && this.methodDescriptor.startsWith("(" + ClassWeaver.STRING_SIGNATURE +")")));

        String attributeName = null;
        String referenceClassName = null;
        String setterMethodName = null;
        Type referenceClassType = null;
        String getterMethodName = null;
        int valueHoldingLocation = 1;
        int valueStorageLocation = 2;

        if (attributeDetails == null){
            VirtualAttributeMethodInfo info = tcw.classDetails.getInfoForVirtualGetMethod(methodName);
            if ((info != null) && this.methodDescriptor.equals(ClassWeaver.VIRTUAL_GETTER_SIGNATURE) ){
                isGetMethod = true;
                isVirtual = true;
                referenceClassName = "java.lang.Object";
                setterMethodName = info.getSetMethodName();
                referenceClassType = Type.getType(ClassWeaver.OBJECT_SIGNATURE);
                getterMethodName = methodName;
            }
        } else {
            attributeName = attributeDetails.getAttributeName();
            referenceClassName = attributeDetails.getReferenceClassName();
            setterMethodName = attributeDetails.getSetterMethodName();
            referenceClassType = attributeDetails.getReferenceClassType();
            getterMethodName = attributeDetails.getGetterMethodName();
            isVirtual = attributeDetails.isVirtualProperty();
        }
        if (isVirtual){
            valueHoldingLocation = 2;
            valueStorageLocation = 3;
        }
        if (isVirtual || (isGetMethod && !attributeDetails.hasField())) {
            if (tcw.classDetails.shouldWeaveFetchGroups()) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                if (isVirtual){
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                } else {
                    methodVisitor.visitLdcInsn(attributeName);
                }
                // _persistence_checkFetched("attributeName");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_checkFetched", "(Ljava/lang/String;)V", false);
            }
            if (!isVirtual && attributeDetails.weaveValueHolders()) {
                // _persistence_initialize_attributeName_vh();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_initialize_" + attributeName + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

                // if (!_persistence_attributeName_vh.isInstantiated()) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_FIELDNAME_PREFIX + attributeName + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "isInstantiated", "()Z", true);
                Label l0 = ASMFactory.createLabel();
                methodVisitor.visitJumpInsn(Opcodes.IFNE, l0);

                // Need to disable change tracking when the set method is called to avoid thinking the attribute changed.
                if (tcw.classDetails.shouldWeaveChangeTracking()) {
                    // PropertyChangeListener temp_persistence_listener = _persistence_listener;
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), "_persistence_listener", ClassWeaver.PCL_SIGNATURE);
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, 4);
                    // _persistence_listener = null;
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, tcw.classDetails.getClassName(), "_persistence_listener", ClassWeaver.PCL_SIGNATURE);
                }
                // setAttributeName((AttributeType)_persistence_attributeName_vh.getValue());
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_FIELDNAME_PREFIX + attributeName + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "getValue", "()Ljava/lang/Object;", true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, referenceClassName.replace('.','/'));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), setterMethodName, "(" + referenceClassType.getDescriptor() + ")V", false);

                if (tcw.classDetails.shouldWeaveChangeTracking()) {
                    // _persistence_listener = temp_persistence_listener;
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 4);
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, tcw.classDetails.getClassName(), "_persistence_listener", ClassWeaver.PCL_SIGNATURE);
                }
                // }
                methodVisitor.visitLabel(l0);
            }
        } else {
            attributeDetails = tcw.classDetails.getSetterMethodToAttributeDetails().get(methodName);
            boolean isSetMethod = (attributeDetails != null) && this.methodDescriptor.equals(attributeDetails.getSetterMethodSignature());
            if (attributeDetails == null){
                VirtualAttributeMethodInfo info = tcw.classDetails.getInfoForVirtualSetMethod(methodName);
                if (info != null && this.methodDescriptor.equals(ClassWeaver.VIRTUAL_GETTER_SIGNATURE) ){
                    isGetMethod = true;
                    isVirtual = true;
                    referenceClassName = "java.lang.Object";
                    setterMethodName = methodName;
                    referenceClassType = Type.getType(ClassWeaver.OBJECT_SIGNATURE);
                    getterMethodName = info.getGetMethodName();
                }
            } else {
                attributeName = attributeDetails.getAttributeName();
                referenceClassName = attributeDetails.getReferenceClassName();
                setterMethodName = attributeDetails.getSetterMethodName();
                referenceClassType = attributeDetails.getReferenceClassType();
                getterMethodName = attributeDetails.getGetterMethodName();
                isVirtual = attributeDetails.isVirtualProperty();
            }
            if (isVirtual){
                valueHoldingLocation = 2;
                valueStorageLocation = 3;
            }
            if (isVirtual || (isSetMethod  && !attributeDetails.hasField())) {
                if(tcw.classDetails.shouldWeaveChangeTracking()) {
                    if(tcw.classDetails.shouldWeaveFetchGroups()) {
                        // if this is a primitive, get the wrapper class
                        String wrapper = ClassWeaver.wrapperFor(referenceClassType.getSort());

                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        if (wrapper != null){
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, valueStorageLocation + 1);
                        } else {
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, valueStorageLocation);
                        }
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), "_persistence_listener", "Ljava/beans/PropertyChangeListener;");
                        Label l0 = ASMFactory.createLabel();
                        methodVisitor.visitJumpInsn(Opcodes.IFNULL, l0);

                        /**
                         * The code below constructs the following code
                         *
                         * AttributeType oldAttribute = getAttribute() // for Objects
                         *
                         * AttributeWrapperType oldAttribute = new AttributeWrapperType(getAttribute()); // for primitives
                         */
                        // 1st part of invoking constructor for primitives to wrap them
                        if (wrapper != null) {
                            methodVisitor.visitTypeInsn(Opcodes.NEW, wrapper);
                            methodVisitor.visitInsn(Opcodes.DUP);
                        }

                        // Call the getter
                        // getAttribute()
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        String getterArgument = "";
                        String getterReturn = referenceClassType.getDescriptor();
                        if (isVirtual){
                            getterArgument = ClassWeaver.STRING_SIGNATURE;
                            getterReturn = ClassWeaver.OBJECT_SIGNATURE;
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), getterMethodName, "(" + getterArgument + ")" + getterReturn, false);
                        if (wrapper != null){
                            // 2nd part of using constructor.
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + referenceClassType.getDescriptor() + ")V", false);
                            methodVisitor.visitVarInsn(Opcodes.ASTORE,  valueStorageLocation + 1);
                        } else {
                            // store the result
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, valueStorageLocation);
                        }

                        Label l1 = ASMFactory.createLabel();
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, l1);
                        methodVisitor.visitLabel(l0);
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

                        if (isVirtual){
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        } else {
                            methodVisitor.visitLdcInsn(attributeName);
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_checkFetchedForSet", "(Ljava/lang/String;)V", false);
                        methodVisitor.visitLabel(l1);

                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

                        if (isVirtual){
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        } else {
                            methodVisitor.visitLdcInsn(attributeName);
                        }

                        if (wrapper != null) {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, valueStorageLocation + 1);
                            methodVisitor.visitTypeInsn(Opcodes.NEW, wrapper);
                            methodVisitor.visitInsn(Opcodes.DUP);
                        } else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, valueStorageLocation);
                        }
                        // get an appropriate load opcode for the type
                        int opcode = referenceClassType.getOpcode(Opcodes.ILOAD);
                        methodVisitor.visitVarInsn(opcode, valueHoldingLocation);
                        if (wrapper != null){
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + referenceClassType.getDescriptor() + ")V", false);
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_propertyChange", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false);
                    } else {
                        // tcw.classDetails.shouldWeaveFetchGroups()
                        /**
                         * The code below constructs the following code
                         *
                         * AttributeType oldAttribute = getAttribute() // for Objects
                         *
                         * AttributeWrapperType oldAttribute = new AttributeWrapperType(getAttribute()); // for primitives
                         */
                        // if this is a primitive, get the wrapper class
                        String wrapper = ClassWeaver.wrapperFor(referenceClassType.getSort());

                        // 1st part of invoking constructor for primitives to wrap them
                        if (wrapper != null) {
                            methodVisitor.visitTypeInsn(Opcodes.NEW, wrapper);
                            methodVisitor.visitInsn(Opcodes.DUP);
                        }

                        // Call the getter
                        // getAttribute()
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        String getterArgument = "";
                        String getterReturn = referenceClassType.getDescriptor();
                        if (isVirtual){
                            getterArgument = ClassWeaver.STRING_SIGNATURE;
                            getterReturn = ClassWeaver.OBJECT_SIGNATURE;
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), getterMethodName, "(" + getterArgument + ")" + getterReturn, false);
                        if (wrapper != null){
                            // 2nd part of using constructor.
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + attributeDetails.getReferenceClassType().getDescriptor() + ")V", false);
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, valueStorageLocation + 1);
                        } else {
                            // store the result
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, valueStorageLocation);
                        }

                        // makes use of the value stored in weaveBeginningOfMethodIfRequired to call property change method
                        // _persistence_propertyChange("attributeName", oldAttribute, argument);
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        if (isVirtual){
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        } else {
                            methodVisitor.visitLdcInsn(attributeName);
                        }
                        if (wrapper != null) {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, valueStorageLocation + 1);
                            methodVisitor.visitTypeInsn(Opcodes.NEW, wrapper);
                            methodVisitor.visitInsn(Opcodes.DUP);
                        } else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, valueStorageLocation);
                        }
                        int opcode = referenceClassType.getOpcode(Opcodes.ILOAD);
                        methodVisitor.visitVarInsn(opcode, valueHoldingLocation);
                        if (wrapper != null) {
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapper, "<init>", "(" + referenceClassType.getDescriptor() + ")V", false);
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_propertyChange", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false);
                    }
                } else {
                    // !tcw.classDetails.shouldWeaveChangeTracking()
                    if(tcw.classDetails.shouldWeaveFetchGroups()) {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        if (isVirtual){
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        } else {
                            methodVisitor.visitLdcInsn(attributeName);
                        }
                        // _persistence_checkFetchedForSet("variableName");
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_checkFetchedForSet", "(Ljava/lang/String;)V", false);
                    }
                }
            }
        }
    }

    /**
     * Modifies methods just before the return.
     *
     * In a setter method for a LAZY mapping, for 'attributeName', the following lines are added at the end of the method.
     *
     *  _persistence_initialize_attributeName_vh();
     *  _persistence_attributeName_vh.setValue(argument);
     *  _persistence_attributeName_vh.setIsCoordinatedWithProperty(true);
     *
     * In a setter method for a non-LAZY mapping, the followings lines are added if change tracking is activated:
     *
     *  _persistence_propertyChange("attributeName", oldAttribute, argument);
     *
     *  Note: This code will wrap primitives by adding a call to the primitive constructor.
     */
    public void weaveEndOfMethodIfRequired() {
        AttributeDetails attributeDetails = tcw.classDetails.getSetterMethodToAttributeDetails().get(methodName);
        boolean isSetMethod = (attributeDetails != null) && this.methodDescriptor.equals(attributeDetails.getSetterMethodSignature());
        if (isSetMethod  && !attributeDetails.hasField()) {
            if (attributeDetails.weaveValueHolders()) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tcw.classDetails.getClassName(), "_persistence_initialize_" + attributeDetails.getAttributeName() + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, "()V", false);

                //_persistence_attributeName_vh.setValue(argument);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_FIELDNAME_PREFIX + attributeDetails.getAttributeName() + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "setValue", "(Ljava/lang/Object;)V", true);

                //  _persistence_attributeName_vh.setIsCoordinatedWithProperty(true);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, tcw.classDetails.getClassName(), ClassWeaver.PERSISTENCE_FIELDNAME_PREFIX + attributeDetails.getAttributeName() + ClassWeaver.PERSISTENCE_FIELDNAME_POSTFIX, ClassWeaver.VHI_SIGNATURE);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ClassWeaver.VHI_SHORT_SIGNATURE, "setIsCoordinatedWithProperty", "(Z)V", true);
            }
        }
    }

}
