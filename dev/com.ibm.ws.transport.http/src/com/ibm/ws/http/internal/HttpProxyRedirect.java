/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>
 * This class is the implementation of the config element httpProxyRedirect. It is used
 * in cases where a proxy server may use different ports than the server that this is
 * running in. When a redirect (from non-secure HTTP port to secure HTTPS port) is
 * desired, this server will need to know which port to redirect to. For example:
 * A proxy server is listening on ports 80/443 on www.ibm.com and it forwards web
 * requests to a Liberty server, myserver.ibm.com, on ports 9080/9443. In order to
 * redirect to the correct port (443), the Liberty server must have some configuration
 * to tell it to redirect requests originally for 80 to 443 (and not 9443). This
 * class handles that.
 * </p><o>
 * By default, all requests on port 80 will be redirected to 443. If a user specifies
 * any other configuration, then that config will be used instead. Here is a sample
 * configuration:
 * <br/>
 * <code>
 * &lt;httpProxyRedirect enabled="true" host="myhost" httpPort="4444" httpsPort="5555"/&gt;
 * </code>
 * </p><p>
 * This tells the server to redirect requests made against myhost:4444 to myhost:5555.
 * </p>
 */
@Component(configurationPid = "com.ibm.ws.http.proxyredirect",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           service = HttpProxyRedirect.class,
           property = { "service.vendor=IBM" })
public class HttpProxyRedirect {

    private final static TraceComponent tc = Tr.register(HttpProxyRedirect.class, "HTTPTransport");

    final static String PROP_ENABLED = "enabled";
    final static String PROP_HOST = "host";
    final static String PROP_HTTP_PORT = "httpPort";
    final static String PROP_HTTPS_PORT = "httpsPort";

    private final static int DEFAULT_HTTP_PORT = 80;
    private final static int DEFAULT_HTTPS_PORT = 443;
    private final static String STAR = "*";

    private final static Map<Integer, Map<String, HttpProxyRedirect>> map = new HashMap<Integer, Map<String, HttpProxyRedirect>>();

    private boolean enabled;
    private int httpPort;
    private int httpsPort;
    private String host;

    /**
     * <p>
     * This method returns the secure port number to redirect to given the specified
     * host and incoming (non-secure) port number. If a proxy redirect has been configured
     * with the specified host and httpPort, the associated httpsPort will be returned.
     * </p><p>
     * If the specified httpPort has been configured but not with the specified host, then
     * this method will return the httpsPort associated with a proxy redirect that has been
     * configured with a wildcard if one exists (i.e. &lt;httpProxyRedirect host="*" .../&gt;).
     * </p><p>
     * If no proxy redirect has been configured for the specified httpPort, then this method
     * will return null.
     * </p>
     * 
     * @return the httpsPort associated with the proxy redirect for the specified host/httpPort.
     */
    public static Integer getRedirectPort(String host, int httpPort) {
        Integer httpsPort = null;
        synchronized (map) {
            if (httpPort == DEFAULT_HTTP_PORT && map.get(httpPort) == null) {
                // use default redirect of 80 to 443
                httpsPort = DEFAULT_HTTPS_PORT;
            } else {
                Map<String, HttpProxyRedirect> redirectsForThisPort = map.get(httpPort);
                if (redirectsForThisPort != null) {
                    HttpProxyRedirect hdr = redirectsForThisPort.get(host);
                    if (hdr == null) {
                        hdr = redirectsForThisPort.get(STAR);
                    }
                    if (hdr != null && hdr.enabled) {
                        httpsPort = hdr.httpsPort;
                    }
                }
            }
        }
        return httpsPort;
    }

    private static void putProxyRedirect(HttpProxyRedirect proxyRedirect) {
        synchronized (map) {
            Map<String, HttpProxyRedirect> redirectsForThisPort = map.get(proxyRedirect.httpPort);
            if (redirectsForThisPort == null) {
                redirectsForThisPort = new HashMap<String, HttpProxyRedirect>();
                map.put(proxyRedirect.httpPort, redirectsForThisPort);
            }
            HttpProxyRedirect oldRedirect = redirectsForThisPort.put(proxyRedirect.host, proxyRedirect);
            if (oldRedirect != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "activate - overwriting existing proxy redirect, " + oldRedirect + ", with " + proxyRedirect);
                /*
                 * This could occur in cases where the user configured two httpProxyRedirects for the same host and httpPort - ex:
                 * <httpProxyRedirect host="myhost" httpPort="4444" httpsPort="5555"/>
                 * ...
                 * <httpProxyRedirect host="myhost" httpPort="4444" httpsPort="6666"/>
                 * 
                 * This might be worth logging as a warning...
                 */
            }
        }
    }

    private static void removeProxyRedirect(HttpProxyRedirect proxyRedirect) {
        synchronized (map) {
            Map<String, HttpProxyRedirect> redirectsForThisPort = map.get(proxyRedirect.httpPort);
            if (redirectsForThisPort != null) {
                redirectsForThisPort.remove(proxyRedirect.host);
                if (redirectsForThisPort.isEmpty()) {
                    map.remove(proxyRedirect.httpPort);
                }
            }
        }
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        Dictionary<String, Object> config = ctx.getProperties();
        enabled = (Boolean) config.get(PROP_ENABLED);
        host = (String) config.get(PROP_HOST);
        httpPort = (Integer) config.get(PROP_HTTP_PORT);
        httpsPort = (Integer) config.get(PROP_HTTPS_PORT);

        putProxyRedirect(this);

    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        removeProxyRedirect(this);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        synchronized (map) {
            removeProxyRedirect(this);
            enabled = (Boolean) config.get(PROP_ENABLED);
            host = (String) config.get(PROP_HOST);
            httpPort = (Integer) config.get(PROP_HTTP_PORT);
            httpsPort = (Integer) config.get(PROP_HTTPS_PORT);
            putProxyRedirect(this);
        }
    }

    @Override
    public String toString() { // used for trace output
        return "HttpProxyRedirect[enabled=" + enabled + ", host=" + host + ", httpPort=" + httpPort + ", httpsPort=" + httpsPort + "]";
    }
}
