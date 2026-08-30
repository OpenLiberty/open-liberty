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
package com.ibm.ws.security.registry.saf.internal;

import java.security.cert.X509Certificate;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.PasswordExpiredException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRevokedException;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFSecurityName;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Authorized version of SAFRegistry. This class extends SAFRegistry and
 * overrides only the methods that utilize authorized SAF services.
 */
public class SAFAuthorizedRegistry extends SAFRegistry {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFAuthorizedRegistry.class);

    /**
     * /**
     * Reference to the SAFCredentialsService, which handles creating, deleting
     * and otherwise managing SAF credentials.
     */
    private SAFCredentialsService _safCredentialsService = null;

    /**
     * The realm associated with this registry. Cached here on the first
     * call to getRealm().
     */
    private String _realm = null;

    /**
     *
     * Indicator if a failover to the unauthorized registry has occurred
     */
    private boolean _failoverOccurred = false;

    /**
     * CTOR. For testing purposes only.
     */
    protected SAFAuthorizedRegistry(SAFRegistryConfig config,
                                    SAFCredentialsService safCredentialsService) {
        super(config);
        _safCredentialsService = safCredentialsService;
    }

    /**
     * CTOR.
     */
    public SAFAuthorizedRegistry(SAFRegistryConfig config,
                                 NativeMethodManager nativeMethodManager,
                                 SAFCredentialsService safCredentialsService) {
        super(config, nativeMethodManager);
        _safCredentialsService = safCredentialsService;
    }

    /**
     * {@inheritDoc}
     *
     * This method authenticates the user via authorized native services (via
     * SAFCredentialsService). The authorized native service returns a credential
     * that can be used later for authorization.
     */
    @Override
    @FFDCIgnore(SAFException.class)
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {

        issueActivationMessage("authorized");

        assertNotEmpty(userSecurityName, "userSecurityName is null");
        assertNotEmpty(password, "password given for user " + userSecurityName + " is null");
        SAFCredential safCred = null;

        try {
            safCred = _safCredentialsService.createPasswordCredential(userSecurityName, password, null);
            this.generateAuthorizedActivatedMessage();

            if (safCred != null) {
                return SAFSecurityName.create(userSecurityName, _safCredentialsService.getSAFCredentialTokenKey(safCred));
            }
        } catch (SAFException se) {
            // we detect and suppress the SAFException for well-known RACF return/
            // reason codes (e.g. bad password), And percolate for unknown rc/rsn and/or real
            // SAF failures. Penalty box errors will failover to unauthorized registry.

            if (se.isPenaltyBoxError() && isFailoverEnabled()) {
                this.generateUnauthorizedFailoverMessage();
                return super.checkPassword(userSecurityName, password);
            } else if (se.isPasswordExpiredError() && isReportPasswordExpiredEnabled()) {
                throw new PasswordExpiredException(se.getMessage(), se);
            } else if (se.isUserRevokedError() && isReportUserRevokedEnabled()) {
                throw new UserRevokedException(se.getMessage(), se);
            } else if (se.isSevere()) {
                throw new RegistryException(se.getMessage(), se);
            } else {
                se.logIfUnexpected();
            }
        }

        return null; // If we didn't get a SAFCredential back, then authentication failed.  Return null.
    }

    /**
     * {@inheritDoc}
     *
     * Uses authorized SAF services (initACEE) to validate a user.
     *
     * If the given userSecurityName already contains a SAFCredentialToken key in it,
     * and that key represents a valid, still-existing SAFCredentialToken, then this
     * method will return true. If the key does NOT represent a valid SAFCredentialToken
     * (i.e the SAFCredentialToken has been deleted), this method returns false.
     *
     * If the given userSecurityName does not contain a SAFCredentialToken key, then
     * the code will attempt to create an ASSERTED credential for it via initACEE.
     *
     * Note that the unauthorized service getpwent (called by SAFRegistry.isValidUser)
     * also validates a user; however it returns success even for REVOKED users.
     */
    @Override
    @FFDCIgnore(SAFException.class)
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");

        boolean isValid = false;
        final int MSG_SUPPRESS = 1;
        try {
            String safCredTokenKey = SAFSecurityName.parseKey(userSecurityName);
            if (safCredTokenKey != null) {
                isValid = (null != _safCredentialsService.getCredentialFromKey(safCredTokenKey));
            } else {
                SAFCredential safCred = _safCredentialsService.createAssertedCredential(userSecurityName, null, MSG_SUPPRESS);
                this.generateAuthorizedActivatedMessage();

                isValid = true; // If createAssertedCredential didn't throw a SAFException, then it worked.

                // Immediately delete the credential we just created. We needed it only to verify the user exists.
                _safCredentialsService.deleteCredential(safCred);
            }
        } catch (SAFException se) {
            if (se.isPenaltyBoxError() && isFailoverEnabled()) {
                this.generateUnauthorizedFailoverMessage();
                return super.isValidUser(SAFSecurityName.parseUserId(userSecurityName));
            } else if (se.isSevere()) {
                throw new RegistryException(se.getMessage(), se);
            } else {
                se.logIfUnexpected();
            }
        }
        return isValid;
    }

    /**
     * {@inheritDoc}
     *
     * Uses authorized SAF services (RACROUTE Extract) to validate a group.
     *
     * RACROUTE Extract is called with groupSecurityName, if SAF RC = 4, RACF RC = 8, and
     * RACF RSN = 0, then the group does not exist and the native code returns false. Otherwise,
     * it returns true.
     *
     * If there is a penalty box error, the was return code is set and returned from the native
     * code and this method will either handle the failover by calling unauthorized services or
     * return false.
     *
     * Note that the unauthorized service getgrnam_r (called by SAFRegistry.isValidGroup)
     * also validates a group; however it returns false for any group that does not have a GID.
     */
    @Override
    @FFDCIgnore(SAFException.class)
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        assertNotEmpty(groupSecurityName, "groupSecurityName is null");

        boolean isValid = false;
        String profilePrefix = _safCredentialsService.getProfilePrefix();
        try {
            SAFServiceResult safServiceResult = new SAFServiceResult();
            isValid = ntv_isValidGroupAuthorized(NativeMethodUtils.convertToEBCDIC(groupSecurityName.toUpperCase()),
                                                 NativeMethodUtils.convertToEBCDIC(profilePrefix),
                                                 safServiceResult.getBytes());
            // Throw exception if we got an error in native code
            if (safServiceResult.getWasReturnCode() != 0) {
                throw new SAFException(safServiceResult);
            }
        } catch (SAFException se) {
            if (se.isPenaltyBoxError() && isFailoverEnabled()) {
                this.generateUnauthorizedFailoverMessage();
                return super.isValidGroup(groupSecurityName);
            } else if (se.isSevere()) {
                throw new RegistryException(se.getMessage(), se);
            } else {
                se.logIfUnexpected();
            }
        }
        return isValid;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(value = { SAFException.class })
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapFailedException, RegistryException, CertificateMapNotSupportedException {

        issueActivationMessage("authorized");

        SAFCredential safCredential = null;
        assertNotEmpty(chain, "no certificates in chain");
        assertNotNull(chain[0], "certificate is null");

        try {
            safCredential = _safCredentialsService.createCertificateCredential(chain[0], null);
            this.generateAuthorizedActivatedMessage();

            if (safCredential == null) {
                throw new CertificateMapFailedException("Certificate could not be mapped to a valid SAF user ID");
            }

            return SAFSecurityName.create(safCredential.getUserId(), _safCredentialsService.getSAFCredentialTokenKey(safCredential));
        } catch (SAFException se) {
            if (se.isPenaltyBoxError() && isFailoverEnabled()) {
                this.generateUnauthorizedFailoverMessage();
                return super.mapCertificate(chain);
            } else {
                throw new CertificateMapFailedException(se.getMessage(), se);
            }
        }

    }

    /**
     * {@inheritDoc}
     *
     * If no realm is defined in the config, this method will attempt to EXTRACT
     * the APPDATA from the SAFDFLT profile in the REALM class and use that as
     * the realm name.
     */
    @Override
    public String getRealm() {
        if (_realm == null) {
            if (_config.realm() == null || _config.realm().length() == 0) {
                _realm = NativeMethodUtils.convertToASCII(ntv_getRealm());
                if (_realm == null || _realm.length() == 0) {
                    // Got nothing back from SAF.  Revert to the default value, which would be the SYSPLEX name.
                    _realm = getDefaultRealm();
                }
            } else {
                _realm = _config.realm();
            }
        }
        return _realm;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");
        if (super.isIncludeSafGroupsEnabled()) {
            return super.getGroupsForUserFull(SAFSecurityName.parseUserId(userSecurityName));
        } else {
            return super.getGroupsForUser(SAFSecurityName.parseUserId(userSecurityName));
        }

    }

    /**
     * {@inheritDoc}
     *
     * This method is called by login modules to obtain the uniqueId that is placed
     * in the WSPrincipal's AccessId. The userSecurityName may contain a safCredTokenKey
     * in it (if it was obtained via a previous call to checkPassword or mapCertificate).
     * This method will strip out the safCredTokenKey, if present, to ensure that it
     * doesn't get into the WSPrincipal.
     */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!isValidUser(userSecurityName))
            throw new EntryNotFoundException(userSecurityName + " is not a valid user");
        return SAFSecurityName.parseUserId(userSecurityName);
    }

    /**
     * {@inheritDoc}
     *
     * This method is called by login modules to obtain the securityName for the WSPrincipal.
     * The uniqueUserId may contain a safCredTokenKey in it (if it was obtained via a previous
     * call to checkPassword or mapCertificate). This method will strip out the safCredTokenKey,
     * if present, to ensure that it doesn't get into the WSPrincipal.
     */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (!isValidUser(uniqueUserId))
            throw new EntryNotFoundException(uniqueUserId + " is not a valid user");
        return SAFSecurityName.parseUserId(uniqueUserId);
    }

    /**
     * If we are trying to use the SAFAuthorizedRegistry and the penalty box has not been
     * configured then we will failover to the unauthorized registry. Generate
     * a message to indicate this, but only on the first occurrence.
     */
    private void generateUnauthorizedFailoverMessage() {
        if (!_failoverOccurred) {
            _failoverOccurred = true;
            Tr.warning(tc, "PENALTY_BOX_FALLBACK", _safCredentialsService.getProfilePrefix());
        }
    }

    /**
     * If we are using the SAFAuthorizedRegistry and we previously fell back to
     * the unauthorized registry, generate a message to indicate this, but only
     * on the first occurrence.
     */
    private void generateAuthorizedActivatedMessage() {
        if (_failoverOccurred) {
            _failoverOccurred = false;
            Tr.info(tc, "PENALTY_BOX_RECOVERY", _safCredentialsService.getProfilePrefix());
        }
    }
}
