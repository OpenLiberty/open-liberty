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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.util.Util_IdentitySetDelta;
import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public class UtilImpl_IdentitySetDelta implements Util_IdentitySetDelta {

    public static final String CLASS_NAME = UtilImpl_IdentitySetDelta.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_IdentitySetDelta(boolean recordAdded, boolean recordRemoved, boolean recordStill) {
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.addedMap = (recordAdded ? new IdentityHashMap<String, String>() : null );
        this.removedMap = (recordRemoved ? new IdentityHashMap<String, String>() : null );
        this.stillMap = (recordRemoved ? new IdentityHashMap<String, String>() : null );
    }

    protected final Map<String, String> addedMap;
    protected final Map<String, String> removedMap;
    protected final Map<String, String> stillMap;

    @Override
    public Set<String> getAdded() {
        return ( (addedMap == null) ? null : addedMap.keySet() );
    }

    @Override
    public boolean isNullAdded() {
        return ((addedMap == null) || addedMap.isEmpty());
    }

    @Override
    public Set<String> getRemoved() {
        return ( (removedMap == null) ? null : removedMap.keySet() );
    }

    @Override
    public boolean isNullRemoved() {
        return ((removedMap == null) || removedMap.isEmpty());
    }

    @Override
    public Set<String> getStill() {
        return ( (stillMap == null) ? null : stillMap.keySet() );
    }

    @Override
    public boolean isNullStill() {
        return ((stillMap == null) || stillMap.isEmpty());
    }

    @Override
    public boolean isNull() {
        return ( isNullAdded() && isNullRemoved() );
    }

    @Override
    public boolean isNull(boolean ignoreRemoved) {
        return ( isNullAdded() && (ignoreRemoved || isNullRemoved()) );
    }

    //

    @Override
    public void describe(String prefix, List<String> nonNull) {
        if ( !isNullAdded() ) {
            nonNull.add(prefix + " Added [ " + getAdded().size() + " ]");
        }
        if ( !isNullRemoved() ) {
            nonNull.add(prefix + " Removed [ " + getAdded().size() + " ]");
        }
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

        for ( String finalKey : finalMap.keySet() ) {
            String useFinalKey;
            if ( initialMap == null ) {
                useFinalKey = finalKey;
            } else {
                useFinalKey = initialDomain.intern(finalKey, Util_InternMap.DO_NOT_FORCE);
            }
            if ( (useFinalKey == null) || !initialMap.containsKey(useFinalKey) ) {
                if ( addedMap != null ) {
                    addedMap.put(finalKey, finalKey);
                }
            } else {
                if ( stillMap != null ) {
                    stillMap.put(finalKey, finalKey);
                }
            }
        }

        for ( String initialKey : initialMap.keySet() ) {
            String useInitialKey;
            if ( finalMap == null ) {
                useInitialKey = initialKey;
            } else {
                useInitialKey = finalDomain.intern(initialKey, Util_InternMap.DO_NOT_FORCE);
            }
            if ( (useInitialKey == null) || !finalMap.containsKey(useInitialKey) ) {
                if ( removedMap != null ) {
                    removedMap.put(initialKey, initialKey);
                }
            } else {
                // if ( stillMap != null ) {
                //     stillMap.put(initialKey, initialKey);
                // }
            }
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        if ( !useLogger.isLoggable(Level.FINER)) {
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

        if ( isNull() ) {
            if ( stillMap == null ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "** UNCHANGED **");
            } else {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "** UNCHANGED [ " + stillMap.keySet().size() + " ] **");
            }
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added:");
        if ( addedMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( addedMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int addedNo = 0;
            for ( String added : addedMap.keySet() ){
                if ( addedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ ... " + addedMap.keySet().size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + addedNo + " ]  "  + added);
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
            for ( String removed : removedMap.keySet() ){
                if ( removedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ ... " + removedMap.keySet().size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + removedNo + " ]  "  + removed);
                }
                removedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still:");
        if ( stillMap == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( stillMap.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + stillMap.keySet().size() + " ]");
            // for ( String still : stillMap.keySet() ){
            //     useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  "  + still);
            // }
        }
    }
}
