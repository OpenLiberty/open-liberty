/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi.extension.impl;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

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
