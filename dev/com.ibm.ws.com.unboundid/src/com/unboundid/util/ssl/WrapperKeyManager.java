/*
 * Copyright 2008-2025 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright 2008-2025 Ping Identity Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2008-2025 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedHashSet;

import com.unboundid.util.NotExtensible;
import com.unboundid.util.NotNull;
import com.unboundid.util.Nullable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.SSLEngine;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides an SSL key manager that may be used to wrap a provided
 * set of key managers.  It provides the ability to select the desired
 * certificate based on a given nickname.
 */
@NotExtensible()
@ThreadSafety(level=ThreadSafetyLevel.INTERFACE_THREADSAFE)
public abstract class WrapperKeyManager
        extends X509ExtendedKeyManager
{
//    private String chosenClientCertificateAlias;
    private String chosenServerCertificateAlias;

    private String algorithm = KeyManagerFactory.getDefaultAlgorithm();
    private boolean isPKIX = "PKIX".equalsIgnoreCase(algorithm);

    // The nickname of the certificate that should be selected.
    @Nullable private final String certificateAlias;

    // The set of key managers that will be used to perform the processing.
    @NotNull private final X509KeyManager[] keyManagers;



    /**
     * Creates a new instance of this wrapper key manager with the provided
     * information.
     *
     * @param  keyManagers       The set of key managers to be wrapped.  It must
     *                           not be {@code null} or empty, and it must contain
     *                           only X509KeyManager instances.
     * @param  certificateAlias  The nickname of the certificate that should be
     *                           selected.  It may be {@code null} if any
     *                           acceptable certificate found may be used.
     */
    protected WrapperKeyManager(@NotNull final KeyManager[] keyManagers,
                                @Nullable final String certificateAlias)
    {
        this.certificateAlias = certificateAlias;

        this.keyManagers = new X509KeyManager[keyManagers.length];
        for (int i=0; i < keyManagers.length; i++)
        {
            this.keyManagers[i] = (X509KeyManager) keyManagers[i];
        }
    }



    /**
     * Creates a new instance of this wrapper key manager with the provided
     * information.
     *
     * @param  keyManagers       The set of key managers to be wrapped.  It must
     *                           not be {@code null} or empty.
     * @param  certificateAlias  The nickname of the certificate that should be
     *                           selected.  It may be {@code null} if any
     *                           acceptable certificate found may be used.
     */
    protected WrapperKeyManager(@NotNull final X509KeyManager[] keyManagers,
                                @Nullable final String certificateAlias)
    {
        this.keyManagers      = keyManagers;
        this.certificateAlias = certificateAlias;
    }



    /**
     * Retrieves the nickname of the certificate that should be selected.
     *
     * @return  The nickname of the certificate that should be selected, or
     *          {@code null} if any acceptable certificate found in the key store
     *          may be used.
     */
    @Nullable()
    public String getCertificateAlias()
    {
        return certificateAlias;
    }



    /**
     * Retrieves the nicknames of the client certificates of the specified type
     * contained in the key store.
     *
     * @param  keyType  The key algorithm name for which to retrieve the available
     *                  certificate nicknames.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     *
     * @return  The nicknames of the client certificates, or {@code null} if none
     *          were found in the key store.
     */
    @Override()
    @Nullable()
    public final synchronized String[] getClientAliases(
            @NotNull final String keyType,
            @Nullable final Principal[] issuers)
    {
        final LinkedHashSet<String> clientAliases =
                new LinkedHashSet<>(StaticUtils.computeMapCapacity(10));

        for (final X509KeyManager m : keyManagers)
        {
            final String[] aliases = m.getClientAliases(keyType, issuers);
            if (aliases != null)
            {
                clientAliases.addAll(Arrays.asList(aliases));
            }
        }

        if (clientAliases.isEmpty())
        {
            return null;
        }
        else
        {
            final String[] aliases = new String[clientAliases.size()];
            return clientAliases.toArray(aliases);
        }
    }



    /**
     * Retrieves the nickname of the certificate that a client should use to
     * authenticate to a server.
     *
     * @param  keyType  The list of key algorithm names that may be used.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     * @param  socket   The socket to be used.  It may be {@code null} if the
     *                  certificate may be for any socket.
     *
     * @return  The nickname of the certificate to use, or {@code null} if no
     *          appropriate certificate is found.
     */
    @Override()
    @Nullable()
    public final synchronized String chooseClientAlias(
            @NotNull final String[] keyType,
            @Nullable final Principal[] issuers,
            @Nullable final Socket socket)
    {
        if (certificateAlias == null)
        {
            for (final X509KeyManager m : keyManagers)
            {
                final String alias = m.chooseClientAlias(keyType, issuers, socket);
                if (alias != null)
                {
                    return alias;
                }
            }

            return null;
        }
        else
        {
            for (final String s : keyType)
            {
                for (final X509KeyManager m : keyManagers)
                {
                    final String[] aliases = m.getClientAliases(s, issuers);
                    if (aliases != null)
                    {
                        for (final String alias : aliases)
                        {
                            // check if PKIX algorithm is used
                            if (isPKIX)
                            {
                                Pattern r = Pattern.compile("\\d+\\.\\d+\\." + certificateAlias + "$", Pattern.CASE_INSENSITIVE);
                                Matcher matcher = r.matcher(alias);
                                if (matcher.find())
                                {
//                                    chosenClientCertificateAlias = alias;
                                    return certificateAlias;
                                }
                            }
                            else
                            {
                                if (alias.equals(certificateAlias))
                                {
                                    return certificateAlias;
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }



    /**
     * Retrieves the nickname of the certificate that a client should use to
     * authenticate to a server.
     *
     * @param  keyType  The list of key algorithm names that may be used.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     * @param  engine   The SSL engine to be used.  It may be {@code null} if the
     *                  certificate may be for any engine.
     *
     * @return  The nickname of the certificate to use, or {@code null} if no
     *          appropriate certificate is found.
     */
    @Override()
    @Nullable()
    public final synchronized String chooseEngineClientAlias(
            @NotNull final String[] keyType,
            @Nullable final Principal[] issuers,
            @Nullable final SSLEngine engine)
    {
        if (certificateAlias == null)
        {
            for (final X509KeyManager m : keyManagers)
            {
                if (m instanceof X509ExtendedKeyManager)
                {
                    final X509ExtendedKeyManager em = (X509ExtendedKeyManager) m;
                    final String alias =
                            em.chooseEngineClientAlias(keyType, issuers, engine);
                    if (alias != null)
                    {
                        return alias;
                    }
                }
                else
                {
                    final String alias = m.chooseClientAlias(keyType, issuers, null);
                    if (alias != null)
                    {
                        return alias;
                    }
                }
            }

            return null;
        }
        else
        {
            for (final String s : keyType)
            {
                for (final X509KeyManager m : keyManagers)
                {
                    final String[] aliases = m.getClientAliases(s, issuers);
                    if (aliases != null)
                    {
                        for (final String alias : aliases)
                        {
                            if (alias.equals(certificateAlias))
                            {
                                return certificateAlias;
                            }
                        }
                    }
                }
            }

            return null;
        }
    }



    /**
     * Retrieves the nicknames of the server certificates of the specified type
     * contained in the key store.
     *
     * @param  keyType  The key algorithm name for which to retrieve the available
     *                  certificate nicknames.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     *
     * @return  The nicknames of the server certificates, or {@code null} if none
     *          were found in the key store.
     */
    @Override()
    @Nullable()
    public final synchronized String[] getServerAliases(
            @NotNull final String keyType,
            @Nullable final Principal[] issuers)
    {
        final LinkedHashSet<String> serverAliases =
                new LinkedHashSet<>(StaticUtils.computeMapCapacity(10));

        for (final X509KeyManager m : keyManagers)
        {
            final String[] aliases = m.getServerAliases(keyType, issuers);
            if (aliases != null)
            {
                serverAliases.addAll(Arrays.asList(aliases));
            }
        }

        if (serverAliases.isEmpty())
        {
            return null;
        }
        else
        {
            final String[] aliases = new String[serverAliases.size()];
            return serverAliases.toArray(aliases);
        }
    }



    /**
     * Retrieves the nickname of the certificate that a server should use to
     * authenticate to a client.
     *
     * @param  keyType  The key algorithm name that may be used.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     * @param  socket   The socket to be used.  It may be {@code null} if the
     *                  certificate may be for any socket.
     *
     * @return  The nickname of the certificate to use, or {@code null} if no
     *          appropriate certificate is found.
     */
    @Override()
    @Nullable()
    public final synchronized String chooseServerAlias(
            @NotNull final String keyType,
            @Nullable final Principal[] issuers,
            @Nullable final Socket socket)
    {
        if (certificateAlias == null)
        {
            for (final X509KeyManager m : keyManagers)
            {
                final String alias = m.chooseServerAlias(keyType, issuers, socket);
                if (alias != null)
                {
                    return alias;
                }
            }

            return null;
        }
        else
        {
                for (final X509KeyManager m : keyManagers)
                {
                    final String[] aliases = m.getServerAliases(keyType, issuers);
                    if (aliases != null)
                    {
                        for (final String alias : aliases)
                        {
                            // check if PKIX algorithm is used
                            if (isPKIX)
                            {
                                Pattern r = Pattern.compile("\\d+\\.\\d+\\." + certificateAlias + "$", Pattern.CASE_INSENSITIVE);
                                Matcher matcher = r.matcher(alias);
                                if (matcher.find())
                                {
                                    chosenServerCertificateAlias = alias;
                                    return certificateAlias;
                                }
                            }
                            else
                            {
                                if (alias.equals(certificateAlias))
                                {
                                    return certificateAlias;
                                }
                            }
                        }
                    }
                }

            return null;
        }
    }



    /**
     * Retrieves the nickname of the certificate that a server should use to
     * authenticate to a client.
     *
     * @param  keyType  The key algorithm name that may be used.
     * @param  issuers  The list of acceptable issuer certificate subjects.  It
     *                  may be {@code null} if any issuer may be used.
     * @param  engine   The SSL engine to be used.  It may be {@code null} if the
     *                  certificate may be for any engine.
     *
     * @return  The nickname of the certificate to use, or {@code null} if no
     *          appropriate certificate is found.
     */
    @Override()
    @Nullable()
    public final synchronized String chooseEngineServerAlias(
            @NotNull final String keyType,
            @Nullable final Principal[] issuers,
            @Nullable final SSLEngine engine)
    {
        if (certificateAlias == null)
        {
            for (final X509KeyManager m : keyManagers)
            {
                if (m instanceof X509ExtendedKeyManager)
                {
                    final X509ExtendedKeyManager em = (X509ExtendedKeyManager) m;
                    final String alias =
                            em.chooseEngineServerAlias(keyType, issuers, engine);
                    if (alias != null)
                    {
                        return alias;
                    }
                }
                else
                {
                    final String alias = m.chooseServerAlias(keyType, issuers, null);
                    if (alias != null)
                    {
                        return alias;
                    }
                }
            }

            return null;
        }
        else
        {
            for (final X509KeyManager m : keyManagers)
            {
                final String[] aliases = m.getServerAliases(keyType, issuers);
                if (aliases != null)
                {
                    for (final String alias : aliases)
                    {
                        if (alias.equals(certificateAlias))
                        {
                            return certificateAlias;
                        }
                    }
                }
            }

            return null;
        }
    }



    /**
     * Retrieves the certificate chain for the certificate with the given
     * nickname.
     *
     * @param  alias  The nickname of the certificate for which to retrieve the
     *                certificate chain.
     *
     * @return  The certificate chain for the certificate with the given nickname,
     *          or {@code null} if the requested certificate cannot be found.
     */
    @Override()
    @Nullable()
    public final synchronized X509Certificate[] getCertificateChain(
            @NotNull  final String alias)
    {
        for (final X509KeyManager m : keyManagers)
        {
            final X509Certificate[] chain = m.getCertificateChain(alias);
            if (chain != null)
            {
                return chain;
            }
            // check if PKIX algorithm is used, then check for PKIX prefix
            else if (isPKIX)
            {
                final X509Certificate[] chain2 = m.getCertificateChain(chosenServerCertificateAlias);
                return chain2;
            }
        }

        return null;
    }



    /**
     * Retrieves the private key for the specified certificate.
     *
     * @param  alias  The nickname of the certificate for which to retrieve the
     *                private key.
     *
     * @return  The private key for the requested certificate, or {@code null} if
     *          the requested certificate cannot be found.
     */
    @Override()
    @Nullable()
    public final synchronized PrivateKey getPrivateKey(
            @NotNull final String alias)
    {
        for (final X509KeyManager m : keyManagers)
        {
            final PrivateKey key = m.getPrivateKey(alias);
            if (key != null)
            {
                return key;
            }
            // check if PKIX algorithm is used, then check for PKIX prefix
            else if (isPKIX)
            {
                final PrivateKey key2 = m.getPrivateKey(chosenServerCertificateAlias);
                return key2;
            }
        }

        return null;
    }
}
