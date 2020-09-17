/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.util.Arrays;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.http.VirtualHost;

import io.openliberty.microprofile.openapi20.utils.CloudUtils;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.ServerInfo;

/**
 * The DefaultHostListener class is a singleton OSGi component that listens for changes to the default_host virtual host
 * and updates a ServerInfo instance with the relevant information. This ServerInfo object is used when generating the
 * server definitions in the OpenAPI document, if required.
 */
@Component(service = { DefaultHostListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class DefaultHostListener {

    private static final TraceComponent tc = Tr.register(DefaultHostListener.class);

    private static DefaultHostListener instance = null;
    
    private final ServerInfo defaultHostServerInfo = new ServerInfo();

    /**
     * The getInstance method returns the singleton instance of the DefaultHostListener
     * 
     * @return DefaultHostListener
     *             The singleton instance
     */
    public static DefaultHostListener getInstance() {
        return instance;
    }

    /**
     * The getDefaultHostServerInfo method returns the ServerInfo object for the default_host virtual host.
     * 
     * @return ServerInfo
     *          The ServerInfo object for the default_host virtual host.
     */
    public ServerInfo getDefaultHostServerInfo() {
        return defaultHostServerInfo;
    }
    
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        instance = this;
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Activating DefaultHostListener", properties);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        instance = null;
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Deactivating DefaultHostListener, reason=" + reason);
        }
    }
    
    @Reference(service = VirtualHost.class, target = "(&(enabled=true)(id=default_host)(|(aliases=*)(httpsAlias=*)))", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateDefaultHostServerInfo(vhost, props);
    }

    protected void updatedVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateDefaultHostServerInfo(vhost, props);
    }

    /**
     * The updateDefaultHostServerInfo method is invoked whenever the default_host virtual host for the server is
     * modified.  It updates the ServerInfo to reflect the changes that have been made.
     * 
     * @param vhost
     *            The VirtualHost that has been set/updated
     * @param props
     *            The properties that have been set/modified
     */
    private void updateDefaultHostServerInfo(VirtualHost vhost, Map<String, Object> props) {

        Object value = props.get("httpsAlias");
        if (value == null) {
            String[] aliases = (String[]) props.get("aliases");
            if (LoggingUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "httpsAlias is null. aliases : " + String.join(", ", aliases));
            }
            value = Arrays.stream(aliases).filter(a -> !a.endsWith(":-1")).findFirst().orElse(null);
            if (LoggingUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "Found non-secure alias: " + value);
            }
        }

        String alias = String.valueOf(value);

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Received new alias: " + alias);
        }

        final String vcapHost = CloudUtils.getVCAPHost();
        final String host;
        if (vcapHost == null) {
            host = vhost.getHostName(alias);
        } else {
            host = vcapHost;
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Changed host using VCAP_APPLICATION.  New value: " + defaultHostServerInfo.getHost());
            }
        }

        synchronized (this.defaultHostServerInfo) {
            defaultHostServerInfo.setHttpPort(vhost.getHttpPort(alias));
            defaultHostServerInfo.setHttpsPort(vhost.getSecureHttpPort(alias));
            defaultHostServerInfo.setHost(host);
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Updated server information: " + defaultHostServerInfo);
        }
    }
}