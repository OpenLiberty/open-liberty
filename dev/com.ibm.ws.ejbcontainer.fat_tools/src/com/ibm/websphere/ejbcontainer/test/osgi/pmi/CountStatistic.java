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
package com.ibm.websphere.ejbcontainer.test.osgi.pmi;

import com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal.AbstractIncrementableStatistic;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class CountStatistic extends AbstractIncrementableStatistic {
    public long getCount() {
        return value.get();
    }
}
