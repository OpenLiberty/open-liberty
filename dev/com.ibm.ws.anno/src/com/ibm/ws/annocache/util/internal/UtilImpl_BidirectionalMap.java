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
package com.ibm.ws.annocache.util.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class UtilImpl_BidirectionalMap implements Util_BidirectionalMap {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");
    private static final Logger stateLogger = Logger.getLogger("com.ibm.ws.annocache.util.state");

    public static final String CLASS_NAME = "UtilImpl_BidirectionalMap";

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected UtilImpl_BidirectionalMap(Util_Factory factory,
                                        String holderTag, String heldTag,
                                        Util_InternMap holderInternMap,
                                        Util_InternMap heldInternMap) {
        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
                        "(" + holderTag + " : " + heldTag + ")";

        this.factory = factory;

        this.holderTag = holderTag;
        this.heldTag = heldTag;

        this.holderInternMap = holderInternMap;
        this.heldInternMap = heldInternMap;

        this.i_holderToHeldMap = new IdentityHashMap<String, Set<String>>();
        this.i_heldToHoldersMap = new IdentityHashMap<String, Set<String>>();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final Util_Factory factory;

    @Override
    @Trivial
    public Util_Factory getFactory() {
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

    protected final String holderTag;

    @Override
    @Trivial
    public String getHolderTag() {
        return holderTag;
    }

    protected String heldTag;

    @Override
    @Trivial
    public String getHeldTag() {
        return heldTag;
    }

    //

    protected final Util_InternMap holderInternMap;

    @Override
    @Trivial
    public Util_InternMap getHolderInternMap() {
        return holderInternMap;
    }

    @Override
    @Trivial
    public boolean containsHolder(String holderName) {
        String methodName = "containsHolder";
        boolean result = holderInternMap.contains(holderName);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Contains holder [ {1} ] [ {2} ]",
                new Object[] { getHashText(), holderName, Boolean.valueOf(result) });
        }
        return result;
    }

    protected String internHolder(String name, boolean doForce) {
        return holderInternMap.intern(name, doForce);
    }

    protected Util_InternMap heldInternMap;

    @Override
    @Trivial
    public Util_InternMap getHeldInternMap() {
        return heldInternMap;
    }

    @Override
    public boolean containsHeld(String heldName) {
        String methodName = "containsHeld";
        boolean result = heldInternMap.contains(heldName);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Contains held [ {1} ] [ {2} ]",
                new Object[] { getHashText(), heldName, Boolean.valueOf(result) });
        }
        return result;
    }

    protected String internHeld(String name, boolean doForce) {
        return heldInternMap.intern(name, doForce);
    }

    // Implementation intention:
    //
    // className -> Set<annotationName> : tell what annotations a class has
    // annotationName -> Set<className> : tell what classes have an annotation

    protected Map<String, Set<String>> i_holderToHeldMap;

    @Override
    @Trivial
    public Set<String> getHolderSet() {
        return (i_holderToHeldMap.keySet());
    }

    //

    protected Map<String, Set<String>> i_heldToHoldersMap;

    @Override
    @Trivial
    public Set<String> getHeldSet() {
        return (i_heldToHoldersMap.keySet());
    }

    //

    @Override
    @Trivial
    public boolean isEmpty() {
        String methodName = "isEmpty";
        boolean result = i_heldToHoldersMap.isEmpty();
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER / RETURN [ {1} ]", 
                new Object[] { getHashText(), Boolean.valueOf(result) });
        }
        return result;
        // return ( i_holdersToHoldersMap.isEmpty() ); // Would work, too.
    }

    //

    @Override
    @Trivial
    public boolean holds(String holderName, String heldName) {
        String methodName = "holds";

        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] [ {2} ] / RETURN [ false ] (holder not stored)", 
                    new Object[] { getHashText(), holderName, heldName } );
            }
            return false;
        }

        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);
        if (i_heldName == null) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] [ {2} ] / RETURN [ false ] (held not stored)", 
                    new Object[] { getHashText(), holderName, heldName } );
            }
            return false;
        }

        // Symmetric: Go through the holder map.  The held map would work just as well.

        Set<String> i_held = i_holderToHeldMap.get(i_holderName);

        boolean result = ((i_held == null) ? false : i_held.contains(i_heldName));
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] [ {2} ] / RETURN [ {3} ]",
                new Object[] { getHashText(), holderName, heldName, Boolean.valueOf(result) } );
        }
        return result;

        // Note (*):
        //
        // The held map can be null, in case the holder map is shared
        // (which is the case for the annotation targets maps, which
        // use a single class intern map for all of the package, class,
        // class method, and class field maps.
        //
        // Similarly, if the lookup was through the held map, the holders
        // mapping could be null.
    }

    //

    @Override
    @Trivial
    public Set<String> selectHeldOf(String holderName) {
        String methodName = "selectHeldOf";
        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ 0 ] (holder not stored)",
                    new Object[] { getHashText(), holderName });
            }
            return Collections.emptySet();
        }

        Set<String> i_held = i_selectHeldOf(i_holderName);
        if (i_held == null) { // See the note (*), above.
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ 0 ] (null held)",
                    new Object[] { getHashText(), holderName });
            }
            return Collections.emptySet();
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashText(), holderName, Integer.valueOf(i_held.size()) });
        }
        return i_held;
    }

    @Override
    @Trivial
    public Set<String> i_selectHeldOf(String i_holderName) {
        String methodName = "i_selectHeldOf";
        Set<String> result = i_holderToHeldMap.get(i_holderName);
        if ( logger.isLoggable(Level.FINER) ) {
            String resultString = ( (result == null) ? "null" : Integer.toString( result.size() ) );
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashText(), i_holderName, resultString });
        }
        return result;
    }

    @Override
    @Trivial
    public Set<String> selectHoldersOf(String heldName) {
        String methodName = "selectHoldersOf";
        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);
        if (i_heldName == null) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ 0 ] (held not stored)",
                    new Object[] { getHashText(), heldName });
            }
            return Collections.emptySet();
        }

        Set<String> i_holders = i_selectHoldersOf(i_heldName);
        if (i_holders == null) { // See the note (*), above.
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ 0 ] (null holders)",
                    new Object[] { getHashText(), heldName });
            }
            return Collections.emptySet();
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashText(), heldName, Integer.valueOf(i_holders.size()) });
        }
        return i_holders;
    }

    @Override
    public Set<String> i_selectHoldersOf(String i_heldName) {
        String methodName = "i_selectHoldersOf";
        Set<String> result = i_heldToHoldersMap.get(i_heldName);
        if ( logger.isLoggable(Level.FINER) ) {
            String resultString = ( (result == null) ? "null" : Integer.toString( result.size() ) );
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashText(), i_heldName, resultString });
        }
        return result;
    }

    //

    public boolean record(String holderName, String heldName) {
        return i_record( internHolder(holderName, Util_InternMap.DO_FORCE),
                         internHeld(heldName, Util_InternMap.DO_FORCE) );
    }

    @Trivial
    public boolean i_record(String i_holderName, String i_heldName) {
        String methodName = "i_record";

        boolean addedHeldToHolder = i_recordHolderToHeld(i_holderName, i_heldName);
        boolean addedHolderToHeld = i_recordHeldToHolder(i_holderName, i_heldName);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Holder [ {1} ] Held [ {2} ] [ {3} ]",
                new Object[] { getHashText(), i_holderName, i_heldName, Boolean.valueOf(addedHeldToHolder) });
        }

        if (addedHeldToHolder != addedHolderToHeld) {
            //  CWWKC0057W "ANNO_UTIL_MAPPING_INCONSISTENCY"
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] Holder [ {1} ] Held [ {2} ] Added to holder [ {3} ] Added to held [ {4} ]",
                    new Object[] {
                        getHashText(),
                        i_holderName,
                        i_heldName,
                        Boolean.valueOf(addedHeldToHolder),
                        Boolean.valueOf(addedHolderToHeld) });
        }

        return addedHeldToHolder;
    }

    @Trivial
    protected boolean i_recordHolderToHeld(String i_holderName, String i_heldName) {
        String methodName = "i_recordHolderToHeld";
        Set<String> i_held = i_recordHolder(i_holderName);
        boolean didAdd = i_held.add(i_heldName);
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Holder [ {1} ] Held [ {2} ] [ {3} ]",
                    new Object[] { getHashText(), i_holderName, i_heldName, Boolean.valueOf(didAdd) });
        }
        return didAdd;
    }

    @Trivial
    protected Set<String> i_recordHolder(String i_holderName) {
        String methodName = "i_recordHolder";
        
        Set<String> i_held = i_holderToHeldMap.get(i_holderName);
        if (i_held == null) {
            i_held = factory.createIdentityStringSet();
            i_holderToHeldMap.put(i_holderName, i_held);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Holder [ {1} ] Added",
                    new Object[] { getHashText(), i_holderName });
            }

        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Holder [ {1} ] Already present",
                    new Object[] { getHashText(), i_holderName });
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Holder [ {1} ] Held [ {2} ]",
                new Object[] { getHashText(), Integer.valueOf(i_held.size()) });
        }
        return i_held;
    }

    @Trivial
    protected boolean i_recordHeldToHolder(String i_holderName, String i_heldName) {
        String methodName = "i_recordHeldToHolder";
        Set<String> i_holders = i_recordHeld(i_heldName);
        boolean didAdd = i_holders.add(i_holderName);
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Holder [ {1} ] Held [ {2} ] [ {3} ]",
                    new Object[] { getHashText(), i_holderName, i_heldName, Boolean.valueOf(didAdd) });
        }
        return didAdd;

    }

    protected Set<String> i_recordHeld(String i_heldName) {
        String methodName = "i_recordHeld";

        Set<String> i_holders = i_heldToHoldersMap.get(i_heldName);
        if (i_holders == null) {
            i_holders = factory.createIdentityStringSet();
            i_heldToHoldersMap.put(i_heldName, i_holders);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Held [ {1} ] Added",
                    new Object[] { getHashText(), i_heldName });
            }

        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Held [ {1} ] Already present",
                    new Object[] { getHashText(), i_heldName });
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Holder [ {1} ] Holders [ {2} ]",
                new Object[] { getHashText(), Integer.valueOf(i_holders.size()) });
        }
        return i_holders;
    }

    public void i_record(UtilImpl_BidirectionalMap otherMap) {
        for ( String i_nextHolder : getHolderSet() ) {
            Set<String> i_held = i_selectHeldOf(i_nextHolder);
            if ( i_held == null ) {
                continue;
            }
            for ( String i_nextHeld : i_held ) {
                i_record(i_nextHolder, i_nextHeld);
            }
        }
    }

    public void i_record(UtilImpl_BidirectionalMap otherMap, Set<String> i_restrictedHolders) {
        for ( String i_otherHolder : otherMap.getHolderSet() ) {
            if ( !i_restrictedHolders.contains(i_otherHolder) ) {
                continue;
            }
            Set<String> i_otherHeld = otherMap.i_selectHeldOf(i_otherHolder);
            if ( i_otherHeld == null ) {
                continue;
            }
            for ( String i_otherHeldName : i_otherHeld ) {
                i_record(i_otherHolder, i_otherHeldName);
            }
        }
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
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BiDi Map: BEGIN: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Holder Intern Map:");
        getHolderInternMap().log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Held Intern Map:");
        getHeldInternMap().log(useLogger);

        logHolderMap(useLogger);

        logHeldMap(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BiDi Map: END: [ {0} ]", getHashText());
    }

    @Trivial
    public void logHolderMap(Logger useLogger) {
        String methodName = "logHolderMap";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Holder-to-held Map: BEGIN");

        for (Map.Entry<String, Set<String>> i_holderEntry : i_holderToHeldMap.entrySet()) {
            String i_holderName = i_holderEntry.getKey();
            Set<String> i_held = i_holderEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "  Holder [ {0} ] Held [ {1} ]",
                    new Object[] { i_holderName, i_held });
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Holder-to-held Map: END");
    }

    @Trivial
    public void logHeldMap(Logger useLogger) {
        String methodName = "logHeldMap";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Held-to-holder Map: BEGIN");

        for (Map.Entry<String, Set<String>> i_heldEntry : i_heldToHoldersMap.entrySet()) {
            String i_heldName = i_heldEntry.getKey();
            Set<String> i_holders = i_heldEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, 
                    "  Held [ {0} ] Holders [ {1} ]",
                    new Object[] { i_heldName, i_holders });
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Held-to-holder Map: END");
    }

    //

    /**
     * <p>Tell if two maps have the same contents.</p>
     *
     * @param otherMap The map to test against this map.
     * @param isCongruent Control parameter: Tells if the other map uses
     *     the same intern maps as this map.
     * @return True if the maps have the same contents.
     */
    public boolean i_equals(UtilImpl_BidirectionalMap otherMap, boolean isCongruent) {
        if ( otherMap == null ) {
            return false; // Null other map.
        } else if ( otherMap == this ) {
            return true; // Identical other map.
        }

        if ( i_holderToHeldMap.keySet().size() != otherMap.i_holderToHeldMap.keySet().size() ) {
            return false; // Unequal sizes of key sets.
        }

        for ( Map.Entry<String, Set<String>> i_holderEntry : i_holderToHeldMap.entrySet() ) {
            String i_holderName = i_holderEntry.getKey();
            Set<String> i_held = i_holderEntry.getValue();

            if ( isCongruent ) {
                Set<String> i_otherHeld = otherMap.i_holderToHeldMap.get(i_holderName);
                if ( i_otherHeld == null ) {
                    return false; // Other does not have the same key.
                } else if ( i_held.size() != i_otherHeld.size() ) {
                    return false; // Other has different count of values for the key.
                } else if ( !i_held.containsAll(i_otherHeld) ) {
                    return false; // Other does not have the same values for the key.
                }

            } else {
                String i_otherHolderName = otherMap.internHolder(i_holderName, Util_InternMap.DO_NOT_FORCE);
                if ( i_otherHolderName == null ) {
                    return false; // Other does not even store the key.
                }

                Set<String> i_otherHeld = otherMap.selectHeldOf(i_otherHolderName);
                if ( i_otherHeld == null ) {
                    return false; // Other does not have the same key.
                } else if ( i_held.size() != i_otherHeld.size() ) {
                    return false; // Other has different count of values for the key.
                } else {
                    for ( String i_heldName : i_held ) {
                        String i_otherHeldName = otherMap.internHeld(i_heldName, Util_InternMap.DO_NOT_FORCE);
                        if ( i_otherHeldName == null ) {
                            return false; // Other does not even store the value.
                        } else if ( !i_otherHeld.contains(i_otherHeldName)) {
                            return false; // Other does not have the value for the key.
                        }
                    }
                }
            }
        }

        return true; // All the same.
    }

    //

    /**
     * Update value resolution tables with held data from this mapping.
     *
     * Holders are not processed.
     *
     * For each held value, if the held is not resolved, record it as
     * unresolved, and if not previously recorded as unresolved, record
     * it as newly unresolved.
     *
     * @param i_allResolved The revolved held values.
     * @param i_newlyResolved Newly resolved held values.  Not currently used.
     * @param i_allUnresolved Known unresolved held values.
     * @param i_newlyUnresolved Newly discovered unresolved held values.
     */
    @Trivial
    public void update(
        Set<String> i_allResolved, Set<String> i_newlyResolved,
        Set<String> i_allUnresolved, Set<String> i_newlyUnresolved) {

        String methodName = "update";

        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashText(), null };

            logParms[1] = Integer.toString( i_allResolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] All resolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_newlyResolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] New resolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_allUnresolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial all unresolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_newlyUnresolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial new unresolved [ {1} ]", logParms);
        } else {
            logParms = null;
        }

        for ( Set<String> i_held : i_holderToHeldMap.values() ) {
            for ( String i_heldName : i_held ) {
                if ( i_allResolved.contains(i_heldName) ) {
                    continue;
                }

                if ( i_allUnresolved.add(i_heldName) ) {
                    if ( logParms != null ) {
                        logParms[1] = i_heldName;
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] New unresolved [ {1} ]", logParms);
                    }
                    i_newlyUnresolved.add(i_heldName);
                }
            }
        }

        if ( logParms != null ) {
            logParms[1] = Integer.toString( i_allUnresolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final all unresolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_newlyUnresolved.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final new unresolved [ {1} ]", logParms);
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent useLogger) {
        Tr.debug(useLogger, MessageFormat.format("BEGIN Intern Map [ {0} ]:", getHashText()));

        Tr.debug(useLogger, MessageFormat.format("BiDi Map: BEGIN: [ {0} ]", getHashText()));

        Tr.debug(useLogger, "Holder Intern Map:");
        getHolderInternMap().log(useLogger);

        Tr.debug(useLogger, "Held Intern Map:");
        getHeldInternMap().log(useLogger);

        logHolderMap(useLogger);

        logHeldMap(useLogger);

        Tr.debug(useLogger, MessageFormat.format("BiDi Map: END: [ {0} ]", getHashText()));
    }

    @Trivial
    public void logHolderMap(TraceComponent useLogger) {
        Tr.debug(useLogger, "Holder-to-held Map: BEGIN");

        for (Map.Entry<String, Set<String>> i_holderEntry : i_holderToHeldMap.entrySet()) {
            String i_holderName = i_holderEntry.getKey();
            Set<String> i_held = i_holderEntry.getValue();

            Tr.debug(useLogger,
            		 MessageFormat.format("  Holder [ {0} ] Held [ {1} ]",
            				              i_holderName, i_held));
        }

        Tr.debug(useLogger, "Holder-to-held Map: END");
    }

    @Trivial
    public void logHeldMap(TraceComponent useLogger) {
        Tr.debug(useLogger, "Held-to-holder Map: BEGIN");

        for ( Map.Entry<String, Set<String>> i_heldEntry : i_heldToHoldersMap.entrySet() ) {
            String i_heldName = i_heldEntry.getKey();
            Set<String> i_holders = i_heldEntry.getValue();

            Tr.debug(useLogger,
                     MessageFormat.format("  Held [ {0} ] Holders [ {1} ]",
                    		              i_heldName, i_holders));
        }

        Tr.debug(useLogger, "Held-to-holder Map: END");
    }

    //

	@Override
	public boolean getIsEnabled() {
		return true;
	}
}
