/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.misc.ejb;

import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Assert;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
@Remote(ImplicitTypeRemote.class)
public class ImplicitTypeBean {
    private int envEntryIntField;
    private int envEntryIntMethodValue;

    private void setEnvEntryIntMethod(int x) {
        envEntryIntMethodValue = x;
    }

    private Integer envEntryIntegerField;
    private Integer envEntryIntegerMethodValue;

    private void setEnvEntryIntegerMethod(Integer x) {
        envEntryIntegerMethodValue = x;
    }

    private ImplicitTypeRemote ejbRefField;
    private ImplicitTypeRemote ejbRefMethodValue;

    private void setEjbRefMethod(ImplicitTypeRemote x) {
        ejbRefMethodValue = x;
    }

    private ImplicitTypeBean ejbLocalRefField;
    private ImplicitTypeBean ejbLocalRefMethodValue;

    private void setEjbLocalRefMethod(ImplicitTypeBean x) {
        ejbLocalRefMethodValue = x;
    }

    private DataSource resourceRefField;
    private DataSource resourceRefMethodValue;

    private void setResourceRefMethod(DataSource x) {
        resourceRefMethodValue = x;
    }

    private UserTransaction resourceEnvRefField;
    private UserTransaction resourceEnvRefMethodValue;

    private void setResourceEnvRefMethod(UserTransaction x) {
        resourceEnvRefMethodValue = x;
    }

    public void test() {
        Assert.assertEquals(1, envEntryIntField);
        Assert.assertEquals(1, envEntryIntMethodValue);

        Assert.assertEquals((Integer) 1, envEntryIntegerField);
        Assert.assertEquals((Integer) 1, envEntryIntegerMethodValue);

        Assert.assertNotNull(ejbRefField);
        Assert.assertNotNull(ejbRefMethodValue);

        Assert.assertNotNull(ejbLocalRefField);
        Assert.assertNotNull(ejbLocalRefMethodValue);

        Assert.assertNotNull(resourceRefField);
        Assert.assertNotNull(resourceRefField);

        Assert.assertNotNull(resourceEnvRefField);
        Assert.assertNotNull(resourceEnvRefMethodValue);
    }
}
