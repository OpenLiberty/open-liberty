/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.Info;

public abstract class InfoImpl implements Info {

    public static final String CLASS_NAME = ClassInfoImpl.class.getName();

    protected String hashText;

    @Override
    @Trivial
    public String getHashText() {
        if (hashText == null) {
            hashText = computeHashText();
        }

        return hashText;
    }

    @Trivial
    protected String computeHashText() {
        return getClass().getName() + "@" + Integer.toString((new Object()).hashCode()) + " ( " + getName() + " )";
    }

    @Override
    @Trivial
    public String toString() {
        return getHashText();
    }

    // Top O' the world ...
    @Trivial
    public InfoImpl(String name, int modifiers, InfoStoreImpl infoStore) {

        this.infoStore = infoStore;

        this.name = internName(name);
        this.modifiers = modifiers;
        // log in the concrete constructors
    }

    // Typing ...

    protected ClassCastException createClassCastException(Class<?> targetClass) {
        return new ClassCastException(getClassCastExceptionText(targetClass));
    }

    protected String getClassCastExceptionText(Class<?> targetClass) {
        return ("Cannot convert [ " + getClass() + " ] named [ " + getQualifiedName() + " ] to [ " + targetClass + " ]");
    }

    // Context ...

    protected InfoStoreImpl infoStore;

    @Override
    public InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    protected abstract String internName(String name);

    // TODO: Should this be allowed??
    //       The current implementation will cause the
    //       addition of a new class info, and visitor calls on that,
    //       possibly extending the class info data base for
    //       indirect access requests.

    protected ClassInfoImpl getDelayableClassInfo(String className) {
        return getInfoStore().getDelayableClassInfo(className);
    }

    protected ClassInfoImpl getDelayableClassInfo(Type type) {
        return getInfoStore().getDelayableClassInfo(type);
    }

    // Basic state ...

    protected int modifiers;

    @Override
    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    @Override
    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    @Override
    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    @Override
    public boolean isPackagePrivate() {
        return (getModifiers() & (Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC)) == 0;
    }

    // Typing ...

    protected String name;

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    // Annotation storage ...

    protected List<AnnotationInfoImpl> declaredAnnotations = Collections.emptyList();

    @Override
    public boolean isDeclaredAnnotationPresent() {
        return isAnnotationPresent(getDeclaredAnnotations());
    }

    private static boolean isAnnotationPresent(Collection<AnnotationInfoImpl> annos) {
        return (annos != null && !annos.isEmpty());
    }

    @Override
    public List<AnnotationInfoImpl> getDeclaredAnnotations() {
        return declaredAnnotations;
    }

    public void setDeclaredAnnotations(AnnotationInfoImpl[] annos) {
        declaredAnnotations = Arrays.asList(annos);
    }

    @Override
    public boolean isDeclaredAnnotationPresent(String annotationName) {
        return getDeclaredAnnotation(annotationName) != null;
    }

    @Override
    public AnnotationInfoImpl getDeclaredAnnotation(String annotationClassName) {
        return getAnnotation(getDeclaredAnnotations(), annotationClassName);
    }

    @Override
    public AnnotationInfoImpl getDeclaredAnnotation(Class<? extends Annotation> clazz) {
        return getDeclaredAnnotation(clazz.getName());
    }

    private static AnnotationInfoImpl getAnnotation(Collection<AnnotationInfoImpl> annoInfos, String annotationClassName) {
        if (annoInfos != null) {
            for (AnnotationInfoImpl aii : annoInfos) {
                if (aii.getAnnotationClassName().equals(annotationClassName)) {
                    return aii;
                }
            }
        }

        return null;
    }

    // Optimized to iterate across the declared annotations first.
    @Override
    public boolean isDeclaredAnnotationWithin(Collection<String> annotationNames) {
        if (declaredAnnotations != null) {
            for (AnnotationInfo annotation : declaredAnnotations) {
                if (annotationNames.contains(annotation.getAnnotationClassName())) {
                    return true;
                }
            }
        }

        return false;
    }

    //
    @Override
    public boolean isAnnotationPresent() {
        return isAnnotationPresent(getAnnotations());
    }

    @Override
    public boolean isAnnotationPresent(String annotationName) {
        return getAnnotation(annotationName) != null;
    }

    @Override
    public boolean isAnnotationWithin(Collection<String> annotationNames) {
        for (AnnotationInfo annotation : getAnnotations()) {
            if (annotationNames.contains(annotation.getAnnotationClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<AnnotationInfoImpl> getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public AnnotationInfoImpl getAnnotation(String annotationName) {
        AnnotationInfoImpl anno = getDeclaredAnnotation(annotationName);
        if (anno == null) {
            anno = getAnnotation(getAnnotations(), annotationName);
        }
        return anno;
    }

    @Override
    public AnnotationInfoImpl getAnnotation(Class<? extends Annotation> clazz) {
        return getAnnotation(clazz.getName());
    }

    //

    @Override
    public abstract void log(TraceComponent logger);

    public void logAnnotations(TraceComponent logger) {

        boolean firstAnnotation = true;
        for (AnnotationInfoImpl nextAnnotation : getAnnotations()) {
            if (firstAnnotation) {
                Tr.debug(logger, "  Annotations:");
                firstAnnotation = false;
            }

            nextAnnotation.log(logger);
        }

        if (firstAnnotation) {
            Tr.debug(logger, "  No Annotations");
        }
    }
}
