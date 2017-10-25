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
package com.ibm.ws.cdi12.test.priority;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator;
import com.ibm.ws.cdi12.test.priority.helpers.Bean;

@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public abstract class JarDecorator extends AbstractDecorator {

    @Inject
    public JarDecorator(@Delegate @Any Bean decoratedBean) {
        super(decoratedBean, JarDecorator.class);
    }

}
