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

package com.ibm.ws.messaging.security.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.security.MSTraceConstants;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.messaging.security.MessagingSecurityException;
import com.ibm.ws.messaging.security.MessagingSecurityService;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationService;
import com.ibm.ws.messaging.security.authentication.internal.MessagingAuthenticationServiceImpl;
import com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService;
import com.ibm.ws.messaging.security.authorization.internal.MessagingAuthorizationServiceImpl;
import com.ibm.ws.messaging.security.beans.Permission;
import com.ibm.ws.messaging.security.beans.QueuePermission;
import com.ibm.ws.messaging.security.beans.TemporaryDestinationPermission;
import com.ibm.ws.messaging.security.beans.TopicPermission;
import com.ibm.ws.messaging.security.utility.MessagingSecurityUtility;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.sib.utils.ras.SibTr;

/**
 * Implementation class for the MessagingSecurity component. It exposes two services
 * MessagingAuthenticationService and MessagingAuthorizationService
 */
@Component(configurationPid = "com.ibm.ws.messaging.security",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class MessagingSecurityServiceImpl implements MessagingSecurityService, ConfigurationListener {

    // Trace component for the MessagingSecurityService Implementation class
    private static final TraceComponent tc = SibTr.register(
                                                            MessagingSecurityServiceImpl.class,
                                                            MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
                                                            MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package name, used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.internal.MessagingSecurityServiceImpl";

    // Liberty Security Service
    private SecurityService securityService = null;

    // Messaging Authentication Service
    private MessagingAuthenticationService sibAuthenticationService = null;

    // Messaging Authorization Service
    private MessagingAuthorizationService sibAuthorizationService = null;

    // Liberty Configuration Admin Service
    private ConfigurationAdmin configAdmin = null;

    private Map<String, Object> properties;

    private final Set<String> pids = new HashSet<String>();

    // Map to hold the Queue Permissions
    private Map<String, QueuePermission> queuePermissions;

    // Map to hold the destination and there permission
    private Map<String, TemporaryDestinationPermission> temporaryDestinationPermissions;

    // Map to hold the Queue Permissions
    // The Key for this Map will be TopicSpace/Topic
    private Map<String, TopicPermission> topicPermissions;

    private final RuntimeSecurityService runtimeSecurityService = RuntimeSecurityService.SINGLETON_INSTANCE;

    private String bundleLocation;

    /**
     * Method to activate Messaging Security component
     * 
     * @param properties : Map containing service & config properties populated/provided by config admin
     * @throws MessagingSecurityException
     */
    @Activate
    protected void activate(BundleContext ctx, Map<String, Object> properties) {

        SibTr.entry(tc, CLASS_NAME + "activate", properties);

        this.properties = properties;
        this.bundleLocation = ctx.getBundle().getLocation();
        populateDestinationPermissions();
        runtimeSecurityService.modifyMessagingServices(this);

        SibTr.exit(tc, CLASS_NAME + "activate");

    }

    /**
     * Called by OSGI framework when there is a modification in server.xml for tag associated with this component
     * 
     * @param cc
     *            Component Context object
     * @param properties
     *            Properties for this component from server.xml
     * 
     */
    @Modified
    protected void modify(ComponentContext cc, Map<String, Object> properties) {
        SibTr.entry(tc, CLASS_NAME + "modify", properties);

        this.properties = properties;
        populateDestinationPermissions();
        runtimeSecurityService.modifyMessagingServices(this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "modify");
        }
    }

    /**
     * Called by OSGI framework when the feature is removed from server.xml
     * 
     * @param reason
     *            int representation of reason the component is stopping
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        SibTr.entry(tc, CLASS_NAME + "deactivate", context);

        runtimeSecurityService.modifyMessagingServices(null);
        queuePermissions = null;
        topicPermissions = null;
        temporaryDestinationPermissions = null;
        sibAuthenticationService = null;
        sibAuthorizationService = null;
        this.bundleLocation = null;
        
        SibTr.exit(tc, CLASS_NAME + "deactivate");
    }

    /**
     * Binding Security Service
     * 
     * @param reference
     */
    @Reference
    protected void setSecurityService(SecurityService securityService) {
        SibTr.entry(tc, CLASS_NAME + "setSecurityService", securityService);

        this.securityService = securityService;

        SibTr.exit(tc, CLASS_NAME + "setSecurityService");
    }

    /**
     * Unbinding Security Service
     * 
     * @param reference
     */
    protected void unsetSecurityService(SecurityService securityService) {
        SibTr.entry(tc, CLASS_NAME + "unsetSecurityService", securityService);

        SibTr.exit(tc, CLASS_NAME + "unsetSecurityService");
    }

    /**
     * Binding the Configuration Admin service
     * 
     * @param reference
     */
    @Reference
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        SibTr.entry(tc, CLASS_NAME + "setConfigAdmin", configAdmin);

        this.configAdmin = configAdmin;

        SibTr.exit(tc, CLASS_NAME + "setConfigAdmin");
    }

    /**
     * Unbinding the Configuration Admin service
     * 
     * @param reference
     */
    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        SibTr.entry(tc, CLASS_NAME + "unsetConfigAdmin", configAdmin);

        SibTr.exit(tc, CLASS_NAME + "unsetConfigAdmin");
    }

    /**
     * Get Security Service from Liberty
     * 
     * @return
     */
    public SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * Get Messaging Authentication Service
     */
    @Override
    public MessagingAuthenticationService getMessagingAuthenticationService() {
        SibTr.entry(tc, CLASS_NAME + "getMessagingAuthenticationService");

        if (sibAuthenticationService == null) {
            sibAuthenticationService = new MessagingAuthenticationServiceImpl(this);
        }

        SibTr.exit(tc, CLASS_NAME + "getMessagingAuthenticationService", sibAuthenticationService);
        return sibAuthenticationService;
    }

    /**
     * Get Messaging Authorization Service
     */
    @Override
    public MessagingAuthorizationService getMessagingAuthorizationService() {
        SibTr.entry(tc, CLASS_NAME + "getMessagingAuthorizationService");

        if (sibAuthorizationService == null) {
            sibAuthorizationService = new MessagingAuthorizationServiceImpl(this);
        }

        SibTr.exit(tc, CLASS_NAME + "getMessagingAuthorizationService", sibAuthorizationService);
        return sibAuthorizationService;
    }

    @Override
    public String getUniqueUserName(Subject subject)
                    throws MessagingSecurityException {
        return MessagingSecurityUtility.getUniqueUserName(subject);
    }

    @Override
    public boolean isUnauthenticated(Subject subject) throws Exception {
        // Code added to query the User Registry about the existence of User
        // If UserRegistry is not available, it will make the User is not authenticated
        String uniqueUserName = getUniqueUserName(subject);
        getUserRegistry().getUserSecurityName(uniqueUserName);
        return MessagingSecurityUtility.isUnauthenticated(subject);
    }

    /**
     * Get User Registry from the Liberty Security component
     * 
     * @return UserRegistry
     */
    public UserRegistry getUserRegistry() {
        SibTr.entry(tc, CLASS_NAME + "getUserRegistry");

        UserRegistry userRegistry = null;
        if (getSecurityService() != null) {
            UserRegistryService userRegistryService = securityService
                            .getUserRegistryService();
            try {
                if (userRegistryService.isUserRegistryConfigured()) {
                    userRegistry = userRegistryService.getUserRegistry();
                } else {
                    MessagingSecurityException mse = new MessagingSecurityException();
                    FFDCFilter.processException(mse, CLASS_NAME + ".getUserRegistry", "1005", this);
                    SibTr.exception(tc, mse);
                    SibTr.error(tc, "USER_REGISTRY_NOT_CONFIGURED_MSE1005");
                }
            } catch (RegistryException re) {
                MessagingSecurityException mse = new MessagingSecurityException(re);
                FFDCFilter.processException(mse, CLASS_NAME + ".getUserRegistry", "1006", this);
                SibTr.exception(tc, mse);
                SibTr.error(tc, "USER_REGISTRY_EXCEPTION_MSE1006");
            }
        }

        SibTr.exit(tc, CLASS_NAME + "getUserRegistry", userRegistry);
        return userRegistry;
    }

    /**
     * @return the queuePermissions
     */
    public Map<String, QueuePermission> getQueuePermissions() {
        return queuePermissions;
    }

    /**
     * @return the temporaryDestinationPermissions
     */
    public Map<String, TemporaryDestinationPermission> getTemporaryDestinationPermissions() {
        return temporaryDestinationPermissions;
    }

    /**
     * @return the topicPermissions
     */
    public Map<String, TopicPermission> getTopicPermissions() {
        return topicPermissions;
    }

    /**
     * Populate the DestinationPermissions map with the destination and there access list
     */
    private void populateDestinationPermissions() {
        SibTr.entry(tc, CLASS_NAME + "populateDestinationPermissions", properties);

        pids.clear();
        String[] roles = (String[]) properties
                        .get(MessagingSecurityConstants.ROLE);
        initializeMaps();
        if (roles != null) {
            checkIfRolesAreUnique(roles);
            for (String role : roles) {
                Dictionary<String, Object> roleProperties = getDictionaryObject(role);
                Set<String> users = null;
                Set<String> groups = null;
                // Get the list of Users
                users = createUserOrGroupSet(roleProperties,
                                             MessagingSecurityConstants.USER);
                // Get the list of Groups
                groups = createUserOrGroupSet(roleProperties,
                                              MessagingSecurityConstants.GROUP);
                if (roleProperties != null) {
                    populateQueuePermissions(roleProperties, users, groups);
                    populateTemporarayDestinationPermissions(roleProperties, users, groups);
                    populateTopicPermissions(roleProperties, users, groups);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            SibTr.debug(tc, CLASS_NAME + " ***** Queue Permissions *****");
            printDestinationPermissions(queuePermissions);
            SibTr.debug(tc, CLASS_NAME + " ***** Topic Permissions *****");
            printDestinationPermissions(topicPermissions);
            SibTr.debug(tc, CLASS_NAME + " ***** Temporary DestinationPermissions *****");
            printDestinationPermissions(temporaryDestinationPermissions);
        }

        SibTr.exit(tc, CLASS_NAME + "populateDestinationPermissions");
    }

    private void initializeMaps() {
        if (queuePermissions != null) {
            queuePermissions.clear();
        }
        else {
            queuePermissions = new ConcurrentHashMap<String, QueuePermission>();
        }
        if (topicPermissions != null) {
            topicPermissions.clear();
        }
        else {
            topicPermissions = new ConcurrentHashMap<String, TopicPermission>();
        }
        if (temporaryDestinationPermissions != null) {
            temporaryDestinationPermissions.clear();
        }
        else {
            temporaryDestinationPermissions = new ConcurrentHashMap<String, TemporaryDestinationPermission>();
        }
    }

    private void populateQueuePermissions(Dictionary<String, Object> roleProperties, Set<String> users, Set<String> groups) {

        String[] tempPermissions = (String[]) roleProperties
                        .get(MessagingSecurityConstants.QUEUE_PERMISSION);
        if (tempPermissions != null) {
            for (String tempPermission : tempPermissions) {
                QueuePermission permission = createQueuePermission(tempPermission, users, groups);
                if (permission != null)
                    queuePermissions.put(permission.getQueueReference(), permission);
            }
        }
    }

    /**
     * @param tempPermission
     * @param users
     * @param groups
     * @return
     */
    private QueuePermission createQueuePermission(String tempPermission, Set<String> users, Set<String> groups) {
        SibTr.entry(tc, CLASS_NAME + "createQueuePermission", new Object[] { tempPermission, users, groups });
        QueuePermission permission = null;
        String queueRef = null;
        Dictionary<String, Object> permissionProperties = getDictionaryObject(tempPermission);
        if (permissionProperties != null) {
            queueRef = (String) permissionProperties
                            .get(MessagingSecurityConstants.QUEUE_REF);
            String[] actionArray = (String[]) permissionProperties
                            .get(MessagingSecurityConstants.ACTION);
            permission = queuePermissions.get(queueRef);
            if (permission == null) {
                permission = new QueuePermission();
                permission.setQueueReference(queueRef);
            }
            permission.addUserAndGroupsToRole(actionArray, users, groups);
        }
        SibTr.exit(tc, CLASS_NAME + "createQueuePermission", permission);
        return permission;
    }

    private void populateTopicPermissions(Dictionary<String, Object> roleProperties, Set<String> users, Set<String> groups) {
        String[] tempPermissions = (String[]) roleProperties
                        .get(MessagingSecurityConstants.TOPIC_PERMISSION);
        if (tempPermissions != null) {
            for (String tempPermission : tempPermissions) {
                TopicPermission permission = createTopicPermission(tempPermission, users, groups);
                if (permission != null) {
                    String topicSpace = permission.getTopicSpaceName();
                    String topicName = permission.getTopicName();
                    String key = getTopicPermissionKey(topicSpace, topicName);
                    topicPermissions.put(key, permission);
                }
            }
        }
    }

    /**
     * @param tempPermission
     * @param users
     * @param groups
     * @return
     */
    private TopicPermission createTopicPermission(String tempPermission, Set<String> users, Set<String> groups) {
        SibTr.entry(tc, CLASS_NAME + "createTopicPermission", new Object[] { tempPermission, users, groups });
        TopicPermission permission = null;
        String topicSpace = null;
        String topic = null;
        Dictionary<String, Object> permissionProperties = getDictionaryObject(tempPermission);
        if (permissionProperties != null) {
            topic = (String) permissionProperties
                            .get(MessagingSecurityConstants.TOPIC_NAME);
            topicSpace = (String) permissionProperties.get(MessagingSecurityConstants.TOPIC_SPACE);
            String[] actionArray = (String[]) permissionProperties
                            .get(MessagingSecurityConstants.ACTION);
            String key = getTopicPermissionKey(topicSpace, topic);
            permission = topicPermissions.get(key);
            if (permission == null) {
                permission = new TopicPermission();
                permission.setTopicName(topic);
                permission.setTopicSpaceName(topicSpace);
            }
            permission.addUserAndGroupsToRole(actionArray, users, groups);
        }
        SibTr.exit(tc, CLASS_NAME + "createTopicPermission", permission);
        return permission;
    }

    private void populateTemporarayDestinationPermissions(Dictionary<String, Object> roleProperties, Set<String> users, Set<String> groups) {

        String[] tempPermissions = (String[]) roleProperties
                        .get(MessagingSecurityConstants.TEMPORARY_DESTINATION_PERMISSION);
        if (tempPermissions != null) {
            for (String tempPermission : tempPermissions) {
                TemporaryDestinationPermission permission = createTemporaryDestinationPermission(tempPermission, users, groups);
                if (permission != null)
                    temporaryDestinationPermissions.put(permission.getPrefix(), permission);
            }
        }
    }

    /**
     * @param tempPermission
     * @param users
     * @param groups
     * @return
     */
    private TemporaryDestinationPermission createTemporaryDestinationPermission(String tempPermission, Set<String> users,
                                                                                Set<String> groups) {
        SibTr.entry(tc, CLASS_NAME + "createTemporaryDestinationPermission", new Object[] { tempPermission, users, groups });
        TemporaryDestinationPermission permission = null;
        String prefix = null;
        Dictionary<String, Object> permissionProperties = getDictionaryObject(tempPermission);
        if (permissionProperties != null) {
            prefix = (String) permissionProperties
                            .get(MessagingSecurityConstants.PREFIX);

            // While creating the Temporary Destination, we check for the length of the prefix, if it is greater
            // than some limit (12), we will just take the prefix upto specified limit. Implementing the same logic
            // here too so that, it will be easy while querying
            if (prefix.length() > MessagingSecurityConstants.MAX_PREFIX_SIZE)
                prefix = prefix.substring(0, MessagingSecurityConstants.MAX_PREFIX_SIZE);

            String[] actionArray = (String[]) permissionProperties
                            .get(MessagingSecurityConstants.ACTION);
            permission = temporaryDestinationPermissions.get(prefix);
            if (permission == null) {
                permission = new TemporaryDestinationPermission();
                permission.setPrefix(prefix);
            }
            permission.addUserAndGroupsToRole(actionArray, users, groups);
        }
        SibTr.exit(tc, CLASS_NAME + "createTemporaryDestinationPermission", permission);
        return permission;
    }

    /**
     * 
     * @param topicSpace
     * @param topic
     * @return
     */
    private String getTopicPermissionKey(String topicSpace, String topic) {
        String key = null;
        if (topic == null || topic.isEmpty()) {
            key = topicSpace;
        } else {
            key = topicSpace + MessagingSecurityConstants.TOPIC_DELIMITER + topic;
        }
        return key;
    }

    private void checkIfRolesAreUnique(String[] roles) {
        SibTr.entry(tc, CLASS_NAME + "checkIfRolesAreUnique");

        List<String> tempList = new ArrayList<String>();
        for (String role : roles) {
            Dictionary<String, Object> roleProperties = getDictionaryObject(role);
            if (roleProperties != null) {
                String roleName = (String) roleProperties.get(MessagingSecurityConstants.NAME);
                if (tempList.contains(roleName)) {
                    SibTr.warning(tc, "DUPLICATE_ROLE_NAME_EXISTS_MSE1012", new Object[] { roleName });
                }
                tempList.add(roleName);
            }
        }

        SibTr.exit(tc, CLASS_NAME + "checkIfRolesAreUnique");
    }

    /**
     * Create the User/Group set for a particular Role
     * 
     * @param properties
     * @param type
     *            "User" or "Group"
     * @return
     *         Set of User/Group
     */
    private Set<String> createUserOrGroupSet(
                                             Dictionary<String, Object> properties, String type) {
        SibTr.entry(tc, CLASS_NAME + "createUserOrGroupSet", new Object[] { properties, type });

        Set<String> userOrGroupSet = new HashSet<String>();
        if (properties != null) {
            String[] tempUsersOrGroups = (String[]) properties.get(type);
            if (tempUsersOrGroups != null) {
                for (String tempUserOrGroup : tempUsersOrGroups) {
                    Dictionary<String, Object> userOrGroupProperties = getDictionaryObject(tempUserOrGroup);
                    if (userOrGroupProperties != null) {
                        String userOrGroup = ((String) userOrGroupProperties
                                        .get(MessagingSecurityConstants.NAME));
                        userOrGroupSet.add(userOrGroup.trim());
                    }
                }
            }
        }

        SibTr.exit(tc, CLASS_NAME + "createUserOrGroupSet", userOrGroupSet);
        return userOrGroupSet;
    }

    /**
     * Get the Dictionary object for the given String
     * 
     * @param input
     * @return
     */
    private Dictionary<String, Object> getDictionaryObject(String input) {
        SibTr.entry(tc, CLASS_NAME + "getDictionaryObject", input);

        Dictionary<String, Object> dictionary = null;
        Configuration config = null;
        try {
            pids.add(input);
            config = configAdmin.getConfiguration(input, bundleLocation);
        } catch (IOException e) {
            MessagingSecurityException mse = new MessagingSecurityException(e);
            FFDCFilter.processException(mse, CLASS_NAME + ".getDictionaryObject", "1008", this);
            SibTr.exception(tc, mse);
            SibTr.error(tc, "IO_EXCEPTION_READING_CONFIGURATION_MSE1008");
            return new Hashtable<String, Object>();
        }
        dictionary = config.getProperties();

        SibTr.exit(tc, CLASS_NAME + "getDictionaryObject", dictionary);
        return dictionary;
    }

    /**
     * Print the Destination Permissions, it will be used for debugging purpose
     */
    private void printDestinationPermissions(Map<String, ?> destinationPermissions) {
        Set<String> destinations = destinationPermissions.keySet();
        for (String destination : destinations) {
            SibTr.debug(tc, CLASS_NAME + " Destination: " + destination);
            Permission permission = (Permission) destinationPermissions.get(destination);
            SibTr.debug(tc, "  Users having permissions!!!");
            Map<String, Set<String>> userRoles = permission.getRoleToUserMap();
            Set<String> uRoles = userRoles.keySet();
            for (String role : uRoles) {
                SibTr.debug(tc, "    " + role + ": " + userRoles.get(role));
            }
            SibTr.debug(tc, "  Groups having permissions!!!");
            Map<String, Set<String>> groupRoles = permission
                            .getRoleToGroupMap();
            Set<String> gRoles = groupRoles.keySet();
            for (String role : gRoles) {
                SibTr.debug(tc, "    " + role + ": " + groupRoles.get(role));
            }
        }
    }
    
    /**
     * Get the Destination Roles, it will be used for audit
     */
    public String[] getDestinationRoles(Map<String, ?> destinationPermissions, String dest, String user) {
        SibTr.debug(tc,  " dest: " + dest + " user: " + user);
        if (user.startsWith("cn=")) {
            user = user.substring(3, user.indexOf(","));
            SibTr.debug(tc, CLASS_NAME + " user truncated to: " + user);
        }
        ArrayList<String> roleList = new ArrayList();
        int element = 0;
        Set<String> destinations = destinationPermissions.keySet();
        
        if (dest.indexOf("/") != -1) {
            dest = dest.substring(0,  dest.indexOf("/"));
        }

        for (String destination : destinations) {
            SibTr.debug(tc, CLASS_NAME + " Destination: " + destination + " dest: " + dest);
            if (destination.equals(dest)) {
                Permission permission = (Permission) destinationPermissions.get(destination);
                Map<String, Set<String>> userRoles = permission.getRoleToUserMap();
                Set<String> uRoles = userRoles.keySet();
                for (String role : uRoles) {
                    SibTr.debug(tc, CLASS_NAME + " role: " + role);
                    SibTr.debug(tc, CLASS_NAME + "    users: " + userRoles.get(role));
                    Set<String> rs = userRoles.get(role);
                    for (String r : rs) {
                        SibTr.debug(tc, CLASS_NAME + "     user: " + r);
                        if (r.equals(user)) {
                            roleList.add(role);
                        }
                     }
                }
                
                Map<String, Set<String>> groupRoles = permission.getRoleToGroupMap();
                Set<String> gRoles = groupRoles.keySet();
                for (String role : gRoles) {
                    SibTr.debug(tc, CLASS_NAME + " role: " + role);
                    SibTr.debug(tc, CLASS_NAME + "    groups: " + groupRoles.get(role));
                   
                    Set <String> rs = groupRoles.get(role);
                    
                    if (rs != null) {
                        List<String> groups = MessagingSecurityUtility
                                        .getGroupsAssociatedToUser(user,
                                                                   this);
                        if (groups != null) {
                            for (String g : groups) {
                                if (rs.contains(g)) {
                                    if (!roleList.contains(role)) {
                                        roleList.add(role);
                                    }
                                }
                            }
                        }
                       
                    }
                }                
                               
            }
        }
        if (roleList != null)
            SibTr.debug(tc,  CLASS_NAME + " roles: " + roleList.toArray().toString());
        else 
            SibTr.debug(tc, CLASS_NAME + " no roles identified for user " + user);
        
        Object[] roleListAsObjectArray = roleList.toArray();
        String[] roleListAsStrArray = Arrays.copyOf(roleListAsObjectArray, roleListAsObjectArray.length, String[].class);
        
        SibTr.exit(tc, CLASS_NAME);
        return (roleListAsStrArray);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getType() == ConfigurationEvent.CM_UPDATED && pids.contains(event.getPid())) {
            populateDestinationPermissions();
            runtimeSecurityService.modifyMessagingServices(this);
        }

    }

}