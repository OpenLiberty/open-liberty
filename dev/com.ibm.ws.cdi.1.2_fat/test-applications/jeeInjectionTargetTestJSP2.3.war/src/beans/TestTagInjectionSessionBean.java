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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@SessionScoped
@Named
public class TestTagInjectionSessionBean implements Serializable {

    private static final long serialVersionUID = -108725347266997794L;
    private static AtomicInteger preDestroyCount = new AtomicInteger(0);

    public static String getPreDestroyCount() {
        return "The preDestroyCount is " + preDestroyCount.get();
    }

    public String getHitMe() {
        String response = "SessionBean Hit";
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