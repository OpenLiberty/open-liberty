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
(name="h:head", clazz = "javax.faces.component.html.HtmlHead",
 defaultRendererType="javax.faces.Head",template=true)
abstract class _HtmlHead extends UIOutput 
{

  static public final String COMPONENT_FAMILY = "javax.faces.Output";
  static public final String COMPONENT_TYPE = "javax.faces.OutputHead";

  /**
   * HTML: The direction of text display, either 'ltr' (left-to-right) or 'rtl' (right-to-left).
   * 
   */
  @JSFProperty
  public abstract String getDir();

  /**
   * HTML: The base language of this document.
   * 
   */
  @JSFProperty
  public abstract String getLang();
  
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