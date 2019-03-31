package com.ibm.ws.rest.handler.validator.loginmodule;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.wsspi.security.auth.callback.WSManagedConnectionFactoryCallback;
import com.ibm.wsspi.security.auth.callback.WSMappingPropertiesCallback;

public class TestLoginModule implements LoginModule {

    public static final String DB_USERNAME = "dbuser";
    public static final String DB_PASSWORD = "dbpass";
    public static final String CUSTOM_PROPERTY_KEY = "testCustomLoginModuleProperties-fooProp";

    private static final String c = TestLoginModule.class.getSimpleName();

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, ?> options;

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        System.out.println(c + " >>> initialize");
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = options;
    }

    /** {@inheritDoc} */
    @Override
    public boolean login() throws LoginException {
        System.out.println(c + " >>> login");
        try {
            Callback[] callbacks = getHandledCallbacks();
            setPasswordCredentialInSubject(callbacks);
            Map<?, ?> properties = ((WSMappingPropertiesCallback) callbacks[1]).getProperties();
            System.out.println("   properties=" + properties);
            // Check for specific custom login module prop set by testCustomLoginModuleProperties
            String customProp = (String) properties.get(CUSTOM_PROPERTY_KEY);
            if (customProp != null)
                System.out.println("  TEST_CHECK " + CUSTOM_PROPERTY_KEY + "=" + customProp);
        } catch (Exception e) {
            throw (LoginException) new LoginException(e.getMessage()).initCause(e);
        }

        return true;
    }

    private Callback[] getHandledCallbacks() throws IOException, UnsupportedCallbackException {
        Callback callbacks[] = new Callback[2];
        callbacks[0] = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
        callbacks[1] = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
        callbackHandler.handle(callbacks);
        return callbacks;
    }

    private void setPasswordCredentialInSubject(Callback[] callbacks) {
        ManagedConnectionFactory managedConnectionFactory = ((WSManagedConnectionFactoryCallback) callbacks[0]).getManagedConnectionFacotry();
        final PasswordCredential passwordCredential = new PasswordCredential(DB_USERNAME, DB_PASSWORD.toCharArray());
        passwordCredential.setManagedConnectionFactory(managedConnectionFactory);
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                subject.getPrivateCredentials().add(passwordCredential);
                return null;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        System.out.println(c + " >>> abort");
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        System.out.println(c + " >>> commit");
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        System.out.println(c + " >>> logout");
        return true;
    }

}
