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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.cache.CacheEvictionListener;
import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFSecurityName;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * SAFCredentialsServiceImpl is an implementation of the SAFCredentialsServices, which
 * creates and destroys and manages all aspects of native SAF credentials on z/OS.
 *
 * The SAFCredentialsService depends on native SAF services. The native SAF services
 * can only be invoked under a native authorized routine. The authorized routines
 * declare their availability to OSGi by registering a com.ibm.ws.zos.core.NativeService.
 *
 * The NativeService is a Java representation of the authorized routine. It contains
 * a few properties, the main ones being:
 *
 * "native.service.name": the name of the authorized routine (as defined in server_authorized_functions.def)
 * "native.service.authorization.group": the authorization group for the routine
 * "is.authorized": flag indicates whether or not the server is authorized to access the routine
 *
 * In the bnd.bnd, SAFCredentialsServiceImpl declares a DS dependency for every
 * NativeService it requires. Each declaration is filtered for is.authorized=true.
 * This ensures that the server is authorized to use the NativeService.
 *
 * SAFCredentialsServiceImpl will be loaded and activated by OSGi ONLY WHEN ALL OF
 * ITS DS DEPENDENCY REQUIREMENTS ARE SATISFIED. Thus, it will be loaded and activated
 * only if the authorized routines (NativeServices) are loaded (i.e. Angel is running)
 * and the server is authorized to use them (is.authorized=true).
 *
 * This is how SAFCredentialsServiceImpl determines whether or not it should run.
 * Basically, OSGi figures it out. If SAFCredentialsServiceImpl gets loaded, then it
 * can safely assume that all of its NativeService dependencies are satisfied.
 */
//<OCD id="com.ibm.ws.security.credentials.saf" name="%saf.credentials.config" description="%saf.credentials.config.desc" ibm:alias="safCredentials">
//<AD id="unauthenticatedUser" name="%unauthenticatedUser" description="%unauthenticatedUser.desc"
//    required="false" type="String"
//    default="WSGUEST" />
//<AD id="profilePrefix" name="%profilePrefix" description="%profilePrefix.desc"
//    required="false" type="String"
//    default="BBGZDFLT" />
//<AD id="mapDistributedIdentities" name="%mapDistributedIdentities" description="%mapDistributedIdentities.desc"
//    required="false" type="Boolean"
//    default="false"/>
//</OCD>

@ObjectClassDefinition(pid = "com.ibm.ws.security.credentials.saf", name = "%saf.credentials.config", description = "%saf.credentials.config.desc", localization = Ext.LOCALIZATION)
@Ext.Alias("safCredentials")
@interface SAFCredentialsConfig {

    @AttributeDefinition(name = "%unauthenticatedUser", description = "%unauthenticatedUser.desc", required = false, defaultValue = "WSGUEST")
    String unauthenticatedUser();

    @AttributeDefinition(name = "%profilePrefix", description = "%profilePrefix.desc", required = false, defaultValue = "BBGZDFLT")
    String profilePrefix();

    //required to require actual configuration
    @AttributeDefinition(name = "%mapDistributedIdentities", description = "%mapDistributedIdentities.desc")
    boolean mapDistributedIdentities();

    @AttributeDefinition(name = "%suppressAuthFailureMessages", description = "%suppressAuthFailureMessages.desc", required = false, defaultValue = "true")
    boolean suppressAuthFailureMessages();
}

@Component(configurationPid = "com.ibm.ws.security.credentials.saf",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           reference = { @Reference(name = "NativeServiceCRCREDPW", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=CRCREDPW)(is.authorized=true))"),
                         @Reference(name = "NativeServiceCRCREDAS", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=CRCREDAS)(is.authorized=true))"),
                         @Reference(name = "NativeServiceDELCRED", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=DELCRED)(is.authorized=true))"),
                         @Reference(name = "NativeServiceGETREALM", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=GETREALM)(is.authorized=true))"),
                         @Reference(name = "NativeServiceISRESTRC", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=ISRESTRC)(is.authorized=true))") },
           property = { "service.vendor=IBM", "type=SAFCredential" })
public class SAFCredentialsServiceImpl implements SAFCredentialsService, CredentialProvider, CacheEvictionListener, Introspector {
    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFCredentialsServiceImpl.class);

    /**
     * Default audit string for creating a password credential.
     */
    private final static String DEFAULT_PASSWORD_AUDIT_STRING = "WebSphere Userid/Password Login";

    /**
     * Default audit string for creating a user credential.
     */
    private final static String DEFAULT_AUTHORIZED_CREATE_AUDIT_STRING = "WebSphere Authorized Login";

    /**
     * Default audit string for creating the unauthenticated user's credential.
     */
    private final static String DEFAULT_UNAUTHENTICATED_AUDIT_STRING = "WebSphere Default/Unauthenticated Login";

    /**
     * Default audit string for creating a certificate credential.
     */
    private final static String DEFAULT_CERTIFICATE_AUDIT_STRING = "WebSphere Certificate Login";

    /**
     * Default audit string for creating a mapped credential.
     */
    private final static String DEFAULT_MAPPED_AUDIT_STRING = "WebSphere Mapped Login";

    /**
     * Default audit string for creating the server's credential.
     */
    private final static String DEFAULT_SERVER_AUDIT_STRING = "WebSphere Server Identity";

    /**
     * Number of times to retry the native operation when it fails
     * because the SAF cred token is being freed
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * The NativeMethodManager service for loading native methods.
     */
    private NativeMethodManager nativeMethodManager;

    /**
     * Maps SAFCredentials to SAFCredentialTokens.
     * Marked 'protected' for testing purposes only.
     */
    protected final SAFCredTokenMap safCredTokenMap = new SAFCredTokenMap(this);

    /**
     * Indicates whether the SAF product supports mixed-case passwords.
     */
    private boolean isMixedCasePWEnabled = false;

    /**
     * The unauthenticated user config attribute name on the <safCredentials> config element.
     */
    public static final String UNAUTHENTICATED_USER_PROPERTY = "unauthenticatedUser";

    /**
     * The profile prefix config attribute name from the <safCredentials> config element.
     */
    public static final String PROFILE_PREFIX_PROPERTY = "profilePrefix";

    /**
     * The map distributed identities config attribute name from the <safCredentials> config element.
     */
    public static final String MAP_DISTRIBUTED_IDENTITIES_PROPERTY = "mapDistributedIdentities";

    /**
     * Indicates weather distributed identities are to be mapped to SAF user IDs.
     */
    private volatile boolean mapDistributedIdentities = false;
    /**
     * The unauthenticated userId.
     */
    private String unauthenticatedUser = null;

    /**
     * The credential for the unauthenticated user.
     */
    private SAFCredential defaultCred = null;

    /**
     * The credential for the server.
     */
    private SAFCredential serverCred = null;

    /**
     * The profile prefix/security domain.
     */
    private String profilePrefix = null;

    /**
     * Indicates whether we've already logged the message about the unauthenticated
     * user not having its "RESTRICTED" attribute set.
     */
    private boolean hasRestrictedBeenChecked = false;

    /**
     * Controls whether or not SAF is permitted to log messages about whether the
     * server or the user is allowed to access profiles protected by WZSSAD.
     */
    private boolean suppressAuthFailureMessages = true;

    //circular reference, but better than the alternatives.
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(com.ibm.ws.security.registry.type=SAF)")
    private volatile ServiceReference<UserRegistry> safUserRegistry;

    /**
     * Security credentials service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    volatile CredentialsService cs;

    @Activate
    protected void activate(SAFCredentialsConfig config) {
        updateConfig(config);
    }

    @Modified
    protected void modify(SAFCredentialsConfig config) {
        updateConfig(config);
    }

    /**
     * Clean up all SAFCredentialTokens (RACOs) in the safCredTokenMap.
     */
    @Deactivate
    protected void deactivate() {
        cleanupCredTokenMap();
    }

    /**
     * Read the config from the given property map.
     */
    protected void updateConfig(SAFCredentialsConfig config) {
        String tmpUnauthenticatedUser = config.unauthenticatedUser();
        if (unauthenticatedUser == null || !unauthenticatedUser.equals(tmpUnauthenticatedUser)) {
            // Either the unauthenticatedUser has been set for the first time, or it's been
            // changed.  Reset the defaultCred and notify the CredentialsService.
            unauthenticatedUser = tmpUnauthenticatedUser;

            SAFCredential tmp = defaultCred;
            defaultCred = null;
            try {
                deleteCredential(tmp);
            } catch (SAFException se) {
                // Nothing much to do here other than FFDC.
            }

            cs.setUnauthenticatedUserid(unauthenticatedUser);
        }

        setProfilePrefix(config.profilePrefix());
        mapDistributedIdentities = config.mapDistributedIdentities();

        suppressAuthFailureMessages = config.suppressAuthFailureMessages();
        ntv_setPenaltyBoxDefaults(suppressAuthFailureMessages);
    }

    /**
     * Set the profile prefix.
     *
     * If the profile prefix has changed, flush out the native penalty box cache
     * (the penalty box keys off the profile prefix).
     *
     */
    protected void setProfilePrefix(String newProfilePrefix) {
        if (profilePrefix != null && !profilePrefix.equals(newProfilePrefix)) {
            // The profilePrefix has changed.  Flush the native penalty box cache.
            ntv_flushPenaltyBoxCache();
        }
        profilePrefix = newProfilePrefix;
    }

    @Reference
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        this.nativeMethodManager.registerNatives(SAFCredentialsServiceImpl.class);

        isMixedCasePWEnabled = ntv_isMixedCasePWEnabled();
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * Delete all SAFCredentialTokens in the safCredTokenMap.
     *
     * This method is called from SAFCredentialsServiceImpl.deactivate.
     */
    private void cleanupCredTokenMap() {
        Collection<SAFCredentialToken> allTokens = safCredTokenMap.values();
        for (SAFCredentialToken token : allTokens) {
            tryToDeleteSAFCredentialToken(token);
        }
        safCredTokenMap.clear();
    }

    /**
     * Create a SAF credential (i.e an ACEE/RACO) using the given user, password,
     * and audit string.
     *
     * @param userSecurityName
     * @param password
     * @param auditString
     *
     * @return A SAFCredential that represents the native credential. If the credential
     *         failed to be created, then null is returned, or an exception is thrown.
     *
     * @throws SAFException if the credential could not be created.
     */
    @Override
    public SAFCredential createPasswordCredential(String userSecurityName,
                                                  @Sensitive String password,
                                                  String auditString) throws SAFException {

        auditString = firstNotNull(auditString, DEFAULT_PASSWORD_AUDIT_STRING);

        // Create the credential.
        SAFCredential safCred = null;
        SAFServiceResult safResult = new SAFServiceResult();
        byte[] safCredTokenBytes = ntv_createPasswordCredential(NativeMethodUtils.convertToEBCDIC(normalizeUserId(userSecurityName)),
                                                                NativeMethodUtils.convertToEBCDICNoTrace(normalizePassword(password)),
                                                                NativeMethodUtils.convertToEBCDIC(auditString),
                                                                NativeMethodUtils.convertToEBCDIC(profilePrefix),
                                                                safResult.getBytes());
        // If we got back non-null, then it worked!
        if (safCredTokenBytes != null) {

            SAFCredentialToken safCredToken = new SAFCredentialToken(safCredTokenBytes);
            safCred = new SAFCredentialImpl(userSecurityName, auditString, SAFCredential.Type.BASIC);
            ((SAFCredentialImpl) safCred).setAuthenticated(true);

            safCredTokenMap.put(safCred, safCredToken);

        } else {
            safResult.setAuthenticationFields(userSecurityName, profilePrefix);
            safResult.throwSAFException();
        }

        return safCred;
    }

    /**
     * Try to delete the given SAFCredentialToken. Catch and ignore any exceptions.
     */
    private void tryToDeleteSAFCredentialToken(SAFCredentialToken safCredToken) {
        try {
            deleteSAFCredentialToken(safCredToken);
        } catch (SAFException se) {
            // Record an FFDC and move on.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential getDefaultCredential() throws SAFException {
        SAFCredential retMe = defaultCred;
        if (retMe == null) {
            retMe = defaultCred = createLazyAssertedCredential(unauthenticatedUser, DEFAULT_UNAUTHENTICATED_AUDIT_STRING);
        }

        return retMe;
    }

    /**
     * Check whether the default credential (for the unauthenticated user)
     * has the "RESTRICTED" attribute set. If not a message is issued.
     */
    protected void checkDefaultCredentialIsRestricted() throws SAFException {

        if (!hasRestrictedBeenChecked && defaultCred != null) {
            if (!isRESTRICTED(defaultCred)) {
                // The unauthenticated user does not have the RESTRICTED attribute set.
                // Issue a warning message about this and continue on.
                Tr.warning(tc, "UNAUTHENTICATED_USER_NOT_RESTRICTED", defaultCred.getUserId());
            }
            hasRestrictedBeenChecked = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential getServerCredential() {
        SAFCredential retMe = serverCred;
        if (retMe == null) {
            String serverUserId = AccessController.doPrivileged(
                                                                new PrivilegedAction<String>() {
                                                                    @Override
                                                                    public String run() {
                                                                        return System.getProperty("user.name");
                                                                    }
                                                                });

            retMe = serverCred = new SAFCredentialImpl(serverUserId, DEFAULT_SERVER_AUDIT_STRING, SAFCredential.Type.SERVER);
        }
        return retMe;
    }

    /**
     * Determine if the user associated with the given credential has the
     * RESTRICTED attribute set. This is determined by checking the aceeraui
     * bit in the ACEE.
     *
     * @param SAFCredential The credential of the user to check.
     *
     * @return true if the user is RESTRICTED; false if not.
     *
     * @throws SAFException If a SAF error occurred (most likely trying to create
     *                          the ACEE).
     */
    protected boolean isRESTRICTED(SAFCredential safCred) throws SAFException {
        SAFServiceResult safResult;
        int retryCount = 0;
        int rc = 0;

        do {
            safResult = new SAFServiceResult();

            rc = ntv_isRESTRICTED(getSAFCredentialTokenBytes(safCred),
                                  safResult.getBytes());
        } while (rc != 0 && rc != 1 && safResult.isRetryable() && retryCount++ < MAX_RETRY_COUNT && SAFServiceResult.yield());

        if (rc == 1) {
            return true;
        } else if (rc == 0) {
            return false;
        } else {
            safResult.throwSAFException();
        }
        return false;
    }

    /**
     * Create an ASSERTED SAF credential (i.e an ACEE/RACO) for the given user.
     *
     * @param userSecurityName The user.
     * @param auditString      An optional audit string used for logging and SMF recording.
     * @param msgSuppress      1 to suppress message, 0 not to suppress message
     *
     * @return A SAFCredential that represents the native credential.
     *
     * @throws SAFException if the credential could not be created.
     */
    @Override
    public SAFCredential createAssertedCredential(String userSecurityName, String auditString, int msgSuppress) throws SAFException {

        // Validate input.
        normalizeUserId(userSecurityName);
        auditString = firstNotNull(auditString, DEFAULT_AUTHORIZED_CREATE_AUDIT_STRING);

        SAFCredential safCred = new SAFCredentialImpl(userSecurityName, auditString, SAFCredential.Type.ASSERTED);
        SAFCredentialToken safCredToken = createAssertedCredentialToken(safCred, msgSuppress);

        safCredTokenMap.put(safCred, safCredToken);

        return safCred;
    }

    /**
     * Create an lazy ASSERTED SAF credential for the given user. Only the java
     * representation of the credential is created. The native half (the RACO) isn't
     * created until it's actually needed, e.g. for authorization.
     *
     * @param userSecurityName
     * @param auditString
     *
     * @return A SAFCredential that represents the native credential.
     *
     * @throws SAFException if the credential could not be created.
     */
    @Override
    public SAFCredential createLazyAssertedCredential(String userSecurityName, String auditString) {
        return new SAFCredentialImpl(userSecurityName, auditString, SAFCredential.Type.ASSERTED);
    }

    /**
     * Create an ASSERTED SAFCredentialToken (i.e a RACO) for the given user.
     *
     * @param userSecurityName The user.
     * @param auditString      An optional audit string used for logging and SMF recording.
     * @param msgSuppress      1 to suppress message, 0 not to suppress message
     *
     * @return A SAFCredentialToken (util_registry.mc token) that represents the native credential.
     *
     * @throws SAFException if the RACO could not be created.
     */
    protected SAFCredentialToken createAssertedCredentialToken(SAFCredential safCred, int msgSuppress) throws SAFException {

        SAFServiceResult safResult = new SAFServiceResult();
        byte[] safCredTokenBytes = ntv_createAssertedCredential(NativeMethodUtils.convertToEBCDIC(normalizeUserId(safCred.getUserId())),
                                                                NativeMethodUtils.convertToEBCDIC(safCred.getAuditString()),
                                                                NativeMethodUtils.convertToEBCDIC(profilePrefix),
                                                                msgSuppress,
                                                                safResult.getBytes());
        if (safCredTokenBytes != null) {
            // We got a token. Wrap and return it.
            // Note: automatically mark ASSERTED tokens as "subjectPopulated".
            //       ASSERTED credentials don't typically get popluated via setCredential().
            //       They're never created for an authentication (password/certificate).
            //       They're usually created only when needed, e.g. when a SAFCredential's native
            //       token has been deleted.
            return new SAFCredentialToken(safCredTokenBytes).setSubjectPopulated(true);
        } else {
            // Null means SAF error.
            safResult.setAuthenticationFields(safCred.getUserId(), profilePrefix);
            safResult.throwSAFException();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential createMappedCredential(String userName,
                                                String registryName,
                                                String auditString) throws SAFException {
        assertNotNull(userName, "userName is null");
        assertNotNull(registryName, "registryName is null");
        auditString = firstNotNull(auditString, DEFAULT_MAPPED_AUDIT_STRING);

        byte[] outputUserId = new byte[8];

        SAFCredentialToken safCredToken = createMappedCredentialToken(userName, registryName, auditString, outputUserId);
        SAFCredential safCred;

        safCred = new SAFCredentialImpl(userName, auditString, NativeMethodUtils.convertToASCII(outputUserId), registryName, this);

        ((SAFCredentialImpl) safCred).setAuthenticated(true);
        safCredTokenMap.put(safCred, safCredToken);

        return safCred;
    }

    /**
     * Create a MAPPED SAFCredentialToken for the given user name and registry name.
     *
     * @param userName     The user name for which to create the credential.
     * @param registryName The registry name for which to create the credential.
     * @param auditString  An audit string used for logging and SMF recording.
     * @param outputUserId SAF userid the user name and registry name was mapped to.
     *
     * @return A SAFCredentialToken that represents the native credential for the user name and registry name.
     *
     * @throws SAFException if the SAFCredentialToken could not be created.
     */
    protected SAFCredentialToken createMappedCredentialToken(String userName,
                                                             String registryName,
                                                             String auditString,
                                                             byte[] outputUserId) throws SAFException {

        SAFServiceResult safResult = new SAFServiceResult();
        byte[] safCredTokenBytes = null;
        byte[] userNameUTF8 = null;
        byte[] registryNameUTF8 = null;
        try {
            userNameUTF8 = (userName + '\0').getBytes("UTF-8");
            registryNameUTF8 = (registryName + '\0').getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("code page conversion error", uee);
        }
        safCredTokenBytes = ntv_createMappedCredential(userNameUTF8,
                                                       registryNameUTF8,
                                                       NativeMethodUtils.convertToEBCDIC(auditString),
                                                       NativeMethodUtils.convertToEBCDIC(profilePrefix),
                                                       outputUserId,
                                                       safResult.getBytes());

        if (safCredTokenBytes != null) {
            // We got a token. Wrap and return it.
            return new SAFCredentialToken(safCredTokenBytes);
        } else {
            // Null means SAF error.
            safResult.setAuthenticationFields(null, profilePrefix);
            safResult.throwSAFException();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential createCertificateCredential(X509Certificate cert, String auditString) throws SAFException {

        auditString = firstNotNull(auditString, DEFAULT_CERTIFICATE_AUDIT_STRING);

        byte[] outputUserId = new byte[8];

        SAFCredentialToken safCredToken = createCertificateCredentialToken(cert, auditString, outputUserId);
        SAFCredential safCred = new SAFCredentialImpl(NativeMethodUtils.convertToASCII(outputUserId), auditString, cert);
        ((SAFCredentialImpl) safCred).setAuthenticated(true);

        safCredTokenMap.put(safCred, safCredToken);

        return safCred;
    }

    /**
     * Create a CERTIFICATE SAFCredentialToken for the given certificate.
     *
     * @param cert        The certificate for which to create the credential.
     * @param auditString An audit string used for logging and SMF recording.
     *
     * @return A SAFCredential that represents the native credential for the certificate.
     *
     * @throws SAFException if the SAFCredential could not be created.
     */
    protected SAFCredentialToken createCertificateCredentialToken(X509Certificate cert, String auditString, byte[] outputUserId) throws SAFException {

        byte[] encodedCertBytes = null;
        try {
            encodedCertBytes = cert.getEncoded();
        } catch (CertificateEncodingException cee) {
            throw new SAFException(cee.getMessage(), cee);
        }

        SAFServiceResult safResult = new SAFServiceResult();
        byte[] safCredTokenBytes = ntv_createCertificateCredential(encodedCertBytes,
                                                                   encodedCertBytes.length,
                                                                   NativeMethodUtils.convertToEBCDIC(auditString),
                                                                   NativeMethodUtils.convertToEBCDIC(profilePrefix),
                                                                   outputUserId,
                                                                   safResult.getBytes());
        if (safCredTokenBytes != null) {
            // We got a token. Wrap and return it.
            return new SAFCredentialToken(safCredTokenBytes);
        } else {
            // Null means SAF error.
            safResult.setAuthenticationFields(null, profilePrefix);
            safResult.throwSAFException();
        }

        return null;
    }

    /**
     * Delete the given SAFCredential. This involves retrieving the SAFCredentialToken
     * associated with the SAFCredential and deleting the RACO storage referenced by
     * the SAFCredentialToken.
     *
     * @param safCredential
     *
     * @throws SAFException if the SAFCredentialToken could not be deleted.
     */
    @Override
    public void deleteCredential(SAFCredential safCredential) throws SAFException {
        if (safCredential != defaultCred) { // Don't ever delete the default credential.
            SAFCredentialToken safCredentialToken = safCredTokenMap.remove(safCredential);
            if (safCredentialToken != null) {
                deleteSAFCredentialToken(safCredentialToken);
            }
        }
    }

    /**
     * Delete the given SAFCredentialToken. Deletes the RACO storage associated with
     * the token.
     *
     * @param safCredentialToken The token to delete
     *
     * @throws SAFException If the SAFCredentialToken could not be deleted.
     */
    protected void deleteSAFCredentialToken(SAFCredentialToken safCredentialToken) throws SAFException {
        int rc = ntv_deleteCredential(safCredentialToken.getBytes());
        if (rc != 0) {
            throw new SAFException("ntv_deleteCredential failed with return code x" + Integer.toHexString(rc));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential getSAFCredentialFromSubject(Subject subject) {
        if (subject == null)
            return null;

        for (Object cred : subject.getPrivateCredentials()) {
            if (cred instanceof SAFCredential) {
                return (SAFCredential) cred;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSAFCredentialTokenKey(SAFCredential safCred) throws SAFException {
        SAFCredentialToken safCredToken = getSAFCredentialToken(safCred);
        return safCredToken.getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getSAFCredentialTokenBytes(SAFCredential safCred) throws SAFException {
        SAFCredentialToken safCredToken = getSAFCredentialToken(safCred);
        return (safCredToken != null) ? safCredToken.getBytes() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SAFCredential getCredentialFromKey(String safCredTokenKey) {
        return safCredTokenMap.getCredential(safCredTokenKey);
    }

    /**
     * Retrieve the SAFCredentialToken associated with the given SAFCredential.
     * Try to recreate it if it doesn't exist.
     *
     * @param The SAFCredential.
     *
     * @return The SAFCredentialToken associated with the SAFCredential. If SAFCredential is null,
     *         this method returns null.
     *
     * @throws SAFException if the SAFCredentialToken doesn't exist and it couldn't
     *                          be created.
     */
    protected SAFCredentialToken getSAFCredentialToken(SAFCredential safCred) throws SAFException {

        // If caller supplies null, we return null.
        if (safCred == null) {
            return null;
        }

        // Check if the token already exists.
        SAFCredentialToken safCredToken = safCredTokenMap.get(safCred);
        if (safCredToken != null) {
            return safCredToken;
        }

        // The token has been deleted, probably due to a auth cache eviction.
        // Recreate it if we can.

        SAFCredential.Type type = safCred.getType();

        switch (type) {
            case BASIC:
                if (safCred.isAuthenticated()) {
                    // Only honor basic auth credentials that have been authenticated
                    safCredToken = createAssertedCredentialToken(safCred, 0);
                } else {
                    // TODO: need to translate these exception messages eventually.
                    throw new SAFException("Cannot recreate native SAF credential for unauthenticated BASIC credential for user " + safCred.getUserId());
                }
                break;

            case ASSERTED:
            case DEFAULT:
                safCredToken = createAssertedCredentialToken(safCred, 0);
                break;

            case CERTIFICATE:
                safCredToken = createCertificateCredentialToken(safCred.getCertificate(), safCred.getAuditString(), new byte[8]);
                break;

            case MAPPED:
                safCredToken = createMappedCredentialToken(safCred.getDistributedUserId(),
                                                           safCred.getRealm(),
                                                           safCred.getAuditString(), new byte[8]);
                break;

            case SERVER:
                return null; // SERVER credential does not require native RACO (at the moment).

            default:
                throw new SAFException("Unrecognized SAFCredential Type: " + type);
        }

        // safCredToken is either non-null, or we've returned null or thrown an exception

        // Mark the token 'subject populated'. We're not on an authentication path
        // (createPasswordCredential/createCertificateCredential), so we don't need
        // to wait for setCredential to be called on it.
        safCredToken.setSubjectPopulated(true);

        // Insert new token into map.
        safCredTokenMap.put(safCred, safCredToken);

        // If we just populated the defaultCred, check its RESTRICTED bit and log
        // an informational message to the user.
        if (safCred == defaultCred) {
            checkDefaultCredentialIsRestricted();
        }

        return safCredToken;
    }

    /**
     * Retrieve the SAFCredentialToken key from the Subject. The SAFCredentialToken key was
     * embedded in the String we returned from a previous call to SAFAuthorizedRegistry.checkPassword/mapCertificate.
     * That String was tucked into the Subject's private credentials hashtable.
     *
     * @param subject
     *
     * @return The safCredTokenKey, or null if the Subject did not contain one.
     */
    protected String getSAFCredTokenKeyFromSubject(Subject subject) {
        Hashtable<String, ?> hash = new SubjectHelper().getHashtableFromSubject(subject, new String[] { AuthenticationConstants.UR_AUTHENTICATED_USERID_KEY });

        if (hash != null) {
            String safSecurityName = (String) hash.get(AuthenticationConstants.UR_AUTHENTICATED_USERID_KEY);
            if (safSecurityName != null) {
                return SAFSecurityName.parseKey(safSecurityName);
            }
        }
        return null;
    }

//    /**
//     * @param isSafRegistryConfigured
//     */
//    protected void setIsSafRegistryConfigured(boolean isSafRegistryConfigured) {
//        this.isSafRegistryConfigured = isSafRegistryConfigured;
//    }

    /**
     * Helper method or tests to override
     * in order to allow setCredential to run.
     *
     * @return true if a SAF userRegistry has been configured, false otherwise
     */
    protected boolean isUserRegistryConfigured() {
//        return isSafRegistryConfigured;
        return safUserRegistry != null;
    }

    /**
     * {@inheritDoc}
     *
     * If the Subject contains a SAFCredTokenKey, use it to look up the SAFCredential.
     * If then, then check the WSPrincipal name. If it is the unauthenticated user, use
     * the default SAFCredential. Otherwise, create an ASSERTED credential for the user
     */
    @Override
    public void setCredential(Subject subject) throws CredentialException {

        if (!mapDistributedIdentities && !isUserRegistryConfigured())
            return;

        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        if (principals.isEmpty()) {
            return;
        }
        if (principals.size() != 1) {
            throw new CredentialException("Too many WSPrincipals in the subject: " + subject);
        }

        SAFCredential safCred = null;

        String safCredTokenKey = getSAFCredTokenKeyFromSubject(subject);

        if (safCredTokenKey != null) {
            // Token key in the username, which was returned by a previous call to
            // SAFAuthorizedRegistry.checkPassword or mapCertificate. Use it to look
            // up the SAFCredential we created under checkPassword/mapCertificate.
            safCred = safCredTokenMap.getCredential(safCredTokenKey);

            // TODO: what if safCred doesn't exist?  This means the safCredTokenKey in the
            // WSPrincipal name isn't valid, which means the WSPrincipal was persisted and re-inflated
            // in a new address space.  Or the Subject was destroyed and the SAFCredential was deleted,
            // but the WSPrincipal somehow survived this and a new Subject was created with it.
            // So... are either of those scenarios possible?

        } else {
            // We have not previously authenticated the user in this address space.
            // So we're either dealing with the unauthenticated user, or a SSO/LTPA
            // token login.
            WSPrincipal wsprin = principals.iterator().next();
            try {
                if (wsprin.getName().equals(unauthenticatedUser)) {
                    // Get the defaultCred which represents the unauthenticated user.
                    safCred = getDefaultCredential();
                } else {
                    Hashtable<String, ?> customProperties = getUniqueIdHashtableFromSubject(subject);
                    String realm = null;
                    String accessId = wsprin.getAccessId();
                    String type = AccessIdUtil.getEntityType(accessId);
                    if (customProperties != null && customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM) != null) {
                        realm = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM);
                    } else {
                        realm = AccessIdUtil.getRealm(accessId);
                    }

                    // If Collective or ODR certificate acccessId, then do not create SAFCredential
                    if (("server".equalsIgnoreCase(type) && ("collective".equalsIgnoreCase(realm)) || ("odr".equalsIgnoreCase(realm) && "user".equalsIgnoreCase(type)))) {
                        return;
                    } else if (mapDistributedIdentities == true) {
                        safCred = createMappedCredential(AccessIdUtil.getUniqueId(wsprin.getAccessId(), realm), realm, null);
                    } else {
                        // SSO/LTPA assertion login.
                        safCred = createAssertedCredential(AccessIdUtil.getUniqueId(wsprin.getAccessId(), realm), null, 0);
                    }
                }
            } catch (SAFException se) {
                CredentialException ce = new CredentialException("could not create SAF credential for " + wsprin.getName());
                ce.initCause(se);
                throw ce;
            }
        }

        if (safCred != null) {

            subject.getPrivateCredentials().add(safCred);

            // Mark the flag in SAFCredentialToken that indicates the credential has been populated in
            // a Subject (and thus it is safe to delete the token from the map. If subsequently someone wants to
            // authz against this SAFCredential, a new native token will be automatically built for it).
            SAFCredentialToken safCredToken = safCredTokenMap.get(safCred);
            if (safCredToken != null) {
                safCredToken.setSubjectPopulated(true);
            }
        }
    }

    private Hashtable<String, ?> getUniqueIdHashtableFromSubject(final Subject subject) {
        final String[] properties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID };
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getHashtableFromSubject(subject, properties);
    }

    /** {@inheritDoc} */
    @FFDCIgnore(SAFException.class)
    @Override
    public boolean isSubjectValid(Subject subject) {
        boolean valid = true;
        SAFCredential safCredential = getSAFCredentialFromSubject(subject);
        if (safCredential != null) {
            try {
                byte[] safCredentialTokenBytes = getSAFCredentialTokenBytes(safCredential);
                if (safCredentialTokenBytes == null || safCredentialTokenBytes.length == 0) {
                    valid = false;
                }
            } catch (SAFException se) {
                valid = false;
            }
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProfilePrefix() {
        return profilePrefix;
    }

    /**
     * Normalize the given userSecurityName for the SAF service call.
     * Fold the id to UPPERCASE.
     *
     * @throws NullPointerException if userSecurityName is null.
     */
    protected String normalizeUserId(String userSecurityName) {
        assertNotNull(userSecurityName, "userSecurityName is null");
        return userSecurityName.toUpperCase();
    }

    /**
     * Normalize the given password for the SAF service call.
     * If mixed-case PW is disabled and PW.length() <=8, UPPERCASE it.
     * Otheriwse, return it as is.
     * Note: If >8chars it is not a password and should be checked as a passphrase.
     *
     * @throws NullPointerException if password is null.
     */
    @Sensitive
    protected String normalizePassword(@Sensitive String password) {
        assertNotNull(password, "password is null");
        return (!isMixedCasePWEnabled && password.length() <= 8) ? password.toUpperCase() : password;
    }

    /**
     * @return the first parm that's not null. If all are null, then null is returned.
     */
    protected <T> T firstNotNull(T... objs) {
        for (T obj : objs) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Simple utility method that compares the first parm to null, and if it is,
     * throws a NullPointerException using the second parm as the message.
     */
    @Trivial
    protected void assertNotNull(Object o, String NPEMsg) {
        if (o == null) {
            throw new NullPointerException(NPEMsg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void evicted(List<Object> victims) {

        for (Object victim : victims) {
            //destroy the SAFCredential when subject is evicted
            SAFCredential safcred = getSAFCredentialFromSubject(((CacheObject) victim).getSubject());

            try {
                if (safcred != null) {
                    deleteCredential(safcred);
                }
            } catch (SAFException se) {
                // Record an FFDC.
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] extractUToken(SAFCredential safCred) throws SAFException {

        byte[] utoken = null;

        // Warning: HERE BE HACKS!
        // In the case where the safCred is the serverCred, we don't actually have a native
        // SAFCredentialToken (ACEE) associated with the serverCred.  We never bothered to create
        // it because a null ACEE in the TCBSENV field means "run as server".
        //
        // ntv_extractUtoken will blow up if we pass it a null SAFCredentialToken, so we either
        // need to:
        // (a) go back and create an ACEE for the serverCred.
        // (b) hack our way around it by not calling ntv_extractUtoken, but pretending we
        //     did by newing up a GenericCredential anyway, with a null utoken.
        //
        // AFAICT, the utoken in the GenericCredential isn't used for anything. The J2C code
        // checks simply for the presence of the GenericCredential in the Subject, and if it's
        // there, assumes the utoken was extracted successfully.  The J2C code then proceeds
        // to sync the Subject to the thread when obtaining a connection.
        //
        // In the case of the server Subject, a null ACEE will be sync'ed to the thread,
        // and this is the identity the resource will use when creating the connection.  Since
        // null ACEE == server identity, the connection will be associated with the server's ID,
        // which is what we want.
        //
        // So to sum up, we're skiping the ntv_extractUtoken for the serverCred and just new'ing
        // up an empty GenericCredential, which the J2C code will honor and use to sync the server's
        // identity to the thread when obtaining the connection.

        if (safCred != serverCred) {
            SAFServiceResult safResult = new SAFServiceResult();
            utoken = ntv_extractUtoken(getSAFCredentialTokenBytes(safCred), safResult.getBytes());

            if (utoken == null) {
                // A null result means something failed.
                safResult.throwSAFException();
            }
        }

        return utoken;
    }

    /**
     * {@inheritDoc}
     *
     * TODO: For now, in Liberty, an empty or null Subject is equivalent to the Server
     * Subject, since we don't have an actual Server Subject just yet.
     */
    @Override
    public boolean isServerSubject(Subject subject) {
        return (subject == null) ||
               (subject.getPrincipals().isEmpty() &&
                subject.getPrivateCredentials().isEmpty() &&
                subject.getPublicCredentials().isEmpty());
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
     * {@inheritDoc}
     */
    public List<String> getGroupsForMappedUser(String userSecurityName) throws EntryNotFoundException, CustomRegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");

        List<String> groups = null;
        List<byte[]> egroups = null;
        try {
            egroups = ntv_getGroupsForMappedUser(NativeMethodUtils.convertToEBCDIC(userSecurityName), new ArrayList<byte[]>());
            groups = NativeMethodUtils.convertToASCII(egroups);

            if (groups.size() == 0 && !isValidMappedUser(userSecurityName))
                throw new EntryNotFoundException("User " + userSecurityName + " not valid");

        } catch (EntryNotFoundException enfe) {
            throw enfe;
        } catch (Throwable t) {
            throw new CustomRegistryException(t.toString());
        }

        return groups;
    }

    public boolean isValidMappedUser(String userSecurityName) throws CustomRegistryException {
        assertNotEmpty(userSecurityName, "userSecurityName is null");
        return ntv_isValidMappedUser(NativeMethodUtils.convertToEBCDIC(userSecurityName));
    }

    // ----------------------------------------------------------------------------------
    // Native methods (security_saf_credentials.c).
    // ----------------------------------------------------------------------------------

    /**
     * Native method to create a credential using user and password. The method
     * returns a native token that indirectly references a RACO. The native token
     * gets wrapped in Java by SAFCredentialToken.
     *
     * @param userSecurityName The user name, in EBCDIC bytes
     * @param password         The user's password, in EBCDIC bytes
     * @param auditString      String written to SAF and SMF auditing records, in EBCDIC bytes
     * @param applname         The APPLNAME (SAF PROFILE PREFIX), in EBCDIC bytes
     * @param safResult        Output parm that is populated with SAF service return/reason codes
     *
     * @return byte[] representing the native credential token. This byte[] gets wrapped in
     *         Java by a SAFCredentialToken. The native token indirectly references the RACO. If a
     *         RACO failed to be created, null is returned. In that event, the safResult output
     *         parm will contain the return and reason codes of the failed SAF service.
     */
    protected native byte[] ntv_createPasswordCredential(byte[] userSecurityName,
                                                         byte[] password,
                                                         byte[] auditString,
                                                         byte[] applname,
                                                         byte[] safResult);

    /**
     * Native method to create a credential for the given user. The method
     * returns a native token that indirectly references a RACO. The native token
     * gets wrapped in Java by SAFCredentialToken.
     *
     * @param userSecurityName The user name, in EBCDIC bytes
     * @param auditString      String written to SAF and SMF auditing records, in EBCDIC bytes
     * @param applname         The APPLNAME (SAF PROFILE PREFIX), in EBCDIC bytes
     * @param msgSuppress      Flag to suppress messages generated from INITACEE, 1 to suppress
     *                             and 0 not to suppress
     * @param safResult        Output parm that is populated with SAF service return/reason codes
     *
     * @return byte[] representing the native credential token. This byte[] gets wrapped in
     *         Java by a SAFCredentialToken. The native token indirectly references the RACO. If a
     *         RACO failed to be created, null is returned. In that event, the safResult output
     *         parm will contain the return and reason codes of the failed SAF service.
     */
    protected native byte[] ntv_createAssertedCredential(byte[] userSecurityName,
                                                         byte[] auditString,
                                                         byte[] applname,
                                                         int msgSuppress,
                                                         byte[] safResult);

    /**
     * Native method to create a credential for the given certificate. The method
     * returns a native token that indirectly references a RACO. The native token
     * gets wrapped in Java by SAFCredentialToken.
     *
     * @param encodedCert    The certificate for which to create the credential.
     * @param certLen        The length of the certificate (cannot use strlen in native due to potential nulls in certificate)
     * @param auditString    An optional audit string used for logging and SMF recording.
     * @param applName       An optional SAF profile prefix.
     * @param outputUsername Output parm that contains the user ID the certificate was mapped to.
     * @param safResult      Output parm that is populed with SAF service return/reason codes.
     *
     * @return byte[] representing the native credential token. This byte[] gets wrapped in
     *         Java by a SAFCredentialToken. The native token indirectly references the RACO. If a
     *         RACO failed to be created, null is returned. In that event, the safResult output
     *         parm will contain the return and reason codes of the failed SAF service.
     */
    protected native byte[] ntv_createCertificateCredential(byte[] encodedCert,
                                                            int certLen,
                                                            byte[] auditString,
                                                            byte[] applName,
                                                            byte[] outputUsername,
                                                            byte[] safResult);

    /**
     * Native method to create a credential for the given mapped identity. The method
     * returns a native token that indirectly references a RACO. The native token
     * gets wrapped in Java by SAFCredentialToken.
     *
     * @param userName       The User's distinguished name for which to create the credential.
     * @param registryName   Registry's name.
     * @param auditString    An optional audit string used for logging and SMF recording.
     * @param applName       An optional SAF profile prefix.
     * @param outputUsername Output parm that contains the user ID the certificate was mapped to.
     * @param safResult      Output parm that is populated with SAF service return/reason codes.
     *
     * @return byte[] representing the native credential token. This byte[] gets wrapped in
     *         Java by a SAFCredentialToken. The native token indirectly references the RACO. If a
     *         RACO failed to be created, null is returned. In that event, the safResult output
     *         parm will contain the return and reason codes of the failed SAF service.
     */
    protected native byte[] ntv_createMappedCredential(byte[] userName,
                                                       byte[] registryName,
                                                       byte[] auditString,
                                                       byte[] applName,
                                                       byte[] outputUsername,
                                                       byte[] safResult);

    /**
     * Native method to delete the RACO storage associated with the given
     * safCredentialToken.
     *
     * @param safCredentialTokenBytes The native token that references the RACO to be deleted.
     */
    protected native int ntv_deleteCredential(byte[] safCredentialTokenBytes);

    /**
     * Determine whether the SAF product supports mixed-case passwords.
     */
    protected native boolean ntv_isMixedCasePWEnabled();

    /**
     * Determine if the user associated with the given credential has the
     * RESTRICTED attribute set. This is determined by checking the aceeraui
     * bit in the ACEE.
     *
     * @param safCredTokenBytes The byte[] representing the native credential token.
     * @param safResultBytes    Output byte[] containing SAF service return/reason codes.
     *
     * @return 1 if the user is RESTRICTED;
     *         0 if not;
     *         some other value if an error occured.
     */
    protected native int ntv_isRESTRICTED(byte[] safCredTokenBytes, byte[] safResultBytes);

    /**
     * Flush out the penalty box cache.
     *
     * This method should be called whenever the profilePrefix is updated.
     */
    protected native void ntv_flushPenaltyBoxCache();

    /**
     * Extract the UTOKEN from the ACEE associated with the given SAFCredentialToken.
     *
     * @param safCredTokenBytes The byte[] representing the native credential token.
     * @param safResultBytes    Output byte[] containing SAF service return/reason codes.
     *
     * @return byte[] The UTOKEN, or null if an error occurred. Error information can
     *         be found in the safResultBytes.
     */
    protected native byte[] ntv_extractUtoken(byte[] safCredTokenBytes, byte[] safResultBytes);

    /**
     * Sets the suppressAuthFailureMessages boolean
     *
     * @param suppressAuthFailureMessages boolean
     */
    protected native void ntv_setPenaltyBoxDefaults(boolean suppressAuthFailureMessages);

    /**
     *
     * @param userName The mvsUserName of the mapped user whose groups we are retrieving
     * @param list     to store the resulting list of users
     * @return
     */
    protected native List<byte[]> ntv_getGroupsForMappedUser(byte[] userName, List<byte[]> list);

    /**
     * Used to validate whether an mvsUserName is valid
     *
     * @param userSecurityName The mvsUserName of the mapped user who we are validating
     * @return boolean that shows whether user is valid
     */
    protected native boolean ntv_isValidMappedUser(byte[] userSecurityName);

    /** {@inheritDoc} */
    @Override
    public String getIntrospectorDescription() {
        return "SAFCredentialsServiceImpl is an implementation of the SAFCredentialsServices, which creates and destroys and manages all aspects of native SAF credentials on z/OS.";
    }

    /** {@inheritDoc} */
    @Override
    public String getIntrospectorName() {
        return "SAFCredentialsServiceImpl";
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(PrintWriter pw) throws IOException {

        pw.println();
        if (safCredTokenMap.entrySet().isEmpty()) {
            pw.println("The active instance of the SAF Credential Token Map is empty.");
        } else {
            pw.println("SAF Credential Token Map Contents:");
            pw.println("  Size = " + safCredTokenMap.size());
            pw.println("  SAF Credential Tokens:");
            for (Entry<SAFCredential, SAFCredentialToken> entry : safCredTokenMap.entrySet()) {
                pw.println("    SAFCredential User Name = " + entry.getKey().getUserId());
                pw.println("    SAFCredentialToken Key = " + entry.getValue().getKey());
                pw.println("----");
            }
        }
        pw.println();
        pw.flush();
    }

}
