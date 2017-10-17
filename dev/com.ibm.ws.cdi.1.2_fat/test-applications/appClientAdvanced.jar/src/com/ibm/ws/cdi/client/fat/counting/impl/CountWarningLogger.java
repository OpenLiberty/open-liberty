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
package com.ibm.ws.cdi.client.fat.counting.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.ibm.ws.cdi.client.fat.counting.CountWarning;

/**
 * A bean that watches for CountWarning events and prints to stdout.
 */
@ApplicationScoped
public class CountWarningLogger {

    public void printWarning(@Observes CountWarning warning) {
        System.out.println("Warning: " + warning.getCount() + " countable methods have been executed");
    }

}
