/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.Constants;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.TAG_NULL_TAG;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * At the moment, this config class can only handle a single address.
 *
 * @version $Rev: 504461 $ $Date: 2007-02-07 00:42:26 -0800 (Wed, 07 Feb 2007) $
 */
public class TSSSSLTransportConfig extends TSSTransportMechConfig {

    private static final TraceComponent tc = Tr.register(TSSSSLTransportConfig.class);

    private transient Authenticator authenticator;
    private TransportAddress[] transportAddresses;
    private short handshakeTimeout = -1;
    private short supports;
    private short requires;

    public TSSSSLTransportConfig() {}

    /**
     * @param authenticator The Authenticator for authenticating the certificate chain.
     */
    public TSSSSLTransportConfig(Authenticator authenticator) {
        this.authenticator = authenticator;
        transportAddresses = new TransportAddress[] {};
    }

    public TSSSSLTransportConfig(TaggedComponent component, Codec codec) throws UserException {
        Any any = codec.decode_value(component.component_data, TLS_SEC_TRANSHelper.type());
        TLS_SEC_TRANS tst = TLS_SEC_TRANSHelper.extract(any);

        supports = tst.target_supports;
        requires = tst.target_requires;
        transportAddresses = tst.addresses;
    }

    public void setTransportAddresses(List<TransportAddress> addrs) {
        this.transportAddresses = addrs.toArray(new TransportAddress[addrs.size()]);
    }

    public TransportAddress[] getTransportAddresses() {
        return transportAddresses.clone(); // TODO: Determine if returning the actual field is enough.
    }

    public short getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public void setHandshakeTimeout(short handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    @Override
    public short getSupports() {
        return supports;
    }

    public void setSupports(short supports) {
        this.supports = supports;
    }

    @Override
    public short getRequires() {
        return requires;
    }

    public void setRequires(short requires) {
        this.requires = requires;
    }

    @Override
    public TaggedComponent encodeIOR(Codec codec) {
        TaggedComponent result = new TaggedComponent();

        TLS_SEC_TRANS tst = new TLS_SEC_TRANS();

        tst.target_supports = supports;
        tst.target_requires = requires;
        tst.addresses = transportAddresses;

        try {
            Any any = ORB.init().create_any();
            TLS_SEC_TRANSHelper.insert(any, tst);

            result.tag = TAG_TLS_SEC_TRANS.value;
            result.component_data = codec.encode_value(any);
        } catch (Exception ex) {
            Tr.error(tc, "Error enncoding transport tagged component, defaulting encoding to NULL");

            result.tag = TAG_NULL_TAG.value;
            result.component_data = new byte[0];
        }

        return result;
    }

    @Override
    public Subject check(SSLSession session) throws SASException {
        validateSSLSessionExistsWhenSSLRequired(session);
        return tryToAuthenticate(session);
    }

    private void validateSSLSessionExistsWhenSSLRequired(SSLSession session) throws SASException {
        if (session == null && requires != 0) {
            // TODO: Get translated message and change security minor code
            throw new SASInvalidEvidenceException("The target security service requires client certificate authentication, but there was no SSL session found.", SecurityMinorCodes.AUTHENTICATION_FAILED);
        }
    }

    /*
     * Attempt to authenticate using a certificate chain. Handles exceptions.
     */
    @FFDCIgnore({ SSLPeerUnverifiedException.class, Exception.class })
    private Subject tryToAuthenticate(SSLSession session) throws SASException {
        Subject transportSubject = null;
        try {
            transportSubject = authenticateWithCertificateChain(session);
        } catch (SSLPeerUnverifiedException e) {
            throwExceptionIfClientCertificateAuthenticationIsRequired(e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The peer could not be verified, but ignoring because client certificate authentication is not required. The exception is: " + e.getMessage());
            }
        } catch (Exception e) {
            /*
             * All the possible exceptions, including AuthenticationException, CredentialExpiredException
             * and CredentialDestroyedException are caught here, and re-thrown
             */
            throwExceptionIfClientCertificateAuthenticationIsRequired(e);
        }

        return transportSubject;
    }

    /*
     * Authenticate using a certificate chain. Exception handling done in caller method.
     */
    private Subject authenticateWithCertificateChain(SSLSession session) throws SSLPeerUnverifiedException, AuthenticationException, CredentialExpiredException, CredentialDestroyedException {
        Subject transportSubject = null;
        if (session != null) {
            Certificate[] certificateChain = session.getPeerCertificates();
            transportSubject = authenticator.authenticate((X509Certificate[]) certificateChain);
            /* Here we need to get the WSCredential from the subject. We will use the subject manager for the same. */
            SubjectHelper subjectHelper = new SubjectHelper();
            WSCredential wsCredential = subjectHelper.getWSCredential(transportSubject);
            /*
             * First we tell the WSCredential that the identity token is in the form of a certificate chain. This is
             * done by setting the property "wssecurity.identity_name" to "ClientCertificate"
             */
            wsCredential.set(Constants.IDENTITY_NAME, Constants.ClientCertificate);
            /*
             * Now we need to put the certificate chain into the WScredential object. By doing this, we
             * make sure that, the authenticated certificates are indeed part of the credential. This credential
             * can be used further down the CSIv2 flow. The certificate is set as a name value pair, with the name
             * "wssecurity.identity_value"
             */
            wsCredential.set(Constants.IDENTITY_VALUE, certificateChain);
        }
        return transportSubject;
    }

    private void throwExceptionIfClientCertificateAuthenticationIsRequired(Exception e) throws SASException {
        if (clientCertificateAuthenticationRequired()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Client certificate authentication is required, but it was not possible to authenticate. The exception is: " + e.getMessage());
            }
            throw new SASInvalidEvidenceException(e.getMessage(), SecurityMinorCodes.AUTHENTICATION_FAILED);
        }
    }

    private boolean clientCertificateAuthenticationRequired() {
        return (requires & EstablishTrustInClient.value) != 0;
    }

    /** {@inheritDoc} */
    @FFDCIgnore({ SSLPeerUnverifiedException.class })
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, SSLSession session) {
        if (session != null) {
            try {
                Certificate[] certificateChain = session.getPeerCertificates();
                return trustedIDEvaluator.isTrusted((X509Certificate[]) certificateChain);
            } catch (SSLPeerUnverifiedException e) {
                // TODO Determine if a message is needed
            }
        }
        return false;
    }

    @Override
    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSSSLTransportConfig: [\n");
        buf.append(moreSpaces).append("SUPPORTS: ").append(ConfigUtil.flags(supports)).append("\n");
        buf.append(moreSpaces).append("REQUIRES: ").append(ConfigUtil.flags(requires)).append("\n");
        if (transportAddresses != null) {
            for (TransportAddress addr : transportAddresses) {
                if (addr != null) {
                    buf.append(moreSpaces).append("  ").append("hostName: ").append(addr.host_name).append(",  port    : ").append(addr.port).append("\n");
                }
            }
        }
        buf.append(moreSpaces).append("handshakeTimeout: ").append(handshakeTimeout).append("\n");
        buf.append(spaces).append("]\n");
    }

}
