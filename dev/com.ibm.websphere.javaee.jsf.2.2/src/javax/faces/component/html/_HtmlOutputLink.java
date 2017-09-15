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
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Renders a HTML a element.
 * 
 * Child f:param elements are added to the href attribute as query parameters.  Other children
 * are rendered as the link text or image.
 */
@JSFComponent
(name = "h:outputLink",
clazz = "javax.faces.component.html.HtmlOutputLink",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlOutputLinkTag",
defaultRendererType = "javax.faces.Link",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder"
)
abstract class _HtmlOutputLink extends UIOutput implements _AccesskeyProperty,
_UniversalProperties, _FocusBlurProperties, _EventProperties, _StyleProperties,
_TabindexProperty, _LinkProperties, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Output";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlOutputLink";

  /**
   * append text to url after '#'
   * 
   * @return
   */
  @JSFProperty(tagExcluded=true)
  public abstract String getFragment();
}
