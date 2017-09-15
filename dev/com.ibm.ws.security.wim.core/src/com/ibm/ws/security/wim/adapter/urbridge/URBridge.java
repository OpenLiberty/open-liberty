/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.urbridge;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.FactoryManager;
import com.ibm.ws.security.wim.Repository;
import com.ibm.ws.security.wim.adapter.urbridge.utils.URBridgeConstants;
import com.ibm.ws.security.wim.adapter.urbridge.utils.URBridgeEntity;
import com.ibm.ws.security.wim.adapter.urbridge.utils.URBridgeEntityFactory;
import com.ibm.ws.security.wim.adapter.urbridge.utils.URBridgeHelper;
import com.ibm.ws.security.wim.adapter.urbridge.utils.URBridgeXPathHelper;
import com.ibm.ws.security.wim.env.ICacheUtil;
import com.ibm.ws.security.wim.util.ControlsHelper;
import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.InitializationException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.GroupMemberControl;
import com.ibm.wsspi.security.wim.model.GroupMembershipControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

public class URBridge implements Repository {

    /**
     * The underlying User Registry
     */
    private UserRegistry userRegistry = null;

    /**
     * The key to extract the id of the configuration. The value returned by this is used as the repository id.
     */
    private static final String KEY_ID = "config.id";

    /**
     * The unique identifier for this repository.
     */
    private String reposId = null;

    /**
     * Map that stores the input-output property names.
     */
    private Map<String, String> attrMap = null;

    /**
     * Custom properties
     */
    private Map<String, String> customPropertyMap = null;

    /**
     * This map will store the values of both attrMap and customPropertyMap
     */
    private Map<String, String> propsMap = null;

    /**
     * The repository base entry name
     */
    private String baseEntryName = null;

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(URBridge.class);

    /**
     * Constant for "registryBaseEntry"
     */
    private static final String BASE_ENTRY = "registryBaseEntry";

    /**
     * Constant for Base Entry Name
     */
    static final String BASE_ENTRY_NAME = "name";

    /**
     * Signature string for vmm SPI
     */
    public final static String SPI_PREFIX = "WIM_SPI ";

    /**
     * Constant for context key for old password
     */
    private static final String OLD_PASSWORD = "OLD_PASSWORD";

    /**
     * Constant for context key for new user security name
     */
    private static final String NEW_SECURITY_NAME = "NEW_SECURITY_NAME";

    /**
     * Constant for context key to replace group members
     */
    private static final String REPLACE_MEMBERS = "REPLACE_MEMBERS";

    /**
     * Constant for context key to delete group members
     */
    private static final String DELETE_MEMBERS = "DELETE_MEMBERS";

    private static final String SAFRegistryImplClass = "com.ibm.ws.security.registry.saf.internal.SAFRegistry";

    /**
     * Name of the entity type for person account.
     */
    private String personAccountType = null;

    /**
     * Name of the entity type for Group.
     */
    private String groupAccountType = null;

    private HashMap<String, String> entityConfigMap = null;

    /**
     * Reference of the ConfigManager. This is needed for the supported
     * entity related configuration
     */
    private ConfigManager configManager = null;

    /**
     * List of supported entities (Default)
     */
    private static List<String> defaultSupportedEntities = null;

    /**
     * Default RDN properties for defaultSupportedEntities
     */
    private static Map<String, String[]> defaultRDNProperties = null;

    /**
     * The userSecurityName cache
     */
    private ICacheUtil iUserSecurityNameCache = null;

    /**
     * The groupSecurityName cache
     */
    private ICacheUtil iGroupSecurityNameCache = null;

    /**
     * The userSearch cache
     */
    private ICacheUtil iUserSearchCache = null;

    /**
     * The groupSearch cache
     */
    private ICacheUtil iGroupSearchCache = null;

    private ICacheUtil iUserUniqueIdCache = null;

    private ICacheUtil iUserDispNameCache = null;

    private ICacheUtil iGroupUniqueIdCache = null;

    private ICacheUtil iGroupDispNameCache = null;

    /*******************************************************************************************/

    private static void initializeSupportedEntities() {
        defaultSupportedEntities = new ArrayList<String>(2);
        defaultSupportedEntities.add(Service.DO_PERSON_ACCOUNT);
        defaultSupportedEntities.add(Service.DO_GROUP);
    }

    private static void initializeRDNProperties() {
        defaultRDNProperties = new HashMap<String, String[]>();
        String[] personRDN = { "uid" };
        String[] groupRDN = { "cn" };

        defaultRDNProperties.put(Service.DO_PERSON_ACCOUNT, personRDN);
        defaultRDNProperties.put(Service.DO_GROUP, groupRDN);
    }

    /**
     *
     */
    private void initializeCaches() {
        int cacheSize = 100;
        long cacheTimeOut = 1200;
        if (FactoryManager.getCacheUtil().isCacheAvailable()) {
            iUserSecurityNameCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iUserUniqueIdCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iUserDispNameCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iGroupSecurityNameCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iUserSearchCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iGroupSearchCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iGroupUniqueIdCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
            iGroupDispNameCache = FactoryManager.getCacheUtil().initialize(cacheSize, cacheSize, cacheTimeOut);
        }
    }

    /**
     * Constructor
     *
     * @param configProps
     * @throws InitializationException
     */
    public URBridge(Map<String, Object> configProps, UserRegistry ur, ConfigManager configMgr) throws InitializationException {
        reposId = (String) configProps.get(KEY_ID);
        userRegistry = ur;
        initializeCaches();
        configManager = configMgr;
        if (defaultSupportedEntities == null)
            initializeSupportedEntities();
        if (defaultRDNProperties == null)
            initializeRDNProperties();
        try {
            initialize(configProps);
        } catch (WIMException e) {
            throw new InitializationException(e);
        }
    }

    private void setMapping() {
        attrMap = new HashMap<String, String>(6);
        attrMap.put(URBridgeConstants.GROUP_SECURITY_NAME_PROP,
                    customPropertyMap.get(URBridgeConstants.GROUP_SECURITY_NAME_PROP) == null ? URBridgeConstants.GROUP_SECURITY_NAME_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.GROUP_SECURITY_NAME_PROP));
        attrMap.put(URBridgeConstants.GROUP_DISPLAY_NAME_PROP,
                    customPropertyMap.get(URBridgeConstants.GROUP_DISPLAY_NAME_PROP) == null ? URBridgeConstants.GROUP_DISPLAY_NAME_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.GROUP_DISPLAY_NAME_PROP));
        attrMap.put(URBridgeConstants.UNIQUE_GROUP_ID_PROP,
                    customPropertyMap.get(URBridgeConstants.UNIQUE_GROUP_ID_PROP) == null ? URBridgeConstants.UNIQUE_GROUP_ID_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.UNIQUE_GROUP_ID_PROP));
        attrMap.put(URBridgeConstants.USER_DISPLAY_NAME_PROP,
                    customPropertyMap.get(URBridgeConstants.USER_DISPLAY_NAME_PROP) == null ? URBridgeConstants.USER_DISPLAY_NAME_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.USER_DISPLAY_NAME_PROP));
        attrMap.put(URBridgeConstants.USER_SECURITY_NAME_PROP,
                    customPropertyMap.get(URBridgeConstants.USER_SECURITY_NAME_PROP) == null ? URBridgeConstants.USER_SECURITY_NAME_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.USER_SECURITY_NAME_PROP));
        attrMap.put(URBridgeConstants.UNIQUE_USER_ID_PROP,
                    customPropertyMap.get(URBridgeConstants.UNIQUE_USER_ID_PROP) == null ? URBridgeConstants.UNIQUE_USER_ID_DEFAULT_PROP : customPropertyMap.get(URBridgeConstants.UNIQUE_USER_ID_PROP));
    }

    /**
     * Initializes the user registry for use by the adapter.
     *
     * @param configProps
     *
     * @return
     */

    public void initialize(Map<String, Object> configProps) throws WIMException {
        try {
            reposId = (String) configProps.get(KEY_ID);

            setCustomProperties((List<Map<String, String>>) configProps.get(ConfigConstants.CONFIG_DO_CUSTOM_PROPERTIES));
            setMapping();
            setBaseEntry(configProps);
            setConfigEntityMapping(configProps);
            propsMap = new HashMap<String, String>();
            propsMap.putAll(attrMap);
            propsMap.putAll(customPropertyMap);
            URBridgeHelper.mapSupportedEntityTypeList(getSupportedEntityTypes());
            personAccountType = URBridgeHelper.getPersonAccountType();
            groupAccountType = URBridgeHelper.getGroupAccountType();
            /*
             * Properties supportedRegistries = new Properties();
             * String registryImplClass = customPropertyMap.get(URBridgeConstants.CUSTOM_REGISTRY_IMPL_CLASS) == null
             * ? null : (String) customPropertyMap.get(URBridgeConstants.CUSTOM_REGISTRY_IMPL_CLASS);
             *
             * if (registryImplClass == null) {
             * String osType = System.getProperty("os.name");
             * if (osType.startsWith("Windows")) {
             * osType = "Windows";
             * }
             * supportedRegistries.load(getClass().getResourceAsStream(registryPropsFile));
             * registryImplClass = supportedRegistries.getProperty(osType);
             * }
             * if (registryImplClass == null) {
             * throw new WIMApplicationException(WIMMessageKey.MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME,
             * Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME,
             * WIMMessageHelper.generateMsgParms(registryImplClass)));
             * }
             *
             * ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
             * if (contextCL == null) {
             * contextCL = this.getClass().getClassLoader();
             * }
             *
             * Class wrapperClass = Class.forName(registryImplClass, true, contextCL);
             * Object wrapperObj = wrapperClass.newInstance();
             *
             * if (wrapperObj instanceof UserRegistry) {
             * userRegistry = (UserRegistry) wrapperObj;
             * Properties initProperties = new Properties();
             * initProperties.putAll(customPropertyMap);
             * userRegistry.initialize(initProperties);
             * }
             * else {
             * throw new WIMApplicationException(WIMMessageKey.MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME,
             * Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME,
             * WIMMessageHelper.generateMsgParms(registryImplClass)));
             * }
             */
        } catch (Throwable th) {
            throw new InitializationException(WIMMessageKey.REPOSITORY_INITIALIZATION_FAILED, Tr.formatMessage(tc, WIMMessageKey.REPOSITORY_INITIALIZATION_FAILED,
                                                                                                               WIMMessageHelper.generateMsgParms(reposId, th.toString())));
        }
    }

    /**
     * Set the baseEntryname from the configuration. The configuration
     * should have only 1 baseEntry
     *
     * @param configProps Map containing the configuration information
     *            for the baseEntries.
     * @throws WIMException Exception is thrown if no baseEntry is set.
     */
    private void setBaseEntry(Map<String, Object> configProps) throws WIMException {
/*
 * Map<String, List<Map<String, Object>>> configMap = Nester.nest(configProps, BASE_ENTRY);
 *
 * for (Map<String, Object> entry : configMap.get(BASE_ENTRY)) {
 * baseEntryName = (String) entry.get(BASE_ENTRY_NAME);
 * }
 */
        baseEntryName = (String) configProps.get(BASE_ENTRY);

        if (baseEntryName == null) {
            throw new WIMApplicationException(WIMMessageKey.MISSING_BASE_ENTRY, Tr.formatMessage(tc, WIMMessageKey.MISSING_BASE_ENTRY,
                                                                                                 WIMMessageHelper.generateMsgParms(reposId)));
        }
    }

    /**
     * Set Custom UR Bridge properties.
     *
     * @param propList
     * @throws WIMException
     */
    private void setCustomProperties(List<Map<String, String>> propList) throws WIMException {
        final String METHODNAME = "setCustomProperties";
        customPropertyMap = new HashMap<String, String>();
        if (propList == null) {
            return;
        }
        Iterator<Map<String, String>> itr = propList.iterator();
        while (itr.hasNext()) {
            Map<String, String> propMap = itr.next();
            String propName = propMap.get(ConfigConstants.CONFIG_PROP_NAME);
            // String propValue = expandVar(propMap.get(ConfigConstants.CONFIG_PROP_VALUE));
            String propValue = propMap.get(ConfigConstants.CONFIG_PROP_VALUE);
            customPropertyMap.put(propName, propValue);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " custom properties " + customPropertyMap);
        }
    }

    /**
     * Set the mapping of RDN properties for each entity type. A map is created
     * with the key as the entity type and the value as the RDN property to be used.
     * This information is taken from the configuration.
     *
     * @param configProps map containing the configuration information.
     *
     * @throws WIMException throw when there is not a mapping for a user
     *             or not a mapping for a group.
     */
    private void setConfigEntityMapping(Map<String, Object> configProps) throws WIMException {
        List<String> entityTypes = getSupportedEntityTypes();
        String rdnProp;
        String type = null;
        entityConfigMap = new HashMap<String, String>();
        for (int i = 0; i < entityTypes.size(); i++) {
            type = entityTypes.get(i);
            rdnProp = (getRDNProperties(type) == null) ? null : getRDNProperties(type)[0];
            entityConfigMap.put(type, rdnProp);
        }
        if (entityConfigMap.get(Service.DO_LOGIN_ACCOUNT) == null && entityConfigMap.get(personAccountType) != null)
            entityConfigMap.put(Service.DO_LOGIN_ACCOUNT, entityConfigMap.get(personAccountType));

        if (tc.isDebugEnabled())
            Tr.debug(tc, "setConfigEntityMapping entityConfigMap:" + entityConfigMap);
    }

    private String[] getRDNProperties(String type) {
        String[] rdnProperties = configManager.getRDNProperties(type);

        if (rdnProperties == null || rdnProperties.length == 0) {
            rdnProperties = defaultRDNProperties.get(type);
            if (rdnProperties == null || rdnProperties.length == 0)
                return null;
            else
                return rdnProperties;
        } else
            return rdnProperties;
    }

    @SuppressWarnings("unchecked")
    private List<String> getSupportedEntityTypes() {
        List<String> supportedEntities = configManager.getSupportedEntityTypes();
        if (supportedEntities != null && supportedEntities.size() > 0)
            return supportedEntities;
        else
            return defaultSupportedEntities;
    }

    /**
     * Get the information about Users and Groups from the underlying User Registry
     *
     * @param root Input object containing set identifiers of objects to be fetched,
     *            and optionally control objects.
     *
     * @throws WIMException improper control objects are in the input datagraph,
     *             invalid properties are in a propertyControl object,or the underlying
     *             user registry throws an exception.
     *
     * @return A Root object containing the required Person(s) or Group(s)
     *
     */
    @Override
    public Root get(Root root) throws WIMException {
        Root returnRoot = new Root();
        String uniqueName = null;
        boolean restRequest = true;
        AuditManager auditManager = new AuditManager();
        if (auditManager.getRESTRequest() == null)
            restRequest = false;

        try {
            List<String> attrList = null;
            List<String> grpMbrAttrs = null;
            List<String> grpMbrshipAttrs = null;

            // Retrieve control objects
            Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
            PropertyControl propertyCtrl = (PropertyControl) ctrlMap.get(Service.DO_PROPERTY_CONTROL);
            GroupMemberControl grpMbrCtrl = (GroupMemberControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBER_CONTROL);
            GroupMembershipControl grpMbrshipCtrl = (GroupMembershipControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBERSHIP_CONTROL);

            // Set attributes to retrieve for member and membership operations.
            if (grpMbrCtrl != null) {
                grpMbrAttrs = getAttributes(grpMbrCtrl, personAccountType);
            }
            if (grpMbrshipCtrl != null) {
                grpMbrshipAttrs = getAttributes(grpMbrshipCtrl, groupAccountType);
            }

            // Get a list of all the entities.
            List<Entity> entities = root.getEntities();
            for (Entity entity : entities) {
                String memberType = validateEntity(entity);
                uniqueName = entity.getIdentifier().getUniqueName();

                Entity returnEntity = null;
                if (Service.DO_GROUP.equalsIgnoreCase(memberType))
                    returnEntity = new Group();
                else
                    returnEntity = new PersonAccount();

                returnRoot.getEntities().add(returnEntity);
                IdentifierType identifier = new IdentifierType();
                identifier.setRepositoryId(reposId);
                returnEntity.setIdentifier(identifier);

                // Wrap the entity in an object that provides functionality to the entity.
                URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
                URBridgeEntity osEntity = osEntityFactory.createObject(returnEntity, this,
                                                                       propsMap, baseEntryName, entityConfigMap);
                osEntity.setSecurityNameProp(uniqueName);

                // Get the attributes and populate the entity.
                attrList = getAttributes(propertyCtrl, memberType);
                if (attrList != null) {
                    osEntity.populateEntity(attrList);
                }

                // If it's a group and there is a GroupMemberControl object, find the members.
                if (Service.DO_GROUP.equalsIgnoreCase(memberType) && grpMbrCtrl != null && grpMbrAttrs != null) {
                    int limit = 0;
                    if (grpMbrCtrl.isSetCountLimit()) {
                        limit = grpMbrCtrl.getCountLimit();
                    }
                    osEntity.getUsersForGroup(grpMbrAttrs, limit);
                }

                // If it's a user and there is a GroupMembershipControl object, find the groups.
                else if ((Service.DO_LOGIN_ACCOUNT.equalsIgnoreCase(memberType) || Service.DO_PERSON_ACCOUNT.equalsIgnoreCase(memberType)) &&
                         grpMbrshipCtrl != null && grpMbrshipAttrs != null) {
                    int limit = 0;
                    if (grpMbrshipCtrl.isSetCountLimit()) {
                        limit = grpMbrshipCtrl.getCountLimit();
                    }
                    osEntity.getGroupsForUser(grpMbrshipAttrs, limit);
                }
            }
        } catch (EntityNotFoundException e) {
            if (restRequest)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "get", reposId, uniqueName, userRegistry.getRealm(), returnRoot,
                            Integer.valueOf("212"));

            throw e;
        } catch (Exception e) {
            throw new WIMException(e);
        }

        setReturnContext(root, returnRoot);
        auditManager.setRealm(userRegistry.getRealm());
        if (isURBridgeResult(returnRoot) && restRequest)
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "get", reposId, uniqueName, userRegistry.getRealm(), returnRoot,
                        Integer.valueOf("200"));

        return returnRoot;
    }

    public String buildRDN(String value) {
        String rdnProperty = "uid";
        String rdnStyleName = rdnProperty + "=" + value + ",o=" + reposId;
        return rdnStyleName;
    }

    /**
     *
     * Get the attributes requested for the entity.
     *
     * @param controlObject the control object containing the attributes.
     * @param type the type of object the attributes are requested for. Group or Person.
     */
    private List<String> getAttributes(PropertyControl control, String type) throws WIMException {
        List<String> attrList = new ArrayList<String>(10);
        if (control != null && control.getProperties() != null)
            attrList = control.getProperties();
        // If the attrList contains ONLY a * object, set the list to all
        if (attrList.size() > 0 && SchemaConstants.VALUE_ALL_PROPERTIES.equals(attrList.get(0))) {
            attrList = getAttributes(type);
        }
        attrList.addAll(getIdentifierAttributes(type));
        return attrList;
    }

    private List<String> getAttributes(String entityType) throws WIMException {
        final String METHODNAME = "getAttributes(entityType)";
        List<String> attrList = new ArrayList<String>();
        if (Service.DO_GROUP.equals(entityType)
            || Entity.getSubEntityTypes(Service.DO_GROUP).contains(entityType)) {
            attrList.add(entityConfigMap.get(entityType));
            attrList.add(attrMap.get(URBridgeConstants.GROUP_DISPLAY_NAME_PROP));
        } else if (Service.DO_LOGIN_ACCOUNT.equals(entityType)
                   || Entity.getSubEntityTypes(Service.DO_LOGIN_ACCOUNT).contains(entityType)) {
            attrList.add(entityConfigMap.get(entityType));
            attrList.add(SchemaConstants.PROP_PRINCIPAL_NAME);
            attrList.add(attrMap.get(URBridgeConstants.USER_DISPLAY_NAME_PROP));
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Entity type " + entityType + " is invalid and is ignored.");
            }
        }
        return attrList;
    }

    private List<String> getIdentifierAttributes(String entityType) throws WIMException {
        final String METHODNAME = "getIdentifierAttributes";
        List<String> attrList = new ArrayList<String>();

        if (Service.DO_GROUP.equals(entityType)
            || Entity.getSubEntityTypes(Service.DO_GROUP).contains(entityType)) {
            attrList.add(attrMap.get(URBridgeConstants.UNIQUE_GROUP_ID_PROP));
            attrList.add(attrMap.get(URBridgeConstants.GROUP_SECURITY_NAME_PROP));
        } else if (Service.DO_LOGIN_ACCOUNT.equals(entityType)
                   || Entity.getSubEntityTypes(Service.DO_LOGIN_ACCOUNT).contains(entityType)) {
            attrList.add(attrMap.get(URBridgeConstants.UNIQUE_USER_ID_PROP));
            attrList.add(attrMap.get(URBridgeConstants.USER_SECURITY_NAME_PROP));
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Entity type " + entityType + " is invalid and is ignored.");
            }
        }
        return attrList;
    }

    /**
     *
     * Validates if a entity is member of the registry.
     *
     * @param controlObject the control object containing the attrs.
     * @throws WIMException if the entity is not valid in the Registry or if the registry is bad or down.
     */
    //Get secName from uniqueName=externlName/uniqueId=externalId.
    //get Entity Type as User or Group from secName.
    //if type is null throw ENFE to be handled by get API.
    private String validateEntity(Entity entity) throws WIMException {
        String METHODNAME = "validateEntity";
        String type = null;
        String secName = null;
        String uniqueId = null;
        String uniqueName = null;

        if (entity.getIdentifier().isSet(SchemaConstants.PROP_UNIQUE_NAME)) {
            uniqueName = entity.getIdentifier().getUniqueName();
        } else if (entity.getIdentifier().isSet(SchemaConstants.PROP_EXTERNAL_NAME)) {
            uniqueName = entity.getIdentifier().getExternalName();
        } else if (entity.getIdentifier().isSet(SchemaConstants.PROP_UNIQUE_ID)) {
            uniqueId = entity.getIdentifier().getUniqueId();
        } else if (entity.getIdentifier().isSet(SchemaConstants.PROP_EXTERNAL_ID)) {
            uniqueId = entity.getIdentifier().getExternalId();
        }

        if (uniqueName != null) {
            secName = uniqueName; // stripRDN(uniqueName);
        }

        if (uniqueId != null && uniqueId.trim().length() > 0) {
            if (isValidUserOrGroup(uniqueId)) {
                uniqueName = uniqueId;
                secName = uniqueId;
            } else {
                secName = getSecNameFromUniqueID(uniqueId);
                uniqueName = secName;
            }
        }

        if (secName != null && secName.trim().length() > 0) {
            String rdnAttr = getRDN(entity.getIdentifier().getUniqueName());
            Set<String> EntityTypes = entityConfigMap.keySet();
            List<String> entities = new ArrayList<String>();
            Iterator<String> it = EntityTypes.iterator();

            while (it.hasNext()) {
                String entityType = it.next();
                if ((rdnAttr == null) || (rdnAttr.equalsIgnoreCase(entityConfigMap.get(entityType)))) {
                    entities.add(entityType);
                }
            }
            //handle if entities.size== or >1 then respect entity.getType from input DO
            //this is better than just letting the last matching type be returned.
            String inputType = entity.getTypeName();
            type = getEntityTypeFromUniqueName(secName, entities, inputType);
            entity.getIdentifier().setUniqueName(uniqueName);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " The entity type for " + secName + " is " + type);
        }
        if (type == null) {
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(secName)));
        }
        return type;
    }

    /**
     * Since UserRegistry throws CustomRegistryException in case of secName not found
     * modify code to handle CustomRegistryException similar to EntryNotFoundException.
     *
     * @param uniqueId
     * @return
     * @throws WIMException
     */
    private String getSecNameFromUniqueID(String uniqueId) throws WIMException {
        String METHODNAME = "getSecNameFromUniqueID";
        String secName = null;
        try {
            secName = getUserSecurityName(uniqueId);
        } catch (EntryNotFoundException e) {
            try {
                secName = getGroupSecurityName(uniqueId);
            } catch (com.ibm.ws.security.registry.EntryNotFoundException renf) {
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                   WIMMessageHelper.generateMsgParms(uniqueId)));
            } catch (RegistryException re) {
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                   WIMMessageHelper.generateMsgParms(uniqueId)));
            }
        } catch (RegistryException e) {
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(uniqueId)));
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " The Security Name for " + uniqueId + " is " + secName);
        }

        return secName;
    }

    private String getEntityTypeFromUniqueName(String secName, List<String> entityType, String inputType) throws WIMException {
        String METHODNAME = "getEntityTypeFromUniqueName";
        String type = null;
        ArrayList<String> typeList = new ArrayList<String>();

        try {
            boolean noSpecificEntityType = false;
            if (entityType.size() == 0 || entityType.size() > 1) {
                noSpecificEntityType = true;
            }
            /*
             * if((SAFRegistryImplClass.equalsIgnoreCase(reg.getClass().getName()))) {
             * if (entityType.contains(personAccountType) || noSpecificEntityType){
             * if (reg.isValidUser(secName))
             * typeList.add(personAccountType);
             * }
             * if(entityType.contains(groupAccountType) || noSpecificEntityType){
             * if (reg.isValidGroup(secName))
             * typeList.add(groupAccountType);
             * }
             * }
             * else
             */
            {
                if (entityType.contains(personAccountType) || noSpecificEntityType) {
                    int resultSize = searchUsers(secName, 1).getList().size();

                    if (resultSize > 0) {
                        typeList.add(personAccountType);
                    }
                }
                if (entityType.contains(groupAccountType) || noSpecificEntityType) {
                    int resultSize = searchGroups(secName, 1).getList().size();

                    if (resultSize > 0) {
                        typeList.add(groupAccountType);
                    }
                }
            }

            // If more than one matching type choose the one which matches with input DO type
            if (typeList.size() > 1) {
                for (int i = 0; i < typeList.size(); i++) {
                    if (typeList.get(i).equals(inputType)) {
                        type = typeList.get(i);
                        break;
                    }
                }
            }

            // If only one matching type or any of types returned not same as input DO type return first match.
            if (type == null && typeList.size() > 0)
                type = typeList.get(0);
        } catch (RegistryException e) {
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(secName)));
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, METHODNAME + " The entity type for " + secName + " is " + type);

        return type;
    }

    /**
     * @param secName
     * @param i
     */
    private String getKey(String secName, int i) {
        return secName + "|" + i;
    }

    /**
     * The method reads the uniqueName of the user and returns the rdn property
     */
    private String getRDN(String name) {
        if (name == null) {
            return name;
        }
        int indexOfEqual = name.indexOf('=');
        if (indexOfEqual < 0) {
            return name;
        }
        String rdnValue = name.substring(0, indexOfEqual);
        return rdnValue;
    }

    /**
     * Searches for users or groups in the underlying user registry.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Root search(Root root) throws WIMException {
        final String METHODNAME = "search";
        String uniqueName = null;
        boolean restRequest = true;
        Root returnRoot = new Root();

        AuditManager auditManager = new AuditManager();
        if (auditManager.getRESTRequest() == null)
            restRequest = false;

        List<Entity> entitys = root.getEntities();
        if (entitys != null && !entitys.isEmpty()) {
            Entity entitee = entitys.get(0);
            if (entitee != null) {
                IdentifierType identifier = entitee.getIdentifier();
                if (identifier != null)
                    uniqueName = identifier.getUniqueName();
            }
        }

        try {
            int countLimit = 0;

            Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
            SearchControl searchControl = (SearchControl) ctrlMap.get(SchemaConstants.DO_SEARCH_CONTROL);

            if (searchControl.isSetCountLimit()) {
                countLimit = searchControl.getCountLimit();
            }

            String expression = searchControl.getExpression();
            if (expression == null || expression.length() == 0) {
                if (restRequest)
                    Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "search", reposId, uniqueName, userRegistry.getRealm(), returnRoot,
                                Integer.valueOf("217"));
                throw new SearchControlException(WIMMessageKey.MISSING_SEARCH_EXPRESSION, Tr.formatMessage(tc, WIMMessageKey.MISSING_SEARCH_EXPRESSION));
            }

            URBridgeXPathHelper xpathHelper = new URBridgeXPathHelper(expression);
            expression = xpathHelper.getExpression();

            boolean returnSubType = searchControl.isReturnSubType();
            List<String> entityTypes = xpathHelper.getEntityTypes();
            Set<String> entityTypeSet = new HashSet<String>();
            List<String> entityTypeList = null;

            if (returnSubType) {
                for (int i = 0; i < entityTypes.size(); i++) {
                    String type = entityTypes.get(i);
                    Set<String> subTypes = Entity.getSubEntityTypes(type);
                    entityTypeSet.add(type);
                    if (subTypes != null) {
                        entityTypeSet.addAll(subTypes);
                    }
                }
            } else {
                entityTypeSet.addAll(entityTypes);
            }
            entityTypeList = new ArrayList<String>(entityTypeSet);

            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " entityType List: " + entityTypeList);

            String type;
            for (int i = 0; i < entityTypeList.size(); i++) {
                type = entityTypeList.get(i);
                if (Service.DO_GROUP.equalsIgnoreCase(type) || Entity.getSubEntityTypes(Service.DO_GROUP).contains(type)) {
                    List<String> searchAttrs = getAttributes(searchControl, type);
                    List<String> returnNames = new ArrayList<String>();

                    /*
                     * if ((SAFRegistryImplClass.equalsIgnoreCase(reg.getClass().getName()))&&!expression.endsWith(SchemaConstants.VALUE_WILD_CARD)) {
                     * try{
                     * String secName = reg.getGroupSecurityName(expression);
                     * returnNames.add(secName);
                     * }
                     * catch (com.ibm.websphere.security.EntryNotFoundException enfe) {
                     * }
                     * catch (com.ibm.websphere.security.CustomRegistryException cre) {
                     * }
                     * } else
                     */
                    {
                        if (!expression.contains(SchemaConstants.VALUE_WILD_CARD)) {
                            countLimit = 1;
                        }

                        returnNames = searchGroups(expression, countLimit).getList();
                    }
                    if (returnNames.size() > 0) {
                        URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
                        for (int j = 0; j < returnNames.size(); j++) {
                            Entity matchDO = null;

                            if (type.equalsIgnoreCase(Service.DO_PERSON_ACCOUNT))
                                matchDO = new PersonAccount();
                            else
                                matchDO = new Group();

                            returnRoot.getEntities().add(matchDO);
                            IdentifierType id = new IdentifierType();
                            matchDO.setIdentifier(id);
                            // Populate the entity with all requested attributes.
                            URBridgeEntity osEntity = osEntityFactory.createObject(matchDO, this, propsMap, baseEntryName,
                                                                                   entityConfigMap);
                            osEntity.setSecurityNameProp(returnNames.get(j));
                            osEntity.populateEntity(searchAttrs);
                            //set identifier
                            id.setRepositoryId(reposId);
                        }
                    }
                    break;
                }
            }
            for (int i = 0; i < entityTypeList.size(); i++) {
                type = entityTypeList.get(i);
                if (Entity.getSubEntityTypes(Service.DO_LOGIN_ACCOUNT).contains(type)) {
                    List<String> searchAttrs = getAttributes(searchControl, type);
                    List<String> returnNames = new ArrayList<String>();

                    /*
                     * if ((SAFRegistryImplClass.equalsIgnoreCase(reg.getClass().getName()))&&!expression.endsWith(SchemaConstants.VALUE_WILD_CARD)) {
                     * try{
                     * String secName= reg.getUserSecurityName(expression);
                     * returnNames.add(secName);
                     * }
                     * catch (com.ibm.websphere.security.EntryNotFoundException enfe) {
                     * }
                     * catch (com.ibm.websphere.security.CustomRegistryException cre) {
                     * }
                     * } else
                     */
                    {
                        if (!expression.contains(SchemaConstants.VALUE_WILD_CARD)) {
                            countLimit = 1;
                        }
                        returnNames = searchUsers(expression, countLimit).getList();
                    }
                    if (returnNames.size() > 0) {
                        URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
                        if (type.equalsIgnoreCase(Service.DO_LOGIN_ACCOUNT)) {
                            type = URBridgeHelper.getPersonAccountType();
                        }

                        for (int j = 0; j < returnNames.size(); j++) {
                            Entity matchDO = new PersonAccount();
                            returnRoot.getEntities().add(matchDO);
                            IdentifierType id = new IdentifierType();
                            matchDO.setIdentifier(id);
                            // Populate the entity with all requested attributes.
                            URBridgeEntity osEntity = osEntityFactory.createObject(matchDO, this, propsMap, baseEntryName,
                                                                                   entityConfigMap);
                            osEntity.setSecurityNameProp(returnNames.get(j));
                            osEntity.populateEntity(searchAttrs);
                            //set identifier
                            id.setRepositoryId(reposId);
                        }
                    }
                    break;
                }
            }
        } catch (WIMException we) {
            throw we;
        } catch (Exception e) {
            if (restRequest)
                Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "search", reposId, uniqueName, userRegistry.getRealm(), returnRoot,
                            Integer.valueOf("221"));
            throw new WIMApplicationException(WIMMessageKey.ENTITY_SEARCH_FAILED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_SEARCH_FAILED,
                                                                                                   WIMMessageHelper.generateMsgParms(e.toString())));
        }

        auditManager.setRealm(userRegistry.getRealm());
        if (isURBridgeResult(returnRoot) && restRequest)
            Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "search", reposId, uniqueName, userRegistry.getRealm(), returnRoot,
                        Integer.valueOf("200"));

        return returnRoot;
    }

    @Override
    @FFDCIgnore({ CertificateMapFailedException.class })
    public Root login(Root root) throws WIMException {
        final String METHODNAME = "login";

        // Create an output object.
        Root returnRoot = new Root();
        URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();

        List<String> attrList = null;
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        LoginControl propertyCtrl = (LoginControl) ctrlMap.get(Service.DO_LOGIN_CONTROL);

        if (propertyCtrl != null) {
            attrList = getAttributes(propertyCtrl, Service.DO_LOGIN_ACCOUNT);
        }

        List<Entity> entities = root.getEntities();

        if (entities.size() > 0) {
            // Deal with the current entity. Entities pulled out as they are authenticated.
            Entity ent = entities.get(0);
            String type = ent.getTypeName();
            String securityName = null;

            // Check if the entity type is an account
            if (Service.DO_LOGIN_ACCOUNT.equalsIgnoreCase(type) || Entity.getSubEntityTypes(Service.DO_LOGIN_ACCOUNT).contains(type)) {
                LoginAccount entity = (LoginAccount) entities.get(0);
                // Attempt to authenticate the user.
                if (entity.isSet(SchemaConstants.PROP_PRINCIPAL_NAME)) {
                    String pname = entity.getPrincipalName();
                    byte[] pwd = entity.getPassword();

                    if ((pname == null) || (pname.trim().length() == 0)) {
                        throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME));
                    }
                    if ((pwd == null) || (pwd.length == 0)) {
                        throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PASSWORD, Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PASSWORD));
                    }

                    String passwordStr;
                    try {
                        passwordStr = new String(pwd, "UTF-8");
                    } catch (UnsupportedEncodingException e1) {
                        throw new WIMApplicationException(WIMMessageKey.CUSTOM_REGISTRY_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.CUSTOM_REGISTRY_EXCEPTION,
                                                                                                                    WIMMessageHelper.generateMsgParms(reposId)));
                    }

                    // first need to check if valid user or not
                    boolean isValidUser = false;
                    if ((SAFRegistryImplClass.equalsIgnoreCase(userRegistry.getClass().getName()))) {
                        try {
                            isValidUser = userRegistry.isValidUser(pname);
                        } catch (RegistryException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, SPI_PREFIX + METHODNAME, " principal, " + pname + ", not found in " + reposId);
                            }
                        }
                    } else {
                        // check for CustomRegistryException and validate Users if users not empty
                        List<String> returnNames = null;
                        try {
                            returnNames = userRegistry.getUsers(pname, 1).getList();
                        } catch (RegistryException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, SPI_PREFIX + METHODNAME, " principal, " + pname + ", not found in " + reposId);
                            }
                        }
                        isValidUser = returnNames != null && returnNames.size() > 0;
                    }
                    if (isValidUser) {
                        try {
                            securityName = userRegistry.checkPassword(pname, passwordStr);
                        } catch (RegistryException e) {
                            throw new WIMException(e);
                        }
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, SPI_PREFIX + METHODNAME + " principal, " + pname + ", not found in " + reposId);
                        }
                    }
                } else if (entity.isSet(SchemaConstants.PROP_CERTIFICATE)) {
                    List<byte[]> certList = entity.getCertificate();
                    int certListSize = certList.size();

                    if (certListSize > 0) {
                        X509Certificate[] certs = new X509Certificate[certListSize];
                        for (int i = 0; i < certs.length; i++) {
                            ByteArrayInputStream bais = new ByteArrayInputStream(certList.get(i));
                            try {
                                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                                certs[i] = (X509Certificate) cf.generateCertificate(bais);
                                bais.close();
                            } catch (Exception e) {
                                throw new WIMApplicationException(WIMMessageKey.CERTIFICATE_GENERATE_FAILED, Tr.formatMessage(tc, WIMMessageKey.CERTIFICATE_GENERATE_FAILED));
                            }
                        }
                        try {
                            securityName = userRegistry.mapCertificate(certs[0]);
                        } catch (CertificateMapNotSupportedException e) {
                            throw new com.ibm.wsspi.security.wim.exception.CertificateMapNotSupportedException(e);
                        } catch (CertificateMapFailedException e) {
                            // throw new com.ibm.wsspi.security.wim.exception.CertificateMapFailedException(e);
                        } catch (RegistryException e) {
                            throw new WIMException(e);
                        }
                    }
                } else {
                    throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME));
                }

                // Check if the user was authenticated, and create entity accordingly.
                if (securityName != null) {
                    PersonAccount person = new PersonAccount();
                    returnRoot.getEntities().add(person);
                    IdentifierType id = new IdentifierType();
                    person.setIdentifier(id);
                    person.setPrincipalName(securityName);
                    // Populate the entity with the securityName attribute.
                    URBridgeEntity osEntity = osEntityFactory.createObject(person, this, attrMap, baseEntryName,
                                                                           entityConfigMap);
                    osEntity.setSecurityNameProp(securityName);
                    if (attrList != null) {
                        osEntity.populateEntity(attrList);
                    }
                    id.setRepositoryId(reposId);
                }
            } else {
                throw new WIMApplicationException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
                                                                                                            WIMMessageHelper.generateMsgParms(type)));
            }
        }

        return returnRoot;
    }

    /**
     * @param uniqueId
     * @return
     */
    @FFDCIgnore({ RegistryException.class })
    private boolean isValidUserOrGroup(String uniqueId) {
        try {
            if (userRegistry.isValidUser(uniqueId))
                return true;
            else if (userRegistry.isValidGroup(uniqueId))
                return true;
            else
                return false;
        } catch (RegistryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unable to determine if this is a valid User/Group");
            return false;
        }
    }

    @Override
    public String getRealm() {
        return userRegistry.getRealm();
    }

    /**
     * @param uniqueId
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getUserSecurityName(String uniqueId) throws EntryNotFoundException, RegistryException {
        String secName = null;
        if (iUserSecurityNameCache.containsKey(uniqueId))
            secName = (String) iUserSecurityNameCache.get(uniqueId);
        else {
            secName = userRegistry.getUserSecurityName(uniqueId);
            iUserSecurityNameCache.put(uniqueId, secName);
        }

        return secName;
    }

    /**
     * @param securityName
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getUniqueUserId(String securityName) throws EntryNotFoundException, RegistryException {
        String uniqueId = null;
        if (iUserUniqueIdCache.containsKey(securityName))
            uniqueId = (String) iUserUniqueIdCache.get(securityName);
        else {
            uniqueId = userRegistry.getUniqueUserId(securityName);
            iUserUniqueIdCache.put(securityName, uniqueId);
        }

        return uniqueId;
    }

    /**
     * @param securityName
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getUserDisplayName(String securityName) throws EntryNotFoundException, RegistryException {
        String displayName = null;
        if (iUserDispNameCache.containsKey(securityName))
            displayName = (String) iUserDispNameCache.get(securityName);
        else {
            displayName = userRegistry.getUserDisplayName(securityName);
            iUserDispNameCache.put(securityName, displayName);
        }

        return displayName;
    }

    /**
     * @param uniqueId
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getGroupSecurityName(String uniqueId) throws EntryNotFoundException, RegistryException {
        String secName = null;
        if (iGroupSecurityNameCache.containsKey(uniqueId))
            secName = (String) iGroupSecurityNameCache.get(uniqueId);
        else {
            secName = userRegistry.getGroupSecurityName(uniqueId);
            iGroupSecurityNameCache.put(uniqueId, secName);
        }

        return secName;
    }

    /**
     * @param securityName
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getUniqueGroupId(String securityName) throws EntryNotFoundException, RegistryException {
        String uniqueId = null;
        if (iGroupUniqueIdCache.containsKey(securityName))
            uniqueId = (String) iGroupUniqueIdCache.get(securityName);
        else {
            uniqueId = userRegistry.getUniqueGroupId(securityName);
            iGroupUniqueIdCache.put(securityName, uniqueId);
        }

        return uniqueId;
    }

    /**
     * @param securityName
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public String getGroupDisplayName(String securityName) throws EntryNotFoundException, RegistryException {
        String displayName = null;
        if (iGroupDispNameCache.containsKey(securityName))
            displayName = (String) iGroupDispNameCache.get(securityName);
        else {
            displayName = userRegistry.getGroupDisplayName(securityName);
            iGroupDispNameCache.put(securityName, displayName);
        }

        return displayName;
    }

    /**
     * @param secName
     * @param i
     * @return
     * @throws RegistryException
     */
    private SearchResult searchUsers(String secName, int i) throws RegistryException {
        String key = getKey(secName, i);
        if (iUserSearchCache.containsKey(key))
            return (SearchResult) iUserSearchCache.get(key);
        else {
            SearchResult result = userRegistry.getUsers(secName, i);
            iUserSearchCache.put(key, result);
            return result;
        }
    }

    /**
     * @param secName
     * @param i
     * @return
     * @throws RegistryException
     */
    private SearchResult searchGroups(String secName, int i) throws RegistryException {
        String key = getKey(secName, i);
        if (iGroupSearchCache.containsKey(key))
            return (SearchResult) iGroupSearchCache.get(key);
        else {
            SearchResult result = userRegistry.getGroups(secName, i);
            iGroupSearchCache.put(key, result);
            return result;
        }
    }

    /**
     * @param securityName
     * @return
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    public List<String> getGroupsForUser(String securityName) throws EntryNotFoundException, RegistryException {
        return userRegistry.getGroupsForUser(securityName);
    }

    /**
     * @param securityName
     * @param countLimit
     * @return
     * @throws RegistryException
     * @throws CustomRegistryException
     * @throws EntryNotFoundException
     * @throws NotImplementedException
     * @throws RemoteException
     */
    public SearchResult getUsersForGroup(String securityName,
                                         int countLimit) throws RemoteException, NotImplementedException, EntryNotFoundException, CustomRegistryException, RegistryException {
        return userRegistry.getUsersForGroup(securityName, countLimit);
    }

    /**
     * @param returnRoot
     */
    private void setReturnContext(Root inRoot, Root returnRoot) {
        // Check if there is a valid response
        if (returnRoot != null && !returnRoot.getEntities().isEmpty()) {
            // Determine if the input object to check if the context was set.
            boolean hasIsURBrigeResult = false;
            if (inRoot != null) {
                List<Context> contexts = inRoot.getContexts();
                for (Context contextInput : contexts) {
                    String key = contextInput.getKey();

                    if (key != null && SchemaConstantsInternal.IS_URBRIDGE_RESULT.equals(key)) {
                        hasIsURBrigeResult = true;
                    }
                }
            }

            if (hasIsURBrigeResult) {
                // Add context for URBridge
                Context context = new Context();
                context.setKey(SchemaConstantsInternal.IS_URBRIDGE_RESULT);
                context.setValue("true");
                returnRoot.getContexts().add(context);
            }
        }
    }

    @Override
    public Root delete(Root root) throws WIMException {
        AuditManager auditManager = new AuditManager();
        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "delete", auditManager.getRepositoryId(), auditManager.getRepositoryUniqueName(),
                    userRegistry.getRealm(), root, Integer.valueOf("209"));

        throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
                                                                                                               WIMMessageHelper.generateMsgParms(reposId)));
    }

    @Override
    public Root create(Root root) throws WIMException {
        AuditManager auditManager = new AuditManager();
        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "create", auditManager.getRepositoryId(), auditManager.getRepositoryUniqueName(),
                    userRegistry.getRealm(), root, Integer.valueOf("209"));

        throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
                                                                                                               WIMMessageHelper.generateMsgParms(reposId)));
    }

    @Override
    public Root update(Root root) throws WIMException {
        AuditManager auditManager = new AuditManager();
        Audit.audit(Audit.EventID.SECURITY_MEMBER_MGMT_01, auditManager.getRESTRequest(), "update", auditManager.getRepositoryId(), auditManager.getRepositoryUniqueName(),
                    userRegistry.getRealm(), root, Integer.valueOf("209"));

        throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
                                                                                                               WIMMessageHelper.generateMsgParms(reposId)));
    }

    /**
     *
     */
    public void stopCacheThreads() {
        if (iUserSecurityNameCache != null)
            iUserSecurityNameCache.stopEvictionTask();
        if (iUserUniqueIdCache != null)
            iUserUniqueIdCache.stopEvictionTask();
        if (iUserDispNameCache != null)
            iUserDispNameCache.stopEvictionTask();
        if (iGroupSecurityNameCache != null)
            iGroupSecurityNameCache.stopEvictionTask();
        if (iUserSearchCache != null)
            iUserSearchCache.stopEvictionTask();
        if (iGroupSearchCache != null)
            iGroupSearchCache.stopEvictionTask();
        if (iGroupUniqueIdCache != null)
            iGroupUniqueIdCache.stopEvictionTask();
        if (iGroupDispNameCache != null)
            iGroupDispNameCache.stopEvictionTask();
    }

    /**
     * @param returnRoot
     */
    private boolean isURBridgeResult(Root returnRoot) {
        // Check if there is a valid response
        if (returnRoot != null && !returnRoot.getEntities().isEmpty()) {
            // Determine if the return object to check if the context was set.
            List<Context> contexts = returnRoot.getContexts();
            for (Context context : contexts) {
                String key = context.getKey();

                if (key != null && SchemaConstantsInternal.IS_URBRIDGE_RESULT.equals(key)) {
                    if ("true".equalsIgnoreCase((String) context.getValue()))
                        return true;
                }
            }
        }
        return false;
    }

/*
 * @Override
 * public Root delete(Root root) throws WIMException {
 * if(userRegistry instanceof WriteableUserRegistry) {
 * if(root.getEntities().size() > 0) {
 * Root returnRoot = new Root();
 * Entity inEntity = root.getEntities().get(0);
 * IdentifierType inId = inEntity.getIdentifier();
 *
 * String memberType = validateEntity(inEntity);
 * String uniqueName = inId.getUniqueName();
 *
 * Entity returnEntity = null;
 * if (Service.DO_GROUP.equalsIgnoreCase(memberType))
 * returnEntity = new Group();
 * else
 * returnEntity = new PersonAccount();
 *
 * returnRoot.getEntities().add(returnEntity);
 * IdentifierType identifier = new IdentifierType();
 * identifier.setRepositoryId(reposId);
 * returnEntity.setIdentifier(identifier);
 *
 * // Wrap the entity in an object that provides functionality to the entity.
 * URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
 * URBridgeEntity osEntity = osEntityFactory.createObject(returnEntity, userRegistry,
 * propsMap, baseEntryName, entityConfigMap);
 * osEntity.setSecurityNameProp(uniqueName);
 *
 * if (Service.DO_GROUP.equalsIgnoreCase(memberType))
 * try {
 * ((WriteableUserRegistry)userRegistry).deleteGroup(uniqueName);
 * } catch (DeleteGroupFailedException e) {
 * throw new WIMException(e);
 * }
 * else
 * try {
 * ((WriteableUserRegistry)userRegistry).deleteUser(uniqueName);
 * } catch (DeleteUserFailedException e) {
 * throw new WIMException(e);
 * }
 *
 * return returnRoot;
 * }
 * }
 * else {
 * throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * WIMMessageHelper.generateMsgParms(reposId)));
 * }
 *
 * return null;
 * }
 *
 * @Override
 * public Root create(Root root) throws WIMException {
 * if(userRegistry instanceof WriteableUserRegistry) {
 * if(root.getEntities().size() > 0) {
 * Root returnRoot = new Root();
 * Entity inEntity = root.getEntities().get(0);
 * IdentifierType inId = inEntity.getIdentifier();
 *
 * String memberType = validateEntity(inEntity);
 * String uniqueName = inId.getUniqueName();
 *
 * Entity returnEntity = null;
 * if (Service.DO_GROUP.equalsIgnoreCase(memberType))
 * returnEntity = new Group();
 * else
 * returnEntity = new PersonAccount();
 *
 * returnRoot.getEntities().add(returnEntity);
 * IdentifierType identifier = new IdentifierType();
 * identifier.setRepositoryId(reposId);
 * returnEntity.setIdentifier(identifier);
 *
 * // Wrap the entity in an object that provides functionality to the entity.
 * URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
 * URBridgeEntity osEntity = osEntityFactory.createObject(returnEntity, userRegistry,
 * propsMap, baseEntryName, entityConfigMap);
 * osEntity.setSecurityNameProp(uniqueName);
 *
 * if (Service.DO_GROUP.equalsIgnoreCase(memberType))
 * try {
 * List<Entity> memberEntities = ((Group)inEntity).getMembers();
 * String[] members = null;
 * if(memberEntities != null && memberEntities.size() > 0) {
 * members = new String[memberEntities.size()];
 * int count = 0;
 * for(Entity member : memberEntities)
 * members[count++] = member.getIdentifier().getUniqueName();
 * }
 * ((WriteableUserRegistry)userRegistry).createGroup(uniqueName, members);
 * } catch (CreateGroupFailedException e) {
 * throw new WIMException(e);
 * }
 * else
 * try {
 * String password = new String(((PersonAccount)inEntity).getPassword());
 * ((WriteableUserRegistry)userRegistry).createUser(uniqueName, password);
 * } catch (CreateUserFailedException e) {
 * throw new WIMException(e);
 * }
 *
 * return returnRoot;
 * }
 * }
 * else {
 * throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * WIMMessageHelper.generateMsgParms(reposId)));
 * }
 *
 * return null;
 * }
 *
 * @Override
 * public Root update(Root root) throws WIMException {
 * if(userRegistry instanceof WriteableUserRegistry) {
 * if(root.getEntities().size() > 0) {
 * Root returnRoot = new Root();
 * Entity inEntity = root.getEntities().get(0);
 * IdentifierType inId = inEntity.getIdentifier();
 *
 * String type = validateEntity(inEntity);
 * String uniqueName = inId.getUniqueName();
 *
 * Entity returnEntity = null;
 * if (Service.DO_GROUP.equalsIgnoreCase(type))
 * returnEntity = new Group();
 * else
 * returnEntity = new PersonAccount();
 *
 * returnRoot.getEntities().add(returnEntity);
 * IdentifierType identifier = new IdentifierType();
 * identifier.setRepositoryId(reposId);
 * returnEntity.setIdentifier(identifier);
 *
 * // Wrap the entity in an object that provides functionality to the entity.
 * URBridgeEntityFactory osEntityFactory = new URBridgeEntityFactory();
 * URBridgeEntity osEntity = osEntityFactory.createObject(returnEntity, userRegistry,
 * propsMap, baseEntryName, entityConfigMap);
 * osEntity.setSecurityNameProp(uniqueName);
 *
 * if (Service.DO_GROUP.equalsIgnoreCase(type)) {
 * try {
 * if(((Group)inEntity).isSetDescription() && ((Group)inEntity).getDescription().size() > 0) {
 * String groupDescription = ((Group)inEntity).getDescription().get(0);
 * ((WriteableUserRegistry)userRegistry).updateGroup(uniqueName, groupDescription);
 * }
 * else {
 * if(root.getContexts() != null && root.getContexts().size() > 0) {
 * boolean replaceMembers = false;
 * boolean deleteMembers = false;
 * for(Context context : root.getContexts()) {
 * if(context.getKey().equalsIgnoreCase(REPLACE_MEMBERS)) {
 * replaceMembers = true;
 * break;
 * }
 * else if(context.getKey().equalsIgnoreCase(DELETE_MEMBERS)) {
 * deleteMembers = true;
 * break;
 * }
 * }
 *
 * if(replaceMembers || deleteMembers) {
 * List<String> members = new ArrayList<String>();
 * for(Entity member : ((Group)inEntity).getMembers()) {
 * members.add(member.getIdentifier().getUniqueName());
 * }
 * if(replaceMembers)
 * ((WriteableUserRegistry)userRegistry).replaceGroupMembers(uniqueName, members);
 * else
 * ((WriteableUserRegistry)userRegistry).deleteGroupMembers(uniqueName, members);
 * }
 * }
 * }
 * }
 * catch (UpdateGroupFailedException e) {
 * throw new WIMException(e);
 * }
 * }
 * else {
 * try {
 * if(((PersonAccount)inEntity).isSetPassword()) {
 * String password = new String(((PersonAccount)inEntity).getPassword());
 * String oldPassword = null;
 * if(root.getContexts() != null && root.getContexts().size() > 0) {
 * for (Context context : root.getContexts()) {
 * if(context.getKey().equalsIgnoreCase(OLD_PASSWORD)) {
 * oldPassword = (String)context.getValue();
 * break;
 * }
 * }
 * }
 * ((WriteableUserRegistry)userRegistry).updatePassword(uniqueName, oldPassword, password);
 * }
 * else {
 * if(root.getContexts() != null && root.getContexts().size() > 0) {
 * String newuserSecurityName = null;
 * for (Context context : root.getContexts()) {
 * if(context.getKey().equalsIgnoreCase(NEW_SECURITY_NAME)) {
 * newuserSecurityName = (String)context.getValue();
 * break;
 * }
 * }
 *
 * if(newuserSecurityName != null)
 * ((WriteableUserRegistry)userRegistry).renameUser(uniqueName, newuserSecurityName);
 * }
 * }
 * } catch (UpdateUserFailedException e) {
 * throw new WIMException(e);
 * }
 * }
 *
 * return returnRoot;
 * }
 * }
 * else {
 * throw new WIMApplicationException(WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * Tr.formatMessage(tc, WIMMessageKey.CANNOT_WRITE_TO_READ_ONLY_REPOSITORY,
 * WIMMessageHelper.generateMsgParms(reposId)));
 * }
 *
 * return null;
 * }
 */
}
