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

import javax.faces.component.UIGraphic;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFExclude;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Renders an HTML img element.
 * <p>
 * The value attribute specifies the url of the image to be displayed;
 * see the documentation for attribute "url" for more details.
 * </p>
 */
@JSFComponent
(name = "h:graphicImage",
clazz = "javax.faces.component.html.HtmlGraphicImage",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlGraphicImageTag",
defaultRendererType = "javax.faces.Image",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder"
)
abstract class _HtmlGraphicImage extends UIGraphic 
    implements _EventProperties,
    _StyleProperties, _UniversalProperties, _AltProperty, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Graphic";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlGraphicImage";

  /**
   * HTML: Overrides the natural height of this image, by specifying height in pixels.
   * 
   */
  @JSFProperty
  public abstract String getHeight();

  /**
   * HTML: Specifies server-side image map handling for this image.
   * 
   */
  @JSFProperty(defaultValue="false")
  public abstract boolean isIsmap();
  
  /**
   * HTML: A link to a long description of the image.
   * 
   */
  @JSFProperty
  public abstract String getLongdesc();
  
  /**
   * HTML: Specifies an image map to use with this image.
   * 
   */
  @JSFProperty
  public abstract String getUsemap();
  
  /**
   * HTML: Overrides the natural width of this image, by specifying width in pixels.
   * 
   */
  @JSFProperty
  public abstract String getWidth();

  /**
   * 
   * @return
   */
  @JSFProperty
  @JSFExclude
  public String getLibrary()
  {
      return null;
  }
  
  /**
   * 
   * @return
   */
  @JSFProperty
  @JSFExclude
  public String getName()
  {
      return null;
  }
}
