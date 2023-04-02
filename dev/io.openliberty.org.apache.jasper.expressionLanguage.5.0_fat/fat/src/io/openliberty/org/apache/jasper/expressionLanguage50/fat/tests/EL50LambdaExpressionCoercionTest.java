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
package io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.el50.fat.coercion.servlets.lambda.EL50LambdaExpressionCoercionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests to ensure jakarta.el.LambdaExpression can be coerced to a functional interface, such as java.util.function.Predicate
 * https://github.com/jakartaee/expression-language/issues/45
 */
@RunWith(FATRunner.class)
public class EL50LambdaExpressionCoercionTest extends FATServletClient {

    @Server("expressionLanguage50_lambdaServer")
    @TestServlet(servlet = EL50LambdaExpressionCoercionServlet.class, contextRoot = "EL50Coercion")
    public static LibertyServer elServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(elServer, "EL50Coercion.war", "io.openliberty.el50.fat.coercion.servlets.lambda");

        elServer.startServer(EL50LambdaExpressionCoercionTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (elServer != null && elServer.isStarted()) {
            elServer.stopServer();
        }
    }
}
