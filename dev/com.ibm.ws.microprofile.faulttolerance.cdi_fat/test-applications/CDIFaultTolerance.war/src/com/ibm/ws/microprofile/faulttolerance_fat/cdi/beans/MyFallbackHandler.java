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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

@Dependent
public class MyFallbackHandler implements FallbackHandler<Connection> {

    @Inject
    private DataBean dataBean;

    @Override
    public Connection handle(ExecutionContext context) {
        System.out.println("Fallback: " + context);
        return new Connection() {

            @Override
            public String getData() {
                return "Fallback for: " + context.getMethod().getName() + " - " + dataBean.getData();
            }
        };
    }

}
