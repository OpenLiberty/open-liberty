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
package com.ibm.ws.anno.info.internal;

import java.util.List;

import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.PackageInfo;

public abstract class ClassInfoImpl extends InfoImpl implements ClassInfo {

    @SuppressWarnings("hiding")
	public static final String CLASS_NAME = ClassInfoImpl.class.getName();

    //

    // Common method for consuming references.
    //
    // This is to indicate a case where an object reference is created but
    // is not used, and this is expected.

    public static void consumeRef(Object ref) {
        // NO-OP
    }

    //

    public static String getPackageName(String name) {
        int index = name.indexOf(".");
        if (index != -1) {
            return name.substring(0, name.lastIndexOf("."));
        } else {
            return name;
        }
    }

    // Used, in particular, by 'ClassInfoCache.addClassInfo(NonDelayedClassInfo)'
    // to select classes to place in the higher retention store of java classes.
    public static boolean isJavaClass(String name) {
        return ( name.startsWith(PackageInfo.JAVA_CLASS_PREFIX) ||
                 name.startsWith(PackageInfo.JAVAX_EJB_CLASS_PREFIX) ||
                 name.startsWith(PackageInfo.JAVAX_SERVLET_CLASS_PREFIX) ||
                 name.startsWith(PackageInfo.JAKARTA_EJB_CLASS_PREFIX) ||
                 name.startsWith(PackageInfo.JAKARTA_SERVLET_CLASS_PREFIX) );
    }

    // Instance state ...

    public ClassInfoImpl(String name, int modifiers, InfoStoreImpl infoStore) {
        super(name, modifiers, infoStore);
    }

    //

    /**
     * <p>Tell if this class info was created to represent an unloadable class.
     * Stub values are assigned, and the interface and superclass values are forced.</p>
     * 
     * @return True if this class object is artificial. Otherwise, false.
     */
    @Override
    public abstract boolean isArtificial();

    // Typing ...

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isDelayedClass() {
        return false;
    }

    public ClassInfoImpl asDelayedClass() {
        throw createClassCastException(DelayedClassInfo.class);
    }

    @Override
    public boolean isNonDelayedClass() {
        return false;
    }

    public NonDelayedClassInfo asNonDelayedClass() {
        throw createClassCastException(NonDelayedClassInfo.class);
    }

    //

    @Override
    protected String internName(String useName) {
        return getInfoStore().internClassName(useName);
    }

    //

    @Override
    public String getQualifiedName() {
        return getName();
    }

    //

    public abstract List<MethodInfoImpl> getMethods();

    public MethodInfoImpl getMethod(String useName) {
        for ( MethodInfoImpl meth : getMethods() ) {
            if ( meth.getName().equals(useName) ) {
                return meth;
            }
        }

        return null;
    }

    @Override
    public boolean isInstanceOf(Class<?> clazz) {
        return isInstanceOf(clazz.getName());
    }
}
