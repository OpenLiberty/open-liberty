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
package com.ibm.ws.cdi.extension.impl;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * This is an extension impl.
 */
@Component(service = WebSphereCDIExtension.class)
public class HelloWorldExtension implements Extension, WebSphereCDIExtension {

    public void oberservBean(@Observes BeforeBeanDiscovery bbd)
    {
        System.out.println("Hello World! We are starting the container");
        System.out.println("Hello " + Validator.class.getName());
        System.out.println("Hello " + ValidatorFactory.class.getName());
    }

    public void scanMyClasses(@Observes ProcessAnnotatedType pat)
                    throws Exception
    {
        System.out.println("Hello World! scanning class " + pat.getAnnotatedType().getJavaClass().getName());

    }

    public void finished(@Observes AfterDeploymentValidation adv)
    {
        System.out.println("Hello World! We are almost finished with the CDI container boot now...");
    }

}
