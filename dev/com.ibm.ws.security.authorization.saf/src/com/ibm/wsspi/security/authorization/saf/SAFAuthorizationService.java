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
package com.ibm.wsspi.security.authorization.saf;

import javax.security.auth.Subject;

import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Perform authorization checks against SAF resources.
 *
 * @author IBM Corporation
 * @version 1.1
 * @ibm-spi
 */
public interface SAFAuthorizationService {

    /**
     * Determines if the Subject on the calling thread has the specified access to the
     * given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     *
     * @return true if the Subject on the calling thread has the required access; otherwise false.
     *
     * @throws NullPointerException if className or resourceName is null.
     */
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel);

    /**
     * Determines if the Subject on the calling thread has the specified access to the
     * given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the Subject on the calling thread has the required access; otherwise false.
     *
     * @throws NullPointerException if className or resourceName is null.
     */
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption);

    /**
     * Determines if the Subject on the calling thread has the specified access to the
     * given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param className               The SAF class of the protected resource.
     * @param resourceName            The SAF protected resource.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure
     *
     * @return true if the Subject on the calling thread has the required access; otherwise false.
     *
     * @throws NullPointerException      if className or resourceName is null.
     * @throws SAFAuthorizationException if the subject on the calling thread could not be authenticated.
     */
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the given Subject has the specified access to the given SAF resource
     * in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param subject      The Subject to authorize.
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException if subject, className or resourceName is null.
     */
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel);

    /**
     * Determines if the given Subject has the specified access to the given SAF resource
     * in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param subject      The Subject to authorize.
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException if subject, className or resourceName is null.
     */
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption);

    /**
     * Determines if the given Subject has the specified access to the given SAF resource
     * in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param subject                 The Subject to authorize.
     * @param className               The SAF class of the protected resource.
     * @param resourceName            The SAF protected resource.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException      if subject, className or resourceName is null.
     * @throws SAFAuthorizationException if the subject could not be authenticated.
     */
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the identity represented by the given SAFCredential has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param safCredential The SAFCredential to authorize.
     * @param className     The SAF class of the protected resource.
     * @param resourceName  The SAF protected resource.
     * @param accessLevel   The required access level. If null, the default is AccessLevel.READ.
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException if safCredential, className or resourceName is null.
     */
    public boolean isAuthorized(SAFCredential safCredential,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel);

    /**
     * Determines if the identity represented by the given SAFCredential has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param safCredential The SAFCredential to authorize.
     * @param className     The SAF class of the protected resource.
     * @param resourceName  The SAF protected resource.
     * @param accessLevel   The required access level. If null, the default is AccessLevel.READ.
     * @param logOption     The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException if safCredential, className or resourceName is null.
     */
    public boolean isAuthorized(SAFCredential safCredential,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption);

    /**
     * Determines if the identity represented by the given SAFCredential has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param safCredential           The SAFCredential to authorize.
     * @param className               The SAF class of the protected resource.
     * @param resourceName            The SAF protected resource.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException      if safCredential, className or resourceName is null.
     * @throws SAFAuthorizationException if the safCredential could not be authenticated.
     */
    public boolean isAuthorized(SAFCredential safCredential,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the identity represented by the given mvsUserId has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param mvsUserId    The user to authorize.
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the Subject has the required access; otherwise false.
     *
     * @throws NullPointerException      if mvsUserId, className or resourceName is null.
     * @throws SAFAuthorizationException if the mvsUserId could not be authenticated.
     */
    public boolean isAuthorized(String mvsUserId,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption) throws SAFAuthorizationException;

    /**
     * Determines if the group represented by the given groupName has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param groupName    The group to authorize.
     * @param className    The SAF class of the protected resource.
     * @param resourceName The SAF protected resource.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the group has the required access; otherwise false.
     *
     * @throws NullPointerException      if groupName, className or resourceName is null.
     * @throws SAFAuthorizationException if the group could not be authenticated.
     */
    public boolean isGroupAuthorized(String groupName,
                                     String className,
                                     String resourceName,
                                     AccessLevel accessLevel,
                                     LogOption logOption) throws SAFAuthorizationException;

    /**
     * Determines if the group represented by the given groupName has the specified
     * access to the given SAF resource in the given SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param groupName               The group to authorize.
     * @param className               The SAF class of the protected resource.
     * @param resourceName            The SAF protected resource.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure
     *
     * @return true if the group has the required access; otherwise false.
     *
     * @throws NullPointerException      if groupName, className or resourceName is null.
     * @throws SAFAuthorizationException if the group could not be authenticated.
     */
    public boolean isGroupAuthorized(String groupName,
                                     String className,
                                     String resourceName,
                                     AccessLevel accessLevel,
                                     LogOption logOption,
                                     boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the group represented by the given groupName has the specified
     * access to the given SAF resource in the Dataset SAF class.
     *
     * This method is protected by <code>WebSphereRuntimePermission</code>
     * with the target name of <code>safAuthorizationService</code>.
     * A <code>java.lang.SecurityException</code> is thrown if Java 2 Security
     * Manager is installed and the code is not granted the permission.
     *
     * @param groupName               The group to authorize.
     * @param resourceName            The SAF protected resource.
     * @param volser                  The volume serial number of the volume where the dataset is located.
     * @param vsam                    The flag to indicate whether a dataset is vsam or non-vsam.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure.
     *
     * @return true if the group has the required access; otherwise false.
     *
     * @throws NullPointerException      if groupName, className or resourceName or volser is null.
     * @throws IllegalArgumentException  if volser is larger 6 characters or resource name is larger than 44 characters.
     * @throws SAFAuthorizationException if the group could not be authenticated.
     */
    public boolean isGroupAuthorizedToDataset(String groupName,
                                              String resourceName,
                                              String volser,
                                              boolean vsam,
                                              AccessLevel accessLevel,
                                              LogOption logOption,
                                              boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Wraps a SAFCredential around the given mvsUserId, then calls isAuthorized(SAFCredential, ...)
     *
     * @param mvsUserId               The user to authorize.
     * @param className               The SAF class of the protected resource.
     * @param resourceName            The SAF protected resource.
     * @param accessLevel             The required access level. If null, the default is AccessLevel.READ.
     * @param logOption               The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     * @param throwExceptionOnFailure The flag for the option to throw a SAFAuthorizationException on failure.
     *
     * @return true if the given mvsUserId has the given accessLevel to the given className and resourceName.
     *
     * @throws NullPointerException      if mvsUserId, className or resourceName is null.
     * @throws SAFAuthorizationException if the mvsUserId could not be authenticated.
     */
    public boolean isAuthorized(String mvsUserId,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the Subject on the calling thread has the specified access to the
     * given SAF resource in the given DATASET class.
     *
     * @param resourceName The SAF protected resource.
     * @param volser       The volume serial number of the volume where the dataset is located.
     * @param vsam         The flag to indicate whether a dataset is vsam or non-vsam.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the given thread subject has the given accessLevel to the given resourceName.
     *
     * @throws NullPointerException      if resourceName or volser is null.
     * @throws IllegalArgumentException  if volser is larger 6 characters or resource name is larger than 44 characters.
     * @throws SAFAuthorizationException if the thread subject could not be authenticated.
     */
    public boolean isAuthorizedToDataset(String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the identity represented by the given Subject has the specified
     * access to the given SAF resource in the dataset class.
     *
     * @param subject      The subject to check authorization on.
     * @param resourceName The SAF protected resource.
     * @param volser       The volume serial number of the volume where the dataset is located.
     * @param vsam         The flag to indicate whether a dataset is vsam or non-vsam.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the given subject has the given accessLevel to the given resourceName.
     *
     * @throws NullPointerException      if resourceName or volser is null.
     * @throws IllegalArgumentException  if volser is larger 6 characters or resource name is larger than 44 characters.
     * @throws SAFAuthorizationException if the subject could not be authenticated.
     */
    public boolean isAuthorizedToDataset(Subject subject,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the identity represented by the given SAFCredential has the specified
     * access to the given SAF resource in the dataset class.
     *
     * @param safCredential The SAF credential to check authorization on.
     * @param resourceName  The SAF protected resource.
     * @param volser        The volume serial number of the volume where the dataset is located.
     * @param vsam          The flag to indicate whether a dataset is vsam or non-vsam.
     * @param accessLevel   The required access level. If null, the default is AccessLevel.READ.
     * @param logOption     The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the given SAFCredential has the given accessLevel to the given resourceName.
     *
     * @throws NullPointerException      if resourceName or volser is null.
     * @throws IllegalArgumentException  if volser is larger 6 characters or resource name is larger than 44 characters.
     * @throws SAFAuthorizationException if the SAFCredential could not be authenticated.
     */
    public boolean isAuthorizedToDataset(SAFCredential safCredential,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Determines if the identity represented by the given mvsUserId has the specified
     * access to the given SAF resource in the dataset class.
     *
     * @param mvsUserId    The user to check authorization on.
     * @param resourceName The SAF protected resource.
     * @param volser       The volume serial number of the volume where the dataset is located.
     * @param vsam         The flag to indicate whether a dataset is vsam or non-vsam.
     * @param accessLevel  The required access level. If null, the default is AccessLevel.READ.
     * @param logOption    The SAF logging option to use for this request. If null, the default is LogOption.ASIS.
     *
     * @return true if the given mvsUserId has the given accessLevel to the given resourceName.
     *
     * @throws NullPointerException      if mvsUserId, resourceName or volser is null.
     * @throws IllegalArgumentException  if volser is larger 6 characters or resource name is larger than 44 characters.
     * @throws SAFAuthorizationException if the mvsUserId could not be authenticated.
     */
    public boolean isAuthorizedToDataset(String mvsUserId,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException;

    /**
     * Gets the RCVTID field of the RCVT control block, as a String.
     *
     * @return The four byte identifier set by the external security product, in the RCVTID field of the RCVT.
     */
    public String getRCVTID();
}
