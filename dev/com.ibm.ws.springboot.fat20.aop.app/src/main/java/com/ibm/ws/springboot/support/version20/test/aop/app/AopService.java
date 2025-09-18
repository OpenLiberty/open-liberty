/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.aop.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AopService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public String getService() {
        LOGGER.info("==> External Service method execution");
        getInternalService();
        return "Spring Boot AOP Service";
    }

    public void getInternalService() {
        LOGGER.info("==> Internal Service method execution");
    }
}
