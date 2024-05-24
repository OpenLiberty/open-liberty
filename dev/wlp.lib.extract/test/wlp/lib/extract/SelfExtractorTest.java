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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

/**
 *
 */
public class SelfExtractorTest {

    /**
     *
     * Test method for {@link wlp.lib.extract.SelfExtractor#validateProductMatches(java.io.File, java.util.List)}.
     */
    @Test
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

    @Test
    public void testGetReturnCodeInvalidVersion() {
        ProductMatch match = new ProductMatch();
        match.setVersion("testVersion");
        match.setEditions(Arrays.asList(new String[] { "edition" }));
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.INVALID_VERSION);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.BAD_OUTPUT);
        assertEquals("Message key does not match.", rc.getMessageKey(), "invalidVersion");

    }

    @Test
    public void testGetReturnCodeInvalidEdition() {
        ProductMatch match = new ProductMatch();
        match.setVersion("testVersion");
        match.setEditions(Arrays.asList(new String[] { "edition" }));
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.INVALID_EDITION);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.BAD_OUTPUT);
        assertEquals("Message key does not match.", rc.getMessageKey(), "invalidEdition");

    }

    @Test
    public void testGetReturnCodeInvalidInstallType() {
        ProductMatch match = new ProductMatch();
        match.setInstallType("installType");
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.INVALID_INSTALL_TYPE);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.BAD_OUTPUT);
        assertEquals("Message key does not match.", rc.getMessageKey(), "invalidInstallType");

    }

    @Test
    public void testGetReturnCodeInvalidLicense() {
        ProductMatch match = new ProductMatch();
        match.setLicenseType("license");
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.INVALID_LICENSE);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.BAD_OUTPUT);
        assertEquals("Message key does not match.", rc.getMessageKey(), "invalidLicense");

    }

    @Test
    public void testGetReturnCodeNotApplicable() {
        ProductMatch match = new ProductMatch();
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.NOT_APPLICABLE);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.OK.getCode());
    }

    @Test
    public void testGetReturnCodeMatched() {
        ProductMatch match = new ProductMatch();
        Properties props = new Properties();
        props.put("com.ibm.websphere.productId", "com.ibm.websphere.appserver");
        props.put("com.ibm.websphere.productVersion", "24.0.0.1");
        props.put("com.ibm.websphere.productEdition", "edition");
        props.put("com.ibm.websphere.productLicenseType", "IPLA");
        props.put("com.ibm.websphere.productInstallType", "Archive");

        ReturnCode rc = SelfExtractor.getReturnCode(props, match, ProductMatch.MATCHED);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.OK.getCode());
    }

    @Test
    public void testValidatePropertiesEmptyProp() {
        List<Properties> props_list = new ArrayList<>();
        List productMatches = new ArrayList<>();
        ReturnCode rc = SelfExtractor.validateProperties(productMatches, props_list);
        assertEquals("Return code does not match.", rc.getCode(), ReturnCode.BAD_OUTPUT);

    }

}
