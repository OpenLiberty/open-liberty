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
package io.openliberty.springboot.support.version30.test.aop.app;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LoggingAspectConfig {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    //This gets called when LTW (LoadTimeWeaving) intercepts the call to the internal method. This explicitly requires LTW because Spring AOP does not intercept internal method calls. 
    @Before("execution(* io.openliberty.springboot.support.version30.test.aop.app.AopService.getInternalService())")
    public void beforeInternalMethodExecution() {
        LOGGER.info("==> Before internal service method execution");
    }

    @After("execution(* io.openliberty.springboot.support.version30.test.aop.app.AopService.getService())")
    public void afterExternalMethodExecution() {
        LOGGER.info("==> After external service method execution");
    }
}
