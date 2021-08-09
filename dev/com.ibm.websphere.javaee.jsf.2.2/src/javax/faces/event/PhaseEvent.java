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
package javax.faces.event;

import javax.faces.context.FacesContext;
import javax.faces.lifecycle.Lifecycle;
import java.util.EventObject;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public class PhaseEvent extends EventObject
{
    private static final long serialVersionUID = -7235692965954486239L;
    // FIELDS
    private FacesContext _facesContext;
    private PhaseId _phaseId;

    // CONSTRUCTORS
    public PhaseEvent(FacesContext facesContext, PhaseId phaseId, Lifecycle lifecycle)
    {
        super(lifecycle);
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext");
        }
        if (phaseId == null)
        {
            throw new NullPointerException("phaseId");
        }
        if (lifecycle == null)
        {
            throw new NullPointerException("lifecycle");
        }

        _facesContext = facesContext;
        _phaseId = phaseId;
    }

    // METHODS
    public FacesContext getFacesContext()
    {
        return _facesContext;
    }

    public PhaseId getPhaseId()
    {
        return _phaseId;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((_facesContext == null) ? 0 : _facesContext.hashCode());
        result = prime * result + ((_phaseId == null) ? 0 : _phaseId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        PhaseEvent other = (PhaseEvent) obj;
        if (source == null)
        {
            if (other.source != null)
            {
                return false;
            }
        }
        else if (!source.equals(other.source))
        {
            return false;
        }
        if (_facesContext == null)
        {
            if (other._facesContext != null)
            {
                return false;
            }
        }
        else if (!_facesContext.equals(other._facesContext))
        {
            return false;
        }
        if (_phaseId == null)
        {
            if (other._phaseId != null)
            {
                return false;
            }
        }
        else if (!_phaseId.equals(other._phaseId))
        {
            return false;
        }
        return true;
    }

}
