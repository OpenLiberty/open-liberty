/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.datastore.web;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataStoreTestServlet extends FATServlet {

    @Inject
    DefaultDSRepo defaultDSRepo;

    /**
     * Use a repository that specifies the Jakarta EE default data source by its JNDI name: java:comp/DefaultDataSource
     */
    @Test
    public void testDefaultDataSourceByJNDIName() {
        assertEquals(false, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));

        defaultDSRepo.insert(DefaultDSEntity.of(25L, "twenty-five"));

        assertEquals(true, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));
    }
}
