/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// ************************************************************
// THIS FILE HAS BEEN UPDATED SINCE IT WAS GENERATED.
// ANY NEWLY GENERATED CODE MUST BE CAREFULLY MERGED WITH
// THIS CODE.
// ************************************************************

package com.ibm.ws.javaee.ddmodel.webext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = ContainerAdapter.class,
           property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.dd.webext.WebExt" })
public class WebExtAdapter implements ContainerAdapter<com.ibm.ws.javaee.dd.webext.WebExt> {

    private static final TraceComponent tc = Tr.register(WebExtAdapter.class);

    //

    /** Tag marking that an invalid module name was detected. */
    private static final String MODULE_NAME_INVALID = "module.name.invalid";
    /** Tag marking that an missing module name was detected. */
    private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";

    /**
     * Mark an error condition to an overlay container.
     *
     * The tag is used as both the overlay key and the overlay value.
     *
     * Each mark is not placed relative to particular data. Each mark
     * may be placed at most once.
     *
     * @param overlay The overlay in which to place the mark.
     * @param errorTag The tag used as the mark.
     *
     * @return True or false telling if this is the first placement
     *     of the tag to the overlay.
     */
    private static boolean markError(OverlayContainer overlay, String errorTag) {
        if ( overlay.getFromNonPersistentCache(errorTag, WebExtAdapter.class) == null ) {
            overlay.addToNonPersistentCache(errorTag, WebExtAdapter.class, errorTag);
            return true;
        } else {
            return false;
        }
    }

    //

    /**
     * The list of web-extension configuration overrides.  These are keyed
     * by module name.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    volatile List<com.ibm.ws.javaee.dd.webext.WebExt> configurations;

    /**
     * Obtain the web extension for a module container.
     *
     * There are four possibilities:
     *
     * (1) The container has neither a web extension nor a configuration override of its
     * web extension.  In this case, answer null: No web extension is available.
     *
     * (2) The container has a web extension and does not have a configuration override
     * (of the web extension).  Answer the web extension.
     *
     * (3) The container does not have a web extension, but does have a configuration
     * override.  Answer the configuration override.
     *
     * (4) The container has both a web extension and a configuration override.  Answer
     * the configuration override with the web extension set as a delegate.
     *
     * @param root The module container.
     * @param rootOverlay The root overlay container.
     * @param artifactContainer The raw module container.
     * @param containerToAdapt The module container.
     *
     * @return The effective web extension of the module.  Null if no extension is
     *     available.
     *
     * @throws UnableToAdaptException Thrown in case of an error obtaining the
     *     effective web extension.  This will usually be a parse error or a
     *     naming error.
     */
    @Override
    @FFDCIgnore(ParseException.class)
    public com.ibm.ws.javaee.dd.webext.WebExt adapt(
        Container root,
        OverlayContainer rootOverlay,
        ArtifactContainer artifactContainer,
        Container containerToAdapt) throws UnableToAdaptException {

        // What web extension is stored depends on the web descriptor version:
        // either "ibm-web-ext.xmi" or "ibm-web-ext.xml".

        com.ibm.ws.javaee.dd.web.WebApp primary = containerToAdapt.adapt(com.ibm.ws.javaee.dd.web.WebApp.class);
        String primaryVersion = ((primary == null) ? null : primary.getVersion());
        boolean xmi = ( "2.2".equals(primaryVersion) || "2.3".equals(primaryVersion) || "2.4".equals(primaryVersion) );
        String ddEntryName;
        if ( xmi ) {
            ddEntryName = com.ibm.ws.javaee.dd.webext.WebExt.XMI_EXT_NAME;
        } else {
            ddEntryName = com.ibm.ws.javaee.dd.webext.WebExt.XML_EXT_NAME;
        }
        Entry ddEntry = containerToAdapt.getEntry(ddEntryName);

        com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl fromConfig =
            getConfigOverrides(rootOverlay, artifactContainer);

        // Neither the web-extension nor the configuration override is available:
        // Answer null.

        if ( (ddEntry == null) && (fromConfig == null) ) {
            return null;
        }

        if ( ddEntry != null ) {
            com.ibm.ws.javaee.dd.webext.WebExt fromApp;
            try {
                fromApp = new com.ibm.ws.javaee.ddmodel.webext.WebExtDDParser(containerToAdapt, ddEntry, xmi).parse();
            } catch ( ParseException e ) {
                throw new UnableToAdaptException(e);
            }

            if ( fromConfig == null ) {
                // Only the web extension is available.
                return fromApp;
            } else {  
                // Both are available: Answer the configuration override, with the
                // web extension set as a delegate.
                fromConfig.setDelegate(fromApp);
                return fromConfig;
            }

        } else {
            // Only the configuration override is available.
            return fromConfig;
        }
    }

    /**
     * Answer the web extension type configuration override of a module.
     *
     * @param overlay The overlay container of the module.
     * @param artifactContainer The raw container of the module.
     *
     * @return The web extension override of the module.
     *
     * @throws UnableToAdaptException Thrown if an error occured while
     *     accessing the non-persistent cache.
     */
    private com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl getConfigOverrides(
        OverlayContainer overlay,
        ArtifactContainer artifactContainer) throws UnableToAdaptException {

        // No match is possible, and no errors are possible, when there are no
        // web extension configuration overrides.

        if ( (configurations == null) || configurations.isEmpty() ) {
          return null;
        }

        // Maybe application information is directly available ...
        ApplicationInfo appInfo = (ApplicationInfo)
            overlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);

        // Or, maybe module information is available and application information can
        // be obtained from that ...
        ModuleInfo moduleInfo = null;
        if ( appInfo == null ) {
            moduleInfo = (ModuleInfo) overlay.getFromNonPersistentCache(artifactContainer.getPath(), ModuleInfo.class);
            if ( moduleInfo == null ) {
               return null;
            }
            appInfo = moduleInfo.getApplicationInfo();
            if ( appInfo == null ) {
                return null;
            }
        }

        // Need a configuration helper ... but that is only available
        // if the application information is of supported type.
        if ( !(appInfo instanceof ExtendedApplicationInfo) ) {
            return null;
        }

        NestedConfigHelper configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if ( configHelper == null ) {
          return null;
        }

        // If the module is a bare module, it's overlay is the root overlay.
        // If the module is nested, the root overlay is the parent of the
        // module's overlay.
        //
        // The root overlay is used for marking web extension override errors:
        // The errors, "module.name.invalid" and "module.name.not.specified"
        // are possible.  Note that each of these is recorded at most once,
        // regardless of how many invalid overrides are present.

        OverlayContainer rootOverlay = overlay;
        if ( overlay.getParentOverlay() != null ) {
            rootOverlay = overlay.getParentOverlay();
        }
	  
        // The configurations were injected by the service machinery.
        // Loop through these to fine one which matches the target module name.
        // Generate errors along the way.
        //
        // What errors are generated depends on the order of processing.
        //
        // A web extension override which does not have a name is invalid, but
        // that won't be detected unless there is a module which matches an override
        // which follows the invalid override.
        //
        // Note that at most one error is generated, regardless of how many web
        // module overrides are missing module names.

        Set<String> overrideModuleNames = new HashSet<String>();

        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");

        for ( com.ibm.ws.javaee.dd.webext.WebExt config : configurations ) {
            com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl configImpl =
                (com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl) config;

            String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
            if ( !servicePid.equals(parentPid) && !parentPid.equals(extendsPid)) {
                continue;
            }

            if ( moduleInfo == null ) {
                return configImpl;
            }

            // Try to match the web extension overrides to the current module based on
            // the module name.
            //
            // Build up a list of the web extension override module names.  Those are
            // are used if no matching override is found for the current module.
            //
            // A web extension override must supply a module name.  If one is detected,
            // emit an error, and continue on to the next override.

            String overrideModuleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
            if ( overrideModuleName == null ) {
                if ( markError(rootOverlay, MODULE_NAME_NOT_SPECIFIED) ) {
                    Tr.error(tc, "module.name.not.specified", "web-ext" );
                }
                continue;
            }
            overrideModuleName = stripExtension(overrideModuleName);
            overrideModuleNames.add(overrideModuleName);

            if ( moduleInfo.getName().equals(overrideModuleName) ) {
                return configImpl;
            }
        }

        // If we got this far, no matching override was located.
        //
        // Not finding a web extension override is considered to be an error --
        // if there are any web extension overrides which are specified to
        // modules not named in the application descriptor.

        if ( (moduleInfo != null) && !overrideModuleNames.isEmpty() ) {
            if ( markError(rootOverlay, MODULE_NAME_INVALID) ) {
                Application appDD = appInfo.getContainer().adapt(Application.class);
                if ( appDD != null ) {
                    Set<String> moduleDDNames = new HashSet<String>();
                    for ( Module moduleDD : appDD.getModules() ) {
                        String moduleDDName = stripExtension( moduleDD.getModulePath() );
                        moduleDDNames.add(moduleDDName);
                    }
                    overrideModuleNames.removeAll(moduleDDNames);
                }

                if ( !overrideModuleNames.isEmpty() ) {
                    Tr.error(tc, "module.name.invalid", overrideModuleNames, "web-ext");
                }
            }
        }

        return null;
    }

    /**
     * Strip the extension from a module name.
     *
     * Remove only ".war" or ".jar", and only if these are present in lower case.
     *
     * The module name is as obtained from an application descriptor, or from a web
     * extension override, or from module information.  Module names are usually,
     * but not always, simple file names.  Module names can be relative paths.
     *
     * @param moduleName The module name to strip.
     *
     * @return The module name with its extension removed.
     */
    private String stripExtension(String moduleName) {
        if ( moduleName.endsWith(".war") || moduleName.endsWith(".jar") ) {
            return moduleName.substring(0, moduleName.length() - 4);
        }
        return moduleName;
    }
}
