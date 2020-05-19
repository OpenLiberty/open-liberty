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
package com.ibm.ws.jsp23.fat.testinjection.tagHandler;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionApplicationBean;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionDependentBean;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionRequestBean;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestTagInjectionSessionBean;
import com.ibm.ws.jsp23.fat.testinjection.interceptors.InterceptMeBinding;

public class MethodInjectionTag extends BodyTagSupport {

    private static final long serialVersionUID = 7413920082866356670L;

    static int appStaticCounter = 0;
    private int appBeanCounter = 0;

    static int sesStaticCounter = 0;
    private int sesBeanCounter = 0;

    static int reqStaticCounter = 0;
    private int reqBeanCounter = 0;

    static int depStaticCounter = 0;
    private int depBeanCounter = 0;

    static int endTagHit = 0;

    // Method Injected bean
    @Inject
    public void setMethodBeanApp(TestTagInjectionApplicationBean bean) {
        appBeanCounter = bean.incAndGetMyCounter();
        appStaticCounter++;
    }

    @Inject
    public void setMethodBeanSes(TestTagInjectionSessionBean bean) {
        sesBeanCounter = bean.incAndGetMyCounter();
        sesBeanCounter = bean.incAndGetMyCounter();
        sesStaticCounter++;
        sesStaticCounter++;
    }

    @Inject
    public void setMethodBeanReq(TestTagInjectionRequestBean bean) {
        reqBeanCounter = bean.incAndGetMyCounter();
        reqBeanCounter = bean.incAndGetMyCounter();
        reqBeanCounter = bean.incAndGetMyCounter();
        reqStaticCounter++;
        reqStaticCounter++;
        reqStaticCounter++;
    }

    @Inject
    public void setMethodBeanDep(TestTagInjectionDependentBean bean) {
        depBeanCounter = bean.incAndGetMyCounter();
        depStaticCounter++;
    }

    public MethodInjectionTag() {

        //StringWriter sw = new StringWriter();
        //new Throwable("").printStackTrace(new PrintWriter(sw));
        //String stackTrace = sw.toString();
        // System.out.println("MethodInjectionTag constructor stack trace: \n " + stackTrace);

    }

    @Override
    public int doStartTag() throws JspException {

        return EVAL_BODY_BUFFERED;
    }

    @InterceptMeBinding
    @Override
    public int doEndTag() throws JspException {

        endTagHit++;

        try {
            String s1 = null;

            if ((appBeanCounter == 0) || (sesBeanCounter == 0) || (reqBeanCounter == 0) || (depBeanCounter == 0)) {
                s1 = "ERROR--> Counter = 0";
            }

            if ((appBeanCounter != appStaticCounter) || (sesBeanCounter != sesStaticCounter)
                || (reqBeanCounter != reqStaticCounter) || (depBeanCounter > 1)) {
                s1 = "ERROR-->  BeanCounters out of whack";
            }

            if (s1 != null) {
                JspWriter out = pageContext.getOut();
                out.println("Message: " + s1);
            } else {
                if ((endTagHit == 2) && (appBeanCounter == 2) && (sesBeanCounter == 4) && (reqBeanCounter == 6) && (depBeanCounter == 1)) {
                    // the second hit on the tag from the same JSP should get us here.
                    s1 = "BeanCounters are OK";
                    JspWriter out = pageContext.getOut();
                    out.println("Message: " + s1);
                } else if (endTagHit == 2) {
                    s1 = "Well that's not right: appBeanCounter: " + appBeanCounter + " sesBeanCounter: " + sesBeanCounter
                         + " reqBeanCounter: " + reqBeanCounter + " depBeanCounter: " + depBeanCounter + " endTagHit: " + endTagHit;
                    JspWriter out = pageContext.getOut();
                    out.println("Message: " + s1);
                }
            }

        } catch (Exception ex) {
            throw new JspException(ex);
        }

        return EVAL_PAGE;
    }

}
