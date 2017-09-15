/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.factory;

import com.ibm.websphere.pmi.PmiModuleConfig;

/**
 * Interface to lookup Stats template configuration.
 * 
 * @ibm-spi
 */

public interface StatsTemplateLookup {
    /**
     * Returns the {@link com.ibm.websphere.pmi.PmiModuleConfig} for a given template.
     * 
     * @param templateName Stats template name
     * @return an instance of PmiModuleConfig that corresponds to the template name
     */
    public PmiModuleConfig getTemplate(String templateName);
}
