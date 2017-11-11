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

package org.apache.myfaces.cdi.config;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.faces.annotation.FacesConfig;
import org.apache.myfaces.cdi.util.CDIUtils;

/**
 *
 */
public class FacesConfigExtension implements Extension
{
    private FacesConfig.Version facesConfigVersion;
    
    void beforeBeanDiscovery(
        @Observes final BeforeBeanDiscovery event, BeanManager beanManager)
    {
        AnnotatedType beanHolder = beanManager.createAnnotatedType(FacesConfigBeanHolder.class);
        event.addAnnotatedType(beanHolder, beanHolder.getJavaClass().getName());
    }
    
    public <T> void collect(@Observes ProcessManagedBean<T> event)
    {
        if (event.getAnnotatedBeanClass().isAnnotationPresent(FacesConfig.class))
        {
            Annotated annotated = event.getAnnotatedBeanClass();
            
            FacesConfig config = (FacesConfig) annotated.getAnnotation(FacesConfig.class);

            if (facesConfigVersion != null)
            {
                facesConfigVersion = facesConfigVersion.ordinal() < config.version().ordinal() ? 
                        config.version() : facesConfigVersion;
            }
            else
            {
                facesConfigVersion = config.version();
            }
        }
    }
    
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager)
    {
        FacesConfigBeanHolder holder = CDIUtils.lookup(beanManager, FacesConfigBeanHolder.class);
        
        holder.setFacesConfigVersion(facesConfigVersion);
    }

}
