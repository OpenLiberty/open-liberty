/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.adapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.javaee.ddmodel.wsbnd.impl.WebservicesBndComponentImpl;
import com.ibm.ws.javaee.ddmodel.wsbnd.impl.WebservicesBndType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = ContainerAdapter.class,
           property = { "service.vendor=IBM", "toType=com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd" })
public final class WebservicesBndAdapter implements ContainerAdapter<WebservicesBnd> {

    private static final String WEBSERVICES_BND_ELEMENT_NAME = "webservices-bnd";
    private static final String MODULE_NAME_INVALID = "module.name.invalid";
    private static final String MODULE_NAME_NOT_SPECIFIED = "module.name.not.specified";
    private static final TraceComponent tc = Tr.register(WebservicesBndAdapter.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    volatile List<WebservicesBnd> configurations;

    private boolean isServer = true;

    @FFDCIgnore(ParseException.class)
    @Override
    public WebservicesBnd adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {

        Entry ddEntry = null;
        if (isServer) {

            if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleInfo.class) != null) {
                ddEntry = containerToAdapt.getEntry(WebservicesBnd.WEB_XML_BND_URI);
            } else if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), EJBModuleInfo.class) != null) {
                ddEntry = containerToAdapt.getEntry(WebservicesBnd.EJB_XML_BND_URI);
            }
        } else {
            //it is a client module and we need to get ibm-ws-bnd.xml from META-INF/
            ddEntry = containerToAdapt.getEntry(WebservicesBnd.EJB_XML_BND_URI);
        }
        WebservicesBndComponentImpl fromConfig = getConfigOverrides(rootOverlay, artifactContainer);
        if (ddEntry == null && fromConfig == null)
            return null;

        if (ddEntry != null) {
            try {
                WsClientBindingParser parser = new WsClientBindingParser(containerToAdapt, ddEntry);
                WebservicesBnd wsBind = parser.parse();
                if (fromConfig == null) {
                    return wsBind;
                } else {
                    fromConfig.setDelegate(wsBind);
                    return fromConfig;
                }
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }
        }

        return fromConfig;
    }

    private WebservicesBndComponentImpl getConfigOverrides(OverlayContainer overlay, ArtifactContainer artifactContainer) throws UnableToAdaptException {
        if (configurations == null || configurations.isEmpty())
            return null;

        ApplicationInfo appInfo = (ApplicationInfo) overlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
        ModuleInfo moduleInfo = null;
        if (appInfo == null) {
            moduleInfo = (ModuleInfo) overlay.getFromNonPersistentCache(artifactContainer.getPath(), ModuleInfo.class);
            if (moduleInfo == null)
                return null;
            appInfo = moduleInfo.getApplicationInfo();
        }
        NestedConfigHelper configHelper = null;
        if (appInfo != null && appInfo instanceof ExtendedApplicationInfo)
            configHelper = ((ExtendedApplicationInfo) appInfo).getConfigHelper();
        if (configHelper == null)
            return null;
		
		OverlayContainer rootOverlay = overlay;
		if (overlay.getParentOverlay() != null)
			rootOverlay = overlay.getParentOverlay();

        Set<String> configuredModuleNames = new HashSet<String>();
        String servicePid = (String) configHelper.get("service.pid");
        String extendsPid = (String) configHelper.get("ibm.extends.source.pid");
        for (WebservicesBnd config : configurations) {
            WebservicesBndComponentImpl configImpl = (WebservicesBndComponentImpl) config;
            String parentPid = (String) configImpl.getConfigAdminProperties().get("config.parentPID");
            if (servicePid.equals(parentPid) || parentPid.equals(extendsPid)) {
                if (moduleInfo == null)
                    return configImpl;
                String moduleName = (String) configImpl.getConfigAdminProperties().get("moduleName");
                if (moduleName == null) {
                    if (rootOverlay.getFromNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebservicesBndAdapter.class) == null) {
                        Tr.error(tc, "module.name.not.specified", WEBSERVICES_BND_ELEMENT_NAME);
                        rootOverlay.addToNonPersistentCache(MODULE_NAME_NOT_SPECIFIED, WebservicesBndAdapter.class, MODULE_NAME_NOT_SPECIFIED);
                    }
                    continue;
                }
                moduleName = stripExtension(moduleName);
                configuredModuleNames.add(moduleName);
                if (moduleInfo.getName().equals(moduleName))
                    return configImpl;
            }
        }
        if (moduleInfo != null && !configuredModuleNames.isEmpty()) {
            if (rootOverlay.getFromNonPersistentCache(MODULE_NAME_INVALID, WebservicesBndAdapter.class) == null) {
                HashSet<String> moduleNames = new HashSet<String>();
                // TODO: Based on the '(appInfo != null)' test, above, the
                //       appInfo could be null here.
                Application app = appInfo.getContainer().adapt(Application.class);
                for (Module m : app.getModules()) {
                    moduleNames.add(stripExtension(m.getModulePath()));
                }
                configuredModuleNames.removeAll(moduleNames);
                if (!configuredModuleNames.isEmpty())
                    Tr.error(tc, "module.name.invalid", configuredModuleNames, WEBSERVICES_BND_ELEMENT_NAME);
                rootOverlay.addToNonPersistentCache(MODULE_NAME_INVALID, WebservicesBndAdapter.class, MODULE_NAME_INVALID);
            }
        }
        return null;
    }

    private String stripExtension(String moduleName) {
        if (moduleName.endsWith(".war") || moduleName.endsWith(".jar")) {
            return moduleName.substring(0, moduleName.length() - 4);
        }
        return moduleName;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        isServer = "server".equals(cc.getBundleContext().getProperty("wlp.process.type"));
    }

    protected void deactivate(ComponentContext cc) {
        // EMPTY
    }

    /**
     * DDParser for webservices.xml
     */
    private static final class WsClientBindingParser extends DDParser {

        public WsClientBindingParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
            super(ddRootContainer, ddEntry);
        }

        WebservicesBnd parse() throws ParseException {
            super.parseRootElement();
            return (WebservicesBnd) rootParsable;
        }

        @Override
        protected ParsableElement createRootParsable() throws ParseException {
            if (WEBSERVICES_BND_ELEMENT_NAME.equals(rootElementLocalName)) {
                return createXMLRootParsable();
            }
            return null;
        }

        private DDParser.ParsableElement createXMLRootParsable() throws ParseException {
            if (namespace == null) {
                throw new ParseException(unknownDeploymentDescriptorVersion());
            }
            if ("http://websphere.ibm.com/xml/ns/javaee".equals(namespace)) {
                version = 10;
                return new WebservicesBndType(getDeploymentDescriptorPath());
            } else {
                throw new ParseException(unknownDeploymentDescriptorVersion());
            }
        }
    }
}
