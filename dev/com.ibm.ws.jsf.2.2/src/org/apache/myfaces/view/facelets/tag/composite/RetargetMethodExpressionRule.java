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

import java.beans.PropertyDescriptor;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

/**
 * Rule used to wire ValueExpressions retrieved by ViewDeclarationLanguage.retargetMethodExpressions 
 * 
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1189343 $ $Date: 2011-10-26 17:53:36 +0000 (Wed, 26 Oct 2011) $
 */
final class RetargetMethodExpressionRule extends MetaRule
{
    final static class RetargetValueExpressionMapper extends Metadata
    {
        private final TagAttribute _attr;
        
        private final String _name;

        public RetargetValueExpressionMapper(TagAttribute attr, String name)
        {
            this._attr = attr;
            this._name = name;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ValueExpression expr = _attr.getValueExpression(ctx, Object.class);
            ((UIComponent) instance).getAttributes().put(_name, expr);
        }
    }

    public final static RetargetMethodExpressionRule INSTANCE = new RetargetMethodExpressionRule();

    public RetargetMethodExpressionRule()
    {
        super();
    }

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        // ViewDeclarationLanguage.retargetMethodExpressions only works when a method-signature 
        // is defined, so we just have to apply this rule on that case. 
        if ("action".equals(name) || 
            "actionListener".equals(name) ||
            "validator".equals(name) ||
            "valueChangeListener".equals(name))
        {
            return new RetargetValueExpressionMapper(attribute, name);
        }
        else
        {
            PropertyDescriptor propertyDescriptor = meta.getProperty(name);
            //Type takes precedence over method-signature
            if (propertyDescriptor != null && 
                    propertyDescriptor.getValue("type") == null)
            {
                ValueExpression methodSignatureExpression = 
                    (ValueExpression) propertyDescriptor.getValue("method-signature");
                
                if (methodSignatureExpression != null)
                {
                    return new RetargetValueExpressionMapper(attribute, name);
                }
                
                ValueExpression targetAttributeNameVE = 
                    (ValueExpression)propertyDescriptor.getValue("targetAttributeName");
                if (targetAttributeNameVE != null)
                {
                    return new RetargetValueExpressionMapper(attribute, name);
                }
                
                //if there is a targets declaration, is a retarget without doubt
                ValueExpression targets = 
                    (ValueExpression)propertyDescriptor.getValue("targets");
                
                if (targets != null)
                {
                    return new RetargetValueExpressionMapper(attribute, name);
                }
            }
        }
        return null;
    }
}
