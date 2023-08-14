/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi;

import java.util.Collections;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.javaee.version.JavaEEVersion;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = CDIExtensionMetadata.class)
public class ConcurrencyExtensionMetadata implements CDIExtensionMetadata {
    /**
     * Jakarta EE version.
     */
    public static Version eeVersion;

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(ConcurrencyExtension.class);
    }

    /**
     * The service ranking of JavaEEVersion ensures we get the highest
     * Jakarta EE version for the configured features.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        eeVersion = Version.parseVersion(version);
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
    }
}