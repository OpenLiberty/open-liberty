/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.config.resource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.spi.FaceletConfigResourceProvider;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;

public class LibertyFaceletConfigResourceProvider extends FaceletConfigResourceProvider {
    private static final String CLASS_NAME = LibertyFaceletConfigResourceProvider.class.getName();
    protected static final Logger logger = Logger.getLogger(CLASS_NAME);

    private static final String META_INF_PREFIX = "META-INF/";
    private static final String FACELET_TAGLIB_SUFFIX = ".taglib.xml";

    private FaceletConfigResourceProvider defaultProvider;

    public LibertyFaceletConfigResourceProvider() {
        super();
    }

    public LibertyFaceletConfigResourceProvider(FaceletConfigResourceProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    @Override
    public Collection<URL> getFaceletTagLibConfigurationResources(
                                                                  ExternalContext context) throws IOException {
        final String method = "getFaceletTagLibConfigurationResources";
        Collection<URL> urlSet = null;

        boolean isOSGIApp = false;
        if (context.getApplicationMap().get("osgi-bundlecontext") != null) {
            isOSGIApp = true;
        }

        //find it the old way
        //delegate the primary classpath search to the default provider
        urlSet = defaultProvider.getFaceletTagLibConfigurationResources(context);

        if (isOSGIApp) {
            Object o = context.getContext();
            ServletContextFacade sc = null;
            if (o instanceof ServletContextFacade) {
                sc = (ServletContextFacade) o;
                //it should be a WebApp

                Container c = sc.getIServletContext().getModuleContainer();
                if (c != null) {
                    urlSet = getFaceletTagLibConfigResourcesHelper(c, urlSet);
                }
            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, method, "OSGI app without a Servlet Context");
                }
            }
        }

        return urlSet;
    }

    private Collection<URL> getFaceletTagLibConfigResourcesHelper(Container c, Collection<URL> urlSet) {
        final String method = "getFaceletTagLibConfigResourcesHelper";
        if (urlSet == null) {
            urlSet = new ArrayList<URL>();
        }
        try {
            List<Container> jarResourceContainers = new ArrayList<Container>();
            WebModuleClassesInfo classesInfo = c.adapt(WebModuleClassesInfo.class);
            int numberOfJars = 0;
            if (classesInfo != null) {
                List<ContainerInfo> containerInfos = classesInfo.getClassesContainers();
                for (ContainerInfo containerInfo : containerInfos) {
                    if (containerInfo.getType() == Type.WEB_INF_LIB) {
                        jarResourceContainers.add(containerInfo.getContainer());
                        numberOfJars++;
                    }
                }
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, method, "Checking " + numberOfJars + " jars for META-INF/*.taglib.xml");
            }
            for (Container jarContainer : jarResourceContainers) {
                Entry metaInf = jarContainer.getEntry(META_INF_PREFIX);
                if (metaInf != null) {
                    Container metaInfContainer = metaInf.adapt(Container.class);
                    for (Entry entry : metaInfContainer) {
                        //does this need to be recursive?  See TagLibraryCache.loadTldsFromContainerRecursive
                        //NO - according to the spec: "and placing the file in the META-INF directory in the jar file."
                        String name = entry.getName();
                        if (name.endsWith(FACELET_TAGLIB_SUFFIX)) {
                            URL urlObject = entry.getResource();
                            urlSet.add(urlObject);
                            if (logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, method, "Adding " + urlObject.toString());
                            }
                        }
                    }
                }
            }
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
        return urlSet;
    }

}
