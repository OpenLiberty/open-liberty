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
package com.ibm.ws.jaxws.ejblifecycle;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService
public class SimpleEcho {
    @Resource
    private SessionContext sessionContext;

    @PostConstruct
    public void initialize() {
        if (null != sessionContext) {
            System.out.println("sessionContext is not null");
        }

        System.out.println(this.getClass().getName() + ": postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ": PreDestroy is invoked");
    }

    public String echo(String value) {
        System.out.println(this.getClass().getName() + ": echo is invoked");
        return value;
    }
}
