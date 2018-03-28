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

import javax.faces.component.UISelectBoolean;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 * Allow the user to choose a "true" or "false" value, presented as a
 * checkbox.
 * <p>
 * Renders as an HTML input tag with its type set to "checkbox", and
 * its name attribute set to the id. The "checked" attribute is rendered
 * if the value of this component is true.
 * </p>
 *
 */
@JSFComponent
(name = "h:selectBooleanCheckbox",
clazz = "javax.faces.component.html.HtmlSelectBooleanCheckbox",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlSelectBooleanCheckboxTag",
defaultRendererType = "javax.faces.Checkbox",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
defaultEventName = "valueChange"
)
abstract class _HtmlSelectBooleanCheckbox extends UISelectBoolean implements
    _AccesskeyProperty, _UniversalProperties, _DisabledReadonlyProperties,
    _FocusBlurProperties, _ChangeProperty, _SelectProperty, 
    _EventProperties, _StyleProperties, _TabindexProperty, _LabelProperty,
    _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.SelectBoolean";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlSelectBooleanCheckbox";

}
