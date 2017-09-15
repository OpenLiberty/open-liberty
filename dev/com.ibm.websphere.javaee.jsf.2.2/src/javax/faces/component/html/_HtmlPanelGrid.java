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
 * This element renders as an HTML table with specified number of columns.
 * <p>
 * Children of this element are rendered as cells in the table, filling
 * rows from left to right.  Facets named "header" and "footer" are optional
 * and specify the content of the thead and tfoot rows, respectively.
 * </p>
 */
@JSFComponent
(name = "h:panelGrid",
clazz = "javax.faces.component.html.HtmlPanelGrid",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlPanelGridTag",
defaultRendererType = "javax.faces.Grid",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder"
)
abstract class _HtmlPanelGrid extends UIPanel implements _EventProperties,
_StyleProperties, _UniversalProperties, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Panel";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlPanelGrid";

  /**
   * HTML: The background color of this element.
   * 
   * @JSFProperty
   */
  public abstract String getBgcolor();

  /**
   * CSV of several row index to start (and end a previous) tbody element
   * 
   * @since 2.0
   */
  @JSFProperty
  public abstract String getBodyrows();
  
  /**
   * HTML: Specifies the width of the border of this element, in pixels.  Deprecated in HTML 4.01.
   * 
   * @JSFProperty
   *   defaultValue="Integer.MIN_VALUE"
   */
  public abstract int getBorder();

  /**
   * HTML: Specifies the amount of empty space between the cell border and
   * its contents.  It can be either a pixel length or a percentage.
   * 
   * @JSFProperty
   */
  public abstract String getCellpadding();

  /**
   * HTML: Specifies the amount of space between the cells of the table.
   * It can be either a pixel length or a percentage of available 
   * space.
   * 
   * @JSFProperty
   */
  public abstract String getCellspacing();

  /**
   * A comma separated list of CSS class names to apply to td elements in
   * each column.
   * 
   * @JSFProperty
   */
  public abstract String getColumnClasses();

  /**
   * Specifies the number of columns in the grid.
   * 
   * @JSFProperty
   *   defaultValue="1"
   */
  public abstract int getColumns();

  /**
   * The CSS class to be applied to footer cells.
   * 
   * @JSFProperty
   */
  public abstract String getFooterClass();

  /**
   * HTML: Controls what part of the frame that surrounds a table is 
   * visible.  Values include:  void, above, below, hsides, lhs, 
   * rhs, vsides, box, and border.
   * 
   * @JSFProperty
   */
  public abstract String getFrame();

  /**
   * The CSS class to be applied to header cells.
   * 
   * @JSFProperty
   */
  public abstract String getHeaderClass();

  /**
   * A comma separated list of CSS class names to apply to td elements in
   * each row.
   * 
   * @JSFProperty
   */
  public abstract String getRowClasses();

  /**
   * HTML: Controls how rules are rendered between cells.  Values include:
   * none, groups, rows, cols, and all.
   * 
   * @JSFProperty
   */
  public abstract String getRules();

  /**
   * HTML: Provides a summary of the contents of the table, for
   * accessibility purposes.
   * 
   * @JSFProperty
   */
  public abstract String getSummary();

  /**
   * HTML: Specifies the desired width of the table, as a pixel length or
   * a percentage of available space.
   * 
   * @JSFProperty
   */
  public abstract String getWidth();

  /**
   * A comma separated list of CSS class names to apply to all captions.
   * If there are less classes than the number of rows, apply the same
   * sequence of classes to the remaining captions, so the pattern is repeated.
   * More than one class can be applied to a row by separating the classes
   * with a space.
   *
   * @return  the new captionClass value
   */
  @JSFProperty
  public abstract String getCaptionClass();
  
  /**
   * Gets The CSS class to be applied to the Caption.
   *
   * @return  the new captionStyle value
   */
  @JSFProperty
  public abstract String getCaptionStyle();

}
