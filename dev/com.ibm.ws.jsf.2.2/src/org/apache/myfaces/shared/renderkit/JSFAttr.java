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
package org.apache.myfaces.shared.renderkit;

import javax.faces.event.ActionListener;


/**
 * Constant declarations for JSF tags
 */
public interface JSFAttr
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // Common Attributes
    String   ID_ATTR                        = "id";
    String   VALUE_ATTR                     = "value";
    String   BINDING_ATTR                   = "binding";
    String   STYLE_ATTR                     = "style";
    String   STYLE_CLASS_ATTR               = "styleClass";
    String   ESCAPE_ATTR                    = "escape";
    String   FORCE_ID_ATTR                  = "forceId";
    String   FORCE_ID_INDEX_ATTR            = "forceIdIndex";
    String   RENDERED                       = "rendered";

    // Common Output Attributes
    String   FOR_ATTR                       = "for";
    String   CONVERTER_ATTR                 = "converter";

    // Ouput_Time Attributes
    String   TIME_STYLE_ATTR                = "timeStyle";
    String   TIMEZONE_ATTR                  = "timezone";

    // Common Input Attributes
    String   REQUIRED_ATTR                  = "required";
    String   VALIDATOR_ATTR                 = "validator";
    String   DISABLED_ATTR                  = "disabled";
    String   READONLY_ATTR                  = "readonly";

    // Input_Secret Attributes
    String   REDISPLAY_ATTR                 = "redisplay";

    // Input_Checkbox Attributes
    String   LAYOUT_ATTR                    = "layout";

    // Select_Menu Attributes
    String   SIZE_ATTR                     = "size";

    // SelectMany Checkbox List/ Select One Radio Attributes
    String BORDER_ATTR                   = "border";
    String DISABLED_CLASS_ATTR           = "disabledClass";
    String ENABLED_CLASS_ATTR            = "enabledClass";
    String SELECTED_CLASS_ATTR           = "selectedClass";
    String UNSELECTED_CLASS_ATTR         = "unselectedClass";
    String HIDE_NO_SELECTION_OPTION_ATTR = "hideNoSelectionOption";

    // Common Command Attributes
    /**@deprecated */
    String   COMMAND_CLASS_ATTR           = "commandClass";
    String   LABEL_ATTR                   = "label";
    String   IMAGE_ATTR                   = "image";
    String   ACTION_ATTR                 = "action";
    String   IMMEDIATE_ATTR              = "immediate";


    // Command_Button Attributes
    String   TYPE_ATTR                    = "type";

    // Common Panel Attributes
    /**@deprecated */
    String   PANEL_CLASS_ATTR       = "panelClass";
    String   FOOTER_CLASS_ATTR      = "footerClass";
    String   HEADER_CLASS_ATTR      = "headerClass";
    String   COLUMN_CLASSES_ATTR    = "columnClasses";
    String   ROW_CLASSES_ATTR       = "rowClasses";
    String   BODYROWS_ATTR          = "bodyrows";

    // Panel_Grid Attributes
    String   COLUMNS_ATTR          = "columns";
    String   COLSPAN_ATTR          = "colspan"; // extension
    String   CAPTION_CLASS_ATTR    = "captionClass";
    String   CAPTION_STYLE_ATTR    = "captionStyle";

    // UIMessage and UIMessages attributes
    String SHOW_SUMMARY_ATTR            = "showSummary";
    String SHOW_DETAIL_ATTR             = "showDetail";
    String GLOBAL_ONLY_ATTR             = "globalOnly";

    // HtmlOutputMessage attributes
    String ERROR_CLASS_ATTR            = "errorClass";
    String ERROR_STYLE_ATTR            = "errorStyle";
    String FATAL_CLASS_ATTR            = "fatalClass";
    String FATAL_STYLE_ATTR            = "fatalStyle";
    String INFO_CLASS_ATTR             = "infoClass";
    String INFO_STYLE_ATTR             = "infoStyle";
    String WARN_CLASS_ATTR             = "warnClass";
    String WARN_STYLE_ATTR             = "warnStyle";
    String TITLE_ATTR                  = "title";
    String TOOLTIP_ATTR                = "tooltip";
    
    // HtmlOutputLink Attributes
    String FRAGMENT_ATTR               = "fragment";

    // GraphicImage attributes
    String NAME_ATTR                   = "name";
    String URL_ATTR                    = "url";
    String LIBRARY_ATTR                = "library";
    
    // HtmlOutputScript (missing) attributes
    String TARGET_ATTR                 = "target";
    
    // UISelectItem attributes
    String ITEM_DISABLED_ATTR          = "itemDisabled";
    String ITEM_DESCRIPTION_ATTR       = "itemDescription";
    String ITEM_LABEL_ATTR             = "itemLabel";
    String ITEM_VALUE_ATTR             = "itemValue";
    String ITEM_ESCAPED_ATTR           = "itemEscaped";
    String NO_SELECTION_OPTION_ATTR    = "noSelectionOption";
    
    // UISelectItems attributes
    String ITEM_LABEL_ESCAPED_ATTR     = "itemLabelEscaped";
    String NO_SELECTION_VALUE_ATTR     = "noSelectionValue";

    // UIData attributes
    String ROWS_ATTR                   = "rows";
    String VAR_ATTR                    = "var";
    String FIRST_ATTR                  = "first";

    // dataTable (extended) attributes
    String ROW_ID                      = "rowId";
    String ROW_STYLECLASS_ATTR         = "rowStyleClass";
    String ROW_STYLE_ATTR              = "rowStyle";
    
    // HtmlColumn attributes
    String ROW_HEADER_ATTR             = "rowHeader";

    // Alternate locations (instead of using AddResource)
    String JAVASCRIPT_LOCATION         = "javascriptLocation";
    String IMAGE_LOCATION              = "imageLocation";
    String STYLE_LOCATION              = "styleLocation";

    String ACCEPTCHARSET_ATTR          = "acceptcharset";
    
    //~ Myfaces Extensions -------------------------------------------------------------------------------

    // UISortData attributes
    String COLUMN_ATTR                 = "column";
    String ASCENDING_ATTR              = "ascending";
    
    // HtmlSelectManyCheckbox attributes
    String LAYOUT_WIDTH_ATTR           = "layoutWidth";

    //
    String TO_FLOW_DOCUMENT_ID_ATTR    = ActionListener.TO_FLOW_DOCUMENT_ID_ATTR_NAME;
}
