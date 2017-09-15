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
package org.apache.myfaces.view.facelets.el;

import javax.faces.view.Location;

/**
 * Identification inferface for types that know about {@link Location}.
 * 
 * If type implements this interface, we can say that it knows where instance
 * implementing this interfaces is located in facelets view.
 * 
 * {@link javax.faces.component.UIComponent} is LocationAware-like, because it knows
 * it's Location: {@link javax.faces.component.UIComponent#VIEW_LOCATION_KEY}
 * 
 * @author martinkoci
 */
public interface LocationAware
{
    
    /**
     * @return the {@link Location} instance where this object exists/is related to
     */
    Location getLocation();

}
