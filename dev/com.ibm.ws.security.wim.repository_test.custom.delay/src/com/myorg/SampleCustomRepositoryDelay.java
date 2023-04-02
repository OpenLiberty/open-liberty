/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.myorg;

import static com.ibm.wsspi.security.wim.SchemaConstants.PROP_PRINCIPAL_NAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import com.ibm.wsspi.security.wim.CustomRepository;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Abbreviated version of SampleCustomRepository. This sample adds sleeps on the
 * login and does not implement the other methods at this time. Groups
 * are also not supported. To add in support for anything messing, start by copying
 * from SampleCustomRepository to save yourself some time.
 *
 * This is intended to support the UserEnumerationTest, although could be expanded for
 * other tests.
 **/
@Component(property = { "config.id=sampleCustomRepositoryDelay" }) //config.id is required for OSGI to identify the repository service
public class SampleCustomRepositoryDelay implements CustomRepository {

    /** The repository ID. This is passed in by declarative services to the activate method. */
    private String repositoryId = null;

    /** The realm name for this repository. */
    private static final String REPOSITORY_REALM = "sampleCustomRepositoryRealm";

    /** Base entries is used for user */
    private final Map<String, String> baseEntries = new HashMap<String, String>();;

    /** In-memory users. */
    private final Map<String, UserInfo> users = new HashMap<String, UserInfo>();

    /** Base entry for this repository. This should be unique among all user registries and repositories. */
    private static final String BASE_ENTRY = "o=ibm,c=us";

    private static final String CLASS_NAME = SampleCustomRepositoryDelay.class.getName();

    @Activate
    @Modified
    protected void activate(Map<String, Object> props) {
        repositoryId = (String) props.get("config.id");

        createInitialUsersInMemory();

        initializeBaseEntries();
    }

    /**
     * @see com.ibm.wsspi.security.wim.CustomRepository#getRepositoryBaseEntries()
     */
    @Override
    public Map<String, String> getRepositoryBaseEntries() {
        return baseEntries;
    }

    /**
     * Get the realm name for this repository. This value should be unique.
     *
     * @see com.ibm.wsspi.security.wim.Repository#getRealm()
     */
    @Override
    public String getRealm() {
        return REPOSITORY_REALM;
    }

    /**
     * Gets are not supported for this test repository.
     *
     * To add in support, start by copying from SampleCustomRepository.
     *
     * @see com.ibm.wsspi.security.wim.Repository#get(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root get(Root inRoot) throws WIMException {
        throw new IllegalStateException("get method not implemented");
    }

    /**
     * Searches are not supported for this test repository.
     *
     * To add in support, start by copying from SampleCustomRepository.
     *
     * @see com.ibm.wsspi.security.wim.Repository#search(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root search(Root inRoot) throws WIMException {
        throw new IllegalStateException("search method not implemented");
    }

    /**
     * Authenticate the account data object in the specified root object.
     *
     * @see com.ibm.wsspi.security.wim.Repository#login(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root login(Root inRoot) throws WIMException {
        System.out.println(CLASS_NAME + " <login> entry, inRoot: \n" + inRoot.toString());
        Root outRoot = new Root();

        /*
         * Get the entity trying to login.
         */
        final List<Entity> entities = inRoot.getEntities();
        LoginAccount person = (LoginAccount) entities.get(0);
        UserInfo userInfo = null;

        /*
         * Authenticate the user. This sample supports user/password-based authentication.
         */

        /*
         * LoginAccount has the principal and password.
         */
        String principalName = person.getPrincipalName();
        System.out.println(CLASS_NAME + "     principalName: " + principalName);

        /*
         * Retrieve and validate the password.
         */
        byte[] inPassword = person.getPassword();
        if ((inPassword == null) || (inPassword.length == 0)) {
            throw new PasswordCheckFailedException("MISSING_OR_EMPTY_PASSWORD", "The password is missing or empty.");
        }

        if ((principalName == null) || (principalName.length() == 0)) {
            throw new PasswordCheckFailedException("MISSING_OR_EMPTY_PRINCIPAL_NAME", "The principal name is missing or empty.");
        }

        userInfo = users.get(principalName);

        /*
         * If we found the user, continue the login process. If we don't have the user, return the Root object
         * with no entity.
         */
        if (userInfo == null) {
            throw new PasswordCheckFailedException("PASSWORD_CHECKED_FAILED", "The password verification for the '" + principalName
                                                                              + "' principal name failed.");

        }
        System.out.println(CLASS_NAME + " <login> sleep before password check for test purposes");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println(CLASS_NAME + " Sleep was interrupted");
        }

        if (!userInfo.checkPassword(inPassword)) {
            throw new PasswordCheckFailedException("PASSWORD_CHECKED_FAILED", "The password verification for the '" + principalName
                                                                              + "' principal name failed.");
        }

        /*
         * The user was successfully authenticated if we have user info. Populate the output entity.
         */
        if (userInfo != null) {
            /*
             * Get the Login Control
             */
            Map<String, Control> ctrlMap = getControlMap(inRoot);

            /*
             * The login control has the properties requested to return.
             */
            LoginControl loginCtrl = (LoginControl) ctrlMap.get(SchemaConstants.DO_LOGIN_CONTROL);
            if (loginCtrl == null) {
                loginCtrl = new LoginControl();
            }
            List<String> propertyNames = loginCtrl.getProperties();
            System.out.println(CLASS_NAME + "     propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

            /*
             * NOTE:: UniqueName is set to principalName in this example. This may not always be the case
             */
            PersonAccount loginPerson = createPersonObject(userInfo);

            /*
             * Set the requested properties of user.
             */
            populateUser(loginPerson, userInfo, propertyNames);

            /*
             * Add it to the return;
             */
            outRoot.getEntities().add(loginPerson);
        }

        System.out.println(CLASS_NAME + " <login> exit, outRoot: \n" + outRoot);

        return outRoot;
    }

    /**
     * Creates are not supported for this test repository
     *
     * To add in support, start by copying from SampleCustomRepository.
     *
     * @see com.ibm.wsspi.security.wim.Repository#create(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root create(Root inRoot) throws WIMException {
        throw new IllegalStateException("create method not implemented");
    }

    /**
     * Deletes are not supported for this test repository
     *
     * To add in support, start by copying from SampleCustomRepository.
     *
     * @see com.ibm.wsspi.security.wim.Repository#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root delete(Root inRoot) throws WIMException {
        throw new IllegalStateException("delete method not implemented");
    }

    /**
     * Updates are not supported for this test repository
     *
     * To add in support, start by copying from SampleCustomRepository.
     *
     * @see com.ibm.wsspi.security.wim.Repository#update(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root update(Root inRoot) throws WIMException {
        throw new IllegalStateException("update method not implemented");
    }

    /**
     * Save the user's information to memory.
     *
     * @param userName     The user's name.
     * @param password     The user's password.
     * @param externalName The user's external name.
     * @param uniqueName   The user's unique name.
     * @param groups       The groups this user is a member of.
     * @param props        The properties for this user.
     */
    private void saveUserInfoInMemory(String userName, byte[] password, String externalName, String uniqueName, Map<String, Object> props) {
        UserInfo userInfo = new UserInfo(userName, password, externalName, uniqueName, props);
        users.put(userName, userInfo);
        System.out.println(CLASS_NAME + "     In memory users size: " + users.size());
    }

    /**
     * Create a {@link PersonAccount} instance to return to the caller.
     *
     * @param externalName The external name of the user.
     * @param uniqueName   The unique name of the user.
     * @return The new {@link PersonAccount} instance.
     */
    private PersonAccount createPersonObject(String externalName, String uniqueName) {
        PersonAccount person = new PersonAccount();
        IdentifierType identifier = createIdentifier(externalName, uniqueName);
        person.setIdentifier(identifier);
        return person;
    }

    /**
     * Create a {@link PersonAccount} instance to return to the caller.
     *
     * @param userInfo The {@link UserInfo} to create the new {@link PersonAccount} instance with.
     * @return The new {@link PersonAccount} instance.
     */
    private PersonAccount createPersonObject(UserInfo userInfo) {
        return createPersonObject(userInfo.getExternalName(), userInfo.getUniqueName());
    }

    /**
     * Get the common name ('cn') from a unique name.
     *
     * @param uniqueName The unique name to extract the common name ('cn') from.
     * @return The common name ('cn').
     * @throws EntityNotFoundException If the uniqueName was not a valid distinguished name.
     */
    private String getCn(String uniqueName) throws EntityNotFoundException {
        String cn = null;
        LdapName ldapName = null;
        try {
            ldapName = new LdapName(uniqueName);
        } catch (InvalidNameException e) {
            throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + uniqueName + "' entity was not found.", e);
        }

        Rdn rdn = ldapName.getRdn(ldapName.size() - 1);
        cn = (String) rdn.getValue();

        System.out.println(CLASS_NAME + "     cn: " + cn);
        return cn;
    }

    private IdentifierType createIdentifier(String externalName, String uniqueId) {
        IdentifierType identifier = new IdentifierType();
        identifier.setRepositoryId(repositoryId);
        identifier.set("realm", REPOSITORY_REALM);
        identifier.setExternalName(externalName);
        identifier.setUniqueName(uniqueId);
        identifier.setExternalId(uniqueId);

        System.out.println(CLASS_NAME + "     identifier: \n" + identifier.toString());
        return identifier;
    }

    private static String getContextProperty(Root root, String propertyName) {
        String result = "";
        List<Context> contexts = root.getContexts();
        for (Context context : contexts) {
            String key = context.getKey();
            if (key != null && key.equals(propertyName)) {
                result = (String) context.getValue();
                break;
            }
        }
        return result;
    }

    private void populateUser(PersonAccount personObject, UserInfo userInfo, List<String> requestPropNames) {
        Map<String, Object> userInfoProps = userInfo.getProps();
        if (userInfoProps == null || userInfoProps.isEmpty()) { //nothing to populate
            System.out.println(CLASS_NAME + "                 No property to populate");
            return;
        }
        System.out.println("<populateUser>, userInfoProps: " + userInfoProps.toString());
        System.out.println("                requestPropNames: " + (requestPropNames == null || requestPropNames.isEmpty() ? "<null>" : requestPropNames.toString()));

        if (requestPropNames == null || requestPropNames.isEmpty() || requestPropNames.get(0).equals("*")) { //populate all properties from user info
            Set<String> props = userInfo.getProps().keySet();
            doPopulatePerson(personObject, userInfoProps, props);
        } else { // populate only requested properties
            doPopulatePerson(personObject, userInfoProps, new HashSet<String>(requestPropNames));
        }
    }

    private void doPopulatePerson(PersonAccount personObject, Map<String, Object> userInfoProps, Set<String> requestPropNames) {

        System.out.println(CLASS_NAME + " <doPopulatePerson>, entry, personObject: \n" + personObject.toString());
        System.out.println("    userInfoProps: " + userInfoProps.toString());
        System.out.println("    requestPropNames: " + requestPropNames.toString());

        for (String propName : requestPropNames) {
            Object propValue = userInfoProps.get(propName);
            if (propValue != null) {
                if (propValue instanceof String) {
                    personObject.set(propName, propValue);
                } else if (propValue instanceof List<?> && !((List<?>) propValue).isEmpty()) {
                    personObject.set(propName, propValue);
                } else {
                    System.out.println(CLASS_NAME + "     un-supported property value type: " + requestPropNames.toString());
                }
            }
        }

        System.out.println(CLASS_NAME + " <doPopulatePerson>, exit, personObject: \n" + personObject.toString());
    }

    private static Map<String, Control> getControlMap(Root root) {
        Map<String, Control> ctrlMap = new HashMap<String, Control>();
        List<Control> controls = root.getControls();
        if (controls != null) {
            for (int i = 0; i < controls.size(); i++) {
                Control control = controls.get(i);
                String type = control.getTypeName();
                if (ctrlMap.get(type) == null) {
                    ctrlMap.put(type, control);
                }
            }
        }
        return ctrlMap;
    }

    /**
     * Create an administrative user
     */
    private void createInitialUsersInMemory() {
        System.out.println(CLASS_NAME + " <createInitialUsersAndGroupsInMemory> entry");

        final String ADMIN_USER_NAME = "adminUser";
        final String ADMIN_USER_DN = "cn=" + ADMIN_USER_NAME + "," + BASE_ENTRY;
        final String ADMIN_USER_PASSWORD = "adminUserpwd";

        /*
         * Create the administrative user.
         */
        Map<String, Object> userProps = new HashMap<String, Object>();
        userProps.put(PROP_PRINCIPAL_NAME, ADMIN_USER_NAME);
        userProps.put("cn", ADMIN_USER_NAME);
        saveUserInfoInMemory(ADMIN_USER_NAME, ADMIN_USER_PASSWORD.getBytes(), ADMIN_USER_DN, ADMIN_USER_DN, userProps);

    }

    /**
     * Initialize the base entries.
     */
    private void initializeBaseEntries() {
        baseEntries.put(BASE_ENTRY, BASE_ENTRY);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.wim.CustomRepository#getRepositoriesForGroups()
     */
    @Override
    public String[] getRepositoriesForGroups() {
        // TODO Auto-generated method stub
        return null;
    }
}
