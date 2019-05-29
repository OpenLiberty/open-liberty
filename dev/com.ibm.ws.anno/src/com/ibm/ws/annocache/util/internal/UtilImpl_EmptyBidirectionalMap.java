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
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;

public class UtilImpl_EmptyBidirectionalMap implements Util_BidirectionalMap {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");
    private static final Logger stateLogger = Logger.getLogger("com.ibm.ws.annocache.util.state");

    public static final String CLASS_NAME = "UtilImpl_EmptyBidirectionalMap";

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected UtilImpl_EmptyBidirectionalMap(UtilImpl_Factory factory,
                                             UtilImpl_InternMap.ValueType holderType, String holderTag,
                                             UtilImpl_InternMap.ValueType heldType, String heldTag) {

        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
                        "(" + holderTag + " : " + heldTag + ")";
        
        this.factory = factory;

        this.holderInternMap = factory.createEmptyInternMap(holderType, holderTag);
        this.heldInternMap = factory.createEmptyInternMap(heldType, heldTag);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
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

    @Deprecated
    @Override
    @Trivial
    public boolean getEnabled() {
        return true;
    }

    //

    protected final UtilImpl_EmptyInternMap holderInternMap;

    @Override
    @Trivial
    public UtilImpl_EmptyInternMap getHolderInternMap() {
        return holderInternMap;
    }

    @Override
    @Trivial
    public String getHolderTag() {
        return holderInternMap.getName();
    }

    @Override
    public boolean containsHolder(String holderName) {
        return false;
    }

    @Trivial
    protected String internHolder(String name, boolean doForce) {
        throw new UnsupportedOperationException();
    }

    //

    protected final UtilImpl_EmptyInternMap heldInternMap;

    @Override
    @Trivial
    public UtilImpl_EmptyInternMap getHeldInternMap() {
        return heldInternMap;
    }

    @Override
    @Trivial
    public String getHeldTag() {
        return heldInternMap.getName();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsHeld(String heldName) {
        return false;
    }

    protected String internHeld(String name, boolean doForce) {
        throw new UnsupportedOperationException();
    }

    //

    // Implementation intention:
    //
    // className -> Set<annotationName> : tell what annotations a class has
    // annotationName -> Set<className> : tell what classes have an annotation

    @Override
    public Set<String> getHolderSet() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getHeldSet() {
        return Collections.emptySet();
    }

    @Override
    public boolean holds(String holderName, String heldName) {
        return false;
    }

    @Override
    public Set<String> selectHeldOf(String holderName) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> selectHoldersOf(String heldName) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> i_selectHeldOf(String i_holderName) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> i_selectHoldersOf(String i_heldName) {
        return Collections.emptySet();
    }

    //

    public boolean record(String holderName, String heldName) {
        throw new UnsupportedOperationException();
    }

    public boolean i_record(String i_holderName, String i_heldName) {
        throw new UnsupportedOperationException();
    }

    protected boolean i_recordHolderToHeld(String i_holderName, String i_heldName) {
        throw new UnsupportedOperationException();
    }

    protected Set<String> i_recordHolder(String i_holderName) {
        throw new UnsupportedOperationException();
    }

    protected boolean i_recordHeldToHolder(String i_holderName, String i_heldName) {
        throw new UnsupportedOperationException();
    }

    protected Set<String> i_recordHeld(String i_heldName) {
        throw new UnsupportedOperationException();
    }

    //

    @Override
    @Trivial
    public void logState() {
        if ( !stateLogger.isLoggable(Level.FINER) ) {
            return;
        }

        log(logger);
    }

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BiDi Map (Empty): BEGIN: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Holder Intern Map:");
        holderInternMap.log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Held Intern Map:");
        heldInternMap.log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Holder-to-held Map: [ NULL ]");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Held-to-holder Map: [ NULL ]");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BiDi Map (Empty): END: [ {0} ]", getHashText());
    }
    
    @Override
    @Trivial
    public void log(TraceComponent useLogger) {
        Tr.debug(useLogger, MessageFormat.format("BiDi Map (Empty): BEGIN: [ {0} ]", getHashText()));

        Tr.debug(useLogger, "Holder Intern Map:");
        holderInternMap.log(useLogger);

        Tr.debug(useLogger, "Held Intern Map:");
        heldInternMap.log(useLogger);

        Tr.debug(useLogger, "Holder-to-held Map: [ NULL ]");

        Tr.debug(useLogger, "Held-to-holder Map: [ NULL ]");

        Tr.debug(useLogger, "BiDi Map (Empty): END: [ {0} ]", getHashText());
    }

    //

    @Override
    public boolean getIsEnabled() {
        return true;
    }
}
