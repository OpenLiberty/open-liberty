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

package com.ibm.ws.messaging.security.authorization;

import javax.security.auth.Subject;

/**
 * Authorization Service Interface for Messaging
 * This is responsible for authorizing a user for accessing destination
 * 
 * @author Sharath Chandra B
 * 
 */
public interface MessagingAuthorizationService {

    /**
     * Check if the AuthenticatedSubject has the access for specific operation on the Queue requested
     * 
     * @param authenticatedSubject
     * @param destination
     * @param operationType (SEND, RECEIVE, BROWSE)
     * @param logWarning
     * @return
     *         true : If the User has access to perform particular action on the destination
     *         false: If the User is not authorized to perform an action on a destination
     * @throws MessagingAuthorizationException
     */
    public boolean checkQueueAccess(Subject authenticatedSubject,
                                    String destination, String operationType, boolean logWarning) throws MessagingAuthorizationException;

    /**
     * Check if the AuthenticatedSubject has the access for specific operation on the Temporary Destination requested
     * 
     * @param authenticatedSubject
     * @param prefix
     * @param operationType (CREATE, SEND, RECEIVE)
     * @return
     *         true : If the User has access to perform particular action on the destination
     *         false: If the User is not authorized to perform an action on a destination
     * @throws MessagingAuthorizationException
     */
    public boolean checkTemporaryDestinationAccess(Subject authenticatedSubject,
                                                   String prefix, String operationType) throws MessagingAuthorizationException;

    /**
     * Check if the AuthenticatedSubject has the access for specific operation on the Topic requested
     * 
     * @param authenticatedSubject
     * @param topicSpace
     * @param topicName
     * @param operationType (SEND, RECEIVE)
     * @return
     *         true : If the User has access to perform particular action on the destination
     *         false: If the User is not authorized to perform an action on a destination
     * @throws MessagingAuthorizationException
     */
    public boolean checkTopicAccess(Subject authenticatedSubject,
                                    String topicSpace, String topicName, String operationType) throws MessagingAuthorizationException;

    /**
     * @param authenticatedSubject
     * @param targetDestination
     * @param aliasDestination
     * @param destinationType
     * @param operationType
     * @param logWarning
     * @return
     *         true : If the User has access to perform particular action on the destination
     *         false: If the User is not authorized to perform an action on a destination
     * @throws MessagingAuthorizationException
     */
    public boolean checkAliasAccess(Subject authenticatedSubject, String targetDestination, String aliasDestination, int destinationType, String operationType,
                                    boolean logWarning) throws MessagingAuthorizationException;

}
