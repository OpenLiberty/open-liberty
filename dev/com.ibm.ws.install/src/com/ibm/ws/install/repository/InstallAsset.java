/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.repository;

import java.util.Locale;

import com.ibm.ws.install.InstallLicense;

/**
 *
 */
public interface InstallAsset {

    /**
     * @return the asset display name in specified locale
     */
    public String getDisplayName(Locale locale);

    /**
     * @return the asset display name in English
     */
    public String getDisplayName();

    /**
     * @return the asset reference id. <br/>
     *         e.g.
     *         <ul>
     *         <li>feature: same a the symbolic name</li>
     *         <li>featureCollection: same a the symbolic name</li>
     *         </ul>
     */
    public String getId();

    /**
     * @return the asset description in specified locale
     */
    public String getDescription(Locale locale);

    /**
     * @return the asset description in English
     */
    public String getDescription();

    /**
     * @return the license in specified locale
     */
    public InstallLicense getLicense(Locale locale);

    /**
     * @return the license in English
     */
    public InstallLicense getLicense();

    /**
     * @return size of the asset in bytes
     */
    public long size();
}
