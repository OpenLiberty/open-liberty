/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.resourceinfoatstartup;

import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import javax.ws.rs.core.Response;

@Startup
@Singleton
@LocalBean
public class StartupSingletonBean {

    @Resource
    ExecutorService executor;

    @PostConstruct
    public void invokeClientAtStartup() {        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                ClientResource client = new ClientResource();
                int numClients = Integer.getInteger("test.clients", 50);
                System.out.println("about to test " + numClients + " clients in EJB startup method");
                Response r = client.test(numClients);
                System.out.println("All Clients Finished " + r.readEntity(String.class));
            }
        });        
    }
}