/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

public class MyFallbackHandler2 implements FallbackHandler<Connection> {

    @Override
    public Connection handle(ExecutionContext context) {
        return new Connection() {

            @Override
            public String getData() {
                return "MyFallbackHandler2 - fallback for " + context.getMethod().getName();
            }
        };
    }

}
