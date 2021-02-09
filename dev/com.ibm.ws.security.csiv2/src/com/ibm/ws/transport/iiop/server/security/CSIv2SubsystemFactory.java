/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.server.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CSIIOP.TransportAddress;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.server.AuthenticatorImpl;
import com.ibm.ws.security.csiv2.server.TraceConstants;
import com.ibm.ws.security.csiv2.server.config.css.ClientConfigHelper;
import com.ibm.ws.security.csiv2.server.config.tss.ServerConfigHelper;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.transport.iiop.security.AbstractCsiv2SubsystemFactory;
import com.ibm.ws.transport.iiop.security.ClientPolicy;
import com.ibm.ws.transport.iiop.security.ServerPolicy;
import com.ibm.ws.transport.iiop.security.ServerPolicyFactory;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.ssl.yoko.SocketFactory;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.ReadyListener;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=3" })
@TraceOptions(traceGroup = TraceConstants.TRACE_GROUP, messageBundle = TraceConstants.MESSAGE_BUNDLE)
public class CSIv2SubsystemFactory extends AbstractCsiv2SubsystemFactory {

    private static final TraceComponent tc = Tr.register(CSIv2SubsystemFactory.class);
    private static final String ADDR_KEY = CSIv2SubsystemFactory.class.getName();

    private SecurityService securityService;
    private TokenManager tokenManager;
    private UnauthenticatedSubjectService unauthenticatedSubjectService;

    private List<String> userRegistries = Collections.emptyList();

    @Reference
    protected void setSecurityService(SecurityService securityService, Map<String, Object> props) {
        this.securityService = securityService;
        String[] userRegistryIds = (String[]) props.get("UserRegistry");
        if (userRegistryIds != null) {
            userRegistries = Arrays.asList(userRegistryIds);
        }
    }

    protected void updatedSecurityService(SecurityService securityService, Map<String, Object> props) {
        String[] userRegistryIds = (String[]) props.get("UserRegistry");
        synchronized (this) {
            if (userRegistryIds != null) {
                userRegistries = Arrays.asList(userRegistryIds);
            } else {
                userRegistries = Collections.emptyList();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known UserRegistry ids: {0}", userRegistries);
        }
        updateRegistered();
    }

    /** {@inheritDoc} */
    @Override
    protected void timeoutMessage(Set<String> requiredSslRefs, ReadyListener listener) {
        if (!super.check(requiredSslRefs)) {
            super.timeoutMessage(requiredSslRefs, listener);
        }
        if (userRegistries.isEmpty()) {
            Tr.error(tc, "NO_USER_REGISTRY", listener.listenerId(), TIMEOUT_SECONDS);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean check(Collection<String> requiredSslRefs) {
        return !userRegistries.isEmpty() && super.check(requiredSslRefs);
    }

    @Reference
    protected void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Reference
    protected void setUnuathenticatedSubjectService(UnauthenticatedSubjectService unauthenticatedSubjectService) {
        this.unauthenticatedSubjectService = unauthenticatedSubjectService;
        SecurityServices.setUnauthenticatedSubjectService(unauthenticatedSubjectService);
    }

    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        if (extraConfig == null) {
            return null;
        }

        Map<String, List<TransportAddress>> addrMap = (Map<String, List<TransportAddress>>) extraConfig.get(ADDR_KEY);
        if (addrMap == null) {
            throw new IllegalStateException("Unexpected initialization order, corba bean config not parsed first: " + extraConfig);
        }
        Authenticator authenticator = new AuthenticatorImpl(securityService.getAuthenticationService());
        String targetName = getRealm();
        TSSConfig tssConfig = new ServerConfigHelper(authenticator, tokenManager, unauthenticatedSubjectService, targetName, defaultAlias).getTSSConfig(properties, addrMap);
        Any any = orb.create_any();
        any.insert_Value(new ServerPolicy.Config(tssConfig));

        Policy securityPolicy = orb.create_policy(ServerPolicyFactory.POLICY_TYPE, any);

        return securityPolicy;
    }

    /** {@inheritDoc} */
    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        // TODO: Determine if system.RMI_OUTBOUND should be created and used for outbound.
        Authenticator authenticator = new AuthenticatorImpl(securityService.getAuthenticationService());
        String domain = getRealm();
        CSSConfig cssConfig = new ClientConfigHelper(authenticator, domain, defaultAlias).getCSSConfig(properties);
        ClientPolicy clientPolicy = new ClientPolicy(cssConfig);
        return clientPolicy;
    }

    private String getRealm() throws RegistryException {
        String realm = "defaultRealm";
        UserRegistryService userRegistryService = securityService.getUserRegistryService();
        if (userRegistryService.isUserRegistryConfigured())
            realm = userRegistryService.getUserRegistry().getRealm();

        return realm;
    }

    private static final String ENDPOINT_KEY = "yoko.orb.oa.endpoint";

    /** {@inheritDoc} */
    @Override
    public void addTargetORBInitProperties(Properties initProperties, Map<String, Object> configProps, List<IIOPEndpoint> endpoints, Map<String, Object> extraProperties) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<TransportAddress>> addrMap = extractTransportAddresses(configProps, endpoints, sb);
        extraProperties.put(ADDR_KEY, addrMap);
        sb.setLength(sb.length() - 1);
        initProperties.put(ENDPOINT_KEY, sb.toString());
    }

    /**
     * @param host
     * @param port
     * @param sslAliasName
     * @param soReuseAddr
     * @param sb
     */
    private static void bindOptions(String host, int port, String sslAliasName, Boolean soReuseAddr, StringBuilder sb) {
        sb.append("iiop --bind ").append(host).append(" --host ").append(host);
        if (port > 0) {
            sb.append(" --port ").append(port);
        }
        if (sslAliasName != null && sslAliasName.trim().isEmpty() == false) {
            sb.append(" --no-profile --sslConfigName ").append(sslAliasName);
        }
        sb.append(" --soReuseAddr ").append(soReuseAddr);
        sb.append(",");
    }

    private Map<String, List<TransportAddress>> extractTransportAddresses(Map<String, Object> properties, List<IIOPEndpoint> endpoints, StringBuilder sb) {
        Map<String, List<TransportAddress>> mapOfAddr = new HashMap<String, List<TransportAddress>>();
        List<TransportAddress> unsecured = new ArrayList<TransportAddress>();
        mapOfAddr.put(null, unsecured);
        for (IIOPEndpoint ep : endpoints) {
            Boolean soReuseAddr = (Boolean) ep.getTcpOptions().get("soReuseAddr");
            if (soReuseAddr == null) {
                soReuseAddr = true;
            }
            String host = ep.getHost();
            if (ep.getIiopPort() > 0) {
                bindOptions(host, ep.getIiopPort(), null, soReuseAddr, sb);
                unsecured.add(new TransportAddress(host, (short) ep.getIiopPort()));
            }
            for (Map<String, Object> iiopsOptions : ep.getIiopsOptions()) {
                String sslAliasName = (String) iiopsOptions.get("sslRef");
                if (sslAliasName == null)
                    sslAliasName = defaultAlias;
                int iiopsPort = (Integer) iiopsOptions.get("iiopsPort");
                bindOptions(host, iiopsPort, sslAliasName, soReuseAddr, sb);
                List<TransportAddress> secured = mapOfAddr.get(sslAliasName);
                if (secured == null) {
                    secured = new ArrayList<TransportAddress>();
                    mapOfAddr.put(sslAliasName, secured);
                }
                secured.add(new TransportAddress(host, (short) iiopsPort));
            }
        }
        return mapOfAddr;
    }

    /** {@inheritDoc} */
    @Override
    public void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {
        args.add("-IIOPconnectionHelper");
        args.add(SocketFactory.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    protected Set<String> extractSslRefs(Map<String, Object> properties, List<IIOPEndpoint> endpoints) {
        Set<String> result = new HashSet<String>();
        for (IIOPEndpoint endpoint : endpoints) {
            for (Map<String, Object> iiopsOptions : endpoint.getIiopsOptions()) {
                String sslAliasName = (String) iiopsOptions.get("sslRef");
                if (sslAliasName == null)
                    sslAliasName = defaultAlias;
                result.add(sslAliasName);
            }
        }
        result.addAll(new ClientConfigHelper(null, null, defaultAlias).extractSslRefs(properties));
        result.addAll(new ServerConfigHelper(null, null, null, null, defaultAlias).extractSslRefs(properties));
        return result;
    }

}
