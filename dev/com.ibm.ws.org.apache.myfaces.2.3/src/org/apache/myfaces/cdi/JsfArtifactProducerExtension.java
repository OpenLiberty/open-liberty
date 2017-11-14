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
package org.apache.myfaces.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.faces.annotation.FacesConfig;

public class JsfArtifactProducerExtension implements Extension
{
    private boolean registerCdiProducers = false;
    
    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event, BeanManager beanManager)
    {
        FacesConfig facesConfig = event.getAnnotatedType().getAnnotation(FacesConfig.class);
        if (facesConfig != null && facesConfig.version() != FacesConfig.Version.JSF_2_2)
        {
            registerCdiProducers = true;
        }
    }
    
    void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager)
    {        
        if (registerCdiProducers)
        {
            AnnotatedType<JsfArtifactProducer> jsfArtifactProducer =
                            beanManager.createAnnotatedType(JsfArtifactProducer.class);
            event.addAnnotatedType(jsfArtifactProducer, jsfArtifactProducer.getJavaClass().getName());
        }
    }
    
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager)
    {
        if (registerCdiProducers)
        {
            afterBeanDiscovery.addBean(new JsfArtifactFlowMapProducer(beanManager));
        }
    }

    public boolean isRegisterCdiProducers()
    {
        return registerCdiProducers;
    }
}
