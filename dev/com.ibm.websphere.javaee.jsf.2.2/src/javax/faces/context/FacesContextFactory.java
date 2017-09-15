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

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.lifecycle.Lifecycle;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class FacesContextFactory implements FacesWrapper<FacesContextFactory>
{
    public abstract FacesContext getFacesContext(Object context, Object request, Object response, Lifecycle lifecycle)
            throws FacesException;

    /**
     * If this factory has been decorated, the implementation doing the decorating may override this method to provide
     * access to the implementation being wrapped. A default implementation is provided that returns <code>null</code>.
     * 
     * @return the decorated <code>FacesContextFactory</code> if this factory decorates another, or <code>null</code>
     *         otherwise
     * 
     * @since 2.0
     */
    public FacesContextFactory getWrapped()
    {
        return null;
    }
}
