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
import javax.annotation.Priority;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.test.managedbean.CounterUtil;
import com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal;

/**
 *
 */

@MyCDIInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class MyCDIInterceptor {

    @Inject
    MyEJBBeanLocal ejbBean;
    @Resource(name = "myStr")
    String myStr;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        ejbBean.addToMsgList(this.getClass().getSimpleName() + ":AroundInvoke called" + " injectedStr:" + myStr);
        return context.proceed();
    }

    @AroundConstruct
    private Object construct(InvocationContext context) {
        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":AroundConstruct called" + " injectedStr:" + myStr);
        try {
            return context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private void postConstruct(InvocationContext context) {
        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":PostConstruct called" + " injectedStr:" + myStr);
        try {
            context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void destroy(InvocationContext context) {
        System.out.println("@PreDestory called " + this.getClass().getSimpleName());

        try {
            context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
