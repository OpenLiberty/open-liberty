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

package jakarta.faces.application;

import jakarta.faces.context.FacesContext;
import jakarta.faces.FacesWrapper;
import java.io.IOException;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class StateManagerWrapper extends StateManager implements FacesWrapper<StateManager>
{
    private StateManager delegate;

    @Deprecated
    public StateManagerWrapper()
    {
    }

    public StateManagerWrapper(StateManager delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public StateManager getWrapped()
    {
        return delegate;
    }

    @Override
    public boolean isSavingStateInClient(FacesContext context)
    {
        return getWrapped().isSavingStateInClient(context);
    }

    @Override
    public String getViewState(FacesContext context)
    {
        return getWrapped().getViewState(context);
    }

    @Override
    public void writeState(FacesContext context, Object state) throws IOException
    {
        getWrapped().writeState(context, state);
    }
}
