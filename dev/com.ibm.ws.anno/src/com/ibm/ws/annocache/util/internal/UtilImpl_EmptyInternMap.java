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

package com.ibm.ws.annocache.util.internal;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class UtilImpl_EmptyInternMap implements Util_InternMap {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");
    public static final String CLASS_NAME = "UtilImpl_EmptyInternMap";

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Override
    @Trivial
    public int getLogThreshHold() {
        return UtilImpl_InternMap.DEFAULT_LOG_THRESHHOLD;
    }

    @Override
    @Trivial
    public String validate(String value, ValueType useValueType) {
        return UtilImpl_InternMap.doValidate(value, useValueType);
    }

    //

    public UtilImpl_EmptyInternMap(UtilImpl_Factory factory, ValueType valueType, String name) {
        super();

        String methodName = "<init>";

        this.factory = factory;

        this.name = name;

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
                        "(" + this.name + ")";

        this.valueType = valueType;
        this.checkValues = logger.isLoggable(Level.FINER);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Value type [ {1} ]",
                    new Object[] { this.hashText, this.valueType });
        }
    }

    //

    protected final UtilImpl_Factory factory;

    @Override
    @Trivial
    public UtilImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final ValueType valueType;

    @Override
    @Trivial
    public ValueType getValueType() {
        return valueType;
    }

    protected boolean checkValues;

    @Trivial
    public boolean getCheckValues() {
        return checkValues;
    }

    //

    protected final String name;

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    @Override
    public Collection<String> getValues() {
        return Collections.emptySet();
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        return 0;
    }

    @Override
    public String intern(String useName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String intern(String useName, boolean doForce) {
        throw new UnsupportedOperationException();
    }

    // Used to allow fast return lookups:
    //
    // For a lookup to an identity map that is backed by an intern map,
    // if the intern map lookup fails, the map lookup must return null.

    @Override
    public boolean contains(String useName) {
        return false;
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Intern Map (Empty): BEGIN: [ {0} ]:", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Size           [ {0} ]", Integer.valueOf(getSize()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Total Length   [ {0} ]", Integer.valueOf(getTotalLength()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Log threshhold [ {0} ]", Integer.valueOf(getLogThreshHold()));

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Intern Map (Empty): END: [ {0} ]:", getHashText());
    }
    
    // Old style logging; required to satisfy the older interface

    @Override
    @Trivial
    public void logState() {
        if (com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging.stateLogger.isDebugEnabled()) {
            log(com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging.stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(TraceComponent useLogger) {
        Tr.debug(useLogger, MessageFormat.format("BEGIN Intern Map (Empty) [ {0} ]:", getHashText()));
        Tr.debug(useLogger, MessageFormat.format("  Log threshhold[ {0} ]", Integer.valueOf(getLogThreshHold())));
        Tr.debug(useLogger, MessageFormat.format("  Size [ {0} ]", Integer.valueOf(getSize())));
        Tr.debug(useLogger, MessageFormat.format("  Total Length [ {0} ]", Integer.valueOf(getTotalLength())));
        Tr.debug(useLogger, "END Intern Map (Empty)");
    }
}
