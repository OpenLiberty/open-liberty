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
package javax.faces.component.html;

import javax.faces.component.UICommand;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * This tag renders as an HTML input element.
 *
 */
@JSFComponent
(name = "h:commandButton",
clazz = "javax.faces.component.html.HtmlCommandButton",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlCommandButtonTag",
defaultRendererType = "javax.faces.Button",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
defaultEventName = "action"
)
abstract class _HtmlCommandButton extends UICommand
    implements _FocusBlurProperties, 
    _EventProperties, _StyleProperties, _UniversalProperties,
    _AccesskeyProperty, _TabindexProperty, _AltProperty, 
    _ChangeSelectProperties, _DisabledReadonlyProperties,
    _LabelProperty, _RoleProperty
{

    static public final String COMPONENT_FAMILY = "javax.faces.Command";
    static public final String COMPONENT_TYPE = "javax.faces.HtmlCommandButton";

    /**
     * HTML: The URL of an image that renders in place of the button.
     * 
     */
    @JSFProperty
    public abstract String getImage();

    /**
     * HTML: A hint to the user agent about the content type of the linked resource.
     * 
     * @JSFProperty
     *   defaultValue = "submit"
     */
    @JSFProperty(defaultValue = "submit")
    public abstract String getType();

}
