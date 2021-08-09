/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthConsent;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OauthConsentStore;

/**
 * Consent store that uses customer provided OAuthStore to manage consents.
 */
public class OauthConsentStoreImpl implements OauthConsentStore {

    private static TraceComponent tc = Tr.register(OauthConsentStoreImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final String componentId;
    private final OAuthStore oauthStore;
    private Timer timer;
    private long cleanupIntervalInMilliseconds = 0;

    /**
     * @param componentId TODO
     * @param oauthStore
     * @param cleanupIntervalInMilliseconds
     */
    public OauthConsentStoreImpl(String componentId, OAuthStore oauthStore, long cleanupIntervalInMilliseconds) {
        this.componentId = componentId;
        this.oauthStore = oauthStore;
        this.cleanupIntervalInMilliseconds = cleanupIntervalInMilliseconds;
    }

    @Override
    public void initialize() {
        scheduleCleanupTask();
    }

    private void scheduleCleanupTask() {
        if (cleanupIntervalInMilliseconds > 0) {
            CleanupTask cleanupTask = new CleanupTask();
            timer = new Timer(true);
            long period = cleanupIntervalInMilliseconds;
            long delay = period;
            timer.schedule(cleanupTask, delay, period);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addConsent(String clientId, String user, String scopeAsString, String resource, String providerId, int lifetimeInSeconds) {
        try {
            long expires = getExpires(lifetimeInSeconds);
            String consentProperties = getConsentProperties(resource);
            OAuthConsent oauthConsent = new OAuthConsent(clientId, user, scopeAsString, resource, providerId, expires, consentProperties);
            oauthStore.create(oauthConsent);
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_CREATE_CONSENT", e.getLocalizedMessage());
            }
        }
    }

    private long getExpires(int lifetimeInSeconds) {
        long expires = 0;
        if (lifetimeInSeconds > 0) {
            expires = new Date().getTime() + (1000L * lifetimeInSeconds);
        }
        return expires;
    }

    private String getConsentProperties(String resource) {
        JsonObject extendedFields = new JsonObject();
        if (resource != null) {
            extendedFields.addProperty(OAuth20Constants.RESOURCE, resource);
        } else {
            extendedFields.addProperty("", "");
        }

        return extendedFields.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean validateConsent(String clientId, String username, String providerId, String[] scopes, String resource) {
        boolean result = false;
        try {
            OAuthConsent oauthConsent = oauthStore.readConsent(providerId, username, clientId, resource);
            result = isValid(oauthConsent, scopes);
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_READ_CONSENT", e.getLocalizedMessage());
            }
        }
        return result;
    }

    private boolean isValid(OAuthConsent oauthConsent, String[] scopes) {
        boolean result = true;

        if (oauthConsent != null && new Date().getTime() < oauthConsent.getExpires()) {
            String scopeStr = oauthConsent.getScope();
            for (String scope : scopes) {
                if (!scopeStr.contains(scope)) {
                    result = false;
                    break;
                }
            }
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public void stopCleanupThread() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private class CleanupTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            try {
                oauthStore.deleteConsents(componentId, new Date().getTime());
            } catch (OAuthStoreException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                    Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_DELETE_CONSENTS", e.getLocalizedMessage());
                }
            }
        }

    }

}
