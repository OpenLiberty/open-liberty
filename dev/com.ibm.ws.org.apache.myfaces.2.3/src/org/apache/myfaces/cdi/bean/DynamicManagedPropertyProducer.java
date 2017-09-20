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

package org.apache.myfaces.cdi.bean;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.faces.annotation.ManagedProperty;
import javax.faces.context.FacesContext;
import org.apache.myfaces.shared.util.ClassUtils;

/**
 *
 */
public class DynamicManagedPropertyProducer implements Bean<Object>, Serializable, PassivationCapable
{
    private static final long serialVersionUID = 1L;
    

    private BeanManager beanManager;
    private ManagedPropertyInfo typeInfo;
    private Set<Type> types;
    private Class<?> beanClass;

    public DynamicManagedPropertyProducer(BeanManager beanManager, ManagedPropertyInfo typeInfo)
    {
        this.beanManager = beanManager;
        this.typeInfo = typeInfo;
        types = new HashSet<Type>(asList(typeInfo.getType(), Object.class));
        Type beanType = typeInfo.getType();
        // Need to check for ParameterizedType to support types such as List<String>
        if ( beanType instanceof ParameterizedType ) {
            beanClass = ClassUtils.simpleClassForName(((ParameterizedType) beanType).getRawType().getTypeName());
        } else {
            // need to use simpleJavaTypeToClass to support Arrays and primitive types
            beanClass = ClassUtils.simpleJavaTypeToClass(beanType.getTypeName());
        }

    }
    
    @Override
    public String getId()
    {
        return typeInfo.getType()+"_"+typeInfo.getExpression();
    }

    public static class DefaultAnnotationLiteral extends AnnotationLiteral<ManagedProperty> implements ManagedProperty
    {
        private static final long serialVersionUID = 1L;
        
        private String value;

        public DefaultAnnotationLiteral(String value)
        {
            this.value = value;
        }

        @Override
        public String value()
        {
            return value;
        }
    }
    
    @Override
    public Class<?> getBeanClass()
    {
        return beanClass;
    }

    @Override
    public Set<Type> getTypes()
    {
        return types;
    }
    
    @Override
    public Set<Annotation> getQualifiers()
    {
        return Collections.singleton(
                (Annotation) new DynamicManagedPropertyProducer.DefaultAnnotationLiteral(typeInfo.getExpression()));
    }
    

    @Override
    public Class<? extends Annotation> getScope()
    {
        return Dependent.class;
    }

    @Override
    public String getName()
    {
        return null;
    }
    

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative()
    {
        return false;
    }
    
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable()
    {
        return true;
    }
    
    @Override
    public Object create(CreationalContext<Object> cc)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext.getApplication().evaluateExpressionGet(
                facesContext, typeInfo.getExpression(), beanClass);
    }

    @Override
    public void destroy(Object t, CreationalContext<Object> cc)
    {
    }
    
}
