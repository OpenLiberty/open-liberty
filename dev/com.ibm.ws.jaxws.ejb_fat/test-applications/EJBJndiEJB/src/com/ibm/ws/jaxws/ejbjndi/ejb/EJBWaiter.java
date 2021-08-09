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

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbjndi.common.Waiter;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.Coffee;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.CoffeeMachine;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.MixedCoffeeMachineService;

@Stateless
@Local(Waiter.class)
public class EJBWaiter implements Waiter {

    @WebServiceRef(value = MixedCoffeeMachineService.class)
    private CoffeeMachine compScopedCoffeeMachine;

    @WebServiceRef(name = "java:module/env/services/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine moduleScopedCoffeeMachine;

    @WebServiceRef(name = "java:app/env/services/ejb/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine appScopedCoffeeMachine;

    @WebServiceRef(name = "java:global/env/services/ejb/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine globalScopedCoffeeMachine;

    @Override
    public Coffee order(String type) {
        if (type.equals("comp")) {
            return compScopedCoffeeMachine.make(type);
        } else if (type.equals("module")) {
            return moduleScopedCoffeeMachine.make(type);
        } else if (type.equals("app")) {
            return appScopedCoffeeMachine.make(type);
        } else if (type.equals("global")) {
            return globalScopedCoffeeMachine.make(type);
        }
        return null;
    }

}
