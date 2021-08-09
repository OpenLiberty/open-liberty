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

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: BeanPropertyTagRule.java 1189343 2011-10-26 17:53:36Z struberg $
 */
public final class BeanPropertyTagRule extends MetaRule
{
    public final static BeanPropertyTagRule INSTANCE = new BeanPropertyTagRule();

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        Method m = meta.getWriteMethod(name);

        // if the property is writable
        if (m != null)
        {
            if (attribute.isLiteral())
            {
                return new LiteralPropertyMetadata(m, attribute);
            }
            else
            {
                return new DynamicPropertyMetadata(m, attribute);
            }
        }

        return null;
    }
    
    final static class LiteralPropertyMetadata extends Metadata
    {

        private final Method method;

        private final TagAttribute attribute;

        private Object[] value;

        public LiteralPropertyMetadata(Method method, TagAttribute attribute)
        {
            this.method = method;
            this.attribute = attribute;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            if (value == null)
            {
                String str = this.attribute.getValue();
                value = new Object[] { ctx.getExpressionFactory().coerceToType(str, method.getParameterTypes()[0]) };
            }
            try
            {
                method.invoke(instance, this.value);
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

        private final Method method;

        private final TagAttribute attribute;

        private final Class<?> type;

        public DynamicPropertyMetadata(Method method, TagAttribute attribute)
        {
            this.method = method;
            this.type = method.getParameterTypes()[0];
            this.attribute = attribute;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            try
            {
                method.invoke(instance, new Object[] { attribute.getObject(ctx, type) });
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
