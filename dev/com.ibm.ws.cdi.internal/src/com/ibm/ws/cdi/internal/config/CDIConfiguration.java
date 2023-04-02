/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.config;

/**
 * Interface for custom CDI properties. The active instance should be retrieved as an OSGi DS.
 */
public interface CDIConfiguration {
    /**
     * Get the Container level configuration about whether to disable the scanning for implicit bean archives.
     * If the value is true, it means CDI Container will not scan archives without beans.xml to see whether they are implicit archives.
     *
     * @return true if the value set in the server configuration for the property of enableImplicitBeanArchives is false
     */
    public boolean isImplicitBeanArchivesScanningDisabled();

    /**
     * Get the Container level configuration about whether to treat an archive which has an empty beans.xml file as
     * an explicit bean archive. (Bean Discovery Mode = ALL).
     *
     * This configuration should only be used by cdi-4.0 and above.
     *
     * @return true if an archive which has an empty beans.xml file should be treated as an explicit bean archive.
     */
    public boolean emptyBeansXmlCDI3Compatibility();
}
