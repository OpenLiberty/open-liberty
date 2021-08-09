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
package org.apache.myfaces.application.viewstate;

import javax.faces.context.FacesContext;

import org.apache.myfaces.application.StateCache;
import org.apache.myfaces.application.StateCacheFactory;

public class StateCacheFactoryImpl extends StateCacheFactory
{

    private StateCache _clientSideStateCache;
    private StateCache _serverSideStateCache;
    
    public StateCacheFactoryImpl()
    {
        _clientSideStateCache = new ClientSideStateCacheImpl();
        _serverSideStateCache = new ServerSideStateCacheImpl();
    }

    @Override
    public StateCache getStateCache(FacesContext facesContext)
    {
        if (facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
        {
            return _clientSideStateCache;
        }
        else
        {
            return _serverSideStateCache;
        }
    }
}
