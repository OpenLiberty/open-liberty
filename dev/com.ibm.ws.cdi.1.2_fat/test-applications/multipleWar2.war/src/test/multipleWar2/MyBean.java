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
package test.multipleWar2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 */
@Named("myNotSameBean")
@ApplicationScoped
public class MyBean {
    String myName = "myWar2Bean";

    public String getName() {
        return myName;
    }
}
