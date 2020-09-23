/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.ConfigParserException;
import com.ibm.ws.config.xml.internal.ConfigValidator.ConfigElementList;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class ConfigValidatorTest {

    static WsLocationAdmin libertyLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;

    private final MetaTypeRegistry metatypeRegistry = new MetaTypeRegistry();
    private ConfigVariableRegistry variableRegistry;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    private void changeLocationSettings(String profileName) {
        SharedLocationManager.createDefaultLocations(SharedConstants.SERVER_XML_INSTALL_ROOT, profileName);
        libertyLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();
        this.variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null, libertyLocation);

        configParser = new XMLConfigParser(libertyLocation, variableRegistry);
    }

    private ConfigElement parseConfiguration(String xml) throws ConfigParserException {
        return configParser.parseConfigElement(new StringReader(xml));
    }

    @Test
    public void testNoConflict() throws Exception {
        changeLocationSettings("default");

        ConfigElement config1 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"a\" sslEnable=\"true\"/>");
        ConfigElement config2 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"a\" mutualAuth=\"false\"/>");
        ConfigElement config3 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"a\"/>");

        ConfigValidator validator = new ConfigValidator(metatypeRegistry, variableRegistry);
        Map<String, ConfigElementList> conflictMap = validator.generateConflictMap(Arrays.asList(config1, config2, config3));

        assertEquals("conflicts", 0, conflictMap.size());
    }

    @Test
    public void testConflictSingleton() throws Exception {
        changeLocationSettings("default");

        ConfigElement config1 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"a\" sslEnable=\"true\"/>");
        ConfigElement config2 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"b\" mutualAuth=\"false\"/>");
        ConfigElement config3 = parseConfiguration("<httpConnector clientAuth=\"false\" logFile=\"a\"/>");

        ConfigValidator validator = new ConfigValidator(metatypeRegistry, variableRegistry);
        Map<String, ConfigElementList> conflictMap = validator.generateConflictMap(Arrays.asList(config1, config2, config3));

        assertTrue("conflicts", conflictMap.size() > 0);

        assertFalse("clientAuth", conflictMap.get("clientAuth").hasConflict());
        assertFalse("sslEnable", conflictMap.get("sslEnable").hasConflict());
        assertFalse("mutualAuth", conflictMap.get("mutualAuth").hasConflict());
        assertTrue("logFile", conflictMap.get("logFile").hasConflict());
    }

    @Test
    public void testConflictFactory() throws Exception {
        changeLocationSettings("default");

        ConfigElement config1 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"a\" sslEnable=\"true\"/>");
        config1.setDocumentLocation("doc1");
        ConfigElement config2 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"b\" mutualAuth=\"false\"/>");
        config2.setDocumentLocation("doc2");
        ConfigElement config3 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"a\"/>");
        config3.setDocumentLocation("doc3");

        ConfigValidator validator = new ConfigValidator(metatypeRegistry, variableRegistry);
        Map<String, ConfigElementList> conflictMap = validator.generateConflictMap(Arrays.asList(config1, config2, config3));

        assertTrue("conflicts", conflictMap.size() > 0);

        assertFalse("clientAuth", conflictMap.get("clientAuth").hasConflict());
        assertFalse("sslEnable", conflictMap.get("sslEnable").hasConflict());
        assertFalse("mutualAuth", conflictMap.get("mutualAuth").hasConflict());
        assertTrue("logFile", conflictMap.get("logFile").hasConflict());

        assertEquals("a", conflictMap.get("logFile").getActiveValue());
    }

    @Test
    public void testConflictFactoryWithMergeBehavior() throws Exception {
        // Same values as testConflictFactory, but the third config element has a merge behavior of "Ignore"
        // This still generates a conflict, but the active value will be "B" instead of "A" as it is in the other test.
        changeLocationSettings("default");

        ConfigElement config1 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"a\" sslEnable=\"true\"/>");
        config1.setDocumentLocation("doc1");
        ConfigElement config2 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"b\" mutualAuth=\"false\"/>");
        config2.setDocumentLocation("doc2");
        ConfigElement config3 = parseConfiguration("<httpConnector id=\"1\" clientAuth=\"false\" logFile=\"a\"/>");
        config3.setDocumentLocation("doc3");

        config1.setMergeBehavior(MergeBehavior.MERGE);
        config2.setMergeBehavior(MergeBehavior.MERGE);
        config3.setMergeBehavior(MergeBehavior.IGNORE);

        ConfigValidator validator = new ConfigValidator(metatypeRegistry, variableRegistry);

        Map<String, ConfigElementList> conflictMap = validator.generateConflictMap(Arrays.asList(config1, config2, config3));

        assertTrue("conflicts", conflictMap.size() > 0);

        assertTrue("logFile", conflictMap.get("logFile").hasConflict());
        assertEquals("b", conflictMap.get("logFile").getActiveValue());
    }
}
