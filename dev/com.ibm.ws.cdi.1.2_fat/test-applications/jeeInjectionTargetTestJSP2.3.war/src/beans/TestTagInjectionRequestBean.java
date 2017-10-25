/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beans;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@RequestScoped
@Named
public class TestTagInjectionRequestBean {

    private static AtomicInteger preDestroyCount = new AtomicInteger(0);

    public static String getPreDestroyCount() {
        return "The preDestroyCount is " + preDestroyCount.get();
    }

    public String getHitMe() {
        String response = "RequestBean Hit";
        return response;
    }

    @PreDestroy
    public void destruct() {
        preDestroyCount.incrementAndGet();
        //System.out.println("Calling PreDestroy on " + this);
    }

    private int myCounter = 0;

    public int incAndGetMyCounter() {
        myCounter++;
        return myCounter;
    }
}