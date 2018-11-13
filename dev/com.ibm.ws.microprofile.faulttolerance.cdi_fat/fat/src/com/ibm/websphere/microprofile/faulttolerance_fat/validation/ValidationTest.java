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
package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.AbstractSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.AbstractSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericArraySearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericArraySearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericComplexSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericComplexSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericLongSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericLongSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericLongSearchC;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericWildcardSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericWildcardSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InPackageSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InPackageSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InterfaceSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InterfaceSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InterfaceSearchC;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.PrivateSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SimpleSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SuperclassSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SuperclassSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.VarargsSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.WildcardSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.OutOfPackageSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.SubclassSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.SubclassSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.SuperclassPrivateSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.SuperclassPrivateSearchB;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.WildcardNegativeSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.subpackage.OutOfPackageSearchB;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@AllowedFFDC
public class ValidationTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("FaultToleranceMultiModule");

    @AfterClass
    public static void shutdown() throws Exception {
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }

    @Test
    public void testAsyncMethodNotReturningFuture() throws Exception {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(AsyncMethodNotReturningFuture.class)
                        .failsWith("CWMFT5001E")
                        .run();
    }

    @Test
    public void testAsyncClassNotReturningFuture() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(AsyncClassNotReturningFuture.class)
                        .failsWith("CWMFT5001E")
                        .run();
    }

    @Test
    public void testFallbackMethodNotExist() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodNotExist.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodWrongParameters() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodWrongParameters.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodWrongReturnType() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackMethodWrongReturnType.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackHandlerWrongType() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackHandlerWrongType.class)
                        .failsWith("CWMFT5008E")
                        .run();
    }

    @Test
    public void testFallbackDefinesHandlerAndMethod() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(FallbackDefinesHandlerAndMethod.class)
                        .failsWith("CWMFT5009E")
                        .run();
    }

    @Test
    public void testRetryNegativeDelay() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeDelay.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeDuration() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeDuration.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeJitter() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeJitter.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryNegativeRetries() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryNegativeRetries.class)
                        .failsWith("CWMFT5010E")
                        .run();
    }

    @Test
    public void testRetryDelayLongerThanDuration() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryDelayLongerThanDuration.class)
                        .failsWith("CWMFT5017E")
                        .run();
    }

    @Test
    public void testRetryJitterLongerThanDelay() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(RetryJitterLongerThanDelay.class)
                        .succeedsWith("CWMFT5019W")
                        .run();
    }

    @Test
    public void testTimeoutNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(TimeoutNegative.class)
                        .failsWith("CWMFT5011E")
                        .run();
    }

    @Test
    public void testBulkheadConcurrentNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(BulkheadConcurrentNegative.class)
                        .failsWith("CWMFT5016E")
                        .run();
    }

    @Test
    public void testBulkheadQueueNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(BulkheadQueueNegative.class)
                        .failsWith("CWMFT5016E")
                        .run();
    }

    @Test
    public void testCircuitBreakerDelayNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerDelayNegative.class)
                        .failsWith("CWMFT5012E")
                        .run();
    }

    @Test
    public void testCircuitBreakerThresholdZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerThresholdZero.class)
                        .failsWith("CWMFT5014E")
                        .run();
    }

    @Test
    public void testCircuitBreakerThresholdNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerThresholdNegative.class)
                        .failsWith("CWMFT5014E")
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioOne() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioOne.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioZero.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioNegative.class)
                        .failsWith("CWMFT5013E")
                        .run();
    }

    @Test
    public void testCircuitBreakerRatioTooLarge() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerRatioTooLarge.class)
                        .failsWith("CWMFT5013E")
                        .run();
    }

    @Test
    public void testCircuitBreakerSuccessThresholdZero() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerSuccessThresholdZero.class)
                        .failsWith("CWMFT5015E")
                        .run();
    }

    @Test
    public void testCircuitBreakerSuccessThresholdNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerSuccessThresholdNegative.class)
                        .failsWith("CWMFT5015E")
                        .run();
    }

    @Test
    public void testCircuitBreakerFailOnEmpty() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(CircuitBreakerFailOnEmpty.class)
                        .failsWith("CWMFT5018E")
                        .run();
    }

    @Test
    public void testFallbackMethodSimple() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(SimpleSearch.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodPrivate() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(PrivateSearch.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodInSuperclass() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(SuperclassSearchA.class)
                        .withClass(SuperclassSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodGeneric() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(GenericSearchA.class)
                        .withClass(GenericSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodGenericLong() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(GenericLongSearchA.class)
                        .withClass(GenericLongSearchB.class)
                        .withClass(GenericLongSearchC.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodGenericComplex() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(GenericComplexSearchA.class)
                        .withClass(GenericComplexSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodGenericArray() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(GenericArraySearchA.class)
                        .withClass(GenericArraySearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodInPackage() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(InPackageSearchA.class)
                        .withClass(InPackageSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodAbstract() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(AbstractSearchA.class)
                        .withClass(AbstractSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodOnInterface() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(InterfaceSearchA.class)
                        .withClass(InterfaceSearchB.class)
                        .withClass(InterfaceSearchC.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodOutOfPackage() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(OutOfPackageSearchA.class)
                        .withClass(OutOfPackageSearchB.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodSuperclassPrivate() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(SuperclassPrivateSearchA.class)
                        .withClass(SuperclassPrivateSearchB.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodSubclass() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(SubclassSearchA.class)
                        .withClass(SubclassSearchB.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodWildcard() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(WildcardSearch.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodGenericWildcard() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(GenericWildcardSearchA.class)
                        .withClass(GenericWildcardSearchB.class)
                        .succeeds()
                        .run();
    }

    @Test
    public void testFallbackMethodWildcardNegative() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(WildcardNegativeSearch.class)
                        .failsWith("CWMFT5021E")
                        .run();
    }

    @Test
    public void testFallbackMethodVarargs() {
        AppValidator.validateAppOn(SHARED_SERVER)
                        .withClass(VarargsSearch.class)
                        .succeeds()
                        .run();
    }

}
