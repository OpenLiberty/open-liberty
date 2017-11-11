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

package org.apache.myfaces.cdi.model;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.faces.model.DataModel;
import javax.faces.model.FacesDataModel;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.shared.util.ClassUtils;

/**
 *
 */
public class FacesDataModelExtension implements Extension
{
    private Set<DataModelInfo> types = new HashSet<DataModelInfo>();

    void beforeBeanDiscovery(
        @Observes final BeforeBeanDiscovery event, BeanManager beanManager)
    {
        AnnotatedType beanHolder = beanManager.createAnnotatedType(FacesDataModelClassBeanHolder.class);
        event.addAnnotatedType(beanHolder);
    }

    public <T> void collect(@Observes ProcessManagedBean<T> event)
    {
        if (event.getAnnotatedBeanClass().isAnnotationPresent(FacesDataModel.class))
        {
            Annotated annotated = event.getAnnotatedBeanClass();
            
            Type type = annotated.getBaseType();

            FacesDataModel conv = (FacesDataModel) annotated.getAnnotation(FacesDataModel.class);
            
            boolean hasValue = conv.forClass() != null;
            if (hasValue)
            {
                types.add(new DataModelInfo(type, conv.forClass()));
            }
        }
    }
    
    public void afterBean(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager)
    {
        for (DataModelInfo typeInfo : types)
        {
            afterBeanDiscovery.addBean(new DynamicDataModelProducer(beanManager, typeInfo));
        }
    }
    
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager)
    {
        FacesDataModelClassBeanHolder holder = CDIUtils.lookup(beanManager, FacesDataModelClassBeanHolder.class);
        for (DataModelInfo typeInfo : types)
        {
            holder.addFacesDataModel(typeInfo.getForClass(), 
                    ClassUtils.simpleClassForName(typeInfo.getType().getTypeName()));
        }
        // Initialize unmodifiable wrapper
        Map<Class<?>,Class<? extends DataModel>> map = holder.getClassInstanceToDataModelWrapperClassMap();
    }

}
