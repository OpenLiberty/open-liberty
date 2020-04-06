/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webservices.wsat;

import javax.xml.ws.Dispatch;

/**
 *
 */
public interface WSATService {

    /**
     * Default Behavior is without using WSDL policy, so we don't need this SPI to enable WS-AT
     * But keep it in case we will use it in future
     * 
     * @param o
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void enableWSAT(Dispatch o) throws Exception;

}
