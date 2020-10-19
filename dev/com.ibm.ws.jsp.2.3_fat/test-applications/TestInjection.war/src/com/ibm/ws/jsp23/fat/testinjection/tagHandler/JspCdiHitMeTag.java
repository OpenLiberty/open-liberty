/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.testinjection.tagHandler;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ibm.ws.jsp23.fat.testinjection.beans.Pojo1;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionDependentBean;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionRequestBean;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionSessionBean;
import com.ibm.ws.jsp23.fat.testinjection.interceptors.InterceptMeBinding;

public class JspCdiHitMeTag extends BodyTagSupport {

    private static final long serialVersionUID = 7413920082866356670L;

    @Inject
    TestTagInjectionRequestBean cdiRequestBean;

    @Inject
    TestTagInjectionSessionBean cdiSessionBean;

    @Inject
    TestTagInjectionDependentBean cdiDependentBean;

    private final TestTagInjectionDependentBean x;

    @Inject
    public JspCdiHitMeTag(TestTagInjectionDependentBean bean) {
        //    public JspCdiHitMeTag() {

        //StringWriter sw = new StringWriter();
        //new Throwable("").printStackTrace(new PrintWriter(sw));
        //String stackTrace = sw.toString();
        // System.out.println("JspCdiHitMeTag constructor stack trace: \n " + stackTrace);

        this.x = bean;
        System.out.println("JspCdiHitMeTag and x is: " + x);
    }

    @Override
    public int doStartTag() throws JspException {

        return EVAL_BODY_BUFFERED;
    }

    @InterceptMeBinding
    @Override
    public int doEndTag() throws JspException {

        try {
            // print the message out
            String s1 = "x is null";
            if (x != null) {
                s1 = "constructor injection OK";
            }

            String s2 = "interceptor failed";
	    // check counter with modular 2 for testTag1() since the constructor bean in testTag2() hits this twice
            if (Pojo1.counter%2 == 1) {
                s2 = "interceptor OK";
            }

            JspWriter out = pageContext.getOut();
            out.println("Message: " + cdiDependentBean.getHitMe() + " " + cdiSessionBean.getHitMe() + " " + cdiRequestBean.getHitMe()
                        + " ..." + s1 + " ..." + s2);

            //System.out.println("HI from doEndTag().  cdiDependentBean.getHitMe() is: " + cdiDependentBean.getHitMe());
            //System.out.println("cdiSessionBean.getHitMe() is: " + cdiSessionBean.getHitMe());
            //System.out.println("cdiRequestBean.getHitMe() is: " + cdiRequestBean.getHitMe());
            //System.out.println("cdiRequestBean.getHitMe() is: " + cdiRequestBean.getHitMe());
            //System.out.println("Pojo1 counter: " + Pojo1.counter);
            //System.out.println("injected bean: " + x);

        } catch (Exception ex) {
            throw new JspException(ex);
        }

        return EVAL_PAGE;
    }

}
