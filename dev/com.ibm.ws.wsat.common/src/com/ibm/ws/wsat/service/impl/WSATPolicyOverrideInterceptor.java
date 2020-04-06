/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicyImpl;
import org.apache.cxf.ws.policy.EndpointPolicyImpl;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.apache.cxf.ws.policy.PolicyOutInterceptor;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 *
 */
public class WSATPolicyOverrideInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent TC = Tr.register(WSATPolicyOverrideInterceptor.class);

    private static final String PATH = "/META-INF/wsat-policy.xml";

    private static final boolean ISREMOVE = false; // default value is false; false -> add wsat policy || true -> remove wsat policy

    private final boolean remove;

    public WSATPolicyOverrideInterceptor() {
        super(Phase.SETUP);
        getAfter().add(PolicyOutInterceptor.class.getName());
        this.remove = ISREMOVE;
    }

    public WSATPolicyOverrideInterceptor(boolean remove) {
        super(Phase.SETUP);
        getAfter().add(PolicyOutInterceptor.class.getName());
        this.remove = remove;
    }

    private Policy generateWSATPolicy(Message msg) throws Throwable {
        Element policyElement;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Start to loadd policy from template", PATH);
        }
        InputStream is = this.getClass().getResourceAsStream(PATH);
        if (is == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "InputStream is NULL??");
            }
            throw new RuntimeException("InputStream Object is null, not able to generate ws-at policy...");
        } else {
            policyElement = DOMUtils.readXml(is).getDocumentElement();
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Try getting policy element", policyElement);
            }
            PolicyBuilder builder = msg.getExchange()
                            .getBus()
                            .getExtension(PolicyBuilder.class);

            Policy wsatPolicy = builder.getPolicy(policyElement);
            if (TC.isDebugEnabled())
                Tr.debug(TC, "Try extract to policy object : ", wsatPolicy);
            return wsatPolicy;
        }
    }

    private void insertPolicy(Message msg, Policy p, AssertionInfoMap map) throws Throwable {
        Exchange exchange = msg.getExchange();
        Bus bus = exchange.get(Bus.class);

        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        if (null == boi) {
            throw new RuntimeException("Failed to get binding operation info");
        }

        Endpoint e = exchange.get(Endpoint.class);
        if (null == e) {
            throw new RuntimeException("Failed to get endpoint");
        }

        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            throw new RuntimeException("Failed to get policy engine");
        }

        EndpointPolicyImpl endpi = new EndpointPolicyImpl(p);
        EffectivePolicyImpl effectivePolicy = new EffectivePolicyImpl();
        effectivePolicy.initialise(endpi, (PolicyEngineImpl) pe, false);

        Collection<Assertion> assertions = new ArrayList<Assertion>();
        assertions.addAll(effectivePolicy.getChosenAlternative());

        AssertionInfoMap tempMap = new AssertionInfoMap(assertions);
        map.putAll(tempMap);

        msg.getInterceptorChain().add(effectivePolicy.getInterceptors());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message msg) throws Fault {
        try {
            AssertionInfoMap map = msg.get(AssertionInfoMap.class);
            if (!remove && map.getAssertionInfo(Constants.AT_ASSERTION_QNAME).isEmpty()) {
                Policy p = generateWSATPolicy(msg);
                insertPolicy(msg, p, map);
            } else if (remove && !map.getAssertionInfo(Constants.AT_ASSERTION_QNAME).isEmpty()) {
                map.remove(Constants.AT_ASSERTION_QNAME);
            }

        } catch (Throwable e) {
            throw new Fault(e);
        }

    }

}
