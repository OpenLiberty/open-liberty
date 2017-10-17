package com.ibm.ws.cdi.ejb.constructor.test;

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

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;

@Stateless
@LocalBean
public class BeanEJB {

    String firstBeanMessage = "";
    String secondBeanMessage = "";
    String thirdBeanMessage = "";
    String forthBeanMessage = "";

    public BeanEJB() {

    }

    @Inject
    public BeanEJB(@MyQualifier Iface one, @MySecondQualifier Iface two,
                   @MyThirdQualifier @TransientReference BeanThree three, @MyForthQualifier BeanFourWhichIsEJB four) {
        firstBeanMessage = one.getMsg();
        secondBeanMessage = two.getMsg();
        thirdBeanMessage = three.getMsg();
        forthBeanMessage = four.getMsg();
    }

    public String test() {

        StaticState.append("First bean message: " + firstBeanMessage);
        StaticState.append(System.getProperty("line.separator"));
        StaticState.append("Second bean message: " + secondBeanMessage);
        StaticState.append(System.getProperty("line.separator"));
        StaticState.append("Third bean message: " + thirdBeanMessage);
        StaticState.append(System.getProperty("line.separator"));
        StaticState.append("Forth bean message: " + forthBeanMessage);
        StaticState.append(System.getProperty("line.separator"));

        return StaticState.getOutput();
    }

}
