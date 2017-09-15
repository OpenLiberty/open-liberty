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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

@JSFFaceletTag(
        name = "f:selectItems",
        bodyContent = "empty",
        componentClass="javax.faces.component.UISelectItems")
public class SelectItemsHandler extends ComponentHandler
{

    public SelectItemsHandler(ComponentConfig config)
    {
        super(config);
    }
    
    
    /**
     * Is either an EL expression pointing to the element in the value collection
     * whose value should be marked as a "no selection" item, or a literal string
     * that exactly matches the value of the item in the collection that must be 
     * marked as the "no selection" item. If the user selects such an item and 
     * the field is marked as required, then it will not pass validation.
     * 
     * @since 2.0
     * @return
     */
    @JSFFaceletAttribute(name = "noSelectionValue",
            className = "javax.el.ValueExpression",
            deferredValueType = "java.lang.Boolean")
    private boolean getNoSelectionValue()
    {
        return false;
    }
}
