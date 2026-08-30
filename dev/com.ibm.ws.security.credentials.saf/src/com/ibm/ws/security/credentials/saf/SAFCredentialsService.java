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
package com.ibm.ws.security.credentials.saf;

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import com.ibm.ws.security.saf.SAFException;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * The SAFCredentialService creates, destroys, and manages all aspects of
 * security credentials created by the z/OS SAF product.
 */
public interface SAFCredentialsService {

    /**
     * Create a BASIC SAF credential for the given user and password.
     *
     * @param userSecurityName
     * @param password
     * @param auditString      An optional audit string used for logging and SMF recording.
     *
     * @return A SAFCredential that represents the native credential.
     *
     * @throws SAFException if the SAFCredential could not be created.
     */
    public SAFCredential createPasswordCredential(String userSecurityName, String password, String auditString) throws SAFException;

    /**
     * Create an ASSERTED SAF credential for the given user.
     *
     * @param userSecurityName The user.
     * @param auditString      An optional audit string used for logging and SMF recording.
     * @param msgSuppress      Flag to suppress RACF message. 1 to suppress message, 0 not to suppress message
     *
     * @throws SAFException If the ASSERTED credential could not be created.
     */
    public SAFCredential createAssertedCredential(String userSecurityName, String auditString, int msgSuppress) throws SAFException;

    /**
     * Create a lazy ASSERTED SAF credential for the given user. Only a partial
     * representation of the credential is created. The full credential isn't created
     * until it is actually needed, e.g. for authorization.
     *
     * @param userSecurityName The user.
     * @param auditString      An optional audit string used for logging and SMF recording.
     *
     * @throws SAFException If the ASSERTED credential could not be created.
     */
    public SAFCredential createLazyAssertedCredential(String userSecurityName, String auditString);

    /**
     * Lookup the SAFCredential using the given SAFCredentialToken key.
     *
     * @param safCredTokenKey The SAFCredentialToken key.
     *
     * @return The SAFCredential for the given SAFCredentialToken key, or null if none was found.
     */
    public SAFCredential getCredentialFromKey(String safCredTokenKey);

    /**
     * Retrieve the SAFCredential associated with the unauthenticated user.
     *
     * @return The SAFCredential associated with the unauthenticated user.
     *
     * @throws SAFException If the credential could not be created.
     */
    public SAFCredential getDefaultCredential() throws SAFException;

    /**
     * Retrieve the SAFCredential associated with the server.
     *
     * @return The SAFCredential associated with the server.
     *
     * @throws SAFException If the credential could not be created.
     */
    public SAFCredential getServerCredential();

    /**
     * Create a CERTIFICATE SAF credential for the given certificate.
     *
     * @param cert        The certificate for which to create the credential.
     * @param auditString An optional audit string used for logging and SMF recording.
     *
     * @return A SAFCredential that represents the native credential for the certificate.
     *
     * @throws SAFException if the SAFCredential could not be created.
     */
    public SAFCredential createCertificateCredential(X509Certificate cert, String auditString) throws SAFException;

    /**
     * Create a MAPPED SAF credential for the given user name and registry name.
     *
     * @param userName     The user name for which to create the credential.
     * @param registryName The registry name for which to create the credential.
     * @param auditString  An optional audit string used for logging and SMF recording.
     *
     * @return A SAFCredential that represents the native credential for the user name and registry name.
     *
     * @throws SAFException if the SAFCredential could not be created.
     */
    SAFCredential createMappedCredential(String userName, String registryName, String auditString) throws SAFException;

    /**
     * Delete the given credential.
     *
     * @param safCredential
     *
     * @throws SAFException if the SAFCredential could not be deleted.
     */
    public void deleteCredential(SAFCredential safCredential) throws SAFException;

    /**
     * Get the SAFCredential from the given Subject
     *
     * @param subject The subject to parse.
     *
     * @return The first SAFCredential found in the set of public credentials,
     *         or null if (a) the Subject is null, or (b) the Subject does not
     *         have a SAFCredential.
     */
    public SAFCredential getSAFCredentialFromSubject(Subject subject);

    /**
     * Get the stringified SAFCredentialToken associated with the given SAFCredential.
     *
     * @param SAFCredential The SAFCredential
     *
     * @return The stringified SAFCredentialToken associated with the given SAFCredential.
     *
     * @throws SAFException if the SAFCredentialToken doesn't exist and could not be created.
     */
    public String getSAFCredentialTokenKey(SAFCredential safCredential) throws SAFException;

    /**
     * Get the SAFCredentialToken, in byte[] form, associated with the given SAFCredential.
     *
     * @param SAFCredential The SAFCredential
     *
     * @return The SAFCredentialToken, in byte[] form, associated with the given SAFCredential.
     *
     * @throws SAFException if the SAFCredentialToken doesn't exist and could not be created.
     */
    public byte[] getSAFCredentialTokenBytes(SAFCredential safCredential) throws SAFException;

    /**
     * Retrieve the profile prefix/security domain.
     *
     * The profile prefix is defined in the config <safCredentials profilePrefix="xx" />
     *
     * @return The profile prefix/security domain.
     */
    public String getProfilePrefix();

    /**
     * Extract the native UTOKEN associated with the given SAFCredential.
     *
     * @param safCred The SAFCredential.
     *
     * @return The native UTOKEN, as a byte[].
     *
     * @throws SAFException if a failure occurred while extracting the UTOKEN.
     */
    public byte[] extractUToken(SAFCredential safCred) throws SAFException;

    /**
     * Returns true if the given Subject is the Server Subject.
     *
     * TODO: For now, in Liberty, an empty or null Subject is equivalent to the Server
     * Subject, since we don't have an actual Server Subject just yet.
     *
     * @param subject
     * @return true if the given Subject is the Server Subject; false otherwise.
     */
    public boolean isServerSubject(Subject subject);

}
