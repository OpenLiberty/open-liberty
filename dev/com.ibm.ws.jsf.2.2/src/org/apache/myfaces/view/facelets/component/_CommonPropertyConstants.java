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
package org.apache.myfaces.view.facelets.component;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * This is a list of the most common properties used by a JSF html
 * component, organized by interfaces.
 * 
 * Note there is a copy of this class on 
 * org.apache.myfaces.shared.renderkit.html.CommonPropertyConstants.
 * Any changes here should be committed there too.
 * 
 * @author Leonardo Uribe
 *
 */
class _CommonPropertyConstants
{
    public static final String COMMON_PROPERTIES_MARKED = "oam.COMMON_PROPERTIES_MARKED";
    
    //_StyleProperties
    public static final long STYLE_PROP       = 0x1L;
    public static final long STYLECLASS_PROP = 0x2L;
    
    //_UniversalProperties
    //_TitleProperty
    public static final long DIR_PROP         = 0x4L;
    public static final long LANG_PROP        = 0x8L;
    public static final long TITLE_PROP       = 0x10L;
    
    //_EscapeProperty
    public static final long ESCAPE_PROP      = 0x20L;

    //_DisabledClassEnabledClassProperties
    //_DisabledReadonlyProperties
    public static final long DISABLED_PROP    = 0x40L;
    public static final long ENABLED_PROP     = 0x80L;
    public static final long READONLY_PROP    = 0x100L;

    //_AccesskeyProperty
    public static final long ACCESSKEY_PROP  = 0x200L;
    
    //_AltProperty
    public static final long ALT_PROP         = 0x400L;
    
    //_ChangeSelectProperties
    public static final long ONCHANGE_PROP    = 0x800L;
    public static final long ONSELECT_PROP    = 0x1000L;
    
    //_EventProperties
    public static final long ONCLICK_PROP     = 0x2000L;
    public static final long ONDBLCLICK_PROP  = 0x4000L;
    public static final long ONMOUSEDOWN_PROP = 0x8000L;
    public static final long ONMOUSEUP_PROP   = 0x10000L;
    public static final long ONMOUSEOVER_PROP = 0x20000L;
    public static final long ONMOUSEMOVE_PROP = 0x40000L;
    public static final long ONMOUSEOUT_PROP  = 0x80000L;
    public static final long ONKEYPRESS_PROP  = 0x100000L;
    public static final long ONKEYDOWN_PROP   = 0x200000L;
    public static final long ONKEYUP_PROP     = 0x400000L;
    
    //_FocusBlurProperties
    public static final long ONFOCUS_PROP     = 0x800000L;
    public static final long ONBLUR_PROP      = 0x1000000L;

    //_LabelProperty
    public static final long LABEL_PROP       = 0x2000000L;
    
    //_LinkProperties
    public static final long CHARSET_PROP     = 0x4000000L;
    public static final long COORDS_PROP      = 0x8000000L;
    public static final long HREFLANG_PROP    = 0x10000000L;
    public static final long REL_PROP         = 0x20000000L;
    public static final long REV_PROP         = 0x40000000L;
    public static final long SHAPE_PROP       = 0x80000000L;
    public static final long TARGET_PROP      = 0x100000000L;
    public static final long TYPE_PROP        = 0x200000000L;

    //_TabindexProperty
    public static final long TABINDEX_PROP    = 0x400000000L;
    
    //Common to input fields
    public static final long ALIGN_PROP       = 0x800000000L;
    public static final long CHECKED_PROP     = 0x1000000000L;
    public static final long MAXLENGTH_PROP   = 0x2000000000L;
    public static final long SIZE_PROP        = 0x4000000000L;
    
    public static final Map<String, Long> COMMON_PROPERTIES_KEY_BY_NAME = new HashMap<String, Long>(64,1);
    
    static
    {
        COMMON_PROPERTIES_KEY_BY_NAME.put("style",      STYLE_PROP);
        
        COMMON_PROPERTIES_KEY_BY_NAME.put("styleClass", STYLECLASS_PROP);
        
        //_UniversalProperties
        //_TitleProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("dir",        DIR_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("lang",       LANG_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("title",      TITLE_PROP);
        
        //_EscapeProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("escape",     ESCAPE_PROP);

        //_DisabledClassEnabledClassProperties
        //_DisabledReadonlyProperties
        COMMON_PROPERTIES_KEY_BY_NAME.put("disabled",   DISABLED_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("enabled",    ENABLED_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("readonly",   READONLY_PROP);

        //_AccesskeyProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("accesskey",  ACCESSKEY_PROP);
        
        //_AltProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("alt",        ALT_PROP);
        
        //_ChangeSelectProperties
        COMMON_PROPERTIES_KEY_BY_NAME.put("onchange",   ONCHANGE_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onselect",   ONSELECT_PROP);
        
        //_EventProperties
        COMMON_PROPERTIES_KEY_BY_NAME.put("onclick",    ONCLICK_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("ondblclick", ONDBLCLICK_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onmousedown",ONMOUSEDOWN_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onmouseup",  ONMOUSEUP_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onmouseover",ONMOUSEOVER_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onmousemove",ONMOUSEMOVE_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onmouseout", ONMOUSEOUT_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onkeypress", ONKEYPRESS_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onkeydown",  ONKEYDOWN_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onkeyup",    ONKEYUP_PROP);
        
        //_FocusBlurProperties
        COMMON_PROPERTIES_KEY_BY_NAME.put("onfocus",    ONFOCUS_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("onblur",     ONBLUR_PROP);

        //_LabelProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("label",      LABEL_PROP);
        
        //_LinkProperties
        COMMON_PROPERTIES_KEY_BY_NAME.put("charset",    CHARSET_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("coords",     COORDS_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("hreflang",   HREFLANG_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("rel",        REL_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("rev",        REV_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("shape",      SHAPE_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("target",     TARGET_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("type",       TYPE_PROP);

        //_TabindexProperty
        COMMON_PROPERTIES_KEY_BY_NAME.put("tabindex",   TABINDEX_PROP);

        //Common to input fields
        COMMON_PROPERTIES_KEY_BY_NAME.put("align",      ALIGN_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("checked",    CHECKED_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("maxlength",  MAXLENGTH_PROP);
        COMMON_PROPERTIES_KEY_BY_NAME.put("size",       SIZE_PROP);
    }
    
    public static void markProperty(UIComponent component, String name)
    {
        Long propertyConstant = COMMON_PROPERTIES_KEY_BY_NAME.get(name);
        if (propertyConstant == null)
        {
            return;
        }
        Long commonPropertiesSet = (Long) component.getAttributes().get(COMMON_PROPERTIES_MARKED);
        if (commonPropertiesSet == null)
        {
            commonPropertiesSet = 0L;
        }
        component.getAttributes().put(COMMON_PROPERTIES_MARKED, commonPropertiesSet | propertyConstant);
    }
    
    public static void markProperty(UIComponent component, long propertyConstant)
    {
        Long commonPropertiesSet = (Long) component.getAttributes().get(COMMON_PROPERTIES_MARKED);
        if (commonPropertiesSet == null)
        {
            commonPropertiesSet = 0L;
        }
        component.getAttributes().put(COMMON_PROPERTIES_MARKED, commonPropertiesSet | propertyConstant);
    }
}
