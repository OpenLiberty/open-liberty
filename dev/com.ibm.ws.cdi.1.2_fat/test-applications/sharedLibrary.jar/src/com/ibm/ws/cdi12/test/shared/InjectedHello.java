/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.shared;

import javax.enterprise.context.ApplicationScoped;

// have a static counter and an instance counter

// call the class from two different applications
// check that the static counter is incremented (shows that the class is loaded by the same class loader)
// check that the instance loader is not (shows that you don't get the same instance)

@ApplicationScoped
public class InjectedHello {
    public String areYouThere(String name) {
        return "Hello from an InjectedHello, I am here: " + name;
    }
}
