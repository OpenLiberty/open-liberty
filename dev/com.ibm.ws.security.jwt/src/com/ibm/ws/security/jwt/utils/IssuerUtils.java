/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConfig;

public class IssuerUtils {

    private static final TraceComponent tc = Tr.register(IssuerUtils.class);
    public static final String JWT_CONTEXT_PATH = "/jwt/";

    //private static AtomicServiceReference<VirtualHost> virtualHostRef;
    //    public static final String KEY_VIRTUAL_HOST = "virtualHost";
    //    private static ConcurrentServiceReferenceMap<String, VirtualHost> virtualHostRef =
    //            new ConcurrentServiceReferenceMap<String, VirtualHost>(KEY_VIRTUAL_HOST);

    //    public static void setVirtualHostService(AtomicServiceReference<VirtualHost> virtualHostServiceRef) {
    //        // TODO Auto-generated method stub
    //        virtualHostRef = virtualHostServiceRef;
    //    }
    //
    //    public static VirtualHost getVirtualHostService() {
    //        return virtualHostRef.getService();
    //    }

    //    public static synchronized void setVirtualHostMultiPleServices(ConcurrentServiceReferenceMap<String, VirtualHost> virtualHostServiceRef) {
    //        virtualHostRef = virtualHostServiceRef;
    //    }
    //
    //    public static synchronized ConcurrentServiceReferenceMap<String, VirtualHost> getVirtualHostMultipleServices() {
    //        return virtualHostRef;
    //    }
    //
    //    public static synchronized VirtualHost getVirtualHostService() {
    //        Iterator<VirtualHost> vhosts = getVirtualHostMultipleServices().getServices();
    //        //virtualHostRef.g
    //        VirtualHost vhost = null;
    //        while (vhosts.hasNext()) {
    //            vhost = vhosts.next();
    //            List<String> list = vhost.getAliases();
    //            for (String alias : list) {
    //                if (alias.equals("default_host")) {
    //                    vhost = null;
    //                }
    //            }
    //
    //        }
    //        return vhost;
    //    }

    public static String getIssuerUrl(JwtConfig jwtConfig) {

        String issuerUrl = jwtConfig.getIssuerUrl();
        String configId = jwtConfig.getId();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "issuer url is:" + issuerUrl);
        }
        if (issuerUrl != null && !issuerUrl.isEmpty()) {
            return issuerUrl;
        } else {
            issuerUrl = jwtConfig.getResolvedHostAndPortUrl();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "issuer url after resolving the host and port is:" + issuerUrl);
            }
            if (issuerUrl != null && !issuerUrl.isEmpty()) {
                return issuerUrl + JWT_CONTEXT_PATH + configId;
            }

            //            jwtConfig.getId();
            //            //VirtualHost vhost = getVirtualHostService();
            //            Iterator<VirtualHost> vhosts = getVirtualHostMultipleServices().getServices();
            //            VirtualHost defaultVirtualHost = null;
            //            while (vhosts.hasNext()) {
            //                VirtualHost vhost = vhosts.next();
            //                if (vhost != null) {
            //                    if (vhost.getName() == "default_host") {
            //                        defaultVirtualHost = vhost;
            //                        continue;
            //                    }
            //                    //String alias = getAlias(vhost);
            //                    List<String> aliases = getAliasList(vhost);
            //                    Iterator<String> it = aliases.iterator();
            //
            //                    //getHostName(vhost, alias);
            //                }
            //            }

        }
        return configId;
    }

    //    private static String getHostName(VirtualHost vhost, String alias) {
    //        return vhost.getHostName(alias);
    //    }
    //
    //    private static Integer getSecurePortNumber(VirtualHost vhost, String alias) {
    //        return vhost.getSecureHttpPort(alias);
    //    }
    //
    //    private static Integer getPortNumber(VirtualHost vhost, String alias) {
    //        return vhost.getHttpPort(alias);
    //    }
    //
    //    private static String getAlias(VirtualHost vhost) {
    //        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //            Tr.debug(tc, "vhost is:" + vhost);
    //        }
    //        List<String> aliases = getAliasList(vhost);
    //        Iterator<String> it = aliases.iterator();
    //        while (it.hasNext()) {
    //            String alias = it.next();
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "alias is:" + alias);
    //            }
    //            String hostName = getHostName(vhost, alias);
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "host name is:" + hostName);
    //            }
    //            Integer sslport = getSecurePortNumber(vhost, alias);
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "host secure port is:" + sslport);
    //            }
    //            if (hostName != null && !hostName.isEmpty() && !hostName.equals("*") && sslport > 0) {
    //                return alias;
    //            }
    //
    //            Integer port = getPortNumber(vhost, alias);
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "host port is:" + port);
    //            }
    //
    //        }
    //        it = aliases.iterator();
    //        while (it.hasNext()) {
    //            String alias = it.next();
    //            if (alias != null) {
    //                return alias;
    //            }
    //        }
    //        return null;
    //    }
    //
    //    private static List<String> getAliasList(VirtualHost vhost) {
    //        return vhost.getAliases();
    //    }

}
