/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.plugins.server.servlet;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Application;

import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.config.Configuration;
import org.jboss.resteasy.spi.config.ConfigurationFactory;
import org.jboss.resteasy.util.HttpHeaderNames;

/**
 * Create a deployment from String-based configuration data
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class ConfigurationBootstrap implements ResteasyConfiguration {
    private ResteasyDeployment deployment = new ResteasyDeploymentImpl();
    private final Configuration config = ConfigurationFactory.getInstance().getConfiguration(this);

    public ResteasyDeployment createDeployment() {
        String deploymentSensitive = getParameter("resteasy.use.deployment.sensitive.factory");
        if (deploymentSensitive != null)
            deployment.setDeploymentSensitiveFactoryEnabled(Boolean.valueOf(deploymentSensitive.trim()));
        else
            deployment.setDeploymentSensitiveFactoryEnabled(true);

        String async = getParameter("resteasy.async.job.service.enabled");
        if (async != null)
            deployment.setAsyncJobServiceEnabled(Boolean.valueOf(async.trim()));
        if (deployment.isAsyncJobServiceEnabled()) {
            String maxJobResults = getParameter("resteasy.async.job.service.max.job.results");
            if (maxJobResults != null) {
                int maxJobs = Integer.valueOf(maxJobResults);
                deployment.setAsyncJobServiceMaxJobResults(maxJobs);
            }
            String maxWaitStr = getParameter("resteasy.async.job.service.max.wait");
            if (maxWaitStr != null) {
                long maxWait = Long.valueOf(maxWaitStr);
                deployment.setAsyncJobServiceMaxWait(maxWait);
            }
            String threadPool = getParameter("resteasy.async.job.service.thread.pool.size");
            if (threadPool != null) {
                int threadPoolSize = Integer.valueOf(threadPool);
                deployment.setAsyncJobServiceThreadPoolSize(threadPoolSize);
            }
            String basePath = getParameter("resteasy.async.job.service.base.path");
            if (basePath != null) {
                deployment.setAsyncJobServiceBasePath(basePath);
            }
        }
        String applicationConfig = getParameter(Application.class.getName());
        if (applicationConfig == null) {
            // stupid spec doesn't use FQN of Application class name
            applicationConfig = getParameter("jakarta.ws.rs.Application");
        } else {
            LogMessages.LOGGER.useOfApplicationClass(Application.class.getName());
        }

        String providers = getParameter(ResteasyContextParameters.RESTEASY_PROVIDERS);

        if (providers != null && !"".equals(providers.trim())) {
            String[] p = providers.split(",");
            for (String pr : p)
                deployment.getProviderClasses().add(pr.trim());
        }
        final String disableProviders = getParameter(ResteasyContextParameters.RESTEASY_DISABLE_PROVIDERS);
        if (disableProviders != null && !disableProviders.isBlank()) {
            deployment.addDisabledProviderClasses(
                    Stream.of(disableProviders.split(",")).map(String::trim).collect(Collectors.toSet()));
        }

        String resteasySecurity = getParameter(ResteasyContextParameters.RESTEASY_ROLE_BASED_SECURITY);
        if (resteasySecurity != null) {
            boolean useResteasySecurity = parseBooleanParam(ResteasyContextParameters.RESTEASY_ROLE_BASED_SECURITY,
                    resteasySecurity);
            deployment.setSecurityEnabled(useResteasySecurity);
        }

        String builtin = getParameter(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS);
        if (builtin != null) {
            boolean useBuiltin = parseBooleanParam(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS, builtin);
            deployment.setRegisterBuiltin(useBuiltin);
        }

        String sProviders = getParameter(ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS);
        if (sProviders != null) {
            LogMessages.LOGGER.noLongerSupported(ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS);
        }
        String scanAll = getParameter(ResteasyContextParameters.RESTEASY_SCAN);
        if (scanAll != null) {
            LogMessages.LOGGER.noLongerSupported(ResteasyContextParameters.RESTEASY_SCAN);
        }
        String sResources = getParameter(ResteasyContextParameters.RESTEASY_SCAN_RESOURCES);
        if (sResources != null) {
            LogMessages.LOGGER.noLongerSupported(ResteasyContextParameters.RESTEASY_SCAN_RESOURCES);
        }

        // Check to see if scanning is being done by deployer (i.e. JBoss App Server)
        String sScannedByDeployer = getParameter(ResteasyContextParameters.RESTEASY_SCANNED_BY_DEPLOYER);
        if (sScannedByDeployer != null) {

        }

        String scannedProviders = getParameter(ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS);

        if (scannedProviders != null) {
            String[] p = scannedProviders.split(",");
            for (String pr : p)
                deployment.getScannedProviderClasses().add(pr.trim());
        }

        String scannedResources = getParameter(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES);

        if (scannedResources != null) {
            String[] p = scannedResources.split(",");
            for (String pr : p)
                deployment.getScannedResourceClasses().add(pr.trim());
        }

        String scannedJndi = getParameter(ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES);

        if (scannedJndi != null) {
            processScannedJndiComponentResources(scannedJndi);
        }

        String scannedResourceClassesWithBuilder = getParameter(
                ResteasyContextParameters.RESTEASY_SCANNED_RESOURCE_CLASSES_WITH_BUILDER);
        if (scannedResourceClassesWithBuilder != null) {
            processScannedResourceClassesWithBuilder(scannedResourceClassesWithBuilder);
        }

        String jndiResources = getParameter(ResteasyContextParameters.RESTEASY_JNDI_RESOURCES);
        if (jndiResources != null && !"".equals(jndiResources.trim())) {
            processJndiResources(jndiResources);
        }

        String jndiComponentResources = getParameter(ResteasyContextParameters.RESTEASY_JNDI_COMPONENT_RESOURCES);
        if (jndiComponentResources != null) {
            processJndiComponentResources(jndiComponentResources);
        }

        String resources = getParameter(ResteasyContextParameters.RESTEASY_RESOURCES);
        if (resources != null && !"".equals(resources.trim())) {
            processResources(resources);
        }

        String unwrapped = getParameter(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS);
        if (unwrapped != null) {
            processUnwrapped(unwrapped);
        }

        String paramMapping = getParameter(ResteasyContextParameters.RESTEASY_MEDIA_TYPE_PARAM_MAPPING);
        if (paramMapping != null) {
            paramMapping = paramMapping.trim();

            if (paramMapping.length() > 0) {
                deployment.setMediaTypeParamMapping(paramMapping);
            } else {
                deployment.setMediaTypeParamMapping(HttpHeaderNames.ACCEPT.toLowerCase());
            }
        }

        String contextObjects = getParameter(ResteasyContextParameters.RESTEASY_CONTEXT_OBJECTS);
        if (contextObjects != null) {
            Map<String, String> map = parseMap(contextObjects);
            deployment.setConstructedDefaultContextObjects(map);
        }

        String mimeExtentions = getParameter(ResteasyContextParameters.RESTEASY_MEDIA_TYPE_MAPPINGS);
        if (mimeExtentions != null) {
            Map<String, String> map = parseMap(mimeExtentions);
            deployment.setMediaTypeMappings(map);
        }

        String languageExtensions = getParameter(ResteasyContextParameters.RESTEASY_LANGUAGE_MAPPINGS);
        if (languageExtensions != null) {
            Map<String, String> map = parseMap(languageExtensions);
            deployment.setLanguageExtensions(map);
        }

        String useContainerFormParams = getParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS);
        if (useContainerFormParams != null) {
            boolean useContainer = parseBooleanParam(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS,
                    useContainerFormParams);
            deployment.setUseContainerFormParams(useContainer);
        }

        String widerMatching = getParameter(ResteasyContextParameters.RESTEASY_WIDER_REQUEST_MATCHING);
        if (widerMatching != null) {
            boolean wider = parseBooleanParam(ResteasyContextParameters.RESTEASY_WIDER_REQUEST_MATCHING, widerMatching);
            deployment.setWiderRequestMatching(wider);
        }

        String addCharset = getParameter(ResteasyContextParameters.RESTEASY_ADD_CHARSET);
        if (addCharset != null) {
            boolean add = parseBooleanParam(ResteasyContextParameters.RESTEASY_ADD_CHARSET, addCharset);
            deployment.setAddCharset(add);
        }

        String disableHtmlSanitizer = getParameter(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER);
        if (disableHtmlSanitizer != null) {
            boolean b = parseBooleanParam(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER, disableHtmlSanitizer);
            deployment.setProperty(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER, b);
        }

      // Liberty Change: TODO: revert this back when we sort out the config via web fragments.
      //String injectorFactoryClass = "org.jboss.resteasy.cdi.CdiInjectorFactory"; // getParameter("resteasy.injector.factory");
      String injectorFactoryClass = "io.openliberty.org.jboss.resteasy.common.cdi.LibertyFallbackInjectorFactory";
      if (injectorFactoryClass != null) {
         deployment.setInjectorFactoryClass(injectorFactoryClass);
      }

        if (applicationConfig != null)
            deployment.setApplicationClass(applicationConfig);
        deployment.getDefaultContextObjects().put(ResteasyConfiguration.class, this);

        String statisticsEnabled = getParameter(ResteasyContextParameters.RESTEASY_STATISTICS_ENABLED);
        if (statisticsEnabled != null) {
            deployment.setStatisticsEnabled(Boolean.valueOf(statisticsEnabled));
        }

        String proxiesImplAllInterfaces = getParameter(ResteasyContextParameters.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES);
        if (proxiesImplAllInterfaces != null) {
            boolean b = parseBooleanParam(ResteasyContextParameters.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES,
                    proxiesImplAllInterfaces);
            deployment.setProperty(ResteasyContextParameters.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES, b);
        } else {
            deployment.setProperty(ResteasyContextParameters.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES, false);
        }

        return deployment;
    }

    protected boolean parseBooleanParam(String key, String value) {
        value = value.trim().toLowerCase();
        if (value.equals("true") || value.equals("1")) {
            return true;
        } else if (value.equals("false") || value.equals("0")) {
            return false;
        } else {
            throw new RuntimeException(Messages.MESSAGES.keyCouldNotBeParsed(key));
        }
    }

    protected Map<String, String> parseMap(String map) {
        Map<String, String> parsed = new HashMap<String, String>();
        String[] entries = map.trim().split(",");
        for (String entry : entries) {
            String[] split = entry.trim().split(":");
            parsed.put(split[0].trim(), split[1].trim());

        }
        return parsed;
    }

    protected void processJndiResources(String jndiResources) {
        String[] resources = jndiResources.trim().split(",");
        for (String resource : resources) {
            deployment.getJndiResources().add(resource);
        }
    }

    protected void processJndiComponentResources(String jndiResources) {
        String[] resources = jndiResources.trim().split(",");
        for (String resource : resources) {
            deployment.getJndiComponentResources().add(resource);
        }
    }

    // this string should be in the form: builder_class1:resource_class1,resource_class2;builder_class2:resource_class3
    protected void processScannedResourceClassesWithBuilder(String scannedResourceClassesWithBuilder) {
        String[] parts = scannedResourceClassesWithBuilder.trim().split(";");
        for (String part : parts) {
            int separationIndex = part.indexOf(":");
            if (separationIndex > 0 && (separationIndex < part.length() - 1)) {
                String resourceBuilder = part.substring(0, separationIndex);
                String resourceClassesStr = part.substring(separationIndex + 1);
                final String[] newResourceClasses = resourceClassesStr.trim().split(",");
                if (newResourceClasses.length == 0) {
                    continue;
                }
                if (deployment.getScannedResourceClassesWithBuilder().get(resourceBuilder) == null) {
                    deployment.getScannedResourceClassesWithBuilder().put(resourceBuilder, Arrays.asList(newResourceClasses));
                } else {
                    deployment.getScannedResourceClassesWithBuilder().get(resourceBuilder)
                            .addAll(Arrays.asList(newResourceClasses));
                }
            }
        }
    }

    protected void processScannedJndiComponentResources(String jndiResources) {
        String[] resources = jndiResources.trim().split(",");
        for (String resource : resources) {
            deployment.getScannedJndiComponentResources().add(resource);
        }
    }

    protected void processResources(String list) {
        String[] resources = list.trim().split(",");
        for (String resource : resources) {
            deployment.getResourceClasses().add(resource);
        }
    }

    protected void processUnwrapped(String list) {
        String[] resources = list.trim().split(",");
        for (String resource : resources) {
            deployment.getUnwrappedExceptions().add(resource);
        }
    }

    public String getParameter(String name) {
        String propName = null;
        if (System.getSecurityManager() == null) {
            propName = config.getOptionalValue(name, String.class).orElse(null);

        } else {

            try {
                propName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return config.getOptionalValue(name, String.class).orElse(null);
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae);
            }
        }
        return propName;
    }

}
