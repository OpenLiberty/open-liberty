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

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.MetaRuleset;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 *
 * @author Leonardo Uribe
 */
@JSFFaceletTag(
        name = "jsf:element",
        componentClass = "org.apache.myfaces.view.facelets.component.JsfElement")
public class JsfElementHandler extends javax.faces.view.facelets.ComponentHandler
{

    public JsfElementHandler(ComponentConfig config)
    {
        super(config);
    }
    
    protected MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset rules = super.createMetaRuleset(type);
        
        rules.alias("class", "styleClass");
        rules.addRule(ElementNameRule.INSTANCE);
        
        return rules;
    }
    
}
