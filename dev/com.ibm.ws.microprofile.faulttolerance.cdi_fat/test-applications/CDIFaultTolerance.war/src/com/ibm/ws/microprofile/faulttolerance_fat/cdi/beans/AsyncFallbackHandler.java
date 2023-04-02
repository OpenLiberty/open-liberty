/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

public class AsyncFallbackHandler implements FallbackHandler<Future<Connection>> {

    @Override
    public Future<Connection> handle(ExecutionContext context) {
        return CompletableFuture.completedFuture(new Connection() {

            @Override
            public String getData() {
                return "AsyncFallbackHandler: " + context.getMethod().getName();
            }

        });
    }

}
