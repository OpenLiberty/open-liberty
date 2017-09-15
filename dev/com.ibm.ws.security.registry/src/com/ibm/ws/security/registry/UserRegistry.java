/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Defines read-only API contract for UserRegistry implementations.
 * Methods must not have side-effects which alter contents of the UserRegistry.
 * UserRegistry implementations must support dynamic configuration updates.
 */
public interface UserRegistry {

    /**
     * Is this really necessary?
     */
    String getType();

    /**
     * The realm is a UserRegistry specific String indicating
     * the <i>realm</i> or <i>domain</i> for which this UserRegistry
     * applies. For example, for OS400 or AIX this would be the
     * host name of the system whose user UserRegistry this object
     * represents.
     *
     * @return String representing the realm name. <code>null</code> is not returned.
     **/
    String getRealm();

    /**
     * Checks the password of the user. This method is called to authenticate a
     * user when the user's name and password are given.
     *
     * @param userSecurityName the username, userid or a UserRegistry-unique identifier.
     * @param password the password of the user.
     * @return Non-null String representing the user security name if the specified
     *         username / password combination is valid, <code>null</code> otherwise.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName or password is <code>null</code> or empty
     **/
    String checkPassword(String userSecurityName, String password) throws RegistryException;

    /**
     * Maps a X509 Certificate to a valid user in the UserRegistry.
     * This is used to map the name in the certificate supplied by a browser
     * to a valid <i>userSecurityName</i> in the UserRegistry.
     *
     * @param cert the X509 certificate.
     * @return the mapped name of the user. <code>null</code> is not returned.
     * @exception CertificateMapNotSupportedException if the particular
     *                certificate is not supported.
     * @exception CertificateMapFailedException if the mapping of the
     *                certificate fails.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if cert is <code>null</code>
     **/
    String mapCertificate(X509Certificate cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException;

    /**
     * Determines if the <i>userSecurityName</i> exists in the UserRegisry.
     *
     * @param userSecurityName the name of the user.
     * @return true if the user is valid, false otherwise.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName is <code>null</code> or empty
     **/
    boolean isValidUser(String userSecurityName) throws RegistryException;

    /**
     * Gets a list of users that match a <i>pattern</i> in the UserRegistry.
     * The maximum number of users returned is defined by the <i>limit</i>
     * argument. This is very useful in situations where there are thousands of
     * users in the UserRegistry and getting all of them at once is not
     * practical.
     *
     * @param pattern the pattern to match.
     *            The pattern depends on the user registry configured.
     *            For example, * will return all the users when using
     *            the basic and ldap registry.
     * @param limit the maximum number of users that should be returned.
     *            A value of 0 implies get all the users.
     *            Specifying a negative value returns an empty SearchResult.
     * @return a <i>SearchResult</i> object that contains the list of users
     *         requested and a flag to indicate if more users exist.
     *         <code>null</code> is not returned.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if pattern is <code>null</code> or empty
     **/
    SearchResult getUsers(String pattern, int limit) throws RegistryException;

    /**
     * Returns the display name for the user specified by userSecurityName.
     * <p>
     * The display name is a UserRegistry-specific string that represents a descriptive,
     * and not necessarily unique, name for a user.
     * <p>
     * This method may be called only when the user information is displayed
     * and is not used for authentication or authorization purposes. If display names
     * are not supported by the UserRegistry return the specified <i>userSecurityName</i>.
     *
     * @param userSecurityName the name of the user.
     * @return the display name for the user. <code>null</code> may be returned.
     * @exception EntryNotFoundException if userSecurityName does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName is <code>null</code> or empty
     **/
    String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the unique ID for a userSecurityName.
     * <p>
     * In some cases, the unique ID is the same as the userSecurityName.
     * The unique ID for an user is the String form of some unique, UserRegistry-specific
     * data that serves to represent the user. For example a Unix uid.
     * <p>
     * This will vary based on the underlying UserRegistry implementation.
     * Some examples are:
     *
     * @param userSecurityName the name of the user.
     * @return the unique ID of the user. <code>null</code> may be returned.
     * @exception EntryNotFoundException if userSecurityName does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName is <code>null</code> or empty
     **/
    String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the name for a user given its <i>uniqueId</i>.
     *
     * @param uniqueUserId the UniqueId of the user.
     * @return the userSecurityName of the user. <code>null</code> may be returned.
     * @exception EntryNotFoundException if the uniqueUserId does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if uniqueUserId is <code>null</code> or empty
     **/
    String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException;

    /**
     * Returns a list of users in a group.
     *
     * The maximum number of users returned is defined by the <i>limit</i>
     * argument.
     * *
     * In rare situations if you are working with a registry where getting all
     * the users from any of your groups is not practical (for example if there
     * are a large number of users) you can throw the NotImplementedException
     * for that particular group(s). If there is no concern about
     * returning the users from groups in the registry it is recommended that
     * this method be implemented without throwing the NotImplemented exception.
     *
     * @param groupSecurityName the name of the group
     * @param limit the maximum number of users that should be returned.
     *            This is very useful in situations where there are lot of
     *            users in the registry and getting all of them at once is not
     *            practical. A value of 0 implies get all the users and hence
     *            must be used with care.
     * @return a <i>SearchResult</i> object that contains the list of users
     *         requested and a flag to indicate if more users exist.
     * @exception NotImplementedException throw this exception in rare situations
     *                if it is not practical to get this information for any of the
     *                group(s) from the registry.
     * @exception EntryNotFoundException if the group does not exist in
     *                the registry
     * @exception RegistryException if there is any registry specific
     *                problem
     **/

    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException;

    /**
     * Determines if the <i>groupSecurityName</i> exists in the UserRegsitry.
     *
     * @param groupSecurityName the name of the group.
     * @return true if the groups exists, false otherwise
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if groupSecurityName is <code>null</code> or empty
     **/
    boolean isValidGroup(String groupSecurityName) throws RegistryException;

    /**
     * Gets a list of groups that match a <i>pattern</i> in the UserRegisty.
     * The maximum number of groups returned is defined by the <i>limit</i>
     * argument.
     *
     * @param pattern the pattern to match.
     *            The pattern depends on the user registry configured.
     *            For example, * will return all the groups when using
     *            the basic and ldap registry.
     * @param limit the maximum number of groups that should be returned.
     *            This is very useful in situations where there are thousands of
     *            groups in the registry and getting all of them at once is not
     *            practical. A value of 0 implies get all the groups and hence
     *            must be used with care.
     * @return a <i>SearchResult</i> object that contains the list of groups
     *         requested and a flag to indicate if more groups exist.
     *         <code>null</code> is not returned.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if pattern is <code>null</code> or empty
     **/
    SearchResult getGroups(String pattern, int limit) throws RegistryException;

    /**
     * Returns the display name for the group specified by groupSecurityName.
     * <p>
     * The display name is a UserRegistry-specific string that represents a descriptive,
     * and not necessarily unique, name for a group.
     * <p>
     * This method may be called only when the group information is displayed
     * and is not used for authentication or authorization purposes. If display names
     * are not supported by the UserRegistry return the specified <i>groupSecurityName</i>.
     *
     * @param groupSecurityName the name of the group.
     * @return the display name for the group. <code>null</code> may be returned.
     * @exception EntryNotFoundException if groupSecurityName does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if groupSecurityName is <code>null</code> or empty
     **/
    String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the unique ID for a group.
     * <p>
     * In some cases, the unique ID is the same as the groupSecurityName.
     * The unique ID for a group is the String form of some unique, UserRegistry-specific
     * data that serves to represent the group. For example a Unix gid.
     *
     * @param groupSecurityName the name of the group.
     * @return the Unique id of the group. <code>null</code> may be returned.
     * @exception EntryNotFoundException if groupSecurityName does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if groupSecurityName is <code>null</code> or empty
     **/
    String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the name for a group given its <i>uniqueId</i>.
     *
     * @param uniqueGroupId the UniqueId of the group.
     * @return the name of the group. <code>null</code> may be returned.
     * @exception EntryNotFoundException if the uniqueGroupId does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if uniqueGroupId is <code>null</code> or empty
     **/
    String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the unique IDs for all of the groups that the <i>uniqueUserId</i>
     * belongs to.
     *
     * @see #getUniqueGroupId(String)
     * @param uniqueUserId the unique ID of the user.
     * @return a List of unique group IDs that the uniqueUserId belongs to.
     *         <code>null</code> is not returned.
     * @exception EntryNotFoundException if uniqueUserId does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if uniqueUserId is <code>null</code> or empty
     **/
    List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException;

    /**
     * Returns the names of all the groups that <i>userSecurityName</i> belongs to.
     *
     * @param userSecurityName the name of the user.
     * @return a List of group names that the user belongs to.
     *         <code>null</code> is not returned.
     * @exception EntryNotFoundException if uniqueUserId does not exist.
     * @exception RegistryException if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName is <code>null</code> or empty
     **/
    List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException;

}
