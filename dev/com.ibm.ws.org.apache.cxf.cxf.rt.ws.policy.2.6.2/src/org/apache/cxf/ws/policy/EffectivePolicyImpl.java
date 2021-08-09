/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyContainingAssertion;

/**
 * 
 */
public class EffectivePolicyImpl implements EffectivePolicy {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(EffectivePolicyImpl.class); 
    private static final Logger LOG = LogUtils.getL7dLogger(EffectivePolicyImpl.class);
    
    protected Policy policy;     
    protected Collection<Assertion> chosenAlternative;
    protected List<Interceptor<? extends org.apache.cxf.message.Message>> interceptors;
    
    public Policy getPolicy() {
        return policy;        
    }
    
    public List<Interceptor<? extends org.apache.cxf.message.Message>> getInterceptors() {
        return interceptors;
    }
    
    public Collection<Assertion> getChosenAlternative() {
        return chosenAlternative;
    }
    
    public void initialise(EndpointPolicyImpl epi, PolicyEngineImpl engine, boolean inbound) {
        initialise(epi, engine, inbound, false);
    }

    public void initialise(EndpointPolicyImpl epi, PolicyEngineImpl engine, boolean inbound, boolean fault) {
        policy = epi.getPolicy();
        chosenAlternative = epi.getChosenAlternative();
        if (chosenAlternative == null) {
            chooseAlternative(engine, null);
        }
        initialiseInterceptors(engine, inbound, fault);  
    }
    
    public void initialise(EndpointInfo ei, 
                    BindingOperationInfo boi, 
                    PolicyEngineImpl engine, 
                    Assertor assertor,
                    boolean requestor,
                    boolean request) {
        initialisePolicy(ei, boi, engine, requestor, request, assertor);
        chooseAlternative(engine, assertor);
        initialiseInterceptors(engine, false);  
    }
    
    public void initialise(EndpointInfo ei, 
                    BindingOperationInfo boi, 
                    PolicyEngineImpl engine, 
                    Assertor assertor,
                    boolean requestor,
                    boolean request,
                    List<List<Assertion>> incoming) {
        initialisePolicy(ei, boi, engine, requestor, request, assertor);
        chooseAlternative(engine, assertor, incoming);
        initialiseInterceptors(engine, false);  
    }
    
    public void initialise(EndpointInfo ei, 
                    BindingOperationInfo boi, 
                    PolicyEngineImpl engine, 
                    boolean requestor, boolean request) {
        Assertor assertor = initialisePolicy(ei, boi, engine, requestor, request, null);
        chooseAlternative(engine, assertor);
        initialiseInterceptors(engine, requestor);  
    }
    
    public void initialise(EndpointInfo ei, 
                    BindingOperationInfo boi, 
                    BindingFaultInfo bfi, 
                    PolicyEngineImpl engine, 
                    Assertor assertor) {
        initialisePolicy(ei, boi, bfi, engine);
        chooseAlternative(engine, assertor);
        initialiseInterceptors(engine, false);  
    }
    
    private <T> T getAssertorAs(Assertor as, Class<T> t) {
        if (t.isInstance(as)) {
            return t.cast(as);
        } else if (as instanceof PolicyUtils.WrappedAssertor) {
            Object o = ((PolicyUtils.WrappedAssertor)as).getWrappedAssertor();
            if (t.isInstance(o)) {
                return t.cast(o);
            }
        }
        return null;
    }
    Assertor initialisePolicy(EndpointInfo ei,
                          BindingOperationInfo boi,  
                          PolicyEngineImpl engine, 
                          boolean requestor, boolean request,
                          Assertor assertor) {
        
        if (boi.isUnwrapped()) {
            boi = boi.getUnwrappedOperation();
        }
        
        BindingMessageInfo bmi = request ? boi.getInput() : boi.getOutput();
        EndpointPolicy ep;
        if (requestor) {
            ep = engine.getClientEndpointPolicy(ei, getAssertorAs(assertor, Conduit.class));
        } else {
            ep = engine.getServerEndpointPolicy(ei, getAssertorAs(assertor, Destination.class));
        }
        policy = ep.getPolicy();
        if (ep instanceof EndpointPolicyImpl) {
            assertor = ((EndpointPolicyImpl)ep).getAssertor();
        }
        
        policy = policy.merge(engine.getAggregatedOperationPolicy(boi));
        if (null != bmi) {
            policy = policy.merge(engine.getAggregatedMessagePolicy(bmi));
        }
        policy = policy.normalize(engine.getRegistry(), true);
        return assertor;
    }
    
    void initialisePolicy(EndpointInfo ei, BindingOperationInfo boi,
                          BindingFaultInfo bfi, PolicyEngineImpl engine) {
        policy = engine.getServerEndpointPolicy(ei, (Destination)null).getPolicy();         
        policy = policy.merge(engine.getAggregatedOperationPolicy(boi));
        if (bfi != null) {
            policy = policy.merge(engine.getAggregatedFaultPolicy(bfi));
        }
        policy = policy.normalize(engine.getRegistry(), true);
    }

    void chooseAlternative(PolicyEngineImpl engine, Assertor assertor) {
        chooseAlternative(engine, assertor, null);
    }
    void chooseAlternative(PolicyEngineImpl engine, Assertor assertor, List<List<Assertion>> incoming) {
        Collection<Assertion> alternative = engine.getAlternativeSelector()
            .selectAlternative(policy, engine, assertor, incoming);
        if (null == alternative) {
            PolicyUtils.logPolicy(LOG, Level.FINE, "No alternative supported.", getPolicy());
            throw new PolicyException(new Message("NO_ALTERNATIVE_EXC", BUNDLE));
        } else {
            setChosenAlternative(alternative);
        }   
    }

    void initialiseInterceptors(PolicyEngineImpl engine) {
        initialiseInterceptors(engine, false);
    }
    
    void initialiseInterceptors(PolicyEngineImpl engine, boolean useIn) {
        initialiseInterceptors(engine, useIn, false);
    }

    void initialiseInterceptors(PolicyEngineImpl engine, boolean useIn, boolean fault) {
        if (engine.getBus() != null) {
            PolicyInterceptorProviderRegistry reg 
                = engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class);
            Set<Interceptor<? extends org.apache.cxf.message.Message>> out 
                = new LinkedHashSet<Interceptor<? extends org.apache.cxf.message.Message>>();
            for (Assertion a : getChosenAlternative()) {
                initialiseInterceptors(reg, engine, out, a, useIn, fault);
            }        
            setInterceptors(new ArrayList<Interceptor<? extends  org.apache.cxf.message.Message>>(out));
        }
    }
    
    
    protected Collection<Assertion> getSupportedAlternatives(PolicyEngineImpl engine,
                                                                   Policy p) {
        Collection<Assertion> alternatives = new ArrayList<Assertion>();

        for (Iterator<List<Assertion>> it = p.getAlternatives(); it.hasNext();) {
            List<Assertion> alternative = it.next();
            if (engine.supportsAlternative(alternative, null)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }

    void initialiseInterceptors(PolicyInterceptorProviderRegistry reg, PolicyEngineImpl engine,
                                Set<Interceptor<? extends org.apache.cxf.message.Message>> out, Assertion a,
                                boolean useIn, boolean fault) {
        QName qn = a.getName();
        
        List<Interceptor<? extends org.apache.cxf.message.Message>> i = null;
        if (useIn && !fault) {
            i = reg.getInInterceptorsForAssertion(qn);
        } else if (!useIn && !fault) {
            i = reg.getOutInterceptorsForAssertion(qn);
        } else if (useIn && fault) {
            i = reg.getInFaultInterceptorsForAssertion(qn);
        } else if (!useIn && fault) {
            i = reg.getOutFaultInterceptorsForAssertion(qn);
        }
        out.addAll(i);

        if (a instanceof PolicyContainingAssertion) {
            Policy p = ((PolicyContainingAssertion)a).getPolicy();
            if (p != null) {
                for (Assertion a2 : getSupportedAlternatives(engine, p)) {
                    initialiseInterceptors(reg, engine, out, a2, useIn, fault);
                }
            }
        }
    }
    
    // for tests
    
    void setPolicy(Policy ep) {
        policy = ep;
    }
    
    void setChosenAlternative(Collection<Assertion> c) {
        chosenAlternative = c;
    }
    
    void setInterceptors(List<Interceptor<? extends org.apache.cxf.message.Message>> out) {
        interceptors = out;
    }
   
}
