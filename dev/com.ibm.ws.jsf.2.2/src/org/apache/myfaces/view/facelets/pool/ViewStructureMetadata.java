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
package org.apache.myfaces.view.facelets.pool;

import org.apache.myfaces.context.RequestViewMetadata;

/**
 * Contains additional information associated to a view and restored each time
 * the view is built by first time.
 *
 */
public abstract class ViewStructureMetadata
{
    /**
     * Retrieve the state of UIViewRoot after the view is built by first time.
     * 
     * @return the viewState
     */
    public abstract Object getViewRootState();
    
    /**
     * Retrieve the associated RequestViewMetadata to this view structure.
     * 
     * @return 
     */
    public abstract RequestViewMetadata getRequestViewMetadata();
    
}
