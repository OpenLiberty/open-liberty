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

import javax.faces.FacesWrapper;
import javax.faces.render.ResponseStateManager;
import org.apache.myfaces.renderkit.MyfacesResponseStateManager;

public class StateCacheUtils
{
    public static boolean isMyFacesResponseStateManager(ResponseStateManager rsm)
    {
        if (rsm instanceof MyfacesResponseStateManager)
        {
            return true;
        }
        else
        {
            ResponseStateManager rsm1 = rsm;
            while (rsm1 != null)
            {
                if (rsm1 instanceof MyfacesResponseStateManager)
                {
                    return true;
                }
                if (rsm1 instanceof FacesWrapper)
                {
                    rsm1 = ((FacesWrapper<? extends ResponseStateManager>) rsm1).getWrapped();
                }
                else
                {
                    rsm1 = null;
                }
            }
            return false;
        }
    }
    
    public static MyfacesResponseStateManager getMyFacesResponseStateManager(ResponseStateManager rsm)
    {
        if (rsm instanceof MyfacesResponseStateManager)
        {
            return (MyfacesResponseStateManager) rsm;
        }
        else
        {
            ResponseStateManager rsm1 = rsm;
            while (rsm1 != null)
            {
                if (rsm1 instanceof MyfacesResponseStateManager)
                {
                    return (MyfacesResponseStateManager) rsm1;
                }
                if (rsm1 instanceof FacesWrapper)
                {
                    rsm1 = ((FacesWrapper<? extends ResponseStateManager>) rsm1).getWrapped();
                }
                else
                {
                    rsm1 = null;
                }
            }
            return null;
        }
    }
}
