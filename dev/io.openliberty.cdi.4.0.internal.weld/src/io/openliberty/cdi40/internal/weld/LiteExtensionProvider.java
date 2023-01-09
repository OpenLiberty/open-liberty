/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.weld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

/**
 * Provides the weld-lite-extension-translator extension when the deployed application has build compatible extensions
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class LiteExtensionProvider implements ExtensionArchiveProvider {
    private static final TraceComponent tc = Tr.register(LiteExtensionProvider.class);

    @Override
    public Collection<ExtensionArchive> getArchives(CDIRuntime cdiRuntime, WebSphereCDIDeployment deployment) throws CDIException {

        // Find all the build compatible extensions and sort them by their classloader
        Map<ClassLoader, Set<Class<? extends BuildCompatibleExtension>>> extensions = new HashMap<>();
        for (WebSphereBeanDeploymentArchive bda : deployment.getWebSphereBeanDeploymentArchives()) {
            for (String bceClassName : bda.getBuildCompatibleExtensionClassNames()) {
                Class<? extends BuildCompatibleExtension> bceClass = loadBuildCompatibleExtension(bceClassName, bda.getClassLoader());
                extensions.computeIfAbsent(bceClass.getClassLoader(), x -> new HashSet<>()).add(bceClass);
            }
        }

        // Create a LiteExtensionArchive for each distinct classloader which has build compatible extensions
        List<ExtensionArchive> newArchives = new ArrayList<>();
        for (Entry<ClassLoader, Set<Class<? extends BuildCompatibleExtension>>> entry : extensions.entrySet()) {
            ClassLoader cl = entry.getKey();
            List<Class<? extends BuildCompatibleExtension>> classes = new ArrayList<>(entry.getValue());
            newArchives.add(new LiteExtensionArchive(cdiRuntime, cl, classes));
        }

        return newArchives;
    }

    private Class<? extends BuildCompatibleExtension> loadBuildCompatibleExtension(String className, ClassLoader cl) throws CDIException {
        try {
            Class<?> clazz = cl.loadClass(className);
            return clazz.asSubclass(BuildCompatibleExtension.class);
        } catch (ClassNotFoundException e) {
            Tr.error(tc, "bce.not.loadable.CWOWB1013E", className);
            throw new CDIException(Tr.formatMessage(tc, "bce.not.loadable.CWOWB1013E", className), e);
        } catch (ClassCastException e) {
            Tr.error(tc, "bce.does.not.implement.bce.CWOWB1014E", className);
            throw new CDIException(Tr.formatMessage(tc, "bce.does.not.implement.bce.CWOWB1014E", className), e);
        }
    }

}
