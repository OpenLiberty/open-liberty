/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import static com.ibm.ws.openapi31.OpenAPIUtils.getOptionalValue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.AnnotationScanner;
import com.ibm.ws.microprofile.openapi.ConfigProcessor;
import com.ibm.ws.microprofile.openapi.OpenAPIFilter;
import com.ibm.ws.microprofile.openapi.Reader;
import com.ibm.ws.microprofile.openapi.StaticFileProcessor;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.openapi31.OASProvider;

/**
 *
 */
public class OpenAPIWebProvider implements OASProvider {

    private static final TraceComponent tc = Tr.register(OpenAPIWebProvider.class);

    /**
     * Configuration property for customization of whether this OASProvider is visible from the public endpoint. The default value is true.
     */
    private static final String PUBLIC = OASConfig.EXTENSIONS_PREFIX + "liberty.public";
    private static final boolean PUBLIC_DEFAULT_VALUE = true;

    private String contextRoot;
    private final ComponentContext ccontext;
    private final Container container;
    private final ClassLoader classloader;
    private final OpenAPI document = null;
    private final boolean isPublic;

    private ServiceRegistration<OASProvider> serviceRegistration;

    private List<String> nonDefaultHosts;

    private static final Dictionary<String, String> PROPS = new Hashtable<String, String>();
    static {
        PROPS.put("service.vendor", "IBM");
    }

    public OpenAPIWebProvider(ComponentContext ccontext, Container container, ClassLoader classloader) {
        this.ccontext = ccontext;
        this.container = container;
        this.classloader = classloader;
        final Config config = ConfigProvider.getConfig(this.classloader);
        isPublic = getOptionalValue(config, PUBLIC, Boolean.class, PUBLIC_DEFAULT_VALUE);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            registerOSGiService();
        }

        else {
            unregisterOSGiService();
        }
    }

    /**
     * @param contextRoot
     */
    public void setModuleURL(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    /**
     * @param hosts
     */
    public void setNonDefaultHosts(List<String> nonDefaultHosts) {
        this.nonDefaultHosts = nonDefaultHosts;
    }

    /** {@inheritDoc} */
    @Override
    public OpenAPI getOpenAPIModel() {

        if (document != null)
            return document;

        //Construct model from ModelReader, static file and annotation scanner
        OpenAPI document = null;
        ConfigProcessor configProcessor = new ConfigProcessor(classloader);
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Retrieved configuration values : " + configProcessor);
        }

        OASModelReader modelReader = com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils.getOASModelReader(classloader, configProcessor.getModelReaderClassName());
        if (modelReader != null) {
            try {
                OpenAPI model = modelReader.buildModel();
                if (model != null) {
                    document = model;
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Content from model reader: ", OpenAPIUtils.getSerializedJsonDocument(document));
                    }
                }
            } catch (Throwable e) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to construct model from the application: " + e.getMessage());
                }
            }
        }

        String openAPIStaticFile = StaticFileProcessor.getOpenAPIFile(container);

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Content from static file: ", openAPIStaticFile);
        }
        if (openAPIStaticFile != null) {

            SwaggerParseResult result = new OpenAPIParser().readContents(openAPIStaticFile, document, null, null);
            if (result != null && result.getOpenAPI() != null) {
                document = result.getOpenAPI();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Document after merging contents from model reader and static file: ", OpenAPIUtils.getSerializedJsonDocument(document));
                }
            } else {
                Tr.error(tc, "OPENAPI_FILE_PARSE_ERROR", contextRoot);
            }

        }

        if (!configProcessor.isScanDisabled()) {
            AnnotationScanner scanner = com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils.creatAnnotationScanner(classloader, container);
            Set<String> classNamesToScan = new HashSet<>();
            if (configProcessor.getClassesToScan() != null) {
                classNamesToScan.addAll(configProcessor.getClassesToScan());
            }

            if (configProcessor.getPackagesToScan() != null) {
                Set<String> foundClasses = scanner.getAnnotatedClassesNames();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Found annotated classes (packages to scan): ", foundClasses);
                }
                for (String packageName : configProcessor.getPackagesToScan()) {
                    for (String className : foundClasses) {
                        if (className.startsWith(packageName)) {
                            classNamesToScan.add(className);
                        }
                    }
                }
            }

            if (classNamesToScan.size() == 0 && scanner.anyAnnotatedClasses()) {
                Set<String> foundClasses = scanner.getAnnotatedClassesNames();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Found annotated classes (any annotated classes): ", foundClasses);
                }
                classNamesToScan.addAll(foundClasses);
            }
            if (configProcessor.getClassesToExclude() != null) {
                classNamesToScan.removeAll(configProcessor.getClassesToExclude());
            }
            if (configProcessor.getPackagesToExclude() != null) {
                for (String packageToExclude : configProcessor.getPackagesToExclude()) {
                    Iterator<String> iterator = classNamesToScan.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().startsWith(packageToExclude)) {
                            iterator.remove();
                        }
                    }
                }
            }

            if (classNamesToScan.size() > 0) {
                Set<Class<?>> classes = new HashSet<>();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Final list of class names to scan: ", classNamesToScan);
                }

                for (String clazz : classNamesToScan) {
                    try {
                        classes.add(classloader.loadClass(clazz));
                    } catch (ClassNotFoundException e) {
                        if (OpenAPIUtils.isEventEnabled(tc)) {
                            Tr.event(tc, "Failed to load class: " + e.getMessage());
                        }
                    }
                }
                Reader reader = new Reader(document);
                reader.setApplicationPath(scanner.getURLMapping(classNamesToScan));
                document = reader.read(classes);
            }
        }
        if (document == null) {
            return document;
        }

        // Filter
        OASFilter oasFilter = com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils.getOASFilter(classloader, configProcessor.getOpenAPIFilterClassName());
        if (oasFilter != null) {
            final OpenAPIFilter filter = new OpenAPIFilter(oasFilter);
            try {
                filter.filter(document);
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Document after filtering: ", OpenAPIUtils.getSerializedJsonDocument(document));
                }
            } catch (Throwable e) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to call OASFilter: " + e.getMessage());
                }
            }
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Finished generating document for " + this);
        }
        processOpenAPI(document);
        return document;
    }

    /** {@inheritDoc} */
    @Override
    public String getOpenAPIDocument() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getContextRoot() {
        return this.contextRoot;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPublic() {
        return this.isPublic;
    }

    public void registerOSGiService() {
        //We want to cause an update to our APIProvider document if this was already registered, so call unregister first.
        //If we were never registered before, then the call to unregister will be no-op
        unregisterOSGiService();

        //getOpenAPIServices(classLoader);

        //Double check before registering
        /*
         * if (cachedOpenAPI == null && apiDocAsString == null && !annotationScanner.anyAnnotatedClasses() &&
         * (openAPIScanner == null || openAPIScanner.getClasses() == null || openAPIScanner.getClasses().isEmpty())) {
         * if (OpenAPIUtils.isDebugEnabled(tc)) {
         * Tr.debug(tc, "Avoiding registration because no annotated classes nor pre-generated docs were found in the web module, and no configuration was set.");
         * }
         * return;
         * }
         */

        //Register API web provider into DS
        serviceRegistration = this.ccontext.getBundleContext().registerService(OASProvider.class, this, PROPS);

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Register Service Registration Obj: " + serviceRegistration);
        }
    }

    public void unregisterOSGiService() {
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Unregister Service Registration Obj: " + serviceRegistration);
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        serviceRegistration = null;
    }

    private void processOpenAPI(OpenAPI openapi) {
        final String contextRoot = getContextRoot();
        if (openapi == null || contextRoot == null || contextRoot.equals("/")) {
            return;
        }

        /*
         * If user specified server (at global level or at path level) then assume they know what they are doing.
         * - No need to add context root (relative) in that case. Otherwise, add context root if path doesn't already start with it.
         * - If non-default host is set then append it to all global relative servers to create a full URL.
         */

        //If there is no global server and the path doesn't already start with context root and doesn't specify path-level servers
        //then add this context root as global relative server
        if (OpenAPIUtils.isContextRootNeeded(openapi, contextRoot)) {
            openapi.addServer(new ServerImpl().url(contextRoot));
        }

        //If the application is using non default host then append them to relative servers. Create new servers when necessary.
        if (nonDefaultHosts != null && !nonDefaultHosts.isEmpty()) {
            List<Server> servers = openapi.getServers();
            if (!OpenAPIUtils.isGlobalServerSpecified(openapi)) {
                //no global servers - add non default hosts as servers
                for (String host : nonDefaultHosts) {
                    openapi.addServer(new ServerImpl().url(host));
                }
            } else {
                // Contains global servers. Append non default hosts on each relative server.
                List<Server> newServers = new ArrayList<Server>();
                for (Server server : servers) {
                    String url = server.getUrl();
                    if (url != null && url.startsWith("/")) {
                        //relative server
                        for (int i = 0; i < nonDefaultHosts.size(); i++) {
                            if (i == 0) {
                                server.setUrl(nonDefaultHosts.get(i) + url); //set new URL on existing server
                            } else {
                                Server newServer = OpenAPIUtils.copyServer(server); //create new server and set URL
                                newServer.setUrl(nonDefaultHosts.get(i) + url);
                                newServers.add(newServer);
                            }
                        }
                    }
                }
                servers.addAll(newServers);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OpenAPIWebProvider={");
        sb.append("contextRoot=");
        sb.append(getContextRoot());
        sb.append("}");
        return sb.toString();
    }
}
