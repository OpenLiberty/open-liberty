/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

// An annotation visitor is used for all processing of annotation occurrences.
//
// Annotation visitors have a mandated calling sequence:
//
// Annotation Visitor:
//   (visit | visitEnum | visitAnnotation | visitArray)*
//   visitEnd
//
// Annotations occur in these contexts:
//
// 1) As declared on target a package, class, field, or method.
// 2) As a child value of an annotation.
// 3) As an array element of an array typed child value of an annotation.
//
// Annotation values occur:
//
// 1) As a method value for an annotation occurrence.
// 2) As a default value for an annotation method.
//
// A new annotation visitor is constructed for each annotation occurrence.
//
// Each annotation visitor is constructed with a reference to a new
// annotation info object which is to be populated.
//
// The annotation info object initially stores the annotation class name.
//
// For the case (1), for a targeted annotation, the annotation info
// initially stores a reference to the target package, class, field,
// or method.

public abstract class AnnotationVisitorImpl_Info extends AnnotationVisitor {

    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");

    private static final String CLASS_NAME = "AnnotationVisitorImpl_Info";

    protected String hashText;

    public String getHashText() {
        return hashText;
    }

    public String getBaseHashText() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    //

    // Entry from:
    //
    // InfoVisitor.visitAnnotation
    // InfoVisitor.visitParameterAnnotation
    //
    // InfoVisitor_Annotation.visitAnnotation
    //
    // Accept visit calls to assign child values to an annotation info object.
    //
    // That annotation info object holds a name-value mapping which holds
    // method values for an annotation occurrence.
    //
    // A number of visit calls is expected, each providing a name value
    // and providing an encoding of a value which is to be assigned.
    //
    // The name value is expected to match one of the methods of the
    // annotation class name.  (This is not validated.)

    protected AnnotationVisitorImpl_Info(InfoStoreImpl iStore) {
        super(Opcodes.ASM8);
        String methodName = "<init>";

        this.infoStore = iStore;

        if (logger.isLoggable(Level.FINER)) {
            this.hashText = getBaseHashText() + "( " + iStore.getHashText() + " )";
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    // Entry from:
    //
    // InfoVisitor.visitAnnotationDefault
    //
    // Create a new annotation visitor for recording the default value
    // of an annotation method.
    //
    // A single value will be stored to the method info, through the annotation
    // value object of the method info.
    //
    // A single immediate visit call, of the several varieties, is expected.
    //
    // The single visit call provides a name parameter.  This parameter has
    // no meaning and is ignored.

    protected final InfoStoreImpl infoStore;

    //

    // Entry point for storing a value into an annotation.
    //
    // Entry from:
    //
    // visit(String, Object) (primitive value case)
    // visitEnum(String, String, String) (enumeration value case)
    // visitAnnotation(String, String) (child annotation value case)
    // visitArray(String) (array case)
    //
    // Perform storage according to the processing case.
    //
    // Annotation case: Put the value in the method name - annotation value table
    //                  of the annotation.
    //
    //  Method default value case: Put the value as the default value of the method.
    //                             The target method name is ignored.
    //
    // Array case: Add the value as a new element of the annotation value.  The
    //             target method name is ignored.

    protected abstract void storeAnnotationValue(String attributeName, AnnotationValueImpl newAnnotationValue);

    //

    // Visit a primitive value of an annotation occurrence.
    //
    // No new annotation visitor is returned.  No further actions are
    // necessary after storing the value.
    //
    // Primitive types are per the java annotations specification,
    // and include:
    //
    // boolean, char, byte, short, int, float, long, double, and String
    //
    // Primitive types also include class objects.  A class object, as an
    // annotation primitive value, is stored as the class qualified name.
    //
    // 'Object' typed primitive values are possible.  This code handles
    // an object typed primitive value the same as other primitive values,
    // with the caller to retrieve the raw primitive value from the
    // annotation value choosing how to type the retrieved value.

    @Override
    public void visit(String name, Object value) {
        String methodName = "visit";
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Name [ {1} ] Value [ 2 ]",
                        new Object[] { getHashText(), name, value });
        }

        if (value instanceof Type) {
            Type valueAsType = (Type) value;

            // TODO: The value stored as the class name, not as an info object.
            //       For now, let the caller obtain the value as a class info.
            //
            //       Whether to allow resolution of class values as class info
            //       objects is an open question.  No particular value is known
            //       at this time for adding a class info type access API.
            //
            // See the similar code in 'InfoVisitor_DefaultValue.visit(String, Object)'.

            // value = infoStore.getDelayableClassInfo( valueAsType.getClassName() );

            value = valueAsType.getClassName();
        }

        AnnotationValueImpl newAnnotationValue = new AnnotationValueImpl(value);

        storeAnnotationValue(name, newAnnotationValue);
    }

    // Visit an enumeration value of an annotation occurrence.

    @Override
    public void visitEnum(String targetMethodName,
                          String enumerationTypeDescription,
                          String enumerationLiteral) {

        String methodName = "visitEnum";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Name [ {1} ] Description [ {2} ] Value [ {3} ]",
                        new Object[] { getHashText(),
                                       targetMethodName,
                                       enumerationTypeDescription,
                                       enumerationLiteral });
        }

        Type enumerationType = Type.getType(enumerationTypeDescription);
        String enumerationClassName = enumerationType.getClassName();

        // TODO: The value stored as the class name, not as an info object.
        //       For now, let the caller obtain the value as a class info.
        //
        //       Whether to allow resolution of class values as class info
        //       objects is an open question.  No particular value is known
        //       at this time for adding a class info type access API.
        //
        // See the similar code in 'InfoVisitor_DefaultValue.visit(String, Object)'.

        // value = infoStore.getDelayableClassInfo( valueAsType.getClassName() );

        AnnotationValueImpl newAnnotationValue =
                        new AnnotationValueImpl(enumerationClassName, enumerationLiteral);

        storeAnnotationValue(targetMethodName, newAnnotationValue);
    }

    // Visit an annotation child value.

    @Override
    public AnnotationVisitor visitAnnotation(String targetMethodName, String annotationClassDescription) {
        String methodName = "visitAnnotation";
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Annotation Class [ {1} ] Description [ 2 ]",
                    new Object[] { getHashText(),
                                   targetMethodName,
                                   annotationClassDescription });
        }

        Type annotationType = Type.getType(annotationClassDescription);
        String annotationClassName = annotationType.getClassName();

        AnnotationInfoImpl newAnnotationInfo = new AnnotationInfoImpl(annotationClassName, infoStore);

        AnnotationValueImpl newAnnotationValue = new AnnotationValueImpl(newAnnotationInfo);

        storeAnnotationValue(targetMethodName, newAnnotationValue);

        return new AnnotationVisitorImpl_Info.AnnotationInfoVisitor(newAnnotationInfo);
    }

    // Visit an array child value of the enumeration.
    //
    // 'name' tells which method of the annotation is being assigned.
    //
    // Elements of the array are not assigned directly.  A new annotation
    // visitor is created with references to the parent annotation info
    // and with a reference to an annotation value object that will
    // hold the array elements.
    //
    // A new annotation visitor is returned, with a pointer to the same annotation
    // info object currently in use, and with a pointer to the annotation value
    // object which will receive the array element values.

    @Override
    public AnnotationVisitor visitArray(String name) {
        String methodName = "visitArray";
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Name [ {1} ]",
                        new Object[] { getHashText(), name });
        }

        final AnnotationValueImpl arrayValue = new AnnotationValueImpl();
        storeAnnotationValue(name, arrayValue);

        return new AnnotationVisitorImpl_Info(infoStore) {
            @Override
            protected void storeAnnotationValue(String attributeName, AnnotationValueImpl newAnnotationValue) {
                arrayValue.addArrayValue(newAnnotationValue);
            }
        };
    }
    
    //

    public static class AnnotationInfoVisitor extends AnnotationVisitorImpl_Info {
        AnnotationInfoImpl annoInfoImpl;

        protected AnnotationInfoVisitor(AnnotationInfoImpl annotationInfo) {
            super(annotationInfo.getInfoStore());
            annoInfoImpl = annotationInfo;
        }

        public AnnotationInfoImpl getAnnotationInfo() {
            return annoInfoImpl;
        }

        @Override
        protected void storeAnnotationValue(String name, AnnotationValueImpl newAnnotationValue) {
            annoInfoImpl.addAnnotationValue(name, newAnnotationValue);
        }
    }
}
