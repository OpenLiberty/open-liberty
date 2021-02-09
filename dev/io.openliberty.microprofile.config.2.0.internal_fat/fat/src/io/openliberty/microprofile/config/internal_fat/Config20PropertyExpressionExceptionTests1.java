/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.brokenPropertyExpression.MissingPropertyExpressionBean1;

/**
 * TODO: This class should be part of Config20ExceptionTests. But can't yet due to: https://github.com/smallrye/smallrye-config/issues/486
 */
@RunWith(FATRunner.class)
public class Config20PropertyExpressionExceptionTests1 extends FATServletClient {

    public static final String BROKEN_PROPERTY_EXPRESSION_APP_NAME = "brokenPropertyExpressionApp";

    public static final String SERVER_NAME = "Config20PropertyExpressionExceptionServer";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.LATEST);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive propertyExpressionWar = ShrinkWrap.create(WebArchive.class, BROKEN_PROPERTY_EXPRESSION_APP_NAME + ".war")
                        .addClass(MissingPropertyExpressionBean1.class);

        // The war should throw a deployment exceptions, hence don't validate.
        ShrinkHelper.exportAppToServer(server, propertyExpressionWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

    }

    @Test
    public void testNonExistingPropertyExpressionForServerXMLVariable() throws Exception {

        List<String> errors = server
                        .findStringsInLogs("SRCFG00011: Could not expand value nonExistingPropertyForServerXMLVariable in property keyFromVariableInServerXML");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        server.stopServer("CWWKZ0002E");
        //CWWKZ0002E: An exception occurred while starting the application brokenPropertyExpressionApp

    }

}
