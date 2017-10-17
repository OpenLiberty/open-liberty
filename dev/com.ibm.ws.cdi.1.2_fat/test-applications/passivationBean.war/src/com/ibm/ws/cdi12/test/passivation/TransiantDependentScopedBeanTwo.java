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
package com.ibm.ws.cdi12.test.passivation;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

@Dependent
public class TransiantDependentScopedBeanTwo {

    public void doNothing() {
        int i = 1;
        i++;
        GlobalState.addString("doNothing" + i);
    }

    @PreDestroy
    public void preD() {
        GlobalState.addString("destroyed-two");
    }

    public TransiantDependentScopedBeanTwo() {}
}
