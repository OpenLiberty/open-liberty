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

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator;
import com.ibm.ws.cdi12.test.priority.helpers.Bean;

/**
 * Enabled for this bean archive in beans.xml.
 */
@Decorator
public abstract class LocalJarDecorator extends AbstractDecorator {

    @Inject
    public LocalJarDecorator(@Delegate @Any Bean decoratedBean) {
        super(decoratedBean, LocalJarDecorator.class);
    }

}
