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
package org.apache.myfaces.lifecycle;

import javax.faces.FacesException;

/**
 * This exception is thrown when a view is not present before start
 * one of the following phases: APPLY_REQUEST_VALUES, PROCESS_VALIDATIONS,
 * INVOKE_APPLICATION, UPDATE_MODEL_VALUES, RENDER_RESPONSE.

 * 
 * @author Leonardo Uribe
 * @since 2.0.8
 *
 */
public class ViewNotFoundException extends FacesException
{
    
    /**
     * 
     */
    private static final long serialVersionUID = -537576038024094272L;

    public ViewNotFoundException()
    {
        super();
    }

    public ViewNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ViewNotFoundException(String message)
    {
        super(message);
    }

    public ViewNotFoundException(Throwable cause)
    {
        super(cause);
    }

}
