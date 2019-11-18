/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import static com.ibm.ws.openapi31.OpenAPIUtils.getConfigPropAsSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.core.util.Yaml;
import com.ibm.ws.microprofile.openapi.impl.parser.ObjectMapperFactory;
import com.ibm.ws.openapi31.merge.OASMergeService;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.openapi31.OASProvider;

/**
 *
 */
@Component(name = "OpenAPIAggregator", service = { OpenAPIAggregator.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class OpenAPIAggregator {

    private static final TraceComponent tc = Tr.register(OpenAPIAggregator.class,com.ibm.ws.openapi31.TraceConstants.TRACE_GROUP, com.ibm.ws.openapi31.TraceConstants.TRACE_BUNDLE_CORE);

    /**
     * Configuration property for exclusion of specific context roots from the public and private endpoints.
     * The format of this value is a comma separated list of strings.
     */
    private static final String EXCLUDE_CONTEXT_ROOTS = OASConfig.EXTENSIONS_PREFIX + "liberty.exclude.context.roots";

    private static final String QUERY_ROOT = "root";
    private final OASMergeService mergeService = new OASMergeService();

    private boolean isActivated = false;
    private final ConcurrentHashMap<OASProvider, OASProviderWrapper> oasProviderWrappers = new ConcurrentHashMap<>();

    private final ServerInfo serverInfo = new ServerInfo(null, -1, -1);
    private OpenAPI customizationOpenAPI = null;
    private final Set<String> excludedContextRoots;

    public OpenAPIAggregator() {
        final Config config = ConfigProvider.getConfig(OpenAPIAggregator.class.getClassLoader());
        excludedContextRoots = getConfigPropAsSet(config, EXCLUDE_CONTEXT_ROOTS);
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        isActivated = true;
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Activating OpenAPIAggregator", properties);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        isActivated = false;
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Deactivating OpenAPIAggregator, reason=" + reason);
        }
    }

    @Reference(service = OASProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setProvider(OASProvider provider) {
        // Skip providers with context roots that have been excluded.
        if (!excludedContextRoots.contains(provider.getContextRoot())) {
            OASProviderWrapper wrapper = new OASProviderWrapper(provider);
            if (wrapper.getOpenAPI() != null) {
                wrapper.validate();
                synchronized (mergeService) {
                    addOpenAPI(wrapper);
                }
            }
            oasProviderWrappers.put(provider, wrapper);
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Finished processing the provider: " + provider);
        }
    }

    protected void unsetProvider(OASProvider provider) {
        OASProviderWrapper wrapper = oasProviderWrappers.remove(provider);

        if (wrapper == null || wrapper.getOpenAPI() == null)
            return;
        synchronized (mergeService) {
            removeOpenAPI(wrapper);
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Finished removing the provider: " + provider);
        }
    }

    private boolean addOpenAPI(OASProviderWrapper wrapper) {
        try {
            mergeService.addAPIProvider(wrapper);
        } catch (Exception e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Exception occurred while merging the Open API from the provider: " + wrapper.getOpenAPIProvider() + e);
            }
        }
        return true;
    }

    private void removeOpenAPI(OASProviderWrapper wrapper) {
        try {
            mergeService.removeAPIProvider(wrapper);
        } catch (Exception e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Exception occured while removing the Open API from the provider: " + wrapper.getOpenAPIProvider() + e);
            }
        }
    }

    private OpenAPI getMergedDocumentation(OASProviderFilterBuilder filterBuilder) {
        final OpenAPI openAPI = createOpenAPI();
        synchronized (mergeService) {
            final Stream<OASProviderWrapper> s = oasProviderWrappers.values().stream().filter(filterBuilder.getPredicate());
            if (filterBuilder.getFindFirst()) {
                final OASProviderWrapper w = s.findFirst().orElse(null);
                if (w != null && w.getOpenAPI() != null) {
                    mergeService.mergeOpenAPI(openAPI, w.getOpenAPI());
                }
            } else {
                s.forEach((v) -> {
                    if (v.getOpenAPI() != null)
                        mergeService.mergeOpenAPI(openAPI, v.getOpenAPI());
                });
            }
        }
        return openAPI;
    }

    public boolean getPublicDocumentation(HttpServletRequest request, boolean compact, boolean yaml, HttpServletResponse response) {
        return getDocumentation(request, compact, yaml, response, new OASProviderFilterBuilder().addPredicate(OASProviderFilterBuilder.publicFilter()));
    }

    public boolean getDocumentation(HttpServletRequest request, boolean compact, boolean yaml, HttpServletResponse response) {
        return getDocumentation(request, compact, yaml, response, new OASProviderFilterBuilder());
    }

    public boolean getDocumentation(HttpServletRequest request, boolean compact, boolean yaml, HttpServletResponse response, OASProviderFilterBuilder filterBuilder) {
        if (!isActivated) {
            return false;
        }
        processRequest(request, filterBuilder);
        String serializedDocument = null;
        final OpenAPI openAPI = getMergedDocumentation(filterBuilder);

        ServerInfo reqServerInfo = null;
        synchronized (serverInfo) {
            reqServerInfo = new ServerInfo(serverInfo);
        }
        ProxySupportUtil.processRequest(request, reqServerInfo);
        reqServerInfo.updateServers(openAPI);

        try {
            if (yaml) {
                serializedDocument = Yaml.mapper().writeValueAsString(openAPI);
            } else if (!yaml && !compact) {
                serializedDocument = Json.pretty().writeValueAsString(openAPI);
            } else if (!yaml && compact) {
                serializedDocument = Json.mapper().writeValueAsString(openAPI);
            }
        } catch (JsonProcessingException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Exception occurred while trying to convert OpenAPI object to String. \n exception: " + e);
            }
        }

        if (OpenAPIUtils.isDumpEnabled(tc)) {
            Tr.dump(tc, "Returning document into the response: compact? " + compact + " yaml?" + yaml + " \n" + serializedDocument);
        }

        try {
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(serializedDocument);
            return true;
        } catch (IOException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Exception occured while writing to document to the response: " + e.getMessage());
            }
        }
        return true;
    }

    private void processRequest(HttpServletRequest request, OASProviderFilterBuilder filterBuilder) {
        final String contextRoot = request.getParameter(QUERY_ROOT);
        if (contextRoot != null && !contextRoot.isEmpty()) {
            filterBuilder.addPredicate(OASProviderFilterBuilder.contextRootFilter(contextRoot));
        }
    }

    @Reference(service = VirtualHost.class, target = "(&(enabled=true)(id=default_host)(|(aliases=*)(httpsAlias=*)))", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateOpenAPIServer(vhost, props);
    }

    protected void updatedVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateOpenAPIServer(vhost, props);
    }

    private void updateOpenAPIServer(VirtualHost vhost, Map<String, Object> props) {
        Object value = props.get("httpsAlias");
        if (value == null) {
            String[] aliases = (String[]) props.get("aliases");
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "httpsAlias is null. aliases : " + String.join(", ", aliases));
            }
            value = Arrays.stream(aliases).filter(a -> !a.endsWith(":-1")).findFirst().orElse(null);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Found non-secure alias: " + value);
            }
        }

        String alias = String.valueOf(value);
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Received new alias: " + alias);
        }

        // Updating the server info in a synchronized block to ensure that it is consistent when it is read.
        synchronized (serverInfo) {
            serverInfo.setHost(vhost.getHostName(alias));
            serverInfo.setHttpPort(vhost.getHttpPort(alias));
            serverInfo.setHttpsPort(vhost.getSecureHttpPort(alias));
            checkVCAPHost(serverInfo);
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Updated server information: " + serverInfo);
        }
    }

    /**
     * This method check the environment variable"VCAP_APPLICATION", which in Cloud Foundry (where Bluemix runs)
     * will be set to the actual host that is visible to the user. In that environment the VHost from Liberty
     * is private and not accessible externally.
     */
    @FFDCIgnore(Exception.class)
    private void checkVCAPHost(ServerInfo server) {
        String VCAP_APPLICATION = System.getenv("VCAP_APPLICATION");
        if (VCAP_APPLICATION != null) {
            try {
                JsonNode node = ObjectMapperFactory.createJson().readValue(VCAP_APPLICATION, JsonNode.class);
                ArrayNode uris = (ArrayNode) node.get("uris");
                if (uris != null && uris.size() > 0 && uris.get(0) != null) {
                    server.setHost(uris.get(0).textValue());
                }

                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Changed hostPort using VCAP_APPLICATION.  New value: " + server.getHost());
                }
            } catch (Exception e) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Exception while parsing VCAP_APPLICATION env: " + e.getMessage());
                }
            }
        }
    }

    public void setOpenAPICustomization(OpenAPI customOpenAPI) {
        customizationOpenAPI = customOpenAPI;
        if (customizationOpenAPI != null) {
            serverInfo.setCustomServers(customOpenAPI.getServers());
        } else {
            serverInfo.setCustomServers(null);
        }

    }

    private OpenAPI createOpenAPI() {
        synchronized (mergeService) {
            OpenAPI openAPI = OpenAPIUtils.createDefaultOpenAPI();

            if (customizationOpenAPI != null) {
                openAPI.setInfo(OpenAPIUtils.ensureValidInfo(customizationOpenAPI.getInfo()));
                openAPI.setSecurity(customizationOpenAPI.getSecurity());
                openAPI.setExternalDocs(customizationOpenAPI.getExternalDocs());

                if (customizationOpenAPI.getServers() != null && !customizationOpenAPI.getServers().isEmpty()) {
                    openAPI.setServers(customizationOpenAPI.getServers());
                } else {
                    if (serverInfo != null) {
                        serverInfo.updateServers(openAPI);
                    }
                }

                OpenAPIUtils.copyOpenAPIObjectExtensions(customizationOpenAPI, openAPI);
            }
            return openAPI;
        }
    }

}
