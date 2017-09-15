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
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * Bean that throws an exception which includes how many times it has been called
 */
@RequestScoped
public class RequestRetryCountingBean {

    private final AtomicInteger count = new AtomicInteger(0);

    @Retry(maxRetries = 3)
    public void call() throws CountingException {
        throw new CountingException(count.incrementAndGet());
    }

}
