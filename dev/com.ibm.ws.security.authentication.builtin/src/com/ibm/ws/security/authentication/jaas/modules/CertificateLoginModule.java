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
package com.ibm.ws.security.authentication.jaas.modules;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 *
 */
public class CertificateLoginModule extends ServerCommonLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(CertificateLoginModule.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private String username = null;
    private String authenticatedId = null;
    private String securityName = null;
    private boolean collectiveCert = false;
    public static ThreadLocal<Boolean> collectiveCertificate = new ThreadLocal<Boolean>();

    /**
     * Gets the required Callback objects needed by this login module.
     *
     * @param callbackHandler
     * @return
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    @Override
    public Callback[] getRequiredCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new WSX509CertificateChainCallback(null);

        callbackHandler.handle(callbacks);
        return callbacks;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ RegistryException.class, CertificateMapFailedException.class, WSLoginFailedException.class, LoginException.class })
    public boolean login() throws LoginException {
        if (isAlreadyProcessed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Already processed by other login module, abstaining.");
            }
            return false;
        }

        X509Certificate certChain[] = null;
        try {
            Callback[] callbacks = getRequiredCallbacks(callbackHandler);
            certChain = ((WSX509CertificateChainCallback) callbacks[0]).getX509CertificateChain();

            // If we have insufficient data, abstain.
            if (certChain == null || certChain.length == 0) {
                return false;
            }

            setAlreadyProcessed();

            // Check if the certificate subject is a collective certificate chain.
            // If it is a collective certificate chain, handle the collective login
            // otherwise, do a user login.
            CollectiveAuthenticationPlugin plugin = getCollectiveAuthenticationPlugin();
            collectiveCert = plugin.isCollectiveCertificateChain(certChain);
            if (collectiveCert || plugin.isCollectiveCACertificate(certChain)) {
                handleCollectiveLogin(certChain, plugin, collectiveCert);
            } else {
                CertificateAuthenticator certAuthen = getCertificateAuthenticator(certChain);
                if (certAuthen != null) {
                    handleCertificateAuthenticator(certChain, certAuthen);
                } else {
                    handleUserLogin(certChain);
                }
            }

            updateSharedState();
            return true;

        } catch (CertificateMapFailedException e) {
            String dn = certChain[0].getSubjectX500Principal().getName();
            String msg = TraceNLS.getFormattedMessage(
                                                      this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "JAAS_AUTHENTICATION_FAILED_CERTNOMAP",
                                                      new Object[] { dn },
                                                      "CWWKS1101I: CLIENT-CERT Authentication failed for the client certificate with dn {0}. The dn did not map to a user in the registry.");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CLIENT-CERT Authentication failed for the client certificate with dn " + dn + ". The dn did not map to a user in the registry.");
            }
            throw new AuthenticationException(msg, e);
        } catch (RegistryException e) {
            String dn = certChain[0].getSubjectX500Principal().getName();
            String msg = TraceNLS.getFormattedMessage(
                                                      this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "JAAS_AUTHENTICATION_FAILED_CERTNOMAP",
                                                      new Object[] { dn },
                                                      "CWWKS1101I: CLIENT-CERT Authentication failed for the client certificate with dn {0}. The dn did not map to a user in the registry.");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CLIENT-CERT Authentication failed for the client certificate with dn " + dn + ". The dn did not map to a user in the registry.");
            }
            throw new AuthenticationException(msg, e);
        } catch (WSLoginFailedException e) {
            throw new AuthenticationException(e.getLocalizedMessage());
        } catch (LoginException e) {
            // Need to re-throw LoginException
            throw e;
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while processing CertificateLoginModule.login.", e);
            }
            // This is not normal: FFDC will be instrumented
            String dn;
            if (certChain != null)
                dn = certChain[0].getSubjectX500Principal().getName();
            else
                dn = "certChain=null";
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "JAAS_AUTHENTICATION_FAILED_CERT_INTERNAL_ERROR",
                                                      new Object[] { dn, e },
                                                      "CWWKS1102E: CLIENT-CERT Authentication failed for the client certificate with dn " + dn + ". An internal error occurred: "
                                                                              + e.getLocalizedMessage());
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "JAAS_AUTHENTICATION_FAILED_CERT_INTERNAL_ERROR", dn, e.getLocalizedMessage());
            }
            throw new AuthenticationException(msg, e);
        }
    }

    /**
     * Get the accessid components (type, realm, username) from the
     * CertificateAuthenticator and create the Principal and credentials
     *
     * @param certChain
     * @param bob
     */
    private void handleCertificateAuthenticator(X509Certificate[] certChain,
                                                CertificateAuthenticator certAuthen) throws Exception {
        String type = certAuthen.getType();
        if (type == null)
            type = AccessIdUtil.TYPE_USER;
        String realm = certAuthen.getRealm();
        if (realm == null)
            realm = "defaultRealm";
        authenticatedId = certAuthen.getUsername();
        username = authenticatedId;
        String accessId = AccessIdUtil.createAccessId(type, realm, authenticatedId);
        addCredentials(accessId);
    }

    /**
     * See if there is a CertificateAuthenticator service that will
     * vouch for this cert chain. Return the 1st authenticator that
     * return true from authenticateCertificateChain()
     *
     * @param certChain
     * @throws LoginException
     * @return
     */
    private CertificateAuthenticator getCertificateAuthenticator(X509Certificate[] certChain) throws LoginException {
        CertificateAuthenticator certAuthen = null;
        ConcurrentServiceReferenceMap<String, CertificateAuthenticator> certAuthens = JAASServiceImpl.getCertificateAuthenticators();
        Set<String> keys = certAuthens.keySet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CertificateAuthenticator keys:", keys);
        }
        // Iterate through the CertificateAuthenticators and call
        // authenticateCertificateChain. Return the 1st one to return true.
        for (String key : keys) {
            CertificateAuthenticator current = certAuthens.getService(key);
            if (current.authenticateCertificateChain(certChain)) {
                certAuthen = current;
                break;
            }
        }
        return certAuthen;
    }

    /**
     * Handles a collective certificate login.
     *
     * @param certChain
     * @param x509Subject
     * @throws InvalidNameException
     * @throws AuthenticationException
     * @throws Exception
     */
    private void handleCollectiveLogin(X509Certificate certChain[], CollectiveAuthenticationPlugin plugin,
                                       boolean collectiveCert) throws InvalidNameException, AuthenticationException, Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "inbound-collectiveCertificate=" + CertificateLoginModule.collectiveCertificate.get());
        // If the chain is not authenticated, it will throw an AuthenticationException
        plugin.authenticateCertificateChain(certChain, collectiveCert);
        X509Certificate cert = certChain[0];
        X500Principal x509Subject = cert.getSubjectX500Principal();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_SERVER,
                                                      "collective",
                                                      x509Subject.getName());
        username = x509Subject.getName();
        authenticatedId = x509Subject.getName();
        addCredentials(accessId);
        collectiveCertificate.set(true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "collectiveCertificate=" + CertificateLoginModule.collectiveCertificate.get());
    }

    /**
     * Add unique ID and call setPrincipalAndCredentials
     *
     * @param accessId
     * @throws Exception
     */
    private void addCredentials(String accessId) throws Exception {
        temporarySubject = new Subject();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, AccessIdUtil.getUniqueId(accessId));
        temporarySubject.getPublicCredentials().add(hashtable);
        setWSPrincipal(temporarySubject, username, accessId, WSPrincipal.AUTH_METHOD_CERTIFICATE);
        setCredentials(temporarySubject, username, username);
        temporarySubject.getPublicCredentials().remove(hashtable);
    }

    /**
     * Handles a non-collective certificate login.
     *
     * Note:
     * In distributed env, both username and securityName in this method are the same.
     * In zOS env, username has platform cred appended while securityName does not.
     *
     * username : TESTUSER::c2c2c70001013....00
     * securityName: TESTUSER
     *
     * @param certChain
     * @throws RegistryException
     * @throws CertificateMapNotSupportedException
     * @throws CertificateMapFailedException
     * @throws EntryNotFoundException
     * @throws Exception
     */
    private void handleUserLogin(X509Certificate certChain[]) throws RegistryException, CertificateMapNotSupportedException, CertificateMapFailedException, EntryNotFoundException, Exception {
        UserRegistry userRegistry = getUserRegistry();
        username = userRegistry.mapCertificate(certChain);
        authenticatedId = userRegistry.getUniqueUserId(username);
        securityName = userRegistry.getUserSecurityName(authenticatedId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "username=[" + username +
                         "] authenticatedId=[" + authenticatedId +
                         "] securityName=[" + securityName + "]");
        }
        setUpTemporarySubject();
    }

    /**
     * Populate a temporary subject in response to a successful authentication.
     * We use a temporary Subject because if something goes wrong in this flow,
     * we are not updating the "live" Subject. If performance is a problem, it may
     * be necessary to create a placeholder instead of a subject and modify the credentials
     * service to return a set of credentials or update the holder in order to place in
     * the shared state.
     *
     * @throws Exception
     */
    private void setUpTemporarySubject() throws Exception {
        temporarySubject = new Subject();
        UserRegistry ur = getUserRegistry();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      ur.getRealm(),
                                                      ur.getUniqueUserId(username));
        setWSPrincipal(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_CERTIFICATE);
        setCredentials(temporarySubject, securityName, username);
        setOtherPrincipals(temporarySubject, securityName, accessId, WSPrincipal.AUTH_METHOD_CERTIFICATE, null);
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (authenticatedId == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Authentication did not occur for this login module, abstaining.");
            return false;
        }
        setUpSubject();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() {
        cleanUpSubject();
        authenticatedId = null;
        username = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() {
        cleanUpSubject();
        authenticatedId = null;
        username = null;
        return true;
    }
}
