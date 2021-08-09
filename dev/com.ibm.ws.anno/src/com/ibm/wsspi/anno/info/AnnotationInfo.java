/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.anno.info;

import java.util.List;

/**
 * <p>Type for annotation instances.</p>
 * 
 * <p>Targeting notes:</p>
 * 
 * <p>Annotations may be attached to packages, to classes, to fields, to methods, and to
 * method parameters.</p>
 * 
 * <p>Particular rules are used to associate annotations to entities in addition to
 * the declaring entity. There are particular rules for inherited methods and fields,
 * particular rules for class and method annotations marked with the inherited meta-annotations,
 * and particular rules for class annotations which have application to methods and fields.</p>
 * 
 * <p>A method or a field annotation not only applies to the method or field which
 * declares the annotation, in the class which declares the method or field, but also apply
 * to the method or field in any subclass which inherites the method or field, but which does
 * not provide a new implementation.</p>
 * 
 * <p>A class annotation which has the inherited meta-annotation not only applies to the class
 * which declared that annotation, but also applies to subclasses of that declaring class. A
 * method annotation which has the inherited meta-annotation not only applies to the method in
 * the class which declares the method and the annotation, but applies as well to the method in
 * any subclasses of the declaring class.</p>
 * 
 * <p>A class annotation which is specified to target fields (or methods), and which is declared
 * on a class has applications to declared fields (or declared methods) of the declaring class.
 * The general rule is to apply the class annotation to declared fields (or declared methods).
 * There are special cases where the general rule is modified to limit application to specific
 * fields (or methods).</p>
 */
public interface AnnotationInfo {
    /**
     * <p>Naming constant: The prefix for the <code>java.lang.annotation</code> package,
     * (with a trailing ".").</p>
     */
    public static final String JAVA_LANG_ANNOTATION_CLASS_PREFIX = "java.lang.annotation.";

    /** <p>Text for the class name of the <code>inherited</code> meta-annotation.</p> */
    public String JAVA_LANG_ANNOTATION_INHERITED = "java.lang.annotation.Inherited";

    /** <p>Naming constant: The qualified name of <code>java.lang.annotation.Annotation</code>.</p> */
    public String ANNOTATION_CLASS_NAME = "java.lang.annotation.Annotation";

    //

    /**
     * <p>Answer a print string for the receiver, for use in debugging. The value is
     * guaranteed to be unique during the lifetime of the receiver, and, for frequently
     * created types, will be created on demand.</p>
     * 
     * @return A print string for the receiver.
     */
    public String getHashText();

    /**
     * <p>Answer the store which holds this info object.</p>
     * 
     * @return The store which holds this info object.
     */
    public InfoStore getInfoStore();

    /**
     * <p>Answer the fully qualified name of the annotation class of this
     * annotation occurrence.</p>
     * 
     * @return The fully qualified name of the annotation class of this annotation
     *         occurrence.
     * 
     * @see #getAnnotationClassInfo()
     */
    public String getAnnotationClassName();

    /**
     * <p>Answer the class of the annotation type of this annotation instance.</p>
     * 
     * <p>If attached to a class, the result is very much <strong>not</strong>
     * that class. If attached to a method or field, this is not the class of
     * the method or field.</p>
     * 
     * @return The class info for the annotation type of this annotation instance.
     * 
     * @see #getAnnotationClassName()
     */
    public ClassInfo getAnnotationClassInfo();

    /**
     * <p>Tell if this annotation has the <code>inherited</code> meta-annotation.</p>
     * 
     * @return True if this annotation has the <code>inherited</code> meta-annotation.
     *         Otherwise, false.
     * 
     * @see #JAVA_LANG_ANNOTATION_INHERITED
     */
    public boolean isInherited();

    //

    /**
     * Tell if the named child value was assigned using the
     * default value from the annotation class.
     * 
     * @param name The name of the child value which is to be tested.
     * @return True if the child value was defaulted. Otherwise, false.
     */
    boolean isValueDefaulted(String name);

    /**
     * Retrieves a named value associated with this AnnotationInfo object.
     * 
     * @param name a String containing the named value to retrieve.
     * @return an AnnotationValue object containing the annotation value with
     *         the given name, or null if the value does not exist.
     */
    public AnnotationValue getValue(String name);

    /**
     * Retrieves a named value associated with this AnnotationInfo object. WITHOUT defaults processing.
     * 
     * ALL CALLS SHOULD GO THROUGH getValue(String name) UNLESS a viable parent classloader is unavailable.
     * In this case, resolution of classes of the form javax.annotation.etc... will not resolve, which may cause
     * error messages or other exceptions. This returns ONLY the annotation values explicitly defined on the class,
     * or null otherwise.
     * 
     * @param name a String containing the named value to retrieve.
     * @return an AnnotationValue object containing the annotation value with
     *         the given name, or null if the value does not exist.
     */
    public AnnotationValue getCachedAnnotationValue(String name);

    //

    /**
     * <p>Answer a member value of this annotation as an annotation.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as an annotation.
     */
    public AnnotationInfo getAnnotationValue(String name);

    /**
     * <p>Answer a member value of this annotation as an array.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as an array.
     */
    public List<? extends AnnotationValue> getArrayValue(String name);

    /**
     * <p>Answer a member value of this annotation as a boolean object.</p>
     * 
     * <p>Answer null if the named member is not available. Absent full
     * defaulting support, valid members will return null if no user defined
     * value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a boolean object.
     */
    public Boolean getBoolean(String name);

    /**
     * <p>Answer a member value of this annotation as a boolean value.</p>
     * 
     * <p>A <code>NullPointerException</code> will occur is the named value
     * is not available.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a boolean value.
     */
    public boolean getBooleanValue(String name);

    /**
     * <p>Answer a member value of this annotation as a byte object.</p>
     * 
     * <p>Answer null if the named member is not available. Absent full
     * defaulting support, valid members will return null if no user defined
     * value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a byte object.
     */
    public Byte getByte(String name);

    /**
     * <p>Answer a member value of this annotation as a byte value.</p>
     * 
     * <p>A <code>NullPointerException</code> will occur is the named value
     * is not available.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a byte value.
     */
    public byte getByteValue(String name);

    /**
     * <p>Answer a member value of this annotation as a character object.</p>
     * 
     * <p>Answer null if the named member is not available. Absent full
     * defaulting support, valid members will return null if no user defined
     * value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a character object.
     */
    public Character getCharacter(String name);

    /**
     * <p>Answer a member value of this annotation as a char value.</p>
     * 
     * <p>A <code>NullPointerException</code> will occur is the named value
     * is not available.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation as a char value.
     */
    public char getCharValue(String name);

    /**
     * <p>Answer a member value of this annotation as a class name (for a class reference value).</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return The member value as the class name of a class reference value.
     */
    public String getClassNameValue(String name);

    /**
     * <p>Answer a member value of this annotation as a double object.</p>
     * 
     * <p>Answer null if the named member is not available. Absent full
     * defaulting support, valid members will return null if no user defined
     * value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation a double object.
     */
    public Double getDouble(String name);

    /**
     * <p>Answer a member value of this annotation as a simple double (double
     * precision floating point) value.</p>
     * 
     * <p>A <code>NullPointerException</code> will occur is the named value
     * is not available.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return A member value of this annotation a double value.
     */
    public double getDoubleValue(String name);

    /**
     * <p>Answer the name of the class of a stored enumerated member value.</p>
     * 
     * <p>Answer null if the named member is not available or has a null
     * value. Absent full defaulting support, valid members will return null if
     * no user defined value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return The name of the class of a stored enumerated member value.
     * 
     * @see #getEnumValue(String)
     */
    public String getEnumClassName(String name);

    /**
     * <p>Answer the name of a stored enumerated member value.</p>
     * 
     * <p>Answer null if the named member is not available or has a null
     * value. Absent full defaulting support, valid members will return null if
     * no user defined value was provided.</p>
     * 
     * @param name The name of the member value to retrieve.
     * 
     * @return The name of a stored enumerated member value.
     * 
     * @see #getEnumClassName(String)
     */
    public String getEnumValue(String name);
}