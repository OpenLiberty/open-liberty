/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

/**
 * Adds a prefix to the message returned from Bean instances
 */
@Decorator
public class BeanDecorator implements Bean {

    @Inject
    @Delegate
    @Any
    Bean bean;

    @Override
    public String getMessage() {
        if (this.bean == null) {
            return "Delegate not injected";
        }
        return "decorated " + this.bean.getMessage();
    }

}
