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

import javax.faces.component.UIPanel;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * This element is used to group other components where the specification requires one child element.
 * 
 * If any of the HTML or CSS attributes are set, its content is rendered within a span element.
 */
@JSFComponent
(name = "h:panelGroup",
clazz = "javax.faces.component.html.HtmlPanelGroup",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlPanelGroupTag",
defaultRendererType = "javax.faces.Group",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder"
)
abstract class _HtmlPanelGroup extends UIPanel implements _StyleProperties, _EventProperties
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Panel";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlPanelGroup";

  /**
   * The type of layout markup to use when rendering this group. If the value is "block"
   * the renderer must produce an HTML "div" element. Otherwise HTML "span" element must be produced.
   *
   * @return  the new layout value
   */
  @JSFProperty
  public abstract String getLayout();

}
