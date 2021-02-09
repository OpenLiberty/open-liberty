/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.osgi.pmi;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Assert;

import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.internal.TestEJBPMICollaboratorFactory;

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
