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
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;
import org.apache.myfaces.view.facelets.PassthroughRule;

/**
 *
 * @author Leonardo Uribe
 */
final class PassthroughRuleImpl extends MetaRule implements PassthroughRule
{

    final static class LiteralAttributeMetadata extends Metadata
    {
        private final String _name;
        private final String _value;

        public LiteralAttributeMetadata(String name, String value)
        {
            _name = name;
            _value = value;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((UIComponent) instance).getPassThroughAttributes().put(_name, _value);
        }
    }

    final static class ValueExpressionMetadata extends Metadata
    {
        private final String _name;

        private final TagAttribute _attr;

        private final Class<?> _type;

        public ValueExpressionMetadata(String name, Class<?> type, TagAttribute attr)
        {
            _name = name;
            _attr = attr;
            _type = type;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((UIComponent) instance).getPassThroughAttributes().put(
                _name, _attr.getValueExpression(ctx, _type));
        }
    }

    public final static PassthroughRuleImpl INSTANCE = new PassthroughRuleImpl();

    public PassthroughRuleImpl()
    {
        super();
    }

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        if (meta.isTargetInstanceOf(UIComponent.class))
        {
            // if component and dynamic, then must set expression
            if (!attribute.isLiteral())
            {
                Class<?> type = meta.getPropertyType(name);
                if (type == null)
                {
                    type = Object.class;
                }
                
                return new ValueExpressionMetadata(name, type, attribute);
            }
            else
            {
                return new LiteralAttributeMetadata(name, attribute.getValue());
            }
        }
        return null;
    }
}
