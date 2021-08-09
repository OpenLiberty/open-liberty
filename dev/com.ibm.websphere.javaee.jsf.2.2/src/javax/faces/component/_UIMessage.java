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
package javax.faces.component;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 *
 * UIMessage is the base component class for components
 * that display a single message on behalf of a component.
 */
@JSFComponent
(clazz = "javax.faces.component.UIMessage",template=true,
defaultRendererType = "javax.faces.Message"
)
abstract class _UIMessage extends UIComponentBase
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Message";
  static public final String COMPONENT_TYPE =
    "javax.faces.Message";

  /**
   * The ID of the component whose attached FacesMessage object (if present) 
   * should be diplayed by this component.
   * <p>
   * This is a required property on the component.
   * </p>
   *
   * @return  the new for value
   */
  @JSFProperty
  (required = true)
  public abstract String getFor();

  /**
   * Specifies whether the detailed information from the message should be shown. 
   * Default to true.
   *
   * @return  the new showDetail value
   */
  @JSFProperty
  (defaultValue = "true")
  public abstract boolean isShowDetail();

  /**
   * Specifies whether the summary information from the message should be shown.
   * Defaults to false.
   *
   * @return  the new showSummary value
   */
  @JSFProperty
  (defaultValue = "false")
  public abstract boolean isShowSummary();

  /**
   * Indicate this component should render already handled messages.
   * Default value is true
   * 
   * @since 2.0
   * @return
   */
  @JSFProperty
  (defaultValue = "true", tagExcluded=true)  
  public abstract boolean isRedisplay();
}
