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
package org.apache.myfaces.view.facelets.tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.MetaRule;
import jakarta.faces.view.facelets.Metadata;
import jakarta.faces.view.facelets.MetadataTarget;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;

/**
 * 
 * @author Jacob Hookom
 * @version $Id$
 */
public final class BeanPropertyTagRule extends MetaRule
{
    public final static BeanPropertyTagRule INSTANCE = new BeanPropertyTagRule();

    @Override
    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        if (meta instanceof LambdaMetadataTargetImpl)
        {
            BiConsumer<Object, Object> f = ((LambdaMetadataTargetImpl) meta).getWriteFunction(name);

            // if the property is writable
            if (f != null)
            {
                if (attribute.isLiteral())
                {
                    return new LiteralPropertyMetadata(meta.getPropertyType(name), f, attribute);
                }
                else
                {
                    return new DynamicPropertyMetadata(meta.getPropertyType(name), f, attribute);
                }
            }
        }
        else
        {
            Method m = meta.getWriteMethod(name);

            // if the property is writable
            if (m != null)
            {
                if (attribute.isLiteral())
                {
                    return new LiteralPropertyMetadata(meta.getPropertyType(name), m, attribute);
                }
                else
                {
                    return new DynamicPropertyMetadata(meta.getPropertyType(name), m, attribute);
                }
            }
        }

        return null;
    }
    
    final static class LiteralPropertyMetadata extends Metadata
    {
        private final Class<?> propertyType;
        private final Method method;
        private final BiConsumer<Object, Object> function;
        private final TagAttribute attribute;
        private Object value;
        private Object[] valueArgs;

        public LiteralPropertyMetadata(Class<?> propertyType, Method method, TagAttribute attribute)
        {
            this.propertyType = propertyType;
            this.method = method;
            this.function = null;
            this.attribute = attribute;
        }
        
        public LiteralPropertyMetadata(Class<?> propertyType, BiConsumer<Object, Object> function,
                TagAttribute attribute)
        {
            this.propertyType = propertyType;
            this.method = null;
            this.function = function;
            this.attribute = attribute;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            try
            {
                if (function != null)
                {
                    if (value == null)
                    {
                        String str = this.attribute.getValue();
                        value = ctx.getExpressionFactory().coerceToType(str, propertyType);
                    }
                    function.accept(instance, value);
                }
                else if (method != null)
                {
                    if (valueArgs == null)
                    {
                        String str = this.attribute.getValue();
                        valueArgs = new Object[] { ctx.getExpressionFactory().coerceToType(str, propertyType) };
                    }
                    method.invoke(instance, valueArgs);
                }
            }
            catch (InvocationTargetException e)
            {
                throw new TagAttributeException(this.attribute, e.getCause());
            }
            catch (Exception e)
            {
                throw new TagAttributeException(this.attribute, e);
            }
        }

    }

    final static class DynamicPropertyMetadata extends Metadata
    {
        private final Class<?> propertyType;
        private final Method method;
        private final BiConsumer<Object, Object> function;
        private final TagAttribute attribute;

        public DynamicPropertyMetadata(Class<?> propertyType, Method method, TagAttribute attribute)
        {
            this.propertyType = propertyType;
            this.method = method;
            this.function = null;
            this.attribute = attribute;
        }

        public DynamicPropertyMetadata(Class<?> propertyType, BiConsumer<Object, Object> function,
                TagAttribute attribute)
        {
            this.propertyType = propertyType;
            this.method = null;
            this.function = function;
            this.attribute = attribute;
        }
        
        @Override
        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            try
            {
                if (method != null)
                {
                    method.invoke(instance, new Object[] { attribute.getObject(ctx, propertyType) });
                }
                else if (function != null)
                {
                    function.accept(instance, attribute.getObject(ctx, propertyType));
                }
            }
            catch (InvocationTargetException e)
            {
                throw new TagAttributeException(attribute, e.getCause());
            }
            catch (Exception e)
            {
                throw new TagAttributeException(attribute, e);
            }
        }
    }

}
