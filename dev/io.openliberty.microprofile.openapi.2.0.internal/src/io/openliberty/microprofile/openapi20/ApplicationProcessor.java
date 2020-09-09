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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.http.VirtualHost;

import io.openliberty.microprofile.openapi20.utils.CloudUtils;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.IndexUtils;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.utils.ModuleUtils;
import io.openliberty.microprofile.openapi20.utils.OpenAPIUtils;
import io.openliberty.microprofile.openapi20.utils.ProxySupportUtil;
import io.openliberty.microprofile.openapi20.utils.ServerInfo;
import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.validation.OASValidator;
import io.openliberty.microprofile.openapi20.validation.ValidatorUtils;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * The ApplicationProcessor class processes applications that are deployed to the OpenLiberty instance in order to
 * generate OpenAPI documents. However, the MP OpenAPI functionality in OpenLiberty only supports generating an OpenAPI
 * document for a single application at a time so, if multiple applications are deployed to the OpenLiberty instance,
 * an OpenAPI document will only be generated for the first application that is processed. Also, if an enterprise
 * application (EAR/EBA) is deployed that contains multiple web modules, an OpenAPI document will only be generated for
 * the first Web Module that generates an OpenAPI document. 
 */
@Component(service = { ApplicationProcessor.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class ApplicationProcessor {

    private static final TraceComponent tc = Tr.register(ApplicationProcessor.class);

    private static ApplicationProcessor instance = null;
    private OpenAPI document = null;
    private static ApplicationInfo currentApp = null;
    private static Map<String, ApplicationInfo> applications = new HashMap<>();
    private final ServerInfo serverInfo = new ServerInfo();

    /**
     * The getInstance method returns the singleton instance of the ApplicationProcessor
     * 
     * @return ApplicationProcessor
     *             The singleton instance
     */
    public static ApplicationProcessor getInstance() {
        return instance;
    }

    /**
     * The activate method invoked by the Service Component Runtime.
     * 
     * @param cc
     *            The ComponentContext for this component
     */
    public void activate(ComponentContext cc) {
        instance = this;
        this.document = OpenAPIUtils.createBaseOpenAPIDocument();
        if (currentApp != null) {
            processApplication(currentApp);
        }
    }

    /**
     * The addApplication method is invoked by the {@link ApplicationListener} when it is notified that an application
     * is starting. 
     * 
     * @param appInfo
     *           The ApplicationInfo for the application that is starting.
     */
    public void addApplication(ApplicationInfo appInfo) {
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Adding application started: appInfo=" + appInfo);
        }
        synchronized (this.document) {
            if (currentApp == null) {
                processApplication(appInfo);
            } else {
                applications.put(appInfo.getName(), appInfo);
            }
        }
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Adding application ended: appInfo=" + appInfo);
        }
    }

    /**
     * The removeApplication method is invoked by the {@link ApplicationListener} when it is notified that an
     * application is stopping.
     * 
     * @param appInfo
     *           The ApplicationInfo for the application that is stopping.
     */
    public void removeApplication(ApplicationInfo appInfo) {
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Removing application started: appInfo=" + appInfo);
        }
        synchronized (this.document) {
            if (currentApp != null && currentApp.getName().equals(appInfo.getName())) {
                currentApp = null;
                this.serverInfo.setApplicationPath(null);
                this.serverInfo.setIsUserServer(false);
                this.document = OpenAPIUtils.createBaseOpenAPIDocument();
                Iterator<java.util.Map.Entry<String, ApplicationInfo>> iterator = applications.entrySet().iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry<String, ApplicationInfo> entry = iterator.next();
                    processApplication(entry.getValue());
                    iterator.remove();
                    if (currentApp != null) {
                        break;
                    }
                }
            } else {
                applications.remove(appInfo.getName());
            }
        }
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Removing application ended: appInfo=" + appInfo);
        }
    }

    /**
     * The getOpenAPIDocument method returns the OpenAPI document for the current application in the specified format
     * (JSON or YAML).
     * 
     * @param request
     *            The HttpServletRequest object
     * @param format
     *            The format desired format of the document
     * @return
     */
    @FFDCIgnore(IOException.class)
    public String getOpenAPIDocument(HttpServletRequest request, Format format) {
        String oasResult = null;
        synchronized (this.document) {
            ServerInfo reqServerInfo = null;
            synchronized (serverInfo) {
                reqServerInfo = new ServerInfo(serverInfo);
            }
            ProxySupportUtil.processRequest(request, reqServerInfo);
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Request server info : " + reqServerInfo);
            }
            reqServerInfo.updateOpenAPIWithServers(this.document);
            try {
                oasResult = OpenApiSerializer.serialize(this.document, format);
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Failed to serialize OpenAPI docuemnt: " + e.getMessage());
                }
            }
        }
        if (LoggingUtils.isDebugEnabled(tc)) {
            Tr.debug(tc, "Serialized document=" + oasResult);
        }
        return oasResult;
    }

    /**
     * The processApplication method processes applications that are added to the OpenLiberty instance. These 
     * 
     * @param appInfo
     *            The ApplicationInfo for the application to be processed.
     */
    @FFDCIgnore(UnableToAdaptException.class)
    private void processApplication(final ApplicationInfo appInfo) {
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Processing application started: appInfo=" + appInfo);
        }
        synchronized (this.document) {
            
            // Make sure that we have valid application info
            if (appInfo != null) {
                // Get the container for the application
                Container appContainer = appInfo.getContainer();
                if (appContainer != null) {
                    
                    // Check for app classes, if it is not there then the app manager is not in control of this app
                    try {
                        NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
                        ApplicationClassesContainerInfo applicationClassesContainerInfo =
                            (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
                        if (applicationClassesContainerInfo != null) {
                            WebModuleInfo moduleInfo = null;
                            
                            // Check to see if the deployed application is an EAR/EBA
                            if (appInfo instanceof EARApplicationInfo) {
                                /*
                                 * Iterate over the entries in the application. An Enterprise Application can contain
                                 * various types of module, including Web modules. We need to attempt to retrieve the
                                 * WebModuleInfo for each entry and, if there is WebModuleInfo, process it. If this
                                 * results in an OpenAPI document being generated, we do not process any more entries
                                 * because we only generate a single OpenAPI document... even if the application
                                 * contains multiple web modules.
                                 */
                                for (Entry entry : appContainer) {
                                    try {
                                        // Attempt to adapt the entry to a container
                                        Container container = entry.adapt(Container.class);
                                        if (container != null) {
                                            
                                            // Attempt to retrieve WebModuleInfo for the container
                                            WebModuleInfo wmi = ModuleUtils.getWebModuleInfo(container);
                                            if (wmi != null) {
                                                
                                                // Process the web module
                                                OpenAPI openAPI = processWebModule(container, wmi);
                                                if (openAPI != null) {
                                                    
                                                    // OpenAPI document generated... finish processing and exit the loop
                                                    currentApp = appInfo;
                                                    this.document = openAPI;
                                                    handleApplicationPath(openAPI, wmi.getContextRoot());
                                                    handleUserServer(openAPI);
                                                    Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, wmi.getApplicationInfo().getDeploymentName());
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (UnableToAdaptException e) {
                                        // Unable to adapt... log it and move on
                                        if (LoggingUtils.isEventEnabled(tc)) {
                                            Tr.event(tc, "Failed to adapt entry: entry=" + entry + " : \n" + e.getMessage());
                                        }
                                    }
                                } // FOR
                            } else {
                                // Not an Enterprise Application... attempt to get the WebModuleInfo
                                moduleInfo = ModuleUtils.getWebModuleInfo(appContainer);
                                
                                // Make sure that we have a valid web module.  If we do, process it.
                                if (moduleInfo != null) {
                                    OpenAPI openAPI = processWebModule(appContainer, moduleInfo);
                                    if (openAPI != null) {
                                        currentApp = appInfo;
                                        handleApplicationPath(openAPI, moduleInfo.getContextRoot());
                                        handleUserServer(openAPI);
                                        this.document = openAPI;
                                        Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, moduleInfo.getApplicationInfo().getDeploymentName());
                                    }
                                    
                                    if (LoggingUtils.isEventEnabled(tc)) {
                                        Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo);
                                    }
                                } else {
                                    if (LoggingUtils.isEventEnabled(tc)) {
                                        Tr.event(tc, "Application Processor: Processing application ended: moduleInfo=null : appInfo=" + appInfo);
                                    }
                                }
                            }
                        } else {
                            // No application classes... the app manager is not in control of this ap
                            if (LoggingUtils.isEventEnabled(tc)) {
                                Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo + ", applicationClassesContainerInfo=null");
                            }
                        }
                    } catch (UnableToAdaptException e) {
                        if (LoggingUtils.isEventEnabled(tc)) {
                            Tr.event(tc, "Failed to adapt NonPersistentCache: container=" + appContainer + " : \n" + e.getMessage());
                        }
                    }
                } else {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo + ", appContainer=null");
                    }
                }
            } else {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Application Processor: Processing application ended: appInfo=null");
                }
            }
            
        }
    }

    /**
     * The processWebModule method attempts to generate an OpenAPI document for the specified web module using the
     * SmallRye implementation.
     * 
     * @param appContainer
     *            The Container for the web module
     * @param moduleInfo
     *            The WebModuleInfo object for the web module
     * @return OpenAPI
     *            The OpenAPI document generated for the web module, or null if the web module is not an OAS
     *            applciation.
     */
    private OpenAPI processWebModule(final Container appContainer, final WebModuleInfo moduleInfo) {
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "WebModule: Processing started : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
        }
        ClassLoader appClassloader = moduleInfo.getClassLoader();
        
        OpenAPI newDocument = null;
        
        // Read and process the MicroProfile config. Try with resources will close the ConfigProcessor when done.
        try (ConfigProcessor configProcessor = new ConfigProcessor(appClassloader)) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Retrieved configuration values : " + configProcessor);
            }
            
            try {
                OpenApiConfig config = configProcessor.getOpenAPIConfig();
                OpenApiDocument.INSTANCE.reset();
                OpenApiDocument.INSTANCE.config(config);
                OpenApiStaticFile staticFile = StaticFileProcessor.getOpenAPIFile(appContainer);
                OpenApiDocument.INSTANCE.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
                OpenApiDocument.INSTANCE.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, IndexUtils.getIndexView(moduleInfo, config)));
                OpenApiDocument.INSTANCE.modelFromReader(OpenApiProcessor.modelFromReader(config, appClassloader));
                OpenApiDocument.INSTANCE.filter(OpenApiProcessor.getFilter(config, appClassloader));
                OpenApiDocument.INSTANCE.initialize();
                
                newDocument =  OpenApiDocument.INSTANCE.get();
                
                /*
                 * We need to determine whether the scanned application is an OAS application at all. In order to do
                 * this we can check two things:
                 * 
                 *     1) Whether a static file was found in the application.
                 *     2) Whether the generated OpenAPI model object is a just the default generated by the SmallRye
                 *        implementation.
                 */
                if (staticFile == null && OpenAPIUtils.isDefaultOpenApiModel(newDocument)) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Default Open API document generated");
                    }
                    newDocument = null;
                }

                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Generated document: " + getSerializedJsonDocument(newDocument));
                }
            } catch (Exception e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    final String message = String.format("Failed to process application %s: %s", moduleInfo.getApplicationInfo().getDeploymentName(), e.getMessage());
                    Tr.event(this, tc, "Failed to process application: " + message);
                }
                Tr.error(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSING_ERROR, moduleInfo.getApplicationInfo().getDeploymentName());
            }

            if (newDocument == null) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "WebModule: Processing ended : Not an OAS application : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
                }
                return null;
            } else if (newDocument.getInfo() == null) {
                newDocument.setInfo(new InfoImpl().title(Constants.DEFAULT_OPENAPI_DOC_TITLE)
                    .version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
            }

            // Validate the document if the validation property has been enabled.
            final boolean validating = configProcessor.isValidating();
            if (validating) {
                try {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Validate document");
                    }
                    validateDocument(newDocument);
                } catch (Throwable e) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Failed to call OASValidator: " + e.getMessage());
                    }
                }
            }
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "WebModule: Processing ended : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
        }
        return newDocument;
    }

    /**
     * The validateDocument method validates the generated OpenAPI model and logs any warnings/errors.
     * 
     * @param document
     *            The OpenAPI document (model) to validate
     */
    @Trivial
    private void validateDocument(OpenAPI document) {
        final OASValidator validator = new OASValidator();
        final OASValidationResult result = validator.validate(document);
        final StringBuilder sbError = new StringBuilder();
        final StringBuilder sbWarnings = new StringBuilder();
        if (result.hasEvents()) {
            result.getEvents().stream().forEach(v -> {
                final String message = ValidatorUtils.formatMessage(ValidationMessageConstants.VALIDATION_MESSAGE, v.message, v.location);
                if (v.severity == Severity.ERROR) {
                    sbError.append("\n - " + message);
                } else if (v.severity == Severity.WARNING) {
                    sbWarnings.append("\n - " + message);
                }
            });

            String errors = sbError.toString();
            if (!errors.isEmpty()) {
                Tr.error(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_ERROR, errors + "\n");
            }

            String warnings = sbWarnings.toString();
            if (!warnings.isEmpty()) {
                Tr.warning(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_WARNING, warnings + "\n");
            }
        }
    }

    /**
     * The handleApplicationPath method checks whether the first path item starts with the specified context root. If it
     * does not, it adds the context root to the server info so that it will be injected when the OpenAPI document is
     * serialized.
     * 
     * @param openAPI
     *            The OpenAPI model
     * @param contextRoot
     *            The context root to check
     */
    @Trivial
    private void handleApplicationPath(final OpenAPI openAPI, String contextRoot) {
        
        // Make sure that we have a valid OpenAPI model
        if (openAPI != null) {
            
            // Now check if the model contains any paths and, if so, whether the first one starts with the context root
            Paths paths = openAPI.getPaths();
            if (  paths == null
               || paths.getPathItems() == null
               || paths.getPathItems().isEmpty()
               || !paths.getPathItems().keySet().iterator().next().startsWith(contextRoot)
               ) {
                // Path doesn't start with context root, so add it to the server info
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Add context root: " + contextRoot);
                }
                serverInfo.setApplicationPath(contextRoot);
            } else {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Path already starts with context root: " + contextRoot);
                }
            }
        }
    }

    /**
     * The handleUserServer method checks whether the specified OpenAPI model defines any servers. If it does, it
     * updates the server info to indicate this.
     * 
     * @param openAPI
     *            The OpenAPI model
     */
    @Trivial
    private void handleUserServer(final OpenAPI openapi) {
        if (openapi != null && openapi.getServers() != null && openapi.getServers().size() > 0) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "User application specifies server");
            }
            serverInfo.setIsUserServer(true);
        }
    }


    /**
     * The getSerializedJsonDocument method is a convenience method used to generate an OpenAPI document from the
     * specified model in order to write it to logs. 
     * 
     * @param openapi
     *            The OpenAPI model
     * @return String
     *            The generated OpenAPI document in JSON format
     */
    @Trivial
    @FFDCIgnore(IOException.class)
    private String getSerializedJsonDocument(final OpenAPI openapi) {
        // Create the variable to return
        String oasResult = null;
        
        // Make sure that we have a valid document
        if (openapi != null) {
            try {
                oasResult = OpenApiSerializer.serialize(openapi, Format.JSON);
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Failed to serialize OpenAPI docuemnt: " + e.getMessage());
                }
            }
        }

        return oasResult;
    }

    @Reference(service = VirtualHost.class, target = "(&(enabled=true)(id=default_host)(|(aliases=*)(httpsAlias=*)))", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateOpenAPIServer(vhost, props);
    }

    protected void updatedVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        updateOpenAPIServer(vhost, props);
    }

    /**
     * The updateOpenAPIServer method is invoked whenever the virtual host for the server is modified.  It updates the
     * server info to reflect the changes that have been made.
     * 
     * @param vhost
     *            The VirtualHost that has been set/updated
     * @param props
     *            The properties that have been set/modified
     */
    private void updateOpenAPIServer(VirtualHost vhost, Map<String, Object> props) {

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

        synchronized (this.serverInfo) {
            serverInfo.setHttpPort(vhost.getHttpPort(alias));
            serverInfo.setHttpsPort(vhost.getSecureHttpPort(alias));
            serverInfo.setHost(vhost.getHostName(alias));
            final String vcapHost = CloudUtils.getVCAPHost();
            if (vcapHost != null) {
                serverInfo.setHost(vcapHost);
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Changed host using VCAP_APPLICATION.  New value: " + serverInfo.getHost());
                }
            }
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Updated server information: " + serverInfo);
        }
    }
}
