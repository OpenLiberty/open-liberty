/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
 * <p>Info type used to represent a class.</p>
 */
public interface ClassInfo extends Info {
    // jakarta review:
    // com.ibm.ws.anno.info.internal.ArrayClassInfo.getSuperclassName()
    // com.ibm.ws.anno.info.internal.DelayedClassInfo.getSuperclassName()
    // com.ibm.ws.anno.info.internal.NonDelayedClassInfo.NonDelayedClassInfo(String, InfoStoreImpl)
    // com.ibm.ws.anno.info.internal.NonDelayedClassInfo.NonDelayedClassInfo(String, String, int, String[], boolean, InfoStoreImpl)
    /** <p>Naming constant: The qualified name of <code>java.lang.Object</code>.</p> */
    public static final String OBJECT_CLASS_NAME = "java.lang.Object";

    // jakarta review: No current references
    /** <p>Naming constant: The qualified name of <code>java.io.Serializable</code>.</p> */
    public static final String SERIALIZABLE_CLASS_NAME = "java.io.Serializable";

    // jakarta review: No current references
    /** <p>Naming constant: The qualified name of <code>java.io.Externalizable</code>.</p> */
    public static final String EXTERNALIZABLE_CLASS_NAME = "java.io.Serializable";

    // jakarta review: No current references
    /** <p>Naming constant: The qualified name of <code>java.rmi.RemoteException</code>.</p> */
    public static final String REMOTE_EXCEPTION_CLASS_NAME = "java.rmi.RemoteException";

    // jakarta review: No current references
    /** <p>Naming constant: The qualified name of <code>javax.ejb.EJBException</code>.</p> */
    public static final String EJB_EXCEPTION_CLASS_NAME = "javax.ejb.EJBException";

    // Support for unloadable classes

    /**
     * <p>Tell if this class info was created to represent an unloadable class.
     * Stub values are assigned, and the interface and superclass values are forced.</p>
     * 
     * @return True if this class object is artificial. Otherwise, false.
     */
    public boolean isArtificial();

    // Basic typing.

    /**
     * <p>Tell if this class object represents an array type.</p>
     * 
     * @return True if this class object represents an array type. Otherwise, false.
     */
    public boolean isArray();

    /**
     * <p>Tell if this class object represents a primitive type (for
     * example, <code>int</code>).</p>
     * 
     * @return True if this class object represents a primitive type. Otherwise, false.
     */
    public boolean isPrimitive();

    /**
     * <p>Tell if this class object is implemented as a delayed class (as opposed to
     * a non-delayed class).</p>
     * 
     * <p>A delayed class will have a backing non-delayed class. Whether a class object
     * is returned as a delayed or as a non-delayed class object is a detail of the
     * implementation. However, the expectation is that frequently used classes (for example,
     * classes from <code>java.lang</code> are never returned as delayed classes. Very
     * likely, classes with annotations will not be returned as delayed classes.</p>
     * 
     * @return True if the class object is implemented as a delayed class. False if the
     *         class object is implemented as a non-delayed class.
     * 
     * {@link #isNonDelayedClass}
     */
    public boolean isDelayedClass();

    /**
     * <p>Tell if this class object is implemented as a non-delayed class (as opposed to
     * a delayed class).</p>
     * 
     * @return True if the class object is implemented as a non-delayed class. False if the
     *         class object is implemented as a delayed class.
     * 
     * {@link #isDelayedClass}
     */
    public boolean isNonDelayedClass();

    //

    /**
     * <p>Answer the name of the package of this class object.</p>
     * 
     * <p>Retrieval of the package name is preferred to retrieving the entire
     * package object.</p>
     * 
     * @return The name of the package of this class object.
     * 
     * {@link #getPackage()}
     */
    public String getPackageName();

    /**
     * <p>Answer the package object of this class object.</p>
     * 
     * <p>Retrieval of the package name is preferred to retrieving the entire
     * package object.</p>
     * 
     * @return The package object of this class object.
     * 
     * {@link #getPackageName()}
     */
    public PackageInfo getPackage();

    /**
     * <p>Tell if this class object encodes a distinguished java class. Which java classes
     * are distinguished is an implementation detail. (The current implementation distinguishes
     * all classes under <code>java</code>, as well as all class under <code>javax.ejb</code>
     * and all classes under <code>javax.servlet</code>.) Classes are distinguished
     * to assist in telling which classes are to be delayed.</p>
     * 
     * @return True if this class is a distinguished java class. Otherwise, false.
     */
    public boolean isJavaClass();

    //

    /**
     * <p>Tell if this class object represents an interface (that is, is defined using the
     * keyword <code>interface</code>, as opposed to representing a concrete (although possibly
     * abstract) class.</p>
     * 
     * @return True if this class object represents an interface. Otherwise, false.
     */
    public boolean isInterface();

    //

    /**
     * <p>Answer the names of the immediately declared interfaces of this
     * class object as a set.</p>
     * 
     * @return The names of the immediately declared interfaces of this
     *         class object as a set.
     * 
     * {@link #getInterfaces()}
     */
    public List<String> getInterfaceNames();

    /**
     * <p>Answer the immediately declared interfaces of this class object.</p>
     * 
     * <p>Retrieval of the interface names is preferred to retrieval of the
     * class objects of the interfaces.</p>
     * 
     * @return The names of the immediately declared interfaces of this
     *         class object as a set.
     * 
     * {@link #getInterfaceNames()}
     */
    public List<? extends ClassInfo> getInterfaces();

    /**
     * <p>Tell if this class object is for an annotation class. That is,
     * whether <code>java.lang.annotation.Annotation</code> is one of the
     * interfaces of the class.</p>
     * 
     * @return True if this class object is for an annotation class.
     *         Otherwise, false.
     */
    public boolean isAnnotationClass(); // Derived from the interfaces.

    //

    /**
     * <p>Answer the name of the superclass of this class. An interface
     * has a null superclass name. The root class <code>java.lang.Object</code>
     * has a null superclass name. All other classes have a non-null
     * superclass name.</p>
     * 
     * <p>Retrieval of the superclass name is preferred to retrieving the
     * superclass object.</p>
     * 
     * @return The superclass name of this class. Null for interfaces and for
     *         <code>java.lang.Object</code>
     * 
     * {@link #isInterface()}
     * {@link #getSuperclass()}
     */
    public String getSuperclassName();

    /**
     * <p>Answer the superclass of this class. An interface has a null superclass.
     * The root class <code>java.lang.Object</code> has a null superclass. All other
     * classes have a non-null
     * superclass name.</p>
     * 
     * <p>Retrieval of the superclass name is preferred to retrieving the
     * superclass object.</p>
     * 
     * @return The superclass of this class. Null for interfaces and for
     *         <code>java.lang.Object</code>
     * 
     * {@link #isInterface()}
     * {@link #getSuperclassName()}
     */
    public ClassInfo getSuperclass();

    //

    /**
     * <p>Tell if a variable of this type can be assigned a value having
     * a specified type, as specified by the name of the type.</p>
     * 
     * <p>That is, is the type as represented by this class object
     * coarser (the same or strictly coarser) than the specified type.</p>
     * 
     * <p>Assignment tests are the reverse of instance-of tests. That is,
     * if type X is assignable from type Y, then Y is an instance of X.
     * Conversely, if Y is an instance of X, then X is assignable from Y.</p>
     * 
     * @param className The name of the type which is to be tested.
     * 
     * @return True if a variable of this type can be assigned a value of
     *         the specified type. Otherwise, false.
     * 
     * {@link #isInstanceOf(String)}
     * {@link #isInstanceOf(Class)}
     */
    public boolean isAssignableFrom(String className);

    /**
     * <p>Tell if this type is a sub-type of another type. The other
     * type is specified by the name of the type.</p>
     * 
     * <p>That is, is the type as represented by this class object
     * finer (the same or strictly finer) than the specified type.</p>
     * 
     * <p>Instance-of tests are the reverse of assignment tests. That is,
     * if type X is an instance of type Y, then Y is assignable from of X.
     * Conversely, if Y is assignable from X, then X is an instance of Y.</p>
     * 
     * @param className The name of the type which is to be tested.
     * 
     * @return True if this type is an instance (sub-type) of
     *         the specified type. Otherwise, false.
     * 
     * {@link #isAssignableFrom(String)}
     */
    public boolean isInstanceOf(String className);

    public boolean isInstanceOf(Class<?> clazz);

    /**
     * <p>Answer the declared fields of the receiver.</p>
     * 
     * <p>The result collection does not support additions.</p>
     * 
     * @return The declared fields of the receiver.
o     */
    public List<? extends FieldInfo> getDeclaredFields();

    /**
     * <p>Answer the declared constructors of the receiver.</p>
     * 
     * <p>The result collection does not support additions.</p>
     * 
     * @return The declared constructors of the receiver.
     */
    public List<? extends MethodInfo> getDeclaredConstructors();

    /**
     * <p>Answer the declared methods of the receiver.</p>
     * 
     * <p>The result collection does not support additions.</p>
     * 
     * @return The declared methods of the receiver.
     * 
     * {@link #getMethods()}
     */
    public List<? extends MethodInfo> getDeclaredMethods();

    /**
     * <p>Answer the methods of the receiver. These are the methods,
     * plus any acquired through inheritance.</p>
     * 
     * <p>The result collection does not support additions.</p>
     * 
     * @return The methods of the receiver.
     * 
     * {@link #getDeclaredFields()}
     */
    public List<? extends MethodInfo> getMethods();

    /**
     * <p>Tell if this class object has an annotations on a field.
     * (Annotations other than declared annotations are those which
     * are added as a result of applications of JSR250.)</p>
     * 
     * @return True if any field of this class has an annotation.
     *         Otherwise, false.
     * 
     * {@link #isMethodAnnotationPresent()}
     */
    public boolean isFieldAnnotationPresent();

    /**
     * <p>Tell if this class object has an annotations on a method.
     * (Annotations other than declared annotations are those which
     * are added as a result of applications of JSR250.)</p>
     * 
     * @return True if any method of this class has an annotation.
     *         Otherwise, false.
     * 
     * {@link #isFieldAnnotationPresent()}
     */
    public boolean isMethodAnnotationPresent();
}
