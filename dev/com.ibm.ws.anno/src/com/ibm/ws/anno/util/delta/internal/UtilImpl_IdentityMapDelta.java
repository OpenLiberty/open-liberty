/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.util.delta.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.util.Util_IdentityMapDelta;
import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public class UtilImpl_IdentityMapDelta implements Util_IdentityMapDelta {

    public static final String CLASS_NAME = UtilImpl_IdentityMapDelta.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_IdentityMapDelta(UtilImpl_Factory factory) {
        this(factory,

             UtilImpl_Factory.DO_RECORD_ADDED,
             UtilImpl_Factory.DO_RECORD_REMOVED,
             UtilImpl_Factory.DO_RECORD_CHANGED,
             UtilImpl_Factory.DO_NOT_RECORD_STILL);
    }

    public UtilImpl_IdentityMapDelta(
        UtilImpl_Factory factory,
        int expectedAdded, int expectedRemoved) {

        this(factory,

             UtilImpl_Factory.DO_RECORD_ADDED,
             UtilImpl_Factory.DO_RECORD_REMOVED,
             UtilImpl_Factory.DO_RECORD_CHANGED,
             UtilImpl_Factory.DO_NOT_RECORD_STILL,

             expectedAdded,
             expectedRemoved,
             UtilImpl_Factory.ZERO_CHANGED,
             UtilImpl_Factory.ZERO_STILL);
    }

    public UtilImpl_IdentityMapDelta(
        UtilImpl_Factory factory,
        boolean recordAdded, boolean recordRemoved, boolean recordChanged, boolean recordStill) {

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.addedMap = factory.createIdentityMap(recordAdded, UtilImpl_Factory.ANY_NUMBER_OF_ADDED);
        this.removedMap = factory.createIdentityMap(recordRemoved, UtilImpl_Factory.ANY_NUMBER_OF_REMOVED);
        this.changedMap = factory.createValuesIdentityMap(recordChanged, UtilImpl_Factory.ANY_NUMBER_OF_CHANGED);
        this.stillMap = factory.createIdentityMap(recordStill, UtilImpl_Factory.ANY_NUMBER_OF_STILL);
    }

    public UtilImpl_IdentityMapDelta(
        UtilImpl_Factory factory,
        boolean recordAdded, boolean recordRemoved, boolean recordChanged, boolean recordStill,
        int expectedAdded, int expectedRemoved, int expectedChanged, int expectedStill) {

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.addedMap = factory.createIdentityMap(recordAdded, expectedAdded);
        this.removedMap = factory.createIdentityMap(recordRemoved, expectedRemoved);
        this.changedMap = factory.createValuesIdentityMap(recordChanged, expectedChanged);
        this.stillMap = factory.createIdentityMap(recordStill, expectedStill);
    }

    //

    protected final Map<String, String> addedMap;
    protected final Map<String, String> removedMap;
    protected final Map<String, String[]> changedMap;
    protected final Map<String, String> stillMap;

    @Override
    public Map<String, String> getAddedMap() {
        return addedMap;
    }

    public void addAdded(String added, String addedValue) {
        if ( addedMap != null ) {
            addedMap.put(added, addedValue);
        }
    }

    public void addAdded(Map<String, String> added) {
        if ( addedMap != null ) {
            addedMap.putAll(added);
        }
    }

    @Override
    public boolean isNullAdded() {
        return ((addedMap == null) || addedMap.isEmpty());
    }

    @Override
    public Map<String, String> getRemovedMap() {
        return removedMap;
    }

    public void addRemoved(String removed, String removedValue) {
        if ( removedMap != null ) {
            removedMap.put(removed, removedValue);
        }
    }

    public void addRemoved(Map<String, String> removed) {
        if ( removedMap != null ) {
            removedMap.putAll(removed);
        }
    }

    protected final boolean AS_ADDED = true;
    protected final boolean AS_REMOVED = false;

    public void add(Map<String, String> map, boolean asAdded) {
        if ( asAdded ) {
            addAdded(map);
        } else {
            addRemoved(map);
        }
    }

    @Override
    public boolean isNullRemoved() {
        return ((removedMap == null) || removedMap.isEmpty());
    }

    @Override
    public Map<String, String[]> getChangedMap() {
        return changedMap;
    }

    public void addChanged(String changed, String newChangedValue, String oldChangedValue) {
        if ( changedMap != null ) {
            changedMap.put(changed, new String[] { newChangedValue, oldChangedValue });
        }
    }

    @Override
    public boolean isNullChanged() {
        return ((changedMap == null) || changedMap.isEmpty());
    }

    @Override
    public Map<String, String> getStillMap() {
        return stillMap;
    }

    public void addStill(String still, String stillValue) {
        if ( stillMap != null ) {
            stillMap.put(still, stillValue);
        }
    }

    public void addStill(Map<String, String> still) {
        if ( stillMap != null ) {
            stillMap.putAll(still);
        }
    }

    @Override
    public boolean isNullUnchanged() {
        return ((stillMap == null) || stillMap.isEmpty());
    }

    @Override
    public boolean isNull() {
        return ( isNullAdded() &&
                 isNullRemoved() &&
                 isNullChanged() );
    }

    //

    @Override
    public void subtract(Map<String, String> finalMap, Map<String, String> initialMap) {
        subtract(finalMap, null, initialMap, null);
    }

    @Override
    public void subtract(
        Map<String, String> finalMap, Util_InternMap finalDomain,
        Map<String, String> initialMap, Util_InternMap initialDomain) {

        for ( Map.Entry<String, String> finalEntry : finalMap.entrySet() ) {
            String finalKey = finalEntry.getKey();
            String finalValue = finalEntry.getValue();

            String asInitial_finalKey = intern(finalKey, initialDomain);
            String initialValue = get(asInitial_finalKey, initialMap);

            if ( initialValue == null ) {
                if ( addedMap != null ) {
                    addedMap.put(finalKey, finalValue);
                }
            } else if ( ((initialValue == null) && (finalValue != null)) || 
                        ((initialValue != null) && (finalValue == null)) ||
                        ((initialValue != null) && (finalValue != null) && !initialValue.equals(finalValue)) ) {
                if ( changedMap != null ) {
                    changedMap.put(finalKey, new String[] { finalValue, initialValue });
                }
            } else {
                if ( stillMap != null ) {
                    stillMap.put(finalKey, finalValue);
                }
            }
        }

        for ( Map.Entry<String, String> initialEntry : initialMap.entrySet() ) {
            String initialKey = initialEntry.getKey();
            String initialValue = initialEntry.getValue();

            String asFinal_initialKey = intern(initialKey, finalDomain);
            String finalValue = get(asFinal_initialKey, finalMap);

            if ( finalValue == null ) {
                if ( removedMap != null ) {
                    removedMap.put(initialKey, initialValue);
                }
            } else if ( ((initialValue == null) && (finalValue != null)) || 
                        ((initialValue != null) && (finalValue == null)) ||
                        ((initialValue != null) && (finalValue != null) && !initialValue.equals(finalValue)) ) {
                // Already recorded.
                // if ( changedMap != null ) {
                //     changedMap.put(initialKey, new String[] { initialValue, initialValue });
                // }
            } else {
                // Already recorded.
                // if ( stillMap != null ) {
                //     stillMap.put(initialKey, initialValue);
                // }
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

    private String get(String i_key, Map<String, String> map) {
        if ( i_key == null ) {
            return null;
        } else {
            return map.get(i_key);
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

    public void log(Util_PrintLogger useLogger) {
        String methodName = "log";

        if ( isNull() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "** UNCHANGED **");
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added:");
        if ( addedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( addedMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int addedNo = 0;
            for ( Map.Entry<String, String> addedEntry : addedMap.entrySet() ) {
                if ( addedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + addedMap.entrySet().size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ {0} ] {1}: {2}",
                        Integer.valueOf(addedNo),
                        addedEntry.getKey(), addedEntry.getValue());
                }
                addedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed:");
        if ( removedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( removedMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int removedNo = 0;
            for ( Map.Entry<String, String> removedEntry : removedMap.entrySet() ) {
                if ( removedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + removedMap.entrySet().size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ {0} ] {1}: {2}",
                        Integer.valueOf(removedNo),
                        removedEntry.getKey(), removedEntry.getValue());
                }
                removedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Changed:");
        if ( changedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( changedMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int changedNo = 0;
            for ( Map.Entry<String, String[]> changedEntry : changedMap.entrySet() ){
                if ( changedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + changedMap.entrySet().size() + " ]");
                    break;
                } else {
                    String[] valueDelta = changedEntry.getValue();
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  {0}: {1} :: ( {2} )",
                        changedEntry.getKey(),
                        valueDelta[FINAL_VALUE_OFFSET], valueDelta[INITIAL_VALUE_OFFSET]);
                }
                changedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still:");
        if ( stillMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( stillMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + stillMap.entrySet().size() + " ]");
//            for ( Map.Entry<String, String> stillEntry : stillMap.entrySet() ){
//                useLogger.logp(Level.FINER, CLASS_NAME, methodName,
//                               "  {0}: {1}",
//                               stillEntry.getKey(), stillEntry.getValue());
//            }
        }
    }

    @Override
    public void describe(String prefix, List<String> nonNull) {
        if ( !isNullAdded() ) {
            nonNull.add(prefix + " Added [ " + getAddedMap().keySet().size() + " ]");
        }
        if ( !isNullRemoved() ) {
            nonNull.add(prefix + " Removed [ " + getRemovedMap().keySet().size() + " ]");
        }
    }
}
