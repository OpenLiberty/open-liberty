/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.registry.util;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.Service;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.security.wim.registry.WIMUserRegistryDefines;
import com.ibm.ws.security.wim.registry.dataobject.IDAndRealm;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.InvalidIdentifierException;
import com.ibm.wsspi.security.wim.exception.InvalidUniqueNameException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.ExternalNameControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Utility class for common bridge functions.
 *
 */
public class BridgeUtils implements WIMUserRegistryDefines {

    private static final TraceComponent tc = Tr.register(BridgeUtils.class);

    /**
     * Group search level.
     */
    private short groupLevel = Service.PROP_LEVEL_IMMEDIATE; // TODO THIS WAS PROP_LEVEL_NESTED (0) IN TWAS
    public boolean returnRealmInfoInUniqueUserId = false; //PM50390.2
    private final String groupLevelLock = "GROUP_LEVEL_LOCK";

    public boolean allowDNAsPrincipalName = false; //PM55588
    private static final String ALLOW_DN_PRINCIPAL_NAME_AS_LITERAL = "com.ibm.ws.wim.registry.allowDNPrincipalNameAsLiteral";
    String urRealmName = null;
    private final VMMService VMMServiceRef;
    private final ConfigManager configMgrRef;

    private boolean hasFederatedRegistry = false;

    public BridgeUtils(VMMService VMMServiceRef, ConfigManager configRef) {
        this.VMMServiceRef = VMMServiceRef;
        this.configMgrRef = configRef;
    }

    public VMMService getWimService() {
        return VMMServiceRef;
    }

    public ConfigManager getCoreConfiguration() {
        return configMgrRef;
    }

    @FFDCIgnore(NumberFormatException.class)
    public void initialize(Map<String, Object> inputProperties) {
        // initialize the method name
        String methodName = "initialize";
        // initialize the WIM service provider

        urRealmName = (String) inputProperties.get("realm");
        String retRealmInfo = (String) inputProperties.get(RETURN_REALM_QUALIFIED_ID);
        if (retRealmInfo != null) {
            returnRealmInfoInUniqueUserId = Boolean.parseBoolean(retRealmInfo);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + RETURN_REALM_QUALIFIED_ID + " = " + returnRealmInfoInUniqueUserId, methodName);
        }
        //PM55588
        String allowDNInPrincipalNameAsLiteral = (String) inputProperties.get(ALLOW_DN_PRINCIPAL_NAME_AS_LITERAL);
        if (allowDNInPrincipalNameAsLiteral != null) {
            allowDNAsPrincipalName = Boolean.parseBoolean(allowDNInPrincipalNameAsLiteral);
        }

        // initialize the group level choice
        // d111719
        String groupChoice = (String) inputProperties.get(GROUP_LEVEL);
        try {
            if ((groupChoice == null) || (groupChoice.equals(""))) {
                synchronized (groupLevelLock) {
                    groupLevel = Service.PROP_LEVEL_IMMEDIATE;
                }
            } else if (Short.parseShort(groupChoice) == Service.PROP_LEVEL_IMMEDIATE) {
                synchronized (groupLevelLock) {
                    groupLevel = Service.PROP_LEVEL_IMMEDIATE;
                }
            } else if (Short.parseShort(groupChoice) == Service.PROP_LEVEL_NESTED) {
                synchronized (groupLevelLock) {
                    groupLevel = Service.PROP_LEVEL_NESTED;
                }
            } else {
                synchronized (groupLevelLock) {
                    groupLevel = Service.PROP_LEVEL_NESTED;
                }
            }
        } catch (NumberFormatException toCatch) {
            synchronized (groupLevelLock) {
                groupLevel = Service.PROP_LEVEL_NESTED;
            }
        }

    }

    /**
     * Validate an id.
     *
     * @param inputId Id to validate.
     *
     * @throws WIMException The id is invalid because it is null or empty.
     *
     * @pre inputId != null
     * @pre inputId != ""
     */
    protected void validateId(String inputId) throws WIMException {
        // f113366
        if (inputId == null) {
            throw new WIMException();
        }
    }

    /**
     * Validate a search limit.
     *
     * @param inputLimit Limit to validate.
     *
     * @throws WIMException The limit is invalid because it is negative.
     *
     * @pre inputLimit >= 0
     */
    protected void validateLimit(int inputLimit) throws WIMException {
        // initialize the method name
        if (inputLimit < 0) {
            throw new WIMException(Integer.toString(inputLimit));
        }
    }

    /**
     * Validate an X509 certificate array.
     *
     * @param chain Certificate chain to validate.
     *
     * @throws com.ibm.wsspi.security.wim.exception.CertificateMapFailedException The certificate array is invalid because it is null,
     *                                                                                empty or contains an null certificate.
     */
    protected void validateCertificate(X509Certificate[] chain) throws com.ibm.wsspi.security.wim.exception.CertificateMapFailedException {
        // validate the certificate array
        if (chain == null || chain.length == 0) {
            throw new com.ibm.wsspi.security.wim.exception.CertificateMapFailedException();
        }

        for (X509Certificate cert : chain) {
            if (cert == null) {
                throw new com.ibm.wsspi.security.wim.exception.CertificateMapFailedException();
            }
        }
    }

    /**
     * Separate the ID and realm from an input String.
     *
     * @param inputString String containing the ID and realm.
     *
     * @return WIMUserRegistryID with the ID and realm separated.
     *
     * @throws WIMException The default realm could not be found.
     *
     * @pre inputString != null
     * @post $return != null
     * @post $return.getId() != null
     * @post $return.getRealm() != null
     */

    protected IDAndRealm separateIDAndRealm(String inputString) throws WIMException {
        // initialize the method name
        String methodName = "seperateIDAndRealm";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + "inputString = \"" + inputString + "\"");
        }
        String defaultRealm = getDefaultRealmName();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + "Default realm name = \"" + defaultRealm + "\"");
        }
        String defaultRealmDelimiter = getCoreConfiguration().getDefaultDelimiter();
        Set<String> virtualRealms = getCoreConfiguration().getRealmNames(); //will not be null since set will always be returned
        Map<String, String> virtualRealmsDelimiter = new HashMap<String, String>();
        for (Iterator<String> itr = virtualRealms.iterator(); itr.hasNext();) {
            String virtualRealm = itr.next();
            String delimiter = getCoreConfiguration().getDelimiter(virtualRealm);
            virtualRealmsDelimiter.put(virtualRealm, delimiter);
        }
        //add default/UR realm to the list of realms to handle no VMM realm scenario
        if (virtualRealms.size() == 0) {
            virtualRealms = new HashSet<String>(); //if empty it will be a abstract set.So need to initialize, else will fail while adding element to it.
            virtualRealms.add(defaultRealm);
            virtualRealmsDelimiter.put(defaultRealm, defaultRealmDelimiter);
        }

        return separateIDAndRealm(inputString, defaultRealm, defaultRealmDelimiter, virtualRealms,
                                  virtualRealmsDelimiter);
    }

    protected IDAndRealm separateIDAndRealm(String inputString, String defaultRealm, String defaultRealmDelimiter,
                                            Set<?> virtualRealms, Map<?, ?> virtualRealmsDelimiter) throws WIMException {
        // initialize the return value
        IDAndRealm returnValue = null;
        // d116636
        // initialize the found flag
        boolean found = false;
        boolean realmFound = false;
        // get the set of virtual realms
        for (Iterator<?> toUse = virtualRealms.iterator(); toUse.hasNext() && !found;) {
            // initialize the buffer
            StringBuffer buffer = new StringBuffer();
            String virtualRealm = (String) toUse.next();
            String delimiter = (String) virtualRealmsDelimiter.get(virtualRealm);
            // d116636
            returnValue = new IDAndRealm();

            /*
             * Edge case. Valid virtual realms should be passed in.
             */
            if (virtualRealm != null) {
                // loop through the string and find the realm and ID
                for (int i = inputString.length() - 1; i >= 0; i--) {
                    // found a delimiter
                    if (Character.toString(inputString.charAt(i)).equals(delimiter)) {
                        // parsing the realm
                        if (!returnValue.isRealmDefined()) {
                            // the delimiter is escaped
                            if (i - 1 >= 0 && inputString.charAt(i - 1) == BACKSLASH) {
                                buffer.append(inputString.charAt(i));
                                i--;
                            }
                            // the delimiter is not escaped
                            else {
                                // true positive
                                String realm = buffer.reverse().toString();
                                if (virtualRealm.equals(realm)) {
                                    returnValue.setDelimiter(delimiter);
                                    returnValue.setRealm(realm);
                                    buffer.setLength(0);
                                    realmFound = true;
                                }
                                // false positive
                                // d116636
                                else {
                                    break;
                                }
                            }
                        }
                        // parsing the ID
                        else {
                            // the delimiter is escaped
                            if (i - 1 >= 0 && inputString.charAt(i - 1) == BACKSLASH) {
                                buffer.append(inputString.charAt(i));
                                i--;
                            }
                            // the delimiter is not escaped
                            else {
                                throw new WIMException(inputString);
                            }
                        }
                    }
                    // found a character
                    else {
                        if (i == 0 && realmFound) {
                            buffer.append(inputString.charAt(i));
                            returnValue.setId(buffer.reverse().toString());
                            found = true;
                            break;
                        } else if (i == 0) {
                            buffer.append(inputString.charAt(i));
                            returnValue.setId(buffer.reverse().toString());
                            buffer.setLength(0);
                        } else {
                            buffer.append(inputString.charAt(i));
                        }
                    }
                }
            }
        }
        // set the default realm if the realm is undefined
        // d117217
        if (!returnValue.isRealmDefined()) {
            // d116636
            returnValue.setDelimiter(defaultRealmDelimiter);
            returnValue.setRealm(defaultRealm);
        }
        // set the ID if no ID found
        // d130391
        if (!found && returnValue.getId().equals("")) {
            returnValue.setId(inputString);
        }
        return returnValue;
    }

    /**
     * Test to see if a property is an IdentifierType property.
     *
     * @param inputProperty Property to test.
     *
     * @return true if the property is an IdentifierType property, false otherwise.
     *
     * @pre inputProperty != null
     */

    public boolean isIdentifierTypeProperty(String inputProperty) {
        // initialize the return value
        boolean returnValue = false;
        // test the property
        if ((inputProperty != null)
            && ((inputProperty.equals(Service.PROP_UNIQUE_ID)) || (inputProperty.equals(Service.PROP_UNIQUE_NAME))
                || (inputProperty.equals(Service.PROP_EXTERNAL_ID)) || (inputProperty.equals(Service.PROP_EXTERNAL_NAME)))) {
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * Create a DataObject for the realm context.
     *
     * @param inputRootDataObject The root DataObject.
     * @param inputRealm          The realm.
     *
     * @pre inputRootDataObject != null
     * @pre inputRealm != null
     * @pre inputRealm != ""
     */
    protected void createRealmDataObject(Root inputRootObject, String inputRealm) {
        // use the root DataGraph to create a Context DataGraph
        List<Context> contexts = inputRootObject.getContexts();
        if (contexts != null) {
            Context ctx = new Context();
            // set "WIM.Realm" in the Context DataGraph to the realm
            ctx.setKey(Service.VALUE_CONTEXT_REALM_KEY);
            ctx.setValue(inputRealm);
            contexts.add(ctx);
        }

    }

    /**
     * Create a DataObject for the property request.
     *
     * @param inputRootDataObject The root DataObject.
     * @param inputProperty       The property to request
     *
     * @pre inputRootDataObject != null
     * @pre inputProperty != null
     * @pre inputProperty != ""
     */
    protected void createPropertyControlDataObject(Root inputRootDataObject, String inputProperty) {
        // use the root DataGraph to create a PropertyControl DataGraph
        List<Control> propertyControls = inputRootDataObject.getControls();
        PropertyControl propCtrl = null;
        if (propertyControls != null) {
            propCtrl = new PropertyControl();
            propertyControls.add(propCtrl);
        }
        // add the requested property to the return list of properties
        if (propCtrl != null) {
            propCtrl.getProperties().add(inputProperty);
        }
    }

    /**
     * Create a DataObject for the login property request.
     *
     * @param inputRootDataObject The root DataObject.
     * @param inputProperty       The property to request
     *
     * @pre inputRootDataObject != null
     * @pre inputProperty != null
     * @pre inputProperty != ""
     */
    // f113366
    protected void createLoginControlDataObject(Root inputRootDataObject, String inputProperty) {
        // use the root DataGraph to create a PropertyControl DataGraph
        List<Control> propertyControls = inputRootDataObject.getControls();
        LoginControl loginCtrl = null;
        if (propertyControls != null) {
            loginCtrl = new LoginControl();
            propertyControls.add(loginCtrl);
        }
        // add the requested property to the return list of properties
        if (loginCtrl != null) {
            loginCtrl.getProperties().add(inputProperty);
        }
    }

    /**
     * Gets an entity using an identifier attribute (ex. uniqueName, externalName).
     *
     * @param root           Root data object
     * @param inputAttrName  Input attribute name
     * @param inputAttrValue Input attribute value
     * @param outputAttrName Ouptut attribute name
     * @param mapUtils       Bridge utility instance
     * @return Entity or null if the input attribute is not an identifier type or if there are no entities
     */
    protected Root getEntityByIdentifier(Root root, String inputAttrName, String inputAttrValue, String outputAttrName, BridgeUtils mapUtils) throws WIMException {

        boolean isInputAttrIdentifier = mapUtils.isIdentifierTypeProperty(inputAttrName); //&& UniqueNameHelper.isDN(inputAttrValue) != null;
        boolean isInputAttrExternalId = inputAttrName.equals(Service.PROP_EXTERNAL_NAME);
        boolean isOutputAttrIdentifier = mapUtils.isIdentifierTypeProperty(outputAttrName);

        Root returnValue = null;

        // check if the input attribute is an identifier type
        if (isInputAttrIdentifier || hasFederatedRegistry) {
            if (!isOutputAttrIdentifier) {
                mapUtils.createPropertyControlDataObject(root, outputAttrName);
            }
            // use the root DataGraph to create a LoginAccount DataObject
            List<Entity> entities = root.getEntities();
            Entity entity = null;
            if (entities != null) {
                entity = new Entity();
                entities.add(entity);
            }
            if (entity != null) {
                IdentifierType idfType = new IdentifierType();
                if (SchemaConstants.PROP_PRINCIPAL_NAME.equalsIgnoreCase(inputAttrName) || "CN".equalsIgnoreCase(inputAttrName))
                    idfType.set(SchemaConstants.PROP_UNIQUE_NAME, inputAttrValue);
                else
                    idfType.set(inputAttrName, inputAttrValue);
                entity.setIdentifier(idfType);
            }

            // Create an external name control if the external identifier is being used
            if (isInputAttrExternalId) {
                List<Control> controls = root.getControls();
                if (controls != null)
                    controls.add(new ExternalNameControl());
            }

            // invoke ProfileService.get with the input root DataGraph
            returnValue = mapUtils.getWimService().get(root);

            if (returnValue != null && returnValue.getEntities().isEmpty()) {
                returnValue = null;
            }
        }

        return returnValue;
    }

    /**
     * Get the group depth level.
     *
     * @return Returns the group depth.
     */
    @Trivial
    protected short getGroupDepth() {
        synchronized (groupLevelLock) {
            return groupLevel;
        }
    }

    /**
     * get input securityName.
     *
     * @param input        security attribute defined in the realm.
     * @param input        security name value.
     * @param loginAccount or Group.
     *
     * @return uniqueName if the property is an IdentifierType property, principalName otherwise.
     *
     */
    protected String getRealInputAttrName(String inputAttrName, String id, boolean isUser) {
        boolean isInputAttrValueDN = UniqueNameHelper.isDN(id) != null;
        boolean isInputAttrIdentifier = isIdentifierTypeProperty(inputAttrName);

        if (!isInputAttrIdentifier && isInputAttrValueDN) {
            // To suppress the below message from coming in trace.log due to defect 94474
            /*
             * if (tc.isWarningEnabled()) {
             * //Tr.warning(tc, "the propertyForInput " + inputAttrName + " doesn't match the format of input value " + id + ", switch to uniqueName");
             * Tr.warning(tc, WIMMessageKey.INVALID_PROPERTY_VALUE_FORMAT, inputAttrName);
             * }
             */
            inputAttrName = "uniqueName";
            //take it as uniqueName because we don't know if it's externalName or uniqueName value
        } else if (isInputAttrIdentifier && !isInputAttrValueDN) {
            // if dealing with LoginAccount or Group
            if (isUser) {
                // To suppress the below message from coming in trace.log due to defect 94474
                /*
                 * if (tc.isWarningEnabled()) {
                 * //Tr.warning(tc, "the propertyForInput " + inputAttrName + " doesn't match the format of input value " + id + ", switch to principalName");
                 * Tr.warning(tc, WIMMessageKey.INVALID_PROPERTY_VALUE_FORMAT, inputAttrName);
                 * }
                 */
                inputAttrName = "principalName"; // loginaccounts
            }
            //OR
            else {
                // To suppress the below message from coming in trace.log due to defect 94474
                /*
                 * if (tc.isWarningEnabled()) {
                 * //Tr.warning(tc, "the propertyForInput " + inputAttrName + " doesn't match the format of input value " + id + ", swith to cn");
                 * Tr.warning(tc, WIMMessageKey.INVALID_PROPERTY_VALUE_FORMAT, inputAttrName);
                 * }
                 */
                inputAttrName = "cn"; // groups
            }
        }
        return inputAttrName;
    }

    /**
     * Method to return the default realm
     *
     * @return default realm Name
     */
    public String getDefaultRealmName() {
        String returnRealm = getCoreConfiguration().getConfiguredPrimaryRealmName();
        if (returnRealm == null) {
            returnRealm = urRealmName;
        }
        return returnRealm;

    }

    /**
     * @param registries
     */
    public void addFederationRegistries(List<com.ibm.ws.security.registry.UserRegistry> registries) {
        if (!registries.isEmpty()) {
            getWimService().addFederationRegistries(registries);
            hasFederatedRegistry = true;
        }
    }

    /**
     * @param registry
     */
    public void removeAllFederatedRegistries() {
        getWimService().removeAllFederatedRegistries();
        hasFederatedRegistry = false;
    }

    static void handleExceptions(Exception e) throws EntryNotFoundException, RegistryException {
        String methodName = "handleExceptions(Exception)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + e.getMessage(), e);
        }

        if (e instanceof EntityNotFoundException
            || e instanceof InvalidUniqueNameException
            || e instanceof InvalidIdentifierException
            || e instanceof EntityNotInRealmScopeException) {

            /*
             * We couldn't find the entity. Wrap to EntryNotFoundException.
             */
            throw new EntryNotFoundException(e.getMessage(), e);

        } else {
            /*
             * Wrap all other exceptions in a RegistryException.
             */
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /**
     * Get a String value for the object, or return null if the object is null.
     *
     * @param o The object to get the string value for.
     * @return The string value of the object, or null if the object is null.
     */
    public static String getStringValue(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }
}
