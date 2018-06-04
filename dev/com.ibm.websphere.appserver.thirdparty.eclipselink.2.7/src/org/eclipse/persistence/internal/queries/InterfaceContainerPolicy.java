/*******************************************************************************
 * Copyright (c) 1998, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.internal.queries;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import org.eclipse.persistence.descriptors.changetracking.CollectionChangeEvent;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedMethodInvoker;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedGetMethod;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.querykeys.QueryKey;

/**
 * <p><b>Purpose</b>: The abstract class for ContainerPolicy's whose container class implements
 * a container interface.
 * <p>
 *
 * @see CollectionContainerPolicy
 * @see MapContainerPolicy
 */
public abstract class InterfaceContainerPolicy extends ContainerPolicy {

    /** The concrete container class. */
    protected Class containerClass;
    protected String containerClassName;

    /** The method which will return a clone of an instance of the containerClass. */
    protected transient Method cloneMethod;

    /**
     * INTERNAL:
     * Construct a new policy.
     */
    public InterfaceContainerPolicy() {
        super();
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class.
     */
    public InterfaceContainerPolicy(Class containerClass) {
        setContainerClass(containerClass);
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class name.
     */
    public InterfaceContainerPolicy(String containerClassName) {
        setContainerClassName(containerClassName);
    }

    /**
     * INTERNAL:
     * Return if the policy is equal to the other.
     * By default if they are the same class, they are considered equal.
     * This is used for query parse caching.
     */
    @Override
    public boolean equals(Object object) {
        return super.equals(object) && getContainerClass().equals(((InterfaceContainerPolicy)object).getContainerClass());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        Class containerClass = getContainerClass();
        result = 31 * result + (containerClass != null ? containerClass.hashCode() : 0);
        return result;
    }

    /**
     * INTERNAL:
     * Return a clone of the specified container.
     */
    @Override
    public Object cloneFor(Object container) {
        if (container == null) {
            return null;
        }

        Method cloneMethod;
        Class javaClass = container.getClass();
        if (javaClass == getContainerClass()) {
            cloneMethod = getCloneMethod();
        } else {
            // container may be a superclass of the concrete container class
            // so we have to use the right clone method...
            cloneMethod = getCloneMethod(javaClass);
        }
        return invokeCloneMethodOn(cloneMethod, container);
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this ContainerPolicy to actual class-based
     * settings. This method is used when converting a project that has been built
     * with class names to a project with classes.
     * @param classLoader
     */
    @Override
    public void convertClassNamesToClasses(ClassLoader classLoader){
        super.convertClassNamesToClasses(classLoader);
        if (getContainerClassName() == null){
            return;
        }
        Class containerClass = null;
        try{
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                try {
                    containerClass = AccessController.doPrivileged(new PrivilegedClassForName(getContainerClassName(), true, classLoader));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.classNotFoundWhileConvertingClassNames(getContainerClassName(), exception.getException());
                }
            } else {
                containerClass = PrivilegedAccessHelper.getClassForName(getContainerClassName(), true, classLoader);
            }
        } catch (ClassNotFoundException exception) {
            throw ValidationException.classNotFoundWhileConvertingClassNames(getContainerClassName(), exception);
        }
        setContainerClass(containerClass);
    }

    /**
     * INTERNAL:
     * Creates a CollectionChangeEvent for the container
     */
    @Override
    public CollectionChangeEvent createChangeEvent(Object collectionOwner, String propertyName, Object collectionChanged, Object elementChanged, int changeType, Integer index, boolean isChangeApplied) {
        return new CollectionChangeEvent(collectionOwner, propertyName, collectionChanged, elementChanged, changeType, index, false, isChangeApplied);// make the remove change event fire.
    }

    /**
     * INTERNAL:
     * Create a query key that links to the map key
     * InterfaceContainerPolicy does not support maps, so this method will return null
     * subclasses will extend this method.
     */
    public QueryKey createQueryKeyForMapKey() {
        return null;
    }

    /**
     * INTERNAL:
     * Return the 'clone()' Method for the container class.
     * Lazy initialization is used, so we can serialize these things.
     */
    public Method getCloneMethod() {
        if (cloneMethod == null) {
            setCloneMethod(getCloneMethod(getContainerClass()));
        }
        return cloneMethod;
    }

    private static final class ClassWeakReference extends WeakReference<Class> {
        private final int hash;

        ClassWeakReference(Class referent) {
            super(referent);
            hash = referent.hashCode();
        }

        ClassWeakReference(Class referent, ReferenceQueue<Class> referenceQueue) {
            super(referent, referenceQueue);
            hash = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof ClassWeakReference) {
                return get() == ((ClassWeakReference) obj).get();
            }

            return false;
        }

        @Override
        public String toString() {
            Class referent = get();
            return new StringBuilder("ClassWeakReference: ").append(referent == null ? null : referent).toString();
        }
    }

    private static final ReferenceQueue<Class> refQueue = new ReferenceQueue<>();
    private static final ConcurrentHashMap<ClassWeakReference, Method> cloneMethodCache = new ConcurrentHashMap<>();
    
    /**
     * INTERNAL:
     * Return the 'clone()' Method for the specified class.
     * Return null if the method does not exist anywhere in the hierarchy
     */
    protected Method getCloneMethod(Class javaClass) {
        for (Object key; (key = refQueue.poll()) != null;) {
            cloneMethodCache.remove(key);
        }
        Method cloneMethod = cloneMethodCache.get(new ClassWeakReference(javaClass));
        if (cloneMethod != null) {
            return cloneMethod;
        }
        try {
            // This must not be set "accessible" - clone() must be public, and some JVM's do not allow access to JDK classes.
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    cloneMethod = AccessController.doPrivileged(new PrivilegedGetMethod(javaClass, "clone", (Class[])null, false));
                } catch (PrivilegedActionException exception) {
                    throw QueryException.methodDoesNotExistInContainerClass("clone", javaClass);
                }
            } else {
                cloneMethod = PrivilegedAccessHelper.getMethod(javaClass, "clone", (Class[])null, false);
            }
        } catch (NoSuchMethodException ex) {
            throw QueryException.methodDoesNotExistInContainerClass("clone", javaClass);
        }

        cloneMethodCache.put(new ClassWeakReference(javaClass, refQueue), cloneMethod);
        return cloneMethod;
    }

    /**
     * INTERNAL:
     * Returns the container class to be used with this policy.
     */
    @Override
    public Class getContainerClass() {
        return containerClass;
    }

    @Override
    public String getContainerClassName() {
        if ((containerClassName == null) && (containerClass != null)) {
            containerClassName = containerClass.getName();
        }
        return containerClassName;
    }

    /**
     * INTERNAL:
     * Return the DatabaseField that represents the key in a DirectMapMapping.  If the
     * keyMapping is not a DirectMapping, this will return null.
     */
    public DatabaseField getDirectKeyField(CollectionMapping mapping) {
        return null;
    }

    public abstract Class getInterfaceType();

    /**
     * INTERNAL:
     * Return whether the iterator has more objects,
     */
    @Override
    public boolean hasNext(Object iterator) {
        return ((Iterator)iterator).hasNext();
    }

    /**
     * INTERNAL:
     * Invoke the specified clone method on the container,
     * handling the necessary exceptions.
     */
    protected Object invokeCloneMethodOn(Method method, Object container) {
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    return AccessController.doPrivileged(new PrivilegedMethodInvoker(method, container, (Object[])null));
                } catch (PrivilegedActionException exception) {
                    Exception throwableException = exception.getException();
                    if (throwableException instanceof IllegalAccessException) {
                        throw QueryException.cannotAccessMethodOnObject(method, container);
                    } else {
                        throw QueryException.methodInvocationFailed(method, container, throwableException);
                    }
                }
            } else {
                return PrivilegedAccessHelper.invokeMethod(method, container, (Object[])null);
            }
        } catch (IllegalAccessException ex1) {
            throw QueryException.cannotAccessMethodOnObject(method, container);
        } catch (InvocationTargetException ex2) {
            throw QueryException.methodInvocationFailed(method, container, ex2);
        }
    }

    /**
     * INTERNAL:
     * Return whether a map key this container policy represents is an attribute
     * By default this method will return false since only subclasses actually represent maps.
     */
    public boolean isMapKeyAttribute(){
        return false;
    }

    /**
     * INTERNAL:
     * Validate the container type.
     */
    @Override
    public boolean isValidContainerType(Class containerType) {
        return Helper.classImplementsInterface(containerType, getInterfaceType());
    }

    /**
     * INTERNAL:
     * Return the next object on the queue.
     * Valid for some subclasses only.
     */
    @Override
    protected Object next(Object iterator) {
        return ((Iterator)iterator).next();
    }

    /**
     * INTERNAL:
     * Set the Method that will return a clone of an instance of the containerClass.
     */
    public void setCloneMethod(Method cloneMethod) {
        this.cloneMethod = cloneMethod;
    }

    /**
     * INTERNAL:
     * Set the class to use as the container.
     */
    @Override
    public void setContainerClass(Class containerClass) {
        this.containerClass = containerClass;
        initializeConstructor();
    }

    @Override
    public void setContainerClassName(String containerClassName) {
        this.containerClassName = containerClassName;
    }

    /**
     * INTERNAL:
     * Return a container populated with the contents of the specified Vector.
     */
    @Override
    public Object buildContainerFromVector(Vector vector, AbstractSession session) {
        // PERF: If a Vector policy just return the original.
        if (this.containerClass == ClassConstants.Vector_class) {
            return vector;
        }
        return super.buildContainerFromVector(vector, session);
    }

    @Override
    protected Object toStringInfo() {
        return getContainerClass();
    }
}
