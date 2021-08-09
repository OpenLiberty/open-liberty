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

public interface ClientBehaviorEvents
{
    String BLUR = "blur";
    String FOCUS = "focus";
    
    //VALUECHANGE and CHANGE are rendered in onchange
    String VALUECHANGE = "valueChange";
    String CHANGE = "change";
    String SELECT = "select";
    
    //ACTION and CLICK are rendered in onclick
    String ACTION = "action";
    String CLICK = "click";
    String DBLCLICK = "dblclick";
    
    String KEYDOWN = "keydown";
    String KEYPRESS = "keypress";
    String KEYUP = "keyup";
    
    String MOUSEDOWN = "mousedown";
    String MOUSEMOVE = "mousemove";
    String MOUSEOUT = "mouseout";
    String MOUSEOVER = "mouseover";
    String MOUSEUP = "mouseup";
    
    String LOAD = "load";
    String UNLOAD = "unload";
}
