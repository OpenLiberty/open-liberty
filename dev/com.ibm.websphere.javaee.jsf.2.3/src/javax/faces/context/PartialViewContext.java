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
package javax.faces.context;

import java.util.Collection;

import javax.faces.event.PhaseId;

/**
 * @since 2.0
 */
public abstract class PartialViewContext
{
    public static final String ALL_PARTIAL_PHASE_CLIENT_IDS = "@all";
    //public static final String NO_PARTIAL_PHASE_CLIENT_IDS = "@none";
    public static final String PARTIAL_EXECUTE_PARAM_NAME = "javax.faces.partial.execute";
    public static final String PARTIAL_RENDER_PARAM_NAME = "javax.faces.partial.render";
    
    /**
     * @since 2.2
     */
    public static final java.lang.String RESET_VALUES_PARAM_NAME = "javax.faces.partial.resetValues";
    
    /**
     * @since 2.3
     */
    public static final String PARTIAL_EVENT_PARAM_NAME =
          "javax.faces.partial.event";
    
    public abstract Collection<String> getExecuteIds();
    
    public abstract PartialResponseWriter getPartialResponseWriter();
    
    public abstract Collection<String> getRenderIds();
    
    public abstract boolean isAjaxRequest();
    
    public abstract boolean isExecuteAll();
    
    public abstract boolean isPartialRequest();
    
    public abstract boolean isRenderAll();
    
    public abstract void processPartial(PhaseId phaseId);
    
    public abstract void release();
    
    public abstract void setPartialRequest(boolean isPartialRequest);
    
    public abstract void setRenderAll(boolean renderAll);

    /**
    * @since 2.3
    */
    public abstract java.util.List<java.lang.String> getEvalScripts();
    
    /**
     * @since 2.2
     * @return 
     */
    public boolean isResetValues()
    {
        return false;
    }

}
