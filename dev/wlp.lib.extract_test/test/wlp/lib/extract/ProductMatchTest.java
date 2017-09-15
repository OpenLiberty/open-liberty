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
package wlp.lib.extract;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ProductMatchTest {
    /**  */
    private static final String WAS_PRODUCT_ID = "com.ibm.websphere.appserver";
    private static final String PRODUCT_VERSION_PROPERTY = "com.ibm.websphere.productVersion";
    private static final String PRODUCT_ID_PROPERTY = "com.ibm.websphere.productId";

    private final Properties targetPlatform = new Properties();

    /* Version Range Tests */
    @Test
    public void testBasicNumericVersionMatches() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.0");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testBasicNumericVersionNotMatches() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testBasicStringVersionMatches() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.next.beta");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.next.beta");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testBasicStringVersionNotMatches() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.next.beta");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.next.foo");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testStringVersionNotMatchesNumericVersion() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.next.beta");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.0.1");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testNumericVersionNotMatchesStringVersion() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.next.beta");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testVersionRangeMatchesMinimum() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.1");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeMatchesGreaterModMultiDigits() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.50.0.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionMultiDigitsRangeMatchesGreaterModMultiDigits() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.100.1.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.50.0.0+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeMatchesGreaterFixpack() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.2");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeMatchesGreaterMod() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.6.1");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeMatchesGreaterRelease() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.6.4.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeMatchesGreaterVersion() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "9.4.3.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.MATCHED, result);
    }

    @Test
    public void testVersionRangeNotMatchesLesserFixpack() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.5.0");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testVersionRangeNotMatchesLesserMod() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.4.1");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testVersionRangeNotMatchesLesserRelease() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.4.5.1");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testVersionRangeNotMatchesLesserVersion() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "7.5.5.1");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testVersionRangeNotMatchesStringVersion() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put(PRODUCT_VERSION_PROPERTY, "8.5.next.beta");

        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productVersion=8.5.5.1+");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("Result from product matcher should match expected value", ProductMatch.INVALID_VERSION, result);
    }

    @Test
    public void testLicenseMismatch() {
        targetPlatform.put(PRODUCT_ID_PROPERTY, WAS_PRODUCT_ID);
        targetPlatform.put("productLicenseType", "ILAN");
        ProductMatch productMatch = new ProductMatch();
        productMatch.add(WAS_PRODUCT_ID);
        productMatch.add("productLicenseType=IPLA");

        int result = productMatch.matches(targetPlatform);
        Assert.assertEquals("The license type should be indicated as invalid", ProductMatch.INVALID_LICENSE, result);

    }
}
