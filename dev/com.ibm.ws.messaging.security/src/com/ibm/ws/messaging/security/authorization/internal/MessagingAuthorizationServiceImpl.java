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

package com.ibm.ws.messaging.security.authorization.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.messaging.security.MSTraceConstants;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.messaging.security.MessagingSecurityException;
import com.ibm.ws.messaging.security.authorization.MessagingAuthorizationException;
import com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService;
import com.ibm.ws.messaging.security.beans.Permission;
import com.ibm.ws.messaging.security.beans.QueuePermission;
import com.ibm.ws.messaging.security.beans.TemporaryDestinationPermission;
import com.ibm.ws.messaging.security.beans.TopicPermission;
import com.ibm.ws.messaging.security.internal.MessagingSecurityServiceImpl;
import com.ibm.ws.messaging.security.utility.MessagingSecurityUtility;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation class for Messaging Authorization Service
 * XML Structure of the Authorization model
 * 
 * @author Sharath Chandra B
 * 
 */
public class MessagingAuthorizationServiceImpl implements MessagingAuthorizationService {

    // Trace component for the MessagingAuthorizationService Implementation class
    private static TraceComponent tc = SibTr.register(MessagingAuthorizationServiceImpl.class,
                                                      MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
                                                      MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package name, used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.authorization.internal.MessagingAuthorizationServiceImpl";

    private MessagingSecurityServiceImpl messagingSecurityService = null;

    private final AuditManager auditManager = new AuditManager();

    /**
     * Constructor
     * 
     * @param messagingSecurityService
     */
    public MessagingAuthorizationServiceImpl(MessagingSecurityServiceImpl messagingSecurityService) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "constructor", messagingSecurityService);
        }
        this.messagingSecurityService = messagingSecurityService;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "constructor");
        }
    }

    private void checkIfUserIsAuthenticated(Subject subject) throws MessagingAuthorizationException {
        try {
            messagingSecurityService.isUnauthenticated(subject);
        } catch (Exception e) {
            throw new MessagingAuthorizationException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService#checkQueueAccess(javax.security.auth.Subject, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public boolean checkQueueAccess(Subject authenticatedSubject, String destination, String operationType, boolean logWarning) throws MessagingAuthorizationException {
        SibTr.entry(tc, CLASS_NAME + "checkQueueAccess", new Object[] { authenticatedSubject, destination, operationType });
        String busName = null;
        String messagingEngine = null;
        String[] roles = null;
        
        if (auditManager != null) {
            if (auditManager.getJMSBusName() != null)
                busName = auditManager.getJMSBusName();
            if (auditManager.getJMSMessagingEngine() != null)
                messagingEngine = auditManager.getJMSMessagingEngine();
        }

             
        if (operationType.equalsIgnoreCase(MessagingSecurityConstants.OPERATION_TYPE_BROWSE)) {
            if (checkQueueAccess(authenticatedSubject, destination, MessagingSecurityConstants.OPERATION_TYPE_RECEIVE, false)) {
                return true;
            }
        }
        checkIfUserIsAuthenticated(authenticatedSubject);
        String userName = null;
        String user = authenticatedSubject.getPrincipals().iterator().next().getName();
        
        Map<String, QueuePermission> mq = messagingSecurityService.getQueuePermissions();
        roles = messagingSecurityService.getDestinationRoles(mq, destination, user);
        SibTr.debug(tc, "checkQueueAccess, roles: " + Arrays.toString(roles));

        try {
            userName = MessagingSecurityUtility.getUniqueUserName(authenticatedSubject);
        } catch (MessagingSecurityException e) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destination, operationType, roles, "queue",
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destination, operationType, roles, "queue",
                            Integer.valueOf("201"));
            }

            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destination), e);
        }
        Map<String, QueuePermission> queuePermissions = messagingSecurityService.getQueuePermissions();
        QueuePermission permission = queuePermissions.get(destination);
        boolean result = checkPermission(permission, operationType, userName);
        if (!result) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destination, operationType, roles, "queue",
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destination, operationType, roles, "queue", Integer.valueOf("201"));
            }  
            if (logWarning) {
                SibTr.debug(tc, "USER_NOT_AUTHORIZED_MSE1010",
                            new Object[] { userName, operationType, destination });
                throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destination));               
            }
        } else {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destination, operationType, roles, "queue",
                            Integer.valueOf("200"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destination, operationType, roles, "queue", Integer.valueOf("200"));
            }

        }

        SibTr.exit(tc, CLASS_NAME + "checkQueueAccess", result);
        return result;
    }


    private boolean checkPermission(Permission permission, String operationType, String userName) {
        SibTr.entry(tc, CLASS_NAME + "checkPermission", new Object[] { permission, operationType, userName });
        if (permission != null) {
            Map<String, Set<String>> userRoleMap = permission
                            .getRoleToUserMap();
            Set<String> usersHavingRole = userRoleMap.get(operationType);
            if (usersHavingRole != null) {
                if (usersHavingRole.contains(userName)) {
                    SibTr.exit(tc, CLASS_NAME + "checkPermission", true);
                    return true;
                }
            }
            Map<String, Set<String>> groupRoleMap = permission.getRoleToGroupMap();
            Set<String> groupsHavingRole = groupRoleMap.get(operationType);
            if (groupsHavingRole != null) {
                List<String> groups = MessagingSecurityUtility
                                .getGroupsAssociatedToUser(userName,
                                                           messagingSecurityService);
                if (groups != null) {
                    for (String group : groups) {
                        if (groupsHavingRole.contains(group)) {
                            SibTr.exit(tc, CLASS_NAME + "checkPermission", true);
                            return true;
                        }
                    }
                }
            }
        }
        SibTr.exit(tc, CLASS_NAME + "checkPermission", false);
        return false;
    }

    /*
     * 
     * @see com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService#checkTemporaryDestinationAccess(javax.security.auth.Subject, java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean checkTemporaryDestinationAccess(Subject authenticatedSubject, String destinationName, String operationType) throws MessagingAuthorizationException {
        SibTr.entry(tc, CLASS_NAME + "checkTemporaryDestinationAccess", new Object[] { authenticatedSubject, destinationName, operationType });

        String busName = null;
        String messagingEngine = null;
        String[] roles = null;
        
        if (auditManager != null) {
            if (auditManager.getJMSBusName() != null)
                busName = auditManager.getJMSBusName();
            if (auditManager.getJMSMessagingEngine() != null)
                messagingEngine = auditManager.getJMSMessagingEngine();
        }
        
        checkIfUserIsAuthenticated(authenticatedSubject);
        String userName = null;
        String user = authenticatedSubject.getPrincipals().iterator().next().getName();
        
        Map<String, TemporaryDestinationPermission> mq = messagingSecurityService.getTemporaryDestinationPermissions();
        roles = messagingSecurityService.getDestinationRoles(mq, destinationName, user);
        SibTr.debug(tc, "checkTemporaryDestinationAccess, roles: " + Arrays.toString(roles));

        boolean result = false;
        try {
            userName = MessagingSecurityUtility.getUniqueUserName(authenticatedSubject);
        } catch (MessagingSecurityException e) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "temporaryDestination",
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "temporaryDestination", Integer.valueOf("201"));
            }

            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destinationName), e);
        }
        Map<String, TemporaryDestinationPermission> tempDestinationPermissions = messagingSecurityService.getTemporaryDestinationPermissions();
        List<String> prefixList = getPrefixMatchingTemporaryDestination(tempDestinationPermissions.keySet(), destinationName);
        for (String prefix : prefixList) {
            TemporaryDestinationPermission permission = tempDestinationPermissions.get(prefix);
            result = checkPermission(permission, operationType, userName);
            if (result)
                break;
        }
        if (!result) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "temporaryDestination", 
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "temporaryDestination", Integer.valueOf("201"));
            }

            SibTr.debug(tc, "USER_NOT_AUTHORIZED_MSE1010",
                        new Object[] { userName, operationType, destinationName });
            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destinationName));
        } else {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "temporaryDestination", 
                            Integer.valueOf("200"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "temporaryDestination", Integer.valueOf("200"));
            }
        }

        SibTr.exit(tc, CLASS_NAME + "checkTemporaryDestinationAccess", result);
        return result;
    }

    /**
     * @param keySet
     * @return
     */
    private List<String> getPrefixMatchingTemporaryDestination(Set<String> keySet, String destinationName) {
        List<String> prefixList = new ArrayList<String>();
        for (String key : keySet) {
            if (destinationName.startsWith(key)) {
                prefixList.add(key);
            }
        }
        return prefixList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService#checkTopicAccess(javax.security.auth.Subject, java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean checkTopicAccess(Subject authenticatedSubject, String topicSpace, String topicName, String operationType) throws MessagingAuthorizationException {
        String busName = null;
        String messagingEngine = null;
        String[] roles = null;
        String resource = null;
        
        if (auditManager != null) {
            if (auditManager.getJMSBusName() != null)
                busName = auditManager.getJMSBusName();
            if (auditManager.getJMSMessagingEngine() != null)
                messagingEngine = auditManager.getJMSMessagingEngine();
        }

        String destinationName = topicSpace;
        resource = "topicSpace";
        if (topicName != null) {
            destinationName = topicSpace + MessagingSecurityConstants.TOPIC_DELIMITER + topicName;
            resource = "topic";
        }
        
        SibTr.entry(tc, CLASS_NAME + "checkTopicAccess", new Object[] { authenticatedSubject, destinationName, operationType });
        checkIfUserIsAuthenticated(authenticatedSubject);
        String userName = null;
        String user = authenticatedSubject.getPrincipals().iterator().next().getName();
        
        Map<String, TopicPermission> mq = messagingSecurityService.getTopicPermissions();
        roles = messagingSecurityService.getDestinationRoles(mq, destinationName, user);
        SibTr.debug(tc, "checkTopicAccess, roles: " + Arrays.toString(roles));
        
        try {
            userName = MessagingSecurityUtility.getUniqueUserName(authenticatedSubject);
        } catch (MessagingSecurityException e) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, resource,
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, resource, Integer.valueOf("201"));
            }

            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destinationName), e);
        }
        Map<String, TopicPermission> topicPermissions = messagingSecurityService.getTopicPermissions();
        TopicPermission permission = getTopicPermission(topicPermissions, destinationName);
        boolean result = checkPermission(permission, operationType, userName);
        if (!result) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, resource,
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, resource, Integer.valueOf("201"));
            }

            SibTr.debug(tc, "USER_NOT_AUTHORIZED_MSE1010",
                        new Object[] { userName, operationType, destinationName });
            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, destinationName));
        } else {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, resource,
                            Integer.valueOf("200"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, resource, Integer.valueOf("200"));
            }
        }

        SibTr.exit(tc, CLASS_NAME + "checkTopicAccess", result);
        return result;
    }

    private TopicPermission getTopicPermission(Map<String, TopicPermission> topicPermissions, String destinationName) {
        SibTr.entry(tc, CLASS_NAME + "getTopicPermission", destinationName);
        TopicPermission permission = null;
        Set<String> topicKeySet = topicPermissions.keySet();
        int lastIndexOfDelimiter = -1;
        if (topicKeySet.contains(destinationName)) {
            SibTr.exit(tc, CLASS_NAME + "getTopicPermission", permission);
            return topicPermissions.get(destinationName);
        }
        while ((lastIndexOfDelimiter = destinationName.lastIndexOf(MessagingSecurityConstants.TOPIC_DELIMITER)) != -1) {
            destinationName = destinationName.substring(0, lastIndexOfDelimiter);
            permission = getTopicPermission(topicPermissions, destinationName);
        }
        SibTr.exit(tc, CLASS_NAME + "getTopicPermission", permission);
        return permission;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService#checkAliasAccess(javax.security.auth.Subject, java.lang.String, java.lang.String,
     * java.lang.String, boolean)
     */
    @Override
    public boolean checkAliasAccess(Subject authenticatedSubject, String destination, String aliasDestination, int destinationType, String operationType,
                                    boolean logWarning) throws MessagingAuthorizationException {
        SibTr.entry(tc, CLASS_NAME + "checkAliasAccess", new Object[] { authenticatedSubject, aliasDestination, operationType });

        String busName = null;
        String messagingEngine = null;
        String[] roles = null;
        
        if (auditManager != null) {
            if (auditManager.getJMSBusName() != null)
                busName = auditManager.getJMSBusName();
            if (auditManager.getJMSMessagingEngine() != null)
                messagingEngine = auditManager.getJMSMessagingEngine();
        }

        roles = new String[] {MessagingSecurityConstants.OPERATION_TYPE_RECEIVE};
        if (operationType.equalsIgnoreCase(MessagingSecurityConstants.OPERATION_TYPE_BROWSE)) {
            if (checkAliasAccess(authenticatedSubject, destination, aliasDestination, destinationType, MessagingSecurityConstants.OPERATION_TYPE_RECEIVE, false)) {
                return true;
            }
        }
        String destinationName = destination + ":" + aliasDestination;

        checkIfUserIsAuthenticated(authenticatedSubject);
        String userName = null;
        String user = authenticatedSubject.getPrincipals().iterator().next().getName();
        try {
            userName = MessagingSecurityUtility.getUniqueUserName(authenticatedSubject);
        } catch (MessagingSecurityException e) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "aliasDestination",
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "aliasDestination", Integer.valueOf("201"));
            }

            throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, aliasDestination), e);
        }
        Permission permission = null;
        if (destinationType == MessagingSecurityConstants.DESTINATION_TYPE_QUEUE) {
            Map<String, QueuePermission> destinationPermission = messagingSecurityService.getQueuePermissions();
            permission = destinationPermission.get(destination);
        } else if (destinationType == MessagingSecurityConstants.DESTINATION_TYPE_TOPICSPACE) {
            Map<String, TopicPermission> destinationPermission = messagingSecurityService.getTopicPermissions();
            permission = destinationPermission.get(destination);
        }
        boolean result = checkPermission(permission, operationType, userName);
        if (!result) {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "aliasDestination",
                            Integer.valueOf("201"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "aliasDestination", Integer.valueOf("201"));
            }
            if (logWarning) {
                SibTr.debug(tc, "USER_NOT_AUTHORIZED_MSE1010",
                            new Object[] { userName, operationType, aliasDestination });
                throw new MessagingAuthorizationException(Tr.formatMessage(tc, "USER_NOT_AUTHORIZED_MSE1010", userName, operationType, aliasDestination));               
            }

        } else {
            if (auditManager != null && auditManager.getJMSConversationMetaData() != null) {
                ConversationMetaData cmd = (ConversationMetaData) auditManager.getJMSConversationMetaData();

                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, cmd.getRemoteAddress().getHostAddress(), new Integer(cmd.getRemotePort()).toString(),
                            cmd.getChainName(), busName, messagingEngine, destinationName, operationType, roles, "aliasDestination",
                            Integer.valueOf("200"));
            } else {
                Audit.audit(Audit.EventID.SECURITY_JMS_AUTHZ_01, user, null, null, null, busName, messagingEngine, destinationName, operationType, 
                            roles, "aliasDestination", Integer.valueOf("200"));
            }

 
        }
        SibTr.exit(tc, CLASS_NAME + "checkAliasAccess", result);
        return result;
    }
}
