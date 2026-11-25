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
package com.ibm.ws.security.credentials.saf.internal;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.ws.security.credentials.saf.SAFCredentialExt;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;
import com.ibm.wsspi.security.credentials.saf.SAFGroupCredential;

/**
 * Java representation of a SAF credential (i.e an ACEE), via several layers
 * of indirection.
 */
public class SAFCredentialImpl implements SAFCredentialExt, SAFGroupCredential {

    /**
     * User associated with this credential.
     */
    private String userId = null;

    /**
     * Audit string used when authenticating this SAFCredential.
     */
    private String auditString = null;

    /**
     * The certificate associated with this credential.
     */
    private X509Certificate cert = null;

    /**
     * The credential type.
     */
    private SAFCredential.Type type = null;

    /**
     * Indicates whether or not the credential has been authenticated.
     */
    private transient boolean authenticated = false;

    /**
     * The cached J2C subject associated with this credential.
     */
    private transient Subject j2cSubject = null;

    /**
     * MVS user id returned after mapping this credential with RACMAP.
     */
    private String mappedMvsUserId = null;

    /**
     * Registry name used when mapping this credential with RACMAP.
     */
    private String mappedRegistryName = null;

    /**
     * List of groups returned when getMvsGroupIds is called on a MAPPED credential.
     */
    private List<String> GroupIds = null;

    /**
     * TraceComponent to send messages from NLS props
     */
    private static final TraceComponent tc = Tr.register(SAFCredentialImpl.class);

    private SAFCredentialsServiceImpl safCredentialsServiceImpl;

    /**
     * CTOR.
     *
     * @param userId      The user associated with this credential.
     * @param auditString The audit string used when authenticating this credential.
     */
    public SAFCredentialImpl(String userId, String auditString, SAFCredential.Type type) {
        this.userId = userId;
        this.auditString = auditString;
        this.type = type;
    }

    /**
     * CTOR.
     *
     * @param userId      The user associated with this credential.
     * @param auditString The audit string used when authenticating this credential.
     * @param cert        The certificate associated with this credential.
     */
    public SAFCredentialImpl(String userId, String auditString, X509Certificate cert) {
        this(userId, auditString, SAFCredential.Type.CERTIFICATE);
        this.cert = cert;
    }

    /**
     * CTOR.
     *
     * @param userId          The user associated with this credential.
     * @param auditString     The audit string used when authenticating this credential.
     * @param mappedMvsUserId The MVS user id the credential was mapped to.
     * @param registryName    The registry name used to map this credential.
     */
    public SAFCredentialImpl(String userId, String auditString, String mappedMvsUserId, String registryName, SAFCredentialsServiceImpl safCredentialsServiceImpl) {
        this(userId, auditString, SAFCredential.Type.MAPPED);
        this.mappedMvsUserId = mappedMvsUserId;
        this.mappedRegistryName = registryName;
        this.safCredentialsServiceImpl = safCredentialsServiceImpl;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String getUserId() {
        if (mappedMvsUserId != null) {
            return mappedMvsUserId;
        } else {
            return userId;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMvsUserId() {
        if (mappedMvsUserId != null) {
            return mappedMvsUserId;
        } else {
            return userId;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealm() {
        return mappedRegistryName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuditString() {
        return auditString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate getCertificate() {
        return cert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public SAFCredential.Type getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Setter for authenticated field.
     */
    protected void setAuthenticated(boolean val) {
        authenticated = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJ2CSubject(Subject j2cSubject) {
        this.j2cSubject = j2cSubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subject getJ2CSubject() {
        return j2cSubject;
    }

    /**
     * For debugging.
     */
    @Override
    public String toString() {
        return "SAFCredentialImpl@" + Integer.toHexString(hashCode()) + ":" + getUserId() + ":" + getType() + ":" + getDistributedUserId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String getDistributedUserId() {
        return userId;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Override
    public List<String> getMvsGroupIds() throws CustomRegistryException, EntryNotFoundException {
        //getMvsGroupIds should only be called on MAPPED credentials. Other credential types can use the userRegistry function instead.
        if (this.getType() != SAFCredential.Type.MAPPED) {
            throw new UnsupportedOperationException(Tr.formatMessage(tc, "NONMAPPED_CREDENTIAL_GROUPS_CALL", this.getType().toString()));
        }

        if (GroupIds == null) {
            //Only get the GroupIds if they aren't already cached
            GroupIds = this.safCredentialsServiceImpl.getGroupsForMappedUser(mappedMvsUserId);
        }

        return GroupIds;
    }
}
