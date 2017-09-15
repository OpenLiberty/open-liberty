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
package org.apache.myfaces.view.facelets.pool.impl;

import org.apache.myfaces.context.RequestViewMetadata;
import org.apache.myfaces.view.facelets.pool.ViewStructureMetadata;

/**
 *
 * @author lu4242
 */
public class ViewStructureMetadataImpl extends ViewStructureMetadata
{
    private Object viewState;
    
    private RequestViewMetadata rvc;
    
    public ViewStructureMetadataImpl(Object viewState, RequestViewMetadata rvc)
    {
        this.viewState = viewState;
        this.rvc = rvc;
    }
    
    /**
     * @return the viewState
     */
    @Override
    public Object getViewRootState()
    {
        return viewState;
    }

    /**
     * @param viewState the viewState to set
     */
    public void setViewState(Object viewState)
    {
        this.viewState = viewState;
    }

    /**
     * @return the rvc
     */
    public RequestViewMetadata getRequestViewMetadata()
    {
        return rvc;
    }

    /**
     * @param rvc the rvc to set
     */
    public void setRequestViewMetadata(RequestViewMetadata rvc)
    {
        this.rvc = rvc;
    }
    
}
