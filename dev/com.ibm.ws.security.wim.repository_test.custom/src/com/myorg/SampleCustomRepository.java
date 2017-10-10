/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.myorg;

import static com.ibm.wsspi.security.wim.SchemaConstants.PROP_PRINCIPAL_NAME;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.ibm.wsspi.security.wim.exception.CertificateMapFailedException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityTypeNotSupportedException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.AncestorControl;
import com.ibm.wsspi.security.wim.model.CacheControl;
import com.ibm.wsspi.security.wim.model.CheckGroupMembershipControl;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.DescendantControl;
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

/**
 * The main purpose of this sample is to demonstrate the use of the
 * custom user repository feature. This sample is a memory-based repository
 * where the users and the groups information is stored in memory as {@link HashMap}
 * of {@link UserInfo} and {@link GroupInfo} object.
 * The UserInfo have group name, password, uniqueId, groups and properties.
 * The GroupInfo have group name, members and properties. In this sample code, we have to create default user that have the admin role.
 *
 * <p/>
 * As such, simplicity - not performance - was a major factor. This
 * sample should be used only to get familiarized with this feature. An
 * actual implementation of a realistic repository should consider various
 * factors like performance, scalability, thread safety, and so on.
 *
 * <p/>
 * This sample demonstrates the following:
 * <ol>
 * <li>How to get and create a user and group.</li>
 * <li>The update method just a replacement of user or group</li>
 * <li>The search method do not utilize the search filter</li>
 * </ol>
 **/
@Component(property = { "config.id=sampleCustomRepository" }) //config.id is required for OSGI to identify the repository service
public class SampleCustomRepository implements CustomRepository {

    /** The repository ID. This is passed in by declarative services to the activate method. */
    private String repositoryId = null;

    /** The realm name for this repository. */
    private static final String REPOSITORY_REALM = "sampleCustomRepositoryRealm";

    /** Base entries is used for user or group */
    private final Map<String, String> baseEntries = new HashMap<String, String>();;

    /** In-memory users. */
    private final Map<String, UserInfo> users = new HashMap<String, UserInfo>();

    /** In-memory groups. */
    private final Map<String, GroupInfo> groups = new HashMap<String, GroupInfo>();

    /** Base entry for this repository. This should be unique among all user registries and repositories. */
    private static final String BASE_ENTRY = "o=ibm,c=us";

    @Activate
    @Modified
    protected void activate(Map<String, Object> props) {
        repositoryId = (String) props.get("config.id");

        createInitialUsersAndGroupsInMemory();

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
     * @see com.ibm.wsspi.security.wim.CustomRepository#getRepositoriesForGroups()
     */
    @Override
    public String[] getRepositoriesForGroups() {
        return null;
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
     * Return information of the specified entities.
     * <p/>
     * The following controls can be passed into a get() call:
     * <p/>
     * <ul>
     * <li>{@link AncestorControl}</li>
     * <li>{@link CacheControl}</li>
     * <li>{@link CheckGroupMembershipControl}</li>
     * <li>{@link DescendantControl}</li>
     * <li>{@link GroupMembershipControl}</li>
     * <li>{@link GroupMemberControl}</li>
     * <li>{@link PropertyControl}</li>
     * </ul>
     * <p/>
     * More than one entity and more than one control can be passed into this call.
     *
     * @see com.ibm.wsspi.security.wim.Repository#get(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root get(Root inRoot) throws WIMException {
        System.out.println("<get> entry, inRoot: \n" + inRoot);
        Root outRoot = new Root();

        /*
         * Extract the controls
         */
        Map<String, Control> controls = getControls(inRoot);
        PropertyControl propertyCtrl = (PropertyControl) controls.get(SchemaConstants.DO_PROPERTY_CONTROL);
        GroupMemberControl grpMbrCtrl = (GroupMemberControl) controls.get(SchemaConstants.DO_GROUP_MEMBER_CONTROL);
        GroupMembershipControl grpMbrshipCtrl = (GroupMembershipControl) controls.get(SchemaConstants.DO_GROUP_MEMBERSHIP_CONTROL);
        CheckGroupMembershipControl checkGrpMbrshpCtrl = (CheckGroupMembershipControl) controls.get(SchemaConstants.DO_CHECK_GROUP_MEMBERSHIP_CONTROL);
        AncestorControl ancestorCtrl = (AncestorControl) controls.get(SchemaConstants.DO_ANCESTOR_CONTROL);
        DescendantControl descendantCtrl = (DescendantControl) controls.get(SchemaConstants.DO_DESCENDANT_CONTROL);
        CacheControl cacheCtrl = (CacheControl) controls.get(SchemaConstants.DO_CACHE_CONTROL);

        /*
         * Throw exceptions for unsupported controls.
         */
        if (ancestorCtrl != null) {
            throw new WIMException("AncestorControl is not supported.");
        }
        if (checkGrpMbrshpCtrl != null) {
            throw new WIMException("CheckGroupMembershipControl is not supported.");
        }
        if (descendantCtrl != null) {
            throw new WIMException("DescendantControl is not supported.");
        }

        /*
         * If present, a CacheControl specifies to clear the cache before continuing with
         * this request.
         */
        if (cacheCtrl != null) {
            String cacheMode = cacheCtrl.getMode();
            if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEARALL.equalsIgnoreCase(cacheMode)) {
                /*
                 * If there was a cache, invalidate the caches here.
                 */
            } else if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEAR_ENTITY.equalsIgnoreCase(cacheMode)) {
                for (Entity entity : inRoot.getEntities()) {
                    /*
                     * If there was a cache, invalidate the cache entries for the specified entity here.
                     */
                }
            }
        }

        /*
         * If present, a PropertyControl lists the properties that have been requested for this operation.
         */
        List<String> propertyNames = null;
        if (propertyCtrl != null) {
            propertyNames = propertyCtrl.getProperties();
        }
        System.out.println("    propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

        for (Entity entity : inRoot.getEntities()) {
            /*
             * Extract the uniqueName for the entity requested.
             *
             * The repository could support using any property to get the user. The
             * sample supports only 'uniqueName'.
             */
            String uniqueName = entity.getIdentifier().getUniqueName();
            System.out.println("    uniqueName: " + uniqueName);
            if (uniqueName == null) {
                throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + uniqueName + "' entity was not found.");
            }
            String cn = getCn(uniqueName);

            /*
             * Determine if the request is for a user or group.
             */
            String entityType = entity.getTypeName();
            System.out.println("    entityType: " + entityType.toString());

            if (SchemaConstants.DO_PERSON_ACCOUNT.equalsIgnoreCase(entityType) || SchemaConstants.DO_LOGIN_ACCOUNT.equalsIgnoreCase(entityType)) {
                UserInfo userInfo = users.get(cn);
                if (userInfo == null) {
                    throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + cn + "' entity was not found.");
                }

                PersonAccount person = createPersonObject(userInfo);
                populateUser(person, userInfo, propertyNames);

                /*
                 * If present, a GroupMembershipControl has the details, that need to be fetched,
                 * of groups which have this entity as a member.
                 */
                if (grpMbrshipCtrl != null) { // Get all groups for this user uniqueId be long to
                    System.out.println("        grpMbrshipCtrl not null, cn = " + cn);
                    List<String> grpMbrshipCtrlPropertyNames = grpMbrshipCtrl.getProperties();
                    List<String> groupNames = userInfo.getGroups();
                    if (groupNames != null && !groupNames.isEmpty()) {
                        for (String groupName : groupNames) {
                            GroupInfo groupInfo = groups.get(groupName);
                            if (groupInfo != null) {
                                Group groupObject = createGroupObject(groupInfo);
                                populateGroup(groupObject, groupInfo, grpMbrshipCtrlPropertyNames);
                                person.getGroups().add(groupObject);
                            }
                        }
                    }
                }

                outRoot.getEntities().add(person);
            } else if (SchemaConstants.DO_GROUP.equalsIgnoreCase(entityType)) {

                /*
                 * If present, a GroupMemberControl has the details, that need to be fetched, of users
                 * and groups that are members of this entity. This control is only applicable when the
                 * requested entity is a Group.
                 */
                if (grpMbrCtrl != null) { // Get all members belong to this group uniqueId
                    GroupInfo groupInfo = groups.get(cn);
                    if (groupInfo == null) {
                        throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + cn + "' entity was not found.");
                    }
                    List<String> grpMbrCtrlPropertyNames = grpMbrCtrl.getProperties();
                    System.out.println("    grpMbrCtrl propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));
                    Group groupObject = createGroupObject(groupInfo);
                    populateGroup(groupObject, groupInfo, propertyNames);

                    List<String> members = groupInfo.getMembers();
                    if (members != null && !members.isEmpty()) {
                        for (String member : members) {
                            UserInfo userInfo = users.get(member);
                            if (userInfo != null) {
                                PersonAccount person = createPersonObject(userInfo);
                                populateUser(person, userInfo, grpMbrCtrlPropertyNames);
                                groupObject.getMembers().add(person);
                            }
                        }
                    }
                    outRoot.getEntities().add(groupObject);

                }
                outRoot.getEntities().add(entity);
            } else {
                if (users.get(cn) != null) {
                    PersonAccount person = createPersonObject(users.get(cn));
                    populateUser(person, users.get(cn), propertyNames);
                    outRoot.getEntities().add(person);
                } else if (groups.get(cn) != null) {
                    Group groupObject = createGroupObject(groups.get(cn));
                    populateGroup(groupObject, groups.get(cn), propertyNames);
                    outRoot.getEntities().add(groupObject);
                }
            }
        }

        System.out.println("<get> exit, outRoot: \n" + outRoot.toString());
        return outRoot;
    }

    /**
     * Search the profile repositories for entities matching the given search
     * expression and returns them with the requested properties.
     *
     * @see com.ibm.wsspi.security.wim.Repository#search(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root search(Root inRoot) throws WIMException {
        System.out.println("<search> entry, inRoot: \n" + inRoot.toString());
        Root outRoot = new Root();

        /*
         * Extract the controls.
         */
        Map<String, Control> ctrlMap = getControlMap(inRoot);
        System.out.println("    ctrlMap: " + ctrlMap.toString());

        /*
         * SearchControl contains the details of search like the search expression and properties requested.
         */
        SearchControl searchControl = (SearchControl) ctrlMap.get(SchemaConstants.DO_SEARCH_CONTROL);
        String searchExpr = searchControl.getExpression();
        List<String> propertyNames = searchControl.getProperties();
        System.out.println("    propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

        /*
         * NOTE:: Do not use PageControl or SortControl in the repository. The paging and sorting is handled
         * automatically.
         */

        if (searchExpr == null || searchExpr.length() == 0) {
            throw new SearchControlException("MISSING_SEARCH_EXPRESSION", "The expression property is missing from the SearchControl data object.");
        }
        String inputPattern = getContextProperty(inRoot, SchemaConstants.USE_USER_FILTER_FOR_SEARCH);

        /*
         * Check if user filter pattern is specified
         */
        if (inputPattern != null && inputPattern.length() > 0) {
            System.out.println("        inputPattern is user: " + inputPattern);
            UserInfo userInfo = users.get(inputPattern);
            if (userInfo != null) {
                PersonAccount person = createPersonObject(userInfo);
                populateUser(person, userInfo, propertyNames);
                outRoot.getEntities().add(person);
            }
        } else {
            /*
             * Check if group filter pattern is specified
             */
            inputPattern = getContextProperty(inRoot, SchemaConstants.USE_GROUP_FILTER_FOR_SEARCH);
            System.out.println("        inputPattern is group: " + inputPattern);
            if (inputPattern != null && inputPattern.length() > 0) {
                GroupInfo groupInfo = groups.get(inputPattern);
                if (groupInfo != null) {
                    Group group = createGroupObject(groupInfo);
                    populateGroup(group, groupInfo, propertyNames);
                    outRoot.getEntities().add(group);
                }
            } else {
                Context context = inRoot.getContexts().get(0);
                System.out.println("    context property " + context.toString() + "is not supported");
            }
        }

        System.out.println("<search> exit, outRoot: \n" + outRoot.toString());
        return outRoot;
    }

    /**
     * Authenticate the account data object in the specified root object.
     *
     * @see com.ibm.wsspi.security.wim.Repository#login(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root login(Root inRoot) throws WIMException {
        System.out.println("<login> entry, inRoot: \n" + inRoot.toString());
        Root outRoot = new Root();

        /*
         * Get the entity trying to login.
         */
        final List<Entity> entities = inRoot.getEntities();
        LoginAccount person = (LoginAccount) entities.get(0);
        UserInfo userInfo = null;

        /*
         * Authenticate the user. This sample supports both certificate-based
         * authentication and user/password-based authentication.
         */
        List<byte[]> certificates = person.getCertificate();
        if (!certificates.isEmpty()) {
            /*
             * The LoginAccount has a certificate to use for authentication.
             */
            X509Certificate certificate = null;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(certificates.get(0));
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                certificate = (X509Certificate) cf.generateCertificate(bais);
                bais.close();
            } catch (Exception e) {
                throw new CertificateMapFailedException("CERTIFICATE_MAP_FAILED", "Mapping of certificate failed.", e);
            }

            /*
             * For this sample, we expect the subject principal to have a DN where the 'cn'
             * is in the first RDN.
             */
            String principalName = getCn(certificate.getSubjectX500Principal().getName());
            System.out.println("    principalName: " + principalName);

            userInfo = users.get(principalName);

        } else {

            /*
             * LoginAccount has the principal and password.
             */
            String principalName = person.getPrincipalName();
            System.out.println("    principalName: " + principalName);

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
            if (userInfo != null && !userInfo.checkPassword(inPassword)) {
                throw new PasswordCheckFailedException("PASSWORD_CHECKED_FAILED", "The password verification for the '" + principalName
                                                                                  + "' principal name failed.");
            }
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
            System.out.println("    propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

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

        System.out.println("<login> exit, outRoot: \n" + outRoot);

        return outRoot;
    }

    /**
     * Creates the entities under the given root data object.
     *
     * @see com.ibm.wsspi.security.wim.Repository#create(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root create(Root inRoot) throws WIMException {
        System.out.println("<create> entry, inRoot: \n" + inRoot);

        Root outRoot = new Root();

        for (Entity entity : inRoot.getEntities()) {
            IdentifierType identity = entity.getIdentifier();
            String typeName = entity.getTypeName();

            /*
             * If external name is specified, directly use it to create user. Unique name is ignored.
             */
            String externalName = identity.getExternalName();
            String uniqueName = externalName;
            if (externalName == null || externalName.length() == 0) {
                uniqueName = identity.getUniqueName();
            }
            System.out.println("    uniqueName: " + uniqueName);
            String cn = getCn(uniqueName);

            /*
             * Get all the property names that belong to this entity.
             */
            List<String> propertyNames = castList(String.class, Entity.getPropertyNames(typeName));
            System.out.println("    propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

            List<String> memberList = new ArrayList<String>();
            List<String> groupList = new ArrayList<String>();

            getMembersAndGroups(entity, propertyNames, memberList, groupList);
            System.out.println("    memberList: " + memberList.toString() + " groupList: " + groupList.toString());

            if (SchemaConstants.DO_PERSON_ACCOUNT.equalsIgnoreCase(typeName)) {
                byte[] password = (byte[]) entity.get(SchemaConstants.PROP_PASSWORD);
                HashMap<String, Object> personProps = processPersonProperties(entity);

                PersonAccount person = createPersonObject(externalName, uniqueName);
                outRoot.getEntities().add(person);
                saveUserInfoInMemory(cn, password, externalName, uniqueName, groupList, personProps);
            }

            if (SchemaConstants.DO_GROUP.equalsIgnoreCase(typeName)) {
                HashMap<String, Object> groupProps = processGroupProperties(entity);

                Group group = createGroupObject(externalName, uniqueName);
                outRoot.getEntities().add(group);

                saveGroupInfoInMemory(cn, externalName, uniqueName, memberList, groupProps);
            }
        }

        System.out.println("<create> exit, outRoot: \n" + outRoot);
        return outRoot;
    }

    /**
     * Get members and/or groups for the requested entity.
     *
     * @param entity The {@link Entity} object containing the membership information.
     * @param propertyNames The property names for the entity.
     * @param memberList The returned list of members. Should be passed in as non-null.
     * @param groupList The returned list of groups. Should be passed in as non-null.
     * @throws WIMException If there was an error parsing the unique names for the 'cn' of the groups or members.
     */
    private void getMembersAndGroups(final Entity entity, List<String> propertyNames, List<String> memberList, List<String> groupList) throws WIMException {
        if (propertyNames != null) {
            for (String propertyName : propertyNames) {

                if (SchemaConstants.DO_MEMBERS.equals(propertyName)) {
                    /*
                     * The members list contains members on a group.
                     */
                    List<Entity> members = ((Group) entity).getMembers();
                    System.out.println("<getMemberAndGroupListFromPropertyNames>, request members: " + members.toString());
                    for (Entity member : members) {
                        String uniqueName = member.getIdentifier().getUniqueName();
                        String cn = getCn(uniqueName);
                        memberList.add(cn);
                    }
                } else if (SchemaConstants.DO_GROUPS.equals(propertyName)) {
                    /*
                     * The groups list contains groups the user or group is a member of.
                     */
                    List<Group> groups = entity.getGroups();
                    System.out.println("<getMemberAndGroupListFromPropertyNames>, request groups: " + groups.toString());
                    for (Group group : groups) {
                        String uniqueName = group.getIdentifier().getUniqueName();
                        String cn = getCn(uniqueName);
                        groupList.add(cn);
                    }
                }
            }
        }

        System.out.println("<getMemberAndGroupListFromPropertyNames> exit, \n memberList: " + memberList.toString() + " groupList: " + groupList.toString());
    }

    /**
     * Delete the entity specified in the root object.
     *
     * @see com.ibm.wsspi.security.wim.Repository#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root delete(Root inRoot) throws WIMException {
        System.out.println("<delete> entry, inRoot: \n" + inRoot);

        Root outRoot = new Root();

        for (Entity entity : inRoot.getEntities()) {
            String typeName = entity.getTypeName();
            IdentifierType identity = entity.getIdentifier();

            String externalName = identity.getExternalName();
            System.out.println("    externalName: " + externalName);

            String uniqueName = identity.getUniqueName();
            System.out.println("    uniqueName: " + uniqueName);

            String cn = getCn(uniqueName);

            List<String> propertyNames = castList(String.class, Entity.getPropertyNames(typeName));
            System.out.println("    propertyNames: " + (propertyNames != null ? propertyNames.toString() : "<null>"));

            if (SchemaConstants.DO_PERSON_ACCOUNT.equalsIgnoreCase(typeName) || SchemaConstants.DO_LOGIN_ACCOUNT.equalsIgnoreCase(typeName)) {
                List<Group> groups = null;
                List<Entity> members = null;
                if (propertyNames != null) {
                    for (String propertyName : propertyNames) {
                        if (SchemaConstants.DO_MEMBERS.equals(propertyName)) {
                            members = ((Group) entity).getMembers();
                            System.out.println("    members: " + members.toString());
                        } else if (SchemaConstants.DO_GROUPS.equals(propertyName)) {
                            groups = entity.getGroups();
                            System.out.println("    groups: " + groups.toString());
                        }
                    }
                }
                removeEntity(typeName, cn);
                PersonAccount person = createPersonObject(externalName, uniqueName);
                outRoot.getEntities().add(person);
            } else if (SchemaConstants.DO_GROUP.equalsIgnoreCase(typeName)) {
                removeEntity(typeName, cn);
                Group group = createGroupObject(externalName, uniqueName);
                outRoot.getEntities().add(group);
            } else {
                throw new EntityTypeNotSupportedException("ENTITY_TYPE_NOT_SUPPORTED", "The entity type '" + typeName + "' is not supported.");
            }
        }

        System.out.println("<delete> exit, outRoot: \n" + outRoot);
        return outRoot;
    }

    /**
     * Updates entity specified in the root object. Input root object could
     * contain a ChangeSummary that can be used to access the change history for
     * any object in the datagraph.
     * <br>
     * Input root object could also contain GroupMembershipControl or
     * GroupMemberControl, which can be used to change the group membership:
     * <ol>
     * <li>To add an entity to a Group, the caller can add the entity
     * dataobject with "groups" property and GroupMembershipControl
     * to the root dataobject.</li>
     * <li>To remove an entity from a Group, the "modifyMode" property of the
     * GroupMembershipControl will be set to "3".</li>
     * </ol>
     *
     * @see com.ibm.wsspi.security.wim.Repository#update(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root update(Root inRoot) throws WIMException {
        System.out.println("<update> entry, inRoot: \n" + inRoot);

        for (Entity entity : inRoot.getEntities()) {
            String typeName = entity.getTypeName();

            IdentifierType identity = entity.getIdentifier();

            /*
             * If external name is specified, directly use it to update entry.
             */
            String dn = identity.getExternalName();
            String uniqueName = dn;
            if (dn == null || dn.length() == 0) {
                uniqueName = identity.getUniqueName();
            }
            System.out.println("    uniqueName: " + uniqueName);
            String cn = getCn(uniqueName);

            if (SchemaConstants.DO_PERSON_ACCOUNT.equalsIgnoreCase(typeName)) {
                users.remove(cn);
            } else if (SchemaConstants.DO_GROUP.equalsIgnoreCase(typeName)) {
                groups.remove(cn);
            } else {
                throw new EntityTypeNotSupportedException("ENTITY_TYPE_NOT_SUPPORTED", "The entity type '" + typeName + "' is not supported.");
            }
        }
        Root outRoot = this.create(inRoot);

        System.out.println("<update> exit, outRoot: \n" + outRoot);
        return outRoot;
    }

    /**
     * Retrieve the list of allowed properties for the user or group. The returned value
     * will return all properties from the entity as well as all extended properties.
     *
     * @param entity The entity to get the allowed properties for.
     * @param isPerson Whether this call is for a {@link PersonAccount}.
     * @return All of the allowed properties.
     */
    private static List<String> allowPropNames(Entity entity, boolean isPerson) {
        List<String> allowProps = new ArrayList<String>();
        if (isPerson) {
            allowProps.addAll(castList(String.class, PersonAccount.getPropertyNames(null)));
            Set<String> extendedPropNames = ((PersonAccount) entity).getExtendedPropertyNames();
            allowProps.addAll(extendedPropNames);
        } else {
            allowProps.addAll(castList(String.class, Group.getPropertyNames(null)));
            Set<String> extendedPropNames = ((Group) entity).getExtendedPropertyNames();
            allowProps.addAll(extendedPropNames);
        }
        return allowProps;
    }

    /**
     * Save the user's information to memory.
     *
     * @param userName The user's name.
     * @param password The user's password.
     * @param externalName The user's external name.
     * @param uniqueName The user's unique name.
     * @param groups The groups this user is a member of.
     * @param props The properties for this user.
     */
    private void saveUserInfoInMemory(String userName, byte[] password, String externalName, String uniqueName, List<String> groups, Map<String, Object> props) {
        UserInfo userInfo = new UserInfo(userName, password, externalName, uniqueName, groups, props);
        users.put(userName, userInfo);
        System.out.println("    In memory users size: " + users.size());
    }

    /**
     * Save the group's information to memory.
     *
     * @param cn The group's common name.
     * @param externalName The group's external name.
     * @param uniqueName The group's unique name.
     * @param members The users that are a member of this group.
     * @param props The properties for this group.
     */
    private void saveGroupInfoInMemory(String cn, String externalName, String uniqueName, List<String> members, Map<String, Object> props) {
        GroupInfo groupInfo = new GroupInfo(cn, externalName, uniqueName, members, props);
        groups.put(cn, groupInfo);
        System.out.println("   In memory groups size: " + groups.size());
    }

    /**
     * Create a {@link Group} instance to return to the caller.
     *
     * @param externalName The external name of the group.
     * @param uniqueName The unique name of the group.
     * @return The new {@link Group} instance.
     */
    private Group createGroupObject(String externalName, String uniqueName) {
        Group group = new Group();
        IdentifierType identifier = createIdentifier(externalName, uniqueName);
        group.setIdentifier(identifier);
        return group;
    }

    /**
     * Create a {@link Group} instance to return to the caller.
     *
     * @param groupInfo The {@link GroupInfo} to create the new {@link Group} instance with.
     * @return The new {@link Group} instance.
     */
    private Group createGroupObject(GroupInfo groupInfo) {
        return createGroupObject(groupInfo.getExternalName(), groupInfo.getUniqueName());
    }

    /**
     * Create a {@link PersonAccount} instance to return to the caller.
     *
     * @param externalName The external name of the user.
     * @param uniqueName The unique name of the user.
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

        System.out.println("    cn: " + cn);
        return cn;
    }

    /**
     * Remove a user or group entity from the repository.
     *
     * @param entypeType The entity type to remove.
     * @param cn The 'cn' for the entity to remove.
     * @throws EntityNotFoundException If the entity does not exist.
     */
    private void removeEntity(String entityType, String cn) throws EntityNotFoundException {
        if (SchemaConstants.DO_GROUP.equals(entityType)) {
            if (groups.get(cn) != null) {
                groups.remove(cn);
            } else {
                throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + cn + "' entity was not found.");
            }

            /*
             * Remove references to the group by the user.
             */
            for (UserInfo ui : users.values()) {
                ui.getGroups().remove(cn);
            }
        } else {
            if (users.get(cn) != null) {
                users.remove(cn);
            } else {
                throw new EntityNotFoundException("ENTITY_NOT_FOUND", "The '" + cn + "' entity was not found.");
            }

            /*
             * Remove references to the user by the group.
             */
            for (GroupInfo gi : groups.values()) {
                gi.getMembers().remove(cn);
            }
        }
    }

    private IdentifierType createIdentifier(String externalName, String uniqueId) {
        IdentifierType identifier = new IdentifierType();
        identifier.setRepositoryId(repositoryId);
        identifier.set("realm", REPOSITORY_REALM);
        identifier.setExternalName(externalName);
        identifier.setUniqueName(uniqueId);
        identifier.setExternalId(uniqueId);

        System.out.println("    identifier: \n" + identifier.toString());
        return identifier;
    }

    private Map<String, Control> getControls(Root root) {
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
            System.out.println("                No property to populate");
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

    private void populateGroup(Group groupObject, GroupInfo groupInfo, List<String> requestPropNames) {
        Map<String, Object> groupInfoProps = groupInfo.getProps();
        if (groupInfoProps == null || groupInfoProps.isEmpty()) { //nothing to populate
            System.out.println("                No property to populate");
            return;
        }

        System.out.println("<populateGroup>, groupInfoProps: " + groupInfoProps.toString());
        System.out.println("                 requestPropNames: " + (requestPropNames == null || requestPropNames.isEmpty() ? "<null>" : requestPropNames.toString()));

        if (requestPropNames == null || requestPropNames.isEmpty() || requestPropNames.get(0).equals("*")) { //populate all properties from group info
            Map<String, Object> props = groupInfo.getProps();
            if (props != null && !props.isEmpty()) {
                Set<String> propKeys = groupInfo.getProps().keySet();
                doPopulateGroup(groupObject, groupInfoProps, propKeys);
            }
        } else {
            doPopulateGroup(groupObject, groupInfoProps, new HashSet<String>(requestPropNames));
        }

        System.out.println("    groupObject: \n" + groupObject.toString());
    }

    private void doPopulatePerson(PersonAccount personObject, Map<String, Object> userInfoProps, Set<String> requestPropNames) {

        System.out.println("<doPopulatePerson>, entry, personObject: \n" + personObject.toString());
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
                    System.out.println("    un-support property value type: " + requestPropNames.toString());
                }
            }
        }

        System.out.println("<doPopulatePerson>, exit, personObject: \n" + personObject.toString());
    }

    private void doPopulateGroup(Group groupObject, Map<String, Object> groupInfoProps, Set<String> requestPropNames) {
        System.out.println("<doPopulateGroup>, entry, groupObject: \n" + groupObject.toString());
        System.out.println("    groupInfoProps: " + groupInfoProps.toString());
        System.out.println("    requestPropNames: " + requestPropNames.toString());

        for (String propName : requestPropNames) {
            Object propValue = groupInfoProps.get(propName);
            if ((!propName.equals("members") && propValue != null)) {
                if (propValue instanceof String) {
                    groupObject.set(propName, propValue);
                } else if (propValue instanceof List<?> && !((List<?>) propValue).isEmpty()) {
                    groupObject.set(propName, propValue);
                } else {
                    System.out.println("    un-support property value type: " + requestPropNames.toString());
                }
            }
        }
        System.out.println("<doPopulateGroup>, exit, groupObject: \n" + groupObject.toString());
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

    private static HashMap<String, Object> processPersonProperties(Entity entity) {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        List<String> allowProps = allowPropNames(entity, true);
        if (allowProps != null) {
            PersonAccount personObject = (PersonAccount) entity;
            for (String propName : allowProps) {
                Object propValue = personObject.get(propName);
                System.out.println("        property name: " + propName + " value: " + propValue);
                if (propValue != null && propValue.toString().length() != 0) {
                    attrs.put(propName, propValue);
                }
            }
        }

        System.out.println("<processPersonProperties>, exit, attrs: " + attrs.toString());
        return attrs;
    }

    private static HashMap<String, Object> processGroupProperties(Entity entity) {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        List<String> allowProps = allowPropNames(entity, false);
        if (allowProps != null) {
            Group groupObject = (Group) entity;
            for (String propName : allowProps) {
                Object propValue = groupObject.get(propName);
                System.out.println("        property name: " + propName + " value: " + propValue);
                if (propValue != null && propValue.toString().length() != 0) {
                    attrs.put(propName, propValue);
                }
            }
        }

        System.out.println("<processGroupProperties>, exit, attrs: " + attrs.toString());
        return attrs;
    }

    /**
     * Create an administrative user and group where the administrative user is a member of the administrative
     * group.
     */
    private void createInitialUsersAndGroupsInMemory() {
        System.out.println("<createInitialUsersAndGroupsInMemory> entry");

        final String ADMIN_USER_NAME = "adminUser";
        final String ADMIN_USER_DN = "cn=" + ADMIN_USER_NAME + "," + BASE_ENTRY;
        final String ADMIN_USER_PASSWORD = "adminUserpwd";

        final String ADMIN_GROUP_NAME = "adminGroup";
        final String ADMIN_GROUP_DN = "cn=" + ADMIN_GROUP_NAME + "," + BASE_ENTRY;

        /*
         * Create the administrative user.
         */
        Map<String, Object> userProps = new HashMap<String, Object>();
        userProps.put(PROP_PRINCIPAL_NAME, ADMIN_USER_NAME);
        userProps.put("cn", ADMIN_USER_NAME);
        List<String> groups = Arrays.asList(new String[] { ADMIN_GROUP_NAME });
        saveUserInfoInMemory(ADMIN_USER_NAME, ADMIN_USER_PASSWORD.getBytes(), ADMIN_USER_DN, ADMIN_USER_DN, groups, userProps);

        /*
         * Create the administrative group.
         */
        Map<String, Object> groupProps = new HashMap<String, Object>();
        groupProps.put("cn", ADMIN_GROUP_NAME);
        List<String> members = Arrays.asList(new String[] { ADMIN_USER_NAME });
        saveGroupInfoInMemory(ADMIN_GROUP_NAME, ADMIN_GROUP_DN, ADMIN_GROUP_DN, members, groupProps);

        System.out.println("<createInitialUsersAndGroupsInMemory> exit");
    }

    /**
     * Initialize the base entries.
     */
    private void initializeBaseEntries() {
        baseEntries.put(BASE_ENTRY, BASE_ENTRY);
    }

    /**
     * Cast the generic collection as a list in a type safe manner. This will throw a {@link ClassCastException} if
     *
     * @param clazz
     * @param collection
     * @return
     * @throws ClassCastException If the collection contained an item that is not of type T.
     */
    private static <T> List<T> castList(Class<? extends T> clazz, Collection<?> collection) {
        List<T> result = new ArrayList<T>(collection.size());
        for (Object item : collection) {
            result.add(clazz.cast(item));
        }
        return result;
    }
}
