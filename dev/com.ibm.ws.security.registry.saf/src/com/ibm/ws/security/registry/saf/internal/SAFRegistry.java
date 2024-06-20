/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.saf.internal;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;

/**
 * Unauthorized User Registry implementation for a z/OS SAF security product.
 *
 * By "unauthorized" we mean that it uses USS services like __passwd to do authentication,
 * not z/OS authorized system services like initACEE. See SAFAuthorizedRegistry for the
 * "authorized" version.
 */
public class SAFRegistry implements UserRegistry {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFRegistry.class);

    /**
     * The realm that this user registry represents.
     */
    protected String _realm;

    /**
     * The configuration associated with this SAFRegistry.
     */
    protected final SAFRegistryConfig _config;

    /**
     * Flag indicates whether the activation message has been issued.
     */
    private boolean hasActivationMessageBeenIssued = false;

    /**
     * Flag indicates whether the IncludeSafGroups message has been issued.
     */
    private boolean hasIncludeSafGroupsMessageBeenIssued = false;

    /**
     * Lock for getGroups method. We use this Object for sync'ing so that
     * we don't have to sync the SAFRegistry object.
     */
    private final static class GetGroupsLock extends Object {
    }

    GetGroupsLock getGroupsLock = new GetGroupsLock();

    /**
     * Lock for getUsers method. We use this Object for sync'ing so that
     * we don't have to sync the SAFRegistry object.
     */
    private final static class GetUsersLock extends Object {
    }

    GetUsersLock getUsersLock = new GetUsersLock();

    /**
     * Store the error number for the daemon program control message
     */
    public static final int emvSerrErr = 157;

    /**
     * CTOR. Testing purposes only.
     */
    protected SAFRegistry(SAFRegistryConfig config) {
        _config = config;
    }

    /**
     * CTOR.
     */
    public SAFRegistry(SAFRegistryConfig config, NativeMethodManager nativeMethodManager) {
        _config = config;

        // Attempt to load native code via the method manager.
        nativeMethodManager.registerNatives(SAFRegistry.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return "SAF";
    }

    /**
     * {@inheritDoc}
     *
     * @return if success, returns userSecurityName; otherwise returns null.
     */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {

        issueActivationMessage("unauthorized");

        assertNotEmpty(userSecurityName, "userSecurityName is null");
        assertNotEmpty(password, "password given for user " + userSecurityName + " is null");

        try {
            return (checkPasswordHelper(userSecurityName, password)) ? userSecurityName : null;
        } catch (RegistryException re) {
            // RegistryException is thrown for unexpected __passwd errors.
            // Just FFDC it.
            return null;
        }
    }

    /**
     * @return true if authentication succeeds; false otherwise
     *
     * @throws RegistryException if unexpected __passwd error occurs.
     */
    protected boolean checkPasswordHelper(String userSecurityName, @Sensitive String password) throws RegistryException {

        PasswdResult passwdResult = new PasswdResult();

        boolean success = ntv_checkPassword(NativeMethodUtils.convertToEBCDIC(userSecurityName),
                                            NativeMethodUtils.convertToEBCDICNoTrace(password),
                                            null, // appl-id
                                            passwdResult.getBytes());

        if (!success && !passwdResult.isNormalAuthFailure()) {
            // If this is an EMVSERR error, print the relevant message
            if (passwdResult.getErrno() == emvSerrErr) {
                Tr.error(tc, "DAEMON_LOGIN_ERROR");
            }
            throw new RegistryException(passwdResult.getMessage(userSecurityName));
        }

        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (!isValidGroup(groupSecurityName))
            throw new EntryNotFoundException(groupSecurityName + " is not a valid group");
        return groupSecurityName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        if (!isValidGroup(uniqueGroupId))
            throw new EntryNotFoundException(uniqueGroupId + " is not a valid group");
        return uniqueGroupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        assertNotEmpty(pattern, "pattern is null");
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        boolean hasMore = false;
        List<String> groups = new ArrayList<String>();
        byte[] egroup = null;
        String group = null;

        synchronized (getGroupsLock) {
            if (!ntv_resetGroupsCursor())
                throw new RegistryException("Failed to reset SAF user database");
            while ((egroup = ntv_getNextGroup()) != null) {
                group = NativeMethodUtils.convertToASCII(egroup);
                if (regex.matcher(group).matches()) {
                    if (limit != 0 && groups.size() >= limit) {
                        hasMore = true;
                        break;
                    }
                    groups.add(group);
                }
            }
            if (!ntv_closeGroupsDB())
                throw new RegistryException("Failed to close SAF user database");
        }

        SearchResult sr = new SearchResult(groups, hasMore);

        return sr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResult getUsersForGroup(String groupSecurityName, int limit) throws EntryNotFoundException, RegistryException, IllegalArgumentException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit is less than zero");
        }

        if (!isValidGroup(groupSecurityName)) {
            throw new EntryNotFoundException(groupSecurityName + " does not exist");
        }

        try {
            List<byte[]> emembers = ntv_getUsersForGroup(NativeMethodUtils.convertToEBCDIC(groupSecurityName), new ArrayList<byte[]>());
            if (emembers == null) {
                String msg = "JNI failure while looking up group " + groupSecurityName;
                throw new RegistryException(msg);
            }

            List<String> members = NativeMethodUtils.convertToASCII(emembers);
            if (limit > 0 && limit < members.size()) {
                return new SearchResult(members.subList(0, limit), Boolean.TRUE);
            } else {
                return new SearchResult(members, Boolean.FALSE);
            }
        } catch (RegistryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RegistryException(t.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");
        if (isIncludeSafGroupsEnabled() && !hasIncludeSafGroupsMessageBeenIssued) {
            hasIncludeSafGroupsMessageBeenIssued = true;
            Tr.warning(tc, "UNAUTH_INCLUDE_SAF");
        }
        List<String> groups = null;
        List<byte[]> egroups = null;
        try {
            egroups = ntv_getGroupsForUser(NativeMethodUtils.convertToEBCDIC(userSecurityName), new ArrayList<byte[]>());
            groups = NativeMethodUtils.convertToASCII(egroups);

            if (groups.size() == 0 && !isValidUser(userSecurityName))
                throw new EntryNotFoundException("User " + userSecurityName + " not valid");

        } catch (EntryNotFoundException enfe) {
            throw enfe;
        } catch (Throwable t) {
            throw new RegistryException(t.toString());
        }

        return groups;
    }

    /**
     * Returns the names of all the groups that <i>userSecurityName</i> belongs to. Includes non OMVS groups.
     *
     * @param userSecurityName the name of the user.
     * @return a List of group names that the user belongs to.
     *         <code>null</code> is not returned.
     * @exception EntryNotFoundException   if uniqueUserId does not exist.
     * @exception RegistryException        if there is any UserRegistry specific problem
     * @exception IllegalArgumentException if userSecurityName is <code>null</code> or empty
     **/
    protected List<String> getGroupsForUserFull(String userSecurityName) throws EntryNotFoundException, RegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");
        List<String> groups = null;
        List<byte[]> egroups = null;
        SAFServiceResult safServiceResult = new SAFServiceResult();

        try {
            egroups = ntv_getGroupsForUserFull(NativeMethodUtils.convertToEBCDIC(userSecurityName), new ArrayList<byte[]>(),
                                               safServiceResult.getBytes());

            groups = NativeMethodUtils.convertToASCII(egroups);

            if (groups.size() == 0 && !isValidUser(userSecurityName))
                throw new EntryNotFoundException("User " + userSecurityName + " not valid");

            for (int i = 0; i < groups.size(); i++) {
                groups.set(i, groups.get(i).trim());
            }
        } catch (EntryNotFoundException enfe) {
            throw enfe;
        } catch (Throwable t) {
            throw new RegistryException(t.toString());
        }
        return groups;
    }

    /**
     * {@inheritDoc}
     *
     * First checks the config. If not configured, then the system's PLEX name is used.
     */
    @Override
    public String getRealm() {
        if (_realm == null) {
            _realm = _config.realm(); // Try the config.
            if (_realm == null || _realm.length() == 0) {
                _realm = getDefaultRealm(); // Use the server's plexname.
            }
        }
        return _realm;
    }

    /**
     * @return the plex name where the server is running.
     */
    protected String getDefaultRealm() {
        String hostname = NativeMethodUtils.convertToASCII(ntv_getPlexName());
        return hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (!isValidGroup(groupSecurityName))
            throw new EntryNotFoundException(groupSecurityName + " is not a valid group");
        return groupSecurityName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        return getGroupsForUser(uniqueUserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!isValidUser(userSecurityName))
            throw new EntryNotFoundException(userSecurityName + " is not a valid user");
        return userSecurityName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!isValidUser(userSecurityName))
            throw new EntryNotFoundException(userSecurityName + " is not a valid user");
        return userSecurityName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (!isValidUser(uniqueUserId))
            throw new EntryNotFoundException(uniqueUserId + " is not a valid user");
        return uniqueUserId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        assertNotEmpty(pattern, "pattern is null");
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        boolean hasMore = false;
        List<String> users = new ArrayList<String>();
        byte[] euser = null;
        String user = null;

        try {
            synchronized (getUsersLock) {
                if (!ntv_resetUsersCursor())
                    throw new RegistryException("Failed to reset SAF user database");
                while ((euser = ntv_getNextUser()) != null) {
                    user = NativeMethodUtils.convertToASCII(euser);
                    if (regex.matcher(user).matches()) {
                        if (limit != 0 && users.size() >= limit) {
                            hasMore = true;
                            break;
                        }
                        users.add(user);
                    }
                }
                if (!ntv_closeUsersDB())
                    throw new RegistryException("Failed to close SAF user database");
            }
        } catch (RegistryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RegistryException(t.toString());
        }

        return new SearchResult(users, hasMore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        assertNotEmpty(groupSecurityName, "groupSecurityName is null");
        return ntv_isValidGroup(NativeMethodUtils.convertToEBCDIC(groupSecurityName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");
        return ntv_isValidUser(NativeMethodUtils.convertToEBCDIC(userSecurityName));
    }

    @Override
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {

        issueActivationMessage("unauthorized");

        assertNotEmpty(chain, "no certificates in chain");
        assertNotNull(chain[0], "certificate is null");

        try {
            final byte[] encodedCert = chain[0].getEncoded();
            String securityName = NativeMethodUtils.convertToASCII(ntv_mapCertificate(encodedCert, encodedCert.length));

            if (securityName == null) {
                throw new CertificateMapFailedException("Certificate could not be mapped to a valid SAF user ID");
            }
            return securityName;

        } catch (CertificateEncodingException cee) {
            throw new CertificateMapFailedException("CertificateEncodingException", cee);
        }
    }

    /**
     * Simple utility method that compares the first parm to null, and if it is,
     * throws an IllegalArgumentException using the second parm as the message.
     */
    @Trivial
    protected void assertNotNull(Object o, String msg) {
        if (o == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Simple utility method that checks if the first parm is null or an
     * empty array, and if it is, throws an IllegalArgumentException using
     * the second parm as the message.
     */
    @Trivial
    protected void assertNotEmpty(Object[] a, String msg) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Simple utility method that checks if the first parm is null or the
     * empty string "", and if it is, throws an IllegalArgumentException using
     * the second parm as the message.
     */
    @Trivial
    protected void assertNotEmpty(String s, String msg) {
        if (s == null || s.length() == 0) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * @return the config attribute: <safRegistry enableFailover="true|false" />.
     *         Default is true.
     */
    protected boolean isFailoverEnabled() {
        return _config.enableFailover();
    }

    protected boolean isReportPasswordExpiredEnabled() {
        return _config.reportPasswordExpired();
    }

    protected boolean isReportPasswordChangeDetailsEnabled() {
        return _config.reportPasswordChangeDetails();
    }

    protected boolean isReportUserRevokedEnabled() {
        return _config.reportUserRevoked();
    }

    protected boolean isIncludeSafGroupsEnabled() {
        return _config.includeSafGroups();
    }

    /**
     * Issue message indicating which type of SAFRegistry (auth vs unauth) is loaded.
     * This method is called "lazily" when the registry is first used.
     * By "used" I mean the first call to either checkPassword or mapCertificate.
     *
     * We issue lazily (instead of in the CTOR) because the SAFRegistry version might
     * bounce between the two during startup as config is processed.
     *
     */
    protected void issueActivationMessage(String fillIn) {
        if (!hasActivationMessageBeenIssued) {
            Tr.info(tc, "ACTIVATION_MESSAGE", fillIn);
            hasActivationMessageBeenIssued = true;
        }
    }

    /**
     * Native methods (security_saf_registry.c).
     */
    protected native boolean ntv_checkPassword(byte[] user, byte[] pwd, String applid, byte[] passwdResult);

    protected native byte[] ntv_getRealm();

    protected native boolean ntv_isValidUser(byte[] userSecurityName);

    protected native boolean ntv_isValidGroup(byte[] groupSecurityName);

    protected native byte[] ntv_mapCertificate(byte[] cert, int certLength);

    protected native List<byte[]> ntv_getGroupsForUser(byte[] userName, List<byte[]> list);

    protected native List<byte[]> ntv_getGroupsForUserFull(byte[] userName, List<byte[]> list, byte[] safServiceResults);

    protected native boolean ntv_resetGroupsCursor();

    protected native byte[] ntv_getNextGroup();

    protected native boolean ntv_closeGroupsDB();

    protected native boolean ntv_resetUsersCursor();

    protected native byte[] ntv_getNextUser();

    protected native boolean ntv_closeUsersDB();

    protected native byte[] ntv_getPlexName();

    protected native List<byte[]> ntv_getUsersForGroup(byte[] groupName, List<byte[]> list);

    protected native byte[] ntv_racrouteExtract(byte[] className,
                                                byte[] profileName,
                                                byte[] fieldName,
                                                byte[] safServiceResults);

    protected native boolean ntv_isValidGroupAuthorized(byte[] groupSecurityName,
                                                        byte[] applname,
                                                        byte[] safServiceResults);
}
