/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.apps.secureAsyncEvents;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.security.RunAs;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
@RunAs("apprentice")
public class SecureApprenticeChef {

    @Inject
    Event<RecipeArrival> recipe;

    public RecipeArrival produceARecipe() throws InterruptedException, ExecutionException, TimeoutException {
        RecipeArrival newRecipe = new RecipeArrival();
        CompletionStage<RecipeArrival> stage = recipe.fireAsync(newRecipe);
        CompletableFuture<RecipeArrival> future = stage.toCompletableFuture();

        // Set a (very) large timeout to be sure that something is wrong as opposed to slow
        RecipeArrival futureRecipe = future.get(60000, TimeUnit.MILLISECONDS);
        return futureRecipe;
    }

}
