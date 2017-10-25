/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.aftertypediscovery.test;

import javax.enterprise.inject.Alternative;

@UseAlternative
@Alternative
public class AfterTypeAlternativeTwo implements AfterTypeAlternativeInterface {

    @Override
    public String getMsg() {
        return "alternative two";
    }
}
