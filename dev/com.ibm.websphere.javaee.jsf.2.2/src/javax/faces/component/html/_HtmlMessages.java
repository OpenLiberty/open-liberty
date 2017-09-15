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

import javax.faces.component.UIMessages;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 * Renders all or some FacesMessages depending on the "for" and
 * "globalOnly" attributes.
 *
 * <ul>
 * <li>If globalOnly = true, only global messages, that have no
 * associated clientId, will be displayed.</li>
 * <li>else if there is a "for" attribute, only messages that are
 * assigned to the component referenced by the "for" attribute
 * are displayed.</li>
 * <li>else all messages are displayed.</li>
 * </ul>
 */
@JSFComponent
(name = "h:messages",
clazz = "javax.faces.component.html.HtmlMessages",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlMessagesTag",
defaultRendererType = "javax.faces.Messages"
)
abstract class _HtmlMessages extends UIMessages implements _StyleProperties, 
_MessageProperties, _UniversalProperties, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Messages";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlMessages";

  /**
   * The layout: "table" or "list". Default: list
   * 
   * @JSFProperty
   *   defaultValue = "list"
   */
  public abstract String getLayout();
  
}
