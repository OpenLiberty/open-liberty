/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Registries. This should extend java.rmi.Remote as the registry can be in
 * a remote process.
 * 
 * <p>Implementation of this interface must provide implementations for:
 * <ul>
 * <li>initialize(java.util.Properties)
 * <li>checkPassword(String,String)
 * <li>mapCertificate(X509Certificate[])
 * <li>getRealm
 * <li>getUsers(String,int)
 * <li>getUserDisplayName(String)
 * <li>getUniqueUserId(String)
 * <li>getUserSecurityName(String)
 * <li>isValidUser(String)
 * <li>getGroups(String,int)
 * <li>getGroupDisplayName(String)
 * <li>getUniqueGroupId(String)
 * <li>getUniqueGroupIds(String)
 * <li>getGroupSecurityName(String)
 * <li>isValidGroup(String)
 * <li>getGroupsForUser(String)
 * <li>getUsersForGroup(String,int)
 * <li>createCredential(String)
 * </ul>
 * 
 * <p>Users of this interface should have the role-based permissions for the
 * Administrator.
 * 
 * <p>The exceptions thrown by the methods in this class are:
 * <ul>
 * <li>java.rmi.RemoteException
 * <li>{@link com.ibm.websphere.security.CertificateMapFailedException} <li>{@link com.ibm.websphere.security.CertificateMapNotSupportedException} <li>
 * {@link com.ibm.websphere.security.CustomRegistryException} <li>{@link com.ibm.websphere.security.EntryNotFoundException} <li>
 * {@link com.ibm.websphere.security.NotImplementedException} <li>{@link com.ibm.websphere.security.PasswordCheckFailedException} </ul>
 * 
 * <p>Each exception type is a recommendation to the Custom Registry
 * developer on it's use case. e.g. EntryNotFoundException should be used when
 * the entry can not be found. There is no requirement that the recommendation
 * be followed. As such, code which invokes registry APIs should hande
 * all thrown exceptions accordingly.
 * 
 * <p>The exceptions thrown by the Custom Registry implementations may be
 * wrapped by the WebSphere code in this release. It is recommended that
 * any exceptions that are thrown be checked for the cause with getCause().
 * 
 * @ibm-spi
 **/
public interface UserRegistry extends java.rmi.Remote {

    /**
     * Initializes the registry. This method is called when creating the
     * registry.
     * 
     * @param props the registry-specific properties with which to
     *            initialize the custom registry
     * @exception CustomRegistryException
     *                if there is any registry specific problem
     * @exception RemoteException
     *                as this extends java.rmi.Remote
     **/
    public void initialize(java.util.Properties props)
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Checks the password of the user. This method is called to authenticate a
     * user when the user's name and password are given.
     * 
     * @param userSecurityName the name of the user or userid or a registry-unique
     *            identifier
     * @param password the password of the user
     * @return a valid <i>userSecurityName</i>. Normally this is
     *         the name of same user whose password was checked but if the
     *         implementation wants to return any other valid
     *         <i>userSecurityName</i> in the registry it can do so
     * @exception PasswordCheckFailedException if <i>userSecurityName</i>/
     *                <i>password</i> combination does not exist in the registry
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String checkPassword(String userSecurityName, String password)
                    throws PasswordCheckFailedException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Maps a Certificate (of X509 format) to a valid user in the Registry.
     * This is used to map the name in the certificate supplied by a browser
     * to a valid <i>userSecurityName</i> in the registry
     * 
     * @param cert the X509 certificate chain
     * @return the mapped name of the user <i>userSecurityName</i>
     * @exception CertificateMapNotSupportedException if the particular
     *                certificate is not supported.
     * @exception CertificateMapFailedException if the mapping of the
     *                certificate fails.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String mapCertificate(X509Certificate[] cert)
                    throws CertificateMapNotSupportedException,
                    CertificateMapFailedException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the realm of the registry.
     * 
     * @return the realm. The realm is a registry-specific string indicating
     *         the <i>realm</i> or <i>domain</i> for which this registry
     *         applies. For example, for OS400 or AIX this would be the
     *         host name of the system whose user registry this object
     *         represents.
     *         If null is returned by this method realm defaults to the
     *         value of "customRealm".
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getRealm()
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Gets a list of users that match a <i>pattern</i> in the registy.
     * The maximum number of users returned is defined by the <i>limit</i>
     * argument.
     * <p>This method is called by GUI(adminConsole) and Scripting(Command Line) to
     * make available the users in the registry for adding them (users) to roles.
     * 
     * @param pattern the pattern to match. (For e.g., a* will match all
     *            userSecurityNames starting with a)
     * @param limit the maximum number of users that should be returned.
     *            This is very useful in situations where there are thousands of
     *            users in the registry and getting all of them at once is not
     *            practical. A value of 0 implies get all the users and hence
     *            must be used with care.
     * @return a <i>Result</i> object that contains the list of users
     *         requested and a flag to indicate if more users exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public Result getUsers(String pattern, int limit)
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Returns the display name for the user specified by userSecurityName.
     * 
     * <p>This method may be called only when the user information is displayed
     * (i.e information purposes only, for example, in GUI) and hence not used
     * in the actual authentication or authorization purposes. If there are no
     * display names in the registry return null or empty string.
     * 
     * <p>In WAS 4.0 custom registry, if you had a display name for the user and
     * if it was different from the security name, the display name was
     * returned for the EJB methods getCallerPrincipal() and the servlet methods
     * getUserPrincipal() and getRemoteUser().
     * In WAS 5.0 for the same methods the security name will be returned by
     * default. This is the recommended way as the display name is not unique
     * and might create security holes.
     * However, for backward compatability if one needs the display name to
     * be returned set the property WAS_UseDisplayName to true.
     * 
     * <p>See the Infocenter documentation for more information.
     * 
     * @param userSecurityName the name of the user.
     * @return the display name for the user. The display name
     *         is a registry-specific string that represents a descriptive, not
     *         necessarily unique, name for a user. If a display name does
     *         not exist return null or empty string.
     * @exception EntryNotFoundException if userSecurityName does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getUserDisplayName(String userSecurityName)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the UniqueId for a userSecurityName. This method is called when
     * creating a credential for a user.
     * 
     * @param userSecurityName the name of the user.
     * @return the UniqueId of the user. The UniqueId for an user is
     *         the stringified form of some unique, registry-specific, data
     *         that serves to represent the user. For example, for the UNIX
     *         user registry, the UniqueId for a user can be the UID.
     * @exception EntryNotFoundException if userSecurityName does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getUniqueUserId(String userSecurityName)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the name for a user given its uniqueId.
     * 
     * @param uniqueUserId the UniqueId of the user.
     * @return the userSecurityName of the user.
     * @exception EntryNotFoundException if the uniqueUserId does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getUserSecurityName(String uniqueUserId)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Determines if the <i>userSecurityName</i> exists in the registry
     * 
     * @param userSecurityName the name of the user
     * @return true if the user is valid. false otherwise
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public boolean isValidUser(String userSecurityName)
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Gets a list of groups that match a <i>pattern</i> in the registy.
     * The maximum number of groups returned is defined by the <i>limit</i>
     * argument.
     * <p>This method is called by GUI(adminConsole) and Scripting(Command Line) to
     * make available the groups in the registry for adding them (groups) to
     * roles.
     * 
     * @param pattern the pattern to match. (For e.g., a* will match all
     *            groupSecurityNames starting with a)
     * @param limit the maximum number of groups that should be returned.
     *            This is very useful in situations where there are thousands of
     *            groups in the registry and getting all of them at once is not
     *            practical. A value of 0 implies get all the groups and hence
     *            must be used with care.
     * @return a <i>Result</i> object that contains the list of groups
     *         requested and a flag to indicate if more groups exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public Result getGroups(String pattern, int limit)
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Returns the display name for the group specified by groupSecurityName.
     * 
     * <p>This method may be called only when the group information is displayed
     * (for example, GUI) and hence not used in the actual authentication or
     * authorization purposes. If there are no display names in the registry
     * return null or empty string.
     * 
     * @param groupSecurityName the name of the group.
     * @return the display name for the group. The display name
     *         is a registry-specific string that represents a descriptive, not
     *         necessarily unique, name for a group. If a display name does
     *         not exist return null or empty string.
     * @exception EntryNotFoundException if groupSecurityName does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getGroupDisplayName(String groupSecurityName)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the Unique id for a group.
     * 
     * @param groupSecurityName the name of the group.
     * @return the Unique id of the group. The Unique id for
     *         a group is the stringified form of some unique,
     *         registry-specific, data that serves to represent the group.
     *         For example, for the Unix user registry, the Unique id could
     *         be the GID.
     * @exception EntryNotFoundException if groupSecurityName does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getUniqueGroupId(String groupSecurityName)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the Unique ids for all the groups that contain the UniqueId of
     * a user.
     * <p>Called during creation of a user's credential.
     * 
     * @param uniqueUserId the uniqueId of the user.
     * @return a List of all the group UniqueIds that the uniqueUserId
     *         belongs to. The Unique id for an entry is the stringified
     *         form of some unique, registry-specific, data that serves
     *         to represent the entry. For example, for the
     *         Unix user registry, the Unique id for a group could be the GID
     *         and the Unique Id for the user could be the UID.
     * @exception EntryNotFoundException if uniqueUserId does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public List<String> getUniqueGroupIds(String uniqueUserId)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Returns the name for a group given its uniqueId.
     * 
     * @param uniqueGroupId the UniqueId of the group.
     * @return the name of the group.
     * @exception EntryNotFoundException if the uniqueGroupId does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public String getGroupSecurityName(String uniqueGroupId)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Determines if the <i>groupSecurityName</i> exists in the registry
     * 
     * @param groupSecurityName the name of the group
     * @return true if the groups exists, false otherwise
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public boolean isValidGroup(String groupSecurityName)
                    throws CustomRegistryException,
                    RemoteException;

    /**
     * Returns the securityNames of all the groups that contain the user
     * 
     * <p>This method is called by GUI(adminConsole) and Scripting(Command Line)
     * to verify the user entered for RunAsRole mapping belongs to that role
     * in the roles to user mapping. Initially, the check is done to see if
     * the role contains the user. If the role does not contain the user
     * explicitly, this method is called to get the groups that this user
     * belongs to so that check can be made on the groups that the role contains.
     * 
     * @param userSecurityName the name of the user
     * @return a List of all the group securityNames that the user
     *         belongs to.
     * @exception EntryNotFoundException if user does not exist.
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public List<String> getGroupsForUser(String userSecurityName)
                    throws EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * Gets a list of users in a group.
     * 
     * <p>The maximum number of users returned is defined by the <i>limit</i>
     * argument.
     * 
     * <p>This method is being used by the WebSphere Application Server Enterprise
     * Process Choreographer (Enterprise Edition) when staff assignments are
     * modeled using groups.
     * 
     * <p>In rare situations if you are working with a registry where getting all
     * the users from any of your groups is not practical (for example if there
     * are a large number of users) you can throw the NotImplementedException
     * for that particualar group(s). Make sure that if the WAS Choreographer
     * in installed (or if installed later) the staff assignments are not
     * modeled using these particular groups. If there is no concern about
     * returning the users from groups in the registry it is recommended that
     * this method be implemented without throwing the NotImplemented exception.
     * 
     * @param groupSecurityName the name of the group
     * @param limit the maximum number of users that should be returned.
     *            This is very useful in situations where there are lot of
     *            users in the registry and getting all of them at once is not
     *            practical. A value of 0 implies get all the users and hence
     *            must be used with care.
     * @return a <i>Result</i> object that contains the list of users
     *         requested and a flag to indicate if more users exist.
     * @exception NotImplementedException throw this exception in rare situations
     *                if it is not pratical to get this information for any of the
     *                group(s) from the registry.
     * @exception EntryNotFoundException if the group does not exist in
     *                the registry
     * @exception CustomRegistryException if there is any registry specific
     *                problem
     * @exception RemoteException as this extends java.rmi.Remote
     **/
    public Result getUsersForGroup(String groupSecurityName, int limit)
                    throws NotImplementedException,
                    EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;

    /**
     * This method is implemented internally by the WebSphere code in this
     * release. This method is not called for the Custom Registry implementations
     * for this release. Return null in the implementation.
     * 
     * <p>Note that since this method is not called one can also return the
     * NotImplementedException as the previous documentation says.
     * 
     **/
    public com.ibm.websphere.security.cred.WSCredential createCredential(String userSecurityName)
                    throws NotImplementedException,
                    EntryNotFoundException,
                    CustomRegistryException,
                    RemoteException;
}
