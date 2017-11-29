/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * A bean representing an application
 * <p>
 * It's a managed bean so CDI can inject into it.
 */
@ApplicationScoped
public class AppBean {

    @Inject
    HelloBean helloBean;

    public void run() {
        System.out.println("AppBean start");
        System.out.println(helloBean.getHello());
        System.out.println("AppBean end");

    }

}
