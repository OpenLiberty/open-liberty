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

import javax.faces.component.UIViewRoot;

/**
 *
 * @author Leonardo Uribe
 */
public abstract class ViewEntry
{
    /**
     * @return the viewRoot
     */
    public abstract UIViewRoot getViewRoot();

    /**
     * Execute all necessary steps to ensure further calls to 
     * getViewRoot() and getViewState() will return valid values.
     * It returns true or false if the Entry is still active or not
     * 
     */
    public abstract boolean activate();
    
    /**
     * @return the result
     */
    public abstract RestoreViewFromPoolResult getResult();

    /**
     * @param result the result to set
     */
    public abstract void setResult(RestoreViewFromPoolResult result);

}
