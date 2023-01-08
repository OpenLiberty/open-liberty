/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.query.tests;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

/**
 *
 */
public class SkipRule implements TestRule {

    private DatabaseVendor database;
    private JPAPersistenceProvider provider;

    public void setDatabase(DatabaseVendor database) {
        this.database = database;
    }

    public void setProvider(JPAPersistenceProvider provider) {
        this.provider = provider;
    }

    // TODO: Disabling this FAT on SQLServer as the tests run too long, resulting in a timeout
    // This FAT should be split later to reduce running time.0
    public boolean isSkipping() {
        return ((database != null)
                && (DatabaseVendor.SQLSERVER.equals(database)));
    }

    @Override
    public Statement apply(Statement arg0, Description arg1) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                System.out.println("Checking Database(" + database.name() + ")/Provider(" + provider.name() + ") to skip tests");
                if (isSkipping()) {
                    throw new AssumptionViolatedException("Database is " + database.name() + " / Provider is " + provider.name() + ". Skipping test!");
                } else {
                    System.out.println("Not Skipping");
                    arg0.evaluate();
                }
            }
        };
    }

}
