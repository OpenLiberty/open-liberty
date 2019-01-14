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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.util.delta.internal.UtilImpl_BidirectionalMapDelta;
import com.ibm.ws.anno.util.delta.internal.UtilImpl_IdentityMapDelta;
import com.ibm.ws.anno.util.delta.internal.UtilImpl_IdentitySetDelta;
import com.ibm.wsspi.anno.util.Util_Exception;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;
import com.ibm.wsspi.anno.util.Util_RelativePath;

public class UtilImpl_Factory implements Util_Factory {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.util");
    
    public static final String CLASS_NAME = "UtilImpl_Factory";

    //

    protected String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_Factory(AnnotationServiceImpl_Service annoService) {
        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.annoService = annoService;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Created", this.hashText);
        }
    }

    //

    protected final AnnotationServiceImpl_Service annoService;

    @Trivial
    public AnnotationServiceImpl_Service getAnnotationService() {
        return annoService;
    }

    //

    @Trivial
    public Util_Exception newUtilException(Logger useLogger, String message) {
        String methodName = "newUtilException";

        Util_Exception exception = new Util_Exception(message);

        if (useLogger.isLoggable(Level.FINER)) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Message [ {0} ]", message);
        }

        return exception;
    }

    //

    @Override
    public UtilImpl_IdentityStringSet createIdentityStringSet() {
        return new UtilImpl_IdentityStringSet();
    }

    @Override
    public Set<String> createIdentityStringSet(int size) {
        return new UtilImpl_IdentityStringSet(size);
    }

    @Override
    public Set<String> createIdentityStringSet(Set<String> initialElements) {
        Set<String> result = new UtilImpl_IdentityStringSet( initialElements.size() );
        result.addAll(initialElements);
        return result;
    }

    @Override
    public UtilImpl_InternMap createInternMap(ValueType valueType, String name) {
        return new UtilImpl_InternMap(this, valueType, name);
    }

    @Override
    public UtilImpl_EmptyInternMap createEmptyInternMap(ValueType valueType, String name) {
        return new UtilImpl_EmptyInternMap(this, valueType, name);
    }

    @Override
    public UtilImpl_EmptyBidirectionalMap createEmptyBidirectionalMap(ValueType holderType, String holderTag,
                                                                      ValueType heldType, String heldTag) {
        return new UtilImpl_EmptyBidirectionalMap(this, holderType, holderTag, heldType, heldTag);
    }

    @Override
    public UtilImpl_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                            ValueType heldType, String heldTag) {

        UtilImpl_InternMap heldInternMap = createInternMap(holderType, holderTag);
        UtilImpl_InternMap holderInternMap = createInternMap(heldType, heldTag);

        return createBidirectionalMap(heldTag, heldInternMap, holderTag, holderInternMap);
    }

    public UtilImpl_BidirectionalMap createBidirectionalMap(String holderTag, UtilImpl_InternMap holderInternMap,
                                                            String heldTag, UtilImpl_InternMap heldInternMap) {

        return new UtilImpl_BidirectionalMap(this,
                                             holderTag, heldTag,
                                             holderInternMap, heldInternMap);
    }

    //

    public static final boolean DO_RECORD_ADDED = true;
    public static final boolean DO_NOT_RECORD_ADDED = false;

    public static final boolean DO_RECORD_REMOVED = true;
    public static final boolean DO_NOT_RECORD_REMOVED = false;

    public static final boolean DO_RECORD_CHANGED = true;
    public static final boolean DO_NOT_RECORD_CHANGED = false;

    public static final boolean DO_RECORD_STILL = true;
    public static final boolean DO_NOT_RECORD_STILL = false;

    public static final int ZERO_ADDED = 0;
    public static final int ZERO_REMOVED = 0;
    public static final int ZERO_CHANGED = 0;
    public static final int ZERO_STILL = 0;

    public static final int ANY_NUMBER_OF_ADDED = -1;
    public static final int ANY_NUMBER_OF_REMOVED = -1;
    public static final int ANY_NUMBER_OF_CHANGED = -1;
    public static final int ANY_NUMBER_OF_STILL = -1;

    public static final boolean AS_ADDED = true;
    public static final boolean AS_REMOVED = false;

    @Trivial
    public UtilImpl_IdentitySetDelta subtractSet(
        Map<String, String> finalSet, Map<String, String> initialSet) {

        return subtractSet(
            DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_NOT_RECORD_STILL,
            finalSet, null, initialSet, null);
    }

    @Trivial
    public UtilImpl_IdentitySetDelta subtractSet(
        Map<String, String> finalSet, Util_InternMap finalDomain,
        Map<String, String> initialSet, Util_InternMap initialDomain) {

        return subtractSet(
            DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_NOT_RECORD_STILL,
            finalSet, finalDomain,
            initialSet, initialDomain);
    }


    public UtilImpl_IdentitySetDelta subtractSet(
        boolean recordAdded, boolean recordRemoved, boolean recordStill,
        Map<String, String> finalSet, Util_InternMap finalDomain,
        Map<String, String> initialSet, Util_InternMap initialDomain) {

        UtilImpl_IdentitySetDelta setDelta =
            new UtilImpl_IdentitySetDelta(recordAdded, recordRemoved, recordStill);

        setDelta.subtract(finalSet, finalDomain, initialSet, initialDomain);

        return setDelta;
    }

    @Trivial
    public UtilImpl_IdentityMapDelta subtractMap(
        Map<String, String> finalMap, Map<String, String> initialMap) {

        return subtractMap(
            DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_RECORD_CHANGED, DO_NOT_RECORD_STILL,
            finalMap, null, initialMap, null);
    }

    @Trivial    
    public UtilImpl_IdentityMapDelta subtractMap(
        Map<String, String> finalMap, Util_InternMap finalDomain,
        Map<String, String> initialMap, Util_InternMap initialDomain) {

        return subtractMap(
            DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_RECORD_CHANGED, DO_NOT_RECORD_STILL,
            finalMap, finalDomain, initialMap, initialDomain);
    }

    public UtilImpl_IdentityMapDelta subtractMap(
        boolean recordAdded, boolean recordRemoved, boolean recordChanged, boolean recordStill,
        Map<String, String> finalMap, Util_InternMap finalDomain,
        Map<String, String> initialMap, Util_InternMap initialDomain) {

        UtilImpl_IdentityMapDelta mapDelta = new UtilImpl_IdentityMapDelta(this,
            recordAdded, recordRemoved, recordChanged, recordStill);

        mapDelta.subtract(finalMap, finalDomain, initialMap, initialDomain);

        return mapDelta;
    }

    public UtilImpl_BidirectionalMapDelta subtractBiMap(
        boolean recordAdded, boolean recordRemoved, boolean recordStill,
        UtilImpl_BidirectionalMap finalMap, UtilImpl_BidirectionalMap initialMap) {

        UtilImpl_BidirectionalMapDelta mapDelta = new UtilImpl_BidirectionalMapDelta(this,
            recordAdded, recordRemoved, recordStill,
            finalMap.getHolderInternMap(), finalMap.getHeldInternMap());

        mapDelta.subtract(finalMap, initialMap);

        return mapDelta;
    }

    @Trivial
    public UtilImpl_BidirectionalMapDelta subtractBiMap(
        UtilImpl_BidirectionalMap finalMap, UtilImpl_BidirectionalMap initialMap) {

        return subtractBiMap(
            DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_NOT_RECORD_STILL,
            finalMap, initialMap);
    }

    //

    public UtilImpl_IdentityMapDelta createSimpleMapDelta() {
        return new UtilImpl_IdentityMapDelta(this);
    }

    public UtilImpl_IdentityMapDelta createSimpleMapDelta(int expectedAdded, int expectedRemoved) {
        return new UtilImpl_IdentityMapDelta(this, expectedAdded, expectedRemoved);
    }

    public Map<String, UtilImpl_IdentityMapDelta> asDeltaMap(Map<String, Map<String, String>> data, boolean asAdded) {
        Map<String, UtilImpl_IdentityMapDelta> deltaMap =
            new IdentityHashMap<String, UtilImpl_IdentityMapDelta>(data.size());

        for ( Map.Entry<String, Map<String, String>> dataEntry : data.entrySet() ) {
            deltaMap.put(dataEntry.getKey(), asDelta(dataEntry.getValue(), asAdded));
        }

        return deltaMap;
    }

    public UtilImpl_IdentityMapDelta asDelta(Map<String, String> map, boolean asAdded) {
        int mapSize = map.size();

        UtilImpl_IdentityMapDelta mapDelta = createSimpleMapDelta( (asAdded ? mapSize : 0), (asAdded ? 0 : mapSize) );

        mapDelta.add(map, asAdded);

        return mapDelta;
    }

    //

    // Unused; needs initial and final domains.
    public void compareData(Map<String, Map<String, String>> finalData,
                            Map<String, Map<String, String>> initialData,
                            Map<String, UtilImpl_IdentityMapDelta> resultData) {

        for ( Map.Entry<String, Map<String, String>> finalDataEntry : finalData.entrySet() ) {
            String finalName = finalDataEntry.getKey();
            Map<String, String> finalMap = finalDataEntry.getValue();

            Map<String, String> initialMap = initialData.get(finalName);

            if ( initialMap == null ) {
                resultData.put(finalName, asDelta(finalMap, UtilImpl_Factory.AS_ADDED));

            } else {
                UtilImpl_IdentityMapDelta entryDelta = null;

                for ( Map.Entry<String, String> finalLeafEntry : finalMap.entrySet() ) {
                    String finalLeafName = finalLeafEntry.getKey();
                    String finalLeafValue = finalLeafEntry.getValue();

                    String initialLeafValue = initialMap.get(finalLeafName);

                    if ( initialLeafValue == null ) {
                        if ( entryDelta == null ) {
                            entryDelta = createSimpleMapDelta();
                        }
                        entryDelta.addAdded(finalLeafName, finalLeafValue);

                    } else if ( !finalLeafValue.equals(initialLeafValue) ) {
                        if ( entryDelta == null ) {
                            entryDelta = createSimpleMapDelta();
                        }
                        entryDelta.addChanged(finalLeafName, finalLeafValue, initialLeafValue);
                    } else {
                        // Ignore still values
                    }
                }

                if ( entryDelta != null ) {
                    resultData.put(finalName, entryDelta);
                }
            }
        }

        for ( Map.Entry<String, Map<String, String>> initialDataEntry : initialData.entrySet() ) {
            String initialName = initialDataEntry.getKey();
            Map<String, String> initialMap = initialDataEntry.getValue();

            Map<String, String> finalMap = finalData.get(initialName);

            if ( initialMap == null ) {
                resultData.put(initialName, asDelta(finalMap, UtilImpl_Factory.AS_REMOVED));

            } else {
                UtilImpl_IdentityMapDelta entryDelta = null;

                for ( Map.Entry<String, String> initialLeafEntry : initialMap.entrySet() ) {
                    String initialLeafName = initialLeafEntry.getKey();
                    String initialLeafValue = initialLeafEntry.getValue();

                    String finalLeafValue = initialMap.get(initialLeafName);

                    if ( finalLeafValue == null ) {
                        if ( entryDelta == null ) {
                            entryDelta = createSimpleMapDelta();
                        }
                        entryDelta.addRemoved(initialLeafName, initialLeafValue);

                    } else {
                        // Changed values were already handled.

                        // Ignore still values.
                    }
                }

                if ( entryDelta != null ) {
                    resultData.put(initialName, entryDelta);
                }
            }
        }
    }

    // unused; needs initial and final domains
    public void subtractDataMap(
        Map<String, Map<String, Map<String, String>>> finalDataMap,
        Map<String, Map<String, Map<String, String>>> initialDataMap,
        Map<String, Map<String, UtilImpl_IdentityMapDelta>> resultDataMap) {

        Map<String, UtilImpl_IdentityMapDelta> resultData = null;

        for ( Map.Entry<String, Map<String, Map<String, String>>> finalMapEntry : finalDataMap.entrySet() ) {
            String finalMapName = finalMapEntry.getKey();
            Map<String, Map<String, String>> finalData = finalMapEntry.getValue();

            Map<String, Map<String, String>> initialData = initialDataMap.get(finalMapName);

            if ( initialData == null ) {
                resultDataMap.put(finalMapName, asDeltaMap(finalData, UtilImpl_Factory.AS_ADDED));

            } else {
                if ( resultData == null ) {
                    resultData = new IdentityHashMap<String, UtilImpl_IdentityMapDelta>();
                }

                compareData(finalData, initialData, resultData);

                if ( !resultData.isEmpty() ) {
                    resultDataMap.put(finalMapName, resultData);
                    resultData = null;
                }
            }
        }

        for ( Map.Entry<String, Map<String, Map<String, String>>> finalMapEntry : finalDataMap.entrySet() ) {
            String finalMapName = finalMapEntry.getKey();
            Map<String, Map<String, String>> finalData = finalMapEntry.getValue();

            Map<String, Map<String, String>> initialData = initialDataMap.get(finalMapName);

            if ( initialData == null ) {
                resultDataMap.put(finalMapName, asDeltaMap(finalData, UtilImpl_Factory.AS_REMOVED));

            } else {
                if ( resultData == null ) {
                    resultData = new IdentityHashMap<String, UtilImpl_IdentityMapDelta>();
                }

                compareData(finalData, initialData, resultData);

                if ( !resultData.isEmpty() ) {
                    resultDataMap.put(finalMapName, resultData);
                    resultData = null;
                }
            }
        }
    }

    public Map<String, String> createIdentityMap(boolean isEnabled, int expectedSize) {
        if ( !isEnabled ) {
            return null;

        } else if ( expectedSize == 0 ) {
            return Collections.emptyMap();
        } else if ( expectedSize == -1 ) {
            return new IdentityHashMap<String, String>();
        } else {
            return new IdentityHashMap<String, String>(expectedSize);
        }
    }

    public Map<String, String[]> createValuesIdentityMap(boolean isEnabled, int expectedSize) {
        if ( !isEnabled ) {
            return null;

        } else if ( expectedSize == 0 ) {
            return Collections.emptyMap();
        } else if ( expectedSize == -1 ) {
            return new IdentityHashMap<String, String[]>();
        } else {
            return new IdentityHashMap<String, String[]>(expectedSize);
        }
    }

    //

    @Override
    @Trivial
    public String normalize(String path) {
        return UtilImpl_PathUtils.normalize(path);
    }

    @Override
    @Trivial
    public String denormalize(String n_path) {
        return UtilImpl_PathUtils.denormalize(n_path);
    }

    @Override
    @Trivial
    public String append(String headPath, String tailPath) {
        return UtilImpl_PathUtils.append(headPath, tailPath);
    }

    @Override
    @Trivial
    public String n_append(String n_headPath, String n_tailPath) {
        return UtilImpl_PathUtils.n_append(n_headPath, n_tailPath);
    }

    @Override
    @Trivial
    public String subtractPath(String basePath, String fullPath) {
        return UtilImpl_PathUtils.subtractPath(basePath, fullPath);
    }

    @Override
    @Trivial
    public String n_subtractPath(String n_basePath, String n_fullPath) {
        return UtilImpl_PathUtils.n_subtractPath(n_basePath, n_fullPath);
    }

    @Override
    @Trivial
    public UtilImpl_RelativePath addRelativePath(String basePath, String relativePath) {
        return UtilImpl_PathUtils.addRelativePath(basePath, relativePath);
    }

    @Override
    @Trivial
    public UtilImpl_RelativePath n_addRelativePath(String n_basePath, String n_relativePath) {
        return UtilImpl_PathUtils.n_addRelativePath(n_basePath, n_relativePath);
    }

    @Override
    @Trivial
    public UtilImpl_RelativePath subtractRelativePath(String basePath, String fullPath) {
        return UtilImpl_PathUtils.subtractRelativePath(basePath, fullPath);
    }

    @Override
    @Trivial
    public Util_RelativePath n_subtractRelativePath(String n_basePath, String n_fullPath) {
        return UtilImpl_PathUtils.n_subtractRelativePath(n_basePath, n_fullPath);
    }

    @Override
    public UtilImpl_RelativePath createRelativePath(String basePath, String relativePath, String fullPath) {
        String n_basePath = normalize(basePath);
        String n_relativePath = normalize(relativePath);
        String n_fullPath = normalize(fullPath);

        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    @Override
    @Trivial
    public UtilImpl_RelativePath n_createRelativePath(String n_basePath, String n_relativePath, String n_fullPath) {
        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    @Override
    @Trivial
    public List<UtilImpl_RelativePath> selectJars(String basePath) {
        return UtilImpl_PathUtils.selectJars(basePath);
    }
}
