/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.el50.fat.coercion.servlets.lambda.EL50LambdaExpressionCoercionServlet;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.ELUtils;

/**
 * Tests to ensure jakarta.el.LambdaExpression can be coerced to a functional interface, such as java.util.function.Predicate
 * https://github.com/jakartaee/expression-language/issues/45
 */
@RunWith(FATRunner.class)
public class EL50LambdaExpressionCoercionTest extends FATServletClient {

    @Server("expressionLanguage50_lambdaServer")
    @TestServlet(servlet = EL50LambdaExpressionCoercionServlet.class, contextRoot = "EL50Coercion")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL50Coercion.war", "io.openliberty.el50.fat.coercion.servlets.lambda");

        // Set websphere.java.security.exempt=true because Expression Language 6.0 removed all references
        // to the SecurityManager and related APIs.
        if (JakartaEEAction.isEE11OrLaterActive()) {
            ELUtils.setServerJavaSecurityExempt(server);
        }

        server.startServer(EL50LambdaExpressionCoercionTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
