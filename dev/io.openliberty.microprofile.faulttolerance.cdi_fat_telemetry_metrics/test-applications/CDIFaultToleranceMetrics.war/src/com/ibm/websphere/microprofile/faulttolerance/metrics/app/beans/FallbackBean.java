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

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.ibm.websphere.microprofile.faulttolerance.metrics.utils.ConnectException;
import com.ibm.websphere.microprofile.faulttolerance.metrics.utils.Connection;

/**
 * Servlet implementation class Test
 */
@RequestScoped
public class FallbackBean {

    private int connectCountA = 0;

    @Inject
    private DataBean dataBean;

    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    @Timeout
    @Timed
    public Connection connectA() throws ConnectException {
        throw new ConnectException("FallbackBean.connectA: " + (++connectCountA));
    }

    public Connection fallback() {
        return new Connection() {

            @Override
            public String getData() {
                return "Fallback for: connectA - " + dataBean.getData();
            }
        };
    }

    public int getConnectCountA() {
        return connectCountA;
    }

}
