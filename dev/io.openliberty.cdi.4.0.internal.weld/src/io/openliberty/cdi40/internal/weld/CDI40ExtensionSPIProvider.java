/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;
import com.ibm.ws.cdi.internal.archive.liberty.ExtensionArchiveImpl;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveFactory;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.kernel.service.utils.ServiceReferenceUtils;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.cdi40.internal.extension.CDI40ExtensionMetadataInternal;

import org.jboss.weld.lite.extension.translator.LiteExtensionTranslator;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CDI40ExtensionSPIProvider implements ExtensionArchiveProvider, ExtensionArchiveFactory {
    private static final TraceComponent tc = Tr.register(CDI40ExtensionSPIProvider.class);

    /**
     * Provides the weld-lite-extension-translator extension when the deployed application has build compatible extensions
     */
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

    /**
     * Creates a CDI extension archive from an implimentation of CDIExtensionMetadata, the class may also implement CDIExtensionMetadataInternal to add options
     * only available to components of Liberty
     *
     * @return An ExtensionArchive with all the contents defined in CDIExtensionMetadata
     */
    public ExtensionArchive newSPIExtensionArchive(CDIRuntime cdiRuntime, ServiceReference<CDIExtensionMetadata> sr,
                                                    CDIExtensionMetadata webSphereCDIExtensionMetaData, WebSphereCDIDeployment applicationContext) throws CDIException {

        ExtensionArchive newExtensionArchive = CDIUtils.newSPIExtensionArchive(cdiRuntime, sr, webSphereCDIExtensionMetaData, applicationContext);
  
        Set<Class<? extends BuildCompatibleExtension>> buildCompatibleExtensionClasses = new HashSet<Class<? extends BuildCompatibleExtension>>();
        Set<String> buildCompatibleExtensionsClassNames = new HashSet<String>();
 
        if (webSphereCDIExtensionMetaData instanceof CDI40ExtensionMetadataInternal) {
            CDI40ExtensionMetadataInternal internalExtension = (CDI40ExtensionMetadataInternal) webSphereCDIExtensionMetaData;
            buildCompatibleExtensionClasses = internalExtension.getBuildCompatibleExtensions();
            buildCompatibleExtensionsClassNames = buildCompatibleExtensionClasses.stream().map(clazz -> clazz.getCanonicalName()).collect(Collectors.toSet());
        }

        if (! buildCompatibleExtensionClasses.isEmpty() && newExtensionArchive instanceof ExtensionArchiveImpl) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "newSPIExtensionArchive", "The previous extension is being ammended: The following classes will be registered as build compatible extensions: " + String.join(", ", buildCompatibleExtensionsClassNames));
            }

            LiteExtensionTranslator translator = new LiteExtensionTranslator(new ArrayList<Class<? extends BuildCompatibleExtension>>(buildCompatibleExtensionClasses), 
                                                                             webSphereCDIExtensionMetaData.getClass().getClassLoader());

            ExtensionArchiveImpl extensionArchiveImpl = (ExtensionArchiveImpl) newExtensionArchive;
            extensionArchiveImpl.addConstructedExtension(translator);
        }

        return newExtensionArchive;
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
