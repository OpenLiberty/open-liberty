/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.wsat.utils;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.PrimitiveAssertion;
import org.junit.Test;

import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.Constants.AssertionStatus;

/**
 * Tests {@link WSCoorUtil#isOptional(Message, boolean)} for the cases in which the policy of the target
 * endpoint cannot be determined.
 *
 * <p>In {@code propagation="check_wsdl"} mode {@code CoorContextOutInterceptor} suppresses the WS-AT
 * coordination context whenever this method returns {@link AssertionStatus#NULL}, so every
 * indeterminate case must return {@code NULL}, exactly as for an endpoint with no WS-AT assertion.
 *
 * <p>These cases are covered by a unit test rather than a FAT because they cannot be reproduced
 * through a real service call: a JAX-WS proxy or {@code Dispatch} always has an {@code Endpoint} on
 * its exchange, and a client whose policy contains the WS-AT assertion never reaches
 * {@code isOptional} at all (see {@code CoorContextOutInterceptor}, which tests
 * {@link WSCoorUtil#assertAT} first). The {@code Dispatch} client without a WSDL is covered by
 * {@code ConditionalPropagationTest} in {@code com.ibm.ws.wsat_fat.assertion}.
 */
public class WSCoorUtilTest {

    /** CXF exchange without an {@link Endpoint}. */
    @Test
    public void testNoEndpointOnExchange() {
        assertEquals(AssertionStatus.NULL, WSCoorUtil.isOptional(message(exchange(null, null)), false));
    }

    /** The policy engine finds no effective policy at all. */
    @Test
    public void testNoEffectivePolicy() {
        assertEquals(AssertionStatus.NULL, WSCoorUtil.isOptional(message(exchange(endpoint(), policyEngine(null))), false));
    }

    /** The effective policy exists but contains no policy components, as for an endpoint with no WSDL policy. */
    @Test
    public void testEmptyEffectivePolicy() {
        assertEquals(AssertionStatus.NULL, WSCoorUtil.isOptional(message(exchange(endpoint(), policyEngine(effectivePolicy(new Policy())))), false));
    }

    /** The effective policy has components, but none of them is a WS-AT assertion. */
    @Test
    public void testEffectivePolicyWithoutWsatAssertion() {
        final All all = new All();
        all.addAssertion(new PrimitiveAssertion(new QName("http://example.com/other", "SomeAssertion")));
        assertEquals(AssertionStatus.NULL, WSCoorUtil.isOptional(message(exchange(endpoint(), policyEngine(effectivePolicy(policy(all))))), false));
    }

    /** Control: a required WS-AT assertion is reported, so the {@code NULL} results above are not just a default. */
    @Test
    public void testRequiredAssertionIsReported() {
        final All all = new All();
        all.addAssertion(new PrimitiveAssertion(Constants.AT_ASSERTION_QNAME));
        assertEquals(AssertionStatus.FALSE, WSCoorUtil.isOptional(message(exchange(endpoint(), policyEngine(effectivePolicy(policy(all))))), false));
    }

    /** Control: an optional WS-AT assertion (an empty {@code All} alongside the assertion) is reported. */
    @Test
    public void testOptionalAssertionIsReported() {
        final All required = new All();
        required.addAssertion(new PrimitiveAssertion(Constants.AT_ASSERTION_QNAME));
        assertEquals(AssertionStatus.TRUE, WSCoorUtil.isOptional(message(exchange(endpoint(), policyEngine(effectivePolicy(policy(required, new All()))))), false));
    }

    // ---------------------------------------------------------------------------------------------
    // Minimal stand-ins for the CXF interfaces that isOptional reads
    // ---------------------------------------------------------------------------------------------

    /** {@code /Policy/ExactlyOne/All...} in the normalised form that {@code isOptionalAssertion} expects. */
    private static Policy policy(All... alternatives) {
        final ExactlyOne exactlyOne = new ExactlyOne();
        for (All alternative : alternatives) {
            exactlyOne.addPolicyComponent(alternative);
        }
        final Policy policy = new Policy();
        policy.addPolicyComponent(exactlyOne);
        return policy;
    }

    private static Message message(Exchange exchange) {
        return proxy(Message.class, (p, m, a) -> "getExchange".equals(m.getName()) ? exchange : defaultValue(m.getReturnType()));
    }

    private static Exchange exchange(Endpoint endpoint, PolicyEngine policyEngine) {
        final Bus bus = proxy(Bus.class, (p, m, a) -> "getExtension".equals(m.getName()) && a[0] == PolicyEngine.class ? policyEngine : defaultValue(m.getReturnType()));
        return proxy(Exchange.class, (p, m, a) -> {
            if ("get".equals(m.getName()) && a.length == 1 && a[0] instanceof Class) {
                if (a[0] == Bus.class) {
                    return bus;
                }
                if (a[0] == Endpoint.class) {
                    return endpoint;
                }
            }
            return defaultValue(m.getReturnType());
        });
    }

    private static Endpoint endpoint() {
        final EndpointInfo info = new EndpointInfo();
        return proxy(Endpoint.class, (p, m, a) -> "getEndpointInfo".equals(m.getName()) ? info : defaultValue(m.getReturnType()));
    }

    private static PolicyEngine policyEngine(EffectivePolicy effectivePolicy) {
        return proxy(PolicyEngine.class,
                     (p, m, a) -> "getEffectiveClientRequestPolicy".equals(m.getName()) ? effectivePolicy : defaultValue(m.getReturnType()));
    }

    private static EffectivePolicy effectivePolicy(Policy policy) {
        return proxy(EffectivePolicy.class, (p, m, a) -> "getPolicy".equals(m.getName()) ? policy : defaultValue(m.getReturnType()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(WSCoorUtilTest.class.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type.isPrimitive() && type != void.class) {
            return 0;
        }
        return null;
    }
}
