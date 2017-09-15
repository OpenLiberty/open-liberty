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

import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponent;

class _CommonEventConstants
{
    public static final String COMMON_EVENTS_MARKED = "oam.COMMON_EVENTS_MARKED";
    
    public static final long ACTION_EVENT        = 0x1L;
    public static final long CLICK_EVENT         = 0x2L;
    public static final long DBLCLICK_EVENT      = 0x4L;
    public static final long MOUSEDOWN_EVENT     = 0x8L;
    public static final long MOUSEUP_EVENT       = 0x10L;
    public static final long MOUSEOVER_EVENT     = 0x20L;
    public static final long MOUSEMOVE_EVENT     = 0x40L;
    public static final long MOUSEOUT_EVENT      = 0x80L;
    public static final long KEYPRESS_EVENT      = 0x100L;
    public static final long KEYDOWN_EVENT       = 0x200L;
    public static final long KEYUP_EVENT         = 0x400L;
    public static final long FOCUS_EVENT         = 0x800L;
    public static final long BLUR_EVENT          = 0x1000L;
    public static final long SELECT_EVENT        = 0x2000L;
    public static final long CHANGE_EVENT        = 0x4000L;
    public static final long VALUECHANGE_EVENT   = 0x8000L;
    public static final long LOAD_EVENT          = 0x10000L;
    public static final long UNLOAD_EVENT        = 0x20000L;
    
    public static final Map<String, Long> COMMON_EVENTS_KEY_BY_NAME = new HashMap<String, Long>(24,1);
    
    static
    {
        //EVENTS
        COMMON_EVENTS_KEY_BY_NAME.put("change",   CHANGE_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("select",   SELECT_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("click",    CLICK_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("dblclick", DBLCLICK_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("mousedown",MOUSEDOWN_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("mouseup",  MOUSEUP_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("mouseover",MOUSEOVER_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("mousemove",MOUSEMOVE_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("mouseout", MOUSEOUT_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("keypress", KEYPRESS_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("keydown",  KEYDOWN_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("keyup",    KEYUP_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("focus",    FOCUS_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("blur",     BLUR_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("load",     LOAD_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("unload",   UNLOAD_EVENT);
        //virtual
        COMMON_EVENTS_KEY_BY_NAME.put("valueChange", VALUECHANGE_EVENT);
        COMMON_EVENTS_KEY_BY_NAME.put("action", ACTION_EVENT);
    }
    
    public static void markEvent(UIComponent component, String name)
    {
        Long propertyConstant = COMMON_EVENTS_KEY_BY_NAME.get(name);
        if (propertyConstant == null)
        {
            return;
        }
        Long commonPropertiesSet = (Long) component.getAttributes().get(COMMON_EVENTS_MARKED);
        if (commonPropertiesSet == null)
        {
            commonPropertiesSet = 0L;
        }
        component.getAttributes().put(COMMON_EVENTS_MARKED, commonPropertiesSet | propertyConstant);
    }
}
