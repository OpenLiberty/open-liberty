/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
// PI46218 hwibell     Unambiguous bean name exception if the same bean name is used in multiple WARs
package org.apache.myfaces.flow.cdi;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowDefinition;

import com.ibm.ws.cdi.CDIServiceUtils;

/**
 * This extension is responsible of scan flow definitions through CDI. For example:
 * 
 * <code>
 * @Produces @FlowDefinition
 * public Flow defineFlow(@FlowBuilderParameter FlowBuilder flowBuilder) {...}
 * </code>
 * 
 * @author Leonardo Uribe
 */
public class FlowBuilderCDIExtension implements Extension
{
	// PI46218 start
    private List<Producer<Flow>> flowProducers = new ArrayList<Producer<Flow>>();
    
    public List<Producer<Flow>> getFlowProducers()
    {
        return flowProducers;
    }
    // PI46218 end
    
    void beforeBeanDiscovery(
        @Observes final BeforeBeanDiscovery event, BeanManager beanManager)
    {
        // Register FlowBuilderFactoryBean as a bean with CDI annotations, so the system
        // can take it into account, and use it later when necessary.
        AnnotatedType<FlowBuilderFactoryBean> flowDiscoveryHelper = 
                        beanManager.createAnnotatedType(FlowBuilderFactoryBean.class);
        event.addAnnotatedType(flowDiscoveryHelper, CDIServiceUtils.getAnnotatedTypeIdentifier(flowDiscoveryHelper, this.getClass()));
    }
    
    // PI46218 start
    /**
     * Stores any producer method that is annotated with @FlowDefinition.
     */
    <T> void findFlowDefinition(@Observes ProcessProducer<T, Flow> processProducer)
    {
        if (processProducer.getAnnotatedMember().isAnnotationPresent(FlowDefinition.class))
        {
            flowProducers.add(processProducer.getProducer());
        }
    }
    // PI46218 end
}
