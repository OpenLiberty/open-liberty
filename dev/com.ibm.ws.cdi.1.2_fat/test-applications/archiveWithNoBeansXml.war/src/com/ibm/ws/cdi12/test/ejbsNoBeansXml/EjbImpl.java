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
package com.ibm.ws.cdi12.test.ejbsNoBeansXml;

import javax.inject.Inject;

/**
 *
 */
public class EjbImpl implements FirstManagedBeanInterface, SecondManagedBeanInterface {

    private OtherManagedSimpleBean bean;

    @Inject
    public EjbImpl(OtherManagedSimpleBean injected) {
        this.bean = injected;
    }

    public EjbImpl() {
        throw new RuntimeException("Wrong Constructor called: EjbImpl()");
    }

    @Override
    public void setValue1(String value) {
        bean.setOtherValue(value);
    }

    @Override
    public String getValue1() {
        return bean.getOtherValue();
    }

    @Override
    public void setValue2(String value) {
        bean.setOtherValue(value);
    }

    @Override
    public String getValue2() {
        return bean.getOtherValue();
    }
}