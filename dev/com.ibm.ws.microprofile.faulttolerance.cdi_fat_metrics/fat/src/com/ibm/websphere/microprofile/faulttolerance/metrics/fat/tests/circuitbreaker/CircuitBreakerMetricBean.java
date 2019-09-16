/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
public class CircuitBreakerMetricBean {

    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, failOn = TestException.class)
    public void doWorkWithExeception(RuntimeException e) {
        if (e != null) {
            throw e;
        }
    }

}
