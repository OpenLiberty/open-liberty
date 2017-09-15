/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.intf.DCacheConfig;
import com.ibm.ws.cache.intf.JSPCache;

/**
 * Abstract superclass of all generic cache processors. A processor provides
 * access to the generic caching specification for different classes of cacheable
 * objects, e.g. Servlets/JSPs or Commands. One processor object is instatiated for
 * each cache policy; the cacheable object is passed into the constructor, which
 * looks up the cache policy for that object. WebSphere will then call the execute()
 * method at the appropriate time to compute the appropriate ids for the object, passing
 * in objects needed to produce that key as necessary for each object class. Finally,
 * WebSphere will call the setEntryInfo method, passing in an entryinfo for that instance
 * of the cacheable object, which will be populated with the information calculated in
 * the execute() method.
 */
abstract public class CacheProcessor {

    private static TraceComponent tc = Tr.register(CacheProcessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    public static final String PROPERTY_SHARING_POLICY = "sharing-policy";
    public static final String PROPERTY_SHARINGPOLICY = "sharingpolicy";
    public static final String PROPERTY_PERSIST_TO_DISK = "persist-to-disk";
    public static final String PROPERTY_DELAY_INVALIDATIONS = "delay-invalidations";
    public static final String PROPERTY_TIMEOUT = "timeout";
    public static final String PROPERTY_INACTIVITY = "inactivity";
    public static final String PROPERTY_PRIORITY = "priority";
    public static final String PROPERTY_DO_NOT_CACHE = "do-not-cache";

    protected static final int SLOT_SHARINGPOLICY = 0;
    protected static final int SLOT_TIMEOUT = 1;
    protected static final int SLOT_PRIORITY = 2;
    protected static final int SLOT_PERSIST_TO_DISK = 3;
    protected static final int SLOT_INACTIVITY = 4;
    protected static final int SLOT_DO_NOT_CACHE = 5;
    protected static final int BASE_SLOTS = 6;

    protected boolean cacheable = false;
    protected StringBuffer id = new StringBuffer();
    protected ArrayList groupIds = null;
    protected ArrayList invalidationIds = null;
    protected ConfigEntry configEntry;
    protected boolean delayInvalidations = false;
    protected int timeout = -1;
    protected int inactivity = -1;
    protected int priority = -1;
    protected int sharingPolicy = -1;
    protected boolean persistToDisk = true;
    protected CacheId cacheIdForMetaDataGenerator = null;
    protected boolean doNotCache = false;

    protected static final Object emptyArgs[] = new Object[0];

    //must provide a null constructor so that subclasses can be pooled
    public CacheProcessor() {}

    public CacheProcessor(ConfigEntry configEntry) {
        this.configEntry = configEntry;

    }

    public void reset(ConfigEntry configEntry) {
        cacheable = false;
        id = new StringBuffer();
        groupIds = null;
        invalidationIds = null;
        this.configEntry = configEntry;
        delayInvalidations = false;
        timeout = -1;
        inactivity = -1;
        priority = -1;
        persistToDisk = true;
        if (configEntry != null) {
            sharingPolicy = configEntry.sharingPolicy;
        } else {
            sharingPolicy = -1;
        }
        doNotCache = false;
        if (cacheIdForMetaDataGenerator != null)
        {
            cacheIdForMetaDataGenerator.metaDataGeneratorImpl = null;
            cacheIdForMetaDataGenerator = null;
        }
    }

    //used when the configuration is loaded to validate and
    //preprocess any data
    public boolean preProcess(ConfigEntry configEntry) {
        boolean valid = true;
        // there is no override, use the default processorData
        if (configEntry.processorData == null)
            configEntry.processorData = new Object[BASE_SLOTS];
        //persistToDisk                                            
        Property p = (Property) configEntry.properties.get(PROPERTY_PERSIST_TO_DISK);
        String val = p != null ? (String) p.value : null;
        if (val != null) {
            val = val.trim();
            configEntry.processorData[SLOT_PERSIST_TO_DISK] = new Boolean(val);
        }

        //do-not-cache                                            
        p = (Property) configEntry.properties.get(PROPERTY_DO_NOT_CACHE);
        val = p != null ? (String) p.value : null;
        if (val != null) {
            configEntry.processorData[SLOT_DO_NOT_CACHE] = new Boolean(val);
        }

        for (int i = 0; i < configEntry.cacheIds.length; i++)
            valid &= preProcess(configEntry.cacheIds[i]);
        return valid;
    }

    public boolean preProcess(CacheId cacheId) {
        // there is no override, use the default processorData
        if (cacheId.processorData == null)
            cacheId.processorData = new Object[BASE_SLOTS];
        if (cacheId.properties != null) {
            //sharing-policy
            Property p = (Property) cacheId.properties.get(PROPERTY_SHARING_POLICY);
            String val = p != null ? (String) p.value : null;
            if (val == null)
                val = (String) cacheId.properties.get(PROPERTY_SHARINGPOLICY);
            if (val != null) {
                if (val.equalsIgnoreCase("none")) {
                    cacheId.processorData[SLOT_SHARINGPOLICY] = new Integer(EntryInfo.NOT_SHARED);
                } else if (val.equalsIgnoreCase("pull")) {
                    cacheId.processorData[SLOT_SHARINGPOLICY] = new Integer(EntryInfo.SHARED_PULL);
                } else if (val.equalsIgnoreCase("push")) {
                    cacheId.processorData[SLOT_SHARINGPOLICY] = new Integer(EntryInfo.SHARED_PUSH);
                } else if (val.equalsIgnoreCase("both")) {
                    cacheId.processorData[SLOT_SHARINGPOLICY] = new Integer(EntryInfo.SHARED_PUSH_PULL);
                } else {
                    //report error
                }
            }
            //timeout
            p = (Property) cacheId.properties.get(PROPERTY_TIMEOUT);
            if (p != null)
                val = p.value;
            if (val != null) {
                cacheId.processorData[SLOT_TIMEOUT] = new Integer(val);
            }

            //inactivity CPF-Inactivity
            p = (Property) cacheId.properties.get(PROPERTY_INACTIVITY);
            if (p != null) {
                val = p.value;
                if (val != null) {
                    cacheId.processorData[SLOT_INACTIVITY] = new Integer(val);
                }
            }

            //priority
            p = (Property) cacheId.properties.get(PROPERTY_PRIORITY);
            if (p != null)
                val = p.value;
            if (val != null) {
                cacheId.processorData[SLOT_PRIORITY] = new Integer(val);
            }

            //do-not-cache
            p = (Property) cacheId.properties.get(PROPERTY_DO_NOT_CACHE);
            if (p != null) {
                val = p.value;
                if (val != null) {
                    cacheId.processorData[SLOT_DO_NOT_CACHE] = new Boolean(val);
                }
            }

        }
        return true;
    }

    public void processCacheIdProperties(CacheId cacheid) {
        if (cacheid.processorData[SLOT_SHARINGPOLICY] != null) {
            sharingPolicy = ((Integer) cacheid.processorData[SLOT_SHARINGPOLICY]).intValue();
        }
        if (cacheid.processorData[SLOT_TIMEOUT] != null) {
            timeout = ((Integer) cacheid.processorData[SLOT_TIMEOUT]).intValue();
        }
        if (cacheid.processorData[SLOT_INACTIVITY] != null) {
            inactivity = ((Integer) cacheid.processorData[SLOT_INACTIVITY]).intValue();
        }
        if (cacheid.processorData[SLOT_PRIORITY] != null) {
            priority = ((Integer) cacheid.processorData[SLOT_PRIORITY]).intValue();
        }
        if (cacheid.processorData[SLOT_DO_NOT_CACHE] != null) {
            doNotCache = ((Boolean) cacheid.processorData[SLOT_DO_NOT_CACHE]).booleanValue();
        }
    }

    public void processConfigEntryProperties() {
        // persistToDisk
        if (configEntry.processorData[SLOT_PERSIST_TO_DISK] != null) {
            persistToDisk = ((Boolean) configEntry.processorData[SLOT_PERSIST_TO_DISK]).booleanValue();
        }
        //do-not-cache
        if (configEntry.processorData[SLOT_DO_NOT_CACHE] != null) {
            doNotCache = ((Boolean) configEntry.processorData[SLOT_DO_NOT_CACHE]).booleanValue();
        }
    }

    abstract protected Object getComponentValue(Component c);

    public String getBaseName() {
        return configEntry.name;
    }

    public boolean execute() {
        cacheable = false;
        //processingInvalidation = false;
        if (configEntry.cacheIds != null)
            for (int i = 0; !cacheable && i < configEntry.cacheIds.length; i++) {
                id.append(getBaseName());
                cacheable = processCacheId(configEntry.cacheIds[i]);
                if (!cacheable) {
                    id.setLength(0);
                }
            }
        if (configEntry.dependencyIds != null) {
            for (int i = 0; i < configEntry.dependencyIds.length; i++) {
                processDependencyId(configEntry.dependencyIds[i]);
            }
        }

        if (configEntry.className.equalsIgnoreCase("command")) {
            Property p = (Property) configEntry.properties.get(PROPERTY_DELAY_INVALIDATIONS);
            if (p != null && "true".equalsIgnoreCase(p.value)) {
                delayInvalidations = true;
            }
        } else {
            setInvalidationIds();
        }
        return cacheable;
    }

    public String getId() {
        if (cacheable)
            return id.toString();
        else
            return null;
    }

    public ArrayList getGroupIds() {
        return groupIds;
    }

    public ArrayList getInvalidationIds() {
        return invalidationIds;
    }

    public int getSharingPolicy() {
        return sharingPolicy;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getInactivity() {
        return inactivity;
    }

    public int getPriority() {
        return priority;
    }

    public void setEntryInfo(com.ibm.websphere.cache.EntryInfo entryInfo) {
        entryInfo.setId(getId());
        entryInfo.addTemplate(getBaseName());
        if (cacheable) {
            entryInfo.setSharingPolicy(getSharingPolicy());
            entryInfo.setPersistToDisk(persistToDisk);
            entryInfo.setTimeLimit(getTimeout());
            entryInfo.setInactivity(getInactivity());
            if (getPriority() > 0)
                entryInfo.setPriority(getPriority());
            if (groupIds != null) {
                for (int i = 0; i < groupIds.size(); i++)
                    entryInfo.addDataId((String) groupIds.get(i));
            }
            if (this.cacheIdForMetaDataGenerator != null) {
                processMetaDataGenerator(this.cacheIdForMetaDataGenerator);
            }
        }
    }

    public void setInvalidationIds() {
        if (configEntry.invalidations != null)
            for (int i = 0; i < configEntry.invalidations.length; i++) {
                processInvalidation(configEntry.invalidations[i]);
            }
    }

    protected boolean processCacheId(CacheId cacheid) {
        boolean success = true;
        if (cacheid.idGenerator != null) {
            String cid = processIdGenerator(cacheid);
            if (cid == null)
                success = false;
            else {
                id.append(':');
                id.append(cid);
            }
        } else {
            if (cacheid.components != null)
                // Move through all components within cache ID and process each one
                for (int i = 0; success && i < cacheid.components.length; i++) {
                    success = processComponent(cacheid.components[i], null, null, cacheid.components[i].required);
                }
        }
        if (success) {
            priority = cacheid.priority;
            timeout = cacheid.timeout;
            inactivity = cacheid.inactivity; // CPF-Inactivity
            this.cacheIdForMetaDataGenerator = null;
            if (cacheid.metaDataGenerator != null) {
                this.cacheIdForMetaDataGenerator = cacheid;
            }

            //CCC end
            // process any custom properties at the global level and
            // cache id level
            processConfigEntryProperties();
            processCacheIdProperties(cacheid);
        }
        return success;
    }

    abstract protected String processIdGenerator(CacheId cacheid);

    abstract protected void processMetaDataGenerator(CacheId cacheid);

    abstract protected String[] processInvalidationGenerator(Invalidation invalidation);

    /*
     * processComponent - processes a component and returns a string, if depIds
     * is not null, then output of processing a component goes into a new depIds
     * entry
     */

    protected boolean processComponent(Component comp, StringBuffer depId,
                                       ArrayList depIds, boolean isRequired) {
        boolean result = false;
        Object value = getComponentValue(comp); // retreive component value from
                                                // cache-specific processor
        String compValue = null;

        /*
         * getComponentValue returns different types depending on the type of
         * cache whose processor is invoked. Need the logic below to account for
         * which type of cache is being processed.
         */
        if (value != null) {
            // Process attribute/method/field types into a string and store in
            // compValue
            if (comp.iType == Component.ATTRIBUTE
                || comp.iType == Component.METHOD
                || comp.iType == Component.FIELD) {
                if (value instanceof Collection) {
                    value = ((Collection) value).toArray();
                }

                if (value instanceof Object[]) {
                    Object[] temp = (Object[]) value;
                    if (temp.length > 0) {
                        StringBuffer res = new StringBuffer();
                        for (int i = 0; i < temp.length; i++) {
                            if (temp[i] != null) {
                                res.append(temp[i]);
                                res.append(",");
                            }
                        }
                        compValue = res.substring(0, res.length() - 1); // to get rid of the last comma
                    }
                }
            }
            // If we do not have an attribute/method/field or this returned
            // null, use toString to obtain the compValue
            if (compValue == null) {
                compValue = value.toString();
            }

            // Component has a value.
            if (compValue != null) {
                result = true;
                // Component has values or component has value ranges
                if (!((comp.values == null || comp.values.size() == 0) && (comp.valueRanges == null || comp.valueRanges.size() == 0))) {

                    boolean valueResult = false;
                    boolean rangeResult = false;

                    // Component contains values
                    if (comp.values != null && comp.values.size() != 0) {
                        Value v = (Value) comp.values.get(compValue);
                        // See if value is in or not
                        if (v != null) {
                            valueResult = true; // non-empty value table and the result is present
                        }
                    }

                    try {
                        int compValueInt = Integer.valueOf(compValue).intValue();
                        if (comp.valueRanges != null
                            && comp.valueRanges.size() != 0) {
                            for (int i = 0; i < comp.valueRanges.size(); i++) {
                                Range r = (Range) comp.valueRanges.get(i);
                                if ((r.low <= compValueInt)
                                    && (compValueInt <= r.high)) {
                                    rangeResult = true;
                                }
                            }

                        }
                    } catch (NumberFormatException ex) { // component not an integer value, rangeResult is false
                        rangeResult = false;
                    }
                    result = valueResult || rangeResult;
                }
                // Component has not-values or component has not-value ranges
                if (!((comp.notValues == null || comp.notValues.size() == 0) && (comp.notValueRanges == null || comp.notValueRanges
                                .size() == 0))) {

                    boolean valueResult = false;
                    boolean rangeResult = false;

                    // Component contains not-values
                    if (comp.notValues != null && comp.notValues.size() != 0) {
                        NotValue nv = (NotValue) comp.notValues.get(compValue);
                        if (nv != null) {
                            valueResult = true; // non-empty value table and the result is present
                        }
                    }

                    try {
                        int compValueInt = Integer.valueOf(compValue)
                                        .intValue();
                        if (comp.notValueRanges != null
                            && comp.notValueRanges.size() != 0) {
                            for (int i = 0; i < comp.notValueRanges.size(); i++) {
                                Range r = (Range) comp.notValueRanges.get(i);
                                if ((r.low <= compValueInt)
                                    && (compValueInt <= r.high))
                                    rangeResult = true;
                            }
                        }
                    } catch (Exception ex) {
                        // component not an integer value, rangeResult is false
                        rangeResult = false;
                    }

                    // In the case of not values, if we find the result or a
                    // result in the specified range, we DO NOT want to cache
                    result = result && !(valueResult || rangeResult);
                }
            } // end if component compValue != null
        } // end value != null

        // Value IS null--check to see if it is required. If not, return success
        // = true. If it is required, success = false.
        else {
            result = !isRequired;
        }

        // Result will be true if we want to cache, false if we do not
        if (result && compValue != null) {
            if (depId == null) {
                id.append(':');
                if (comp.id == null || comp.id.equals("")) {
                    comp.id = comp.type;
                }
                id.append(comp.id);
                if (!comp.ignoreValue) {
                    id.append('=');
                    id.append(compValue);
                }
            } else {
                if (!comp.ignoreValue) {
                    if (depId.length() != 0)
                        depId.append(':');
                    depId.append(compValue);
                    String multiIds = null;
                    if (depIds != null) {
                        for (int i = 0; i < depIds.size(); i++) {
                            multiIds = ((String) depIds.get(i));
                            multiIds = multiIds + ":" + compValue;
                            depIds.set(i, multiIds);
                        }
                    }
                }
            }
        }

        if (result == false) {
            JSPCache cache = ServerCache.getJspCache(configEntry.instanceName);
            DCacheConfig cacheConfig = cache.getCache().getCacheConfig();
            if (cacheConfig.isUse602RequiredAttrCompatibility() && !isRequired) {
                result = !isRequired;
            }
        }

        if (tc.isDebugEnabled() && false == result) {
            Tr.debug(tc, "reason comp.id=" + comp.id + " result=" + result + " compValue=" + compValue);
        }

        return result;
    }

    protected void processDependencyId(DependencyId dependencyId) {
        if (dependencyId != null) {
            StringBuffer depId = new StringBuffer();
            depId.append(dependencyId.baseName);
            ArrayList multipleIds = new ArrayList();
            boolean success = true;
            for (int i = 0; success && i < dependencyId.components.length; i++) {
                String currentDepId = depId.toString();
                success = processComponent(dependencyId.components[i], depId, multipleIds, dependencyId.components[i].required);

                if (dependencyId.components[i].multipleIds) {
                    if (dependencyId.components[i].iType == Component.ATTRIBUTE || dependencyId.components[i].iType == Component.METHOD
                        || dependencyId.components[i].iType == Component.FIELD) {
                        Object result = getComponentValue(dependencyId.components[i]);
                        if (result instanceof Collection) {
                            result = ((Collection) result).toArray();
                        }
                        if (result instanceof Object[]) {
                            Object[] temp = (Object[]) result;
                            if (temp.length > 0) {
                                String multipleId = null;
                                for (int j = 0; j < temp.length; j++) {
                                    if (temp[j] != null) {
                                        multipleId = currentDepId + ":" + temp[j].toString();
                                        multipleIds.add(multipleId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (success) {
                if (groupIds == null)
                    groupIds = new ArrayList();
                groupIds.add(depId.toString());
                Iterator it = multipleIds.iterator();
                while (it.hasNext()) {
                    groupIds.add(it.next());
                }
            }
        }
    }

    protected void processInvalidation(Invalidation invalidation) {
        if (invalidation != null) {
            if (invalidation.invalidationGenerator != null) {
                String[] ids = processInvalidationGenerator(invalidation);
                if (ids != null) {
                    if (invalidationIds == null)
                        invalidationIds = new ArrayList();
                    for (int i = 0; i < ids.length; i++) {
                        String id = "";
                        if (invalidation.baseName != null && !invalidation.baseName.equals("")) {
                            id = invalidation.baseName + ":" + ids[i];
                        } else {
                            id = ids[i];
                        }
                        invalidationIds.add(id);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "InvalidationGenerator - id=" + id);
                    }
                }
            } else {
                StringBuffer invId = new StringBuffer();
                invId.append(invalidation.baseName);
                ArrayList multipleIds = new ArrayList();
                boolean success = true;
                for (int i = 0; success && i < invalidation.components.length; i++) {
                    String currentInvId = invId.toString();
                    success = processComponent(invalidation.components[i], invId, multipleIds, invalidation.components[i].required);

                    if (invalidation.components[i].multipleIds) {
                        if (invalidation.components[i].iType == Component.ATTRIBUTE || invalidation.components[i].iType == Component.METHOD
                            || invalidation.components[i].iType == Component.FIELD) {
                            Object result = getComponentValue(invalidation.components[i]);
                            if (result instanceof Collection) {
                                result = ((Collection) result).toArray();
                            }
                            if (result instanceof Object[]) {
                                Object[] temp = (Object[]) result;
                                if (temp.length > 0) {
                                    String multipleId = null;
                                    for (int j = 0; j < temp.length; j++) {
                                        if (temp[j] != null) {
                                            multipleId = currentInvId + ":" + temp[j].toString();
                                            multipleIds.add(multipleId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (success) {
                    if (invalidationIds == null)
                        invalidationIds = new ArrayList();
                    invalidationIds.add(invId.toString());
                    Iterator it = multipleIds.iterator();
                    while (it.hasNext()) {
                        invalidationIds.add(it.next());
                    }
                }
            }
        }
    }

    public boolean isDelayInvalidations() {
        return delayInvalidations;
    }

    public boolean getDoNotCache() {
        return doNotCache;
    }

    /**
     * @return the configEntry
     */
    public ConfigEntry getConfigEntry() {
        return configEntry;
    }

}
