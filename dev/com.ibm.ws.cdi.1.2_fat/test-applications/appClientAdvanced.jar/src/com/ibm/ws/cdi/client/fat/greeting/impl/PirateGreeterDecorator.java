/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi.client.fat.greeting.impl;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import com.ibm.ws.cdi.client.fat.greeting.Greeter;

/**
 * A decorator that makes a greeter a little more piratey
 */
@Decorator
@Priority(APPLICATION)
public class PirateGreeterDecorator implements Greeter {

    @Delegate
    @Inject
    @Any
    private Greeter delegate;

    @Override
    public String getHello() {
        return delegate.getHello() + ", I mean... Ahoy!";
    }

}
