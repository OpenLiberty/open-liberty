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
package org.apache.myfaces.shared.util.renderkit;


/**
 * Constant declarations for JSF tags
 */
public final class JsfProperties
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // Common Attributes
    public static final String   ID_PROP                        = "id";
    public static final String   VALUE_PROP                     = "value";
    public static final String   BINDING_PROP                   = "binding";
    public static final String   STYLE_PROP                     = "style";
    public static final String   STYLE_CLASS_PROP               = "styleClass";
    public static final String   ESCAPE_PROP                    = "escape";
    public static final String   FORCE_ID_PROP                  = "forceId";
    public static final String   FORCE_ID_INDEX_PROP            = "forceIdIndex";
    public static final String   RENDERED_PROP                  = "rendered";

    // Common Output Attributes
    public static final String   FOR_PROP                       = "for";
    public static final String   CONVERTER_PROP                 = "converter";

    // Ouput_Time Attributes
    public static final String   TIME_STYLE_PROP                = "timeStyle";
    public static final String   TIMEZONE_PROP                  = "timezone";

    // Common Input Attributes
    public static final String   REQUIRED_PROP                  = "required";
    public static final String   VALIDATOR_PROP                 = "validator";
    public static final String   DISABLED_PROP                  = "disabled";
    public static final String   READONLY_PROP                  = "readonly";

    // Input_Secret Attributes
    public static final String   REDISPLAY_PROP                 = "redisplay";

    // Input_Checkbox Attributes
    public static final String   LAYOUT_PROP                    = "layout";

    // Select_Menu Attributes
    public static final String   SIZE_PROP                     = "size";

    // SelectMany Checkbox List/ Select One Radio Attributes
    public static final String BORDER_PROP                   = "border";
    public static final String DISABLED_CLASS_PROP           = "disabledClass";
    public static final String ENABLED_CLASS_PROP            = "enabledClass";
    public static final String SELECTED_CLASS_PROP           = "selectedClass";
    public static final String UNSELECTED_CLASS_PROP         = "unselectedClass";
    public static final String HIDE_NO_SELECTION_OPTION_PROP = "hideNoSelectionOption";

    // Common Command Attributes
    /**@deprecated */
    public static final String   COMMAND_CLASS_PROP           = "commandClass";
    public static final String   LABEL_PROP                   = "label";
    public static final String   IMAGE_PROP                   = "image";
    public static final String   ACTION_PROP                 = "action";
    public static final String   IMMEDIATE_PROP              = "immediate";


    // Command_Button Attributes
    public static final String   TYPE_PROP                    = "type";

    // Common Panel Attributes
    /**@deprecated */
    public static final String   PANEL_CLASS_PROP       = "panelClass";
    public static final String   FOOTER_CLASS_PROP      = "footerClass";
    public static final String   HEADER_CLASS_PROP      = "headerClass";
    public static final String   COLUMN_CLASSES_PROP    = "columnClasses";
    public static final String   ROW_CLASSES_PROP       = "rowClasses";
    public static final String   BODYROWS_PROP          = "bodyrows";

    // Panel_Grid Attributes
    public static final String   COLUMNS_PROP          = "columns";
    public static final String   COLSPAN_PROP          = "colspan"; // extension
    public static final String   CAPTION_CLASS_PROP    = "captionClass";
    public static final String   CAPTION_STYLE_PROP    = "captionStyle";

    // UIMessage and UIMessages attributes
    public static final String SHOW_SUMMARY_PROP            = "showSummary";
    public static final String SHOW_DETAIL_PROP             = "showDetail";
    public static final String GLOBAL_ONLY_PROP             = "globalOnly";

    // HtmlOutputMessage attributes
    public static final String ERROR_CLASS_PROP            = "errorClass";
    public static final String ERROR_STYLE_PROP            = "errorStyle";
    public static final String FATAL_CLASS_PROP            = "fatalClass";
    public static final String FATAL_STYLE_PROP            = "fatalStyle";
    public static final String INFO_CLASS_PROP             = "infoClass";
    public static final String INFO_STYLE_PROP             = "infoStyle";
    public static final String WARN_CLASS_PROP             = "warnClass";
    public static final String WARN_STYLE_PROP             = "warnStyle";
    public static final String TITLE_PROP                  = "title";
    public static final String TOOLTIP_PROP                = "tooltip";
    
    // HtmlOutputLink Attributes
    public static final String FRAGMENT_PROP               = "fragment";

    // GraphicImage attributes
    public static final String NAME_PROP                   = "name";
    public static final String URL_PROP                    = "url";
    public static final String LIBRARY_PROP                = "library";
    
    // HtmlOutputScript (missing) attributes
    public static final String TARGET_PROP                 = "target";
    
    // UISelectItem attributes
    public static final String ITEM_DISABLED_PROP          = "itemDisabled";
    public static final String ITEM_DESCRIPTION_PROP       = "itemDescription";
    public static final String ITEM_LABEL_PROP             = "itemLabel";
    public static final String ITEM_VALUE_PROP             = "itemValue";
    public static final String ITEM_ESCAPED_PROP           = "itemEscaped";
    public static final String NO_SELECTION_OPTION_PROP    = "noSelectionOption";
    
    // UISelectItems attributes
    public static final String ITEM_LABEL_ESCAPED_PROP     = "itemLabelEscaped";
    public static final String NO_SELECTION_VALUE_PROP     = "noSelectionValue";

    // UIData attributes
    public static final String ROWS_PROP                   = "rows";
    public static final String VAR_PROP                    = "var";
    public static final String FIRST_PROP                  = "first";

    // dataTable (extended) attributes
    public static final String ROW_ID_PROP                 = "rowId";
    public static final String ROW_STYLECLASS_PROP         = "rowStyleClass";
    public static final String ROW_STYLE_PROP              = "rowStyle";
    
    // HtmlColumn attributes
    public static final String ROW_HEADER_PROP             = "rowHeader";

    // Alternate locations (instead of using AddResource)
    public static final String JAVASCRIPT_LOCATION_PROP    = "javascriptLocation";
    public static final String IMAGE_LOCATION_PROP         = "imageLocation";
    public static final String STYLE_LOCATION_PROP         = "styleLocation";

    public static final String ACCEPTCHARSET_PROP          = "acceptcharset";
    
    //~ Myfaces Extensions -------------------------------------------------------------------------------

    // UISortData attributes
    public static final String COLUMN_PROP                 = "column";
    public static final String ASCENDING_PROP              = "ascending";
    
    // HtmlSelectManyCheckbox attributes
    public static final String LAYOUT_WIDTH_PROP           = "layoutWidth";

}
