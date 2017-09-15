/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * RTC 160610. Contains methods moved from WebAppConfig which should not be SPI.  
 */
public interface WebAppConfigExtended extends WebAppConfig {

    /**
     * Returns the Module metadata associated with this config
     * 
     * @return
     */
    public WebModuleMetaData getMetaData();

}