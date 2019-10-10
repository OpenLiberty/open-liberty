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

import java.lang.annotation.Annotation;
import java.util.Collection;

import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>Base class for info objects.</p>
 * 
 * <p>The purpose of the class information layer is to represent
 * java objects (packages, classes, methods, and fields), and to
 * provide a way to associate annotations to those java objects.</p>
 * 
 * <p>The layer provides data structures that are very similar to
 * structures provided by java reflection. However, the implementation
 * avoids java class resolution, and is tuned for fast and efficient
 * annotation processing.</p>
 * 
 * <p>Several types are provided that represent java objects --
 * as a replacement for java reflection:</p>
 * 
 * {@link PackageInfo} {@link ClassInfo} {@link MethodInfo} {@link FieldInfo}
 * 
 * <p>Several types are provided that represent annotations and
 * annotation values:</p>
 * 
 * {@link AnnotationInfo} {@link AnnotationValue}
 * 
 * <p>A single type is provided to represent a simplified method
 * descriptor. This is used for fast method lookups:</p>
 * 
 * {@link MethodInfoDescriptorInterface}
 * 
 * <p>A number of implementation details are surfaced through the
 * interface. That is, classes are further typed as array and primitive
 * classes. Also, the class interface exposes limited knowledge of
 * the persistence state of a particular class, as expressed through
 * proxy and non-proxy classes.</p>
 * 
 * <p>Several operations provide parameter based variations. Most typical,
 * an operation may provide a variation that accepts a string parameter,
 * another variation that accepts a class object parameter, and a third variation
 * that access a java class parameter. For example, {@link #isAnnotationPresent(String)}, {@link #isAnnotationPresent(ClassInfo), and {@link #isAnnotationPresent(Class)}.
 * These are related by name. That is, the following are equivalent:</p>
 * 
 * <code>
 * isAnnotationPresent( "javax.ejb.EJB" );
 * isAnnotationPresent( classInfo.getName() );
 * isAnnotationPresent( javax.ejb.EJB.class.getName() );
 * </code>
 * 
 * <p>That is, if <code>classInfo</code> is the class object for the class
 * <code>javax.ejb.EJB</code>.</p>
 * 
 * <p>Any info object, including annotation objects, may have annotations. In addition,
 * class objects may have annotations through their methods and fields. An info object
 * may have annotations which were declared on that object, and may have annotations
 * which are present by annotation inheritance.</p>
 * 
 * <p/>
 * 
 * <p>Model notes:</p>
 * 
 * <p>On the relationship of info objects to their annotations:</p>
 * 
 * <pre>
 * [ Info ] 1 -> * [ AnnotationInfo ]
 * I.getDeclaredAnnotations()
 * A.getDeclaringInfo()
 * 
 * [ Info ] + -> * [ AnnotationInfo ]
 * I.getAnnotations()
 * A.getFoundInfo()
 * </pre>
 * 
 * <p>That is, an info has two collections of annotations: A collection
 * of declared annotations, and a collection of all annotations which apply to
 * the info object.</p>
 * 
 * <p>Both relationships are bi-directional, with one difference. Each annotation
 * has exactly one declaring info, but have one or more found info objects.</p>
 * 
 * <p>The declaring info object is always one of the found info objects. Additional
 * found info objects arise because of class inheritance, and because of JSR250
 * application of class annotations to fields and methods.</p>
 * 
 * <pre>
 * I.isDeclaredAnnotationPresent()
 * I.isAnnotationPresent()
 * </pre>
 * 
 * <p>These have a meaning which will be described when the relationship between
 * classes, fields, and methods, is described.</p>
 * 
 * <p>On the hierarchy of info object types:</p>
 * 
 * <pre>
 * [ Info ]
 * [ PackageInfo ] [ ClassInfo ] [ MethodInfo ] [ FieldInfo ]
 * 
 * [ ClassInfo ]
 * [ PrimitiveClassInfo ] [ ArrayClassInfo ] [ NonDelayedClassInfo ] [ DelayedClassInfo ]
 * </pre>
 * 
 * <p>That is, an info may for a package, a class, a method, or a field. A class may be
 * a primitive class, an array class, or either of a non-delayed class or a delayed class.</p>
 * 
 * <p>Non-delayed and delayed classes represent the same class objects, so that this
 * representation provides only three different types of classes: primitive, array, and
 * "other classes not primitive classes or array classes".</p>
 * 
 * <p>Here a class info object may represent either a java interface, or a java class.</p>
 * <pre>
 * [ ClassInfo ] 1 -> * [ MethodInfo ]
 * 
 * C.getDeclaredMethods()
 * M.getDeclaringClass()
 * 
 * [ ClassInfo ] + -> * [ MethodInfo ]
 * 
 * C.getMethods()
 * M.getFoundClasses()
 * </pre>
 * 
 * <p>That is, a class has a (possibly empty) collection of declared methods,
 * and a (possibly empty) collection of methods, which, depending on whether the
 * class info object represents a java interface or a java class, includes
 * either the declared methods plus the declared methods of all super-interfaces
 * (when the class info object represents a java interface), or includes the
 * declared methods plus the declared methods of all super-classes (when the
 * class info object represents a java class).</p>
 * 
 * <p>A key point is that a method has exactly one declaring class, and one
 * or more found classes, corresponding to the classes into which the method
 * is inherited.</p>
 * 
 * <p>The situation for fields is exactly analogous, and is present with no
 * further discussion:</p>
 * 
 * <pre>
 * [ ClassInfo ] 1 -> * [ FieldInfo ]
 * 
 * C.getDeclaredFields()
 * F.getDeclaringClass()
 * 
 * [ ClassInfo ] + -> * [ FieldInfo ]
 * 
 * C.getFields()
 * F.getFoundClasses()
 * </pre>
 * 
 * <p>As info type objects, methods and fields may have annotations, and whether
 * a class has any annotations, including both annotations on the class itself,
 * and including annotations on fields or methods of the class, is a useful
 * property to know. That leads to several new operations:</p>
 * 
 * <pre>
 * C.isMethodAnnotationPresent()
 * C.isFieldAnnotationPresent()
 * </pre>
 */
public interface Info {
    // Logging ...

    /**
     * <p>Answer a print string for the receiver, for use in debugging. The value is
     * guaranteed to be unique during the lifetime of the receiver, and, for frequently
     * created types, will be created on demand.</p>
     * 
     * @return A print string for the receiver.
     */
    public String getHashText();

    /**
     * <p>Log the receiver to the specified logger.</p>
     * 
     * @param logger The logger to receive the display of the receiver.
     */
    public void log(TraceComponent logger);

    //

    // Basic typing ...

    /**
     * <p>Answer the store which holds this info object.</p>
     * 
     * @return The store which holds this info object.
     */
    public InfoStore getInfoStore();

    //

    /**
     * <p>Answer an integer encoding the modifiers (for example,
     * <code>public</code>, <code>protected</code>, or <code>private</code>)
     * of the receiver. The integer encoding uses the bit-field values
     * defined by {@link java.lang.reflect.Modifier}.</p>
     * 
     * @return The integer encoding the receiver's modifiers.
     * 
     * @see java.lang.reflect.Modifier#PUBLIC
     * @see java.lang.reflect.Modifier#PRIVATE
     * @see java.lang.reflect.Modifier#PROTECTED
     * @see java.lang.reflect.Modifier#STATIC
     * @see java.lang.reflect.Modifier#FINAL
     * @see java.lang.reflect.Modifier#SYNCHRONIZED
     * @see java.lang.reflect.Modifier#VOLATILE
     * @see java.lang.reflect.Modifier#TRANSIENT
     * @see java.lang.reflect.Modifier#NATIVE
     * @see java.lang.reflect.Modifier#INTERFACE
     * @see java.lang.reflect.Modifier#ABSTRACT
     * @see java.lang.reflect.Modifier#STRICT
     */
    public int getModifiers();

    /**
     * <p>Tell if this info object was declared with the <code>public</code> modifier.</p>
     * 
     * @return True if this info object was declared as public. Otherwise, false.
     * 
     * @see #getModifiers()
     * @see java.lang.reflect.Modifiers#PUBLIC
     */
    public boolean isPublic();

    /**
     * <p>Tell if this info object was declared with the <code>protected</code> modifier.</p>
     * 
     * @return True if this info object was declared as protected. Otherwise, false.
     * 
     * @see #getModifiers()
     * @see java.lang.reflect.Modifiers#PROTECTED
     */
    public boolean isProtected();

    /**
     * <p>Tell if this info object was declared with the <code>private</code> modifier.</p>
     * 
     * <p>Particular rules apply for the inheritance of private fields or methods. While
     * private fields and methods are not inherited per general java processing, there are
     * cases where javaEE annotations processing is aware of annotations defined on private
     * members on a superclass.</p>
     * 
     * @return True if this info object was declared as private. Otherwise, false.
     * 
     * @see #getModifiers()
     * @see java.lang.reflect.Modifiers#PRIVATE
     */
    public boolean isPrivate();

    /**
     * <p>Tell if this info object was declared with no modifier.</p>
     * 
     * @return True if this info object was declared as package private. Otherwise, false.
     * 
     * @see #getModifiers()
     */
    public boolean isPackagePrivate();

    /**
     * <p>Answer the name of the receiver.</p>
     * 
     * <p>The name of a class, package, or annotation, is the same as the
     * qualified name of the class or package. The name of a field or a
     * method is the name within the enclosing class.</p>
     * 
     * @return The name of the receiver.
     * 
     * @see #getQualifiedName()
     */
    public String getName();

    /**
     * <p>Answer the qualified name of the receiver. This is the same as the
     * regular name, with two specific exceptions:</p>
     * 
     * <ul>
     * <li>For fields, the qualified name is the class
     * name plus "." plus the field name.</li>
     * 
     * <li>For methods, the qualified name is the class
     * name plus "." plus the method name.</li>
     * </ul>
     * 
     * <p>Qualified names provide unique IDs for packages, classes, methods,
     * and fields. The names of annotations are the names of an annotation
     * class, and are not unique.</p>
     * 
     * @return The qualified name of the receiver.
     */
    public String getQualifiedName();

    // Annotation storage ...

    /**
     * <p>Tell if any immediate declared annotations are present.</p>
     * 
     * @return True if any direct declared annotation is present. Otherwise, false.
     */
    public boolean isDeclaredAnnotationPresent();

    /**
     * <p>Answer the collection of annotations of the receiver.</p>
     * 
     * <p>This API does not provide the ability to update the retrieved
     * collection of annotations.</p>
     * 
     * @return The collection of annotations of the receiver.
     */
    public Collection<? extends AnnotationInfo> getDeclaredAnnotations();

    /**
     * <p>Tell if any declared annotation having the specified name is
     * present. (The name of an annotation is the name of the annotation class.</p>
     * 
     * <p>At most one annotation having the specified name may be present.</p>
     * 
     * @param annotationClassName The name of the annotation to detect.
     * 
     * @return True if any declared annotation having the specified name is
     *         present. Otherwise, false.
     */
    public boolean isDeclaredAnnotationPresent(String annotationClassName);

    /**
     * <p>Answer the declared annotation of the receiver which has the specified
     * name.</p>
     * 
     * <p>At most one annotation having the specified name may be present.</p>
     * 
     * <p>Answer null if no matching annotation is found.</p>
     * 
     * @param annotationClassName The name of the declared annotation to retrieve.
     * 
     * @return The declared annotation of the receiver having the specified name.
     *         Null if no matching annotation is present.
     */
    public AnnotationInfo getDeclaredAnnotation(String annotationClassName);

    public AnnotationInfo getDeclaredAnnotation(Class<? extends Annotation> clazz);

    /**
     * <p>Tell if any of the receiver's declared annotations is present in a set of
     * annotations, testing by name.</p>
     * 
     * <p>This method is implemented to iterate across the receiver's annotations,
     * not across the annotation names in the selection set. That tunes the
     * implementation for cases where the expected number of annotations of the
     * receiver is small compared to the number of annotations in the selection set.</p>
     * 
     * @param annotationNames The names of annotations to test against.
     * @return True if any of the receiver's declared annotations is in the selection
     *         set. Otherwise, false.
     */
    public boolean isDeclaredAnnotationWithin(Collection<String> annotationNames);

    //

    /**
     * <p>Tell if any direct annotations are present.</p>
     * 
     * <p>This test detects both declared annotations, and annotations which
     * are present through field or method inheritance, or through annotation
     * inheritance.</p>
     * 
     * @return True if any declared annotation is present. Otherwise, false.
     */
    public boolean isAnnotationPresent();

    /**
     * <p>Answer the collection of annotations of the receiver.</p>
     * 
     * <p>This API does not provide the ability to update the retrieved
     * collection of annotations.</p>
     * 
     * @return The collection of annotations of the receiver.
     */
    public Collection<? extends AnnotationInfo> getAnnotations();

    /**
     * <p>Tell if any annotation having the specified name is
     * present. (The name of an annotation is the name of the annotation class.</p>
     * 
     * <p>At most one annotation having the specified name may be present.</p>
     * 
     * @param annotationClassName The name of the annotation to detect.
     * 
     * @return True if any annotation having the specified name is
     *         present. Otherwise, false.
     */
    public boolean isAnnotationPresent(String annotationClassName);

    /**
     * <p>Answer the annotation of the receiver which has the specified name.</p>
     * 
     * <p>At most one annotation having the specified name may be present.</p>
     * 
     * <p>Answer null if no matching annotation is found.</p>
     * 
     * @param annotationClassName The name of the annotation to retrieve.
     * 
     * @return The annotation of the receiver having the specified name.
     *         Null if no matching annotation is present.
     */
    public AnnotationInfo getAnnotation(String annotationClassName);

    public AnnotationInfo getAnnotation(Class<? extends Annotation> clazz);

    /**
     * <p>Tell if any of the receiver's annotations is present in a set of
     * annotations, testing by name.</p>
     * 
     * <p>This method is implemented to iterate across the receiver's annotations,
     * not across the annotation names in the selection set. That tunes the
     * implementation for cases where the expected number of annotations of the
     * receiver is small compared to the number of annotations in the selection set.</p>
     * 
     * @param annotationNames The names of annotations to test against.
     * @return True if any of the receiver's annotations is in the selection set.
     *         Otherwise, false.
     */
    public boolean isAnnotationWithin(Collection<String> annotationNames);
}
