/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 * Tracks the BDAs which have been discovered so far and tests whether an archive has already been added as an accessible BDA.
 * <p>
 * This is necessary to avoid adding a new BDA for module libraries if the same archive is already visible to the module via another mechanism.
 * <p>
 * E.g. if three EJB modules all reference the same library from their classpath, we need to be careful. Each EJB module can see everything visible to all the other EJB modules
 * so if we created a BDA each time the library is referenced, each EJB module would see all three copies resulting in problems resolving beans.
 */
class DiscoveredBdas {

    private static final TraceComponent tc = Tr.register(DiscoveredBdas.class);
    private final Map<ArchiveType, Set<WebSphereBeanDeploymentArchive>> bdasByType;
    private final Set<String> libraryPaths;
    private final Set<String> modulePaths;
    private final WebSphereCDIDeployment webSphereCDIDeployment;

    public DiscoveredBdas(WebSphereCDIDeployment webSphereCDIDeployment) {
        this.webSphereCDIDeployment = webSphereCDIDeployment;
        bdasByType = new HashMap<ArchiveType, Set<WebSphereBeanDeploymentArchive>>();
        ArchiveType[] types = { ArchiveType.SHARED_LIB, ArchiveType.EAR_LIB, ArchiveType.RAR_MODULE, ArchiveType.EJB_MODULE,
                                ArchiveType.WEB_MODULE, ArchiveType.CLIENT_MODULE };
        for (ArchiveType type : types) {
            bdasByType.put(type, new HashSet<WebSphereBeanDeploymentArchive>());
        }
        libraryPaths = new HashSet<String>();
        modulePaths = new HashSet<String>();
    }

    /**
     * Returns true if the given archive is accessible to all modules of the given module type
     * <ul>
     * <li>EAR and shared libraries are available to all modules</li>
     * <li>EJB and RAR modules are accessible to all other EJB modules, RAR modules and Web modules</li>
     * <li>Web and App Client modules are not accessible to other modules</li>
     * </ul>
     *
     * @param archiveType the module type
     * @param archive an archive
     * @return true if the archive is available to all modules of archiveType, otherwise false
     * @throws CDIException
     */
    public boolean isAlreadyAccessible(ArchiveType archiveType, CDIArchive archive) throws CDIException {
        String path = archive.getPath();
        if (libraryPaths.contains(path)) {
            return true;
        } else if (archiveType == ArchiveType.EAR_LIB || archiveType == ArchiveType.RAR_MODULE || archiveType == ArchiveType.EJB_MODULE
                   || archiveType == ArchiveType.WEB_MODULE || archiveType == ArchiveType.CLIENT_MODULE) {
            return modulePaths.contains(path);
        } else {
            return false;
        }
    }

    /**
     * Registers a newly discovered BDA and adds it to the deployment
     *
     * @param moduleType the module type
     * @param bda the new BDA
     * @throws CDIException
     */
    public void addDiscoveredBda(ArchiveType moduleType, WebSphereBeanDeploymentArchive bda) throws CDIException {
        webSphereCDIDeployment.addBeanDeploymentArchive(bda);
        Set<WebSphereBeanDeploymentArchive> bdaSet = bdasByType.get(moduleType);
        if (bdaSet != null) {
            bdaSet.add(bda);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignore this type: {0}, as CDI does not need to add this in the addDiscoveredBda ", moduleType);
            }
        }

        String path = bda.getArchive().getPath();
        if (moduleType == ArchiveType.SHARED_LIB || moduleType == ArchiveType.EAR_LIB) {
            libraryPaths.add(path);
        } else if (moduleType == ArchiveType.RAR_MODULE || moduleType == ArchiveType.EJB_MODULE) {
            modulePaths.add(path);
        }
    }

    /**
     * The method should be called last to wire all libraries and modules
     */
    public void makeCrossBoundaryWiring() throws CDIException {
        Collection<WebSphereBeanDeploymentArchive> sharedLibs = bdasByType.get(ArchiveType.SHARED_LIB);
        Collection<WebSphereBeanDeploymentArchive> earLibs = bdasByType.get(ArchiveType.EAR_LIB);
        Collection<WebSphereBeanDeploymentArchive> rarModules = bdasByType.get(ArchiveType.RAR_MODULE);
        Collection<WebSphereBeanDeploymentArchive> ejbModules = bdasByType.get(ArchiveType.EJB_MODULE);
        Collection<WebSphereBeanDeploymentArchive> warModules = bdasByType.get(ArchiveType.WEB_MODULE);
        Collection<WebSphereBeanDeploymentArchive> clientModules = bdasByType.get(ArchiveType.CLIENT_MODULE);
        Collection<WebSphereBeanDeploymentArchive> allAccessibleBdas = new HashSet<WebSphereBeanDeploymentArchive>();
        allAccessibleBdas.addAll(sharedLibs);
        allAccessibleBdas.addAll(earLibs);
        allAccessibleBdas.addAll(ejbModules);
        allAccessibleBdas.addAll(rarModules);

        wireBdas(earLibs, allAccessibleBdas);
        wireBdas(rarModules, allAccessibleBdas);
        wireBdas(ejbModules, allAccessibleBdas);
        wireBdas(warModules, allAccessibleBdas);
        wireBdas(clientModules, allAccessibleBdas);
        wireBdasBasedOnClassLoader(earLibs, warModules);
    }

    /**
     * Make a wire from the group of bdas to a set of destination bdas
     *
     * @param wireFromBdas the wiring from bda
     * @param wireToBdas the wiring destination bda
     */
    private void wireBdas(Collection<WebSphereBeanDeploymentArchive> wireFromBdas, Collection<WebSphereBeanDeploymentArchive> wireToBdas) {
        for (WebSphereBeanDeploymentArchive wireFromBda : wireFromBdas) {
            Collection<BeanDeploymentArchive> accessibleBdas = wireFromBda.getBeanDeploymentArchives();
            for (WebSphereBeanDeploymentArchive wireToBda : wireToBdas) {
                if ((wireToBda != wireFromBda) && ((accessibleBdas == null) || !accessibleBdas.contains(wireToBda))) {
                    wireFromBda.addBeanDeploymentArchive(wireToBda);
                }
            }
        }
    }

    private void wireBdasBasedOnClassLoader(Collection<WebSphereBeanDeploymentArchive> wireFromBdas, Collection<WebSphereBeanDeploymentArchive> wireToBdas) throws CDIException {
        for (WebSphereBeanDeploymentArchive wireFromBda : wireFromBdas) {
            Collection<BeanDeploymentArchive> accessibleBdas = wireFromBda.getBeanDeploymentArchives();
            for (WebSphereBeanDeploymentArchive wireToBda : wireToBdas) {
                if ((wireToBda != wireFromBda) && ((accessibleBdas == null) || !accessibleBdas.contains(wireToBda))) {
                    CDIUtils.addWiring(wireFromBda, wireToBda);
                }
            }
        }
    }

    public boolean isExcluded(WebSphereBeanDeploymentArchive bda) {
        return bdasByType.get(ArchiveType.CLIENT_MODULE).contains(bda);
    }
}
