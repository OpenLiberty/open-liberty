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
package com.ibm.ws.install;

import java.util.Locale;

/**
 * This class provides the APIs to get an instance of installed asset.
 */
public interface InstalledAsset {

    /**
     * @return the display name of the installed asset
     */
    public String getDisplayName();

    /**
     * @return the display name of the installed asset in the specified Locale
     */
    public String getDisplayName(Locale locale);

    /**
     * @return the short description of the installed asset
     */
    public String getShortDescription();

    /**
     * @return the short description of the installed asset in the specified Locale
     */
    public String getShortDescription(Locale locale);

    /**
     * @return true if the visibility of the installed asset is public or install
     */
    public boolean isPublic();

    /**
     * @return the product id of the installed asset
     */
    public String getProductId();
}
