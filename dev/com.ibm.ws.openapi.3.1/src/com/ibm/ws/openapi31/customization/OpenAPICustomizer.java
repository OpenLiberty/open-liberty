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
package com.ibm.ws.openapi31.customization;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.openapi31.OpenAPIAggregator;
import com.ibm.ws.openapi31.OpenAPIUtils;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Customize OpenAPI document and explorer
 */
@Component(service = { OpenAPICustomizer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class OpenAPICustomizer implements FileMonitor {

    private static final TraceComponent tc = Tr.register(OpenAPICustomizer.class);

    private WsLocationAdmin locationAdminProvider;

    private final String KEY_OAS_AGGREGATOR = "OpenAPIAggregator";
    private final AtomicServiceReference<OpenAPIAggregator> oasAggregatorRef = new AtomicServiceReference<OpenAPIAggregator>(KEY_OAS_AGGREGATOR);

    private ComponentContext context;
    private ServiceRegistration<FileMonitor> fileMonitor;

    private final String LOCATION_YAML = "${server.config.dir}/openapi-3.1/customization.yaml";
    private final String LOCATION_YML = "${server.config.dir}/openapi-3.1/customization.yml";
    private final String LOCATION_JSON = "${server.config.dir}/openapi-3.1/customization.json";
    private static final String FILE_POLLING_INTERVAL = OASConfig.EXTENSIONS_PREFIX + "liberty.file.polling.interval";
    private static final int FILE_POLLING_INTERVAL_DEFAULT_VALUE = 2;
    private final List<File> customizationFiles = new ArrayList<File>();
    private Integer pollingInterval = FILE_POLLING_INTERVAL_DEFAULT_VALUE;

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        this.context = context;
        final Config config = ConfigProvider.getConfig(OpenAPICustomizer.class.getClassLoader());
        pollingInterval = OpenAPIUtils.getOptionalValue(config, FILE_POLLING_INTERVAL, Integer.class, FILE_POLLING_INTERVAL_DEFAULT_VALUE, v -> v >= 0);
        oasAggregatorRef.activate(context);
        normalizeDefaultCustomizerPaths();
        processCustomizationFiles();
    }

    private File normalizeCustomizerPath(String path) {
        String normalizedPath = PathUtils.normalize(path);
        WsResource resource = getLocationAdmin().resolveResource(normalizedPath);
        File customizationFile = resource.asFile();
        return customizationFile;
    }

    private void normalizeDefaultCustomizerPaths() {
        customizationFiles.add(normalizeCustomizerPath(LOCATION_YAML));
        customizationFiles.add(normalizeCustomizerPath(LOCATION_YML));
        customizationFiles.add(normalizeCustomizerPath(LOCATION_JSON));
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        deactivateFileMonitor();
        oasAggregatorRef.deactivate(context);
    }

    private void setCustomization(File customizationFile) {
        OpenAPI parsedOpenAPI = OpenAPIUtils.parseOpenAPI(OpenAPIUtils.getAPIDocFromFile(customizationFile));
        if (parsedOpenAPI == null) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Document is null/empty for OpenAPI Definition: " + customizationFile);

            }
            Tr.warning(tc, "CUSTOMIZATION_IS_NULL", customizationFile);
        }
        getOASProviderAggregator().setOpenAPICustomization(parsedOpenAPI);
        if (OpenAPIUtils.isDebugEnabled(tc)) {
            Tr.debug(this, tc, "Default customization file was pushed : location=" + customizationFile);
        }
    }

    @Reference(service = OpenAPIAggregator.class, name = KEY_OAS_AGGREGATOR)
    protected void setOASProviderAggregator(ServiceReference<OpenAPIAggregator> oasProviderAggregator) {
        oasAggregatorRef.setReference(oasProviderAggregator);
    }

    protected void unsetOASProviderAggregator(ServiceReference<OpenAPIAggregator> oasProviderAggregator) {
        oasAggregatorRef.unsetReference(oasProviderAggregator);
    }

    protected OpenAPIAggregator getOASProviderAggregator() {
        OpenAPIAggregator oasProviderAggregator = oasAggregatorRef.getService();

        if (oasProviderAggregator == null) {
            throw new IllegalStateException(OpenAPIUtils.getOsgiServiceErrorMessage(this.getClass(), "OASProviderAggregator"));
        }
        return oasProviderAggregator;
    }

    @Reference(service = WsLocationAdmin.class)
    protected void setLocationAdmin(WsLocationAdmin provider) {
        this.locationAdminProvider = provider;
    }

    protected void unsetLocationAdmin(WsLocationAdmin provider) {
        this.locationAdminProvider = null;
    }

    private WsLocationAdmin getLocationAdmin() {
        if (locationAdminProvider == null) {
            throw new IllegalStateException(OpenAPIUtils.getOsgiServiceErrorMessage(this.getClass(), "WsLocationAdmin"));
        }
        return locationAdminProvider;
    }

    private boolean processCustomizationFiles() {
        List<File> existingFiles = new ArrayList<File>();
        try {
            //check which default files exist
            existingFiles = customizationFiles.stream().filter(f -> f.exists()).collect(Collectors.toList());
        } catch (SecurityException se) {
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "SecurityException occurred while checking whether default files exist: " + se);
            }
            //Let it FFDC - so users would know that the file couldn't be accessed due to security restriction
        }

        int fileCount = existingFiles.size();
        if (fileCount == 0) {
            //no default files exist - setup file monitoring on all default files
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "None of the default customization files exist. Set up monitoring on all of them.");
            }
            monitorFiles(customizationFiles.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList()));
        } else {
            File file = existingFiles.iterator().next();
            if (fileCount > 1) {
                //multiple default files exist - monitor only one file and issue a warning
                Tr.warning(tc, "MULTIPLE_DEFAULT_OPENAPI_FILES", existingFiles.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ", "{", "}")),
                           file.getAbsolutePath());
            }
            monitorFiles(Arrays.asList(file.getAbsolutePath()));
            setCustomization(file);
            return true;
        }

        return false;
    }

    //-------------------
    // File Monitoring
    //-------------------

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> arg0) {
        //no-op
    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        if (OpenAPIUtils.isDebugEnabled(tc)) {
            Tr.debug(this, tc, "Received notification from FileMonitor: createdFiles=" + createdFiles + " : modifiedFiles=" + modifiedFiles + " : deletedFiles=" + deletedFiles);
        }

        if ((deletedFiles != null && !deletedFiles.isEmpty())) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Default customization file was deleted : location=" + deletedFiles.iterator().next().toString());
            }
            getOASProviderAggregator().setOpenAPICustomization(null);
            processCustomizationFiles();
        }

        if ((createdFiles != null && !createdFiles.isEmpty())) {
            File file = createdFiles.iterator().next();
            if (createdFiles.size() > 1) {
                //multiple files created - monitor only one file and issue a warning
                Tr.warning(tc, "MULTIPLE_DEFAULT_OPENAPI_FILES", createdFiles.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ", "{", "}")),
                           file);
            }
            monitorFiles(Arrays.asList(file.getAbsolutePath()));
            setCustomization(file);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Customization file was created : location=" + file.toString());
            }
        }

        if ((modifiedFiles != null && !modifiedFiles.isEmpty())) {
            File file = modifiedFiles.iterator().next();
            setCustomization(file);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Customization file was modified : location=" + file.toString());
            }
        }
    }

    private synchronized void deactivateFileMonitor() {
        if (fileMonitor != null) {
            fileMonitor.unregister();
            fileMonitor = null;
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Deactivated FileMonitor service.");
            }
        }
    }

    private synchronized void monitorFiles(List<String> fileLocations) {

        if (pollingInterval == 0) {
            deactivateFileMonitor();
            return;
        }

        final BundleContext bundleContext = context.getBundleContext();
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_VENDOR, "IBM");
        props.put(FileMonitor.MONITOR_INTERVAL, String.valueOf(pollingInterval) + "s");
        props.put(FileMonitor.MONITOR_FILES, fileLocations);
        if (fileMonitor == null) {
            fileMonitor = bundleContext.registerService(FileMonitor.class, this, props);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Registered FileMonitor service : fileLocations=" + fileLocations.stream().map(f -> f.toString()).collect(Collectors.joining(", ", "{", "}")));
            }
        } else {
            fileMonitor.setProperties(props);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Updated FileMonitor service : fileLocations=" + fileLocations.stream().map(f -> f.toString()).collect(Collectors.joining(", ", "{", "}")));
            }
        }
    }

}