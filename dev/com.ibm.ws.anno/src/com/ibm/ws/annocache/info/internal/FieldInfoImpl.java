/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.annocache.info.FieldInfo;

public class FieldInfoImpl extends InfoImpl implements FieldInfo {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");

    private static final String CLASS_NAME = FieldInfoImpl.class.getSimpleName();

    private static String getHashSuffix(String name, NonDelayedClassInfoImpl declaringClass) {
        return declaringClass.getName() + '.' + name;
    }

    //

    public FieldInfoImpl(String name, String desc, int modifiers, Object defaultValue,
                         NonDelayedClassInfoImpl declaringClass) {

        super( name, modifiers, declaringClass.getInfoStore(),
               getHashSuffix(name, declaringClass) );

        String methodName = "<init>";

        InfoStoreImpl useInfoStore = declaringClass.getInfoStore();

        this.declaringClass = declaringClass;

        // Use the class name of the field type directory as computed by the
        // ASM type.  Previously, the class name was obtained by getting
        // the class info for the field type, then retrieving the name from
        // that class info.
        //
        // This results in the same type name value, but, avoids the class info
        // lookup.  The processing order is unchanged, as the class info retrieval
        // obtained a delayable class info object.  That is, this processing would
        // not have caused nested class info processing.

        // this.typeName = useInfoStore.getClassInfo( Type.getType(desc) ).getName();

        this.type = useInfoStore.getDelayableClassInfo(Type.getType(desc));

        // TODO: Not sure whether to cache the field type as a ClassInfo object or as a Type object.

        this.defaultValue = defaultValue;

        // This bloats the log if allowed on FINER.        
        if (logger.isLoggable(Level.FINEST) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Created [ {0} ] on [ {1} ] of type [ {2} ] and default [ {3} ]",
                        new Object[] { getHashText(),
                                       getDeclaringClass().getHashText(),
                                       getType(),
                                       getDefaultValue() });
        }
    }

    //

    @Override
    protected String internName(String fieldName) {
        return getInfoStore().internFieldName(fieldName);
    }

    @Override
    public String getQualifiedName() {
        return getDeclaringClass().getName() + '.' + getName();
    }

    //

    protected ClassInfoImpl declaringClass;

    @Override
    public ClassInfoImpl getDeclaringClass() {
        return declaringClass;
    }

    //

    protected ClassInfoImpl type;

    @Override
    public String getTypeName() {
        return getType().getName();
    }

    @Override
    public ClassInfoImpl getType() {
        return type;
    }

    //

    public Object defaultValue;

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Field [ {0} ]", getHashText());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Name [ {0} ]", getName());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Type [ {0} ]", getType().getHashText());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Default Value [ {0} ]", getDefaultValue());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Declaring Class [ {0} ]", getDeclaringClass().getHashText());

        logAnnotations(useLogger);
    }

    //

    @Override
    public void log(TraceComponent tc) {

        Tr.debug(tc, MessageFormat.format("Field [ {0} ]", getHashText()));
        Tr.debug(tc, MessageFormat.format("  Name [ {0} ]", getName()));
        Tr.debug(tc, MessageFormat.format("  Type [ {0} ]", getType().getHashText()));
        Tr.debug(tc, MessageFormat.format("  Default Value [ {0} ]", getDefaultValue()));

        Tr.debug(tc, MessageFormat.format("  Declaring Class [ {0} ]", getDeclaringClass().getHashText()));

        logAnnotations(tc);
    }
}
