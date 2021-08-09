/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.delta.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMapDelta;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

public class UtilImpl_BidirectionalMapDelta implements Util_BidirectionalMapDelta {

    public static final String CLASS_NAME = UtilImpl_BidirectionalMapDelta.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_BidirectionalMapDelta(
        Util_Factory factory,
        boolean recordAdded, boolean recordRemoved, boolean recordStill,
        Util_InternMap holderMap, Util_InternMap heldMap) {

        this.factory = (UtilImpl_Factory) factory;

        //

        this.holderTag = holderMap.getName();
        this.heldTag = heldMap.getName();

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        //

        this.addedMap = (UtilImpl_BidirectionalMap) ( recordAdded ? this.factory.createBidirectionalMap(holderTag, holderMap, heldTag, heldMap) : null );
        this.removedMap = (UtilImpl_BidirectionalMap)  ( recordRemoved ? this.factory.createBidirectionalMap(holderTag, holderMap, heldTag, heldMap) : null );
        this.stillMap = (UtilImpl_BidirectionalMap)  ( recordStill ? this.factory.createBidirectionalMap(holderTag, holderMap, heldTag, heldMap) : null );
    }

    //

    protected final UtilImpl_Factory factory;

    @Override
    public UtilImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final String holderTag;

    @Override
    public String getHolderTag() {
        return holderTag;
    }

    protected final String heldTag;

    @Override
    public String getHeldTag() {
        return heldTag;
    }

    //

    protected final UtilImpl_BidirectionalMap addedMap;

    @Override
    public Util_BidirectionalMap getAddedMap() {
        return addedMap;
    }

    @Override
    public boolean isNullAdded() {
        return ( (addedMap == null) || addedMap.isEmpty() );
    }

    protected final UtilImpl_BidirectionalMap removedMap;

    @Override
    public UtilImpl_BidirectionalMap getRemovedMap() {
        return removedMap;
    }

    @Override
    public boolean isNullRemoved() {
        return ( (removedMap == null) || removedMap.isEmpty() );
    }

    protected final UtilImpl_BidirectionalMap stillMap;

    @Override
    public UtilImpl_BidirectionalMap getStillMap() {
        return stillMap;
    }

    @Override
    public boolean isNullStill() {
        return ( (stillMap == null) || stillMap.isEmpty() );
    }

    @Override
    public boolean isNull() {
        return ( isNullAdded() && isNullRemoved() );
    }

    @Override
    public boolean isNull(boolean ignoreRemoved) {
        return ( isNullAdded() && (ignoreRemoved || isNullRemoved()) ); 
    }

    @Override
    public void describe(String prefix, List<String> nonNull) {
        if ( !isNullAdded() ) {
            nonNull.add(prefix + " Added [ " + getAddedMap().getHolderSet().size() + " ]");
        }

        if ( !isNullRemoved() ) {
            nonNull.add(prefix + " Removed [ " + getRemovedMap().getHolderSet().size() + " ]");
        }
    }

    //

    @Override
    public void subtract(
        Util_BidirectionalMap finalMap, Util_BidirectionalMap initialMap) {

        Util_InternMap finalHolderDomain = finalMap.getHolderInternMap();
        Util_InternMap finalHeldDomain = finalMap.getHeldInternMap();

        Util_InternMap initialHolderDomain = initialMap.getHolderInternMap();
        Util_InternMap initialHeldDomain = initialMap.getHeldInternMap();

        if ( finalHolderDomain == initialHolderDomain ) {
            finalHolderDomain = null;
            initialHolderDomain = null;
        }

        if ( finalHeldDomain == initialHeldDomain ) {
            finalHeldDomain = null;
            initialHeldDomain = null;
        }

        for ( String i_finalHolderName : finalMap.getHolderSet() ) {
            Set<String> i_finalHeld = finalMap.i_selectHeldOf(i_finalHolderName);

            String asInitial_i_finalHolderName = intern(i_finalHolderName, initialHolderDomain);
            Set<String> i_initialHeld = selectHeldOf(asInitial_i_finalHolderName, initialMap);

            if ( (i_initialHeld == null) || i_initialHeld.isEmpty() ) {
                if ( addedMap != null ) {
                    for ( String i_heldName : i_finalHeld ) {
                        addedMap.i_record(i_finalHolderName, i_heldName);
                    }
                }
            } else {
                for ( String i_finalHeldName : i_finalHeld ) {
                    String asInitial_i_finalHeldName = intern(i_finalHeldName, initialHeldDomain);

                    if ( (asInitial_i_finalHeldName != null) && i_initialHeld.contains(asInitial_i_finalHeldName) ) {
                        if ( stillMap != null ) {
                            stillMap.i_record(i_finalHolderName, i_finalHeldName);
                        }
                    } else {
                        if ( addedMap != null ) {
                            addedMap.i_record(i_finalHolderName, i_finalHeldName);
                        }
                    }
                }
            }
        }

        for ( String i_initialHolderName : initialMap.getHolderSet() ) {
            Set<String> i_initialHeld = initialMap.selectHeldOf(i_initialHolderName);

            String asFinal_i_initialHolderName = intern(i_initialHolderName, finalHolderDomain);
            Set<String> i_finalHeld = selectHeldOf(asFinal_i_initialHolderName, finalMap);

            if ( (i_finalHeld == null) || i_finalHeld.isEmpty() ) {
                if ( removedMap != null ) {
                    for ( String i_heldName : i_initialHeld ) {
                        removedMap.i_record(i_initialHolderName, i_heldName);
                    }
                }
            } else {
                for ( String i_initialHeldName : i_initialHeld ) {
                    String asFinal_i_initialHeldName = intern(i_initialHeldName, finalHeldDomain);

                    if ( (asFinal_i_initialHeldName != null) && i_finalHeld.contains(asFinal_i_initialHeldName) ) {
                        // if ( stillMap != null ) {
                        //    stillMap.i_record(i_holderName, i_heldName);
                        // }
                    } else {
                        if ( removedMap != null ) {
                            removedMap.i_record(i_initialHolderName, i_initialHeldName);
                        }
                    }
                }
            }
        }
    }

    private String intern(String value, Util_InternMap internMap) {
        if ( internMap == null ) {
            return value;
        } else {
            return internMap.intern(value, Util_InternMap.DO_NOT_FORCE);
        }
    }

    private Set<String> selectHeldOf(String i_holder, Util_BidirectionalMap biMap) {
        if ( i_holder == null ) {
            return null;
        } else {
            return biMap.selectHeldOf(i_holder);
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        log( new UtilImpl_PrintLogger(useLogger) );
    }

    @Override
    public void log(PrintWriter writer) {
        log( new UtilImpl_PrintLogger(writer) );
    }

    @Override
    public void log(Util_PrintLogger useLogger) {
        String methodName = "log";

        boolean nullAdded = isNullAdded();
        boolean nullRemoved = isNullRemoved();
        boolean nullStill = isNullStill();
        
        if ( nullAdded && nullRemoved ) {
            int numStill = ( nullStill ? 0 : getStillMap().getHolderSet().size() );

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                "Mapping Delta: [ {0} ] Unchanged [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(numStill) });

        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Mapping Delta: BEGIN: [ {0} ]", getHashText());

            if ( !nullAdded ) {
                logAddedAnnotations(useLogger);
            }
            if ( !nullRemoved ) {
                logRemovedAnnotations(useLogger);
            }
            if ( !nullStill ) {
                logStillAnnotations(useLogger);
            }

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Mapping Delta: END: [ {0} ]", getHashText());
        }
    }

    public void logAddedAnnotations(Util_PrintLogger useLogger) {
        String methodName = "logAddedAnnotations";
        Util_BidirectionalMap useAddedMap = getAddedMap();
        if ( useAddedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added Entries: ** NOT RECORDED **");
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added Entries: BEGIN");
        logMap(useLogger, useAddedMap);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added Entries: END");
    }

    public void logRemovedAnnotations(Util_PrintLogger useLogger) {
        String methodName = "logRemovedAnnotations";

        UtilImpl_BidirectionalMap useRemovedMap = getRemovedMap();
        if ( useRemovedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed Entries: ** NOT RECORDED **");
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed Entries: BEGIN");
        logMap(useLogger, useRemovedMap);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed Entries: END");
    }

    public void logStillAnnotations(Util_PrintLogger useLogger) {
        String methodName = "logStillAnnotations";
        
        UtilImpl_BidirectionalMap useStillMap = getStillMap();
        if ( useStillMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still Entries: [ ** NOT RECORDED ** ]");
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still Entries: BEGIN");
        logMap(useLogger, useStillMap);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still Entries: END");
    }

    protected void logMap(Util_PrintLogger useLogger, Util_BidirectionalMap map) {
        String methodName = "logMap";
        Set<String> useHolderSet = map.getHolderSet();
        if ( useHolderSet.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            for ( String i_holderName : map.getHolderSet() ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", i_holderName );

                for ( String i_heldName : map.i_selectHeldOf(i_holderName) ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    [ {0} ]", i_heldName);
                }
            }
        }
    }
}
