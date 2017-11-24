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
package com.ibm.ws.cdi.client.fat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi.client.fat.counting.CountBean;
import com.ibm.ws.cdi.client.fat.greeting.English;
import com.ibm.ws.cdi.client.fat.greeting.French;
import com.ibm.ws.cdi.client.fat.greeting.Greeter;

@ApplicationScoped
public class AppBean {

    @Inject
    @English
    private Greeter englishHello;

    @Inject
    @French
    private Greeter frenchHello;

    @Inject
    private CountBean counter;

    public void run() {

        counter.setWarningLevel(5);

        // Call both our beans
        System.out.println(englishHello.getHello());
        System.out.println(frenchHello.getHello());

        // Make some more hello calls to exercise our counting interceptor
        System.out.println(englishHello.getHello());
        System.out.println(englishHello.getHello());
        System.out.println(englishHello.getHello());
        System.out.println(englishHello.getHello());
        System.out.println(englishHello.getHello());

        System.out.println("There were " + counter.getCount() + " countable calls made");
    }

}
