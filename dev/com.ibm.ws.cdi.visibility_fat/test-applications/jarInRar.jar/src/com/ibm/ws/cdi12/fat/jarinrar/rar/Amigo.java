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
package com.ibm.ws.cdi12.fat.jarinrar.rar;

import javax.enterprise.inject.Default;

/**
 * Simple bean class
 */
@Default
public class Amigo {

    public boolean yoQueroBurritos() {
        return true;
    }

    @Override
    public String toString() {
        return "Buenos Dias me Amigo!";
    }
}
