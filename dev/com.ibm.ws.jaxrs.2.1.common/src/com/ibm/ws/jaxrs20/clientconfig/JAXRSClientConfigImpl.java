/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.clientconfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientConfigHolder;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientConstants;

@Component(immediate = true,
           service = { JAXRSClientConfig.class },
           configurationPid = "com.ibm.ws.jaxrs20.common.clientConfig",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM" })
public class JAXRSClientConfigImpl implements JAXRSClientConfig {
    private static final TraceComponent tc = Tr.register(JAXRSClientConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String URI = "uri";

    private static final HashSet<String> propertiesToRemove = new HashSet<>();
    private static final HashMap<String, String> propsToTranslate = new HashMap<>();
    static {
        // this is stuff the framework always adds, we don't need it so we'll filter it out.
        propertiesToRemove.add("defaultSSHPublicKeyPath");
        propertiesToRemove.add("defaultSSHPrivateKeyPath");
        propertiesToRemove.add("config.overrides");
        propertiesToRemove.add("config.id");
        propertiesToRemove.add("component.id");
        propertiesToRemove.add("config.displayId");
        propertiesToRemove.add("component.name");
        propertiesToRemove.add("config.source");
        propertiesToRemove.add("service.pid");
        propertiesToRemove.add("service.vendor");
        propertiesToRemove.add("service.factoryPid");
        propertiesToRemove.add(URI);

        // translate the shorthand notation in server.xml to the longer property name that is used programatically
        propsToTranslate.put("connectiontimeout", JAXRSClientConstants.CONNECTION_TIMEOUT);
        propsToTranslate.put("disablecncheck", JAXRSClientConstants.DISABLE_CN_CHECK);
        propsToTranslate.put("proxyhost", JAXRSClientConstants.PROXY_HOST);
        propsToTranslate.put("proxyport", JAXRSClientConstants.PROXY_PORT);
        propsToTranslate.put("proxytype", JAXRSClientConstants.PROXY_TYPE);
        propsToTranslate.put("receivetimeout", JAXRSClientConstants.RECEIVE_TIMEOUT);
        propsToTranslate.put("sslconfig", JAXRSClientConstants.SSL_REFKEY);

    }

    /**
     * given the map of properties, remove ones we don't care about, and translate some others.
     * If it's not one we're familiar with, transfer it unaltered
     *
     * @param props - input list of properties
     * @return - a new Map of the filtered properties.
     */
    private Map<String, String> filterProps(Map<String, Object> props) {
        HashMap<String, String> filteredProps = new HashMap<>();
        Iterator<String> it = props.keySet().iterator();
        boolean debug = tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();

        while (it.hasNext()) {
            String key = it.next();
            String newKey = null;
            if (debug) {
                Tr.debug(tc, "key: " + key + " value: " + props.get(key));
            }
            // skip stuff we don't care about
            if (propertiesToRemove.contains(key)) {
                continue;
            }

            // convert from shorthand key (timeout) to actual prop name (com.ibm.way.too.long.name.timeout)
            // note that this swap is NOT case sensitive.
            newKey = key;
            if (propsToTranslate.containsKey(key.toLowerCase())) {
                newKey = propsToTranslate.get(key.toLowerCase());
                if (debug) {
                    Tr.debug(tc, " translated " + key + " to " + newKey);
                }
            }

            filteredProps.put(newKey, props.get(key).toString());

            // special case for authnToken
            if (newKey.compareTo("authnToken") == 0) {
                String replacementKey = validateAuthn(props.get(key).toString());
                if (replacementKey != null) {
                    filteredProps.remove(newKey);
                    filteredProps.put(replacementKey, "true");
                } else {
                    filteredProps.remove(newKey); // invalid token type, back it out.
                }
            }
        }
        return filteredProps;
    }

    /**
     * validate the value for authnToken key and select appropriate new key
     * Note that the check is not case sensitive.
     *
     * @param value - shorthand key name
     * @return long property name
     */
    private String validateAuthn(String value) {
        // for now, if we got here we're validating an authnToken
        String result = null;
        String valueLower = value.toLowerCase();
        do {
            if (valueLower.equals("saml")) {
                result = JAXRSClientConstants.SAML_HANDLER;
                break;
            }
            if (valueLower.equals("oauth")) {
                result = JAXRSClientConstants.OAUTH_HANDLER;
                break;
            }
            if (valueLower.equals("mpjwt")) {
                result = JAXRSClientConstants.MPJWT_HANDLER;
                break;
            }
            if (valueLower.equals("jwt")) {
                result = JAXRSClientConstants.JWT_HANDLER;
                break;
            }
            if (valueLower.equals("ltpa")) {
                result = JAXRSClientConstants.LTPA_HANDLER;
            }
        } while (false);
        if (result == null) {
            Tr.warning(tc, "warn.invalid.authorization.token.type", value); // CWWKW0061W
        }
        return result;
    }

    /**
     * find the uri parameter which we will key off of
     *
     * @param props
     * @return value of uri param within props, or null if no uri param
     */
    private String getURI(Map<String, Object> props) {
        if (props == null)
            return null;
        if (props.keySet().contains(URI)) {
            return (props.get(URI).toString());
        } else {
            return null;
        }
    }

    /**
     * if an encoded uri was specified, ex: http%3A%2F%2Ffred,
     * then we'll need to decode it to get a match
     * because that is what the apache code does to get a lookup string.
     *
     * TODO: couldn't get this to work because we can't resolve URIBuilder at the time the service starts.
     * need to find a way around that.
     *
     * @param uri
     * @return normalized uri
     */
    private String normalize(String uri) {
        return uri;
        //return javax.ws.rs.core.UriBuilder.fromUri(uri).build().toString();

    }

    //needed to defer activation until the cxf jaxrs client impl classes are loaded.
    //otherwise, our call to UriBuilder can fail.
    // todo: find a way to resolve uribuilder
    /*---------
    @Reference(service = com.ibm.wsspi.classloading.ResourceProvider.class, target = "(resources=META-INF/services/javax.ws.rs.client.ClientBuilder)")
    protected void setResourceProvider(com.ibm.wsspi.classloading.ResourceProvider rp) {
        //no-op
    }

    protected void unsetResourceProvider(com.ibm.wsspi.classloading.ResourceProvider rp) {
        //no-op
    }

    @Reference
    protected void setJaxRsServiceActivator(JaxRsServiceActivator jrsa) {
        //no-op
    }

    protected void unsetJaxRsServiceActivator(JaxRsServiceActivator jrsa) {
        //no-op
    }
    ------------*/

    @Activate
    protected void activate(Map<String, Object> properties) {
        if (properties == null)
            return;
        String uri = getURI(properties);
        if (uri == null)
            return;
        uri = normalize(uri);
        JAXRSClientConfigHolder.addConfig(this.toString(), uri, filterProps(properties));
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        if (properties == null)
            return;
        JAXRSClientConfigHolder.removeConfig(this.toString());
        // if they deleted the uri attribute, no point in adding.
        String uri = getURI(properties);
        if (uri == null)
            return;
        uri = normalize(uri);
        JAXRSClientConfigHolder.addConfig(this.toString(), uri, filterProps(properties));
    }

    @Deactivate
    protected void deactivate() {
        JAXRSClientConfigHolder.removeConfig(this.toString());

    }
}
