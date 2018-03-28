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

import javax.faces.component.UISelectOne;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 * Allow the user to choose one option from a set of options.
 * <p>
 * Rendered as a listbox with the MULTIPLE attribute set to false.
 * </p>
 * <p>
 * The available choices are defined via child f:selectItem or
 * f:selectItems elements. The size of the listbox defaults to the
 * number of available choices; if size is explicitly set to a
 * smaller value, then scrollbars will be rendered. If size is set
 * to 1 then a "drop-down menu" (aka "combo-box") is rendered, though
 * if this is the intent then selectOneMenu should be used instead.
 * </p>
 * <p>
 * The value attribute of this component is read to determine
 * which of the available options is initially selected; its value
 * should match the "value" property of one of the child SelectItem
 * objects.
 * </p>
 * <p>
 * On submit of the enclosing form, the value attribute's bound
 * property is updated to contain the "value" property from the
 * chosen SelectItem.
 * </p>
 *
 */
@JSFComponent
(name = "h:selectOneListbox",
clazz = "javax.faces.component.html.HtmlSelectOneListbox",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlSelectOneListboxTag",
defaultRendererType = "javax.faces.Listbox",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
defaultEventName = "valueChange"
)
abstract class _HtmlSelectOneListbox extends UISelectOne implements
    _AccesskeyProperty, _UniversalProperties, _DisabledReadonlyProperties,
    _FocusBlurProperties, _ChangeProperty, _EventProperties,
    _StyleProperties, _TabindexProperty, _DisabledClassEnabledClassProperties,
    _LabelProperty, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.SelectOne";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlSelectOneListbox";

  /**
   * see JSF Spec.
   * 
   * @JSFProperty
   *   defaultValue="Integer.MIN_VALUE"
   */
  public abstract int getSize();

}
