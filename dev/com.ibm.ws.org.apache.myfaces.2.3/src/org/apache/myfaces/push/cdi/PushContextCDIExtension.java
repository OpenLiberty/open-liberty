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

package org.apache.myfaces.push.cdi;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.faces.push.Push;
import javax.faces.push.PushContext;

/**
 *
 */
public class PushContextCDIExtension implements Extension
{
    private List<Producer<PushContext>> pushContextProducers = new ArrayList<Producer<PushContext>>();

    public List<Producer<PushContext>> getPushContextProducers()
    {
        return pushContextProducers;
    }

    void beforeBeanDiscovery(
        @Observes final BeforeBeanDiscovery event, BeanManager beanManager)
    {
        // Register FlowBuilderFactoryBean as a bean with CDI annotations, so the system
        // can take it into account, and use it later when necessary.
        AnnotatedType<PushContextFactoryBean> pushContextFactoryBean =
                        beanManager.createAnnotatedType(PushContextFactoryBean.class);
        event.addAnnotatedType(pushContextFactoryBean);
        
        AnnotatedType wcbean = beanManager.createAnnotatedType(WebsocketChannelTokenBuilderBean.class);
        event.addAnnotatedType(wcbean);        
        
        AnnotatedType sessionhandlerbean = beanManager.createAnnotatedType(WebsocketSessionBean.class);
        event.addAnnotatedType(sessionhandlerbean);

        AnnotatedType viewTokenBean = beanManager.createAnnotatedType(WebsocketViewBean.class);
        event.addAnnotatedType(viewTokenBean);

        AnnotatedType apphandlerbean = beanManager.createAnnotatedType(WebsocketApplicationBean.class);
        event.addAnnotatedType(apphandlerbean);
    }

    /**
     * Stores any producer method that is annotated with @Push
     */
    <T> void findFlowDefinition(@Observes ProcessProducer<T, PushContext> processProducer)
    {
        if (processProducer.getAnnotatedMember().isAnnotationPresent(Push.class))
        {
            pushContextProducers.add(processProducer.getProducer());
        }
    }

}
