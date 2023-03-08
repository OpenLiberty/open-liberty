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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.kernel.security.thread.J2CIdentityService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.kernel.security.thread.ThreadIdentityService;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * Manages the native z/OS security environment associated with the thread.
 *
 */
@Trivial
public class ThreadIdentityServiceImpl implements ThreadIdentityService, J2CIdentityService {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(ThreadIdentityServiceImpl.class);

    /**
     * The NativeMethodManager service for loading native methods.
     */
    private NativeMethodManager nativeMethodManager = null;

    /**
     * Config attribute that controls whether or not sync-to-thread for app dispatch
     * is enabled.
     */
    protected static final String APP_ENABLED_KEY = "appEnabled";

    /**
     * Config attribute that controls whether or not sync-to-thread for J2C
     * is enabled.
     */
    protected static final String J2C_ENABLED_KEY = "j2cEnabled";

    /**
     * Reflects APP_ENABLED_KEY config value.
     */
    private boolean appEnabled = false;

    /**
     * Reflects J2C_ENABLED_KEY config value.
     */
    private boolean j2cEnabled = false;

    /**
     * Number of times to retry the native operation when it fails
     * because the SAF cred token is being freed
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * ThreadLocal holds the current identity assigned to each thread.
     */
    private ThreadLocal<SAFCredential> currentIdentity;

    /**
     * Reference to SAFCredentialsService, for retrieving SAFCredentialTokens from the
     * SAFCredential.
     */
    private SAFCredentialsService safCredentialsService = null;

    /**
     * Cached Boolean indicates whether the native identity services are available --
     * meaning that sync-to-thread is enabled and the server has authorization to do it.
     *
     * This cached object is cleared whenever the config is updated (see updateConfig).
     */
    private Boolean isNativeEnabled = null;

    /**
     * Helper class for creating J2C subjects.
     */
    private ThreadIdentityJ2CHelper j2cHelper = null;

    /**
     * A cache of app config objects (WebAppConfiguration) to Booleans indicating
     * whether or not syncToOSThread is enabled in the app config.
     */
    private final Map<WebAppConfiguration, Boolean> appConfigCache = new HashMap<WebAppConfiguration, Boolean>();

    /**
     * Inject the NativeMethodManager service.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        this.nativeMethodManager.registerNatives(ThreadIdentityServiceImpl.class);
    }

    /**
     * Remove the NativeMethodManager service.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * Set the SAFCredentialsService ref.
     */
    protected void setSafCredentialsService(SAFCredentialsService safCredentialsService) {
        this.safCredentialsService = safCredentialsService;
        this.j2cHelper = new ThreadIdentityJ2CHelper(safCredentialsService);

        final SAFCredential serverCred = this.safCredentialsService.getServerCredential();
        currentIdentity = new ThreadLocal<SAFCredential>() {
            @Override
            protected synchronized SAFCredential initialValue() {
                return serverCred;
            }
        };

    }

    /**
     * Unset the SAFCredentialsService ref.
     */
    protected void unsetSafCredentialsService(SAFCredentialsService safCredentialsService) {
        if (this.safCredentialsService == safCredentialsService) {
            this.safCredentialsService = null;
        }
    }

    /**
     * Invoked by OSGi when service is activated.
     */
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        updateConfig(props);
    }

    /**
     * Invoked by OSGi when service is deactivated.
     */
    protected void deactivate(ComponentContext cc) {
    }

    /**
     * Invoked by OSGi when the <safAuthorization> configuration has changed.
     */
    protected void modify(Map<String, Object> props) {
        updateConfig(props);
    }

    /**
     * This method is called whenever the SAF authorization config is updated.
     */
    protected void updateConfig(Map<String, Object> props) {
        appEnabled = ((Boolean) props.get(APP_ENABLED_KEY)).booleanValue();
        j2cEnabled = ((Boolean) props.get(J2C_ENABLED_KEY)).booleanValue();
        isNativeEnabled = null; // Reset the cached value.
        ntv_resetIsNativeEnabledCache(); // Reset the cached value in native.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object set(Subject subject) {
        Object retMe = null;

        SAFCredential safCred = safCredentialsService.getSAFCredentialFromSubject(subject);

        if (safCred != null) {
            retMe = setThreadSecurityEnvironment(safCred);
        } else if (safCredentialsService.isServerSubject(subject)) {
            retMe = runAsServer();
        }

        return retMe;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(Object token) {
        setThreadSecurityEnvironment((SAFCredential) token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runAsServer() {
        return setThreadSecurityEnvironment(safCredentialsService.getServerCredential());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppThreadIdentityEnabled() {
        if (appEnabled) {
            Boolean tmp = isNativeEnabled; // tmp is used for thread safety.
            if (tmp == null) {
                boolean enabled = ntv_isSyncToThreadEnabled(NativeMethodUtils.convertToEBCDIC(safCredentialsService.getProfilePrefix()));
                tmp = isNativeEnabled = Boolean.valueOf(enabled);
            }
            return tmp.booleanValue() && isApplicationSyncToThreadConfiguredForCurrentComponent();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isJ2CThreadIdentityEnabled() {
        if (j2cEnabled) {
            Boolean tmp = isNativeEnabled; // tmp is used for thread safety.
            if (tmp == null) {
                boolean enabled = ntv_isSyncToThreadEnabled(NativeMethodUtils.convertToEBCDIC(safCredentialsService.getProfilePrefix()));
                tmp = isNativeEnabled = Boolean.valueOf(enabled);
            }
            return tmp.booleanValue();
        } else {
            return false;
        }
    }

    /**
     * Set the native thread identity using the given SAFCredential.
     *
     * @param safCred The SAFCredential that represents the identity to set on the thread.
     *
     * @return The SAFCredential representing the identity previously assigned to the thread.
     */
    SAFCredential setThreadSecurityEnvironment(SAFCredential safCred) {

        SAFCredential prev = getCurrent();

        // Don't bother if the given safCred is already on the thread.
        if (safCred != null && safCred != prev && !safCred.equals(prev)) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Sync'ing identity to thread. Previous/New:", prev, safCred);
                Tr.debug(tc, "Calling stack", stackTraceToString());
            }

            int retryCount = 0;
            int rc = 0;
            SAFServiceResult safResult;
            byte[] profilePrefix = NativeMethodUtils.convertToEBCDIC(safCredentialsService.getProfilePrefix());

            do {
                byte[] safCredToken = getSAFCredentialTokenBytes(safCred);

                safResult = new SAFServiceResult();

                rc = ntv_setThreadSecurityEnvironment(safCredToken,
                                                      profilePrefix,
                                                      safResult.getBytes());
            } while (rc != 0 && safResult.isRetryable() && retryCount++ < MAX_RETRY_COUNT && SAFServiceResult.yield());

            if (rc == 0) {
                // success.
                setCurrent(safCred);

            } else {
                safResult.setAuthorizationFields(safCred.getUserId(), "BBG.SYNC." + safCred.getUserId(), "SURROGAT", null, null, false);
                // analyze SAFServiceResult for details about the error.
                throw new RuntimeException(new SAFException(safResult));
            }
        } else if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Will not sync identity to thread. Attempted/Current", safCred, prev);
        }

        return prev;
    }

    /**
     * @return the result of safCredentialsService.getSAFCredentialTokenBytes(safCred)
     *
     * @throws IllegalArgumentException if the safcredtoken could not be obtained
     */
    private byte[] getSAFCredentialTokenBytes(SAFCredential safCred) {

        try {
            return safCredentialsService.getSAFCredentialTokenBytes(safCred);
        } catch (SAFException se) {
            throw new IllegalArgumentException(se);
        }
    }

    /**
     * @return the current identity on the thread.
     */
    private SAFCredential getCurrent() {
        SAFCredential retMe = currentIdentity.get();
        if (retMe == null) {
            // GetCurrent must never return null, as the return value is usually
            // passed back to callers (as the token to hand back on the reset), and
            // those callers will generally interpret null as meaning that sync-to-thread
            // is not enabled.
            throw new IllegalStateException("current thread identity is null");
        }
        return retMe;
    }

    /**
     * Save/remember the current identity on the thread.
     *
     * @param curr The current identity on the thread.
     */
    private void setCurrent(SAFCredential curr) {
        currentIdentity.set(curr);
    }

    /**
     * {@inheritDoc}
     *
     * TODO: is there a better way to obtain the app's config data?
     */
    public boolean isApplicationSyncToThreadConfiguredForCurrentComponent() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        if (cmd == null) {
            // No ComponentMetaData.  Could mean we're on an un-managed thread
            // kicked off by the app?  Or maybe we're under servlet context init?
            // Either way, there's no way to determine if sync-to-thread is enabled
            // for the app, so default to false.
            return false;
        }

        ModuleMetaData mmd = cmd.getModuleMetaData();
        if (mmd instanceof WebModuleMetaData) {
            WebModuleMetaData wmmd = (WebModuleMetaData) mmd;
            // TODO: SecurityMetaData has internal access.  Cannot be ref'ed here.
            // SecurityMetaData securityMetadata = (SecurityMetadata) wmmd.getSecurityMetaData();
            // return (securityMetadata != null && securityMetadata.isSyncToOSThreadRequested());

            // Cache the result.
            // TODO: verify that dynamic updates are seen.
            WebAppConfiguration wac = (WebAppConfiguration) wmmd.getConfiguration();

            Boolean retMe = appConfigCache.get(wac);

            if (retMe == null) {
                // Not cached yet.  Initialize to FALSE, then look for it in
                // the app's env-entrys.
                retMe = Boolean.FALSE;

                List<EnvEntry> envEntries = wac.getEnvEntries();

                // Copied from SecurityServletConfiguratorHelper.java.
                final String SYNC_TO_OS_THREAD_ENV_ENTRY_KEY = "com.ibm.websphere.security.SyncToOSThread";

                for (EnvEntry envEntry : envEntries) {
                    if (SYNC_TO_OS_THREAD_ENV_ENTRY_KEY.equals(envEntry.getName())) {

                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found SyncToOSThread setting in env-entry", envEntry.getName(), envEntry.getValue());
                        }

                        retMe = Boolean.valueOf(envEntry.getValue());

                        break;
                    }
                }

                appConfigCache.put(wac, retMe);
            }

            return retMe.booleanValue();
        } else {
            // SyncToOSThread currently not supported for EJBs.
            return false;
        }

    }

    /**
     * Stringify the current stack trace.
     *
     * @return Stringified stack trace.
     */
    protected String stackTraceToString() {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();

        StringBuffer sb = new StringBuffer();

        // Start at element #2 in the stack trace.
        // 0: Thread.getStackTraceImpl()
        // 1: Thread.getStackTrace()
        // 2: ThreadIdentityServiceImpl.stackTraceToString()
        // 3: ThreadIdentityServiceImpl.setThreadSecurityEnvironment()
        for (int i = 3; i < stes.length && i < 20; ++i) {
            sb.append(stes[i] + "\n");
        }

        return sb.toString();
    }

    /**
     * Create and return a J2C subject based on the invocation subject.
     *
     * @return Subject the J2C subject
     */
    @Override
    public Subject getJ2CInvocationSubject() {
        return j2cHelper.getJ2CInvocationSubject();
    }

    /**
     * Native method to set the native security environment (TCBSENV) with the
     * ACEE referenced by the given safCredToken.
     *
     * @param safCredToken  The SAFCredentialToken bytes that reference the RACO of the identity
     *                          that will be set on the thread.
     * @param profilePrefix The profile prefix.
     * @param safResult     Output parm contains return code/reason codes from the native services.
     *
     * @return 0 if all goes well; non-zero on error. Use safResult to get information about the error.
     */
    protected native int ntv_setThreadSecurityEnvironment(byte[] safCredToken, byte[] profilePrefixEbcdic, byte[] safResult);

    /**
     * Native method to determine if sync to thread is enabled.
     *
     * @param profilePrefix The profile prefix.
     *
     * @return 1 if sync to thread is enabled.
     */
    protected native boolean ntv_isSyncToThreadEnabled(byte[] profilePrefixEbcdic);

    /**
     * Native method to reset the flag indicating that we have checked
     * to see if sync to thread is enabled.
     *
     */
    protected native void ntv_resetIsNativeEnabledCache();

}
