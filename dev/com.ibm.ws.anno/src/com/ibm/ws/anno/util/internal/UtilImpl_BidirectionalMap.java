/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.util.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class UtilImpl_BidirectionalMap implements Util_BidirectionalMap {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.util");
    private static final Logger stateLogger = Logger.getLogger("com.ibm.ws.anno.util.state");

    public static final String CLASS_NAME = "UtilImpl_BidirectionalMap";

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected UtilImpl_BidirectionalMap(UtilImpl_Factory factory,
                                        String holderTag, String heldTag,
                                        UtilImpl_InternMap holderInternMap,
                                        UtilImpl_InternMap heldInternMap) {
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

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final UtilImpl_Factory factory;

    @Override
    public UtilImpl_Factory getFactory() {
        return factory;
    }

    //

    @Deprecated
    @Override
    public boolean getEnabled() {
        return true;
    }

    //

    protected final String holderTag;

    @Override
    public String getHolderTag() {
        return holderTag;
    }

    protected String heldTag;

    @Override
    public String getHeldTag() {
        return heldTag;
    }

    //

    protected final UtilImpl_InternMap holderInternMap;

    @Override
    public UtilImpl_InternMap getHolderInternMap() {
        return holderInternMap;
    }

    @Override
    public boolean containsHolder(String holderName) {
        return (holderInternMap.contains(holderName));
    }

    protected String internHolder(String name, boolean doForce) {
        return holderInternMap.intern(name, doForce);
    }

    protected UtilImpl_InternMap heldInternMap;

    @Override
    public UtilImpl_InternMap getHeldInternMap() {
        return heldInternMap;
    }

    @Override
    public boolean containsHeld(String heldName) {
        return (heldInternMap.contains(heldName));
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
    public Set<String> getHolderSet() {
        return (i_holderToHeldMap.keySet());
    }

    //

    protected Map<String, Set<String>> i_heldToHoldersMap;

    @Override
    public Set<String> getHeldSet() {
        return (i_heldToHoldersMap.keySet());
    }

    //

    @Override
    public boolean isEmpty() {
        return ( i_heldToHoldersMap.isEmpty() );
        // return ( i_holdersToHoldersMap.isEmpty() ); // Would work, too.
    }

    //

    @Override
    public boolean holds(String holderName, String heldName) {
        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            return false;
        }

        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);
        if (i_heldName == null) {
            return false;
        }

        // Symmetric: Go through the holder map.  The held map would work just as well.

        Set<String> i_held = i_holderToHeldMap.get(i_holderName);

        return ((i_held == null) ? false : i_held.contains(i_heldName));

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
    public Set<String> selectHeldOf(String holderName) {
        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            return Collections.emptySet();
        }

        Set<String> i_held = i_selectHeldOf(i_holderName);
        if (i_held == null) { // See the note (*), above.
            return Collections.emptySet();
        }

        return i_held;
    }

    @Override
    public Set<String> i_selectHeldOf(String i_holderName) {
        return i_holderToHeldMap.get(i_holderName);
    }

    @Override
    public Set<String> selectHoldersOf(String heldName) {
        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);
        if (i_heldName == null) {
            return Collections.emptySet();
        }

        Set<String> i_holders = i_selectHoldersOf(i_heldName);
        if (i_holders == null) { // See the note (*), above.
            return Collections.emptySet();
        }

        return i_holders;
    }

    @Override
    public Set<String> i_selectHoldersOf(String i_heldName) {
        return i_heldToHoldersMap.get(i_heldName);
    }

    //

    public boolean record(String holderName, String heldName) {
        return i_record(internHolder(holderName, Util_InternMap.DO_FORCE),
                        internHeld(heldName, Util_InternMap.DO_FORCE));
    }

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

    protected boolean i_recordHolderToHeld(String i_holderName, String i_heldName) {
        Set<String> i_held = i_recordHolder(i_holderName);
        return i_held.add(i_heldName);
    }

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

        return i_held;
    }

    protected boolean i_recordHeldToHolder(String i_holderName, String i_heldName) {
        Set<String> i_holders = i_recordHeld(i_heldName);
        return i_holders.add(i_holderName);
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
    public void logState() {
        if ( !stateLogger.isLoggable(Level.FINER) ) {
            return;
        }

        log(logger);
    }

    @Override
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
     *
     * @return True if the maps have the same contents.
     */
    public boolean i_equals(UtilImpl_BidirectionalMap otherMap) {
        // The other map is assumed to use the same intern maps as this
        // map.  That assumption ties to the use of 'containsAll' on the
        // held and holder sets, which are identity maps.
        //
        // If the other map uses string which do not use the same intern maps,
        // an extra layer of intern'ing of the other key and set element strings
        // would be necessary.

        if ( otherMap == null ) {
            return false;
        } else if ( otherMap == this ) {
            return true;
        }

        if ( i_holderToHeldMap.keySet().size() != otherMap.i_holderToHeldMap.keySet().size() ) {
            return false;
        }

        for ( Map.Entry<String, Set<String>> i_holderEntry : i_holderToHeldMap.entrySet() ) {
            String i_holderName = i_holderEntry.getKey();
            Set<String> i_held = i_holderEntry.getValue();

            Set<String> i_otherHeld = otherMap.i_holderToHeldMap.get(i_holderName);
            if ( i_otherHeld == null ) {
                return false;
            } else if ( i_held.size() != i_otherHeld.size() ) {
                return false;
            } else if ( !i_held.containsAll(i_otherHeld) ) {
                return false;
            }

            // // Alternate for comparing maps which do not share the same intern maps:
            //
            // Set<String> i_otherHeld = otherMap.selectHeldOf(i_holderName);
            // if ( i_otherHeld == null ) {
            //     return false;
            // } else if ( i_held.size() != i_otherHeld.size() ) {
            //     return false;
            // } else {
            //     for ( String i_heldName : i_held ) {
            //         String i_otherHeldName = otherMap.internHeld(i_heldName, Util_InternMap.DO_NOT_FORCE);
            //         if ( i_otherHeldName == null ) {
            //             return false;
            //         } else if ( !i_otherHeld.contains(i_otherHeldName)) {
            //             return false;
            //         }
            //     }
            // }
        }

        return true;
    }

    //

    public void update(Set<String> i_resolved, Set<String> i_newlyResolved,
                       Set<String> i_unresolved, Set<String> i_newlyUnresolved) {

        for ( Set<String> i_held : i_holderToHeldMap.values() ) {
            for ( String i_heldName : i_held ) {
                if ( i_resolved.contains(i_heldName) ) {
                    continue;
                }

                if ( i_unresolved.add(i_heldName) ) {
                    i_newlyUnresolved.add(i_heldName);
                }
            }
        }
    }
}
