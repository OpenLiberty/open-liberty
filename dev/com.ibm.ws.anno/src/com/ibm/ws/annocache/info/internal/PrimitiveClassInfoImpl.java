/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class PrimitiveClassInfoImpl extends ClassInfoImpl {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
    private static final String CLASS_NAME = PrimitiveClassInfoImpl.class.getSimpleName();

    //

    // Created by the ClassInfoCache; the type class name is probably
    // the type class's name, but, let the caller choose the name.

    public PrimitiveClassInfoImpl(String typeClassName, Class<?> typeClass, InfoStoreImpl infoStore) {
        super(typeClassName, 0, infoStore);

        String methodName = "<init>";

        this.javaType = typeClass;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Created", getHashText());
        }
    }

    //

    /**
     * <p>Tell if this class info was created to represent an unloadable class.
     * Stub values are assigned, and the interface and superclass values are forced.</p>
     * 
     * @return True if this class object is artificial. Otherwise, false.
     */
    @Override
    public boolean isArtificial() {
        return false;
    }

    //

    @Override
    public boolean isPrimitive() {
        return true;
    }

    public PrimitiveClassInfoImpl asPrimitiveClass() {
        return this;
    }

    //

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public PackageInfoImpl getPackage() {
        return null;
    }

    @Override
    public boolean isJavaClass() {
        return false;
    }

    //

    @Override
    public String getSuperclassName() {
        return null;
    }

    @Override
    public ClassInfoImpl getSuperclass() {
        return null;
    }

    //

    // TODO: Confusing method name.
    //
    // The 'type' is a java class, not an instance of org.objectweb.asm.Type.

    protected Class<?> javaType;

    public Class<?> getType() {
        return javaType;
    }

    //

    @Override
    public List<String> getInterfaceNames() {
        return Collections.emptyList();
    }

    @Override
    public List<ClassInfoImpl> getInterfaces() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAnnotationClass() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    // Are these correct?  For example, 'long' is not assignable from 'int'
    // according to this implementation.

    @Override
    public boolean isAssignableFrom(String className) {
        return className.equals( getName() );
    }

    @Override
    public boolean isInstanceOf(String className) {
        return className.equals( getName() );
    }

    @Override
    public List<FieldInfoImpl> getDeclaredFields() {
        return Collections.emptyList();
    }

    @Override
    public List<MethodInfoImpl> getDeclaredConstructors() {
        return Collections.emptyList();
    }

    @Override
    public List<MethodInfoImpl> getDeclaredMethods() {
        return Collections.emptyList();
    }

    @Override
    public List<MethodInfoImpl> getMethods() {
        return Collections.emptyList();
    }

    //

    @Override
    public boolean isFieldAnnotationPresent() {
        return false;
    }

    @Override
    public boolean isMethodAnnotationPresent() {
        return false;
    }

    //
    
    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    " Primitive [ {0} ] Type [ {1} ]",
                    new Object[] { getHashText(), getType() });
    }

    //

    @Override
    public void log(TraceComponent tc) {

        Tr.debug(tc, MessageFormat.format(" Primitive [ {0} ] Type [ {1} ]",
                                          new Object[] { getHashText(), getType() }));

        // For a primitive, there are no other values to log.
    }
}
