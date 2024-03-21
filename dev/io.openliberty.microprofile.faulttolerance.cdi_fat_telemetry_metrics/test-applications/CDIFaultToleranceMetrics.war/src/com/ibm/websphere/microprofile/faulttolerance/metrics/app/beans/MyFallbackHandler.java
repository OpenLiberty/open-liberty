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
package com.ibm.websphere.microprofile.faulttolerance.metrics.app.beans;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.websphere.microprofile.faulttolerance.metrics.utils.Connection;

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
