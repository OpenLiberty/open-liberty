/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.metatype;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

import test.common.SharedOutputManager;

/**
 * ExtendedObjectClassDefinitionImplTest
 */
public class ExtendedObjectClassDefinitionImplTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    public static final String EXPECTED_ALIAS = "testAlias";

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    /**
     * Tests getAlias() with empty location.
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithEmptyLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(EXPECTED_ALIAS.equals(alias));
    }

    /**
     * Tests getAlias() with a null location.
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithNullLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = null;
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(EXPECTED_ALIAS.equals(alias));
    }

    /**
     * Test getAlias() with a location not containing prefix: kernel@reference
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithRuntimeKernelAtLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "kernel@reference:file:/C:/rtc/workspaces/xopen/build.image/wlp/lib/com.ibm.ws.compat_1.0.1.jar";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(EXPECTED_ALIAS.equals(alias));
    }

    /**
     * Test getAlias() with a location not containing prefix: feature@reference
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithRuntimeFeatureAtLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "feature@reference:file:/C:/rtc/workspaces/xopen/build.image/wlp/lib/com.ibm.ws.kernel.fileinstall_1.0.jar";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(EXPECTED_ALIAS.equals(alias));;
    }

    /**
     * Test getAlias() with a location not containing feature@ or kernel@.
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithPlainURLLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "file:/C:/rtc/workspaces/xopen/build.image/wlp/usr/servers/com.ibm.ws.config.bvt/bundles/two.test.server.config_1.0.jar";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(alias == null);
    }

    /**
     * Tests getAlias() with a location containing a productExtension tag.
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasWithUSRProductExtensionLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "feature@productExtension:testproduct:reference:file:/C:/rtc/workspaces/xopen/build.image/wlp/producttest/lib/test.prod.extensions_1.0.0.jar";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(("testproduct_" + EXPECTED_ALIAS).equals(alias));
    }

    /**
     * Tests getAlias() with a location containing a user productExtension tag.
     *
     * @throws Exception
     */
    @Test
    public void testgetAliasEmptyLocation() throws Exception {
        EquinoxObjectClassDefinition eocdMock = createExtOBjClassDef();
        String location = "feature@productExtension:usr:reference:file:/C:/rtc/workspaces/xopen/build.image/wlp/usr/extension/lib/test.user.prod.extensions_1.0.0.jar";
        ExtendedObjectClassDefinitionImpl eocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(eocdMock, location);
        String alias = eocd.getAlias();
        assertTrue(("usr_" + EXPECTED_ALIAS).equals(alias));
    }

    /**
     * Creates a Equinox Extended Object Class Definition mock object.
     *
     * @return Equinox Extended Object Class Definition mock object.
     */
    public EquinoxObjectClassDefinition createExtOBjClassDef() {
        final Mockery mockery = new Mockery();
        final EquinoxObjectClassDefinition eocdMock = mockery.mock(EquinoxObjectClassDefinition.class);
        final Set<String> uris = new HashSet<String>();
        uris.add(XMLConfigConstants.METATYPE_EXTENSION_URI);
        final HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(ExtendedObjectClassDefinition.ALIAS_ATTRIBUTE, EXPECTED_ALIAS);
        mockery.checking(new Expectations() {
            {
                oneOf(eocdMock).getExtensionUris();
                will(returnValue(uris));
                oneOf(eocdMock).getExtensionAttributes(XMLConfigConstants.METATYPE_EXTENSION_URI);
                will(returnValue(attributes));
                oneOf(eocdMock).getAttributeDefinitions(ObjectClassDefinition.ALL);
                will(returnValue(new EquinoxAttributeDefinition[] {}));
            }
        });

        return eocdMock;
    }
}
