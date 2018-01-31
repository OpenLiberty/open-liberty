/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.internal.cmdline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.AssetType;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.cmdline.InstallExecutor;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.resources.ApplicableToProduct;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;

import wlp.lib.extract.SelfExtractor;

/**
 * This API contains methods to execute the Find Action.
 */
public class FindAction implements ActionHandler {

    static final protected Logger logger = InstallLogUtils.getInstallLogger();

    private String searchStr;
    private boolean showDescriptions;
    private AssetType assetType = AssetType.all;
    private String name;
    private String fromDir;
    private boolean isDebug = false;
    private boolean isBadConnectionFound = false;

    ReturnCode initialize(Arguments args) {

        String verboseLevel = args.getOption("verbose");
        isDebug = verboseLevel != null && verboseLevel.equalsIgnoreCase("debug");

        searchStr = "";
        if (!args.getPositionalArguments().isEmpty()) {
            if (args.getPositionalArguments().size() > 1) {
                logger.log(Level.SEVERE, CmdUtils.getMessage("ERROR_MORE_THAN_0_OR_1_ARGUMENTS", "find", args.getPositionalArguments().size()));
                return ReturnCode.BAD_ARGUMENT;
            }
            searchStr = args.getPositionalArguments().get(0);
        }
        String t = args.getOption("type");
        if (t != null && !t.isEmpty()) {
            try {
                assetType = AssetType.valueOf(t);
            } catch (Exception e) {
                logger.log(Level.SEVERE, CmdUtils.getMessage("ERROR_TYPE_INVALID_OPTION", t));
                return ReturnCode.BAD_ARGUMENT;
            }
        }
        if (args.getOption("name") != null) {
            if (searchStr.isEmpty()) {
                logger.log(Level.SEVERE, CmdUtils.getMessage("ERROR_NO_SEARCHSTRING_NAME_OPTION"));
                return ReturnCode.BAD_ARGUMENT;
            }
            name = searchStr;
            searchStr = "";
        }
        showDescriptions = args.getOption("showdescriptions") != null;
        fromDir = args.getOption("from");

        if (fromDir != null && fromDir.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_REQUIRED", "from"));
            return ReturnCode.BAD_ARGUMENT;
        }
        return ReturnCode.OK;
    }

    /**
     * Execute Find Action
     * return exit code upon completion
     */
    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        Properties repoProperties;
        ReturnCode rc = initialize(args);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }

        InstallKernel installKernel = InstallKernelFactory.getInstance();

        //Load the repository properties instance from properties file
        try {
            repoProperties = RepositoryConfigUtils.loadRepoProperties();
            if (repoProperties != null) {
                //Set the repository properties instance in Install Kernel
                installKernel.setRepositoryProperties(repoProperties);
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage());
            return InstallExecutor.returnCode(e.getRc());
        }

        installKernel.setUserAgent(InstallConstants.ASSET_MANAGER);

        //Get valid configured repositories and prompt user if authentication is required
        try {
            rc = CmdUtils.checkRepositoryStatus((InstallKernelInteractive) installKernel, repoProperties, "find", fromDir);
            if (rc.equals(ReturnCode.BAD_CONNECTION_FOUND)) {
                rc = ReturnCode.OK;
                isBadConnectionFound = true;
            }
            if (rc.equals(ReturnCode.USER_ABORT)) {
                return rc;
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage());
            return InstallExecutor.returnCode(e.getRc());
        }

        try {
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_SEARCHING"));
            logger.log(Level.INFO, "");

            // temporarily workaround to queryDirectoryBasedRepo
            Map<ResourceType, List<RepositoryResource>> assets = queryDirectoryRepo(installKernel.getLoginInfo());

            Map<ResourceType, List<RepositoryResource>> remoteAssets = ((InstallKernelImpl) installKernel).queryAssets(searchStr, assetType);

            merge(assets, remoteAssets);

            if (assets.isEmpty()) {
                logger.log(Level.INFO, CmdUtils.getMessage("MSG_NO_ASSET_FIND"));
            } else {
                log(assets);
                if (name == null) {
                    showAllResults(assets);
                } else {
                    boolean shown = showResult(assets, name);
                    if (!shown) {
                        logger.log(Level.INFO, CmdUtils.getMessage("MSG_NO_ASSET_FIND"));
                    }
                }
            }
        } catch (InstallException e) {
            InstallException newError = CmdUtils.convertToBadConnectionError(e, isBadConnectionFound);
            logger.log(Level.SEVERE, newError.getMessage(), newError);
            return InstallExecutor.returnCode(newError.getRc());
        }
        return ReturnCode.OK;
    }

    private void log(Map<ResourceType, List<RepositoryResource>> assets) {
        if (isDebug) {
            for (List<RepositoryResource> mrList : assets.values()) {
                InstallUtils.log(mrList);
            }
        }
    }

    private boolean searchDescription(String description) {
        if (description != null && !description.trim().isEmpty()) {
            String[] search = searchStr.split(" ");
            String lowerCaseDesc = description.toLowerCase();
            for (String s : search) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty() && lowerCaseDesc.contains(trimmed.toLowerCase()))
                    return true;
            }
        }
        return false;
    }

    private boolean match(RepositoryResource mr) {
        if (searchStr == null || searchStr.trim().isEmpty())
            return true;
        String name = mr.getName();
        if (name != null && name.toLowerCase().contains(searchStr.toLowerCase())) {
            return true;
        }
        if (searchDescription(mr.getDescription()))
            return true;
        String description = mr instanceof EsaResource ? ((EsaResource) mr).getShortDescription() : mr instanceof SampleResource ? ((SampleResource) mr).getShortDescription() : null;
        return searchDescription(description);
    }

    private boolean isApplicable(RepositoryResource mr) {
        ResourceType type = mr.getType();
        if (ResourceType.FEATURE.equals(type)) {
            Visibility v = ((EsaResource) mr).getVisibility();
            if (!v.equals(Visibility.PUBLIC) && !v.equals(Visibility.INSTALL))
                return false;
            if (v.equals(Visibility.INSTALL))
                type = ResourceType.ADDON;
        }
        boolean typeMatched = assetType.equals(AssetType.all) ||
                              assetType.equals(AssetType.feature) && ResourceType.FEATURE.equals(type) ||
                              assetType.equals(AssetType.addon) && ResourceType.ADDON.equals(type) ||
                              assetType.equals(AssetType.sample) && ResourceType.PRODUCTSAMPLE.equals(type) ||
                              assetType.equals(AssetType.opensource) && ResourceType.OPENSOURCE.equals(type);
        if (!typeMatched)
            return false;

        // AppliesTo is null if the resource is not an ApplicableToProduct
        String appliesTo = null;
        if (mr instanceof ApplicableToProduct) {
            appliesTo = ((ApplicableToProduct) mr).getAppliesTo();
        }
        @SuppressWarnings("rawtypes")
        List productMatchers = SelfExtractor.parseAppliesTo(appliesTo);
        wlp.lib.extract.ReturnCode validInstallRC = SelfExtractor.validateProductMatches(Utils.getInstallDir(), productMatchers);
        if (validInstallRC != wlp.lib.extract.ReturnCode.OK) {
            logger.log(Level.FINEST, mr.getName() + " is not applicable: " + appliesTo);
            return false;
        }
        return true;
    }

    private Map<ResourceType, List<RepositoryResource>> queryDirectoryRepo(RepositoryConnectionList loginInfo) {
        Map<ResourceType, List<RepositoryResource>> results = new HashMap<ResourceType, List<RepositoryResource>>();
        List<RepositoryResource> addOns = new ArrayList<RepositoryResource>();
        List<RepositoryResource> features = new ArrayList<RepositoryResource>();
        List<RepositoryResource> samples = new ArrayList<RepositoryResource>();
        List<RepositoryResource> openSources = new ArrayList<RepositoryResource>();

        if (loginInfo == null)
            return results;
        for (RepositoryConnection rc : loginInfo) {
            if (rc instanceof DirectoryRepositoryConnection || rc instanceof ZipRepositoryConnection) {
                try {
                    logger.log(Level.FINEST, "query directory repository " + rc.getRepositoryLocation());
                    Collection<? extends RepositoryResource> resources = new RepositoryConnectionList(rc).getAllResources();
                    for (RepositoryResource mr : resources) {
                        if (mr instanceof EsaResource || mr instanceof SampleResource) {
                            ResourceType t = mr.getType();
                            if (ResourceType.FEATURE.equals(t)) {
                                if (((EsaResource) mr).getVisibility().equals(Visibility.INSTALL))
                                    t = ResourceType.ADDON;
                            }
                            if (match(mr) && isApplicable(mr)) {
                                if (ResourceType.FEATURE.equals(t)) {
                                    if (!InstallUtils.contains(features, mr)) {
                                        features.add(mr);
                                    }
                                } else if (ResourceType.ADDON.equals(t)) {
                                    if (!InstallUtils.contains(addOns, mr)) {
                                        addOns.add(mr);
                                    }
                                } else if (ResourceType.PRODUCTSAMPLE.equals(t)) {
                                    if (!InstallUtils.contains(samples, mr)) {
                                        samples.add(mr);
                                    }
                                } else if (ResourceType.OPENSOURCE.equals(t)) {
                                    if (!InstallUtils.contains(openSources, mr)) {
                                        openSources.add(mr);
                                    }
                                }
                            }
                        }
                    }
                } catch (RepositoryBackendException e) {
                    String msg = e.getFailingConnection() == null ? "" : " from " + e.getFailingConnection().getRepositoryLocation();
                    logger.log(Level.FINEST, "Failed to get resources" + msg + ". Reason: " + e.getMessage());
                }
            }
        }

        if (!addOns.isEmpty())
            results.put(ResourceType.ADDON, addOns);
        if (!features.isEmpty())
            results.put(ResourceType.FEATURE, features);
        if (!samples.isEmpty())
            results.put(ResourceType.PRODUCTSAMPLE, samples);
        if (!openSources.isEmpty())
            results.put(ResourceType.OPENSOURCE, openSources);
        return results;
    }

    private void merge(Map<ResourceType, List<RepositoryResource>> assets, Map<ResourceType, List<RepositoryResource>> remoteAssets) {
        for (Entry<ResourceType, List<RepositoryResource>> remote : remoteAssets.entrySet()) {
            ResourceType t = remote.getKey();
            List<RepositoryResource> mrList = assets.get(t);
            if (mrList == null) {
                assets.put(t, remote.getValue());
            } else {
                for (RepositoryResource remoteMr : remote.getValue()) {
                    if (!InstallUtils.contains(mrList, remoteMr)) {
                        mrList.add(remoteMr);
                    }
                }
            }
        }
    }

    private boolean matchName(EsaResource esa, String name) {
        String shortName = esa.getShortName();
        if (shortName != null && shortName.equalsIgnoreCase(name)) {
            return true;
        } else {
            String provideFeature = esa.getProvideFeature();
            if (provideFeature != null && provideFeature.equals(name)) {
                return true;
            } else {
                String displayName = esa.getName();
                if (displayName != null && displayName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchName(SampleResource sr, String name) {
        String shortName = sr.getShortName();
        if (shortName != null && shortName.equalsIgnoreCase(name)) {
            return true;
        } else {
            String displayName = sr.getName();
            if (displayName != null && displayName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean showResult(Map<ResourceType, List<RepositoryResource>> assets, String name) {
        boolean shown = false;
        // Addons
        List<RepositoryResource> addons = assets.get(ResourceType.ADDON);
        if (addons != null) {
            for (RepositoryResource addon : addons) {
                EsaResource esa = (EsaResource) addon;
                if (matchName(esa, name)) {
                    showESA(esa, "addon", showDescriptions);
                    shown = true;
                }
            }
        }

        // Features
        List<RepositoryResource> features = assets.get(ResourceType.FEATURE);
        if (features != null) {
            for (RepositoryResource feature : features) {
                EsaResource esa = (EsaResource) feature;
                if (matchName(esa, name)) {
                    showESA(esa, "feature", showDescriptions);
                    shown = true;
                }
            }
        }

        // Samples
        List<RepositoryResource> samples = assets.get(ResourceType.PRODUCTSAMPLE);
        if (samples != null) {
            for (RepositoryResource sample : samples) {
                SampleResource sr = (SampleResource) sample;
                if (matchName(sr, name)) {
                    showSample(sr, "sample", showDescriptions);
                    shown = true;
                }
            }
        }

        // OpenSources
        List<RepositoryResource> openSources = assets.get(ResourceType.OPENSOURCE);
        if (openSources != null) {
            for (RepositoryResource openSource : openSources) {
                SampleResource sr = (SampleResource) openSource;
                if (matchName(sr, name)) {
                    showSample(sr, "opensource", showDescriptions);
                    shown = true;
                }
            }
        }

        return shown;
    }

    private void showAllResults(Map<ResourceType, List<RepositoryResource>> assets) {
        // Addons
        List<RepositoryResource> addons = assets.get(ResourceType.ADDON);
        if (addons != null) {
            Collections.sort(addons, new Comparator<RepositoryResource>() {
                @Override
                public int compare(RepositoryResource mr1, RepositoryResource mr2) {
                    return getName((EsaResource) mr1).compareTo(getName((EsaResource) mr2));
                }
            });
            for (RepositoryResource esa : addons) {
                if (esa instanceof EsaResource)
                    showESA((EsaResource) esa, "addon", showDescriptions);
                else
                    logger.log(Level.FINEST, "Unexpected addon: " + esa.getName() + " - " + esa.getShortDescription());
            }
        }

        // Features
        List<RepositoryResource> features = assets.get(ResourceType.FEATURE);
        if (features != null) {
            Collections.sort(features, new Comparator<RepositoryResource>() {
                @Override
                public int compare(RepositoryResource mr1, RepositoryResource mr2) {
                    return getName((EsaResource) mr1).compareTo(getName((EsaResource) mr2));
                }
            });
            for (RepositoryResource esa : features) {
                if (esa instanceof EsaResource)
                    showESA((EsaResource) esa, "feature", showDescriptions);
                else
                    logger.log(Level.FINEST, "Unexpected feature: " + esa.getName() + " - " + esa.getShortDescription());
            }
        }

        // Samples
        List<RepositoryResource> samples = assets.get(ResourceType.PRODUCTSAMPLE);
        if (samples != null) {
            Collections.sort(samples, new Comparator<RepositoryResource>() {
                @Override
                public int compare(RepositoryResource mr1, RepositoryResource mr2) {
                    return mr1.getName().compareTo(mr2.getName());
                }
            });
            for (RepositoryResource sample : samples) {
                if (sample instanceof SampleResource)
                    showSample((SampleResource) sample, "sample", showDescriptions);
                else
                    logger.log(Level.FINEST, "Unexpected sample: " + sample.getName() + " - " + sample.getShortDescription());
            }
        }

        // OpenSources
        List<RepositoryResource> openSources = assets.get(ResourceType.OPENSOURCE);
        if (openSources != null) {
            Collections.sort(openSources, new Comparator<RepositoryResource>() {
                @Override
                public int compare(RepositoryResource mr1, RepositoryResource mr2) {
                    return mr1.getName().compareTo(mr2.getName());
                }
            });
            for (RepositoryResource openSource : openSources) {
                if (openSource instanceof SampleResource)
                    showSample((SampleResource) openSource, "opensource", showDescriptions);
                else
                    logger.log(Level.FINEST, "Unexpected opensource: " + openSource.getName() + " - " + openSource.getShortDescription());
            }
        }
    }

    private String getName(EsaResource esa) {
        String shortName = esa.getShortName();
        if (shortName != null && !shortName.isEmpty())
            return shortName;
        return esa.getProvideFeature();
    }

    private void showESA(EsaResource esa, String type, boolean viewInfo) {
        if (!viewInfo) {
            logger.log(Level.INFO, type + " : " + getName(esa) + " : " + esa.getName());
            return;
        }
        logger.log(Level.INFO, getName(esa));
        logger.log(Level.INFO, NLS.getMessage("find.view.info.name", esa.getName()));
        logger.log(Level.INFO, NLS.getMessage("find.view.info.symbolic.name", esa.getProvideFeature()));
        String description = esa.getShortDescription();
        if (description != null && !description.isEmpty()) {
            description = NLS.getMessage("find.view.info.description", description);
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new StringReader(description));
            try {
                for (String line; (line = reader.readLine()) != null;) {
                    InstallUtils.wordWrap(sb, line, "        ");
                }
                logger.log(Level.INFO, sb.toString());
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        Collection<String> requiredFeatures = esa.getRequireFeature();
        if (requiredFeatures != null && !requiredFeatures.isEmpty()) {
            logger.log(Level.INFO, NLS.getMessage("find.view.info.enabled.by"));
            for (String requiredFeature : requiredFeatures) {
                logger.log(Level.INFO, "        " + requiredFeature);
            }
        }
        logger.log(Level.INFO, "");
    }

    private void showSample(SampleResource sample, String type, boolean viewInfo) {
        String shortName = sample.getShortName();
        if (shortName == null) {
            shortName = CmdUtils.getMessage("MSG_NO_NAME");
        }
        if (!viewInfo) {
            logger.log(Level.INFO, type + " : " + shortName + " : " + sample.getName());
            return;
        }
        logger.log(Level.INFO, shortName);
        logger.log(Level.INFO, NLS.getMessage("find.view.info.name", sample.getName()));
        String description = sample.getShortDescription();
        if (description != null && !description.isEmpty()) {
            description = NLS.getMessage("find.view.info.description", description);
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new StringReader(description));
            try {
                for (String line; (line = reader.readLine()) != null;) {
                    InstallUtils.wordWrap(sb, line, "        ");
                }
                logger.log(Level.INFO, sb.toString());
            } catch (IOException e) {
                logger.log(Level.SEVERE, description);
            }
        }
        Collection<String> requiredFeatures = sample.getRequireFeature();
        if (requiredFeatures != null && !requiredFeatures.isEmpty()) {
            logger.log(Level.INFO, NLS.getMessage("find.view.info.enabled.by"));
            for (String requiredFeature : requiredFeatures) {
                logger.log(Level.INFO, "        " + requiredFeature);
            }
        }
        logger.log(Level.INFO, "");
    }
}
