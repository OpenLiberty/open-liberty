/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.VersionRange;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.pojo.FeatureTool;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 *
 */
@Component(service = { IFeatureToolService.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class FeatureToolService implements IFeatureToolService, EventHandler {
    private static final TraceComponent tc = Tr.register(FeatureToolService.class);

    /**
     * Store all the file paths of the manifests we've processed, and their last modified time.
     * If manifests are detected that have a newer time stamp, the features will need to be refreshed.
     */
    private final AtomicReference<Map<String, Long>> processedManifestFiles = new AtomicReference<Map<String, Long>>(new HashMap<String, Long>());

    /**
     * A more complex way to detect new / changed features could and maybe should be employed here.
     * The old model was good, but for the immediate needs, just knowing if its the first pass through
     * should be sufficient. Note: there is the window where if you added a feature after starting the
     * server, but before accessing the Admin Center, you may need to reload. That, however, is a
     * reasonable risk to take for right now.
     */
    private boolean initialProcess = true;

    private final Map<String, IconInfo> iconMap = new HashMap<String, IconInfo>();

    private FeatureProvisioner provisionerService;
    private VariableRegistry variableRegistryService;

    private ServiceRegistration<EventHandler> eventHandlerReg = null;
    private static final String FEATURE_MANAGER_TOPIC = "com/ibm/ws/kernel/feature/internal/FeatureManager/*";
    private static final String FEATURE_PROVISIONING_STARTED = "started";
    private static final String FEATURE_PROVISIONING_COMPLETE = "complete";
    private static final String FEATURE_PROVISIONING_PENDING = "pending";
    private final AtomicReference<String> featureProvisioningState = new AtomicReference<String>(FEATURE_PROVISIONING_COMPLETE);
    // This boolean is used each time we check for new features. Because we only need to dispose of the bundleRepos once, this will get
    // set to false at the start of the check, and if we refresh the repos, it gets set to true so that we don't keep disposing and
    // re-reading.
    private boolean bundleRepoRefreshed = false;
    // The wait 200ms between each provison check
    private final int featureProvisionCheckInterval = 200;
    // .. And wait up to 2secs for a provision to complete
    private final int featureProvisionMaxLoopCount = 10;

    /**
     * The injection point for the provisioner service that allows us to get the installed features.
     *
     * @param provisionerService The provisioner Service.
     */
    @Reference(service = FeatureProvisioner.class)
    protected synchronized void setKernelProvisioner(FeatureProvisioner provisionerService) {
        this.provisionerService = provisionerService;
    }

    protected synchronized void unsetKernelProvisioner(FeatureProvisioner provisionerService) {
        if (this.provisionerService == provisionerService) {
            this.provisionerService = null;
        }
    }

    /**
     * The injection point for the variableRegistry service that allows us to get feature locations.
     *
     * @param variableRegistryService The variableRegistry service
     */
    @Reference(service = VariableRegistry.class)
    protected synchronized void setVariableRegistry(VariableRegistry variableRegistryService) {
        this.variableRegistryService = variableRegistryService;
    }

    protected synchronized void unsetVariableRegistry(VariableRegistry variableRegistryService) {
        if (this.variableRegistryService == variableRegistryService) {
            this.variableRegistryService = null;
        }
    }

    protected void activate(ComponentContext componentCtx) {
        // Register an event handler to identify when the FeatureManager has provisioned new features
        Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
        svcProps.put(EventConstants.EVENT_TOPIC, new String[] { FEATURE_MANAGER_TOPIC });
        eventHandlerReg = componentCtx.getBundleContext().registerService(EventHandler.class, this, svcProps);
    }

    protected void deactivate() {
        if (eventHandlerReg != null)
            eventHandlerReg.unregister();
    }

    /** {@inheritDoc} */
    @Override
    public Set<FeatureTool> getTools() {
        return processToolFeatures(Locale.getDefault());
    }

    /**
     * This method reads all of the feature manifests in the Liberty runtime, and processes
     * each of these to ensure that they have been included in the catalog. We add any new tools
     * we find, and also remove any that have been uninstalled from the runtime. We store the
     * read manifests so we don't repeatedly reread the same ones for performance reasons.
     *
     * @param locale The Locale in which to load all of the manifests
     * @return The set of FeatureTools for the given locale
     */
    private synchronized Set<FeatureTool> processToolFeatures(Locale locale) {
        // Always create a new set of Tools. This simplifies our logic, and because the cost to do this is low,
        // and because we have so few tools, the performance impact is negligible.
        Set<FeatureTool> toolFeatures = new HashSet<FeatureTool>();
        Map<String, Long> newTimestamps = new HashMap<String, Long>();
        Map<String, Long> oldTimestamps = processedManifestFiles.get();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Previously processed Manifests: ", oldTimestamps);
        }

        // Set the bundleRepo refresh boolean to false for the start of the checking of manifests.
        bundleRepoRefreshed = false;

        // Start by looking at the feature manifests in the core locations.
        // We use the variableRegistry to get the installDir for the current runtime.
        File featureDir = new File(variableRegistryService.resolveString(VariableRegistry.INSTALL_DIR), "lib/features");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing Core Manifests: " + featureDir);
        }
        File[] manifestFiles = getFeatureManifests(newTimestamps, oldTimestamps, featureDir);
        addAllFeatureTools(toolFeatures, locale, featureDir, "", manifestFiles, ExtensionConstants.CORE_EXTENSION);

        // Process the usr extension location.
        featureDir = new File(variableRegistryService.resolveString("${feature:usr}"));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing Usr Manifests: " + featureDir);
        }
        manifestFiles = getFeatureManifests(newTimestamps, oldTimestamps, featureDir);
        addAllFeatureTools(toolFeatures, locale, featureDir, "usr:", manifestFiles, ExtensionConstants.USER_EXTENSION);

        // Now we process all of the product extensions in the runtime.
        for (ProductExtensionInfo productExtension : ProductExtension.getProductExtensions()) {
            String featureType = productExtension.getName();
            // Look up the productExtension feature dir from values stored in variableRegistryService.
            String productExtnFeatureDirName = variableRegistryService.resolveString("${feature:" + featureType + "}");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing " + featureType + " Production Extension Manifests: " + productExtnFeatureDirName);
            }
            featureDir = new File(productExtnFeatureDirName);
            manifestFiles = getFeatureManifests(newTimestamps, oldTimestamps, featureDir);
            addAllFeatureTools(toolFeatures, locale, featureDir, featureType + ":", manifestFiles, featureType);
        }

        // Finally set the processed Manifests to the currentFeatureManifestsProcessed variable.
        // We want to set this rather than add remove, so that we don't miss any uninstalled features.
        processedManifestFiles.set(newTimestamps);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting processed Manifest Files: " + processedManifestFiles);
        }

        // Finally if we need to clean up the bundleRepositories, ensuring that there isn't an active feature provisioning .
        waitForFeatureProvision();
        BundleRepositoryRegistry.disposeAll();

        initialProcess = false;
        return toolFeatures;
    }

    /**
     * Determine if any of the elements in the newTimestamps is new (does not exist in oldTimestamps)
     * or if the stored time stamp for entry is newer than the old entry.
     *
     * @param newTimestamps The current set of timestamps
     * @param oldTimestamps The previous set of timestamps
     * @return {@code true} if there is a new (or newer) entry
     */
    private boolean hasNewOrChangedManifests(Map<String, Long> newTimestamps, Map<String, Long> oldTimestamps) {
        // Iterate through all of the new time stamps and compare with the old time stamps
        for (Entry<String, Long> newTimestamp : newTimestamps.entrySet()) {
            Long oldTimestamp = oldTimestamps.get(newTimestamp.getKey());
            // If the old time stamp does not exist, or if the new time stamp is newer, we've been changed.
            if (oldTimestamp == null || newTimestamp.getValue() > oldTimestamp) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the feature manifests in the featureDir. If there are new features since
     * last we checked, refresh the feature manager!
     *
     * @param newTimestamps The current set of timestamps
     * @param oldTimestamps The previous set of timestamps
     * @param featureDir The feature directory to search
     * @return Array of File objects which are feature manifests
     */
    private File[] getFeatureManifests(Map<String, Long> newTimestamps, Map<String, Long> oldTimestamps, File featureDir) {
        // Find all the feature manifests in the feature dir.
        File[] manifestFiles = ManifestUtils.findAllFeatureManifests(featureDir);

        // Add all of the manifest paths to the current set of processed manifest paths.
        Map<String, Long> theseTimestamps = getManifestPaths(manifestFiles);
        if (hasNewOrChangedManifests(theseTimestamps, oldTimestamps) && !initialProcess) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "New or updated feature manifests detected. Refreshing the Feature Manager");
            }

            // Set the feature provisioning state to be pending. This is so that we'll wait until the provisioning that we trigger completes.
            // There is still a small window where we may finish before the provisioning is complete, but there is no way with this
            // mechanism that we can know 100% that the new tools have been provisioned.
            featureProvisioningState.set(FEATURE_PROVISIONING_PENDING);
            // Refresh the features
            provisionerService.refreshFeatures();
            // Wait for the provision to complete. We don't do anything if it fails, as we need to return control back to the UI, and
            // the user will see that the tool isn't listed.
            waitForFeatureProvision();

        }
        newTimestamps.putAll(theseTimestamps);

        return manifestFiles;
    }

    /**
     * Adds all of the FeatureTool objects to the toolFeatures set based on the given
     * feature directory.
     *
     * @param toolFeatures The Set to add FeatureTool to update
     * @param locale The locale in which to load the manifests
     * @param featureDir The feature directory
     * @param featureType The type of features in the featureDir
     * @param manifestFiles The manifest files in the featureDir
     * @param bundleRepoType - A String indicating the Bundle repository type, that we will use to find the bundle to read.
     */
    private void addAllFeatureTools(Set<FeatureTool> toolFeatures, Locale locale, File featureDir,
                                    String featureType, File[] manifestFiles, String bundleRepoType) {
        // Get all of the feature tool manifests
        Map<String, Map<String, String>> featureToolManifests = getFeatureToolManifests(manifestFiles, featureType);

        // If we have feature manifests to process then check to see if we need to add them to the catalog.
        if (!featureToolManifests.isEmpty()) {
            // As we have manifests to process, we need to dispose of the bundle repos, so we re-read the manifests that exist on disk.
            // We only need to do this once, so first check to see if we've already done a dispose for this run, and then check
            // that there isn't a FeatureManager Provision going on. If not then refresh the bundle repos.
            if (!bundleRepoRefreshed && waitForFeatureProvision()) {
                BundleRepositoryRegistry.disposeAll();
                bundleRepoRefreshed = true;
            }

            // We now go through and read the manifests and work out if the tool is already in the catalog.
            // If it isn't we'll add it to the list of tools to add.
            addToolsToCatalog(toolFeatures, featureToolManifests, bundleRepoType, featureDir, locale);
        }

    }

    /**
     * This method takes a set of manifest files, and finds which of these are Tool features that have been installed into the
     * runtime.
     *
     * @param manifests An array of manifest files to process.
     * @param installedFeaturePrefix A prefix that is used for user and product extension feature names.
     * @return A Map whose key is the feature name, and whose value is a Map of Manifest headers for the particular feature. This
     *         can be empty if no features are found.
     */
    private Map<String, Map<String, String>> getFeatureToolManifests(File[] manifests, String installedFeaturePrefix) {
        // Get a list of installed features.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing manifests: " + Arrays.toString(manifests));
        }

        Set<String> installedFeatures = provisionerService.getInstalledFeatures();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Installed features: " + installedFeatures);
        }

        // Find all of the adminCenter Tool features.
        Map<String, Map<String, String>> allToolFeatureManifests = ManifestUtils.findToolFeatureManifests(manifests);

        // Now we have the list of all Tool features that are on disk, find which of these have already been installed into
        // the runtime.
        return filterInstalledToolFeatureManifests(allToolFeatureManifests, installedFeatures, installedFeaturePrefix);
    }

    /**
     * This method returns a Map of file paths and their last modified times based
     * on the provided File array.
     *
     * @param manifestsToProcess An array of manifest files to process.
     * @return A Map of file paths of the manifests and their last modified times.
     */
    private Map<String, Long> getManifestPaths(File[] manifestsToProcess) {
        Map<String, Long> manifestPaths = new HashMap<String, Long>();
        if (manifestsToProcess != null) {
            for (final File manifest : manifestsToProcess) {
                manifestPaths.put(manifest.getAbsolutePath(), getLastModified(manifest));
            }
        }
        return manifestPaths;
    }

    /**
     * Gets the last modified time of the File.
     *
     * @param fileToCheck
     * @return
     */
    private Long getLastModified(final File fileToCheck) {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return fileToCheck.lastModified();
            }
        });
    }

    /**
     * This method returns a Map of features, and their manifest headers, that are AdminCenter Tool features and are installed
     * into the runtime.
     *
     * @param featureManifests A Map containing all the feature manifests headers, keyed by feature name.
     * @param installedFeatures A Set of Strings listing all the installed features in the runtime.
     * @param installedFeaturePrefix An extension prefix for feature names.
     * @return
     */
    private Map<String, Map<String, String>> filterInstalledToolFeatureManifests(Map<String, Map<String, String>> featureManifests,
                                                                                 Set<String> installedFeatures, String installedFeaturePrefix) {
        // Setup the return object
        Map<String, Map<String, String>> installedFeatureManifests = new HashMap<String, Map<String, String>>();

        if (installedFeatures != null) {
            for (Map<String, String> manifestHeaders : featureManifests.values()) {
                // Find the feature name that the Feature manager will use in the installed feature list.,
                String featureName = ManifestUtils.getInstalledFeatureName(manifestHeaders.get(ManifestUtils.SUBSYSTEM_SYMBOLIC_NAME),
                                                                           manifestHeaders.get("IBM-ShortName"), installedFeaturePrefix);
                // If we don't have a list of installedFeatures then add the tool feature. If we do have a
                // installedfeature list, then ensure that the feature is contained within the list.
                if (installedFeatures.contains(featureName))
                    installedFeatureManifests.put(featureName, manifestHeaders);
            }
        }

        return installedFeatureManifests;
    }

    /**
     * Retrieve a tool by its symbolicname and version. The
     * Feature SymbolicName and version map to the feature Manifest
     * Subsystem-SymbolicName and version. If the tool is a Bookmark this
     * method will not return anything as the Bookmark doesn't set a
     * Symbolic Name.
     *
     * @param symbolicName The symbolicName of the tool.
     * @param version The version of the tool
     * @return The Tool object with the specified name and version, or {@code null} if no such tool exists.
     */
    private FeatureTool getTool(Set<FeatureTool> set, String featureName, String version) {
        FeatureTool tool = null;
        // Iterate over all of the known tools
        for (FeatureTool checkTool : set) {
            String checkToolFeatureName = checkTool.getFeatureName();
            String checkToolVersion = checkTool.getFeatureVersion();
            // If we have match of Symbolicname and version, then store
            // the tool for returning.
            if (featureName != null && version != null) {
                if (featureName.equals(checkToolFeatureName) && version.equals(checkToolVersion))
                    tool = checkTool;
            }
        }
        return tool;
    }

    /**
     * TODO: Remove this code in favor of a kernel SPI.
     * Shamefully copied from the kernel code. This whole component will go away in 4Q14 though.
     *
     * @param l10nDir
     * @param symbolicName
     * @param locale
     * @return
     */
    @FFDCIgnore(IOException.class)
    private ResourceBundle getResourceBundle(File l10nDir, String symbolicName, Locale locale) {
        //KEEP IN SYNC WITH getLocalizationFiles !!!
        File[] files = new File[] { new File(l10nDir, symbolicName + "_" + locale.toString() + ".properties"),
                                    new File(l10nDir, symbolicName + "_" + locale.getLanguage() + ".properties"),
                                    new File(l10nDir, symbolicName + ".properties") };

        for (File file : files) {
            if (file.exists()) {
                try {
                    return new PropertyResourceBundle(new FileReader(file));
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    /**
     * TODO: This code is a hack-job, copied from various other places. We need to clean this up
     * and remove this code in favor of a kernel SPI.
     *
     * @param featureDir
     * @return
     */
    private String getTranslatedProperty(File featureDir, String symbolicName, String property, Locale locale) {
        File l10nDir = new File(featureDir, "l10n");
        ResourceBundle rb = getResourceBundle(l10nDir, symbolicName, locale);
        if (rb != null) {
            // Find the new value in the resource bundle
            return rb.getString(property);
        }
        return property;
    }

    /**
     * This method adds any new tool feature manifests to the AdminCenter Catalog. We build up the tool with the metadata in the feature
     * manifest and in some cases, metadata in the included Bundle manifests.
     *
     * @param set The set to update
     * @param toolManifests The list of installed Tool Manifests.
     * @param bundleRepoType - A String indicating the Bundle repository type, that we will use to find the bundle to read.
     * @param featureDir The featureDir for the current extension.
     * @param locale The Locale in which to load the manifests
     */
    private void addToolsToCatalog(Set<FeatureTool> set, Map<String, Map<String, String>> toolManifests,
                                   String bundleRepoType, File featureDir, Locale locale) {
        // Iterate over the tool feature manifests
        for (Map.Entry<String, Map<String, String>> featureManifest : toolManifests.entrySet()) {

            // Get the FeatureName from the key.
            String featureName = featureManifest.getKey();

            Map<String, String> featureHeaders = featureManifest.getValue();

            String symbolicName = ManifestHeaderProcessor.parseBundleSymbolicName(featureHeaders.get(ManifestUtils.SUBSYSTEM_SYMBOLIC_NAME)).getName();
            String featureVersion = featureHeaders.get(ManifestUtils.SUBSYSTEM_VERSION);
            String shortName = featureHeaders.get(ManifestUtils.IBM_SHORT_NAME);

            String name = featureHeaders.get(ManifestUtils.SUBSYSTEM_NAME);
            // If we have a Subsystem name, then use that. If we don't then use the IBM Short name. If we don't have this
            // then default back to the SymbolicName.
            if (name == null) {
                if (shortName != null) {
                    name = shortName;
                } else {
                    name = symbolicName;
                }
            } else {
                if (name.charAt(0) == '%') {
                    // name is translated, try to resolve it
                    name = getTranslatedProperty(featureDir, symbolicName, name.substring(1), locale);
                }
            }

            // if ("openidConnectServer-1.0".equals(featureName)) {
            //     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            //         Tr.debug(tc, "Skipping feature " + featureName);
            //         continue;
            //     }
            // }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing feature " + featureName + ":");
                Tr.debug(tc, "   SymbolicName:" + symbolicName);
                Tr.debug(tc, "   IBMShortName:" + shortName);
                Tr.debug(tc, "   Version:     " + featureVersion);
                Tr.debug(tc, "   Name:        " + name);
            }

            // Get the description.
            String description = featureHeaders.get(ManifestUtils.SUBSYSTEM_DESCRIPTION);
            if (description != null && description.charAt(0) == '%') {
                // description is translated, try to resolve it
                description = getTranslatedProperty(featureDir, symbolicName, description.substring(1), locale);
            }

            // We use an Icon serving Rest Service that allows us to specify the feature name and version, and icon size.
            // When this URL is invoked, the icon is returned. We use this for security reasons so that we stop malicious url's
            // in the icon header.
            String icon = featureHeaders.get(ManifestUtils.SUBSYSTEM_ICON);
            if (icon != null)
                iconMap.put(symbolicName, new IconInfo(icon, new File(featureDir, "icons").getAbsolutePath()));
            String iconURL = "/ibm/api/adminCenter/v1/icons/" + ((icon == null) ? "default" : symbolicName);

            // We get the Tool URL from the bundles that are defined in the Subsystem Content header.
            String content = featureHeaders.get(ManifestUtils.SUBSYSTEM_ENDPOINT_CONTENT);
            if (content != null) {
                addToolsToCatalogForEndpoints(set, symbolicName, featureName, featureVersion, featureHeaders, featureDir, iconURL, description);
            } else {
                content = featureHeaders.get(ManifestUtils.SUBSYSTEM_CONTENT);
                String url = findToolURL(symbolicName, content, bundleRepoType);

                // We now should have the metadata to create the tool.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding FeatureTool " + featureName + ":");
                    Tr.debug(tc, "   Description:" + description);
                    Tr.debug(tc, "   icon:       " + iconURL);
                    Tr.debug(tc, "   url:        " + url);
                }

                // We need to check to see if this tool is already in the catalog. Look it up with the SymbolicName and
                // version. If it does exist we need to remove the existing tool. Then we always add the new tool.
                FeatureTool existingTool = getTool(set, featureName, featureVersion);
                if (existingTool != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Processing feature " + featureName + ":");
                    set.remove(existingTool);
                }
                set.add(new FeatureTool(featureName, featureVersion, shortName, name, url, iconURL, description));
            }
        }
    }

    /**
     * This method adds the endpoint tools to the AdminCenter Catalog.
     *
     * @param set The set to update
     * @param featureName The feature name
     * @param featureVersion The feature version
     * @param featureHeaders The feature headers
     * @param iconURL URL to the tool icon
     * @param description feature description
     */
    private void addToolsToCatalogForEndpoints(Set<FeatureTool> set, String symbolicName, String featureName, String featureVersion,
                                               Map<String, String> featureHeaders, File featureDir, String defaultIconURL,
                                               String description) {
        // look for endpoint metadata in the manifest:
        // Subsystem-Endpoint-Names: clientManagement=OpenID Connect Client Management, personalTokenManagement=OpenID Connect Personal Token Management, usersTokenManagement=OpenID Connect Users Token Management
        // Subsystem-Endpoint-Urls: className=com.ibm.ws.security.openidconnect.server.plugins.UIHelperService, methodName=getProviderInfo
        // Subsystem-Endpoint-ShortNames: clientManagement=clientManagement-1.0, personalTokenManagement=personalTokenManagement-1.0, usersTokenManagement=usersTokenManagement-1.0
        String endpointNames = featureHeaders.get(ManifestUtils.SUBSYSTEM_ENDPOINT_NAMES);
        String endpointUrls = featureHeaders.get(ManifestUtils.SUBSYSTEM_ENDPOINT_URLS);
        String endpointShortNames = featureHeaders.get(ManifestUtils.SUBSYSTEM_ENDPOINT_SHORTNAMES);
        String endpointIcons = featureHeaders.get(ManifestUtils.SUBSYSTEM_ENDPOINT_ICONS); // To do later

        if (endpointNames != null && endpointUrls != null && endpointShortNames != null) {
            // convert the string into map format, eg.
            // for endpoint name, "clientManagement=OpenID Connect Client Management, personalTokenManagement=OpenID Connect Personal Token Management, usersTokenManagement=OpenID Connect Users Token Management"
            // for endpoint url, "className=com.ibm.ws.security.openidconnect.server.plugins.UIHelperService, methodName=getProviderInfo"
            // for endpoint shortname, "clientManagement=clientManagement-1.0, personalTokenManagement=personalTokenManagement-1.0, usersTokenManagement=usersTokenManagement-1.0"
            Map<String, String> endpointNameMap = convertStringToMap(endpointNames);
            Map<String, String> endpointShortNameMap = convertStringToMap(endpointShortNames);
            Map<String, String> endpointUrlMap = convertStringToMap(endpointUrls);
            Map<String, String> endpointIconMap = null;
            if (endpointIcons != null) {
                endpointIconMap = convertIconStringToMap(endpointIcons);
            }

            if (endpointNameMap.size() > 0 && endpointShortNameMap.size() > 0) {
                // url could be passed in two ways:
                // - invoking the specified method of the specified class
                // - pass in in a format similar to endpoint name and short name. No further manipulation is
                //   necessary with this format.
                if (endpointUrlMap.get("className") != null && endpointUrlMap.get("methodName") != null) {
                    List<Map<String, String>> endpointProviderList = getEndpointProviders(endpointUrlMap);
                    if (endpointProviderList != null) {
                        endpointUrlMap = getEndpointUrlMap(endpointProviderList);
                    }
                }

                if (endpointUrlMap.size() > 0) {
                    for (Map.Entry<String, String> urlEntry : endpointUrlMap.entrySet()) {
                        String endpointUrl = urlEntry.getValue();

                        int lastSlashIndex = endpointUrl.lastIndexOf("/");
                        if (lastSlashIndex != -1) {
                            String endpointNameInUrl = endpointUrl.substring(lastSlashIndex + 1);
                            int secondLastSlashIndex = endpointUrl.lastIndexOf("/", lastSlashIndex - 1);
                            String providerNameInUrl = endpointUrl.substring(secondLastSlashIndex + 1, lastSlashIndex);
                            String[] endpointFeatureNameAndVersion = getEndpointNameAndVersion(featureName);
                            String endpointFeatureName;
                            // endpoint feature name is required to be a specific format (see ToolDataAPI.getToolNameFromToolID)
                            // The format is featureName-d.d, eg. com.ibm.websphere.appserver.adminCenter.tool.explore-1.0
                            if (endpointFeatureNameAndVersion != null && endpointFeatureNameAndVersion.length == 2) {
                                // construct a feature name with endpoint in it and keep the version at the end, eg.
                                // openidConnectServer.clientManagement.OP-1.0
                                endpointFeatureName = endpointFeatureNameAndVersion[0] + "." + endpointNameInUrl + "." + providerNameInUrl + endpointFeatureNameAndVersion[1];
                            } else {
                                // construct without the feature name as the feature name doesn't match the expected format
                                endpointFeatureName = endpointNameInUrl + "." + providerNameInUrl + "-1.0";
                            }

                            // append the provider name to the given name
                            String endpointName = endpointNameMap.get(endpointNameInUrl) + " - " + providerNameInUrl;
                            // short name is used in the hash, insert the provider name into the short name,
                            // eg. clientManagement.OP-1.0
                            String endpointShortName = buildEndpointShortName(endpointShortNameMap.get(endpointNameInUrl), providerNameInUrl);
                            String iconURL = defaultIconURL;
                            if (endpointIconMap != null) {
                                String icon = endpointIconMap.get(endpointNameInUrl);
                                if (icon != null) {
                                    iconMap.put(symbolicName + "/" + endpointNameInUrl, new IconInfo(icon, new File(featureDir, "icons").getAbsolutePath()));
                                    iconURL = "/ibm/api/adminCenter/v1/icons/" + symbolicName + "/" + endpointNameInUrl;
                                }
                            }
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Adding FeatureTool " + endpointFeatureName + ":");
                                Tr.debug(tc, "   Description:       " + description);
                                Tr.debug(tc, "   endpointName:      " + endpointName);
                                Tr.debug(tc, "   endpointShortName: " + endpointShortName);
                                Tr.debug(tc, "   icon:              " + iconURL);
                                Tr.debug(tc, "   url:               " + endpointUrl);
                            }
                            FeatureTool existingTool = getTool(set, endpointFeatureName, featureVersion);
                            if (existingTool != null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Processing endpoint name " + endpointFeatureName + ":");
                                set.remove(existingTool);
                            }
                            set.add(new FeatureTool(endpointFeatureName, featureVersion, endpointShortName, endpointName, endpointUrl, iconURL, description));
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "no endpoint url");
                }
            }
        }
    }

    /**
     * This method finds the Web-Contextpath to use as the AdminCenter Tool endpoint. It finds the WABs defined in the Subsystem-Content
     * and gathers the Web-ContextPaths of each of these, also looking for the IBM-AdminCenter-PrimaryEndpoint header that contains this features symbolic name
     * as it's value, which indicates that this is the bundle to use as the initial URL for this tool.
     * If we don't find the IBM-AdminCenter-Endpoint, we there are more than 1 WAB , we alphabetically sort the list and
     * pick the 1st context path in the list.
     *
     * @param featureSymbolicName The Subsystem-SymbolicName of the feature we are processing.
     * @param subsystemContent The SubsystemContent header to process.
     * @param bundleRepoType - A String indicating the Bundle repository type, that we will use to find the bundle to read.
     * @return A String containing the Tool URL to use.
     */
    private String findToolURL(String featureSymbolicName, String subsystemContent, String bundleRepoType) {

        String toolURL = null;
        // Ensure we have some subsystem content to process.
        if (subsystemContent != null) {

            List<SortedBundle> featureBundles = new ArrayList<SortedBundle>();
            // Process the SubsystemContent as an Export String, because this breaks up the string in the way we want it, i.e. directives
            // are split up for us.
            for (NameValuePair nvp : ManifestHeaderProcessor.parseExportString(subsystemContent)) {
                Map<String, String> attrs = nvp.getAttributes();

                // We process each line, and we ensure that we're only processing files, and not other subsystems.
                String type = attrs.get("type");
                if (!"osgi.subsystem.feature".equals(type)) {
                    // Get the version, location and finally lookup the bundle in the repo.
                    String version = attrs.get("version");
                    String location = attrs.get("location:");
                    VersionRange versionRange = null;
                    if (version != null)
                        versionRange = new VersionRange(version);

                    BundleRepositoryHolder bundleRepoHolder = BundleRepositoryRegistry.getRepositoryHolder(bundleRepoType);

                    File bundle = null;
                    if (bundleRepoHolder != null) {
                        ContentBasedLocalBundleRepository bundleRepo = bundleRepoHolder.getBundleRepository();

                        bundle = bundleRepo.selectBundle(location, nvp.getName(), versionRange);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Bundle matching location:" + location + "  Symbolicname:" + nvp.getName() +
                                         " and versionRange: " + versionRange + " = " + bundle);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Not matching BundleRepositoryHolder found for repository type " + bundleRepoType);
                    }
                    // We should get a bundle back that we can then process.
                    if (bundle != null) {
                        // Now generate a Sorted Bundle that allows us to put it into a List and have it auto sort.
                        SortedBundle sortedBundle = ManifestUtils.processBundleManifest(bundle);
                        if (sortedBundle != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Adding Sorted Bundle: " + sortedBundle);
                            featureBundles.add(sortedBundle);
                        }
                    }
                }
            }

            // Go through the bundles and see if we have a Primary Endpoint Feature name that matches the feature we're processing.
            // There should only be one bundle that has this header, and indicates that the context root is the
            // initial URL for the tool.
            for (SortedBundle sortedBundle : featureBundles) {
                if (toolURL == null && sortedBundle.isUIEndpoint()) {
                    if (featureSymbolicName != null && featureSymbolicName.equals(sortedBundle.getPrimaryEndpointFeatureName())) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Selected SortedBundle: " + sortedBundle);
                        toolURL = sortedBundle.getWebContextPath();
                    }
                }
            }

            // If we haven't found a toolURL yet, it means that none of the bundles have the adminCenter header.
            // We now need to sort the bundles and take the web-ContextPath of the 1st one in the sorted list.
            if (toolURL == null) {
                // Sort all the WABs, and get the WebContext Path of the 1st one in the list.
                Collections.sort(featureBundles);

                for (SortedBundle sortedBundle : featureBundles) {
                    if (toolURL == null && sortedBundle.getWebContextPath() != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Selected SortedBundle: " + sortedBundle);
                        toolURL = sortedBundle.getWebContextPath();
                    }
                }
            }
        }
        return toolURL;
    }

    /**
     * (non-Javadoc)
     *
     * @see com.ibm.ws.ui.internal.v1.IFeatureToolService#getIconMap(java.lang.String, java.lang.String)
     */
    @Override
    public Map<String, String> getIconMap() {
        Map<String, String> result = new HashMap<String, String>();

        processToolFeatures(Locale.getDefault());
        for (Map.Entry<String, IconInfo> iconEntry : iconMap.entrySet()) {
            String header = iconEntry.getValue().getIconHeader();
            result.put(iconEntry.getKey(), header);
        }
        return result;
    }

    private static class IconInfo {
        private String iconHeader = null;
        private String iconInstallLocation = null;

        public IconInfo(String iconHeader, String iconInstallLocation) {
            this.iconHeader = iconHeader;
            this.iconInstallLocation = iconInstallLocation;
        }

        /**
         * @return the iconHeader
         */
        public String getIconHeader() {
            return iconHeader;
        }

        /**
         * @return the iconInstallLocation
         */
        public String getIconInstallLocation() {
            return iconInstallLocation;
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see com.ibm.ws.ui.internal.v1.IFeatureToolService#getInstallDir(java.lang.String)
     */
    @Override
    public String getFeatureIconInstallDir(String featureSymbolicName) {
        String result = null;
        IconInfo iconInfo = iconMap.get(featureSymbolicName);
        if (iconInfo != null)
            result = iconInfo.getIconInstallLocation();

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<FeatureTool> getToolsForRequestLocale() {
        List<FeatureTool> t = new ArrayList<FeatureTool>();
        t.addAll(processToolFeatures(RequestNLS.getLocale()));
        return t;
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTool getToolForRequestLocale(String id) {
        for (FeatureTool f : processToolFeatures(RequestNLS.getLocale())) {
            if (f.getId().equals(id)) {
                return f;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void handleEvent(Event event) {
        String eventTopic = event.getTopic();
        if (eventTopic.endsWith("/FEATURE_CHANGING")) {
            featureProvisioningState.set(FEATURE_PROVISIONING_STARTED);
        } else if (eventTopic.endsWith("/FEATURE_CHANGE")) {
            // Only set the provisioning state to complete if it has been set to started. This is to ensure that if we
            // configure it to be pending during an existing provision, we'll wait until the next one to complete.
            featureProvisioningState.compareAndSet(FEATURE_PROVISIONING_STARTED, FEATURE_PROVISIONING_COMPLETE);
        } else {
            // ignore all other events
        }
    }

    /**
     * This method checks to see if a FeatureManager provision is running, and waits for it to complete.
     * If returns true if there is no provision happening, and false, if we've timed out waiting for the provision to
     * complete. It is hugely unlikely that we will return false, as this indicates that something has gone spectacularly wrong in the
     * feature manager.
     *
     * @return A boolean indicating whether there is a provision running. We will wait if we find one is running, and return when it is complete,
     *         unless the provision takes longer than our set wait time. At this point we will return false.
     */
    private boolean waitForFeatureProvision() {
        boolean result = false;
        int iteration = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Current Feature Provisioning state: " + featureProvisioningState.get());
        while (!(result = FEATURE_PROVISIONING_COMPLETE.equals(featureProvisioningState.get())) && iteration++ < featureProvisionMaxLoopCount) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Provisioning either active or pending (iteration: " + iteration + "): " + result);
                Thread.sleep(featureProvisionCheckInterval);
            } catch (InterruptedException ie) {
                //Nop
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Final provisioning state: " + featureProvisioningState.get());
        return result;
    }

    private Map<String, String> convertStringToMap(String stringValue) {
        String[] keyValuePairs = stringValue.split(",");
        Map<String, String> map = new HashMap<String, String>();
        for (String pair : keyValuePairs) {
            String[] keyValuePair = pair.split("=");
            if (keyValuePair.length == 2) {
                map.put(keyValuePair[0].trim(), keyValuePair[1].trim());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "return map: " + map);
        }
        return map;
    }

    private Map<String, String> convertIconStringToMap(String stringValue) {
        Map<String, String> map = new HashMap<String, String>();
        Matcher match = Pattern.compile("(\\w+)=(.*?)(?=,\\w+=|$)").matcher(stringValue);
        while (match.find()) {
            map.put(match.group(1), match.group(2));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "return map: " + map);
        }
        return map;
    }

    private List<Map<String, String>> getEndpointProviders(Map<String, String> endpointUrlMap) {
        List<Map<String, String>> endpointProviderList = null;
        final String className = endpointUrlMap.get("className");
        final String methodName = endpointUrlMap.get("methodName");
        try {
            final Method method = Class.forName(className).getMethod(methodName, (Class<?>[]) null);
            Object returnObj = method.invoke(null, (Object[]) null);

            if (returnObj instanceof List<?>) {
                endpointProviderList = (List<Map<String, String>>) returnObj;
            }
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception: " + ex.getMessage());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "endpoint providers: " + endpointProviderList);
        }
        return endpointProviderList;
    }

    private Map<String, String> getEndpointUrlMap(List<Map<String, String>> endpointProviderList) {
        Map<String, String> endpointUrlInMap = new HashMap<String, String>();
        for (Map<String, String> endpointUrlMapInList : endpointProviderList) {
            String provider = endpointUrlMapInList.get("name");
            if (provider == null) {
                provider = "default"; // use default as the provider name
            }
            for (Map.Entry<String, String> urlEntry : endpointUrlMapInList.entrySet()) {
                if (!"name".equals(urlEntry.getKey())) {
                    endpointUrlInMap.put(urlEntry.getKey() + "." + provider, urlEntry.getValue());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "endpointUrls: " + endpointUrlInMap);
        }
        return endpointUrlInMap;
    }

    private String[] getEndpointNameAndVersion(String featureName) {
        String[] nameAndVersion = null;
        Pattern pattern = Pattern.compile("(.*)(-\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(featureName);
        if (matcher.matches() && matcher.groupCount() == 2) {
            nameAndVersion = new String[2];
            nameAndVersion[0] = matcher.group(1);
            nameAndVersion[1] = matcher.group(2);
        }
        return nameAndVersion;
    }

    private String buildEndpointShortName(String initEndpointShortName, String providerName) {
        String returnEndpointShortName = initEndpointShortName + "." + providerName;
        String[] shortNameAndVersion = getEndpointNameAndVersion(initEndpointShortName);
        if (shortNameAndVersion != null) {
            returnEndpointShortName = shortNameAndVersion[0] + "." + providerName + shortNameAndVersion[1];
        }

        return returnEndpointShortName;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean isFeatureProvisioned(String featureToFind) {
        if (provisionerService != null) {
            return provisionerService.getInstalledFeatures().contains(featureToFind);
        }
        return false;

    }
}
