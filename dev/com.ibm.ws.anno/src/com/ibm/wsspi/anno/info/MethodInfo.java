/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
 * <p>Info object type representing a java method.</p>
 * 
 * <p>The name and qualified name of a method info object are different: The
 * qualified name of a field includes the name of the class declaring the method.</p>
 */
public interface MethodInfo extends Info {
    /**
     * <p>Answer the info object of the class which declared this method.</p>
     * 
     * @return The info object of the class which declares this method.
     */
    public ClassInfo getDeclaringClass();

    /**
     * <p>Answer the qualified name of the receiver. For methods, the qualified name is
     * the declaring class name plus <q>.</q> plus the method name.</p>
     * 
     * <p>Qualified names provide unique IDs for packages, classes, methods,
     * and methods. The names of annotations are the names of an annotation
     * class, and are not unique.</p>
     * 
     * @return The qualified name of the receiver.
     */
    @Override
    public String getQualifiedName();

    // Basic method info: Parameters, exceptions, and return type.

    /**
     * <p>Answer the in-order names of the types of the parameters of this method.
     * (Note: These are the parameter <weak>type</weak> names, not the
     * parameter names.)</p>
     * 
     * @return The in-order names of the types of the parameters of this method.
     * 
     * @see #getParameterTypes()
     */
    public List<String> getParameterTypeNames();

    /**
     * <p>Answer the in-order types of the parameters of this method.</p>
     * 
     * <p>The in-order types of the parameters of this method.</p>
     */
    public List<? extends ClassInfo> getParameterTypes();

    /**
     * <p>Answer the set of names of the exceptions which may be thrown by
     * this method.</p>
     * 
     * @return The names of the exceptions which may be thrown by this method,
     *         returned as a set.
     * 
     * @see #getExceptionNames()
     */
    public List<String> getExceptionTypeNames();

    /**
     * <p>Answer the class info objects of the exceptions which may be thrown by
     * this method. (No order of the returned class info objects is guaranteed.)</p>
     * 
     * @return The class info objects of the exceptions which may be thrown by
     *         this method.
     */
    public List<? extends ClassInfo> getExceptionTypes();

    /**
     * <p>Answer the name of the return type of this method.</p>
     * 
     * <p>For a non-primitive non-void return type, the java qualified
     * class name of the return type is returned. For primitive types,
     * a singleton string is returned, per the java descriptor of the
     * return type:</p>
     * 
     * <ul>
     * <li>void: V</li>
     * <li>boolean: Z</li>
     * <li>char: C</li>
     * <li>byte: B</li>
     * <li>short: S</li>
     * <li>int: I</li>
     * <li>float: F</li>
     * <li>long: J</li>
     * <li>double: D</li>
     * </ul>
     * 
     * @return The name of the return type of this method.
     */
    public String getReturnTypeName();

    /**
     * <p>Answer the class info object of the return type of this method.
     * Note that a non-null class info object is returned in all cases,
     * including <code>void</code>. (<code>void.class</code> is a valid
     * java class).</p>
     * 
     * @return The class info object of the return type of this method.
     */
    public ClassInfo getReturnType();

    /**
     * <p>Answer a string encoding the signature of this method. This is
     * a collation of the qualified name of the method, and of a parenthesized
     * and comma delimited list of the types of the parameters of the method.</p>
     * 
     * <p>For example: <code>methodWithEmptyParameters()</code>,
     * <code>methodWithTwoParameters(java.lang.String, myPackage.myClass)</code>.</p>
     * 
     * @return A string encoding the signature of this method.
     */
    public String getSignature();

    public String getDescription();

    //

    /**
     * <p>Answer the in-order parameter annotation collections of
     * parameters of this method.</p>
     * 
     * @return The in-order parameter annotation collections of parameters
     *         of this method.
     */
    public List<List<? extends AnnotationInfo>> getParameterAnnotations();

    //

    /**
     * <p>If this method is on an annotation interface, answer the default value of
     * the method. If no default is defined, or if this method is not on an annotation
     * interface, answer null.</p>
     */
    public AnnotationValue getAnnotationDefaultValue();
}
