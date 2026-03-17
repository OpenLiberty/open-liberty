/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.identitystore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import io.openliberty.security.jakartasec40.JakartaSec40Constants;
import jakarta.el.ELException;
import jakarta.el.PropertyNotFoundException;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.IdentityStore.ValidationType;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition.Credentials;

/*-
 * A wrapper class that offers convenience methods for retrieving configuration
 * from an {@link InMemoryIdentityStoreDefinition} instance.
 *
 * <p/>
 * The methods in this class will evaluate any EL expressions provided in the
 * {@link InMemoryIdentityStoreDefinition} first and if no EL expressions are provided,
 * return the literal value instead.
 *
 * As a reminder, here is an example of an in memory identity store annotation:
 * @InMemoryIdentityStoreDefinition(
 *     priority = 10,
 *     priorityExpression = "#{80/20}",
 *     useFor = {VALIDATE, PROVIDE_GROUPS},
 *     useForExpression = "#{'VALIDATE'}",
 *     value = {
 *         @Credentials(callerName = "bill", password = "secret1", groups = { "foo", "bar" }),
 *         @Credentials(callerName = "sally", password = "secret1", groups = { "user" }),
 *         @Credentials(callerName = "dave", password = "secret1", groups = { "caller", "user", "foo", "bar" })
 *     }
 * )
 */

public class InMemoryIdentityStoreDefinitionWrapper {

    private static final TraceComponent tc = Tr.register(InMemoryIdentityStoreDefinitionWrapper.class);

    /** The definitions for this IdentityStore. */
    private final InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition;

    /**
     * NOTE: For priority and useFor, we do not need to store priorityExpression, nor useForExpression as they
     * effect the end values of priority and useFor respectively, but are not needed retrospectively.
     */

    /** The priority for this IdentityStore. Will be null when set by a deferred EL expression. */
    private final Integer priority;

    /** The ValidationTypes this IdentityStore can be used for. Will be null when set by a deferred EL expression. */
    private final Set<ValidationType> useFor;

    /** the annotations credentials in a key/value map: callerName = {password, groups[]} */
    private final Map<String, CredentialValue> credentials;

    private final ELHelperWrapper elHelper;

    /**
     * Create a new instance of an {@link InMemoryIdentityStoreDefinitionWrapper} that will provide
     * convenience methods to access configuration from the {@link InMemoryIdentityStoreDefinition}
     * instance.
     *
     * @param inMemoryIdentityStoreDefinition The {@link InMemoryIdentityStoreDefinition} to wrap.
     */
    public InMemoryIdentityStoreDefinitionWrapper(InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition) {

        if (inMemoryIdentityStoreDefinition == null) {
            throw new IllegalArgumentException("The InMemoryIdentityStoreDefinition cannot be null.");
        }
        this.inMemoryIdentityStoreDefinition = inMemoryIdentityStoreDefinition;
        elHelper = new ELHelperWrapper();

        Tr.warning(tc, "JAKARTASEC_WARNING_PRODUCTION_USE");

        /*
         * Evaluate the configuration. The values will be non-null if the setting is NOT
         * a deferred EL expression. If it is a deferred EL expression, we will dynamically
         * evaluate it at call time.
         */
        priority = evaluatePriority(false);
        useFor = evaluateUseFor(false);

        // with the credentials portion of the application annotation from the application, parse out the values
        int credentialsLength = (inMemoryIdentityStoreDefinition.value() == null) ? 0 : inMemoryIdentityStoreDefinition.value().length;
        if (credentialsLength == 0) {
            credentials = Collections.emptyMap();
        } else {
            Credentials[] creds = Arrays.copyOf(inMemoryIdentityStoreDefinition.value(), credentialsLength);

            // convert annotation credentials into an easily search-able HashMap with standard Java types
            Map<String, CredentialValue> credsMap = new HashMap<>(credentialsLength);
            for (int i = 0; i < credentialsLength; i++) {
                CredentialValue credential = getCredential(creds[i].callerName(), creds[i].password(), creds[i].groups());
                credsMap.put(creds[i].callerName(), credential);
            }
            credentials = Collections.unmodifiableMap(credsMap);
        }
    }

    private CredentialValue getCredential(String callerName, @Sensitive String password, String[] groups) {
        String evaluatedPassword = evaluatePassword(password, false);
        return new CredentialValue(callerName, (evaluatedPassword == null) ? "" : evaluatedPassword, groups);
    }

    /**
     * Evaluate and return the password.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *                          immediate EL expression or not set by an EL expression. If false, return the
     *                          value regardless of where it is evaluated.
     * @return The password or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluatePassword(@Sensitive String password, boolean immediateOnly) {
        String evaluatedPassword;

        // evaluate possible EL expressions
        try {
            evaluatedPassword = elHelper.processString("password", password, immediateOnly, true);
        } catch (IllegalArgumentException e) {
            /*
             * If deferred expression and called during initialization, return null so the expression can be re-evaluated
             * again later.
             */
            if (immediateOnly && ELHelperWrapper.isDeferredExpression(password)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "evaluatePassword", "Returning null since password is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "password", "" });
            evaluatedPassword = ""; /* Default value from spec. */
        }

        return evaluatedPassword;
    }

    /**
     * Evaluate and return the priority.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *                          immediate EL expression or not set by an EL expression. If false, return the
     *                          value regardless of where it is evaluated.
     * @return The priority or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore({ IllegalArgumentException.class, PropertyNotFoundException.class, ELException.class })
    private Integer evaluatePriority(boolean immediateOnly) {
        String priorityExpression = inMemoryIdentityStoreDefinition.priorityExpression();
        int priority = inMemoryIdentityStoreDefinition.priority();
        try {
            return elHelper.processInt("priorityExpression", priorityExpression, priority, immediateOnly);
        } catch (PropertyNotFoundException e) {
            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "priority/priorityExpression", JakartaSec40Constants.SPEC_DEFAULT_PRIORITY });
            return JakartaSec40Constants.SPEC_DEFAULT_PRIORITY;
        } catch (ELException e) {
            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "priority/priorityExpression", JakartaSec40Constants.SPEC_DEFAULT_PRIORITY });
            return JakartaSec40Constants.SPEC_DEFAULT_PRIORITY;
        } catch (IllegalArgumentException e) {
            /*
             * If deferred expression and called during initialization, return null so the expression can be re-evaluated
             * again later.
             */
            if (immediateOnly && ELHelperWrapper.isDeferredExpression(priorityExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "evaluatePriority", "Returning null since priorityExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "priority/priorityExpression", JakartaSec40Constants.SPEC_DEFAULT_PRIORITY });
            return JakartaSec40Constants.SPEC_DEFAULT_PRIORITY;
        }
    }

    /**
     * Evaluate and return the useFor.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *                          immediate EL expression or not set by an EL expression. If false, return the
     *                          value regardless of where it is evaluated.
     * @return The useFor or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private Set<ValidationType> evaluateUseFor(boolean immediateOnly) {

        String useForExpression = inMemoryIdentityStoreDefinition.useForExpression();
        ValidationType[] useFor = inMemoryIdentityStoreDefinition.useFor();
        try {
            return elHelper.processUseFor(useForExpression, useFor, immediateOnly);
        } catch (PropertyNotFoundException e) {
            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "useFor/useForExpression", JakartaSec40Constants.SPEC_DEFAULT_USEFOR });
            return Set.of(JakartaSec40Constants.SPEC_DEFAULT_USEFOR);
        } catch (IllegalArgumentException e) {
            /*
             * If deferred expression and called during initialization, return null so the expression can be re-evaluated
             * again later.
             */
            if (immediateOnly && ELHelperWrapper.isDeferredExpression(useForExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "evaluateUseFor", "Returning null since useForExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            Tr.warning(tc, "JAKARTASEC_WARNING_IDSTORE_CONFIG", new Object[] { "useFor/useForExpression", JakartaSec40Constants.SPEC_DEFAULT_USEFOR });

            return Set.of(JakartaSec40Constants.SPEC_DEFAULT_USEFOR);
        }
    }

    /**
     * Get the priority for the {@link IdentityStore}.
     *
     * @return The priority.
     *
     * @see InMemoryIdentityStoreDefinition#priority()
     * @see InMemoryIdentityStoreDefinition#priorityExpression()
     */
    int getPriority() {
        if (this.priority != null) {
            return this.priority;
        }
        Integer evalPriority = null;
        return (evalPriority = evaluatePriority(false)) != null ? evalPriority : JakartaSec40Constants.SPEC_DEFAULT_PRIORITY;
    }

    /**
     * Get the useFor for the {@link IdentityStore}.
     *
     * @return The useFor.
     *
     * @see InMemoryIdentityStoreDefinition#useFor()
     * @see InMemoryIdentityStoreDefinition#useForExpression()
     */
    Set<ValidationType> getUseFor() {
        return (this.useFor != null) ? this.useFor : evaluateUseFor(false);
    }

    /**
     * Get a specific credential from the credentials map given a caller name.
     *
     * @param callerName is the caller name key to the map.
     * @return the CredentialValue, or null if the caller name is invalid.
     */
    public CredentialValue getCredential(String callerName) {
        return credentials.get(callerName);
    }

    /***********************************************************************
     * Helper classes
     ***********************************************************************/

    private static class ELHelperWrapper extends ELHelper {

        /**
         * Required as protected in base class.
         */
        @Override
        public Set<ValidationType> processUseFor(String useForExpression, ValidationType[] useFor, boolean immediateOnly) {
            return super.processUseFor(useForExpression, useFor, immediateOnly);
        }

        /**
         * Evaluate possible string EL expressions
         */
        @Override
        public String processString(String name, @Sensitive String expression, boolean immediateOnly, boolean mask) throws IllegalArgumentException {
            return super.processString(name, expression, immediateOnly, mask);
        }
    }

    /**
     * This class models the Credentials interface, but is specific for use within
     * the identitystore package, modelling the data, and providing minimal APIs for
     * external use.
     */

    static final class CredentialValue extends CredentialPassword {

        private final String callerName;
        private final String[] groups;

        CredentialValue(final String callerName, final @Sensitive String password, final String[] groups) {
            super(password);
            this.callerName = callerName;
            this.groups = (groups == null) ? null : Arrays.copyOf(groups, groups.length);
        }

        String[] getGroups() {
            if ((groups == null) || (groups.length == 0)) {
                return new String[0];
            }
            return Arrays.copyOf(groups, groups.length);
        }

        @Override
        public String toString() {
            return "CredentialValue [callerName=" + callerName + ", password=" + "*****" + ", groups=" + Arrays.toString(groups) + "]";
        }

    }

    /**
     * This class models just the credential password, but the internal details are
     * hidden from any client as passwords are stored as ProtectedString where possible,
     * and password validation against a client value is handled internally.
     */

    private static class CredentialPassword {

        private final ProtectedString password;
        private final String hashAlgorithm; // works as isHashed boolean

        protected CredentialPassword(final @Sensitive String password) throws IllegalArgumentException {

            if ((password == null) || (password.length() == 0)) {
                this.password = new ProtectedString(new char[0]);
                this.hashAlgorithm = null;
                return;
            }

            // store the hash algorithm if credential password is hashed
            this.hashAlgorithm = PasswordUtil.isHashed(password) ? PasswordUtil.getCryptoAlgorithm(password) : null;
            this.password = new ProtectedString(password.toCharArray());
        }

        /**
         * Validate a user supplied password against this credential.
         *
         * All combinations of this credential password and the user password
         * are considered: credential password (AES/XOR/HASH/Plain) =
         * user password (AES/XOR/HASH/Plain)
         *
         * @param userPassword is the one that the user has specified.
         * @return boolean true if passwords match, false else.
         */
        protected boolean validate(@Sensitive ProtectedString userPassword) {

            if ((userPassword == null) || (userPassword.isEmpty() == true)) {
                return false;
            }

            final String passwordStr = new String(this.password.getChars());
            final String userPasswordStr = new String(userPassword.getChars());

            // check if userPassword is hashed
            final boolean isUserPasswordHashed = PasswordUtil.isHashed(userPasswordStr);

            // case 1: both passwords are hashed - direct comparison
            if (this.hashAlgorithm != null && isUserPasswordHashed) {
                return passwordStr.equals(userPasswordStr);
            }

            // case 2: either password or user password is hashed, so hash the other
            if (this.hashAlgorithm != null || isUserPasswordHashed) {
                return compareHashedPasswords(passwordStr, userPasswordStr);
            }

            // case 3: neither password is hashed - decode and compare
            return comparePlainPasswords(passwordStr, userPasswordStr);
        }

        /**
         * Compare passwords when exactly one is hashed.
         * Hashes the non-hashed password to match the hashed one.
         */
        private boolean compareHashedPasswords(@Sensitive String passwordStr, @Sensitive String userPasswordStr) {

            String hashedPassword;
            String passwordToHash;
            String targetHashAlgorithm;

            if (this.hashAlgorithm != null) {
                // credential password is hashed - hash the user password to compare
                hashedPassword = passwordStr;
                passwordToHash = userPasswordStr;
                targetHashAlgorithm = this.hashAlgorithm;
            } else {
                // user password is hashed - decode and hash credential password to compare
                hashedPassword = userPasswordStr;
                passwordToHash = PasswordUtil.passwordDecode(passwordStr);
                if (passwordToHash == null) {
                    passwordToHash = "";
                }
                targetHashAlgorithm = PasswordUtil.getCryptoAlgorithm(userPasswordStr);
            }

            // now hash the non-hashed password and compare with the hashed one
            HashMap<String, String> props = new HashMap<String, String>();
            props.put(PasswordUtil.PROPERTY_HASH_ENCODED, hashedPassword);

            try {
                String encodedPassword = PasswordUtil.encode(passwordToHash, targetHashAlgorithm, props);
                return hashedPassword.equals(encodedPassword);
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "compareHashedPasswords", "password encoding failure : " + e.getMessage());
                }
                throw new IllegalArgumentException("password encoding failure : " + e.getMessage());
            }
        }

        /**
         * Compare passwords when neither is hashed.
         * Decodes both passwords (handles XOR/AES encoding or plain) and compares.
         */
        private boolean comparePlainPasswords(@Sensitive String passwordStr, @Sensitive String userPasswordStr) {

            // decode both passwords (handles plain text, XOR, AES, and badly encoded passwords)
            final String decodedPassword = PasswordUtil.passwordDecode(passwordStr);
            final String decodedUserPassword = PasswordUtil.passwordDecode(userPasswordStr);

            if ((decodedPassword != null) && (decodedUserPassword != null)) {
                return decodedPassword.equals(decodedUserPassword);
            }

            // if here, then one of the passwords was encoded improperly
            return false;
        }
    }
}
