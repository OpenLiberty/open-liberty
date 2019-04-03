/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.postgresql;

import java.util.function.Supplier;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

// TODO: Eventually this rule can be moved to fattest.simplicitly, but leave it
// here for a bit while we incubate the concept
public class RequireDockerRule<T extends GenericContainer<?>> implements TestRule {

    static final Class<?> c = RequireDockerRule.class;
    private final Supplier<T> containerSupplier;
    private T container;

    public RequireDockerRule(Supplier<T> container) {
        this.containerSupplier = container;
    }

    /**
     * Used to ensure we only create one instance of the supplied container.
     * Repeated calls to Supplier.get() may return different instances on each call.
     */
    private T resolve() {
        if (container == null) {
            synchronized (this) {
                if (container == null)
                    container = containerSupplier.get();
            }
        }
        return container;
    }

    @Override
    public Statement apply(Statement stmt, Description desc) {
        try {
            Log.info(c, "apply", "Checking if Docker client is available...");
            DockerClientFactory.instance().client();
            Log.info(c, "apply", "Docker is available. Tests will run.");
        } catch (Throwable t) {
            Log.info(c, "apply", "Docker is NOT available on this machine");
            if (FATRunner.FAT_TEST_LOCALRUN) {
                Log.error(c, "apply", t);
                throw t;
            } else {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        Log.info(desc.getTestClass(), desc.getMethodName(),
                                 "Test class or method is skipped due to Docker not being available.");
                    }
                };
            }
        }
        // Docker is available, let the tests run
        return resolve().apply(stmt, desc);
    }

    public T get() {
        return resolve();
    }

}
