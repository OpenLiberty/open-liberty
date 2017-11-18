/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi;

/**
 * Interface for custom CDI properties. The active instance can either be retrieved through DS or through a static getter method.
 */
public interface CDIContainerConfig {
    /**
     * Get the Container level configuration about whether to disable the scanning for implicit bean archives.
     * If the value is true, it means CDI Container will not scan archives without beans.xml to see whether they are implicit archives.
     *
     * @return true if the value set in the server configuration for the property of enableImplicitBeanArchives is false
     */
    public boolean isImplicitBeanArchivesScanningDisabled();
}
