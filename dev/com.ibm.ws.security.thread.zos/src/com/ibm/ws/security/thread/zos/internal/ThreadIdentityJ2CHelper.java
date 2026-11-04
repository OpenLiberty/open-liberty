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
package com.ibm.ws.security.thread.zos.internal;

import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.saf.SAFCredentialExt;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 *
 * Helper methods for creating J2C subjects.
 */
public class ThreadIdentityJ2CHelper {

    /**
     * Reference to SAFCredentialsService, for retrieving SAFCredentials from Subjects
     * and extracting UTOKENs.
     */
    private SAFCredentialsService safCredentialsService = null;

    /**
     * For retrieving the invocation subject.
     */
    private final SubjectManager subjectManager = new SubjectManager();

    /**
     * CTOR
     *
     * @param safCredentialsService
     */
    public ThreadIdentityJ2CHelper(SAFCredentialsService safCredentialsService) {
        this.safCredentialsService = safCredentialsService;
    }

    /**
     * Create and return a J2C subject based on the invocation subject
     * on the thread.
     *
     * If the invocation subject is null, the caller subject is used.
     * If the caller subject is null, the server subject is used.
     *
     * The J2C subject has an additional GenericCredential in its private
     * credential set that represents the native UTOKEN extracted from
     * the ACEE representing the identity in the subject.
     *
     * @return Subject the J2C subject
     */
    public Subject getJ2CInvocationSubject() {

        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }

        if (subject == null) {
            // tWAS got server subject here.  In Liberty, a null subject is equivalent
            // to the server subject, so we're good.
        }

        Subject j2cSubject = null;

        try {
            j2cSubject = getJ2CSubject(subject);
        } catch (SAFException se) {
            throw new IllegalStateException("Unexpected SAF failure trying to retrieve J2C Subject: " + se.getMessage(), se);
        }

        return j2cSubject;
    }

    /**
     * Create and return a J2C subject based on the given subject.
     *
     * The J2C subject has an additional GenericCredential in its private
     * credential set that represents the native UTOKEN extracted from
     * the ACEE representing the identity in the subject.
     *
     * @param subject The subject on which to base the new J2C subject.
     *                    The identity and SAFCredential from this subject are copied
     *                    to the new J2C subject.
     *
     * @return Subject the J2C subject
     *
     * @throws SAFException For any native SAF errors.
     */
    private Subject getJ2CSubject(Subject subject) throws SAFException {

        SAFCredentialExt safCred = getSAFCredential(subject);

        Subject j2cSubject = safCred.getJ2CSubject();

        if (j2cSubject == null) {
            // not cached.  Create and cache it.
            j2cSubject = createJ2CSubject(safCred, subject);
            safCred.setJ2CSubject(j2cSubject);
        }

        return j2cSubject;
    }

    /**
     *
     * @param subject
     *
     * @return The SAFCredential for the given Subject. If the subject contains no
     *         SAFCredential, then the default credential is returned.
     *
     * @throws SAFException For any native SAF errors.
     */
    private SAFCredentialExt getSAFCredential(Subject subject) throws SAFException {
        SAFCredential safCred = safCredentialsService.getSAFCredentialFromSubject(subject);

        if (safCred == null) {
            if (safCredentialsService.isServerSubject(subject)) {
                safCred = safCredentialsService.getServerCredential();
            } else {
                // tWAS defaulted to default cred.
                safCred = safCredentialsService.getDefaultCredential();
            }
        }

        return (SAFCredentialExt) safCred;
    }

    /**
     * Create a J2C Subject for the given SAFCredential and Subject.
     *
     * The J2C subject has an additional GenericCredential in its private
     * credential set that represents the native UTOKEN extracted from
     * the ACEE representing the identity in the subject.
     *
     * I moved this method here, instead of SAFCredentialsServiceImpl, to eliminate
     * the dependency between credentials.saf and javax.j2eeconnector (for GenericCredential).
     *
     * @param subject
     *
     * @return The J2C subject.
     *
     * @throws SAFException For any native SAF errors.
     */
    private Subject createJ2CSubject(SAFCredentialExt safCred, Subject fromSubject) throws SAFException {

        byte[] utoken = safCredentialsService.extractUToken(safCred);

        // Create a GenericCred using the UTOKEN.
        GenericCredentialImpl genericCred = new GenericCredentialImpl(safCred.getUserId(), utoken);

        // Create the J2C subject and populate it with the GenericCredential and SAFCredential.
        Subject j2cSubject = new Subject();
        j2cSubject.getPrivateCredentials().add(genericCred);
        j2cSubject.getPrivateCredentials().add(safCred);

        if (fromSubject != null) {
            // Copy over the principal for good measure. I don't think it's used anywhere.
            Set<WSPrincipal> principals = fromSubject.getPrincipals(WSPrincipal.class);
            if (!principals.isEmpty()) {
                WSPrincipal wsprin = principals.iterator().next();
                j2cSubject.getPrincipals().add(wsprin);
            }
        }

        j2cSubject.setReadOnly();
        return j2cSubject;
    }

}
