/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.equinox.module.internal;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.artifact.equinox.module.ModuleContainerFinder;
import com.ibm.wsspi.kernel.equinox.module.ModuleBundleFileFactory;
import com.ibm.wsspi.kernel.equinox.module.ModuleInfo;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * A module bundle file factory that creates bundle files that are backed by {@link Container containers}.
 * A {@link ModuleContainerFinder} service is used to locate containers for the bundle files this factory
 * creates.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class ModuleBundleFileFactoryImpl implements ModuleBundleFileFactory {
    ConcurrentServiceReferenceSet<ModuleContainerFinder> containerFinders = new ConcurrentServiceReferenceSet<ModuleContainerFinder>("ContainerFinder");
    Map<File, Container> nestedFiles = Collections.synchronizedMap(new HashMap<File, Container>());
    BundleContext context;

    @Activate
    protected void activate(ComponentContext ctx) {
        containerFinders.activate(ctx);
        context = ctx.getBundleContext();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        containerFinders.deactivate(ctx);
    }

    @Override
    public BundleFileWrapper createBundleFile(ModuleInfo moduleInfo) {
        // locate a container for the moduleInfo
        Container container = getContainer(moduleInfo);
        // only return a module bundle file if a container is found
        return (container == null) ? null : new ModuleBundleFile(container, moduleInfo.getBundleFile(), nestedFiles, context);
    }

    private Container getContainer(ModuleInfo moduleInfo) {
        if (moduleInfo.isBundleRoot()) {
            // This is a bundle root look in the container finders
            for (Iterator<ModuleContainerFinder> iFinders = containerFinders.getServices(); iFinders.hasNext();) {
                Container result = iFinders.next().findContainer(moduleInfo.getLocation());
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
        // this is a nested file, see if we have a container for it
        return nestedFiles.get(moduleInfo.getBundleFile().getBaseFile());
    }

    @Reference(service = ModuleContainerFinder.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setContainerFinder(ServiceReference<ModuleContainerFinder> ref) {
        containerFinders.addReference(ref);
    }

    protected void unsetContainerFinder(ServiceReference<ModuleContainerFinder> ref) {
        containerFinders.removeReference(ref);
    }
}
