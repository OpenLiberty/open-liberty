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
package componenttest.aries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.Cell;
import com.ibm.websphere.simplicity.OperationResults;
import com.ibm.websphere.simplicity.Topology;
import com.ibm.websphere.simplicity.commands.authorizationgroup.MapUsersToAdminRole;
import com.ibm.websphere.simplicity.config.WIMUserAndGroupConfiguration;
import com.ibm.websphere.simplicity.config.securitydomain.JAASJ2CAuthenticationEntrySettings;
import com.ibm.websphere.simplicity.config.securitydomain.UserRealmType;
import com.ibm.websphere.simplicity.config.usersgroups.Group;
import com.ibm.websphere.simplicity.config.usersgroups.User;
import com.ibm.ws.topology.helper.SecurityConfigurator;
import com.ibm.ws.topology.helper.UserRealmConfigurator;
import com.ibm.ws.topology.helper.impl.WASSecurityConfigurator;

/**
 *
 */
public class LibertySecurityConfigurator implements SecurityConfigurator {

    private static final Logger _logger = Logger.getLogger(WASSecurityConfigurator.class.getName());

    private boolean adminSecurity = getSwitchDefault("aries.componenttest.security", false);
    private boolean appSecurity = getSwitchDefault("aries.componenttest.app.security", false);

    // No nonsense: every cell must have the same administrator!
    private final String primaryAdmin = getDefaultUser();
    private final String primaryAdminPassword = getDefaultPassword();

    private UserRealmType realmType = UserRealmType.WIMUserRegistry;
    private final WIMConfigurator wimConfigurator = new WIMConfigurator();

    private final List<JAASJ2CAuthenticationEntrySettings> j2cAuthData = new ArrayList<JAASJ2CAuthenticationEntrySettings>();

    private static boolean getSwitchDefault(String propertyName, boolean defaultValue) {
        boolean result = defaultValue;
        if (System.getProperty(propertyName) != null) {
            result = System.getProperty(propertyName).equalsIgnoreCase("true");
        }

        _logger.fine("Property '" + propertyName + "' -> " + result);
        return result;
    }

    public static String getDefaultUser() {
        return Topology.getCells().get(0).getConnInfo().getUser();
    }

    public static String getDefaultPassword() {
        return Topology.getCells().get(0).getConnInfo().getPassword();
    }

    /** {@inheritDoc} */
    @Override
    public SecurityConfigurator adminSecurity(boolean enabled) {
        adminSecurity = enabled;
        return this;
    }

    @Override
    public SecurityConfigurator appSecurity(boolean enabled) {
        appSecurity = enabled;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void apply() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public SecurityConfigurator j2cAuthData(String alias, String user, String password) {
        JAASJ2CAuthenticationEntrySettings entry = new JAASJ2CAuthenticationEntrySettings();
        entry.setAlias(alias);
        entry.setUser(user);
        entry.setPassword(password);
        entry.setDescription("No description :)");
        j2cAuthData.add(entry);

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityConfigurator java2Security(boolean arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public SecurityConfigurator localOSRealm() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public UserRealmConfigurator wimUserRealm() {
        realmType = UserRealmType.WIMUserRegistry;
        return wimConfigurator;
    }

    private class WIMConfigurator implements UserRealmConfigurator {
        private class UserInfo {
            String uid;
            String password;
            String[] groups;
            String[] roles;

            public UserInfo(String uid, String password, String[] groups, String[] roles) {
                this.groups = groups;
                this.uid = uid;
                this.password = password;
                this.roles = roles;
            }
        }

        private final List<UserInfo> users = new ArrayList<UserInfo>();
        private final Set<String> groups = new HashSet<String>();
        private final Map<String, String[]> roles = new HashMap<String, String[]>();

        @Override
        public SecurityConfigurator end() {
            return LibertySecurityConfigurator.this;
        }

        @Override
        public UserRealmConfigurator group(String cn) {
            groups.add(cn);
            return this;
        }

        @Override
        public UserRealmConfigurator user(String uid, String password, String... groups) {
            users.add(new UserInfo(uid, password, groups, new String[0]));
            return this;
        }

        @Override
        public UserRealmConfigurator role(String role, String... users) {
            roles.put(role, users);
            return this;
        }

        public void setupUsersAndGroups(Cell cell) throws Exception {
            WIMUserAndGroupConfiguration conf = cell.getSecurityConfiguration().getWIMUserAndGroupConfiguration();
            cleanup(conf);

            Map<String, Group> groupsByName = new HashMap<String, Group>();

            // work-around for NullPointer bug, fill the group cache by doing a lookup
            conf.getGroupByCommonName("random");

            for (String group : groups) {
                OperationResults<Group> gres = conf.createGroup(group, "No description :)");
                groupsByName.put(group, gres.getResult());
            }

            // work-around for NullPointer bug, fill the group cache by doing a lookup
            conf.getUserByUserID("random");

            for (UserInfo user : users) {
                OperationResults<User> ures =
                                conf.createUser(user.uid, user.password, user.uid + "first", user.uid + "last", user.uid + "@aries.ibm.com");
                User u = ures.getResult();
                for (String groupName : user.groups) {
                    Group group = groupsByName.get(groupName);
                    if (group != null) {
                        group.addNewUserMember(u.getUniqueName());
                    } else {
                        throw new IllegalStateException("Group " + groupName + " for user " + user.uid + " is not defined.");
                    }
                }
            }

            for (Map.Entry<String, String[]> entry : roles.entrySet()) {
                String role = entry.getKey();
                String[] userIds = entry.getValue();
                MapUsersToAdminRole roleTask = new MapUsersToAdminRole(role, userIds);

                roleTask.run(cell);
            }
        }

        private void cleanup(WIMUserAndGroupConfiguration conf) throws Exception {
            Set<User> users = conf.searchUsersByUserID("*", 300000, null);
            for (User u : users) {
                if (!!!u.getUid().equals(primaryAdmin))
                    conf.deleteUser(u.getUniqueName());
            }

            Set<Group> groups = conf.searchGroups(true, "*", 1000, null);
            for (Group g : groups) {
                conf.deleteGroup(g.getUniqueName());
            }
        }
    }
}
