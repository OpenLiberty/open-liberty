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
package org.apache.myfaces.shared.component;

/**
 * Behavioral interface.
 * By default, displayValueOnly is false, and the components have the default behaviour.
 * When displayValueOnly is true, the renderer should not render any input widget.
 * Only the text corresponding to the component's value should be rendered instead.
 */
public interface DisplayValueOnlyCapable
{
    String DISPLAY_VALUE_ONLY_ATTR = "displayValueOnly";
    String DISPLAY_VALUE_ONLY_STYLE_ATTR = "displayValueOnlyStyle";
    String DISPLAY_VALUE_ONLY_STYLE_CLASS_ATTR = "displayValueOnlyStyleClass";
    
    boolean isSetDisplayValueOnly();
    boolean isDisplayValueOnly();
    void setDisplayValueOnly(boolean displayValueOnly);

    String getDisplayValueOnlyStyle();
    void setDisplayValueOnlyStyle(String style);
    
    String getDisplayValueOnlyStyleClass();
    void setDisplayValueOnlyStyleClass(String styleClass);
}
