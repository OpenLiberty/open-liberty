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
package org.apache.myfaces.view.facelets.tag.composite;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

/**
 * Copy of org.apache.myfaces.view.facelets.tag.jsf.ComponentRule
 * used for composite components.
 * 
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1194849 $ $Date: 2011-10-29 09:28:45 +0000 (Sat, 29 Oct 2011) $
 */
final class CompositeComponentRule extends MetaRule
{

    final class LiteralAttributeMetadata extends Metadata
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
            ((UIComponent) instance).getAttributes().put(_name, _value);
        }
    }
    
    final class TypedLiteralAttributeMetadata extends Metadata
    {
        private final String _name;
        private final TagAttribute _attr;
        private final Class<?> _type;

        public TypedLiteralAttributeMetadata(String name, Class<?> type, TagAttribute attr)
        {
            _name = name;
            _attr = attr;
            _type = type;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((UIComponent) instance).getAttributes().put(_name, _attr.getObject(ctx, _type));
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
            // Call setValueExpression to set ValueExpression instances
            // cause problems later because when getAttributes().get() is called
            // it is evaluated. put the ValueExpression directly on the map
            // prevents it and does not cause any side effects.
            // Maybe we should call setValueExpression when the getter and 
            // setter exists on the component class.
            //((UIComponent) instance).getAttributes().put(_name, _attr.getValueExpression(ctx, _type));
            ((UIComponent) instance).setValueExpression(_name, _attr.getValueExpression(ctx, _type));
            //((UIComponent) instance).setValueExpression(_name, 
            //        ctx.getFacesContext().getApplication().
            //        getExpressionFactory().createValueExpression(
            //                _attr.getValueExpression(ctx, _type), ValueExpression.class));
        }
    }

    //private final static Logger log = Logger.getLogger("facelets.tag.component");
    private final static Logger log = Logger.getLogger(CompositeComponentRule.class.getName());

    public final static CompositeComponentRule INSTANCE = new CompositeComponentRule();

    public CompositeComponentRule()
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
            else if (meta.getWriteMethod(name) == null)
            {
                Class<?> type = meta.getPropertyType(name);
                if (type == null)
                {
                    if (meta.getProperty(name) == null)
                    {
                        // this was an attribute literal, but not property
                        warnAttr(attribute, meta.getTargetClass(), name);
                    }

                    return new LiteralAttributeMetadata(name, attribute.getValue());
                }
                else
                {
                    return new TypedLiteralAttributeMetadata(name, type, attribute);
                }
            }
        }
        return null;
    }

    private static void warnAttr(TagAttribute attr, Class<?> type, String n)
    {
        if (log.isLoggable(Level.FINER))
        {
            log.finer(attr + " Property '" + n + "' is not on type: " + type.getName());
        }
    }

}
