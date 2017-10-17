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
package com.ibm.ws.cdi.test.managedbean.interceptors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.test.managedbean.CounterUtil;
import com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal;

/**
 * This is non-cdi interceptor
 */

public class MyNonCDIInterceptor extends MyInterceptorBase {

    @Inject
    MyEJBBeanLocal ejbBean;

    @Resource(name = "myInt")
    int myInt;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {

        ejbBean.addToMsgList(this.getClass().getSimpleName() + ":" + getAroundInvokeText() + " called" + " injectedInt:" + myInt);

        return context.proceed();
    }

    @AroundConstruct
    private Object construct(InvocationContext context) {

        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":AroundConstruct called" + " injectedInt:" + myInt);
        try {
            return context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private Object postConstruct(InvocationContext context) {

        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":PostConstruct called" + " injectedInt:" + myInt);
        try {
            return context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private Object destroy(InvocationContext context) {
        System.out.println("@PreDestory called " + this.getClass().getSimpleName());

        try {
            return context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
