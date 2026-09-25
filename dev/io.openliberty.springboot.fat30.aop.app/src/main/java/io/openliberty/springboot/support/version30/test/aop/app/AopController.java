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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

@RestController
public class AopController {
    @Autowired
    AopService aopService;

    @RequestMapping("/service")
    public WebAsyncTask<String> aop() {
        return new WebAsyncTask<>(30_000L, aopService::getService);
    }

    @GetMapping("/service-sync")
    public String aopSync() {
        return aopService.getService();
    }

}
