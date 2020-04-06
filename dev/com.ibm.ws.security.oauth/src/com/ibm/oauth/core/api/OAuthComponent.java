/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.statistics.OAuthStatistics;

/**
 * Common marker interface for methods shared by both OAuth10Component and
 * OAuth20Component interfaces.
 * 
 */
public interface OAuthComponent {

    /**
     * Returns the configuration object associated with the component
     * 
     * @return the configuration object associated with the component
     */
    public OAuthComponentConfiguration getConfiguration();

    /**
     * Returns the component instance that created and owns this component
     * 
     * @return the component instance that created and owns this component
     */
    public OAuthComponentInstance getParentComponentInstance();

    /**
     * Returns the statistics manager for this component
     * 
     * @return the statistics manager associated with the component
     */
    public OAuthStatistics getStatistics();
}
