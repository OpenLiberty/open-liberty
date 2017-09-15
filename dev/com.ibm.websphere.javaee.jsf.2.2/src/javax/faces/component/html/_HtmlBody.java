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
import javax.faces.convert.Converter;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFExclude;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * 
 */
@JSFComponent
(name="h:body", clazz = "javax.faces.component.html.HtmlBody",
 defaultRendererType="javax.faces.Body",template=true,
 implementz = "javax.faces.component.behavior.ClientBehaviorHolder")
abstract class _HtmlBody extends UIOutput 
    implements _EventProperties, _UniversalProperties, _StyleProperties, _RoleProperty
{

  static public final String COMPONENT_FAMILY = "javax.faces.Output";
  static public final String COMPONENT_TYPE = "javax.faces.OutputBody";

  /**
   * HTML: Script to be invoked when the page is loaded
   * 
   */
  @JSFProperty(clientEvent="load")
  public abstract String getOnload();
  
  /**
   * HTML: Script to be invoked when the page is unloaded
   * 
   */
  @JSFProperty(clientEvent="unload")
  public abstract String getOnunload();
  
  /**
   * 
   * @since 2.1.0
   * @return
   */
  @JSFProperty
  public abstract String getXmlns();

  @JSFExclude
  @JSFProperty(tagExcluded=true)
  @Override
  public Converter getConverter()
  {
    return super.getConverter();
  }

  @JSFExclude  
  @JSFProperty(tagExcluded=true)
  @Override
  public Object getValue()
  {
    return super.getValue();
  }

  @JSFExclude
  @JSFProperty(tagExcluded=true)
  @Override
  public String getId()
  {
    return super.getId();
  }

  @JSFExclude
  @JSFProperty(tagExcluded=true)
  @Override
  public boolean isRendered()
  {
    return super.isRendered();
  }
}
