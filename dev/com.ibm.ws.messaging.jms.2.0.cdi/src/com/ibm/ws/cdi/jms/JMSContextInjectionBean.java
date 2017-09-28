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
package com.ibm.ws.cdi.jms;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;
import javax.naming.NamingException;

/*
 * This class is used for creating the Injected JMSContext instances.
 */

public class JMSContextInjectionBean {

    /*
     * This method is called while JMSContext is injected in the container. For every new @Inject
     * Annotation a new instance of JMSContext is returned. In case of active transaction when the
     * injected JMSContext is being used, a new instance is created and added to the transaction
     * registry so that next request with the same configuration will return the existing JMSContext
     * rather than creating a new one.
     */
    @Produces
    public JMSContext getJMSContext(InjectionPoint injectionPoint) throws NamingException {

        //Default connection factory string,  if no @JMSConnectionFactory annotation 
        //is provided, then this value is used for lookup
        String connectionFactoryString = "java:comp/DefaultJMSConnectionFactory";
        //Default acknowledge mode
        int acknowledgeMode = JMSContext.AUTO_ACKNOWLEDGE;

        //user name and password
        String userName = null;
        String password = null;

        //Get all the passed annotations and get the values
        if (injectionPoint != null)
        {

            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSConnectionFactory.class)) {
                JMSConnectionFactory jmsConnectionFactory = injectionPoint.getAnnotated().getAnnotation(JMSConnectionFactory.class);
                connectionFactoryString = jmsConnectionFactory.value();
            }

            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSPasswordCredential.class)) {
                JMSPasswordCredential jmsPasswordCredential = injectionPoint.getAnnotated().getAnnotation(JMSPasswordCredential.class);
                userName = jmsPasswordCredential.userName();
                password = jmsPasswordCredential.password();
            }
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSSessionMode.class)) {
                JMSSessionMode jmsSessionMode = injectionPoint.getAnnotated().getAnnotation(JMSSessionMode.class);
                acknowledgeMode = jmsSessionMode.value();
            }
        }

        //Create a Info object about the configuration, based on this we can compare if the second 
        //request for creating the new injected JMSContext within transaction is having the same
        //set of parameter,  then use the same JMSContext
        JMSContextInfo info = new JMSContextInfo(connectionFactoryString, userName, password, acknowledgeMode);

        return new JMSContextInjected(info);
    }

    //The CDI container will call this method once the injected JMSContext goes out of scope
    public void closeJMSContext(@Disposes JMSContext context) {
        if (context instanceof JMSContextInjected) {

            ((JMSContextInjected) context).closeInternalJMSContext();
        }
    }

}
