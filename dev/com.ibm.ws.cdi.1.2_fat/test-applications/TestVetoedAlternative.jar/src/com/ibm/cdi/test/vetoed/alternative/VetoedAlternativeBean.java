/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.cdi.test.vetoed.alternative;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Vetoed;

@Vetoed
@Alternative
@ApplicationScoped
public class VetoedAlternativeBean {

    private final String msg = "Vetoed Alternative Hello World";

    public String getMsg() {
        return msg;
    }
}
