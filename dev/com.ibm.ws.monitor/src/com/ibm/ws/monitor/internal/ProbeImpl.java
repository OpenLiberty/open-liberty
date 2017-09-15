/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.monitor.Probe;

/**
 * Implementation of the {@link Probe} interface. This object will hold weak
 * references to the class and method associated with the probe such that
 * listeners can efficiently access the probe site information during processing.
 * <p>
 * This class may be safely used as a key to information cached by a probe
 * listener.
 */
public final class ProbeImpl implements Probe {

    /**
     * The instance initializer name in Java classes. This is used to
     * represent constructors instead of the simple class name.
     */
    private final static String CTOR_METHOD_NAME = "<init>";

    /**
     * The name used to uniquely identify a probe point within a class
     * instance.
     */
    final String name;

    /**
     * A runtime unique identifier for this probe.
     */
    final long identifier;

    /**
     * A reference to the class instance that has been injected with this
     * probe.
     */
    final WeakReference<Class<?>> sourceClassReference;

    /**
     * A reference to the method instance that has been injected with this
     * probe.
     */
    WeakReference<Method> sourceMethodReference;

    /**
     * A reference to the constructor instance that has been injected with
     * this probe.
     */
    WeakReference<Constructor<?>> sourceConstructorReference;

    /**
     * The method name. This is kept separate from the {@link Method} weak reference so we can re-acquire the {@code Method} at runtime
     * if needed.
     */
    final String sourceMethodName;

    /**
     * The method descriptor. This is used to help re-acquire a reference
     * to the {@link Method} after the weak reference is cleared.
     */
    final String sourceMethodDescriptor;

    /**
     * The identifier of the bundle that loaded the probed class or -1 if
     * not loaded by the OSGi framework from a bundle.
     */
    final long sourceBundleId;

    /**
     * Create a probe implementation that is associated with a method.
     * <p>
     * A {@link Method} <em>or</em> {@link Constructor} reference is required.
     * 
     * @param probeManagerImpl the ProbeManagerImpl managing this probe
     * @param clazz the class that has injected with probes that emit events
     * @param name the name of the probe; unique within a class
     * @param ctor the constructor that emits events associated with this probe
     * @param method the method that emits events associated with this probe
     * @param probeKind the attach site for this probe
     */
    ProbeImpl(ProbeManagerImpl probeManagerImpl, Class<?> clazz, String name, Constructor<?> ctor, Method method) {
        this.name = name;
        this.sourceClassReference = new WeakReference<Class<?>>(clazz);
        this.sourceMethodReference = new WeakReference<Method>(method);
        this.sourceConstructorReference = new WeakReference<Constructor<?>>(ctor);
        this.identifier = probeManagerImpl.generateProbeId();

        // Save the descriptor so we can recalculate from the class if needed
        if (method != null) {
            this.sourceMethodName = method.getName();
            this.sourceMethodDescriptor = Type.getMethodDescriptor(method);
        } else if (ctor != null) {
            this.sourceMethodName = CTOR_METHOD_NAME;
            this.sourceMethodDescriptor = Type.getConstructorDescriptor(ctor);
        } else {
            throw new IllegalArgumentException("Method or constructor reference is required");
        }

        Bundle bundle = FrameworkUtil.getBundle(clazz);
        this.sourceBundleId = bundle == null ? -1L : bundle.getBundleId();
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getSourceClass() {
        return sourceClassReference.get();
    }

    /**
     * {@inheritDoc}
     */
    public Method getSourceMethod() {
        Method method = sourceMethodReference.get();
        if (method != null || sourceMethodName == CTOR_METHOD_NAME) {
            return method;
        }
        for (Method m : ReflectionHelper.getDeclaredMethods(getSourceClass())) {
            if (m.getName().equals(sourceMethodName) && Type.getMethodDescriptor(m).equals(sourceMethodDescriptor)) {
                method = m;
                sourceMethodReference = new WeakReference<Method>(method);
                break;
            }
        }
        return method;
    }

    /**
     * {@inheritDoc}
     */
    public Constructor<?> getSourceConstructor() {
        Constructor<?> ctor = sourceConstructorReference.get();
        if (ctor != null || sourceMethodName != CTOR_METHOD_NAME) {
            return ctor;
        }
        for (Constructor<?> c : ReflectionHelper.getDeclaredConstructors(getSourceClass())) {
            if (Type.getConstructorDescriptor(c).equals(sourceMethodDescriptor)) {
                ctor = c;
                sourceConstructorReference = new WeakReference<Constructor<?>>(ctor);
                break;
            }
        }
        return ctor;
    }

    /**
     * {@inheritDoc}
     */
    public long getSourceBundleId() {
        return sourceBundleId;
    }

    /**
     * The unique identifier for this probe instance. A primitive is
     * used for fast evaluation.
     * 
     * @return a unique identifier for this probe instance
     */
    public long getIdentifier() {
        return identifier;
    }

    /**
     * {@inheritDoc} Get a somewhat descriptive name for this probe that is guaranteed to
     * be unique within a class instance. This name should be considered
     * opaque to users.
     * 
     * @return the descriptive name of this probe
     */
    public String getName() {
        return name;
    }

    /**
     * An efficient hash code.
     */
    public int hashCode() {
        return (int) identifier;
    }

    /**
     * An efficient equality test.
     * 
     * @param o the object to test for equality
     * 
     * @return true iff <code>o</code> equals <code>this</code>
     */
    public boolean equals(Object o) {
        if (o instanceof ProbeImpl) {
            ProbeImpl that = (ProbeImpl) o;
            return this.identifier == that.identifier;
        }
        return false;
    }

    /**
     * A readable representation of what this probe is associated with.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";identifier=").append(identifier);
        sb.append(",clazz=").append(sourceClassReference.get());
        sb.append(",name=").append(name);
        return sb.toString();
    }
}
