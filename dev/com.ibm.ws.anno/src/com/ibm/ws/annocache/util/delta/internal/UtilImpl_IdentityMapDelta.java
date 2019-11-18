/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.util.Util_IdentityMapDelta;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

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

        this.addedMap_f = factory.createIdentityMap(recordAdded, UtilImpl_Factory.ANY_NUMBER_OF_ADDED);
        this.removedMap_i = factory.createIdentityMap(recordRemoved, UtilImpl_Factory.ANY_NUMBER_OF_REMOVED);
        this.changedMap_f = factory.createValuesIdentityMap(recordChanged, UtilImpl_Factory.ANY_NUMBER_OF_CHANGED);
        this.stillMap_f = factory.createIdentityMap(recordStill, UtilImpl_Factory.ANY_NUMBER_OF_STILL);
    }

    public UtilImpl_IdentityMapDelta(
        UtilImpl_Factory factory,
        boolean recordAdded, boolean recordRemoved, boolean recordChanged, boolean recordStill,
        int expectedAdded, int expectedRemoved, int expectedChanged, int expectedStill) {

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.addedMap_f = factory.createIdentityMap(recordAdded, expectedAdded);
        this.removedMap_i = factory.createIdentityMap(recordRemoved, expectedRemoved);
        this.changedMap_f = factory.createValuesIdentityMap(recordChanged, expectedChanged);
        this.stillMap_f = factory.createIdentityMap(recordStill, expectedStill);
    }

    //

    // All keys and values of 'added' are from the domain of the final map.
    //
    // All keys and values of 'removed' are from the domain of the initial map.
    //
    // The keys of the changed map are from the domain of the final map.
    // The changed values are, respectively, the value from the final map,
    // which is from the final domain, and the value from the initial map,
    // which is from the initial domain.  This is a biased recording, since it
    // it handles the initial and final keys asymmetrically.  An unbiased
    // representation would have two changed maps: One with keys from the
    // final map, and the other with keys from the initial map.
    //
    // Currently, all keys and values of 'still' are from the domain of final map,
    // but, there *could* be two still maps, one for each of the maps.
    // That would be potentially useful for keeping values from
    // the initial and final domains apart.

    protected final Map<String, String> addedMap_f;
    protected final Map<String, String> removedMap_i;
    protected final Map<String, String[]> changedMap_f;
    protected final Map<String, String> stillMap_f;

    // protected final Map<String, String[]> changedMap_i;
    // protected final Map<String, String> stillMap_i;

    @Override
    public Map<String, String> getAddedMap() {
        return addedMap_f;
    }

    public void recordAdded(String addedKey_f, String addedValue_f) {
        if ( addedMap_f != null ) {
            addedMap_f.put(addedKey_f, addedValue_f);
        }
    }

    public void recordAdded(Map<String, String> added_f) {
        if ( addedMap_f != null ) {
            addedMap_f.putAll(added_f);
        }
    }

    @Override
    public boolean isNullAdded() {
        return ((addedMap_f == null) || addedMap_f.isEmpty());
    }

    @Override
    public Map<String, String> getRemovedMap() {
        return removedMap_i;
    }

    public void recordRemoved(String removedKey_i, String removedValue_i) {
        if ( removedMap_i != null ) {
            removedMap_i.put(removedKey_i, removedValue_i);
        }
    }

    public void recordRemoved(Map<String, String> removed_i) {
        if ( removedMap_i != null ) {
            removedMap_i.putAll(removed_i);
        }
    }

    protected final boolean AS_ADDED = true;
    protected final boolean AS_REMOVED = false;

    public void recordTransfer(Map<String, String> transferMap, boolean asAdded) {
        if ( asAdded ) {
            recordAdded(transferMap);
        } else {
            recordRemoved(transferMap);
        }
    }

    @Override
    public boolean isNullRemoved() {
        return ((removedMap_i == null) || removedMap_i.isEmpty());
    }

    @Override
    public Map<String, String[]> getChangedMap() {
        return changedMap_f;
    }

    public void recordChanged(String changedKey_f, String value_f, String value_i) {
        if ( changedMap_f != null ) {
            changedMap_f.put(changedKey_f, new String[] { value_f, value_i });
        }
    }

    @Override
    public boolean isNullChanged() {
        return ((changedMap_f == null) || changedMap_f.isEmpty());
    }

    @Override
    public Map<String, String> getStillMap() {
        return stillMap_f;
    }

    public void recordStill(String stillKey_f, String stillValue_f) {
        if ( stillMap_f != null ) {
            stillMap_f.put(stillKey_f, stillValue_f);
        }
    }

    public void recordStill(Map<String, String> still) {
        if ( stillMap_f != null ) {
            stillMap_f.putAll(still);
        }
    }

    @Override
    public boolean isNullUnchanged() {
        return ((stillMap_f == null) || stillMap_f.isEmpty());
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

        if ( ((finalMap == null) || finalMap.isEmpty()) &&
             ((initialMap == null) || initialMap.isEmpty()) ) {
            // Nothing to do: Both map are empty.

        } else if ( (finalMap == null) || finalMap.isEmpty() ) {
            // Everything in the initial map was removed.

            if ( removedMap_i != null ) {
                removedMap_i.putAll(initialMap);
            }

        } else if ( (initialMap == null) || initialMap.isEmpty() ) {
            // Everything in the final map was added.

            if ( addedMap_f != null ) {
                addedMap_f.putAll(finalMap);
            }

        } else {
            if ( initialDomain == finalDomain ) {
                // When the domains are the same, which includes the case
                // when both domains are null, the step of interning the
                // element values across domains is skipped.

                for ( Map.Entry<String, String> entry_f : finalMap.entrySet() ) {
                    String key_f = entry_f.getKey();
                    String value_f = entry_f.getValue();

                    if ( !initialMap.containsKey(key_f) ) {
                        addedMap_f.put(key_f,  value_f);

                    } else {
                        String value_i = initialMap.get(key_f);

                        if ( ((value_i == null) && (value_f != null)) ||
                             ((value_i != null) && (value_f == null)) ||
                             ((value_i != null) && (value_f != null) && !value_f.equals(value_i)) ) {
                            if ( changedMap_f != null ) {
                                recordChanged(key_f, value_f, value_i);
                            }
                        } else {
                            if ( stillMap_f != null ) {
                                recordStill(key_f, value_f);
                            }
                        }
                    }
                }

                for ( Map.Entry<String, String> entry_i : initialMap.entrySet() ) {
                    String key_i = entry_i.getKey();
                    String value_i = entry_i.getValue();

                    if ( !finalMap.containsKey(key_i) ) {
                        addedMap_f.put(key_i,  value_i);

                    } else {
                        String value_f = finalMap.get(key_i);
                        
                        if ( ((value_f == null) && (value_i != null)) ||
                             ((value_f != null) && (value_i == null)) ||
                             ((value_f != null) && (value_i != null) && !value_i.equals(value_f)) ) {

                            if ( changedMap_f != null ) {
                                recordChanged(key_i, value_i, value_f);
                            }
                        } else {
                            // if ( stillMap_i != null ) {
                            //     recordStill(key_i, value_i);
                            // }
                        }
                    }
                }

            } else {
                // When the domains are different, values must be interned across
                // domains before doing membership tests.

//                for ( Map.Entry<String, String> finalEntry : finalMap.entrySet() ) {
//                    String finalKey = finalEntry.getKey();
//                    String finalValue = finalEntry.getValue();
//                    System.out.println("Final Key [ " + finalKey + " ] Value [ " + finalValue + " ]");
//                }
//
//                for ( Map.Entry<String, String> initialEntry : initialMap.entrySet() ) {
//                    String initialKey = initialEntry.getKey();
//                    String initialValue = initialEntry.getValue();
//                    System.out.println("Initial Key [ " + initialKey + " ] Value [ " + initialValue + " ]");
//                }

                for ( Map.Entry<String, String> entry_f : finalMap.entrySet() ) {
                    String key_f = entry_f.getKey();
                    String value_f = entry_f.getValue();

                    String key_i = initialDomain.intern(key_f, Util_InternMap.DO_NOT_FORCE);

                    // An final key that does cross into the initial domain cannot
                    // be key of the initial map.
                    if ( (key_i == null) || !initialMap.containsKey(key_i) ) {
                        addedMap_f.put(key_f, value_f);

                    } else {
                        String value_i = initialMap.get(key_i);

                        if ( ((value_i == null) && (value_f != null)) ||
                             ((value_i != null) && (value_f == null)) ||
                             ((value_i != null) && (value_f != null) && !value_f.equals(value_i)) ) {
                            if ( changedMap_f != null ) {
                                recordChanged(key_f, value_f, value_i);
                            }
                        } else {
                            if ( stillMap_f != null ) {
                                recordStill(key_f, value_f);
                            }
                        }
                    }
                }

                for ( Map.Entry<String, String> entry_i : initialMap.entrySet() ) {
                    String key_i = entry_i.getKey();
                    String value_i = entry_i.getValue();

                    String key_f = finalDomain.intern(key_i, Util_InternMap.DO_NOT_FORCE);

                    // An initial key that did not cross into the final domain cannot
                    // be key of the final map.
                    if ( (key_f == null) || !finalMap.containsKey(key_f) ) {
                        addedMap_f.put(key_i,  value_i);

                    } else {
                        // Changes should have already been recorded
                        // when processing the final map.
                        //
                        // Note that 'recordChanged' in this case exposes
                        // a weakness of how changes are recorded, in that
                        // that record has a bias towards the final key.

                        // String value_f = finalMap.get(key_f);

                        // if ( ((value_f == null) && (value_i != null)) ||
                        //         ((value_f != null) && (value_i == null)) ||
                        //         ((value_f != null) && (value_i != null) && !value_i.equals(value_f)) ) {
                        //     if ( changedMap_f != null ) {
                        //            recordChanged(key_i, value_i, value_f);
                        //     }
                        // } else {
                        //     if ( stillMap != null ) {
                        //         recordStill(initialKey, initialValue);
                        //     }
                        // }
                    }
                }
            }
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
        if ( addedMap_f == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( addedMap_f.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int addedNo = 0;
            for ( Map.Entry<String, String> addedEntry : addedMap_f.entrySet() ) {
                if ( addedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + addedMap_f.entrySet().size() + " ]");
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
        if ( removedMap_i == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( removedMap_i.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int removedNo = 0;
            for ( Map.Entry<String, String> removedEntry : removedMap_i.entrySet() ) {
                if ( removedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + removedMap_i.entrySet().size() + " ]");
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
        if ( changedMap_f == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( changedMap_f.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int changedNo = 0;
            for ( Map.Entry<String, String[]> changedEntry : changedMap_f.entrySet() ){
                if ( changedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + changedMap_f.entrySet().size() + " ]");
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
        if ( stillMap_f == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( stillMap_f.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + stillMap_f.entrySet().size() + " ]");
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
