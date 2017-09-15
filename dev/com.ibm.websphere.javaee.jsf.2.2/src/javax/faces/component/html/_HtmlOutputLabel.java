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

import javax.faces.component.UIOutput;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 * Renders a HTML label element.
 * 
 * In addition to the JSF specification, MyFaces allows it to directly give an output text via the "value" attribute.
 */
@JSFComponent
(name = "h:outputLabel",
clazz = "javax.faces.component.html.HtmlOutputLabel",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlOutputLabelTag",
defaultRendererType = "javax.faces.Label",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder"
)
abstract class _HtmlOutputLabel extends UIOutput implements _FocusBlurProperties,
_EventProperties, _StyleProperties, _UniversalProperties, _AccesskeyProperty,
_TabindexProperty, _EscapeProperty, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Output";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlOutputLabel";

  /**
   * The client ID of the target input element of this label.
   * 
   * @JSFProperty
   */
  public abstract String getFor();

}
