/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package wlp.lib.extract;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class SelfExtractorTest {

//    protected final Mockery mockery = new JUnit4Mockery() {
//        {
//            setImposteriser(ClassImposteriser.INSTANCE);
//        }
//    };
//
//    String expectedkeyID = String.format("%x", 1L);
//    File outputDir = mockery.mock(File.class);

    /**
     * TODO: enable it later
     * Test method for {@link wlp.lib.extract.SelfExtractor#validateProductMatches(java.io.File, java.util.List)}.
     */
    @Test
    @Ignore
    public void testmatchLibertyPropertiesInvalidEdition() {
        String appliesTo = "com.ibm.websphere.appserver; productVersion=24.0.0.1; productEdition=Open; editions=\"BASE, DEVELOPER\"";
        List productMatches = SelfExtractor.parseAppliesTo(appliesTo);
        appliesTo = "com.ibm.websphere.appserver; productVersion=24.0.0.2; productEdition=Open; editions=\"BASE, DEVELOPER\"";
        productMatches.addAll(SelfExtractor.parseAppliesTo(appliesTo));
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "LIBERTY_CORE");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.matchLibertyProperties(productMatches, props);
        assertTrue("Message key should be invalidEdition but it was '" + rc.getMessageKey() + "'", rc.getMessageKey().equals("invalidEdition"));

    }

}
