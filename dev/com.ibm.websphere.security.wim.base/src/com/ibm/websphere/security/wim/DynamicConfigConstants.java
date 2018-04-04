/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim;

/**
 * The interface containing all dynamic configuration related constants.
 */
public interface DynamicConfigConstants {
    /**
     * The key to specify the profile repository configuration data object.
     */
    String DYNA_CONFIG_KEY_REPOS_CONFIG = "DYNA_CONFIG_KEY_REPOS_CONFIG";
    /**
     * The key to specify the property extension repository configuration data object.
     */
    String DYNA_CONFIG_KEY_PROP_EXT_REPOS_CONFIG = "DYNA_CONFIG_KEY_PROP_EXT_REPOS_CONFIG";
    /**
     * The key to specify the new LDAP bind DN.
     */
    String DYNA_CONFIG_KEY_LDAP_BIND_DN = "DYNA_CONFIG_KEY_LDAP_BIND_DN";
    /**
     * The key to specify the new LDAP bind password.
     */
    String DYNA_CONFIG_KEY_LDAP_BIND_PASSWORD = "DYNA_CONFIG_KEY_LDAP_BIND_PASSWORD";
    /**
     * The key to specify the database administrative password.
     */
    String DYNA_CONFIG_KEY_DB_ADMIN_PASSWORD = "DYNA_CONFIG_KEY_DB_ADMIN_PASSWORD";
    /**
     * The key to specify the repository id.
     */
    String DYNA_CONFIG_KEY_REPOS_ID = "DYNA_CONFIG_KEY_REPOS_ID";
    /**
     * The key to specify the name of the base entry in vmm.
     */
    String DYNA_CONFIG_KEY_BASE_ENTRY = "DYNA_CONFIG_KEY_BASE_ENTRY";
    /**
     * The key to specify the name of the base entry in repository.
     */
    String DYNA_CONFIG_KEY_BASE_ENTRY_IN_REPOS = "DYNA_CONFIG_KEY_BASE_ENTRY_IN_REPOS";
    /**
     * The key to specify the Hashtable which contains data objects for specifying the configuration information of the entity type which are specific to each repository.
     */
    String DYNA_CONFIG_KEY_ENTITY_CONFIGS = "DYNA_CONFIG_KEY_ENTITY_CONFIGS";
    /**
     * The key to specify the data object for specifying the configuration information of a property which are specific to a repository.
     */
    String DYNA_CONFIG_KEY_PROP_CONFIG = "DYNA_CONFIG_KEY_PROP_CONFIG";
    /**
     * The key to specify the qualified name of the entity type.
     */
    String DYNA_CONFIG_KEY_ENTITY_TYPE = "DYNA_CONFIG_KEY_ENTITY_TYPE";
    /**
     * The key to specify the list which contains the RDN properties of a entity type.
     */
    String DYNA_CONFIG_KEY_RDNS = "DYNA_CONFIG_KEY_RDNS";
    /**
     * The key to specify the default parent of a entity type.
     */
    String DYNA_CONFIG_KEY_DEFAULT_PARENT = "DYNA_CONFIG_KEY_DEFAULT_PARENT";
    /**
     * The key to specify the data object for specifying the configuration information of a realm.
     */
    String DYNA_CONFIG_KEY_REALM_CONFIG = "DYNA_CONFIG_KEY_REALM_CONFIG";
    /**
     * The key to specify the name of a realm.
     */
    String DYNA_CONFIG_KEY_REALM_NAME = "DYNA_CONFIG_KEY_REALM_NAME";

    /**
     * Dynamically adds a new repository to the current configuration at runtime.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_REPOS_CONFIG (required) - [DataObject] profile repository configuration data object based on wimconfig.xsd.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_REPOSITORY = "websphere.usermanager.serviceprovider.add.repository";
    /**
     * Dynamically adds a new base entry to the specified profile repository.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_REPOS_ID (required) - [String] The id of the repository to add the base entry to.
     * <li>DYNA_CONFIG_KEY_BASE_ENTRY (required) - [String] The base entry to be added.
     * <li>DYNA_CONFIG_KEY_BASE_ENTRY_IN_REPOS - [String] The name of the base entry in the repository if it is different from the name in vmm.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_BASE_ENTRY = "websphere.usermanager.serviceprovider.add.baseentry";
    /**
     * Dynamically adds a new supported entity type configuration. This entity type must have existed in schema.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_ENTITY_TYPE (required) - [String] The qualified name of the entity type to be added.
     * <li>DYNA_CONFIG_KEY_RDNS (required) - [List] The RDN properties of the entity type.
     * <li>DYNA_CONFIG_KEY_DEFAULT_PARENT (required) - [String] The default parent of the entity type.
     * <li>DYNA_CONFIG_KEY_ENTITY_CONFIGS - [Hashtable] The key of the Hashtable is the repository id, the value of the Hashtable is a data object.
     * For example, if the repository is of LDAP type, the data object is the LdapEntityTypes,
     * which can contain all configuration setting specific to LDAP adapter. If the repository does not need any specific setting (like Database repository), this parameter is not
     * needed.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_ENTITY_CONFIG = "websphere.usermanager.serviceprovider.add.entityconfig";
    /**
     * Dynamically adds a new property configuration to the specified profile repository. This property must have existed in schema.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_REPOS_ID (required) - [String] The id of the repository to add the property configuration to.
     * <li>DYNA_CONFIG_KEY_PROP_CONFIG (required) - [DataObject] The configuration data object based on wimconfig.xsd.
     * It contains configuration information of this property which are specific to this repository.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_PROPERTY_CONFIG = "websphere.usermanager.serviceprovider.add.propertyconfig";
    /**
     * Dynamically adds a new realm to the current configuration at runtime.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_REALM_CONFIG (required) - [DataObject] realm configuration data object based on wimconfig.xsd.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_REALM = "websphere.usermanager.serviceprovider.add.realm";
    /**
     * Dynamically adds a new participating base entry to a realm.
     * Base entry must be already defined in one of the profile repository.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_BASE_ENTRY (required) - [String] the base entry to be added.
     * <li>DYNA_CONFIG_KEY_REALM_NAME (required) - [String] the name of the realm.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_PARTICIPATING_BASE_ENTRY = "websphere.usermanager.serviceprovider.add.participatingbaseentry";
    /**
     * Dynamically adds a new default parent to a realm.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_DEFAULT_PARENT (required) - [String] the unique name of the default parent.
     * <li>DYNA_CONFIG_KEY_ENTITY_TYPE (required) - [String] the qualified name of the entity type to add the default parent to.
     * <li>DYNA_CONFIG_KEY_REALM_NAME (required) - [String] the name of the realm.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_DEFAULT_PARENT_TO_REALM = "websphere.usermanager.serviceprovider.add.defaultparenttorealm";
    /**
     * Dynamically adds property extension repository to the current configuration at runtime.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_PROP_EXT_REPOS_CONFIG (required) - [DataObject] property extension repository configuration data object based on wimconfig.xsd.
     * </ul>
     *
     */
    String DYNA_CONFIG_EVENT_ADD_PROPERTY_EXTENSION_REPOSITORY = "websphere.usermanager.serviceprovider.add.propertyextensionrepository";
    /**
     * Dynamically update the LDAP bind information of the specified LDAP repository at runtime.
     *
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_LDAP_BIND_DN - [String] the new LDAP bind DN.
     * <li>DYNA_CONFIG_KEY_LDAP_BIND_PASSWORD - [UTF-8 encoded byte array] the new LDAP bind password.
     * This parameter is required if either DYNA_CONFIG_KEY_LDAP_BIND_DN is specified.
     * <li>DYNA_CONFIG_KEY_REPOS_ID (required) - [String] the repository id of the LDAP adapter to update.
     * </ul>
     *
     * Following are the scenarios:
     * <ul>
     * <li>DYNA_CONFIG_KEY_LDAP_BIND_DN + DYNA_CONFIG_KEY_LDAP_BIND_PASSWORD+ DYNA_CONFIG_KEY_REPOS_ID:
     * The specified LDAP repository will be updated with the new bind DN and password.
     *
     * <li>DYNA_CONFIG_KEY_REPOS_ID:
     * Then bind information (including host names, bind DN, bind password,context pool settings and other settings related to LDAP server)
     * in wimconfig.xml will be read and used for reset the LDAP bind in the specified LDAP repository.
     * </ul>
     */
    String DYNA_CONFIG_EVENT_UPDATE_LDAP_BIND_INFO = "websphere.usermanager.serviceprovider.update.ldap.bindinfo";
    /**
     * Dynamically update the database administrator password for database repository.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_DB_ADMIN_PASSWORD (required) - [UTF-8 encoded byte array] the new admin password.
     * <li>DYNA_CONFIG_KEY_REPOS_ID (required) - [String] the repository id of the database repository to update.
     * </ul>
     */
    String DYNA_CONFIG_EVENT_UPDATE_DB_ADMIN_PASSWORD = "websphere.usermanager.serviceprovider.update.db.adminidpassword";
    /**
     * Dynamically update the database administrator password for entry mapping repository.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_DB_ADMIN_PASSWORD (required) - [UTF-8 encoded byte array] the new admin password.
     * </ul>
     */
    String DYNA_CONFIG_EVENT_UPDATE_ENTRY_MAPPING_ADMIN_PASSWORD = "websphere.usermanager.serviceprovider.update.entrymapping.adminidpassword";
    /**
     * Dynamically update the database administrator password for property extension repository.
     * This constant is used as first input parameter in dynamicUpdateconfig method:
     * public void dynamicUpdateConfig(String updateType, Hashtable configData)
     *
     * Config data (key - value):
     * <ul>
     * <li>DYNA_CONFIG_KEY_DB_ADMIN_PASSWORD (required) - [UTF-8 encoded byte array] the new admin password.
     * </ul>
     */
    String DYNA_CONFIG_EVENT_UPDATE_PROPERTY_EXTENSION_ADMIN_PASSWORD = "websphere.usermanager.serviceprovider.update.propertyextension.adminidpassword";
}
