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
package org.apache.myfaces.view.facelets.tag.ui;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.TagAttribute;

/**
 * Facelet alternative to c:forEach or h:dataTable
 *
 */
public class RepeatHandler extends ComponentHandler
{
    
    public RepeatHandler(ComponentConfig config)
    {
        super(config);
    }

    protected MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset meta = super.createMetaRuleset(type);

        if (!UILibrary.NAMESPACE.equals(this.tag.getNamespace()) &&
            !UILibrary.ALIAS_NAMESPACE.equals(this.tag.getNamespace()))
        {
            meta.add(new TagMetaData(type));
        }

        meta.alias("class", "styleClass");

        return meta;
    }

    private class TagMetaData extends Metadata
    {
        private final String[] _attrs;

        public TagMetaData(Class<?> type)
        {
            Set<String> names = new HashSet<String>();
            for (TagAttribute attribute : tag.getAttributes().getAll())
            {
                if ("class".equals(attribute.getLocalName()))
                {
                    names.add("styleClass");
                }
                else
                {
                    names.add(attribute.getLocalName());
                }
            }
            
            try
            {
                for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors())
                {
                    if (descriptor.getWriteMethod() != null)
                    {
                        names.remove(descriptor.getName());
                    }
                }
            }
            catch (Exception e)
            {
                // do nothing
            }
            
            _attrs = names.toArray(new String[names.size()]);
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            UIComponent component = (UIComponent) instance;
            Map<String, Object> attrs = component.getAttributes();
            attrs.put("alias.element", tag.getQName());
            if (_attrs.length > 0)
            {
                attrs.put("alias.attributes", _attrs);
            }
        }
    }
}
