/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Liberty's LDAP {@link IdentityStore} implementation.
 */
@Default
@ApplicationScoped
public class LdapIdentityStore implements IdentityStore {

    /** The definitions for this IdentityStore. */
    private final LdapIdentityStoreDefinitionWrapper idStoreDefinition;

    /**
     * Construct a new {@link LdapIdentityStore} instance using the specified definitions.
     *
     * @param idStoreDefinition The definitions to use to configure the {@link IdentityStore}.
     */
    public LdapIdentityStore(LdapIdentityStoreDefinition idStoreDefinition) {
        this.idStoreDefinition = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);
    }

    /**
     * Bind to the LDAP server for administrative operations such as searches.
     *
     * @return The bound {@link DirContext}.
     * @throws NamingException If there was a failure to bind to the LDAP server.
     */
    @SuppressWarnings("unused")
    private DirContext bind() throws NamingException {
        return bind(this.idStoreDefinition.getBindDn(), this.idStoreDefinition.getBindDnPassword());
    }

    /**
     * Bind to the LDAP server.
     *
     * @param bindDn The distinguished name used to bind to the LDAP server.
     * @param bindPw The password used to bind to the LDAP server.
     * @return The bound {@link DirContext}.
     * @throws NamingException If there was a failure to bind to the LDAP server.
     */
    private DirContext bind(String bindDn, @Sensitive String bindPw) throws NamingException {

        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, this.idStoreDefinition.getUrl());

        /*
         * Add credentials.
         */
        if (bindDn != null && !bindDn.isEmpty()) {

            /*
             * Support encoded passwords.
             */
            String decodedBindPw = PasswordUtil.passwordDecode(bindPw.trim());
            if (decodedBindPw == null || decodedBindPw.isEmpty()) {
                throw new IllegalArgumentException("An empty password is invalid.");
            }

            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, new ProtectedString(decodedBindPw.toCharArray()));
        }

        /*
         * TODO How do we support SSL setting? End-point based configuration?
         */

        return new InitialLdapContext(env, null);
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        // TODO Add group lookup.
        throw new UnsupportedOperationException(getClass().getName() + " does not yet implement the 'getCallerGroups(CredentialValidationResult)' method.");
    }

    @Override
    public int priority() {
        return this.idStoreDefinition.getPriority();
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {

        /*
         * Only support UserPasswordCredential.
         */
        if (!(credential instanceof UsernamePasswordCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        // TODO Add validation.

        throw new UnsupportedOperationException(getClass().getName() + " does not yet implement the 'validate(Credential)' method.");
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return this.idStoreDefinition.getUseFor();
    }
}
