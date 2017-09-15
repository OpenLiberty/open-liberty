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

import javax.faces.component.UIInput;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 *
 * Renders as an HTML input tag with its type set to "hidden".
 * Unless otherwise specified, all attributes accept static values
 * or EL expressions.
 *
 */
@JSFComponent
(name = "h:inputHidden",
tagClass = "org.apache.myfaces.taglib.html.HtmlInputHiddenTag",
defaultRendererType = "javax.faces.Hidden"
)
public class HtmlInputHidden extends UIInput
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Input";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlInputHidden";

  /**
   * Construct an instance of the HtmlInputHidden.
   */
  public HtmlInputHidden()
  {
    setRendererType("javax.faces.Hidden");
  }

  @Override
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }
  
  protected enum PropertyKeys
  {
      //No properties, but we need it for maintain binary compatibility
  }
}
