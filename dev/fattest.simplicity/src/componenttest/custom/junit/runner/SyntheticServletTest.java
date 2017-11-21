/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.runners.model.FrameworkMethod;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class SyntheticServletTest extends FrameworkMethod {

    private final Field server;
    private final String queryPath;
    private final String testName;

    public SyntheticServletTest(Field server, String queryPath, Method method) {
        super(method);
        this.server = server;
        this.queryPath = queryPath;
        this.testName = method.getName();
    }

    @Override
    public Object invokeExplosively(Object target, Object... params) throws Throwable {
        Log.info(SyntheticServletTest.class, "invokeExplosively", "Running test: " + testName);
        LibertyServer s = (LibertyServer) server.get(null);
        FATServletClient.runTest(s, queryPath, testName);
        return null;
    }
}
