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
import java.util.Collections;
import java.util.ResourceBundle;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.attachment.external.ExternalAttachmentProvider;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;


/**
 * Configures a Server, Client, Bus with the specified policies. If a series of 
 * Policy <code>Element</code>s are supplied, these will be loaded into a Policy
 * class using the <code>PolicyBuilder</code> extension on the bus. If the 
 * PolicyEngine has not been started, this feature will start it.
 *
 * @see PolicyBuilder
 * @see AbstractFeature
 */
@NoJSR250Annotations
public class WSPolicyFeature extends AbstractFeature {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(WSPolicyFeature.class);
    
    private Collection<Policy> policies;
    private Collection<Element> policyElements;
    private Collection<Element> policyReferenceElements;
    private boolean ignoreUnknownAssertions;
    private AlternativeSelector alternativeSelector; 
    private boolean enabled = true;
  
       

    public WSPolicyFeature() {
        super();
    }

    public WSPolicyFeature(Policy... ps) {
        super();
        policies = new ArrayList<Policy>();
        Collections.addAll(policies, ps);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void initialize(Bus bus) {
        
        // this should never be null as features are initialised only
        // after the bus and all its extensions have been created
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        
        synchronized (pe) {
            pe.setEnabled(enabled);
            pe.setIgnoreUnknownAssertions(ignoreUnknownAssertions);
            if (null != alternativeSelector) {
                pe.setAlternativeSelector(alternativeSelector);
            }
        }
    }

    @Override
    public void initialize(Client client, Bus bus) {
        Endpoint endpoint = client.getEndpoint();
        Policy p = initializeEndpointPolicy(endpoint, bus);
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        EndpointInfo ei = endpoint.getEndpointInfo();
        EndpointPolicy ep = pe.getClientEndpointPolicy(ei, null);
        pe.setClientEndpointPolicy(ei, ep.updatePolicy(p));
    }

    @Override
    public void initialize(Server server, Bus bus) {
        Endpoint endpoint = server.getEndpoint();
        Policy p = initializeEndpointPolicy(endpoint, bus);
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        EndpointInfo ei = endpoint.getEndpointInfo();
        EndpointPolicy ep = pe.getServerEndpointPolicy(ei, null);
        pe.setServerEndpointPolicy(ei, ep.updatePolicy(p));

        // Add policy to the service model (and consequently to the WSDL)
        ServiceModelPolicyUpdater pu = new ServiceModelPolicyUpdater(ei);
        for (PolicyProvider pp : ((PolicyEngineImpl) pe).getPolicyProviders()) {
            if (pp instanceof ExternalAttachmentProvider) {
                pu.addPolicyAttachments(((ExternalAttachmentProvider) pp).getAttachments());
            }
        }
    }

    private Policy initializeEndpointPolicy(Endpoint endpoint, Bus bus) {
        
        initialize(bus);
        DescriptionInfo i = endpoint.getEndpointInfo().getDescription();
        Collection<Policy> loadedPolicies = null;
        if (policyElements != null || policyReferenceElements != null) {
            loadedPolicies = new ArrayList<Policy>();
            PolicyBuilder builder = bus.getExtension(PolicyBuilder.class); 
            if (null != policyElements) {
                for (Element e : policyElements) {
                    loadedPolicies.add(builder.getPolicy(e));
                }
            }
            if (null != policyReferenceElements) {
                for (Element e : policyReferenceElements) {
                    PolicyReference pr = builder.getPolicyReference(e);
                    Policy resolved = resolveReference(pr, builder, bus, i);
                    if (null != resolved) {
                        loadedPolicies.add(resolved);
                    }
                }
            }
        } 
        
        Policy thePolicy = new Policy();
        
        if (policies != null) {
            for (Policy p : policies) {
                thePolicy = thePolicy.merge(p);
            }
        }
        
        if (loadedPolicies != null) {
            for (Policy p : loadedPolicies) {
                thePolicy = thePolicy.merge(p);
            }
        }
        
        return thePolicy;
    }
    
    public Collection<Policy> getPolicies() {
        if (policies == null) {
            policies = new ArrayList<Policy>();
        }
        return policies;
    }

    public void setPolicies(Collection<Policy> policies) {
        this.policies = policies;
    }

    public Collection<Element> getPolicyElements() {
        if (policyElements == null) {
            policyElements = new ArrayList<Element>();
        }
        return policyElements;
    }

    public void setPolicyElements(Collection<Element> elements) {
        policyElements = elements;
    }
    
    public Collection<Element> getPolicyReferenceElements() {
        if (policyReferenceElements == null) {
            policyReferenceElements = new ArrayList<Element>();
        }
        return policyReferenceElements;
    }

    public void setPolicyReferenceElements(Collection<Element> elements) {
        policyReferenceElements = elements;
    }
      
    public void setIgnoreUnknownAssertions(boolean ignore) {
        ignoreUnknownAssertions = ignore;
    } 
    
    
    public void setAlternativeSelector(AlternativeSelector as) {
        alternativeSelector = as;
    }
    
    Policy resolveReference(PolicyReference ref, PolicyBuilder builder, Bus bus, DescriptionInfo i) {
        Policy p = null;
        if (!ref.getURI().startsWith("#")) {
            String base = i == null ? null : i.getBaseURI();
            p = resolveExternal(ref, base, bus);
        } else {
            p = resolveLocal(ref, bus, i);
        }
        if (null == p) {
            throw new PolicyException(new Message("UNRESOLVED_POLICY_REFERENCE_EXC", BUNDLE, ref.getURI()));
        }
        
        return p;
    }   
    
    Policy resolveLocal(PolicyReference ref, final Bus bus, DescriptionInfo i) {
        String uri = ref.getURI().substring(1);
        String absoluteURI = i == null ? uri : i.getBaseURI() + uri;
        PolicyRegistry registry = bus.getExtension(PolicyEngine.class).getRegistry();
        Policy resolved = registry.lookup(absoluteURI);
        if (null != resolved) {
            return resolved;
        }
        ReferenceResolver resolver = new ReferenceResolver() {
            public Policy resolveReference(String uri) {
                PolicyBean pb = bus.getExtension(ConfiguredBeanLocator.class)
                        .getBeanOfType(uri, PolicyBean.class);
                if (null != pb) {
                    PolicyBuilder builder = bus.getExtension(PolicyBuilder.class);
                    return builder.getPolicy(pb.getElement()); 
                }
                return null;
            }
        };
        resolved = resolver.resolveReference(uri);
        if (null != resolved) {
            ref.setURI(absoluteURI);
            registry.register(absoluteURI, resolved);
        }
        return resolved;
    }
    
    protected Policy resolveExternal(PolicyReference ref,  String baseURI, Bus bus) {
        PolicyBuilder builder = bus.getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver(baseURI, builder);
        PolicyRegistry registry = bus.getExtension(PolicyEngine.class).getRegistry();
        Policy resolved = registry.lookup(ref.getURI());
        if (null != resolved) {
            return resolved;
        }
        return resolver.resolveReference(ref.getURI());
    }
}
