/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.util.Collection;

import javax.security.auth.login.LoginException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class SingleSignonTokenImpl extends AbstractTokenImpl implements Serializable, SingleSignonToken {

    private static final TraceComponent tc = Tr.register(SingleSignonTokenImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = -7144674627439090488L;

    private Token token = null;
    private final short version = 2;
    private transient TokenService tokenService;
    private String tokenType = null; // Required for deserialization in distributed environments
    private transient boolean wasDeserialized = false;

    public SingleSignonTokenImpl(TokenService tokenService, String tokenType) {
        this.tokenService = tokenService;
        this.tokenType = tokenType;
    }

    public void initializeToken(byte[] ssoToken) throws LoginException {
        initializeToken(ssoToken, false);
    }

    public void initializeToken(byte[] ssoToken, boolean refreshIfExpired) throws LoginException {
        try {
            token = null;

            try {
                token = getTokenService().recreateTokenFromBytes(ssoToken);
            } catch (TokenExpiredException e) {
                // refresh if needed
            }

            final Token tokenPriv = token;
            AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    setToken(tokenPriv);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }

    public void initializeToken(final Token token) {
        this.token = token;

        AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {
            @Override
            public Void run() {
                setToken(token);
                return null;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AttributeNameConstants.WSSSOTOKEN_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public short getVersion() {
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        SingleSignonToken newToken = new SingleSignonTokenImpl(getTokenService(), tokenType);
        token = AccessController.doPrivileged(new java.security.PrivilegedAction<Token>() {
            @Override
            public Token run() {
                return getToken();
            }
        });
        ((SingleSignonTokenImpl) newToken).initializeToken((Token) token.clone());
        return newToken;
    }

    /**
     * Get the TokenService that can be used to initialize this token. We will either grab the service sent in on the
     * constructor or in the case that the instance was created via deserialization, we will get it from OSGi.
     *
     * @return The TokenService.
     */
    private TokenService getTokenService() {
        if (!wasDeserialized || tokenService != null) {
            return tokenService;
        }

        /*
         * We were deserialized, so we need to look up our TokenService.
         */
        Bundle b = FrameworkUtil.getBundle(SingleSignonTokenImpl.class);
        BundleContext bc = b.getBundleContext();
        if (bc == null) {
            Tr.error(tc, "BUNDLE_CONTEXT_MISSING", b.getSymbolicName());
            String formattedMessage = TraceNLS.getFormattedMessage(
                                                                   this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "BUNDLE_CONTEXT_MISSING",
                                                                   new Object[] { b.getSymbolicName() },
                                                                   "CWWKS4004E: Could not retrieve the BundleContext for the {0} bundle. The bundle may still be loading. Try again later.");
            throw new IllegalStateException(formattedMessage);
        }

        String filter = "(" + TokenManagerImpl.KEY_TOKEN_TYPE + "=" + tokenType + ")";
        try {
            Collection<ServiceReference<TokenService>> serviceRefs = bc.getServiceReferences(TokenService.class, filter);

            if (serviceRefs.isEmpty()) {
                Tr.error(tc, "TOKEN_SERVICE_CONFIG_ERROR_NO_SUCH_SERVICE_TYPE", tokenType);
                String formattedMessage = TraceNLS.getFormattedMessage(
                                                                       this.getClass(),
                                                                       TraceConstants.MESSAGE_BUNDLE,
                                                                       "TOKEN_SERVICE_CONFIG_ERROR_NO_SUCH_SERVICE_TYPE",
                                                                       new Object[] { tokenType },
                                                                       "CWWKS4000E: A configuration error has occurred. The requested TokenService instance of type {0} could not be found.");
                throw new IllegalArgumentException(formattedMessage);
            }
            ServiceReference<TokenService> serviceRef = serviceRefs.stream().reduce((first, second) -> first).orElse(null);

            tokenService = bc.getService(serviceRef);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "In readObject, set the TokenService for " + this + " to " + tokenService);
            }
            return tokenService;
        } catch (InvalidSyntaxException e) {
            Tr.error(tc, "OSGI_INVALID_FILTER", filter, e.getMessage());
            String formattedMessage = TraceNLS.getFormattedMessage(
                                                                   this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "OSGI_INVALID_FILTER",
                                                                   new Object[] { filter, e.getMessage() },
                                                                   "CWWKS4005E: There was a syntax error encountered while retrieving the TokenService using the {0} service filter: {1}");
            throw new IllegalArgumentException(formattedMessage, e);
        }
    }

    /**
     * Read the {@link SingleSignonTokenImpl} from the {@link ObjectInputStream}.
     *
     * The difference from the defaultReadObject is that this will set the transient field <code>tokenService</code> to correspond to the OSGi {@link TokenService} instance that
     * handles the token type stored in the <code>tokenType</code> field.
     *
     * @param in The {@link ObjectInputStream} to read from.
     * @throws IOException            If there was an error reading from the ObjectInputStream.
     * @throws ClassNotFoundException If one of the classes loaded from the ObjectInputStream cannot be found.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        wasDeserialized = true; // Mark this instance has been deserialized.
    }
}
