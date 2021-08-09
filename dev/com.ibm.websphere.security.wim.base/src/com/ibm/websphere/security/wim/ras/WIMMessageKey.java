/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras;

/**
 * This file contains the virtual member manager message keys.
 */
public interface WIMMessageKey {

    /**
     * property 'prop_name' is not defined.
     */
    String PROPERTY_NOT_DEFINED = "PROPERTY_NOT_DEFINED";

    /**
     * The entity 'unique_id' was not found.
     */
    String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";

    /**
     * The parent of the entity to be created 'uniqueName' was not found.
     */
    String PARENT_NOT_FOUND = "PARENT_NOT_FOUND";

    /**
     * The operation 'action_type' is not supported for entity type
     * 'entity_type'.
     */
    String OPERATION_NOT_SUPPORTED = "OPERATION_NOT_SUPPORTED";

    /**
     * Mandatory property 'property_name' is missing.
     */
    String MISSING_MANDATORY_PROPERTY = "MISSING_MANDATORY_PROPERTY";

    /**
     * SerachControl is missing for the search call.
     */
    String MISSING_SEARCH_CONTROL = "MISSING_SEARCH_CONTROL";

    /**
     * cookie is missing from the PageControl data object.
     */
    String MISSING_COOKIE = "MISSING_COOKIE";

    /**
     * cookie specified in the PageControl data object is invalid.
     */
    String INVALID_COOKIE = "INVALID_COOKIE";

    /**
     * The number of search results 'result_size' exceeds the maximum search
     * result limit 'search_limit'.
     */
    String EXCEED_MAX_TOTAL_SEARCH_LIMIT = "EXCEED_MAX_TOTAL_SEARCH_LIMIT";

    /**
     * The syntax of the member DN 'unique_name'is invalid. Check if the special
     * characters are escaped.
     */
    String INVALID_UNIQUE_NAME_SYNTAX = "INVALID_UNIQUE_NAME_SYNTAX";

    /**
     * The syntax of the LDAP DN 'dn'is invalid. Check if the special
     * characters are escaped.
     */
    String INVALID_DN_SYNTAX = "INVALID_DN_SYNTAX";

    /**
     * The initialize property 'property_name'is invalid.
     */
    String INVALID_INIT_PROPERTY = "INVALID_INIT_PROPERTY";

    /**
     * The entity 'unique_name'has descendants.
     */
    String ENTITY_HAS_DESCENDENTS = "ENTITY_HAS_DESCENDENTS";

    /**
     * The following naming exception occured during processing: 'Root NamingException'.
     */
    String NAMING_EXCEPTION = "NAMING_EXCEPTION";

    /**
     * The following system exception occured during processing: 'root_exception'.
     */
    String SYSTEM_EXCEPTION = "SYSTEM_EXCEPTION";

    /**
     * The following SQL Exception occured during processing: 'Root SQLException'.
     */
    String SQL_EXCEPTION = "SQL_EXCEPTION";

    /**
     * The following generic Exception occured during processing: 'Root Exception'.
     */
    String GENERIC = "GENERIC";

    /**
     * The data type of Property 'property_name' is invalid.
     */
    String INVALID_PROPERTY_DATA_TYPE = "INVALID_PROPERTY_DATA_TYPE";

    /**
     * The sort keys are missing from the SortControl data object.
     */
    String MISSING_SORT_KEY = "MISSING_SORT_KEY";

    /**
     * The identifier is invalid
     */
    String INVALID_IDENTIFIER = "INVALID_IDENTIFIER";

    /**
     * The identifier of a entity is required
     */
    String ENTITY_IDENTIFIER_NOT_SPECIFIED = "ENTITY_IDENTIFIER_NOT_SPECIFIED";

    /**
     * specify multiple entities in one call is not allowed
     */
    String ACTION_MULTIPLE_ENTITIES_SPECIFIED = "ACTION_MULTIPLE_ENTITIES_SPECIFIED";

    /**
     * The class or interface ''{0}'' defined in configuration property ''{0}'' is not found.
     */
    String CLASS_OR_INTERFACE_NOT_FOUND = "CLASS_OR_INTERFACE_NOT_FOUND";

    /**
     * The virtual member manager model package with name space URI ''http://www.ibm.com/websphere/wim'' is not found in XSD file ''{0}'' defined in configuration property
     * ''xsdFileName''.
     */
    String WIM_MODEL_PACKAGE_NOT_FOUND_IN_XSD = "WIM_MODEL_PACKAGE_NOT_FOUND_IN_XSD";

    /**
     * The package name ''{0}'' defined in configuration property ''{0}'' is invalid.
     */
    String INVALID_PACKAGE_NAME = "INVALID_PACKAGE_NAME";

    /**
     * virtual member manager configuration XML file ''{0}'' is not found.
     */
    String WIM_CONFIG_XML_FILE_NOT_FOUND = "WIM_CONFIG_XML_FILE_NOT_FOUND";

    /**
     * Ticket for the asynchronous operation is invalid: {0}
     */
    String INVALID_TICKET = "INVALID_TICKET";

    /**
     * The method is not implemented.
     */
    String METHOD_NOT_IMPLEMENTED = "METHOD_NOT_IMPLEMENTED";

    /**
     * The required identifiers are missing
     */
    String REQUIRED_IDENTIFIERS_MISSING = "REQUIRED_IDENTIFIERS_MISSING";

    /**
     * The specified entity type is not supported.
     */
    String ENTITY_TYPE_NOT_SUPPORTED = "ENTITY_TYPE_NOT_SUPPORTED";

    /**
     * The specified attribute type is not supported.
     */
    String ATTRIBUTE_NOT_SUPPORTED = "ATTRIBUTE_NOT_SUPPORTED";

    /**
     * A required control object is missing from the datagraph.
     */
    String MISSING_REQUIRED_CONTROL = "MISSING_REQUIRED_CONTROL";

    /**
     * A required property in a control is not set.
     */
    String MISSING_CONTROL_ATTRIBUTE = "MISSING_CONTROL_ATTRIBUTE";

    /**
     * Login information for a user is not set.
     */
    String LOGIN_INFORMATION_MISSING = "LOGIN_INFORMATION_MISSING";

    /**
     * A search expression is not correctly formatted.
     */
    String MALFORMED_SEARCH_EXPRESSION = "MALFORMED_SEARCH_EXPRESSION";

    /**
     * An RDN style format is required but not set for the identifiers uniqueName
     */
    String RDN_STYLE_FORMAT_REQUIRED = "RDN_STYLE_FORMAT_REQUIRED";

    /**
     * An attribute for an 'entityType' in the wimconfig.xml is not set.
     */
    String ENTITY_TYPE_CONFIGURATION_ATTRIBUTE_MISSING = "ENTITY_TYPE_CONFIGURATION_ATTRIBUTE_MISSING";

    /**
     * The realm information in the datagraph does not match the realm of the repository.
     */
    String INCORRECT_REALM = "INCORRECT_REALM";

    /**
     * The realm name passed in {0} is invalid.
     */
    String INVALID_REALM_NAME = "INVALID_REALM_NAME";

    /**
     * The '{0}' base entry name specified in '{1}' is not valid.
     */
    String INVALID_BASE_ENTRY_NAME = "INVALID_BASE_ENTRY_NAME";

    /**
     * No repository configuration found for the base entry '{0}'.
     */
    String REPOSITORY_NOT_FOUND_FOR_BASE_ENTRY = "REPOSITORY_NOT_FOUND_FOR_BASE_ENTRY";

    /**
     * No repository configuration found for the realm '{0}'.
     */
    String REPOSITORY_NOT_FOUND_FOR_REALM = "REPOSITORY_NOT_FOUND_FOR_REALM";

    /**
     * Configuration file '{0}' is invalid. Root cause is '{1}'.
     */
    String INVALID_WIM_CONFIG_XML_FILE = "INVALID_WIM_CONFIG_XML_FILE";

    /**
     * Extension XML file ''{0}'' is invalid. Root cause is ''{1}''.
     */
    String INVALID_WIM_EXTENSION_XML_FILE = "INVALID_WIM_EXTENSION_XML_FILE";

    /**
     * The property "{0}" is not defined for entity type "{1}".
     */
    String PROPERTY_NOT_DEFINED_FOR_ENTITY = "PROPERTY_NOT_DEFINED_FOR_ENTITY";

    /**
     * The "expression" property is missing from the SearchControl data object.
     */
    String MISSING_SEARCH_EXPRESSION = "MISSING_SEARCH_EXPRESSION";

    /**
     * The specified entity is not in the scope of the realm.
     */
    String ENTITY_NOT_IN_REALM_SCOPE = "ENTITY_NOT_IN_REALM_SCOPE";

    /**
     * The default parent cannot be found for a particular entity type
     */
    String DEFAULT_PARENT_NOT_FOUND = "DEFAULT_PARENT_NOT_FOUND";

    /**
     * The operation "method_name" is not supported in repository "repos_id".
     */
    String OPERATION_NOT_SUPPORTED_IN_REPOSITORY = "OPERATION_NOT_SUPPORTED_IN_REPOSITORY";

    /**
     * Asynchronous operation is not supported by the repository, repos_id.
     */
    String ASYNC_OPERATION_NOT_SUPPORTED_BY_REPOSITORY = "ASYNC_OPERATION_NOT_SUPPORTED_BY_REPOSITORY";

    /**
     * The entity 'unique_name' already exist.
     */
    String ENTITY_ALREADY_EXIST = "ENTITY_ALREADY_EXIST";

    /**
     * Failed to create entity: 'reason'
     */
    String ENTITY_CREATE_FAILED = "ENTITY_CREATE_FAILED";

    /**
     * Failed to update entity 'uniqueName' : 'reason'
     */
    String ENTITY_UPDATE_FAILED = "ENTITY_UPDATE_FAILED";

    /**
     * Failed to delete entity 'uniqueName' : 'reason'
     */
    String ENTITY_DELETE_FAILED = "ENTITY_DELETE_FAILED";

    /**
     * Failed to get entity 'uniqueName' : 'reason'
     */
    String ENTITY_GET_FAILED = "ENTITY_GET_FAILED";

    /**
     * Failed to search entity : 'reason'
     */
    String ENTITY_SEARCH_FAILED = "ENTITY_SEARCH_FAILED";

    /**
     * Failed to read file 'fileName': 'reason'
     */
    String ERROR_READING_FILE = "ERROR_READING_FILE";

    /**
     * Failed to write to file 'fileName': 'reason'
     */
    String ERROR_WRITING_FILE = "ERROR_WRITING_FILE";

    /**
     * The directory 'dir_name' is not found.
     */
    String DIRECTORY_NOT_FOUND = "DIRECTORY_NOT_FOUND";

    /**
     * The repository adapter 'repositoryId' could not be initialized: 'reason'.
     */
    String REPOSITORY_INITIALIZATION_FAILED = "REPOSITORY_INITIALIZATION_FAILED";

    /**
     * The dataGraph could not be loaded: 'reason'
     */
    String LOAD_DATAGRAPH_FAILED = "LOAD_DATAGRAPH_FAILED";

    /**
     * The password match failed.
     */
    String PASSWORD_MATCH_FAILED = "PASSWORD_MATCH_FAILED";

    /**
     * The password check failed for principal name "{0}". Root cause is: "{1}".
     */
    String PASSWORD_CHECKED_FAILED = "PASSWORD_CHECKED_FAILED";

    /**
     * The password match failed for principal name {0}.
     */
    String PASSWORD_MATCH_FAILED_FOR_PRINCIPALNAME = "PASSWORD_MATCH_FAILED_FOR_PRINCIPALNAME";

    /**
     * Authentication is not supported by repository 'reposId'.
     */
    String AUTHENTICATE_NOT_SUPPORTED = "AUTHENTICATE_NOT_SUPPORTED";

    /**
     * Authentication with certificate is not supported by repository 'reposId'.
     */
    String AUTHENTICATION_WITH_CERT_NOT_SUPPORTED = "AUTHENTICATION_WITH_CERT_NOT_SUPPORTED";

    /**
     * The password is missing or empty.
     */
    String MISSING_OR_EMPTY_PASSWORD = "MISSING_OR_EMPTY_PASSWORD";

    /**
     * The search expression does not follow the supported search expression grammar
     */
    String SEARCH_EXPRESSION_ERROR = "SEARCH_EXPRESSION_ERROR";

    /**
     * Cannot search by a composite property
     */
    String SEARCH_BY_COMPOSITE_PROPERTY_NOT_SUPPORTED = "SEARCH_BY_COMPOSITE_PROPERTY_NOT_SUPPORTED";

    /**
     * Cannot search by a property type is "Object"
     */
    String SEARCH_BY_LOB_PROPERTY_NOT_SUPPORTED = "SEARCH_BY_LOB_PROPERTY_NOT_SUPPORTED";

    /**
     * The data type of the property is invalid.
     */
    String PROPERTY_INVALID_DATA_TYPE = "PROPERTY_INVALID_DATA_TYPE";

    /**
     * The specified property definition "{0}" with value "{1}" for property "{2}" is invalid.
     *
     */
    String INVALID_PROPERTY_DEFINITION = "INVALID_PROPERTY_DEFINITION";

    /**
     * The prefix "prefix" of name space "name_space" is duplicated with existing prefix.
     */
    String DUPLICATE_NS_PREFIX = "DUPLICATE_NS_PREFIX";

    /**
     * The name space URI "name_space" is duplicate.
     */
    String DUPLICATE_NS_URI = "DUPLICATE_NS_URI";

    /**
     * The name space URI can not be null or empty.
     */
    String INVALID_NS_URI = "INVALID_NS_URI";

    /**
     * The value of the property is invalid.
     */
    String INVALID_PROPERTY_VALUE = "INVALID_PROPERTY_VALUE";

    /**
     *
     */
    String EXT_ID_HAS_MULTIPLE_VALUES = "EXT_ID_HAS_MULTIPLE_VALUES";

    /**
     * The LDAP attribute used as an external identifier 'extid_attribute'
     * contained a null value for entity 'entity_dn'.
     */
    String EXT_ID_VALUE_IS_NULL = "EXT_ID_VALUE_IS_NULL";

    /**
     * Duplicate entries are found from the external identifier 'extId' in repository 'repos_id'.
     */
    String DUPLICATE_EXTTERNAL_ID = "DUPLICATE_EXTTERNAL_ID";

    /**
     * External name '{0}' is specified, but the external name control is not specified.
     */
    String EXTERNAL_NAME_CONTROL_NOT_FOUND = "EXTERNAL_NAME_CONTROL_NOT_FOUND";

    /**
     * Cannot construct uniqueName when create an entity
     */
    String CAN_NOT_CONSTRUCT_UNIQUE_NAME = "CAN_NOT_CONSTRUCT_UNIQUE_NAME";

    /**
     * Invalid value of property 'level', {0}, specified in {1}.
     */
    String INVALID_LEVEL_IN_CONTROL = "INVALID_LEVEL_IN_CONTROL";

    /**
     * The specified entity 'name' is not a group.
     */
    String ENTITY_IS_NOT_A_GROUP = "ENTITY_IS_NOT_A_GROUP";

    /**
     * virtual member manager Plug-in Manager being initialized.
     */
    public final static String PLUGIN_MANAGER_INITIALIZED = "PLUGIN_MANAGER_INITIALIZED";

    /**
     * virtual member manager Plug-in Manager is successfully loaded.
     */
    public final static String PLUGIN_MANAGER_SUCCESSFULLY_LOADED = "PLUGIN_MANAGER_SUCCESSFULLY_LOADED";

    /**
     * virtual member manager Plug-in Manager failure occurred while loading ''{0}''. Please check configuration file.
     */
    public final static String PLUGIN_MANAGER_SUBSCRIBER_LOAD_FAILURE = "PLUGIN_MANAGER_SUBSCRIBER_LOAD_FAILURE";

    /**
     * virtual member manager Plug-in Manager successfully loaded plug-in ''{0}''.
     */
    public final static String PLUGIN_MANAGER_SUBSCRIBER_LOAD_SUCCESS = "PLUGIN_MANAGER_SUBSCRIBER_LOAD_SUCCESS";

    /**
     * Classname: subscriber_classname not found for Topic-Subscriber subscriber_name
     */
    public final static String PLUGIN_MANAGER_SUBSCRIBER_NOT_FOUND_ERROR = "PLUGIN_MANAGER_SUBSCRIBER_NOT_FOUND_ERROR";

    /**
     * Duplicate definitions of InlineExit inlineexit_name defined.
     */
    public final static String PLUGIN_MANAGER_MULTI_INLINE_DUPLICATE_NAME_ERROR = "PLUGIN_MANAGER_MULTI_INLINE_DUPLICATE_NAME_ERROR";

    /**
     * Duplicate definitions of Topic-Emitters topicemitter_name defined.
     */
    public final static String PLUGIN_MANAGER_MULTI_TOPIC_EMITTER_DUPLICATE_NAME_ERROR = "PLUGIN_MANAGER_MULTI_TOPIC_EMITTER_DUPLICATE_NAME_ERROR";

    /**
     * exitpoint_name point of Topic-Emitter topicemitter_name has a noncompliant Topic-Subscriber topicsubscriber_name of SubscriberType subscribertype_name stored in the the
     * wrong subscriber list.
     */
    public final static String PLUGIN_MANAGER_INVALID_SUBSCRIBER_TYPE_ERROR = "PLUGIN_MANAGER_INVALID_SUBSCRIBER_TYPE_ERROR";

    /**
     * exitpoint_name point of Topic-Emitter topicemitter_name has an invalid topicsubscriber_name subscriber reference of subscribertype_name.
     */
    public final static String PLUGIN_MANAGER_INVALID_SUBSCRIBER_REF_ERROR = "PLUGIN_MANAGER_INVALID_SUBSCRIBER_REF_ERROR";

    /**
     * Topic-Emitter topicemitter_name is missing in the configuration file.
     */
    public final static String PLUGIN_MANAGER_TOPIC_EMITTER_MISSING_ERROR = "PLUGIN_MANAGER_TOPIC_EMITTER_MISSING_ERROR";

    /**
     * Critical exception has occurred inside a subscriber of plugin: {0}
     */
    public final static String SUBSCRIBER_CRITICAL_EXCEPTION = "SUBSCRIBER_CRITICAL_EXCEPTION";

    /**
     * Starting bootstrap sequence for the dynamic reload manager.
     */
    public final static String DYNAMIC_RELOAD_START_BOOTSTRAP = "DYNAMIC_RELOAD_START_BOOTSTRAP";

    /**
     * Waiting for notification that the server has finished starting...
     */
    public final static String DYNAMIC_RELOAD_WAIT_NOTIF_SERVER_STARTED = "DYNAMIC_RELOAD_WAIT_NOTIF_SERVER_STARTED";

    /**
     * Received notification that the server has finished starting
     */
    public final static String DYNAMIC_RELOAD_RECEIVED_NOTIF_SERVER_STARTED = "DYNAMIC_RELOAD_RECEIVED_NOTIF_SERVER_STARTED";

    /**
     * Initialization of the dynamic reload manager completed successfully.
     */
    public final static String DYNAMIC_RELOAD_INIT_SUCCESS = "DYNAMIC_RELOAD_INIT_SUCCESS";

    /**
     * Initialization of the dynamic reload manager failed.
     */
    public final static String DYNAMIC_RELOAD_INIT_FAILURE = "DYNAMIC_RELOAD_INIT_FAILURE";

    /**
     * An error occured while broadcasting an event from the deployment manager to managed nodes.
     */
    public final static String DYNAMIC_RELOAD_EVENT_BROADCAST_ERROR = "DYNAMIC_RELOAD_EVENT_BROADCAST_ERROR";

    /**
     * Managed node {0} is either unavailable or failed to process event (1).
     */
    public final static String DYNAMIC_RELOAD_MANAGED_NODE_UNAVAILABLE = "DYNAMIC_RELOAD_MANAGED_NODE_UNAVAILABLE";

    /**
     * Broadcasting event {0} to managed nodes.
     */
    public final static String DYNAMIC_RELOAD_DMGR_BROADCAST_EVENT = "DYNAMIC_RELOAD_DMGR_BROADCAST_EVENT";

    /**
     * Received event {0} from the deployment manager.
     */
    public final static String DYNAMIC_RELOAD_MANAGED_NODE_RECEIVED_EVENT = "DYNAMIC_RELOAD_MANAGED_NODE_RECEIVED_EVENT";

    /**
     * All updates must be performed at the deployment mananger and not at a managed node.
     */
    public final static String DYNAMIC_RELOAD_INVALID_UPDATE_AT_MANAGED_NODE = "DYNAMIC_RELOAD_INVALID_UPDATE_AT_MANAGED_NODE";

    /**
     * An error occurred while broadcasting an event from the admin agent to base profile.
     */
    public final static String DYNAMIC_RELOAD_EVENT_BROADCAST_ERROR_TO_PROFILE = "DYNAMIC_RELOAD_EVENT_BROADCAST_ERROR_TO_PROFILE";

    /**
     * Base profile {0} is either unavailable or failed to process event {1}.
     */
    public final static String DYNAMIC_RELOAD_REGISTERED_PROFILE_UNAVAILABLE = "DYNAMIC_RELOAD_REGISTERED_PROFILE_UNAVAILABLE";

    /**
     * Broadcasting event {0} to base profile {1}.
     */
    public final static String DYNAMIC_RELOAD_AA_BROADCAST_EVENT_TO_PROFILE = "DYNAMIC_RELOAD_AA_BROADCAST_EVENT_TO_PROFILE";

    /**
     * Received event {0} from the admin agent.
     */
    public final static String DYNAMIC_RELOAD_REGISTERED_PROFILE_RECEIVED_EVENT = "DYNAMIC_RELOAD_REGISTERED_PROFILE_RECEIVED_EVENT";

    /**
     * Requested update can not be performed at the admin agent.
     */
    public final static String DYNAMIC_RELOAD_INVALID_UPDATE_AT_ADMIN_AGENT = "DYNAMIC_RELOAD_INVALID_UPDATE_AT_ADMIN_AGENT";

    /**
     * Initialization of the MBean, {0}, completed successfully
     */
    public final static String MBEAN_INIT_SUCCESS = "MBEAN_INIT_SUCCESS";

    /**
     * Deactivation of the MBean, {0}, completed successfully
     */
    public final static String MBEAN_DEACTIVATION_SUCCESS = "MBEAN_DEACTIVATION_SUCCESS";

    /**
     * Initialization of the MBean, {0}, failed: {1}
     */
    public final static String MBEAN_INIT_FAILURE = "MBEAN_INIT_FAILURE";

    /**
     * Failed to get or call the MBean, {0}: {1}
     */
    public final static String MBEAN_GET_CALL_FAILURE = "MBEAN_GET_CALL_FAILURE";

    public static final String INVALID_BASE_ENTRY_DEFINITION = "INVALID_BASE_ENTRY_DEFINITION";

    static final String INVALID_REALM_DEFINITION = "INVALID_REALM_DEFINITION";

    static final String INVALID_PARTICIPATING_BASE_ENTRY_DEFINITION = "INVALID_PARTICIPATING_BASE_ENTRY_DEFINITION";

    static final String INVALID_UR_ATTRIBUTE_MAPPING_DEFINITION = "INVALID_UR_ATTRIBUTE_MAPPING_DEFINITION";

    static final String MISSING_BASE_ENTRY_IN_REALM = "MISSING_BASE_ENTRY_IN_REALM";

    static final String TBS_CERTIFICATE_UNSUPPORTED = "TBS_CERTIFICATE_UNSUPPORTED";

    static final String UNKNOWN_CERTIFICATE_ATTRIBUTE = "UNKNOWN_CERTIFICATE_ATTRIBUTE";

    /**
     * The repository Id {0} specified is invalid.
     */
    String INVALID_REPOSITORY_ID = "INVALID_REPOSITORY_ID";

    /**
     * The principal name is missing or empty.
     */
    String MISSING_OR_EMPTY_PRINCIPAL_NAME = "MISSING_OR_EMPTY_PRINCIPAL_NAME";

    /**
     * No principal is found from the given principal name {0}.
     */
    String PRINCIPAL_NOT_FOUND = "PRINCIPAL_NOT_FOUND";

    /**
     * Multiple principals are found from the given principal name {0}.
     */
    String MULTIPLE_PRINCIPALS_FOUND = "MULTIPLE_PRINCIPALS_FOUND";

    /**
     * A search pattern 'pattern' is not valid.
     */
    String INVALID_SEARCH_PATTERN = "INVALID_SEARCH_PATTERN";

    /**
     * The database type 'dbType' is not supported.
     */
    String DB_TYPE_NOT_SUPPORTED = "DB_TYPE_NOT_SUPPORTED";

    /**
     * The entity type 'entity_type' in name space URI 'nsURI' is already defined.
     */
    String ENTITY_TYPE_ALREADY_DEFINED = "ENTITY_TYPE_ALREADY_DEFINED";

    /**
     * The property type 'propType' is already defined for entity type 'entityType."
     */
    String PROPERTY_TYPE_ALREADY_DEFINED = "PROPERTY_TYPE_ALREADY_DEFINED";

    /**
     * The LDAP entry 'dn' for the entity 'unique_name' is not found:
     */
    String LDAP_ENTRY_NOT_FOUND = "LDAP_ENTRY_NOT_FOUND";

    /**
     * The specified search expression 'search_expr" is invalid.
     */
    String INVALID_SEARCH_EXPRESSION = "INVALID_SEARCH_EXPRESSION";

    /**
     * Duplicate entity type 'entity_type' is defined in virtual member manager configuration XML file.
     */
    String DUPLICATE_ENTITY_TYPE = "DUPLICATE_ENTITY_TYPE";

    /**
     * The initial context pool size should be less the maximum context pool size.
     */
    String INIT_POOL_SIZE_TOO_BIG = "INIT_POOL_SIZE_TOO_BIG";

    /**
     * The preferred context pool size should be less the maximum context pool size.
     */
    String PREF_POOL_SIZE_TOO_BIG = "PREF_POOL_SIZE_TOO_BIG";

    /**
     * WebSphere variable, 'variable name', was not resolved.
     */
    String WAS_VARIABLE_NOT_RESOLVED = "WAS_VARIABLE_NOT_RESOLVED";

    /**
     * The reference type 'ref_type' is not found in the schema.
     */
    String REFERENCE_TYPE_NOT_FOUND = "REFERENCE_TYPE_NOT_FOUND";

    /**
     * Can not create multiple entity types in one call.
     */
    String CAN_NOT_CREATE_MULTIPLE_ENTITY_TYPES = "CAN_NOT_CREATE_MULTIPLE_ENTITY_TYPES";

    /**
     * Can not create multiple property types in one call.
     */
    String CAN_NOT_CREATE_MULTIPLE_PROPERTY_TYPES = "CAN_NOT_CREATE_MULTIPLE_PROPERTY_TYPES";

    /**
     * Can not create new entity type and new property type in one API call.
     */
    String CAN_NOT_CREATE_BOTH_ENTITY_AND_PROPERTY = "CAN_NOT_CREATE_BOTH_ENTITY_AND_PROPERTY";

    /**
     * The entity has two parameters that need to match but do not.
     */
    String PARAMS_DO_NOT_MATCH = "PARAMS_DO_NOT_MATCH";

    /**
     * No searchable attributes were specified in a search request.
     */
    String SEARCH_ATTR_NOT_SPECIFIED = "SEARCH_ATTR_NOT_SPECIFIED";

    /**
     * Too many attributes were specified in a search request.
     */
    String SEARCH_PARAMETERS_OVER_SPECIFIED = "SEARCH_PARAMETERS_OVER_SPECIFIED";

    /**
     * The entity with uniqueName '{0}' is not of type '{1}'
     */
    String INCORRECT_ENTITY_TYPE = "INCORRECT_ENTITY_TYPE";

    /**
     * The specified uniqueId property '{0}' of the parent is not valid.
     */
    String INVALID_PARENT_UNIQUE_ID = "INVALID_PARENT_UNIQUE_ID";

    /**
     * The format of value of the property '{0}' is invalid.
     */
    String INVALID_PROPERTY_VALUE_FORMAT = "INVALID_PROPERTY_VALUE_FORMAT";

    /**
     * The value '{0}' of 'countLimit' parameter in the SearchControl DataObject is greater than the value '{1}' of the 'searchLimit' parameter.
     */
    String CANNOT_SPECIFY_COUNT_LIMIT = "CANNOT_SPECIFY_COUNT_LIMIT";

    /**
     * The schema package with name space URI '{0}' is not found.
     */
    String SCHEMA_PACKAGE_NOT_FOUND = "SCHEMA_PACKAGE_NOT_FOUND";

    /**
     * Cannot create or update operational property '{0}'.
     */
    String CANNOT_SPECIFIED_OPERATIONAL_PROPERTY_VALUE = "CANNOT_SPECIFIED_OPERATIONAL_PROPERTY_VALUE";

    /**
     * The {0} repository ID is reserved.
     */
    String REPOSITORY_ID_RESERVED = "REPOSITORY_ID_RESERVED";

    /**
     * The {0} repository ID already exists.
     */
    String REPOSITORY_ID_ALREADY_EXISTS = "REPOSITORY_ID_ALREADY_EXISTS";

    /**
     * The {0} realm already exists.
     */
    String REALM_ALREADY_EXISTS = "REALM_ALREADY_EXISTS";

    /**
     * Supported entity type {0} already exists.
     */
    String SUPPORTED_ENTITY_TYPE_ALREADY_EXISTS = "SUPPORTED_ENTITY_TYPE_ALREADY_EXISTS";

    /**
     * RDN attribute {0} already exists.
     */
    String RDN_ATTR_ALREADY_EXISTS = "RDN_ATTR_ALREADY_EXISTS";

    /**
     * LDAP server with {0} primary host already exists.
     */
    String PRIMARY_HOST_ALREADY_EXISTS = "PRIMARY_HOST_ALREADY_EXISTS";

    /**
     * LDAP server with {0} backup host and {1} port already exists.
     */
    String BACKUP_HOST_PORT_ALREADY_EXISTS = "BACKUP_HOST_PORT_ALREADY_EXISTS";

    /**
     * LDAP entity type {0} already exists.
     */
    String LDAP_ENTITY_TYPE_ALREADY_EXISTS = "LDAP_ENTITY_TYPE_ALREADY_EXISTS";

    /**
     * LDAP dynamic group member attribute {0} already exists.
     */
    String DYMANIC_GROUP_MEMBER_ATTR_ALREADY_EXISTS = "DYMANIC_GROUP_MEMBER_ATTR_ALREADY_EXISTS";

    /**
     * LDAP group member attribute {0} already exists.
     */
    String GROUP_MEMBER_ATTR_ALREADY_EXISTS = "GROUP_MEMBER_ATTR_ALREADY_EXISTS";

    /**
     * LDAP group member attribute mapping for {0} already exists.
     */
    String GROUP_MEMBER_ATTR_FOR_OBJECTCLASS_EXISTS = "GROUP_MEMBER_ATTR_FOR_OBJECTCLASS_EXISTS";
    /**
     * LDAP server with {0} primary host is not found.
     */
    String INVALID_PRIMARY_HOST = "INVALID_PRIMARY_HOST";

    /**
     * LDAP server with {0} backup host and {1} port is not found.
     */
    String INVALID_BACKUP_HOST_PORT = "INVALID_BACKUP_HOST_PORT";

    /**
     * Supported entity type {0} is not found.
     */
    String INVALID_SUPPORTED_ENTITY_TYPE = "INVALID_SUPPORTED_ENTITY_TYPE";

    String INVALID_SUPPORTED_ENTITY_TYPE_DEFINITION = "INVALID_SUPPORTED_ENTITY_TYPE_DEFINITION";

    /**
     * LDAP dynamic group member attribute {0} is not found.
     */
    String INVALID_DYNAMIC_GROUP_MEMBER_ATTR = "INVALID_DYNAMIC_GROUP_MEMBER_ATTR";

    /**
     * LDAP group member attribute {0} is not found.
     */
    String INVALID_GROUP_MEMBER_ATTR = "INVALID_GROUP_MEMBER_ATTR";

    /**
     * LDAP RDN Attribute {0} is not found.
     */
    String INVALID_RDN_ATTR = "INVALID_RDN_ATTR";

    /**
     * LDAP entity type {0} is not found.
     */
    String INVALID_LDAP_ENTITY_TYPE = "INVALID_LDAP_ENTITY_TYPE";

    /**
     * Login properties are not valid: {0}
     **/
    String INVALID_LOGIN_PROPERTIES = "INVALID_LOGIN_PROPERTIES";

    /**
     * Search filter is not valid: {0}
     **/
    String INVALID_SEARCH_FILTER = "INVALID_SEARCH_FILTER";

    /**
     * Search base is not valid: {0}
     **/
    String INVALID_SEARCH_BASE = "INVALID_SEARCH_BASE";

    /**
     * Search base is not valid: {0}
     **/
    String NON_EXISTING_SEARCH_BASE = "NON_EXISTING_SEARCH_BASE";

    /**
     * Object class(es) is not valid: {0}
     **/
    String INVALID_OBJECT_CLASSES = "INVALID_OBJECT_CLASSES";

    /**
     * Distinguished name [{0}] for base entry in the repository is not valid
     **/
    String INVALID_BASE_ENTRY_NAME_IN_REPOSITORY = "INVALID_BASE_ENTRY_NAME_IN_REPOSITORY";

    /**
     * The value for supportChangeLog parameter is invalid
     */
    String INVALID_SUPPORT_CHANGE_LOG = "INVALID_SUPPORT_CHANGE_LOG";

    /**
     * One or more of the required parameters are not specified. Required parameters are {0}.
     */
    String REQUIRED_PARAMETERS_NOT_SPECIFIED = "REQUIRED_PARAMETERS_NOT_SPECIFIED";

    /**
     * The {0} nonprofile repository, cannot be updated.
     */
    String CONFIG_NON_PROFILE_REPO_CANNOT_BE_UPDATED = "CONFIG_NON_PROFILE_REPO_CANNOT_BE_UPDATED";

    /**
     * A duplicate value is not allowed for parameter '{0}'
     */
    String CONFIG_GROUP_SCOPE_CANNOT_BE_SET = "CONFIG_GROUP_SCOPE_CANNOT_BE_SET";

    /**
     * The parameter value '{0}' is not correct for the parameter '{1}'.
     * The parameter must have one of the following values: '{2}'.
     */
    String CONFIG_VALUE_NOT_VALID = "CONFIG_VALUE_NOT_VALID";

    /**
     * Search 'principalName' with other properties is not supported.
     */
    String CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS = "CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS";

    /**
     * Updating 'propertyName' is not supported when ChangeSummary is on.
     */
    String UPDATE_PROPERTY_NOT_SUPPORTED_WITH_CHANGESUMMARY = "UPDATE_PROPERTY_NOT_SUPPORTED_WITH_CHANGESUMMARY";

    /**
     * Incorrect count limit '{0}' is specified in the SearchControl data object.
     */
    String INCORRECT_COUNT_LIMIT = "INCORRECT_COUNT_LIMIT";

    /**
     * The syntax of the certificate filter '{0}' is invalid. The correct syntax is: LDAP attribute=${Client certificate attribute} (for example, uid=${SubjectCN}).
     */
    String INVALID_CERTIFICATE_FILTER = "INVALID_CERTIFICATE_FILTER";

    /**
     * The DN field '{0}' is unknown.
     */
    String UNKNOWN_DN_FIELD = "UNKNOWN_DN_FIELD";

    /**
     * Async adapter can not be used in conjunction with Property Extension database or other synchronous/asynchronous adapters.
     */
    String ASYNC_CALL_WITH_MULTIPLE_REPOSITORIES_NOT_SUPPORTED = "ASYNC_CALL_WITH_MULTIPLE_REPOSITORIES_NOT_SUPPORTED";

    /**
     * Invalid value specified for parameter '{0}'
     */
    String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";

    /**
     * Invalid value specified for parameter '{0}'. Warning
     */
    String INVALID_PARAM_VALUE_WARN = "INVALID_PARAM_VALUE_WARN";

    /**
     * The repository '{0}' is a read-only. It does not support a "write" operation.
     */
    String CANNOT_WRITE_TO_READ_ONLY_REPOSITORY = "CANNOT_WRITE_TO_READ_ONLY_REPOSITORY";

    /**
     * The initialization property '{0}' is missing from configuration.
     */
    String MISSING_INI_PROPERTY = "MISSING_INI_PROPERTY";

    /**
     * Could not register as WebSphere Application Server adminService notification listener.
     */
    String ADMIN_SERVICE_REGISTRATION_FAILED = "ADMIN_SERVICE_REGISTRATION_FAILED";

    /**
     * Can not create or update property '{0}' in repository '{1}'.
     */
    String CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY = "CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY";

    /**
     * Account {0} is stored in the file registry in temporary workspace. You must use the "$AdminConfig save" command to save it in the main repository.
     **/
    String FILE_REGISTRY_ACCOUNT_ADDED = "FILE_REGISTRY_ACCOUNT_ADDED";

    /**
     * The password is changed for {0} in the file registry in temporary workspace. You must use the "$AdminConfig save" command to save it in the main repository.
     **/
    String FILE_REGISTRY_ACCOUNT_PASSWORD_CHANGED = "FILE_REGISTRY_ACCOUNT_PASSWORD_CHANGED";

    /**
     * The realm {0} cannot be deleted because it is set as the default realm.
     **/
    String CANNOT_DELETE_DEFAULT_REALM = "CANNOT_DELETE_DEFAULT_REALM";

    /**
     * The realm {0} cannot be deleted because it is the only realm defined. There must always be at least 1 realm defined.
     **/
    String CANNOT_DELETE_ONLY_REALM = "CANNOT_DELETE_ONLY_REALM";

    /**
     * The base entry {0} could not be added to the realm {1} because a repository must
     * reference the base entry before it is added.
     **/
    String BASE_ENTRY_CANNOT_BE_ADDED_TO_REALM = "BASE_ENTRY_CANNOT_BE_ADDED_TO_REALM";

    /**
     * The base entry {0} could not be added to the realm {1} because
     * the base entry is already defined in a realm.
     **/
    String DUPLICATE_BASE_ENTRY_IN_REALM = "DUPLICATE_BASE_ENTRY_IN_REALM";

    /**
     * The base entry {0} could not be deleted because it is the last base entry in the realm {1}
     **/
    String CANNOT_DELETE_ONLY_BASE_ENTRY_IN_REALM = "CANNOT_DELETE_ONLY_BASE_ENTRY_IN_REALM";

    /**
     * The base entry {0} could not be added to the repository {1} because it is already defined in this or other repository.
     **/
    String BASE_ENTRY_ALREADY_IN_REPOSITORY = "BASE_ENTRY_ALREADY_IN_REPOSITORY";

    /**
     * The base entry {0} could not be deleted from the repository {1} because it
     * is the last base entry defined in the repository.
     **/
    String CANNOT_DELETE_ONLY_BASE_ENTRY_IN_REPOSITORY = "CANNOT_DELETE_ONLY_BASE_ENTRY_IN_REPOSITORY";

    /**
     * The base entry {0} could not be deleted from the repository because it is referenced by a realm.
     **/
    String BASE_ENTRY_STILL_REFERENCED_BY_REALM = "BASE_ENTRY_STILL_REFERENCED_BY_REALM";

    /**
     * The repository {0} could not be deleted because it has at least one base entry that is referenced by a realm.
     **/
    String DELETE_REPOSITORY_PREREQUISITE_ERROR = "DELETE_REPOSITORY_PREREQUISITE_ERROR";

    /**
     * The file {0} could not be found.
     **/
    String FILE_NOT_FOUND = "FILE_NOT_FOUND";

    /**
     * Adapter class name is missing or is not valid: {0}
     **/
    String MISSING_OR_INVALID_ADAPTER_CLASS_NAME = "MISSING_OR_INVALID_ADAPTER_CLASS_NAME";

    /**
     * Could not connect to the {0} repository using properties: {1}
     **/
    String REPOSITORY_CONNECTION_FAILED = "REPOSITORY_CONNECTION_FAILED";

    /**
     * Could not connect to {0} repository because connection data is not valid or is insufficient.
     **/
    String MISSING_OR_INVALID_CONNECTION_DATA = "MISSING_OR_INVALID_CONNECTION_DATA";

    /**
     * LDAP server is not configured for the repository {0}.
     */
    String MISSING_LDAP_SERVER_CONFIGURATION = "MISSING_LDAP_SERVER_CONFIGURATION";

    /**
     * LDAP group is not configured for the repository {0}.
     */
    String MISSING_LDAP_GROUP_CONFIGURATION = "MISSING_LDAP_GROUP_CONFIGURATION";

    /**
     * Realm configuration does not exist.
     */
    String MISSING_REALM_CONFIGURATION = "MISSING_REALM_CONFIGURATION";

    /**
     * Base entry {0} is not found in the {1} realm.
     **/
    String BASE_ENTRY_NOT_FOUND_IN_REALM = "BASE_ENTRY_NOT_FOUND_IN_REALM";

    /**
     * Base entry {0} is not found in the repository {1}
     **/
    String BASE_ENTRY_NOT_FOUND_IN_REPOSITORY = "BASE_ENTRY_NOT_FOUND_IN_REPOSITORY";

    /**
     * The configuration is not complete. Saving an incomplete configuration can cause startup problems.
     */
    String CONFIG_NOT_COMPLETE = "CONFIG_NOT_COMPLETE";

    /**
     * The configuration is saved in a temporary workspace. You must use the "$AdminConfig save" command to save it in the main repository.
     */
    String CONFIG_SAVED_IN_WORKSPACE = "CONFIG_SAVED_IN_WORKSPACE";

    /**
     * Each configured repository must contain at least one base entry. Please add a base entry before saving the configuration.
     */
    String MUST_ADD_BASE_ENTRY_TO_REPOSITORY = "MUST_ADD_BASE_ENTRY_TO_REPOSITORY";

    /**
     * Each configured repository must contain at least one base entry. Please add a base entry.
     */
    String MISSING_BASE_ENTRY = "MISSING_BASE_ENTRY";

    /**
     * Each configured realm must contain at least one participating base entry. Please add a base entry before saving the configuration.
     */
    String MUST_ADD_BASE_ENTRY_TO_REALM = "MUST_ADD_BASE_ENTRY_TO_REALM";

    /**
     * Each configured supported entity type must have at least one RDN property. Please add a valid RDN property before saving the configuration.
     */
    String MUST_ADD_RDN_PROP_TO_SUPPORTED_ENTITY_TYPE = "MUST_ADD_RDN_PROP_TO_SUPPORTED_ENTITY_TYPE";

    /**
     * Base entry specified is invalid: {0}. It must be a distinguished name.
     */
    String BASE_ENTRY_MUST_BE_DN = "BASE_ENTRY_MUST_BE_DN";

    /**
     * One or more of the related parameters are not specified. {0} is specified but {1} is not.
     */
    String RELATED_PARAMETERS_NOT_SPECIFIED = "RELATED_PARAMETERS_NOT_SPECIFIED";

    /**
     * The property {0} was not properly specified for setupIdMgrDB.
     **/
    String DB_SETUP_PROPERTY_MISSING = "DB_SETUP_PROPERTY_MISSING";

    /**
     * Command completed successfully.
     **/
    String COMMAND_COMPLETED_SUCCESSFULLY = "COMMAND_COMPLETED_SUCCESSFULLY";

    /**
     * Initialization of the authorization component completed successfully.
     **/
    String AUTH_INIT_SUCCESS = "AUTH_INIT_SUCCESS";

    /**
     * Initialization of the authorization component failed.
     **/
    String AUTH_INIT_FAILURE = "AUTH_INIT_FAILURE";

    /**
     * The {0} attribute may not be assigned to multiple groups.
     **/
    String AUTH_ATTR_MULTIPLE_GROUP = "AUTH_ATTR_MULTIPLE_GROUP";

    /**
     * An error occurred while performing an access verification for the {0} principal.
     **/
    String AUTH_CHECK_FAILURE = "AUTH_CHECK_FAILURE";

    /**
     * An unexpected error occurred while retrieving the caller's subject.
     **/
    String AUTH_SUBJECT_FAILURE = "AUTH_SUBJECT_FAILURE";

    /**
     * An unexpected error occurred while retrieving the subject's credentials.
     **/
    String AUTH_SUBJECT_CRED_FAILURE = "AUTH_SUBJECT_CRED_FAILURE";

    /**
     * The delegated administration view plug-in is either
     * missing or failed to return a value for the {0} entity.
     **/
    String AUTH_VIEW_PLUGIN_FAILURE = "AUTH_VIEW_PLUGIN_FAILURE";

    /**
     * The principal {0} is not authorized to perform the operation\n\t
     * {1} on {2}
     **/
    String AUTH_ACCESS_FAILURE = "AUTH_ACCESS_FAILURE";

    /**
     * The principal {0} does not have the role {1} required for the operation\n\t
     * {2}
     **/
    String AUTH_ACCESS_ROLE_REQUIRED = "AUTH_ACCESS_ROLE_REQUIRED";

    /**
     * An unexpected error occurred while looking up the {0} attribute
     * for the {1} entity to evaluate an access rule.
     **/
    String AUTH_RULE_ATTR_FAILURE = "AUTH_RULE_ATTR_FAILURE";

    /**
     * The {0} attribute for the {1} entity was not found to evaluate an access rule.
     **/
    String AUTH_RULE_ATTR_MISSING = "AUTH_RULE_ATTR_MISSING";

    /**
     * The dynamicUpdateConfig method of the ServiceProvider does not support an event with the type '{0}'.
     */
    String DYNA_UPDATE_CONFIG_EVENT_NOT_SUPPORT = "DYNA_UPDATE_CONFIG_EVENT_NOT_SUPPORT";

    /**
     * Cannot add a new realm through the dynamicUpdateConfig method of the ServiceProvider when there is no realm configured in vitural member manager.
     */
    String DYNA_UPDATE_CONFIG_ADD_REALM_WITHOUT_REALM_CONFIG = "DYNA_UPDATE_CONFIG_ADD_REALM_WITHOUT_REALM_CONFIG";

    /**
     * The DataObject type {0} for event type {1} is incorrect.
     */
    String DYNA_UPDATE_CONFIG_WRONG_DATA_OBJECT_TYPE = "DYNA_UPDATE_CONFIG_WRONG_DATA_OBJECT_TYPE";

    /**
     * The value for the dynamic configuration key {0} of event type {1} is missing.
     */
    String DYNA_UPDATE_CONFIG_MISSING_VALUE = "DYNA_UPDATE_CONFIG_MISSING_VALUE";

    /**
     * The default parent for entity type {0} in realm {1} already defined.
     */
    String DEFAULT_PARENT_ALREADY_DEFINED = "DEFAULT_PARENT_ALREADY_DEFINED";

    /**
     * Property extension repository has been already defined.
     */
    String PROPERTY_EXTENSION_REPOSITORY_ALREADY_DEFINED = "PROPERTY_EXTENSION_REPOSITORY_ALREADY_DEFINED";

    /**
     * The specified repository id {0} is not a LDAP repository.
     */
    String NOT_LDAP_REPOSITORY = "NOT_LDAP_REPOSITORY";

    /**
     * The dynamic configuration parameter {0} is missing.
     */
    String MISSING_DYNA_CONFIG_PARAMETER = "MISSING_DYNA_CONFIG_PARAMETER";

    /**
     * The write operations are not allowed on secondary LDAP server {0}.
     */
    String WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED = "WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED";

    /**
     * Entity DataObject is missing for operation {0}.
     */
    String MISSING_ENTITY_DATA_OBJECT = "MISSING_ENTITY_DATA_OBJECT";

    /**
     * The {0} search limit specified in the SearchControl data object is invalid.
     */
    String INCORRECT_SEARCH_LIMIT = "INCORRECT_SEARCH_LIMIT";

    /**
     * No database type was specified to be setup. Check DB, FED, and LA parameters.
     */
    String DB_SETUP_NO_DB_SPECIFIED = "DB_SETUP_NO_DB_SPECIFIED";

    /**
     * The {0} repository ID is not valid as repository for groups.
     */
    String INVALID_REPOSITORY_FOR_GROUPS = "INVALID_REPOSITORY_FOR_GROUPS";

    /**
     * The required parameter {0} is missing from realm {1}.
     */
    String MISSING_REALM_RELATED_PARAMETER = "MISSING_REALM_RELATED_PARAMETER";

    /**
     * The passed in DataObject type {0} is not valid for operation {1}. The correct DataObject type is {2}.
     */
    String INVALID_DATA_OBJECT_TYPE = "INVALID_DATA_OBJECT_TYPE";

    /**
     * Repository '{0}' is not a database repository and can not contain entities from other repository.
     */
    String CANNOT_STORE_ENTITY_FROM_OTHER_REPOSITORY = "CANNOT_STORE_ENTITY_FROM_OTHER_REPOSITORY";

    /**
     * Entity mapping repository is not defined in virtual member manager configuration.
     */
    String ENTRY_MAPPING_REPOSITORY_NOT_DEFINED = "ENTRY_MAPPING_REPOSITORY_NOT_DEFINED";

    /**
     * Property extension repository is not defined in virtual member manager configuration.
     */
    String PROPERTY_EXTENSION_REPOSITORY_NOT_DEFINED = "PROPERTY_EXTENSION_REPOSITORY_NOT_DEFINED";

    /**
     * The database migration task : {0} loading failed.
     */
    String MIGRATION_DATABASE_LOADING = "MIGRATION_DATABASE_LOADING";

    /**
     * At least one of the config path parameters for migrating wmm configuration task is missing.
     */
    String MIGRATION_CONFIG_PATH_MISSING = "MIGRATION_CONFIG_PATH_MISSING";

    /**
     * Database connection failed with database URL {0}.
     */
    String DATABASE_CONNECTION = "DATABASE_CONNECTION";

    /**
     * The {0} file is invalid.
     */
    String MIGRATION_WMM_FILE_INVALID = "MIGRATION_WMM_FILE_INVALID";

    /**
     * Migrating WMM failed due to: {0}.
     */
    String MIGRATION_ERROR = "MIGRATION_ERROR";

    /**
     * Command completed successfully. Invoke updateWMMReference command with reposType={0} to update reference properties.
     */
    String INVOKE_UPDATE_WMM_REFERENCE_COMMAND = "INVOKE_UPDATE_WMM_REFERENCE_COMMAND";

    /**
     * Initialization of component, {component_name}, failed: {reason}
     */
    String COMPONENT_INITIALIZATION_FAILED = "COMPONENT_INITIALIZATION_FAILED";

    /**
     * The user with uniqueName {0} cannot be deleted because it is the logged in user.
     */
    String CANNOT_DELETE_LOGGED_IN_USER = "CANNOT_DELETE_LOGGED_IN_USER";

    /**
     * To manage users and groups, either federated repositories must be the current realm definition or the
     * current realm definition configuration must match the federated repositories configuration.
     * If you use Lightweight Directory Access Protocol (LDAP), configure both the federated repositories
     * and standalone LDAP registry configurations to use the same LDAP server.
     */
    String WAS_USER_REGISTRY_NOT_SUPPORTED = "WAS_USER_REGISTRY_NOT_SUPPORTED";

    /**
     * WAS Primary Admin user Can Not be deleted
     */

    String CANNOT_DELETE_PRIMARY_ADMIN = "CANNOT_DELETE_PRIMARY_ADMIN";

    /**
     * can not find attribute mapping.
     */
    String ATTRIBUTE_MAPPING_NOT_DEFINED = "ATTRIBUTE_MAPPING_NOT_DEFINED";

    /**
     * there are more than one attribute configurations defined for {0}, please specify the entity type.
     */
    String MORE_THAN_ONE_ATTRIBUTE_MAPPING = "MORE_THAN_ONE_ATTRIBUTE_MAPPING";

    /**
     * attribute mapping for ''{0}'' already exist.
     */
    String ATTRIBUTE_MAPPING_ALREADY_EXIST = "ATTRIBUTE_MAPPING_ALREADY_EXIST";

    /**
     * The passed in DataObject is null.
     */
    String INVALID_DATA_OBJECT = "INVALID_DATA_OBJECT";

    /**
     * The wmmnode ''{0}'' failed to be migrated to virtual member manager realm.
     */
    String BASE_ENTRY_MIGRATION_WARNING = "BASE_ENTRY_MIGRATION_WARNING";
    /**
     * repositoriesForGroups is not configured for the repository {0}.
     */
    String MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION = "MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION";

    /**
     * Could not retrieve information associated with changelog in TDS repository
     */
    String ERROR_IN_CHANGELOG_CONFIGURATION = "ERROR_IN_CHANGELOG_CONFIGURATION";

    /**
     * NULL checkpoint value
     */
    String NULL_CHECKPOINT_VALUE = "NULL_CHECKPOINT_VALUE";

    /**
     * No change handler associated with repository type : {0}
     */
    String NO_ASSOCIATED_CHANGE_HANDLER = "NO_ASSOCIATED_CHANGE_HANDLER";
    /**
     * Invalid ChangeType
     */
    String INVALID_CHANGETYPE = "INVALID_CHANGETYPE";

    /* LI-80054-Start */
    /**
     * Successfully added new property '' {0}'' to the entity ''{1}''
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_SUCCESSFUL = "EXTEND_SCHEMA_PROPERTY_EXTENSION_SUCCESSFUL";

    /**
     * Command failed since a property doesn't exist in Look Aside Repository for the given applicable entity types.
     */
    String EXTENDED_PROPERTY_NOT_DEFINED_FOR_ENTITY_TYPES = "EXTENDED_PROPERTY_NOT_DEFINED_FOR_ENTITY_TYPES";

    /**
     * Command to add attribute to the entity has failed.
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_FAILED = "EXTEND_SCHEMA_PROPERTY_EXTENSION_FAILED";

    /**
     * Command failed since the Entity type ''{0}'' provided is an invalid entity type.
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_APPLICABLE_ENTITY_TYPE = "EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_APPLICABLE_ENTITY_TYPE";

    /**
     * Command failed since the Required Entity type ''{0}'' provided is an invalid entity type.
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_REQUIRED_ENTITY_TYPE = "EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_REQUIRED_ENTITY_TYPE";

    /**
     * Command failed since the data type of the attribute ''{0}'' provided as ''{1}'' provided is an invalid data type.
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_DATA_TYPE = "EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_DATA_TYPE";

    /**
     * Command failed since the Required Entity type ''{0}'' provided is not in the list of applicable entity types.
     */
    String EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_REQUIRED_NOTIN_APPLICABLE_ENTITY_TYPE = "EXTEND_SCHEMA_PROPERTY_EXTENSION_INVALID_REQUIRED_NOTIN_APPLICABLE_ENTITY_TYPE";

    /**
     * Command failed since The namespace URI for the prefix, {0} is not defined.
     */
    String INVALID_NS_PREFIX = "INVALID_NS_PREFIX";

    /**
     * Command failed since the prefix {0} provided for the entity type is invalid.
     */
    String INVALID_NS_PREFIX_FOR_ENTITY_TYPE_OR_PROPERTY = "INVALID_NS_PREFIX_FOR_ENTITY_TYPE_OR_PROPERTY";

    /**
     * Command failed since prefix for the namespace URI '<Variable formatSpec="{0}">ns_URI</Variable>' is not provided.
     */
    String NO_NS_PREFIX_FOR_NS_URI = "NO_NS_PREFIX_FOR_NS_URI";

    /**
     * DB Login information for a user is not set.
     */
    String INVALID_DB_CREDENTIALS = "INVALID_DB_CREDENTIALS";
    /* LI-80054-End */

    /**
     * The Custom Registry {0} is not supported by WebSphere Application Server.
     */
    String MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME = "MISSING_OR_INVALID_CUSTOM_REGISTRY_CLASS_NAME";

    /**
     * Exception is thrown from Custom Registry configured as Virtual Member Manager repository "{0}".
     */
    String CUSTOM_REGISTRY_EXCEPTION = "CUSTOM_REGISTRY_EXCEPTION";

    /**
     * Failure in generating the certificate.
     */
    String CERTIFICATE_MAP_FAILED = "CERTIFICATE_MAP_FAILED";

    /**
     * Failure to map certificate.
     */
    String CERTIFICATE_GENERATE_FAILED = "CERTIFICATE_GENERATE_FAILED";

    /**
     * Attribute mapping for <attribute_name> is not defined for one or more specified entity types.
     */
    String ATTRIBUTE_MAPPING_NOT_DEFINED_FOR_ENTITY_TYPE = "ATTRIBUTE_MAPPING_NOT_DEFINED_FOR_ENTITY_TYPE";

    /**
     * Attribute mapping for <attribute_name> already exists for one or more specified entity types.
     */
    String ATTRIBUTE_MAPPING_ALREADY_EXISTS_FOR_ENTITY_TYPE = "ATTRIBUTE_MAPPING_ALREADY_EXISTS_FOR_ENTITY_TYPE";

    /**
     * Common attribute mapping for <attribute_name> already exists.
     */
    String COMMON_ATTRIBUTE_MAPPING_ALREADY_EXISTS = "COMMON_ATTRIBUTE_MAPPING_ALREADY_EXISTS";

    /**
     * Common attribute mapping for <attribute_name> can not be added.
     */
    String INVALID_COMMON_ATTRIBUTE_MAPPING = "INVALID_COMMON_ATTRIBUTE_MAPPING";

    /**
     * Attribute or property name is required.
     */
    String ATTR_OR_PROP_NAME_REQD = "ATTR_OR_PROP_NAME_REQD";

    /**
     * WAS User Registry Attribute Name Mapping is invalid.
     */
    String INVALID_USER_REGISTRY_ATTRIBUTE_NAME = "INVALID_USER_REGISTRY_ATTRIBUTE_NAME";

    /**
     * Specified property can not be mapped.
     */
    String PROPERTY_CAN_NOT_BE_MAPPED = "PROPERTY_CAN_NOT_BE_MAPPED";

    /**
     * Source and Destination Domain Name cannot be same.
     */
    String SOURCE_DEST_DOMAIN_CANNOT_BE_SAME = "SOURCE_DEST_CANNOT_HAVE_SAME_VALUE";

    /**
     * Destination Domain Can not be Global/Admin
     */

    String DEST_DOMAIN_CANNOT_BE_ADMIN_OR_GLOBAL = "DESTINATION_DOMAIN_CANNOT_BE_ADMIN_OR_GLOBAL";

    /**
     * Command Failed To Execute Successfully
     */

    String COULD_NOT_COPY_VMM_RELATED_FILES = "COULD_NOT_COPY_VMM_RELATED_FILES";

    /**
     * Specified dbschema not available.
     */
    String DBSCHEMA_NOT_AVAILABLE = "DBSCHEMA_NOT_AVAILABLE";

    /**
     * Domain cannot be admin
     */
    String DOMAIN_CANNOT_BE_ADMIN = "DOMAIN_CANNOT_BE_ADMIN";

    /**
     * Specified tablespace prefix is invalid.
     */
    String DB_TABLESPACE_PREFIX_INVALID = "DB_TABLESPACE_PREFIX_INVALID";

    /**
     * Specified role name is invalid.
     */
    String INVALID_ROLE_NAME = "INVALID_ROLE_NAME";

    /**
     * Specified user or group id not unique.
     */
    String USER_OR_GROUP_ID_NOT_UNIQUE = "USER_OR_GROUP_ID_NOT_UNIQUE";

    /**
     * Specified user or group is already mapped to a role.
     */
    String USER_OR_GROUP_ALREADY_MAPPED = "USER_OR_GROUP_ALREADY_MAPPED";
    /**
     * /**
     * Could not connect to the LDAP Server specified.
     */

    String CANNOT_CONNECT_LDAP_SERVER = "CANNOT_CONNECT_TO_LDAP_SERVER";

    /**
     * The Userregistry is using current LDAP Server
     */

    String CURRENT_LDAP_SERVER = "CURRENT_LDAP_SERVER";

    /**
     * Repository Name must be either WIMLA or WIMDB in the file specified.
     */

    String REPOSITORY_NAME_MUST_BE_EITHER_WIMLA_OR_WIMDB = "REPOSITORY_NAME_MUST_BE_EITHER_WIMLA_OR_WIMDB";
    /**
     * Sort Control not valid
     */

    String SORT_CONTROL_NOT_VALID = "SORT_CONTROL_NOT_VALID";

    /**
     * The security domain 'domain_name' is not found.
     */
    String DOMAIN_NOT_FOUND = "DOMAIN_NOT_FOUND";

    /**
     * Deleted group member list.
     */
    String DELETED_GROUPMEMBER = "DELETED_GROUPMEMBER";

    /**
     * The Transaction was rolled back.
     */
    String TRANSACTION_ROLLED_BACK = "TRANSACTION_ROLLED_BACK";

    /**
     * Specified Entity type is invalid.
     */
    String INVALID_ENTITY_TYPE = "INVALID_ENTITY_TYPE";

    /**
     * Repository '{0}': Clear cache mode '{1}' passed in the Cache Control by user '{2}'.
     */
    String CLEAR_ALL_CLEAR_CACHE_MODE = "CLEAR_ALL_CLEAR_CACHE_MODE";

    /**
     * Repository '{0}': Unknown clear cache mode '{1}' passed in the Cache Control.
     */
    String UNKNOWN_CLEAR_CACHE_MODE = "UNKNOWN_CLEAR_CACHE_MODE";

    /**
     * Repository '{0}': Clear cache mode '{1}' is not supported for this operation.
     */
    String UNSUPPORTED_CLEAR_CACHE_MODE = "UNSUPPORTED_CLEAR_CACHE_MODE";

    /**
     * Specified default parent is not within realm scope.
     */
    String DEFAULT_PARENT_NOT_IN_SCOPE = "DEFAULT_PARENT_NOT_IN_SCOPE";

    /**
     * No mapping existing for specified entitytype in realm.
     */
    String NO_PARENT_FOR_ENTITY_TYPE_IN_REALM = "NO_PARENT_FOR_ENTITY_TYPE_IN_REALM";

    /**
     * Entity type name is not specified for property names.
     */
    String ENTITY_TYPE_NAME_NOT_SPECIFIED = "ENTITY_TYPE_NAME_NOT_SPECIFIED";

    /**
     * Value for timestamp format specified for LDAP adapter is invalid.
     */
    String INVALID_TIMESTAMP_FORMAT = "INVALID_TIMESTAMP_FORMAT";

    /**
     * The extended property has already been defined and will be ignored.
     */
    String DUPLICATE_PROPERTY_EXTENDED = "DUPLICATE_PROPERTY_EXTENDED";

    /**
     * The extended property cannot override a property on the entity.
     */
    String DUPLICATE_PROPERTY_ENTITY = "DUPLICATE_PROPERTY_ENTITY";

    /**
     * The LDAP registry will ignore the certificate authentication request since 'certificateMapeMode' is set to IGNORE.
     */
    String LDAP_REGISTRY_CERT_IGNORED = "LDAP_REGISTRY_CERT_IGNORED";

    /**
     * No custom X.509 certificate mapper implementation has been registered with the LDAP registry.
     */
    String LDAP_REGISTRY_MAPPER_NOT_BOUND = "LDAP_REGISTRY_MAPPER_NOT_BOUND";

    /**
     * The custom X.509 certificate mapper implementation has thrown a CertificateMapNotSupportedException.
     */
    String LDAP_REGISTRY_CUSTOM_MAPPER_NOT_SUPPORTED = "LDAP_REGISTRY_CUSTOM_MAPPER_NOT_SUPPORTED";

    /**
     * The custom X.509 certificate mapper implementation has thrown a CertificateMapFailedException.
     */
    String LDAP_REGISTRY_CUSTOM_MAPPER_FAILED = "LDAP_REGISTRY_CUSTOM_MAPPER_FAILED";

    /**
     * The custom X.509 certificate mapper implementation returned an invalid mapping value.
     */
    String LDAP_REGISTRY_INVALID_MAPPING = "LDAP_REGISTRY_INVALID_MAPPING";

    /**
     * CWIMK0011E: The user registry operation could not be completed. A valid user registry or repository was not found.
     */
    String MISSING_REGISTRY_DEFINITION = "MISSING_REGISTRY_DEFINITION";

    /**
     * The defined userFilter attribute will be ignored since there are loginProperty attributes defined.
     */
    String LOGINPROPERTY_OVERRIDE_USERFILTER = "LOGINPROPERTY_OVERRIDE_USERFILTER";

    /**
     * CWIML4553E: Kerberos login failed using Kerberos principal {0} and Kerberos credential cache (ccache) {1}.
     */
    String KRB5_LOGIN_FAILED_CACHE = "KRB5_LOGIN_FAILED_CACHE";

    /**
     * WIML4554E: Kerberos login failed using Kerberos principal {0} and Kerberos keytab {1}.
     */
    String KRB5_LOGIN_FAILED_KEYTAB = "KRB5_LOGIN_FAILED_KEYTAB";

    /**
     * CWIML4555E: Kerberos login failed using Kerberos principal {0} and the default Kerberos credential cache (ccache).
     */
    String KRB5_LOGIN_FAILED_DEFAULT_CACHE = "KRB5_LOGIN_FAILED_DEFAULT_CACHE";

    /**
     * CWIML4556E: Kerberos login failed using Kerberos principal {0} and the default Kerberos keytab.
     */
    String KRB5_LOGIN_FAILED_DEFAULT_KEYTAB = "KRB5_LOGIN_FAILED_DEFAULT_KEYTAB";

    /**
     * CWIML4557I: LDAPRegistry {0} configured with Kerberos credential cache (ccache) filename {1} and keytab filename {2}, using Kerberos credential cache (ccache) for Kerberos
     * bind authentication to LDAP server.
     */
    String KRB5_TICKETCACHE_USED = "KRB5_TICKETCACHE_USED";

    /**
     * CWIML4558E: The {0} Kerberos principal name is incorrectly formatted, or the realm name is missing, or a default realm name cannot be found.
     */
    String INVALID_KRB5_PRINCIPAL = "INVALID_KRB5_PRINCIPAL";

    /**
     * CWIML4559E: LDAPRegistry {0} could not read the Kerberos file {1}.
     */
    String CANNOT_READ_KRB5_FILE = "CANNOT_READ_KRB5_FILE";

    /**
     * CWIML4560E: The [{0}] attribute from the {1} element is configured to a file that does not exist at: {2}
     */
    String KRB5_FILE_NOT_FOUND = "KRB5_FILE_NOT_FOUND";

    /**
     * CWIML4561I: The LdapRegistry component is configured to use a {0} file located at {1}
     */
    String KRB5_FILE_FOUND = "KRB5_FILE_FOUND";

    /**
     * CWIML4518W: The {0} {1} value is malformed. The value must be a series of objectclass:attribute or *:attribute pairs, where each pair is separated by a semi-colon.
     */
    String IDMAP_INVALID_FORMAT = "IDMAP_INVALID_FORMAT";

    /**
     * CWIML4521E: The {0} LdapRegistry attempted to bind to the Ldap server using Kerberos credentials for {1} principal name, but the KerberosService
     * is not available. The bind authentication mechanism is {2}.
     */
    String KRB5_SERVICE_NOT_AVAILABLE = "KRB5_SERVICE_NOT_AVAILABLE";

    /**
     * CWIML4523E: The {0} value for {1} is invalid. It requires an attribute value assertion where the value assertion is =%v. For example, {2}.
     */
    String FILTER_MISSING_PERCENT_V = "FILTER_MISSING_PERCENT_V";
}
