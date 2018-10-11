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

package com.ibm.ws.monitor.internal.bci;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;
import com.ibm.ws.monitor.internal.ProbeManagerImpl;

/**
 * Class adapter that is responsible for injecting the provided probes
 * into classes.
 */
public class ProbeInjectionClassAdapter extends ClassVisitor {

    final ProbeManagerImpl probeManager;

    final Class<?> probedClasses;

    final Map<String, Method> methodMap = new HashMap<String, Method>();

    final Map<String, Constructor<?>> constructorMap = new HashMap<String, Constructor<?>>();

    final Set<ProbeMethodAdapter> probeAdapters = new HashSet<ProbeMethodAdapter>();

    Set<ProbeListener> interestedListeners;

    public ProbeInjectionClassAdapter(ClassVisitor delegate, ProbeManagerImpl probeManager, final Class<?> clazz) {
        super(Opcodes.ASM7, delegate);
        this.probeManager = probeManager;
        this.probedClasses = clazz;
        this.interestedListeners = probeManager.getInterestedByClass(clazz);
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    for (Method m : clazz.getDeclaredMethods()) {
                        methodMap.put(m.getName() + Type.getMethodDescriptor(m), m);
                    }
                    for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                        constructorMap.put("<init>" + Type.getConstructorDescriptor(c), c);
                    }
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof RuntimeException)
                throw (RuntimeException) e.getCause();
            else
                throw new RuntimeException(e);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (isStaticInitMethod(access, name)) {
            return mv;
        }

        MethodInfo methodInfo = new MethodInfo(this, access, name, descriptor, signature, exceptions);
        Set<ProbeListener> interested = getInterested(name, descriptor);
        ProbeMethodAdapter pma = new ProbeMethodAdapter(mv, methodInfo);

        for (MethodAdapters ma : MethodAdapters.values()) {
            for (ProbeListener listener : interested) {
                if (ma.isRequiredForListener(listener)) {
                    pma = ma.create(pma, methodInfo, interested);
                    probeAdapters.add(pma);
                    break;
                }
            }
        }

        return pma;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        final Map<ProbeListener, Set<ProbeImpl>> listenerProbes = new HashMap<ProbeListener, Set<ProbeImpl>>();
        for (ProbeMethodAdapter pma : probeAdapters) {
            for (ProbeImpl probe : pma.getEnabledProbes()) {
                for (ProbeListener listener : pma.getProbeListeners(probe)) {
                    Set<ProbeImpl> probes = listenerProbes.get(listener);
                    if (probes == null) {
                        probes = new HashSet<ProbeImpl>();
                        listenerProbes.put(listener, probes);
                    }
                    probes.add(probe);
                }
            }
        }

        for (ProbeListener listener : listenerProbes.keySet()) {
            probeManager.addActiveProbesforListener(listener, listenerProbes.get(listener));
        }
    }

    protected boolean isStaticInitMethod(int access, String name) {
        if ((access & ACC_STATIC) != 0) {
            return "<clinit>".equals(name);
        }
        return false;
    }

    protected boolean isConstructor(int access, String name) {
        if ((access & ACC_STATIC) == 0) {
            return "<init>".equals(name);
        }
        return false;
    }

    public boolean isModifiedClass() {
        for (ProbeMethodAdapter pma : probeAdapters) {
            if (!pma.getEnabledProbes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    Constructor<?> getConstructor(String name, String desc) {
        return constructorMap.get(name + desc);
    }

    Method getMethod(String name, String desc) {
        return methodMap.get(name + desc);
    }

    private Set<ProbeListener> getInterested(String name, String desc) {
        Set<ProbeListener> interested = new HashSet<ProbeListener>();

        String key = name + desc;
        Method m = methodMap.get(key);
        Constructor<?> c = constructorMap.get(key);

        for (ProbeListener listener : interestedListeners) {
            if (m != null && listener.getProbeFilter().matches(m, true)) {
                interested.add(listener);
            } else if (c != null && listener.getProbeFilter().matches(c, true)) {
                interested.add(listener);
            }
        }

        return interested;
    }

    ProbeManagerImpl getProbeManager() {
        return probeManager;
    }

    Class<?> getProbedClass() {
        return probedClasses;
    }

}
