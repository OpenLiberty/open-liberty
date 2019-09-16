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
package com.ibm.ws.jaxws.ejbjndi.webejb;

import javax.jws.WebService;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbjndi.common.Coffee;
import com.ibm.ws.jaxws.ejbjndi.ejb.client.CoffeemateProvider;
import com.ibm.ws.jaxws.ejbjndi.web.client.WebMilkProvider;

@WebService(endpointInterface = "com.ibm.ws.jaxws.ejbjndi.common.CoffeeMachine")
public class MixedCoffeeMachine {

    @WebServiceRef(value = com.ibm.ws.jaxws.ejbjndi.ejb.client.EJBCoffeemateProviderService.class)
    private CoffeemateProvider coffeemateProvider;

    private WebMilkProvider milkProvider;

    public boolean isSupported(String type) {
        return "Mixed".equals(type);
    }

    public Coffee make(String type) {
        Coffee coffee = new Coffee(type);

        configureEndpointURL((BindingProvider) coffeemateProvider, EndpointInfoHolder.COFFEEMATE_PROVIDER_ENDPOINT_URL);
        coffee.addCoffeemate(coffeemateProvider.take("a little"));

        configureEndpointURL((BindingProvider) milkProvider, EndpointInfoHolder.MILK_PROVIDER_ENDPOINT_URL);
        coffee.addMilk(milkProvider.take("lots of"));
        return coffee;
    }

    private void configureEndpointURL(BindingProvider provider, String endpointURL) {
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
    }
}
