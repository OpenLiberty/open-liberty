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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import com.ibm.websphere.monitor.MonitorManager;
import com.ibm.websphere.monitor.annotation.Arg;
import com.ibm.websphere.monitor.annotation.Args;
import com.ibm.websphere.monitor.annotation.Elapsed;
import com.ibm.websphere.monitor.annotation.FieldValue;
import com.ibm.websphere.monitor.annotation.Returned;
import com.ibm.websphere.monitor.annotation.TargetInstance;
import com.ibm.websphere.monitor.annotation.TargetMember;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.annotation.Thrown;

/**
 * Adapter object that holds a reference to a probe listener target and
 * method. The adapter will reflectively invoke the correct method on the
 * target where the probe fires. By using an adapter, we can exploit a
 * common interface.
 * In the future, this adapter could be enhanced to only pass a subset of the
 * probe data to the listener.
 */
public class ProbeListener {

    final MonitorManager monitorManager;
    final ListenerConfiguration config;
    final Object probeTarget;
    final Method method;
    final Class<?>[] parameterTypes;
    final Annotation[][] annotations;
    final Object[] args;
    final ArrayList<Integer> annotationsList = new ArrayList<Integer>();
    boolean isValidated = false;

    ProbeListener(MonitorManager monitorManager, ListenerConfiguration config, Object target, Method method) {
        this.monitorManager = monitorManager;
        this.probeTarget = target;
        this.method = method;
        this.method.setAccessible(true);
        this.config = config;
        this.parameterTypes = method.getParameterTypes();
        for (int i = 0; i < this.parameterTypes.length; i++) {
            this.parameterTypes[i] = getBoxedType(this.parameterTypes[i]);
        }
        this.annotations = method.getParameterAnnotations();
        this.args = new Object[this.parameterTypes.length];
        for (int i = 0; i < this.parameterTypes.length; i++) {
            args[i] = getDefaultValue(parameterTypes[i]);
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Annotation[] annotations = this.annotations[i];
            for (Annotation a : annotations) {
                if (a instanceof This) {
                    annotationsList.add(i, MonitorConstants.This);
                } else if ((a instanceof Args) && (parameterTypes[i].isAssignableFrom(Object[].class))) { // @Args
                    annotationsList.add(i, MonitorConstants.Args);
                } else if (a instanceof Elapsed) {
                    annotationsList.add(i, MonitorConstants.Elapsed);
                } else if (a instanceof Returned) {
                    annotationsList.add(i, MonitorConstants.Returned);
                } else if (a instanceof FieldValue) {
                    annotationsList.add(i, MonitorConstants.FieldValue);
                } else if (a instanceof TargetInstance) {
                    annotationsList.add(i, MonitorConstants.TargetInstance);
                } else if (a instanceof TargetMember) {
                    annotationsList.add(i, MonitorConstants.TargetMember);
                } else if (a instanceof Thrown) {
                    annotationsList.add(i, MonitorConstants.Thrown);
                } else if (a instanceof Arg) { // @Arg(n)
                    annotationsList.add(i, MonitorConstants.Arg);
                }
            }

        }
    }

    Object getProbeTarget() {
        return probeTarget;
    }

    Method getTargetMethod() {
        return method;
    }

    public ListenerConfiguration getListenerConfiguration() {
        return config;
    }

    public ProbeFilter getProbeFilter() {
        return config.getProbeFilter();
    }

    public Set<String> getGroupNames() {
        return config.getGroupNames();
    }

    Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    Annotation[][] getParameterAnnotations() {
        return annotations;
    }

    Object[] getdefaultvalues() {
        return args;
    }

    private Class<?> getBoxedType(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        } else if (clazz.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (clazz.equals(Character.TYPE)) {
            return Character.class;
        } else if (clazz.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (clazz.equals(Short.TYPE)) {
            return Short.class;
        } else if (clazz.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (clazz.equals(Long.TYPE)) {
            return Long.class;
        } else if (clazz.equals(Float.TYPE)) {
            return Float.class;
        } else if (clazz.equals(Double.TYPE)) {
            return Double.class;
        }
        return clazz;
    }

    private Object getDefaultValue(Class<?> parameterType) {
        if (!parameterType.isPrimitive()) {
            return null;
        } else if (parameterType.equals(Boolean.TYPE)) {
            return Boolean.valueOf(false);
        } else if (parameterType.equals(Character.TYPE)) {
            return Character.valueOf('\0');
        } else if (parameterType.equals(Byte.TYPE)) {
            return Byte.valueOf((byte) 0);
        } else if (parameterType.equals(Short.TYPE)) {
            return Short.valueOf((short) 0);
        } else if (parameterType.equals(Integer.TYPE)) {
            return Integer.valueOf(0);
        } else if (parameterType.equals(Long.TYPE)) {
            return Long.valueOf(0L);
        } else if (parameterType.equals(Float.TYPE)) {
            return Float.valueOf(0.0f);
        } else if (parameterType.equals(Double.TYPE)) {
            return Double.valueOf(0.0d);
        }
        return null;
    }

    ArrayList<Integer> getAnnotationsList() {
        return this.annotationsList;
    }

    void setValidated(boolean validated) {
        this.isValidated = validated;
    }

    public boolean isValidated() {
        return this.isValidated;
    }

}
