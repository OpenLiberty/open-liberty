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
package com.ibm.wsspi.security.wim;

/**
 * The interface containing all schema related constants.
 */
public interface SchemaConstants {

    /**
     * The name space URI of vmm data graph model.
     */
    String WIM_NS_URI = "http://www.ibm.com/websphere/wim";
    /**
     * The name space prefix of vmm data graph model.
     */
    String WIM_NS_PREFIX = "wim";
    /**
     * The name of the root data object 'DocumentRoot'.
     */
    String DO_DOCUMENT_ROOT = "DocumentRoot";
    /**
     * The name space URI of vmm data graph model.
     */
    String WIM_MODEL_PACKAGE = "com.ibm.websphere.wim.model.ModelPackage";
    /**
     * The name of the DataObject "schema" in vmm data graph
     */
    String DO_SCHEMA = "schema";
    /**
     * The name of the DataObject "entitySchema" in vmm data graph
     */
    String DO_ENTITY_SCHEMA = "entitySchema";
    /**
     * The name of the property "nsURI" in vmm data graph. The value of this property is any namespace URI.
     */
    String PROP_NS_URI = "nsURI";
    /**
     * The name of the property "nsPrefix" in vmm data graph. The value of this property is the prefix of the namespace specified in nsURI.
     */
    String PROP_NS_PREFIX = "nsPrefix";
    /**
     * The name of the property "entityName" in vmm data graph. Its value is the name of the new entity type.
     */
    String PROP_ENTITY_NAME = "entityName";
    /**
     * The name of the property "entityTypeName" in vmm data graph. Its value is the name of the new entity type.
     */
    String PROP_ENTITY_TYPE_NAME = "entityTypeName";
    /**
     * The name of the property "entityTypeNames" in vmm data graph. Its value is the name of the new entity type.
     */
    String PROP_ENTITY_TYPE_NAMES = "entityTypeNames";
    /**
     * The name of the property "parentEntityName" in vmm data graph. Its value is the name of the parent entity data object.
     */
    String PROP_PARENT_ENTITY_NAME = "parentEntityName";
    /**
     * The name of the property "propertyNames" in vmm data graph. Its values are the list of property names.
     */
    String PROP_PROPERTY_NAMES = "propertyNames";
    /**
     * The name of the DataObject "entityConfiguration" in vmm data graph. This data object contains the configuration information of the new entity type.
     */
    String DO_ENTITY_CONFIGURATION = "entityConfiguration";
    /**
     * The name of the property "defaultParent" in vmm data graph. Its value is the unique name of the default parent of an entity type.
     */
    String PROP_DEFAULT_PARENT = "defaultParent";
    /**
     * The name of the property "rdnProperty" in vmm data graph. Its value is the name of rdn property.
     */
    String PROP_RDN_PROPERTY = "rdnProperty";
    /**
     * The name of the DataObject "actionNotAllow" in vmm data graph. It contains the name of the action, repository UUID
     */
    String DO_ACTION_NOT_ALLOW = "actionNotAllow";
    /**
     * The name of the property "actionName" in vmm data graph. Its value is the name of the action.
     */
    String PROP_ACTION_NAME = "actionName";
    /**
     * The name of the property "name" in the vmm data graph.
     */
    String PROP_NAME = "name";
    /**
     * The name of the property "lang" in the vmm data graph.
     */
    String PROP_LANG = "lang";
    /**
     * The name of the property "value" in the vmm data graph.
     */
    String PROP_VALUE = "value";
    /**
     * The name of the property "values" in the vmm data graph.
     */
    String PROP_VALUES = "values";
    /**
     * The name of the data object "propertySchema" in vmm data graph.
     */
    String DO_PROPERTY_SCHEMA = "propertySchema";
    /**
     * The name of the property "propertyName" in the vmm data graph
     */
    String PROP_PROPERTY_NAME = "propertyName";
    /**
     * The name of the property "multiValued" in the vmm data graph. It indicates if a property can have multiple values.
     */
    String PROP_MULTI_VALUED = "multiValued";
    /**
     * The name of the property "dataType" in the vmm data graph. Its value is name of the data type. i.e INTEGER
     */
    String PROP_DATA_TYPE = "dataType";
    /**
     * The name of the property "valueLength" in the vmm data graph. Its value indicates the value length of a String type property.
     */
    String PROP_VALUE_LENGTH = "valueLength";
    /**
     * The name of the property "applicableEntityTypeNames" in the vmm data graph. Its values are the names of entity type which a property is applicable.
     */
    String PROP_APPLICABLE_ENTITY_TYPE_NAMES = "applicableEntityTypeNames";
    /**
     * The name of the property "requiredEntityTypeNames" in the vmm data graph. Its values are the names of entity type which a property is required.
     */
    String PROP_REQUIRED_ENTITY_TYPE_NAMES = "requiredEntityTypeNames";
    /**
     * The name of the DataObject "metaData" in the vmm data graph. It contains name/values pair.
     */
    String DO_META_DATA = "metaData";

    /**
     * Name of the DataObject 'Root' in the virtual member manager data graph.
     * This is used while creating or accessing the 'Root' DataObject.
     * While creating the DataObject or while retrieving it, this constant is used as the name of the DataObject('Root') to be created or retrieved.
     */
    String DO_ROOT = "Root";
    /**
     * Name of type of root DataObject i.e. 'RootType'.
     * It is used to identify whether the given DataObject is of type 'Root' or not.
     */
    String DO_ROOT_TYPE = "RootType";
    /**
     * Name of the property 'validated' in the virtual member manager DataObject.
     * While modifying or accessing the property 'validated' of the DataObject, this constant is used as the name of the property 'validated'.
     */
    String PROP_VALIDATED = "validated";
    /**
     * Name of the 'entities' DataObject in the virtual member manager data graph.
     * While creating an entity DataObject or while retrieving it, this constant is used as the name of the entity DataObject('entities') to be created or retrieved.
     */
    String DO_ENTITIES = "entities";
    /**
     * Name of the 'controls' DataObject in the virtual member manager data graph.
     * While creating the controls DataObject or while retrieving it, this constant is used as the name of the control DataObject('controls') to be created or retrieved.
     */
    String DO_CONTROLS = "controls";
    /**
     * Name of the 'PropertyControl' DataObject.
     * 'PropertyControl' DataObject is usually used to store or retrieve property information from DataObject.
     */
    String DO_PROPERTY_CONTROL = "PropertyControl";
    /**
     * Name of the 'LoginControl' DataObject.
     * 'LoginControl' DataObject is mainly used to pass essential information for login operation.
     */
    String DO_LOGIN_CONTROL = "LoginControl";
    /**
     * Stands for the constant 'mappedProperties'.
     * This is usually used to retrieve all the mapped properties from 'LoginControl' DataObject.
     */
    String PROP_MAPPED_PROPERTIES = "mappedProperties";
    /**
     * The constant for type "PropertyDefinitionControl"
     */
    String DO_PROPERTY_DEFINITION_CONTROL = "PropertyDefinitionControl";
    /**
     * The constant for type "ExtensionPropertyDefinitionControl"
     */
    String DO_EXTENSION_PROPERTY_DEFINITION_CONTROL = "ExtensionPropertyDefinitionControl";
    /**
     * Stands for the constant 'properties'.
     * It represents properties of controls DataObject.
     * It is used while retrieving all the properties from controls DataObject.
     */
    String PROP_PROPERTIES = "properties";
    /**
     * Stands for the constant 'properties'.
     * It represents properties of entities DataObject.
     * It is used while retrieving all the properties from entities DataObject.
     */
    String DO_PROPERTIES = "properties";
    /**
     * Name of the 'EntityTypeControl' DataObject.
     * While creating the EntityTypeControl DataObject or while retrieving it, this constant is used as the name of the control DataObject('EntityTypeControl') to be created or
     * retrieved.
     */
    String DO_ENTITY_TYPE_CONTROL = "EntityTypeControl";
    /**
     * Stands for constant '*'.
     * It represents all the properties. Hence it can be used to retrieve all the properties from entity.
     */
    String VALUE_ALL_PROPERTIES = "*";
    /**
     * The constant for element name "contextProperties"
     */
    String DO_CONTEXT_PROPERTIES = "contextProperties";
    /**
     * Name of the 'contexts' DataObject in virtual member manager data graph.
     * While creating the contexts DataObject or while retrieving it, this constant is used as the name of the contexts DataObject('contexts') to be created or retrieved.
     */
    String DO_CONTEXTS = "contexts";
    String DO_CONTEXT = "context";
    /**
     * The name of the property "key" in contexts DataObject of virtual member manager data graph.
     * This is used while modifying or retrieving property 'key' of contexts DataObject.
     */
    String PROP_KEY = "key";
    /**
     * The constant for the type of "Entity"
     */
    String DO_ENTITY = "Entity";
    /**
     * The constant for the entity of type 'Person'
     * This is used while creating or retrieving entity DataObject of type 'Person'.
     */
    String DO_PERSON = "Person";
    /**
     * The constant for the entity of type 'PersonAccount'
     * This is used while creating or retrieving entity DataObject of type 'PersonAccount'.
     */
    String DO_PERSON_ACCOUNT = "PersonAccount";
    /**
     * Name of the 'Group' DataObject.
     * This is used while creating or retrieving DataObject of type 'Group'.
     */
    String DO_GROUP = "Group";
    /**
     * Name of the 'OrgContainer' DataObject.
     */
    String DO_ORGCONTAINER = "OrgContainer";
    /**
     * The constant for the entity of type 'LoginAccount'
     * This is used while creating or retrieving entity DataObject of type 'LoginAccount'.
     */
    String DO_LOGIN_ACCOUNT = "LoginAccount";
    /**
     * The constant for property 'realm' in DataObject.
     * This is usually used while assigning or retrieving property 'realm' from entity.
     */
    String PROP_REALM = "realm";
    /**
     * The constant for property 'principalName' in entity DataObject.
     * This is used for assigning or retrieving property 'principalName' from entity DataObject.
     */
    String PROP_PRINCIPAL_NAME = "principalName";
    /**
     * Name of the 'principal' DataObject.
     * This is used while creating or retrieving DataObject of type 'principal'.
     */
    String DO_PRINCIPAL = "principal";
    /**
     * The constant for property 'password' in entity DataObject.
     * This is used for assigning or retrieving property 'password' from entity DataObject.
     */
    String PROP_PASSWORD = "password";
    /**
     * The constant for property 'certificate' in entity DataObject.
     * This is used for assigning or retrieving property 'certificate' from entity DataObject.
     */
    String PROP_CERTIFICATE = "certificate";

    /**
     * The constant for element name "identifier"
     */
    String DO_IDENTIFIER = "identifier";
    /**
     * The constant for type of the identifier element i.e. 'IdentifierType'.
     */
    String DO_IDENTIFIER_TYPE = "IdentifierType";
    /**
     * The constant for property 'uniqueName' in an identifier DataObject.
     * This is used for assigning or retrieving property 'uniqueName' from identifier DataObject.
     * This is also used while dealing with 'uniqueName' property in User Registry.
     */
    String PROP_UNIQUE_NAME = "uniqueName";
    /**
     * The constant for property 'uniqueId' in an identifier DataObject.
     * This is used for assigning or retrieving property 'uniqueId' from identifier DataObject.
     */
    String PROP_UNIQUE_ID = "uniqueId";
    /**
     * The constant for property 'externalName' in an identifier DataObject.
     * This is used for assigning or retrieving property 'externalName' from identifier DataObject.
     */
    String PROP_EXTERNAL_NAME = "externalName";
    /**
     * The constant for property name "externalId";
     */
    String PROP_EXTERNAL_ID = "externalId";
    /**
     * The constant for property name "repositoryId"
     */
    String PROP_REPOSITORY_ID = "repositoryId";
    /**
     * The constant for property name "repositoryId"
     */
    String DO_REPOSITORY_IDS = "repositoryIds";
    /**
     * The constant for property name "members"
     */
    String DO_MEMBERS = "members";
    /**
     * The constant for property name "groups"
     */
    String DO_GROUPS = "groups";
    /**
     * The constant for property name "children"
     */
    String DO_CHILDREN = "children";
    /**
     * The constant for type "SearchControl"
     */
    String DO_SEARCH_CONTROL = "SearchControl";
    /**
     * The constant for property name "GroupMemberControl"
     */
    String DO_GROUP_MEMBER_CONTROL = "GroupMemberControl";
    /**
     * The constant for property name "AncestorControl"
     */
    String DO_ANCESTOR_CONTROL = "AncestorControl";
    /**
     * The constant for property name "DescendantControl"
     */
    String DO_DESCENDANT_CONTROL = "DescendantControl";
    /**
     * The constant for property name "DataTypeControl"
     */
    String DO_DATATYPE_CONTROL = "DataTypeControl";
    /**
     * The constant for property name "ExternalNameControl"
     */
    String DO_EXTERNAL_NAME_CONTROL = "ExternalNameControl";

    /**
     * The constant for property name "GroupMembershipControl"
     */
    String DO_GROUP_MEMBERSHIP_CONTROL = "GroupMembershipControl";
    /**
     * The constant for property name "CheckGroupMembershipControl"
     */
    String DO_CHECK_GROUP_MEMBERSHIP_CONTROL = "CheckGroupMembershipControl";
    /**
     * The constant for property name "CheckGroupMembershipControl"
     */
    String PROP_IN_GROUP = "inGroup";
    /**
     * The constant for property name "level"
     */
    String PROP_LEVEL = "level";
    /**
     * The constant for property name "level-1"
     */
    short PROP_LEVEL_IMMEDIATE = 1;
    /**
     * The constant for property name "level-0"
     */
    short PROP_LEVEL_NESTED = 0;
    /**
     * The constant for property name "treeview"
     */
    String PROP_TREEVIEW = "treeView";
    /**
     * The constant for property name "countLimit"
     */
    String PROP_COUNT_LIMIT = "countLimit";
    /**
     * The constant for property name "searchLimit"
     */
    String PROP_SEARCH_LIMIT = "searchLimit";
    /**
     * The constant for property name "timeLimit"
     */
    String PROP_TIME_LIMIT = "timeLimit";
    /**
     * The constant for property name "expression"
     */
    String PROP_SEARCH_EXPRESSION = "expression";
    /**
     * The constant for property name "searchBases"
     */
    String PROP_SEARCH_BASES = "searchBases";
    /**
     * The constant for type "SearchResponseControl"
     */
    String DO_SEARCH_RESPONSE_CONTROL = "SearchResponseControl";
    /**
     * The constant for property name "hasMoreResults"
     */
    String PROP_HAS_MORE_RESULTS = "hasMoreResults";
    /**
     * The constant for type "SortControl"
     */
    String DO_SORT_CONTROL = "SortControl";
    /**
     * The constant for property name "sortKeys"
     */
    String DO_SORT_KEYS = "sortKeys";
    /**
     * The constant for property name "locale"
     */
    String PROP_SORT_LOCALE = "locale";
    /**
     * The constant for propert name "ascendingOrder"
     */
    String PROP_ASCENDING_ORDER = "ascendingOrder";
    /**
     * The constant for type "PageControl"
     */
    String DO_PAGE_CONTROL = "PageControl";
    /**
     * The constant for property name "size"
     */
    String PROP_SIZE = "size";
    /**
     * The constant for property name "cookie"
     */
    String PROP_COOKIE = "cookie";
    /**
     * The constant for type "PageResponseControl"
     */
    String DO_PAGE_RESPONSE_CONTROL = "PageResponseControl";

    // 80118-Start
    /**
     * The constant for type "ChangeControl"
     */
    String DO_CHANGE_CONTROL = "ChangeControl";
    /**
     * The constant for type "ChangeResponseControl"
     */
    String DO_CHANGE_RESPONSE_CONTROL = "ChangeResponseControl";
    /**
     * The constant for "checkPointType"
     */
    String DO_CHECKPOINT_TYPE = "CheckPointType";
    /**
     * The constant for "checkPoint"
     */
    String DO_CHECKPOINT = "checkPoint";
    /**
     * The constant for property type "changeType"
     */
    String PROP_CHANGETYPE = "changeType";
    /**
     * The constant for property type "changeTypes"
     */
    String PROP_CHANGETYPES = "changeTypes";
    /**
     * The constant for property type "repositoryCheckPoint"
     */
    String PROP_REPOSITORY_CHECKPOINT = "repositoryCheckPoint";
    // 80118-End
    /**
     * The constant for property name "totalSize"
     */
    String PROP_TOTAL_SIZE = "totalSize";
    /**
     * The constant for element name "parent"
     */
    String DO_PARENT = "parent";
    /**
     * The constant for property name "modifyMode";
     */
    String PROP_MODIFY_MODE = "modifyMode";
    /**
     * The constant for repository property name.
     */
    String META_REPOSITORY_PROPERTY_NAME = "repositoryPropertyName";
    /**
     * The constant for element name "propertyDataTypes".
     */
    String DO_PROPERTY_DATA_TYPES = "propertyDataTypes";
    /**
     * The constant for repository data type.
     */
    String META_REPOSITORY_DATA_TYPE = "repositoryDataType";
    /**
     * The constant for LDAP object class meta data.
     */
    String META_LDAP_OBJECT_CLASSES = "objectClasses";
    /**
     * The constant for LDAP object class meta data that is used for entity creation.
     */
    String META_LDAP_OBJECT_CLASSES_FOR_CREATE = "objectClassesForCreate";
    /**
     * The constant for LDAP RDN attributes meta data.
     */
    String META_LDAP_RDN_ATTRIBUTES = "rdnAttributes";
    /**
     * The constant for LDAP search bases meta data.
     */
    String META_LDAP_SEARCH_BASES = "searchBases";
    /**
     * The constant for LDAP search filter meta data.
     */
    String META_LDAP_SEARCH_FILTER = "searchFilter";

    /**
     * The constant for element "RequestControl"
     **/
    String DO_REQUEST_CONTROL = "RequestControl";

    /**
     * The constant for property name "requiredInteractionStyle";
     **/
    String PROP_REQUIRED_INTERACTION_STYLE = "requiredInteractionStyle";

    /**
     * The constant for property name "ticket";
     **/
    String PROP_TICKET = "ticket";

    /**
     * The constant for element "ResponseControl"
     **/
    String DO_RESPONSE_CONTROL = "ResponseControl";

    /**
     * The constant for property name "complete";
     **/
    String PROP_COMPLETE = "complete";
    /**
     * The constant for element "extensionPropertySchema".
     */
    String DO_EXTENSION_PROPERTY_SCHEMA = "extensionPropertySchema";
    /**
     * The constant for element "ExtensionPropertyDataTypeControl".
     */
    String DO_EXTENSION_PROPERTY_DATATYPE_CONTROL = "ExtensionPropertyDataTypeControl";
    /**
     * The constant for element "DeleteControl".
     */
    String DO_DELETE_CONTROL = "DeleteControl";

    /**
     * The constant for property name "deleteDescendants".
     */
    String PROP_DELETE_DESCENDANTS = "deleteDescendants";
    /**
     * The constant for property name "returnDeleted".
     */
    String PROP_RETURN_DELETED = "returnDeleted";
    /**
     * The constant for property name "createTimestamp".
     */
    String PROP_CREATE_TIMESTAMP = "createTimestamp";

    /**
     * The constant for property name "modifyTimestamp".
     */
    String PROP_MODIFY_TIMESTAMP = "modifyTimestamp";

    /**
     * The constant for assign mode
     */
    int VALUE_MODIFY_MODE_ASSIGN = 1;

    /**
     * The constant for replace mode
     */
    int VALUE_MODIFY_MODE_REPLACE = 2;

    /**
     * The constant for unassign mode
     */
    int VALUE_MODIFY_MODE_UNASSIGN = 3;
    /**
     * The constant string for the virtual realm key
     */
    String VALUE_CONTEXT_REALM_KEY = "realm";
    /**
     * The constant string for trusting the entity type
     */
    String VALUE_CONTEXT_TRUST_ENTITY_TYPE_KEY = "trustEntityType";
    /**
     * The constant string for the wildcard "*"
     */
    String VALUE_WILD_CARD = "*";
    /**
     * The constant string for WMM ADAPTER CLASS NAME
     */
    String PROP_WMM_ADAPTER_CLASS_NAME = "WMMAdapterClassName";
    /**
     * The constant for property name "returnSubType"
     */
    String PROP_RETURN_SUB_TYPE = "returnSubType";
    /**
     * Instance Class: java.lang.String
     */
    String DATA_TYPE_STRING = "String";
    /**
     * Instance Class: int
     */
    String DATA_TYPE_INT = "Int";
    /**
     * Instance Class: java.lang.Object
     */
    String DATA_TYPE_DATE = "Date";
    /**
     * Instance Class: java.lang.Object
     */
    String DATA_TYPE_DATE_TIME = "DateTime";
    /**
     * Instance Class: dobjava.lang.Object
     */
    String DATA_TYPE_ANY_SIMPLE_TYPE = "AnySimpleType";
    /**
     * Instance Class: java.lang.String
     */
    String DATA_TYPE_ANY_URI = "AnyURI";
    /**
     * Instance Class: java.lang.boolean
     */
    String DATA_TYPE_BOOLEAN = "Boolean";
    /**
     * Instance Class: long
     */
    String DATA_TYPE_LONG = "Long";
    /**
     * Instance Class: double
     */
    String DATA_TYPE_DOUBLE = "Double";
    /**
     * Instance Class: AddressType
     */
    String DATA_TYPE_ADDRESS_TYPE = "AddressType";
    /**
     * Instance Class: short
     */
    String DATA_TYPE_SHORT = "Short";
    /**
     * Instance Class: java.lang.String
     */
    String DATA_TYPE_TOKEN = "Token";
    /**
     * Instance Class: byte[]
     */
    String DATA_TYPE_BASE_64_BINARY = "Base64Binary";
    /**
     * Instance Class: byte
     */
    String DATA_TYPE_BYTE = "Byte";

    // vmm Data type
    /**
     * Instance Class: java.lang.String
     */
    String DATA_TYPE_LANG_TYPE = "LangType";
    /**
     * Instance Class: identifier data object
     */
    String DATA_TYPE_IDENTIFIER_TYPE = "IdentifierType";
    /**
     * Instance Class: entity data object
     */
    String DATA_TYPE_ENTITY_TYPE = "Entity";
    /**
     * Instance Class: group data object
     */
    String DATA_TYPE_GROUP_TYPE = "Group";
    /**
     * Instance Class: person data object
     */
    String DATA_TYPE_PERSON_TYPE = "Person";
    /**
     * The constant string for entity type 'Entity'
     */
    String TYPE_ENTITY = "Entity";
    /**
     * The constant string for entity type 'Person'
     */
    String TYPE_PERSON = "Person";
    /**
     * The constant string for entity type 'Group'
     */
    String TYPE_GROUP = "Group";
    /**
     * The constant string for entity type 'OrgContainer'
     */
    String TYPE_ORG_CONTAINER = "OrgContainer";
    /**
     * The constant string for type "IdentifierType"
     */
    String TYPE_IDENTIFIER = "IdentifierType";
    /**
     * The constant string for entity type 'LoginAccount'
     */
    String TYPE_LOGIN_ACCOUNT = "LoginAccount";
    /**
     * The constant string for context type 'Context'
     */
    String TYPE_CONTEXT = "Context";
    /**
     * The constant string for entity type 'PersonAccount'
     */
    String TYPE_PERSON_ACCOUNT = "PersonAccount";

    /**
     * The constant string for type "metaDataType"
     */
    String TYPE_META_DATA = "MetaDataType";

    /**
     * The constant for metadata property 'valueLength'.
     */
    String META_DATABASE_VALUE_LENGTH = "valueLength";

    /**
     * The constant for metadata property 'readOnly'.
     */
    String META_DATABASE_READ_ONLY = "readOnly";

    /**
     * The constant for metadata property 'caseExactMatch'.
     * It is used to make the searches case sensitive.
     */
    String META_DATABASE_CASE_EXACT_MATCH = "caseExactMatch";

    /**
     * The constant for metadata property 'multiValued'.
     */
    String META_DATABASE_MULTI_VALUED = "multiValued";

    /**
     * The constant for metadata property 'classname'.
     */
    String META_DATABASE_CLASSNAME = "classname";

    /**
     * The constant for metadata property 'description'.
     */
    String META_DATABASE_DESCRIPTION = "description";

    /**
     * The constant for metadata property 'applicationId'.
     */
    String META_DATABASE_APPLICATION_ID = "applicationId";

    /**
     * The constant for metadata property 'isComposite'.
     */
    String META_DATABASE_IS_COMPOSITE = "isComposite";

    /**
     * The constant for metadata property 'metaName'.
     */
    String META_DATABASE_META_NAME = "metaName";

    /**
     * Name of 'viewIdentifiers' DataObject.
     * Constant for View Processing
     */
    String DO_VIEW_IDENTIFIERS = "viewIdentifiers";
    String DO_VIEW_IDENTIFIER_TYPE = "ViewIdentifierType";
    /**
     * Name of 'ViewControl' DataObject.
     * This constant is used for view-specific operations.
     */
    String DO_VIEW_CONTROL = "ViewControl";
    /**
     * Name of property 'viewName' of ViewControl DataObject.
     * This constant is used for view-specific operations.
     */
    String PROP_VIEW_NAME = "viewName";
    /**
     * Name of property 'viewEntryUniqueId' of ViewControl DataObject.
     * This constant is used for view-specific operations.
     */
    String PROP_VIEW_ENTRY_UNIQUE_ID = "viewEntryUniqueId";
    /**
     * Name of property 'viewEntryName' of ViewControl DataObject.
     * This constant is used for view-specific operations.
     */
    String PROP_VIEW_ENTRY_NAME = "viewEntryName";
    /**
     * The constant for DataObject type "SortKeyType"
     */
    String DO_SORT_KEY_TYPE = "SortKeyType";

    /**
     * This constant stands for 'sync'
     * This constant is used for RequestControl.requiredInteractionStyle
     */
    String SYNC_MODE = "sync";
    /**
     * This constant stands for 'async'
     * This constant is used for RequestControl.requiredInteractionStyle
     */
    String ASYNC_MODE = "async";
    /**
     * This constant stands for 'syncOrAsync'
     * This constant is used for RequestControl.requiredInteractionStyle
     */
    String SYNC_OR_ASYNC_MODE = "syncOrAsync";

    // 80118-Start
    /**
     * Constant for change type 'add' for added entities.
     */
    String CHANGETYPE_ADD = "add";
    /**
     * Constant for change type 'delete' for deleted entities.
     */
    String CHANGETYPE_DELETE = "delete";
    /**
     * Constant for change type 'modify' for modified entities.
     */
    String CHANGETYPE_MODIFY = "modify";
    /**
     * Constant for change type 'rename' for renamed entities.
     */
    String CHANGETYPE_RENAME = "rename";
    /**
     * The constant indicating all change types
     */
    String CHANGETYPE_ALL = "*";
    // 80118-End

    /**
     * Constant for the Cache control data object.
     */
    String DO_CACHE_CONTROL = "CacheControl";

    /**
     * Constant for the clear cache mode property in the CacheControl DO.
     */
    String CACHE_MODE = "mode";

    /**
     * Constant for the clear single entity mode property in the CacheControl DO.
     */
    String CACHE_MODE_CLEAR_ENTITY = "clearEntity";

    /**
     * Constant for the clearAll mode property in the CacheControl DO.
     */
    String CACHE_MODE_CLEARALL = "clearAll";

    /**
     * Constant for identifying whether an entity type is mandatory or not.
     */
    String IS_REQUIRED = "isRequired";

    /**
     * Custom property to allow VMM to treat '=' symbol as a literal
     */
    String ALLOW_DN_PRINCIPALNAME_AS_LITERAL = "allowDNPrincipalNameAsLiteral";

    /**
     * Constant to force using userFilter while searching users (if applicable)
     */
    static final String USE_USER_FILTER_FOR_SEARCH = "useUserFilterForSearch";

    /**
     * Constant to force using groupFilter while searching groups (if applicable)
     */
    static final String USE_GROUP_FILTER_FOR_SEARCH = "useGroupFilterForSearch";

}
