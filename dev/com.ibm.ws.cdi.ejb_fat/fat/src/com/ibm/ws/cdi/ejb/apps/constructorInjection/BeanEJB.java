package com.ibm.ws.cdi.ejb.apps.constructorInjection;

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
        StaticState.append(System.lineSeparator());
        StaticState.append("Second bean message: " + secondBeanMessage);
        StaticState.append(System.lineSeparator());
        StaticState.append("Third bean message: " + thirdBeanMessage);
        StaticState.append(System.lineSeparator());
        StaticState.append("Forth bean message: " + forthBeanMessage);
        StaticState.append(System.lineSeparator());

        return StaticState.getOutput();
    }

}
