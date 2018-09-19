/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.info.internal;

import java.util.List;

import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.PackageInfo;

public abstract class ClassInfoImpl extends InfoImpl implements ClassInfo {

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

    public static boolean isJavaClass(String name) {
        return (name.startsWith(PackageInfo.JAVA_CLASS_PREFIX) ||
                name.startsWith(PackageInfo.JAVAX_EJB_CLASS_PREFIX) || name.startsWith(PackageInfo.JAVAX_SERVLET_CLASS_PREFIX));
    }

    // Instance state ...

    private static String getHashSuffix(String name) {
        return name;
    }

    public ClassInfoImpl(String name, int modifiers, InfoStoreImpl infoStore) {
        super( name, modifiers, infoStore, getHashSuffix(name) );
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
        throw createClassCastException(DelayedClassInfoImpl.class);
    }

    @Override
    public boolean isNonDelayedClass() {
        return false;
    }

    public NonDelayedClassInfoImpl asNonDelayedClass() {
        throw createClassCastException(NonDelayedClassInfoImpl.class);
    }

    //

    @Override
    protected String internName(String className) {
        return getInfoStore().internClassName(className);
    }

    //

    @Override
    public String getQualifiedName() {
        return getName();
    }

    //

    @Override
    public abstract List<MethodInfoImpl> getMethods();

    public MethodInfoImpl getMethod(String methodName) {
        for ( MethodInfoImpl methodInfo : getMethods() ) {
            if ( methodInfo.getName().equals(methodName) ) {
                return methodInfo;
            }
        }

        return null;
    }

    @Override
    public boolean isInstanceOf(Class<?> targetClass) {
        return isInstanceOf( targetClass.getName() );
    }
}
