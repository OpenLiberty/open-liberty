/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config;

import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Information about a web fragment
 */
public interface WebFragmentInfo {
    /**
     * Returns the library URI
     * 
     * @return
     */
    public String getLibraryURI();

    /**
     * Returns the container associated with the web fragment
     * 
     * @return
     */
    public Container getFragmentContainer();

    /**
     * Returns the web fragment
     * 
     * @return
     */
    public WebFragment getWebFragment();

    public boolean isSeedFragment();

    public boolean isPartialFragment();
}
