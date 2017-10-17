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
package com.ibm.ws.cdi.test.managedbean;

import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.cdi.test.managedbean.interceptors.MyCDIInterceptorBinding;
import com.ibm.ws.cdi.test.managedbean.interceptors.MyNonCDIInterceptor;

/**
 *
 */
@ManagedBean
@MyCDIInterceptorBinding
@Interceptors({ MyNonCDIInterceptor.class })
public class MyManagedBean {

    private List<String> msgList;

    @Resource(name = "myBeanName")
    String myBeanName;

    @Inject
    MyEJBBeanLocal myEjbBean;

    public List<String> getMsgList() {

        if (this.msgList == null) {
            this.msgList = myEjbBean.getMsgList();
        }
        return this.msgList;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void doPostConstruct() {

        CounterUtil.addToMsgList(this.getClass().getSuperclass().getSimpleName() + " called postConstruct()");
    }

    @SuppressWarnings("unused")
    @PreDestroy
    private void doPreDestroy() {
        System.out.println("@PreDestory called " + this.getClass().getSuperclass().getSimpleName());
    }

}
