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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Service;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_BidirectionalMapDelta;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_IdentityMapDelta;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_IdentitySetDelta;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;
import com.ibm.wsspi.annocache.util.Util_Exception;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_RelativePath;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class UtilImpl_Factory implements Util_Factory {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");
    
    public static final String CLASS_NAME = "UtilImpl_Factory";

    //

    protected String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Activate
    public UtilImpl_Factory() {
        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Created", this.hashText);
        }
    }

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

    public UtilImpl_BidirectionalMap createBidirectionalMap(String holderTag, Util_InternMap holderInternMap,
                                                            String heldTag, Util_InternMap heldInternMap) {

        return new UtilImpl_BidirectionalMap(this,
                                             holderTag, heldTag,
                                             holderInternMap, heldInternMap);
    }

    // Old style factory method.  Should no longer be in use.
	@Override
	public Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag, ValueType heldType,
			String heldTag, boolean isEnabled) {
		return createBidirectionalMap(holderType, holderTag, heldType, heldTag);
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
//        if ( !mapDelta.isNull() ) {
//        	System.out.println("Strange");
//        }
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

        mapDelta.recordTransfer(map, asAdded);

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
                        entryDelta.recordAdded(finalLeafName, finalLeafValue);

                    } else if ( !finalLeafValue.equals(initialLeafValue) ) {
                        if ( entryDelta == null ) {
                            entryDelta = createSimpleMapDelta();
                        }
                        entryDelta.recordChanged(finalLeafName, finalLeafValue, initialLeafValue);
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
                        entryDelta.recordRemoved(initialLeafName, initialLeafValue);

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
