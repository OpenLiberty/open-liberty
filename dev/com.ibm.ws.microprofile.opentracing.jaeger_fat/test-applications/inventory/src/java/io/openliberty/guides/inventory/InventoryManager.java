//tag::copyright[]
/*******************************************************************************
* Copyright (c) 2017, 2019 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - Initial implementation
*******************************************************************************/
//end::copyright[]
package io.openliberty.guides.inventory;

import java.util.ArrayList;
import java.util.Properties;
import io.openliberty.guides.inventory.client.SystemClient;
import io.openliberty.guides.inventory.model.InventoryList;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Collections;

import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.openliberty.guides.inventory.model.*;

@ApplicationScoped
public class InventoryManager {
    
    private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
    private SystemClient systemClient = new SystemClient();
    // tag::custom-tracer[]
    @Inject Tracer tracer;
    // end::custom-tracer[]

    public Properties get(String hostname) {
        systemClient.init(hostname, Integer.getInteger("bvt.prop.HTTP_default"));
        Properties properties = systemClient.getProperties();
        
        return properties;
    }

    public void add(String hostname, Properties systemProps) {
        Properties props = new Properties();
        props.setProperty("os.name", systemProps.getProperty("os.name"));
        props.setProperty("user.name", systemProps.getProperty("user.name"));

        SystemData system = new SystemData(hostname, props);
        if (!systems.contains(system)) {
            // tag::custom-tracer[]
            try (Scope childScope = tracer.buildSpan("add() Span")
                                              .startActive(true)) {
                // tag::addToInvList[]
                systems.add(system);
                // end::addToInvList[]
            }
            // end::custom-tracer[]
        }
    }

    @Traced(value = true, operationName = "InventoryManager.list")
    public InventoryList list() {
        return new InventoryList(systems);
    }
    
}
