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
package jaxrs21.fat.contextandclient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.client.ClientBuilder;

@RequestScoped
public class Client {

    public String get(String remoteUri) {        
        
        CompletableFuture<String> completableFuture = ClientBuilder.newClient().target(remoteUri).request().rx().get(String.class).toCompletableFuture();
        String response = null;
        try {
            response = completableFuture.get().trim();            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println(this.getClass() + " Response = " + response);
        return response;
    }
}