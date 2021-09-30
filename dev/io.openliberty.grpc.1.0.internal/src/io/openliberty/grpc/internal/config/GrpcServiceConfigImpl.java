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
package io.openliberty.grpc.internal.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.grpc.internal.GrpcMessages;

/**
 * Adapted from com.ibm.ws.jaxrs20.clientconfig.JAXRSClientConfig
 */
@Component(immediate = true, service = { GrpcServiceConfig.class,
        ApplicationRecycleComponent.class }, configurationPid = "io.openliberty.grpc.serverConfig", configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
                "service.vendor=IBM" })
public class GrpcServiceConfigImpl implements GrpcServiceConfig, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(GrpcServiceConfigImpl.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    /**
     * Reference to the ApplicationRecycleCoordinator which will be used to restart
     * apps on a config update
     */
    @Reference(name = "appRecycleService")
    private ApplicationRecycleCoordinator appRecycleSvcRef;

    /**
     * J2EE of applications providing grpc services
     */
    private final static Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final HashSet<String> propertiesToRemove = new HashSet<>();

    static {
        // this is stuff the framework always adds, we don't need it so we'll filter it
        // out.
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
        propertiesToRemove.add(GrpcConfigConstants.TARGET_PROP);
    }

    /**
     * given the map of properties, remove ones we don't care about, and translate
     * some others. If it's not one we're familiar with, transfer it unaltered
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

            if (debug) {
                Tr.debug(tc, "key: " + key + " value: " + props.get(key));
            }
            // skip stuff we don't care about
            if (propertiesToRemove.contains(key)) {
                continue;
            }
            if (key.compareTo(GrpcConfigConstants.MAX_INBOUND_MSG_SIZE_PROP) == 0) {
                if (!GrpcServiceConfigValidation.validateMaxInboundMessageSize(props.get(key).toString()))
                    continue;
            }
            filteredProps.put(key, props.get(key).toString());

        }
        return filteredProps;
    }

    /**
     * find the serviceName parameter which we will key off of
     *
     * @param props
     * @return value of serviceName param within props, or null if no serviceName
     *         param
     */
    private String getServiceName(Map<String, Object> props) {
        if (props == null)
            return null;
        if (props.keySet().contains(GrpcConfigConstants.TARGET_PROP)) {
            return (props.get(GrpcConfigConstants.TARGET_PROP).toString());
        } else {
            return null;
        }
    }

    /**
     * Invoked when <grpc/> element is first processed; the new configuration
     * is processed and added to the GrpcServiceConfigHolder.  All previously
     * initialized gRPC apps will be restarted to propagate the new config.
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (properties == null)
            return;
        String serviceName = getServiceName(properties);
        if (serviceName == null) {
            return;
        }
        GrpcServiceConfigHolder.addConfig(this.toString(), serviceName, filterProps(properties));
        recycleDependentApps();
    }

    /**
     * Invoked when <grpcService/> element is updated. This will: 1. remove the
     * previous configuration assigned in GrpcServiceConfigHolder 2. process the new
     * configuration 3. add the new config to GrpcServiceConfigHolder 4. restart all
     * active grpc service applications
     */
    @Modified
    protected void modified(Map<String, Object> properties) {
        if (properties == null) {
            return;
        }
        GrpcServiceConfigHolder.removeConfig(this.toString());
        String serviceName = getServiceName(properties);
        if (serviceName == null) {
            return;
        }
        GrpcServiceConfigHolder.addConfig(this.toString(), serviceName, filterProps(properties));
        recycleDependentApps();
    }

    /**
     * Invoked when <grpc/> element is first removed; All previously
     * initialized gRPC apps will be restarted to propagate the config
     * removal.
     */
    @Deactivate
    protected void deactivate() {
        GrpcServiceConfigHolder.removeConfig(this.toString());
        recycleDependentApps();
    }

    /**
     * Restart any previously initialized apps that provide gRPC services
     */
    private void recycleDependentApps() {
        Set<String> members = getDependentApplications();
        if (!members.isEmpty() && !FrameworkState.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "recycling applications: " + members);
            }
            appRecycleSvcRef.recycleApplications(members);
        }
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    /**
     * Get the J2EE names for apps that provide grpc services
     */
    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>();
        members.addAll(applications);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "getDependentApplications: " + members);
        }
        return members;
    }

    public static void addApplication(String name) {
        applications.add(name);
    }

    public static void removeApplication(String name) {
        applications.remove(name);
    }
}
