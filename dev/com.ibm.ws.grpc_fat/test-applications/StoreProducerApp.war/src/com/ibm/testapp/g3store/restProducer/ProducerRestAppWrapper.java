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
package com.ibm.testapp.g3store.restProducer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.ibm.testapp.g3store.restProducer.api.ProducerRestEndpoint;

/**
 * @author anupag
 *
 */
@ApplicationPath("/v1")
@OpenAPIDefinition(
                   tags = {
                            @Tag(name = "Create Apps", description = " create apps ")

                   },
                   info = @Info(
                                title = "Producer StoreClient App",
                                version = "1.0",
                                description = "An api for adding and remving new mobile retail apps in Store."))
public class ProducerRestAppWrapper extends Application {

    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static ExecutorService executorService() {
        return executor;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(ProducerRestEndpoint.class);
        return resources;
    }
}
