/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.common.crypto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 *
 */
public class CryptoUtilsTest {

    @Before
    public void setUp() {
        // useEnhancedSecurityAlgorithms() caches its return value, reset the cached value to ensure test isolation.
        CryptoUtils.isEnhancedSecurityChecked = false;
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_ibmJcePlusFipsAvailable() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(true);
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();

            CryptoUtils.fips140_3Checked = false;
            assertTrue("Expected FIPS 140-3 to be enabled, but was disabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_openJcePlusFipsAvailable() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(true);
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();

            CryptoUtils.fips140_3Checked = false;
            assertTrue("Expected FIPS 140-3 to be enabled, but was disabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_ibmJcePlusFipsProviderAvailable() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(true);
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();

            CryptoUtils.fips140_3Checked = false;
            assertTrue("Expected FIPS 140-3 to be enabled, but was disabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_openJcePlusFipsProviderAvailable() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(true);
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();

            CryptoUtils.fips140_3Checked = false;
            assertTrue("Expected FIPS 140-3 to be enabled, but was disabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_noProvidersAvailable_useFipsProvider_true_fipsProviderName_ibmJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("IBMJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);
            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            // Edge case where fipsLevel is 140-3, but isFips140_2Enabled returns true.
            // This happens because the fipsLevel is 140-3, but IBMJCEPlusFIPS nor OpenJCEPlusFIPS are available so the FIPS 140-3 check fails.
            // However, the usefipsprovider and usefipsProviderName are set correctly so the FIPS 140-2 check passes.
            // This is probably okay to leave as-is as the server admin will likely try to resolve the missing provider
            // issue to use the intended FIPS 140-3 and then the FIPS 140-3 check will pass and the FIPS 140-2 check will fail.
            assertTrue("Expected FIPS 140-2 to be enabled, but was disabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_noProvidersAvailable_useFipsProvider_true_fipsProviderName_openJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("OpenJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_noProvidersAvailable_useFipsProvider_true_fipsProviderName_noProviderName() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("NO_PROVIDER_NAME");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_noProvidersAvailable_useFipsProvider_false() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("false");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_3_noProvidersAvailable_useFipsProvider_invalid() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-3");
            mock.when(CryptoUtils::isIBMJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSAvailable).thenReturn(false);
            mock.when(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable).thenReturn(false);
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("abc");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isIBMJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::isOpenJCEPlusFIPSProviderAvailable, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_2_useFipsProvider_true_fipsProviderName_ibmJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-2");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("IBMJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertTrue("Expected FIPS 140-2 to be enabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_2_useFipsProvider_true_fipsProviderName_openJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-2");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("OpenJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_2_useFipsProvider_true_fipsProviderName_noProviderName() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-2");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("NO_PROVIDER_NAME");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_2_useFipsProvider_false() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-2");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("false");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));
            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_140_2_useFipsProvider_invalid() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("140-2");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("abc");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_disabled_useFipsProvider_true_fipsProviderName_ibmJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("IBMJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            // Edge case where fipsLevel is disabled, but isFips140_2Enabled returns true.
            // This may occur when using older IBM JDK's where com.ibm.fips.mode
            // may not exist yet, so getFipsLevel would return disabled, so
            // this getFips140_2Enabled result here is valid. Also, usefipsprovider
            // and usefipsProviderName are IBM JDK properties, so we don't need to worry
            // about the Semeru JDK case.
            assertTrue("Expected FIPS 140-2 to be enabled, but was disabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_disabled_useFipsProvider_true_fipsProviderName_openJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("OpenJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_disabled_useFipsProvider_true_fipsProviderName_noProviderName() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("NO_PROVIDER_NAME");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_disabled_useFipsProvider_false() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("false");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_disabled_useFipsProvider_invalid() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("abc");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_invalid_useFipsProvider_true_fipsProviderName_ibmJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("abc");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("IBMJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            // Edge case where fipsLevel is abc, but isFips140_2Enabled returns true.
            // I don't think this will ever actually occur because the IBM JDK won't let you set
            // com.ibm.fips.mode to abc. The Semeru JDK does let you set it to abc,
            // but usefipsprovider and usefipsProviderName are IBM JDK properties, so this combination
            // wouldn't occur in practice.
            assertTrue("Expected FIPS 140-2 to be enabled, but was disabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_invalid_useFipsProvider_true_fipsProviderName_openJcePlusFips() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("abc");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("OpenJCEPlusFIPS");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_invalid_useFipsProvider_true_fipsProviderName_noProviderName() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("abc");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("true");
            mock.when(CryptoUtils::getFipsProviderName).thenReturn("NO_PROVIDER_NAME");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::getFipsProviderName, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_invalid_useFipsProvider_false() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("abc");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("false");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_fipsLevel_invalid_useFipsProvider_invalid() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("abc");
            mock.when(CryptoUtils::getUseFipsProvider).thenReturn("abc");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(false);

            CryptoUtils.fips140_3Checked = false;
            assertFalse("Expected FIPS 140-3 to be disabled, but was enabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::getUseFipsProvider, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testFipsEnabled_useEnhancedSecurityAlgorithms_true() {
        try (MockedStatic<CryptoUtils> mock = Mockito.mockStatic(CryptoUtils.class)) {
            mock.when(CryptoUtils::getFipsLevel).thenReturn("disabled");
            mock.when(CryptoUtils::isFips140_3Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::isFips140_2Enabled).thenCallRealMethod();
            mock.when(CryptoUtils::useEnhancedSecurityAlgorithms).thenReturn(true);

            CryptoUtils.fips140_3Checked = false;
            assertTrue("Expected FIPS 140-3 to be enabled, but was disabled.", CryptoUtils.isFips140_3Enabled());
            assertFalse("Expected FIPS 140-2 to be disabled, but was enabled.", CryptoUtils.isFips140_2Enabled());

            mock.verify(CryptoUtils::getFipsLevel, Mockito.times(1));
            mock.verify(CryptoUtils::isFips140_3Enabled, Mockito.times(2));
            mock.verify(CryptoUtils::isFips140_2Enabled, Mockito.times(1));
            mock.verify(CryptoUtils::useEnhancedSecurityAlgorithms, Mockito.times(1));

            mock.verifyNoMoreInteractions();
        }
    }

}
