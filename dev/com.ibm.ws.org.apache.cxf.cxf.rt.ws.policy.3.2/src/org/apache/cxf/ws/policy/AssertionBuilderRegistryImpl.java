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

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.xml.namespace.QName;


import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.apache.neethi.AssertionBuilderFactoryImpl;
import org.apache.neethi.PolicyBuilder;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 *
 */
@NoJSR250Annotations(unlessNull = "bus")
public class AssertionBuilderRegistryImpl extends AssertionBuilderFactoryImpl implements
    AssertionBuilderRegistry, BusExtension {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AssertionBuilderRegistryImpl.class);
    private static final Logger LOG
        = LogUtils.getL7dLogger(AssertionBuilderRegistryImpl.class);
    private boolean ignoreUnknownAssertions = true;
    private Set<QName> ignored = new HashSet<>();
    private Bus bus;
    private boolean dynamicLoaded;

    public AssertionBuilderRegistryImpl() {
        super(null);
    }
    public AssertionBuilderRegistryImpl(Bus b) {
        super(null);
        setBus(b);
    }

    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (b != null) {
            b.setExtension(this, AssertionBuilderRegistry.class);
            org.apache.cxf.ws.policy.PolicyBuilder builder
                = b.getExtension(org.apache.cxf.ws.policy.PolicyBuilder.class);
            if (builder instanceof PolicyBuilder) {
                engine = (PolicyBuilder)builder;
            }
        }
    }

    public Class<?> getRegistrationType() {
        return AssertionBuilderRegistry.class;
    }

    public boolean isIgnoreUnknownAssertions() {
        return ignoreUnknownAssertions;
    }

    public void setIgnoreUnknownAssertions(boolean ignore) {
        ignoreUnknownAssertions = ignore;
    }

    protected synchronized void loadDynamic() {
        if (!dynamicLoaded && bus != null) {
            dynamicLoaded = true;
            ConfiguredBeanLocator c = bus.getExtension(ConfiguredBeanLocator.class);
            if (c != null) {
                c.getBeansOfType(AssertionBuilderLoader.class);
                for (AssertionBuilder<?> b : c.getBeansOfType(AssertionBuilder.class)) {
                    registerBuilder(b);
                }
            }
        }
    }
    protected AssertionBuilder<?> handleNoRegisteredBuilder(QName qname) {
        if (ignoreUnknownAssertions) {
            boolean alreadyWarned = ignored.contains(qname);
            if (!alreadyWarned) {
                ignored.add(qname);
                Message m = new Message("NO_ASSERTIONBUILDER_EXC", BUNDLE, qname.toString());
               
                // Liberty Change Start: Don't log warning for custom CXF policy assertions
                if(qname.toString().contains("http://cxf.apache.org/custom/security-policy")) {
                    LOG.finest(m.toString());
                } else {
                    LOG.warning(m.toString());
                }
                // Liberty Change End
            }
            return new XMLPrimitiveAssertionBuilder();
        }
        Message m = new Message("NO_ASSERTIONBUILDER_EXC", BUNDLE, qname.toString());
        throw new PolicyException(m);
    }

}
