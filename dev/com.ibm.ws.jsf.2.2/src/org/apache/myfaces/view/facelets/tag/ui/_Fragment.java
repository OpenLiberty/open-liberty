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

import javax.faces.component.UIComponentBase;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Just like composition has decorate as a non-trimming version of the same behavior,
 * component has fragment. This allows you to logically insert a UIComponent into the 
 * tree and possibly bind this fragment to a JavaBean property or Map for management. 
 * The fragment tag was also added to Facelets to get around some of the optimizations 
 * used in handling text within your document. If you are using components such as 
 * &lt;f:panelGrid/&gt;, which renders its children, you may want more control over the 
 * building process that Facelets does and using a fragment tag will help guarantee 
 * parent/child relationships for you.
 * 
 * <p>
 * The component class used for this tag is 
 * org.apache.myfaces.view.facelets.tag.ui.ComponentRef and the 
 * real java class that contains this description is not used on runtime.
 * </p>
 */
@JSFComponent(
        configExcluded=true,
        defaultRendererType="javax.faces.resource.Script")
abstract class _Fragment extends UIComponentBase
{
    public final static String COMPONENT_TYPE = "facelets.ui.ComponentRef";
    public final static String COMPONENT_FAMILY = "facelets";


    @JSFProperty(tagExcluded=true)
    @Override
    public boolean isRendered()
    {
        return super.isRendered();
    }
}
