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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Assert;

import com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal.TestEJBPMICollaboratorFactory;

public class EJBStatsAccessor {
    private static String DOMAIN = "EJBTestPMI";
    private static String BEAN_NAME = "beanName";

    public static ObjectName createObjectName(String beanName) {
        try {
            return new ObjectName(DOMAIN, BEAN_NAME, beanName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(beanName, e);
        }
    }

    public static EJBStats getEJBStats(ObjectName on) {
        Assert.assertEquals(DOMAIN, on.getDomain());
        String beanName = on.getKeyProperty(BEAN_NAME);
        return TestEJBPMICollaboratorFactory.getCollaborator(beanName);
    }

    public static WSStats getWSStats(ObjectName on) {
        throw new UnsupportedOperationException();
    }
}
