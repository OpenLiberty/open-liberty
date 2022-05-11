/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import static org.junit.Assert.assertEquals;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.data.Data;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataTestServlet extends FATServlet {

    @Inject
    PersonRepo repo;

    @Resource
    private UserTransaction tran;

    /**
     * Verify the test application can access a class from the data experimentation bundle.
     * Remove this later when the test does something useful.
     */
    @Test
    public void testDataAnnoAccessibleToApp() throws Throwable {
        Data.class.getName();
    }

    /**
     * See if the interceptor is invoked
     */
    @Test
    public void testDataInterceptor() throws Throwable {
        assertEquals("Data", repo.findByName("First", "Last"));
    }

    /**
     * Remove this later when the test does something useful with the configured data source.
     */
    @Test
    public void testDataSourceLookup() throws Throwable {
        InitialContext.doLookup("java:comp/DefaultDataSource");
    }
}
