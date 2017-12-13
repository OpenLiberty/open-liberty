/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beanvalidationcdi.validation;

import java.util.Locale;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.MessageInterpolator;

import beanvalidationcdi.beans.TestBean;

/**
 * Simple custom {@link MessageInterpolator} implementation that tests whether
 * CDI managed beans can be injected into this bean.
 */
public class CustomMessageInterpolator implements MessageInterpolator {

    @Inject
    TestBean bean;

    @Override
    public String interpolate(String arg0, Context arg1) {
        if (bean == null) {
            throw new IllegalStateException("bean is null, CDI must not have injected it");
        }
        return bean.getSomething();
    }

    @Override
    public String interpolate(String arg0, Context arg1, Locale arg2) {
        if (bean == null) {
            throw new IllegalStateException("bean is null, CDI must not have injected it");
        }
        return bean.getSomething();
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(CustomMessageInterpolator.class.getSimpleName() + " is getting destroyed.");
    }

}
