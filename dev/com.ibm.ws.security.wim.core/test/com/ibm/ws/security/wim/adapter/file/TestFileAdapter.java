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
package com.ibm.ws.security.wim.adapter.file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.wim.BaseRepository;
import com.ibm.ws.security.wim.ConfiguredRepository;
import com.ibm.ws.security.wim.util.ControlsHelper;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.InvalidArgumentException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.GroupMemberControl;
import com.ibm.wsspi.security.wim.model.GroupMembershipControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.security.wim.adapter.file.config",
           property = "service.vendor=IBM",
           reference = @Reference(bind = "setSecurityService", unbind = "unsetSecurityService", service = SecurityService.class, name = BaseRepository.KEY_SECURITY_SERVICE))
public class TestFileAdapter extends BaseRepository implements ConfiguredRepository {

    private final FileData data = new FileData();

    static final String REALM = "realm";
    String reposRealm = null;
    private static final TraceComponent tc = Tr.register(TestFileAdapter.class, "wimUtil", "com.ibm.ws.security.wim.util.resources.WimUtilMessages");

    @Override
    @Activate
    public void activate(Map<String, Object> properties, ComponentContext cc) {
        reposRealm = (String) properties.get(REALM);
        super.activate(properties, cc);
    }

    @Override
    @Modified
    protected void modify(Map<String, Object> properties) {
        reposRealm = (String) properties.get(REALM);
        super.modify(properties);
    }

    @Override
    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        super.deactivate(reason, cc);
    }

    @Override
    public Root get(Root inRoot) throws WIMException {
        final Root root = new Root();

        if (data != null) {
            Entity entity = inRoot.getEntities().get(0);
            String uniqueName = entity.getIdentifier().getUniqueName();
            String expr = null;
            String type = entity.getTypeName();

            if (uniqueName == null)
                return null;

            if (uniqueName.startsWith("uid")) {
                type = "PersonAccount";
                int index = uniqueName.indexOf(',', 4);
                expr = "uid='" + uniqueName.substring(4, index) + "'";
            } else {
                type = "Group";
                int index = uniqueName.indexOf(',', 3);
                expr = "cn='" + uniqueName.substring(3, index) + "'";
            }

            if ("PersonAccount".equalsIgnoreCase(type)) {
                try {
                    PersonAccount dataEntity = (PersonAccount) data.get("PersonAccount", expr);
                    if (dataEntity != null) {
                        dataEntity.getIdentifier().setRepositoryId(reposId);
                        dataEntity.setPrincipalName(dataEntity.getUid()); //workaround to set principalName for UR API getUserSecurityName with default mapping
                        root.getEntities().add(dataEntity);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else if ("Group".equalsIgnoreCase(type)) {
                try {
                    final Entity dataEntity = data.get("Group", expr);
                    if (dataEntity != null) {
                        dataEntity.getIdentifier().setRepositoryId(reposId);
                        root.getEntities().add(dataEntity);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }

            Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inRoot);
            GroupMemberControl grpMbrCtrl = (GroupMemberControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBER_CONTROL);
            GroupMembershipControl grpMbrshipCtrl = (GroupMembershipControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBERSHIP_CONTROL);

            // Get the group members, get the requested properties of the members
            if (grpMbrCtrl != null) {
                Group group = (Group) root.getEntities().get(0);
                int level = grpMbrCtrl.getLevel();
                if (level < 0) {
                    throw new InvalidArgumentException(WIMMessageKey.INVALID_LEVEL_IN_CONTROL, WIMMessageKey.INVALID_LEVEL_IN_CONTROL);
                }

                if (level == 0) {
                    HashSet<String> knownMembers = new HashSet<String>();
                    List<Entity> members = group.getMembers();
                    for (Entity member : members) {
                        knownMembers.add(member.getIdentifier().getUniqueName());
                    }

                    getGroupMembers(group, level, knownMembers);
                }
            }

            // Gets the groups the entity belongs to
            if (grpMbrshipCtrl != null) {
                Entity dataEntity = root.getEntities().get(0);
                int level = grpMbrshipCtrl.getLevel();
                if (level < 0) {
                    throw new InvalidArgumentException(WIMMessageKey.INVALID_LEVEL_IN_CONTROL, WIMMessageKey.INVALID_LEVEL_IN_CONTROL);
                }

                getGroupMembership(dataEntity, level, new HashSet<String>());
            }
        }

        return root;
    }

    private ArrayList<Group> getGroupMembership(Entity dataEntity, int level, HashSet<String> groups) {
        ArrayList<Group> groupList = new ArrayList<Group>();

        if (dataEntity != null) {
            IdentifierType id = dataEntity.getIdentifier();
            String uniqueName = id.getUniqueName();

            // Get the groups that directly contain this uniqueName
            String searchExpression = "members/identifier/@uniqueName='" + uniqueName + "'";

            try {
                List<Entity> entities = data.search("Group", searchExpression, true, true);
                if (entities != null) {
                    for (Entity entity : entities) {
                        if (!groups.contains(entity.getIdentifier().getUniqueName())) {
                            groups.add(entity.getIdentifier().getUniqueName());
                            dataEntity.getGroups().add((Group) entity);
                            groupList.add((Group) entity);
                        }
                    }
                }

                // If nested then further explore.
                if (level == 0) {
                    ArrayList<Group> current = new ArrayList<Group>();
                    current.addAll(groupList);
                    ArrayList<Group> newGroups = new ArrayList<Group>();
                    boolean foundNewGroups = true;

                    if (foundNewGroups) {
                        foundNewGroups = false;
                        for (Group group : current) {
                            id = group.getIdentifier();
                            uniqueName = id.getUniqueName();

                            // Get the groups that directly contain this uniqueName
                            searchExpression = "members/identifier/@uniqueName='" + uniqueName + "'";
                            entities = data.search("Group", searchExpression, true, true);
                            if (entities != null) {
                                for (Entity entity : entities) {
                                    if (!groups.contains(entity.getIdentifier().getUniqueName())) {
                                        groups.add(entity.getIdentifier().getUniqueName());
                                        dataEntity.getGroups().add((Group) entity);
                                        newGroups.add((Group) entity);
                                    }
                                }
                            }
                        }

                        if (newGroups.size() > 0) {
                            foundNewGroups = true;
                            current.clear();
                            current.addAll(newGroups);
                            newGroups.clear();
                        } else {
                            foundNewGroups = false;
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return groupList;
    }

    private void getGroupMembers(Group group, int level, HashSet<String> knownMembers) {
        if (knownMembers != null) {
            String type = null;
            String expr = null;
            HashSet<String> newMembers = new HashSet<String>();
            HashSet<String> currentMembers = new HashSet<String>();
            currentMembers.addAll(knownMembers);

            boolean findNewMembers = true;
            if (findNewMembers) {
                for (String uniqueName : currentMembers) {
                    if (uniqueName.startsWith("uid")) {
                        type = "PersonAccount";
                        int index = uniqueName.indexOf(',', 4);
                        expr = "uid='" + uniqueName.substring(4, index) + "'";
                    } else {
                        type = "Group";
                        int index = uniqueName.indexOf(',', 3);
                        expr = "cn='" + uniqueName.substring(3, index) + "'";
                    }

                    if ("Group".equalsIgnoreCase(type)) {
                        try {
                            Group dataEntity = (Group) data.get("Group", expr);
                            group.getMembers().addAll(dataEntity.getMembers());
                            List<Entity> innerMembers = dataEntity.getMembers();
                            for (Entity innerMember : innerMembers) {
                                if (!currentMembers.contains(innerMember.getIdentifier().getUniqueName()) && !newMembers.contains(innerMember.getIdentifier().getUniqueName()))
                                    newMembers.add(innerMember.getIdentifier().getUniqueName());
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }

                if (newMembers.size() > 0) {
                    findNewMembers = true;
                    knownMembers.addAll(newMembers);
                    currentMembers.clear();
                    currentMembers.addAll(newMembers);
                    newMembers.clear();
                } else {
                    findNewMembers = false;
                }
            }
        }
    }

    @Override
    public Root login(Root inRoot) throws WIMException {
        Root root = new Root();

        if (data != null) {

            LoginAccount person = (LoginAccount) inRoot.getEntities().get(0);
            String principalName = person.getPrincipalName();
            byte[] password = person.getPassword();

            if ((password == null) || (password.length == 0)) {
                throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PASSWORD, WIMMessageKey.MISSING_OR_EMPTY_PASSWORD);
            }

            if ((principalName == null) || (principalName.length() == 0)) {
                throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME);
            }

            String searchExpr = "uid='" + principalName + "'";

            try {
                List<Entity> entities = data.search("PersonAccount", searchExpr, true, true);
                if (entities.size() > 1) {
                    throw new PasswordCheckFailedException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND);
                }
                if (entities.size() == 1) {

                    Entity result = entities.get(0);
//                    if (principalName != null) {
                    // set the principalName to incoming principalName
                    result.set("principalName", principalName);
                    entities.remove(0);
                    entities.add(result); //add it back
//                    }

                    root.getEntities().addAll(entities);
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".search", "294");
            }
        }

        return root;
    }

    @Override
    public Root search(Root inRoot) throws WIMException {
        Root root = new Root();

        if (data != null) {
            Map<String, Control> ctrlMap = ControlsHelper.getControlMap(inRoot);
            // PageControl and SortControl are ignored
            SearchControl searchControl = (SearchControl) ctrlMap.get(SchemaConstants.DO_SEARCH_CONTROL);
            String searchExpr = searchControl.getExpression();
            boolean pricipalName = false;

            if (searchExpr == null || searchExpr.length() == 0) {
                throw new SearchControlException(WIMMessageKey.MISSING_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.MISSING_SEARCH_EXPRESSION,
                                                                                                           (Object[]) null));
            }

            if (searchExpr.contains("LoginAccount"))
                searchExpr = searchExpr.replace("LoginAccount", "PersonAccount");

            if (searchExpr.contains("principalName")) {
                searchExpr = searchExpr.replace("principalName", "uid");
                pricipalName = true;
            }

            // System.out.println("Searching for .. " + searchControl.getExpression() + " to get " + searchControl.getProperties());

            // Extract the entity type.
            int startIndex = searchExpr.indexOf("@xsi:type='");
            if (startIndex < 0)
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           (Object[]) null));

            int endIndex = searchExpr.indexOf('\'', startIndex + 11);

            if (endIndex <= 0 || endIndex < startIndex)
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           (Object[]) null));

            String entityType = searchExpr.substring(startIndex + 11, endIndex);

            // Extract the rest of the criteria.
            String criteria = "cn=*";
            if (searchExpr.length() > endIndex + 1) {
                String criteriaSubString = searchExpr.substring(endIndex + 1);

                if (criteriaSubString.startsWith(" and")) {
                    criteria = criteriaSubString.substring(4);
                }

                if (criteria.endsWith("]") && !criteria.startsWith("[")) {
                    criteria = criteria.substring(0, criteria.length() - 1);
                }
            }

            try {
                List<Entity> entities = data.search(entityType, criteria, true, true);
                if (pricipalName) {
                    for (Entity entity : entities) {
                        ((PersonAccount) entity).setPrincipalName(((PersonAccount) entity).getUid());
                    }
                }
                root.getEntities().addAll(entities);
            } catch (Exception e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".search", "375");
            }
        }

        return root;
    }

    @Override
    public String getRealm() {
        return reposRealm;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.Repository#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root delete(Root root) throws WIMException {

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.Repository#create(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root create(Root root) throws WIMException {

        return root;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.wim.Repository#update(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root update(Root root) throws WIMException {

        return null;
    }
}
