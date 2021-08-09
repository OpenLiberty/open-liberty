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
package com.ibm.ws.jaxws.ejbHandler;

import javax.ejb.Singleton;
import javax.jws.WebService;

/**
 *
 */
@Singleton
@WebService
public class EchoBean implements Echo {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.ejbHandler.Echo#echo(java.lang.String)
     */
    @Override
    public String echo(String value) {
        return value;
    }
}
