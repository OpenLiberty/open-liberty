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
package com.ibm.ws.jaxws.ejbjndi.ejb;

import javax.ejb.Stateless;
import javax.jws.WebService;

import com.ibm.ws.jaxws.ejbjndi.common.CoffeemateProvider;

@WebService(endpointInterface = "com.ibm.ws.jaxws.ejbjndi.common.CoffeemateProvider")
@Stateless
public class EJBCoffeemateProvider implements CoffeemateProvider {

    @Override
    public String take(String amount) {
        return EJBCoffeemateProvider.class.getName() + " [" + amount + "]";
    }
}
