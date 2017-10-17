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
package com.ibm.ws.cdi12.aftertypediscovery.test;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

@Decorator
public class AfterTypeBeanDecorator implements AfterTypeInterface {

    @Inject
    @Delegate
    @Any
    AfterTypeInterface bean;

    @Override
    public String getMsg() {
        return "New msg: decorated. Origonal msg:" + bean.getMsg();
    }

}
