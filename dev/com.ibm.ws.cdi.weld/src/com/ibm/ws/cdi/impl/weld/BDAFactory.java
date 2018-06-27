/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 * The implementation of Weld spi BeanDeploymentArchive to represent a CDI bean
 * archive.
 *
 *
 */
public class BDAFactory {

    /**
     * Need to cache runtime extension as they don't change per live server and the bda containing the same classes for every application
     */
    private static ConcurrentHashMap<String, Set<String>> runtimeBDAClasses = new ConcurrentHashMap<String, Set<String>>();

    public static WebSphereBeanDeploymentArchive createBDA(WebSphereCDIDeployment deployment,
                                                           String archiveID,
                                                           CDIArchive archive,
                                                           CDIRuntime cdiRuntime) throws CDIException {
        //this isn't really part of any EE module so we just use the archiveID which should be unique
        EEModuleDescriptor eeModuleDescriptor = new WebSphereEEModuleDescriptor(archiveID, archive.getType());
        return createBDA(deployment, archiveID, archive, cdiRuntime, eeModuleDescriptor);
    }

    public static WebSphereBeanDeploymentArchive createBDA(WebSphereCDIDeployment deployment,
                                                           String archiveID,
                                                           CDIArchive archive,
                                                           CDIRuntime cdiRuntime,
                                                           EEModuleDescriptor eEModuleDescriptor) throws CDIException {
        Set<String> additionalClasses = Collections.<String> emptySet();
        Set<String> additionalAnnotations = Collections.<String> emptySet();

        WebSphereBeanDeploymentArchive bda = createBDA(deployment,
                                                       archiveID,
                                                       archive,
                                                       cdiRuntime,
                                                       additionalClasses,
                                                       additionalAnnotations,
                                                       false,
                                                       false,
                                                       eEModuleDescriptor);
        return bda;
    }

    //only for extensions
    public static WebSphereBeanDeploymentArchive createBDA(WebSphereCDIDeployment deployment,
                                                           ExtensionArchive extensionArchive,
                                                           CDIRuntime cdiRuntime) throws CDIException {

        Set<String> additionalClasses = extensionArchive.getExtraClasses();
        Set<String> additionalAnnotations = extensionArchive.getExtraBeanDefiningAnnotations();
        boolean extensionCanSeeApplicationBDAs = extensionArchive.applicationBDAsVisible();

        boolean extClassesOnlyBDA = extensionArchive.isExtClassesOnly();
        String archiveID = deployment.getDeploymentID() + "#" + extensionArchive.getName() + ".additionalClasses";
        //this isn't really part of any EE module so we just use the archiveID which should be unique
        EEModuleDescriptor eeModuleDescriptor = new WebSphereEEModuleDescriptor(archiveID, extensionArchive.getType());

        WebSphereBeanDeploymentArchive bda = createBDA(deployment,
                                                       archiveID,
                                                       extensionArchive,
                                                       cdiRuntime,
                                                       additionalClasses,
                                                       additionalAnnotations,
                                                       extensionCanSeeApplicationBDAs,
                                                       extClassesOnlyBDA,
                                                       eeModuleDescriptor);
        return bda;
    }

    public static WebSphereBeanDeploymentArchive createBDA(WebSphereCDIDeployment deployment, OnDemandArchive onDemandArchive, CDIRuntime cdiRuntime) throws CDIException {

        String archiveID = deployment.getDeploymentID() + "#" + onDemandArchive.getName() + ".additionalClasses";
        return createBDA(deployment, archiveID, onDemandArchive, cdiRuntime);
    }

    private static WebSphereBeanDeploymentArchive createBDA(WebSphereCDIDeployment deployment,
                                                            String archiveID,
                                                            CDIArchive archive,
                                                            CDIRuntime cdiRuntime,
                                                            Set<String> additionalClasses,
                                                            Set<String> additionalBeanDefiningAnnotations,
                                                            boolean extensionCanSeeApplicationBDAs,
                                                            boolean extensionClassesOnlyBDA,
                                                            EEModuleDescriptor eEModuleDescriptor) throws CDIException {
        Set<String> extensionClassNames = archive.getExtensionClasses();
        Set<String> allClassNames = new HashSet<String>();
        if (archive.getType() == ArchiveType.RUNTIME_EXTENSION) {
            if (extensionClassesOnlyBDA) {
                // no need to scan as we just create a bda with the extension classes
                allClassNames.addAll(extensionClassNames);
            } else {
                // trying to see whether we have cached the classes before. If not, scan the bundle
                String key = archive.getName();
                Set<String> clazzes = runtimeBDAClasses.get(key);
                if (clazzes == null) {
                    clazzes = archive.getClassNames();
                    Set<String> result = runtimeBDAClasses.putIfAbsent(key, clazzes);
                    if (result == null) {
                        result = clazzes;
                    }
                    allClassNames.addAll(result);
                } else {
                    allClassNames.addAll(clazzes);
                }
            }
        } else {
            allClassNames.addAll(archive.getClassNames());

        }

        return new BeanDeploymentArchiveImpl(deployment, archiveID, archive, cdiRuntime, allClassNames, additionalClasses, additionalBeanDefiningAnnotations, extensionCanSeeApplicationBDAs, extensionClassNames, eEModuleDescriptor);
    }
}
