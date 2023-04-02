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

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.BuildCompatibleExtensionFinder;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.Resource;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

/**
 * Find CDI 4.0 Build Compatible Extensions
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CDI40BceFinder implements BuildCompatibleExtensionFinder {

    public static final String BCE_EXTENSION = BuildCompatibleExtension.class.getName();
    public static final String META_INF_SERVICES_BCE_EXTENSION = CDIUtils.META_INF_SERVICES + BCE_EXTENSION;
    public static final String WEB_INF_CLASSES_META_INF_SERVICES_BCE_EXTENSION = CDIUtils.WEB_INF_CLASSES + META_INF_SERVICES_BCE_EXTENSION;

    /** {@inheritDoc} */
    @Override
    public Set<String> findBceClassNames(CDIArchive archive) {
        if (archive.getType() == ArchiveType.WEB_MODULE) {
            Resource webInfClassesMetaInfServicesEntry = archive.getResource(WEB_INF_CLASSES_META_INF_SERVICES_BCE_EXTENSION);
            return CDIUtils.parseServiceSPIExtensionFile(webInfClassesMetaInfServicesEntry);
        } else {
            Resource metaInfServicesEntry = archive.getResource(META_INF_SERVICES_BCE_EXTENSION);
            return CDIUtils.parseServiceSPIExtensionFile(metaInfServicesEntry);
        }
    }

}
