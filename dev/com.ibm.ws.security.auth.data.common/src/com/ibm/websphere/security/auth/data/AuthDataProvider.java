/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.data;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.auth.data.internal.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@Component(service = AuthDataProvider.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class AuthDataProvider {

    protected static final String CFG_KEY_ID = "id";
    protected static final String CFG_KEY_DISPLAY_ID = "config.displayId";
    protected static final String CFG_KEY_USER = "user";
    protected static final String CFG_KEY_PASSWORD = "password";
    protected static final String CFG_KEY_KRB5_PRINCIPAL = "krb5Principal";

    private static final TraceComponent tc = Tr.register(AuthDataProvider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String AUTH_DATA_REF_NAME = "authData";
    private static final ConcurrentServiceReferenceMap<String, AuthData> authDataMap = new ConcurrentServiceReferenceMap<String, AuthData>(AUTH_DATA_REF_NAME);
    private static final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private static final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private static final ReadLock readLock = reentrantReadWriteLock.readLock();
    private static final Pattern DEFAULT_NESTED_PATTERN = Pattern.compile(".*(\\[default-\\d*\\])$");
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("(default-\\d*)$");

    @Reference(name = AUTH_DATA_REF_NAME, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setAuthData(ServiceReference<AuthData> authDataRef) {
        writeLock.lock();
        try {
            authDataMap.putReference(getKey(authDataRef), authDataRef);
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetAuthData(ServiceReference<AuthData> authDataRef) {
        writeLock.lock();
        try {
            authDataMap.removeReference(getKey(authDataRef), authDataRef);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * If the id was not specified (starts with default-), then use the config.displayId instead.
     * The config admin assigns an id of the format default-n, where n is a number beginning at 0.
     * For cardinality 1, 0, or -1, the value of n is 0. For multiple cardinality, the values are
     * assigned sequentially starting with 0.
     *
     * @param authDataRef
     * @return the key to use for saving the auth data configuration.
     */
    private String getKey(ServiceReference<AuthData> authDataRef) {
        String key = (String) authDataRef.getProperty(CFG_KEY_ID);
        if (key == null || DEFAULT_PATTERN.matcher(key).matches() || DEFAULT_NESTED_PATTERN.matcher(key).matches()) {
            key = (String) authDataRef.getProperty(CFG_KEY_DISPLAY_ID);
        }
        return key;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        writeLock.lock();
        try {
            authDataMap.activate(cc);
        } finally {
            writeLock.unlock();
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        writeLock.lock();
        try {
            authDataMap.deactivate(cc);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Gets the auth data for the specified auth data alias.
     *
     * @param authDataAlias the auth data alias representing the auth data entry in the configuration.
     * @return the auth data.
     */
    public static AuthData getAuthData(String authDataAlias) throws LoginException {
        readLock.lock();
        try {
            AuthData authDataConfig = authDataMap.getService(authDataAlias);
            validateAuthDataConfig(authDataAlias, authDataConfig);
            return authDataConfig;
        } finally {
            readLock.unlock();
        }
    }

    private static void validateAuthDataConfig(String authDataAlias, AuthData authDataConfig) throws LoginException {
        validateAuthDataExists(authDataAlias, authDataConfig);

        String user = authDataConfig.getUserName();
        String krb5Principal = authDataConfig.getKrb5Principal();

        // The 'user' and 'krb5Principal' attributes are mutually exclusive
        if (user != null && krb5Principal != null) {
            Tr.error(tc, "AUTH_DATA_EXCLUSIVE_ATTRS", CFG_KEY_USER, CFG_KEY_KRB5_PRINCIPAL, authDataAlias);
            throw new LoginException(Tr.formatMessage(tc, "AUTH_DATA_EXCLUSIVE_ATTRS", CFG_KEY_USER, CFG_KEY_KRB5_PRINCIPAL, authDataAlias));
        }

        if (krb5Principal != null) {
            validateAuthDataAttribute(CFG_KEY_KRB5_PRINCIPAL, krb5Principal);
        } else {
            validateAuthDataAttribute(CFG_KEY_USER, user);
            validateAuthDataAttribute(CFG_KEY_PASSWORD, authDataConfig.getPassword());
        }
    }

    private static void validateAuthDataExists(String authDataAlias, AuthData authDataConfig) throws LoginException {
        if (authDataConfig == null) {
            Object[] traceObjects = new Object[] { authDataAlias };
            Tr.error(tc, "AUTH_DATA_CONFIG_ERROR_NO_SUCH_ALIAS", traceObjects);
            throw new LoginException(TraceNLS.getFormattedMessage(AuthDataProvider.class,
                                                                  TraceConstants.MESSAGE_BUNDLE,
                                                                  "AUTH_DATA_CONFIG_ERROR_NO_SUCH_ALIAS",
                                                                  traceObjects,
                                                                  "CWWKS1300E: A configuration exception has occurred. The requested authentication data alias {0} could not be found."));
        }
    }

    private static void validateAuthDataAttribute(String attribute, @Sensitive Object value) throws LoginException {
        String currentValue = null;
        if (value instanceof char[]) {
            currentValue = String.valueOf((char[]) value);
        } else {
            currentValue = (String) value;
        }

        if (currentValue == null || currentValue.trim().length() == 0) {
            Object[] traceObjects = new Object[] { attribute };
            Tr.error(tc, "AUTH_DATA_CONFIG_ERROR_INCOMPLETE", traceObjects);
            throw new LoginException(TraceNLS.getFormattedMessage(AuthDataProvider.class,
                                                                  TraceConstants.MESSAGE_BUNDLE,
                                                                  "AUTH_DATA_CONFIG_ERROR_INCOMPLETE",
                                                                  traceObjects,
                                                                  "CWWKS1301E: A configuration error has occurred. The attribute {0} must be defined."));
        }
    }

}
