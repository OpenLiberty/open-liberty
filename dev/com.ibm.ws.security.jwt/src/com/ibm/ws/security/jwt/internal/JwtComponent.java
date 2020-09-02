/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

@Component(service = JwtConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.jwt.builder", name = "jwtConfig", property = "service.vendor=IBM")
public class JwtComponent implements JwtConfig {

    private static final TraceComponent tc = Tr.register(JwtComponent.class);

    private String issuer = null;
    private String issuerUrl = null;
    private long valid;
    private long expiresInSeconds;
    private boolean isJwkEnabled;
    private boolean jti;
    private List<String> audiences;
    private String sigAlg;
    private List<String> claims;
    private String scope;
    private String sharedKey;
    private String keyStoreRef;
    private String trustStoreRef;
    private String keyAlias;
    private String trustedAlias;
    private long jwkRotationTime;
    private int jwkSigningKeySize;
    private long elapsedNbfTime;

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;

    private JWKProvider jwkProvider = null;

    private DynamicMBean httpsendpointInfoMBean;

    private DynamicMBean httpendpointInfoMBean;

    private ServerInfoMBean serverInfoMBean;

    private List<String> amrAttributes;

    private final KeyAlgorithmChecker keyAlgChecker = new KeyAlgorithmChecker();

    @org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        httpendpointInfoMBean = endpointInfoMBean;
    }

    protected void unsetEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        if (httpendpointInfoMBean == endpointInfoMBean) {
            httpendpointInfoMBean = null;
        }
    }

    @org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        httpsendpointInfoMBean = endpointInfoMBean;
    }

    protected void unsetHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        if (httpsendpointInfoMBean == endpointInfoMBean) {
            httpsendpointInfoMBean = null;
        }
    }

    /**
     * DS injection WebSphere:feature=kernel,name=ServerInfo
     */
    @org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=kernel,name=ServerInfo)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setServerInfoMBean(ServerInfoMBean serverInfoMBean) {
        this.serverInfoMBean = serverInfoMBean;
    }

    protected void unsetServerInfoMBean(ServerInfoMBean serverInfoMBean) {
        if (this.serverInfoMBean == serverInfoMBean) {
            this.serverInfoMBean = null;
        }
    }

    @Activate
    protected void activate(Map<String, Object> properties, ComponentContext cc) {
        process(properties);
    }

    @Modified
    protected void modify(Map<String, Object> properties) {
        process(properties);
    }

    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {

    }

    private void process(Map<String, Object> props) {
        // TODO Auto-generated method stub
        if (props == null || props.isEmpty()) {
            return;
        }
        issuer = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_ID));
        issuerUrl = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_ISSUER));
        isJwkEnabled = (Boolean) props.get(JwtUtils.CFG_KEY_JWK_ENABLED);
        jti = (Boolean) props.get(JwtUtils.CFG_KEY_JTI);
        valid = ((Long) props.get(JwtUtils.CFG_KEY_VALID)).longValue();
        expiresInSeconds = ((Long) props.get(JwtUtils.CFG_KEY_EXPIRES_IN_SECONDS)).longValue();
        sigAlg = JwtConfigUtil.getSignatureAlgorithm(props, JwtUtils.CFG_KEY_SIGNATURE_ALGORITHM);
        audiences = JwtUtils.trimIt((String[]) props.get(JwtUtils.CFG_KEY_AUDIENCES));
        scope = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_SCOPE));
        claims = JwtUtils.trimIt((String[]) props.get(JwtUtils.CFG_KEY_CLAIMS));
        sharedKey = JwtConfigUtil.processProtectedString(props, JwtUtils.CFG_KEY_SHARED_KEY);
        keyStoreRef = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_KEYSTORE_REF));
        keyAlias = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_KEY_ALIAS_NAME));
        trustStoreRef = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_TRUSTSTORE_REF));
        trustedAlias = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_TRUSTED_ALIAS));
        // TODO
        // Check the algorithm and see if the key type matches with it. If not,
        // log warning and
        // make the key type matches with the algorithm
        // RS256=x509
        // HS256=shared secret
        // It it is JWK, then the algorithm should be RS256
        jwkRotationTime = (Long) props.get(JwtUtils.CFG_KEY_JWK_ROTATION_TIME);
        // Rotation time is in minutes, so convert value to milliseconds
        jwkRotationTime = jwkRotationTime * 60 * 1000;
        jwkSigningKeySize = ((Long) props.get(JwtUtils.CFG_KEY_JWK_SIGNING_KEY_SIZE)).intValue();
        elapsedNbfTime = ((Long) props.get(JwtUtils.CFG_KEY_ELAPSED_NBF)).longValue();
        amrAttributes = JwtUtils.trimIt((String[]) props.get(JwtUtils.CFG_AMR_ATTR));

        if (isJwkCapableSigAlgorithm()) {
            initializeJwkProvider(this);
        }

        //expiresInSeconds wins if present
        if (expiresInSeconds > -1) {
            valid = expiresInSeconds;
        } else {
            valid = valid * 3600;
        }
    }

    private boolean isJwkCapableSigAlgorithm() {
        if (sigAlg == null) {
            return false;
        }
        return (keyAlgChecker.isRSAlgorithm(sigAlg) || keyAlgChecker.isESAlgorithm(sigAlg));
    }

    private void initializeJwkProvider(JwtConfig jwtConfig) {

        if (jwtConfig == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No config object found");
            }
            return;
        }
        if (jwtConfig.isJwkEnabled()) {
            jwkProvider = new JWKProvider(jwtConfig.getJwkSigningKeySize(), jwtConfig.getSignatureAlgorithm(),
                    jwtConfig.getJwkRotationTime());
        }
    }

    @Override
    public PrivateKey getPrivateKey() {

        return privateKey;
    }

    @Override
    public PublicKey getPublicKey() {

        return publicKey;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return issuer;
    }

    @Override
    public String getIssuerUrl() {
        // TODO Auto-generated method stub
        return issuerUrl;
    }

    @Override
    public long getValidTime() {
        // Converted from hours to seconds 5/2018
        return valid;
    }

    @Override
    public List<String> getAudiences() {
        // TODO Auto-generated method stub
        return audiences;
    }

    @Override
    public String getSignatureAlgorithm() {
        // TODO Auto-generated method stub
        return sigAlg;
    }

    @Override
    public List<String> getClaims() {
        // TODO Auto-generated method stub
        return claims;
    }

    @Override
    public String getScope() {
        // TODO Auto-generated method stub
        return scope;
    }

    @Override
    public boolean getJti() {
        // TODO Auto-generated method stub
        return jti;
    }

    @Override
    public String getTrustStoreRef() {
        // TODO Auto-generated method stub
        return trustStoreRef;
    }

    @Override
    public String getKeyStoreRef() {
        // TODO Auto-generated method stub
        return keyStoreRef;
    }

    @Override
    public String getKeyAlias() {
        // TODO Auto-generated method stub
        return keyAlias;
    }

    @Override
    public String getTrustedAlias() {
        // TODO Auto-generated method stub
        return trustedAlias;
    }

    @Override
    @Sensitive
    public String getSharedKey() {
        return sharedKey;
    }

    @Override
    public String getJwkJsonString() {
        if (!isJwkEnabled() && jwkProvider == null) {
            // create jwk from x509 certificate.
            try {
                privateKey = JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
                publicKey = JwtUtils.getPublicKey(keyAlias, keyStoreRef);
            } catch (KeyStoreException kse) {

            } catch (CertificateException cfe) {

            } catch (Exception e) {

            }
            if (publicKey != null && privateKey != null) {
                jwkProvider = new JWKProvider(getJwkSigningKeySize(), getSignatureAlgorithm(), getJwkRotationTime(),
                        publicKey, privateKey);
            }
        }
        return jwkProvider != null ? jwkProvider.getJwkSetString() : null;
    }

    @Override
    public JSONWebKey getJSONWebKey() {
        return jwkProvider != null ? jwkProvider.getJWK() : null;
    }

    @Override
    public long getJwkRotationTime() {
        return jwkRotationTime;
    }

    @Override
    public int getJwkSigningKeySize() {
        return jwkSigningKeySize;
    }

    @Override
    public boolean isJwkEnabled() {
        // TODO Auto-generated method stub
        return isJwkEnabled;
    }

    @Override
    public String getResolvedHostAndPortUrl() {
        if (httpsendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpsendpointInfoMBean.getAttribute("Host"));
                int port = (Integer) httpsendpointInfoMBean.getAttribute("Port");
                return "https://" + host + ":" + port;
            } catch (Exception e) {

            }
        }
        if (httpendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpendpointInfoMBean.getAttribute("Host"));
                int port = (Integer) httpendpointInfoMBean.getAttribute("Port");
                return "http://" + host + ":" + port;
            } catch (Exception e) {

            }
        }
        return null;
    }

    @Override
    public long getElapsedNbfTime() {
        return elapsedNbfTime;
    }

    /**
     * If the given host is "*", try to resolve this to a hostname or ip address
     * by first checking the configured ${defaultHostName}. If
     * ${defaultHostName} is "*", "localhost", or not specified, try obtaining
     * the local ip address via InetAddress.
     *
     * @return the resolved host, or "localhost" if the host could not be
     *         resolved
     */
    protected String resolveHost(String host) {
        if ("*".equals(host)) {
            // Check configured ${defaultHostName}
            if (serverInfoMBean != null) {
                host = serverInfoMBean.getDefaultHostname();
                if (host == null || host.equals("localhost")) {
                    // This is, as a default, not useful. Use the local IP
                    // address instead.
                    host = getLocalHostIpAddress();
                }
            } else {
                host = getLocalHostIpAddress();
            }

        }
        return (host == null || host.trim().isEmpty()) ? "localhost" : host;
    }

    /**
     * @return InetAddress.getLocalHost().getHostAddress(); or null if that
     *         fails.
     */
    protected String getLocalHostIpAddress() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            });

        } catch (PrivilegedActionException pae) {
            // FFDC it
            return null;
        }
    }

    @Override
    public List<String> getAMRAttributes() {
        return amrAttributes;
    }

}
