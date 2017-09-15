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
package org.apache.myfaces.view.facelets.tag.jsf;

import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.convert.Converter;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: ValueHolderRule.java 1189343 2011-10-26 17:53:36Z struberg $
 */
public final class ValueHolderRule extends MetaRule
{

    final static class LiteralConverterMetadata extends Metadata
    {

        private final String converterId;

        public LiteralConverterMetadata(String converterId)
        {
            this.converterId = converterId;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((ValueHolder) instance).setConverter(ctx.getFacesContext().getApplication()
                    .createConverter(this.converterId));
        }
    }

    final static class DynamicConverterMetadata2 extends Metadata
    {

        private final TagAttribute attr;

        public DynamicConverterMetadata2(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((UIComponent) instance).setValueExpression("converter", attr.getValueExpression(ctx, Converter.class));
        }
    }

    final static class LiteralValueMetadata extends Metadata
    {

        private final String value;

        public LiteralValueMetadata(String value)
        {
            this.value = value;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((ValueHolder) instance).setValue(this.value);
        }
    }

    final static class DynamicValueExpressionMetadata extends Metadata
    {

        private final TagAttribute attr;

        public DynamicValueExpressionMetadata(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((UIComponent) instance).setValueExpression("value", attr.getValueExpression(ctx, Object.class));
        }
    }

    public final static ValueHolderRule INSTANCE = new ValueHolderRule();

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        if (meta.isTargetInstanceOf(ValueHolder.class))
        {

            if ("converter".equals(name))
            {
                if (attribute.isLiteral())
                {
                    return new LiteralConverterMetadata(attribute.getValue());
                }
                else
                {
                    return new DynamicConverterMetadata2(attribute);
                }
            }

            if ("value".equals(name))
            {
                if (attribute.isLiteral())
                {
                    return new LiteralValueMetadata(attribute.getValue());
                }
                else
                {
                    return new DynamicValueExpressionMetadata(attribute);
                }
            }
        }
        return null;
    }

}
