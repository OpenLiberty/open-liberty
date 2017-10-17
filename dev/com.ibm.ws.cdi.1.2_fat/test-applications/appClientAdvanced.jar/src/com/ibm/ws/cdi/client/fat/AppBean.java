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
