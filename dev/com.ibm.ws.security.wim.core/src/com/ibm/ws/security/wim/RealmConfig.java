/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.config.xml.internal.nester.Nester;

/**
 * Realm Configuration wrapper object
 */
public class RealmConfig extends HashMap<String, Object> {

    private static final TraceComponent tc = Tr.register(RealmConfig.class);

    private static final long serialVersionUID = -1L;
    public static final String DEFAULT_REALM = "defaultRealm";
    public static final String ALLOW_IF_REPODOWN = "allowOpIfRepoDown";
    public static final String DELIMITER = "delimiter";
    public static final String NAME = "name";
    //public static final String SECURITY_USE_ENABLED = "enabled";
    public static final String PARTICIPATING_BASEENTRIES = "participatingBaseEntry";
    public static final String DEFAULT_PARENTS = "defaultParents";
    public static final String DEFAULT_PARENT = "defaultParent";
    public static final String ENTITY_TYPE_NAME = "entityTypeName";
    public static final String PARENT_UNIQUE_NAME = "parentUniqueName";
    public static final String UNIQUE_USER_ID_MAPPING = "uniqueUserIdMapping";
    public static final String USER_SECURITY_NAME_MAPPING = "userSecurityNameMapping";
    public static final String USER_DISPLAY_NAME_MAPPING = "userDisplayNameMapping";
    public static final String UNIQUE_GROUP_ID_MAPPING = "uniqueGroupIdMapping";
    public static final String GROUP_SECURITY_NAME_MAPPING = "groupSecurityNameMapping";
    public static final String GROUP_DISPLAY_NAME_MAPPING = "groupDisplayNameMapping";
    public static final String URATTR_UNIQUE_USER_ID = "uniqueUserId";
    public static final String URATTR_USER_SECURITY_NAME = "userSecurityName";
    public static final String URATTR_USER_DISPLAY_NAME = "userDisplayName";
    public static final String URATTR_UNIQUE_GROUP_ID = "uniqueGroupId";
    public static final String URATTR_GROUP_SECURITY_NAME = "groupSecurityName";
    public static final String URATTR_GROUP_DISPLAY_NAME = "groupDisplayName";
    public static final String INPUT_PROPERTY = "inputProperty";
    public static final String OUTPUT_PROPERTY = "outputProperty";
    boolean defaultRealm = false;

    boolean allowOpIfRepoDown = false;
    String delimiter = "/";
    String name;
    //String securityUse;
    String[] participatingBaseEntries;
    private Map<String, String> defaultParentMapping = new HashMap<String, String>();

    public boolean isDefaultRealm() {
        return (Boolean) get(DEFAULT_REALM);
    }

    public boolean isAllowOpIfRepoDown() {
        return (Boolean) get(ALLOW_IF_REPODOWN);
    }

    public String getDelimiter() {
        return (String) get(DELIMITER);
    }

    public String getName() {
        return (String) get(NAME);
    }

    /*
     * public boolean getSecurityUse() {
     * return (Boolean) get(SECURITY_USE_ENABLED);
     * }
     */

    public String[] getParticipatingBaseEntries() {
        return (String[]) get(PARTICIPATING_BASEENTRIES);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getDefaultParentMapping() {
        return (Map<String, String>) get(DEFAULT_PARENTS);
    }

    public void setDefaultParentMapping(Map<String, String> defaultParentMapping) {
        this.defaultParentMapping = defaultParentMapping;
    }

    public RealmConfig(Map<String, Object> realmProps, boolean isDefaultRealm) {

        Map<String, List<Map<String, Object>>> propMap = Nester.nest(realmProps, UNIQUE_USER_ID_MAPPING,
                                                                     USER_SECURITY_NAME_MAPPING,
                                                                     USER_DISPLAY_NAME_MAPPING,
                                                                     UNIQUE_GROUP_ID_MAPPING,
                                                                     GROUP_SECURITY_NAME_MAPPING,
                                                                     GROUP_DISPLAY_NAME_MAPPING,
                                                                     PARTICIPATING_BASEENTRIES,
                                                                     DEFAULT_PARENTS);
        populateURMapping(UNIQUE_USER_ID_MAPPING, propMap);
        populateURMapping(USER_SECURITY_NAME_MAPPING, propMap);
        populateURMapping(USER_DISPLAY_NAME_MAPPING, propMap);
        populateURMapping(UNIQUE_GROUP_ID_MAPPING, propMap);
        populateURMapping(GROUP_SECURITY_NAME_MAPPING, propMap);
        populateURMapping(GROUP_DISPLAY_NAME_MAPPING, propMap);
        populateURMapping(DEFAULT_PARENTS, propMap);

        List<Map<String, Object>> baseEntries = propMap.get(PARTICIPATING_BASEENTRIES);
        int i = 0;
        for (Map<String, Object> baseEntry : baseEntries) {
            String name = (String) baseEntry.get(NAME);
            if (name == null) {
                //TODO this message doesn't make sense any more
                Tr.error(tc, WIMMessageKey.INVALID_PARTICIPATING_BASE_ENTRY_DEFINITION, baseEntry);
            } else {
                if (participatingBaseEntries == null) {
                    participatingBaseEntries = new String[baseEntries.size()];
                }
                participatingBaseEntries[i++] = name;
            }
        }
        put(PARTICIPATING_BASEENTRIES, participatingBaseEntries);
        for (Map.Entry<String, Object> entry : realmProps.entrySet()) {
            String key = entry.getKey();
            if (genericKey(key)) {
                put(key, entry.getValue());
            }
        }
        put(DEFAULT_REALM, isDefaultRealm);

        if (participatingBaseEntries == null || participatingBaseEntries.length == 0) {
            Tr.error(tc, WIMMessageKey.MISSING_BASE_ENTRY_IN_REALM, getName());
        }

        Map<String, String> defaultParentMap = getDefaultParentMap(propMap);
        if (defaultParentMap != null)
            put(DEFAULT_PARENTS, defaultParentMap);
    }

    private Map<String, String> getDefaultParentMap(Map<String, List<Map<String, Object>>> propMap) {
        HashMap<String, String> defaultParentsMap = null;;
        List<Map<String, Object>> defaultParentsList = propMap.get(DEFAULT_PARENTS);

        if (!defaultParentsList.isEmpty()) {
            defaultParentsMap = new HashMap<String, String>();
            for (Map<String, Object> defaultParents : defaultParentsList) {
                String entityName = ((String) defaultParents.get(NAME));
                String parentUniqueName = ((String) defaultParents.get(PARENT_UNIQUE_NAME));
                defaultParentsMap.put(entityName, parentUniqueName);
            }
        }

        return defaultParentsMap;
    }

    /**
     * @param key
     * @return
     */
    private boolean genericKey(String key) {
        int i = key.indexOf('.');
        if (i == -1) {
            return true;
        }
        String k = key.substring(0, i - 1);
        if (k.equals(UNIQUE_USER_ID_MAPPING) ||
            k.equals(USER_SECURITY_NAME_MAPPING) ||
            k.equals(USER_DISPLAY_NAME_MAPPING) ||
            k.equals(UNIQUE_GROUP_ID_MAPPING) ||
            k.equals(GROUP_SECURITY_NAME_MAPPING) ||
            k.equals(GROUP_DISPLAY_NAME_MAPPING) ||
            k.equals(PARTICIPATING_BASEENTRIES)) {
            return false;
        }
        return true;
    }

    @Trivial
    private void populateURMapping(String key, Map<String, List<Map<String, Object>>> propMap) {
        List<Map<String, Object>> urMappingList = propMap.get(key);

        if (!urMappingList.isEmpty()) {
            Map<String, Object> urMapping = urMappingList.get(0);
            String[] propMapping = new String[2];
            propMapping[0] = (String) urMapping.get(INPUT_PROPERTY);
            propMapping[1] = (String) urMapping.get(OUTPUT_PROPERTY);
            put(key, propMapping);
        }

    }

    public String getURMapInputPropertyInRealm(String inputProperty) {
        Object propMapping = get(inputProperty);
        String returnProp = null;
        if (propMapping != null)
            returnProp = ((String[]) get(inputProperty))[0];
        return returnProp;
    }

    public String getURMapOutputPropertyInRealm(String outputProperty) {
        Object propMapping = get(outputProperty);
        String returnProp = null;
        if (propMapping != null)
            returnProp = ((String[]) get(outputProperty))[1];
        return returnProp;
    }

}
