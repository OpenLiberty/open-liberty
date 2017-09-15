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

import javax.faces.component.UIColumn;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFExclude;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Creates a UIComponent that represents a single column of data within a parent UIData component.
 * <p>
 * This tag is commonly used as a child of the h:dataTable tag, to represent a column of
 * data within an html table. It can be decorated with nested "header" and "footer" facets
 * which cause the output of header and footer rows.
 * </p>
 * <p>
 * The non-facet child components of this column are re-rendered on each table row
 * to generate the content of the cell. Those child components can reference the "var"
 * attribute of the containing h:dataTable to generate appropriate output for each row.
 * </p>
 */
@JSFComponent
(name = "h:column",
clazz = "javax.faces.component.html.HtmlColumn",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlColumnTag")
abstract class _HtmlColumn extends UIColumn
{

  static public final String COMPONENT_FAMILY = "javax.faces.Column";
  static public final String COMPONENT_TYPE = "javax.faces.HtmlColumn";

  /**
   * CSS class to be used for the header.
   *
   * @return  the new headerClass value
   */
  @JSFProperty
  public abstract String getHeaderClass();

  /**
   * CSS class to be used for the footer.
   *
   * @return  the new footerClass value
   */
  @JSFProperty
  public abstract String getFooterClass();

  /**
   * If true the column is rendered with "th" and scope="row" attribute,
   * instead "td"
   *
   * @since 2.0
   * @return
   */
  @JSFProperty (defaultValue="false")
  public abstract boolean isRowHeader();
  
  @JSFProperty(deferredValueType="java.lang.Boolean")
  @JSFExclude
  @Override
  public boolean isRendered()
  {
      return super.isRendered();
  }
}
