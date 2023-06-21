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
package org.apache.myfaces.application;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * @author Manfred Geiler (latest modification by $Author$)
 * @author Thomas Spiegl
 * @version $Revision$ $Date$
 */
public class ApplicationFactoryImpl extends ApplicationFactory
{
    private static final Logger log = Logger.getLogger(ApplicationFactoryImpl.class.getName());

    /**
     * Application is thread-safe (see Application javadoc)
     * "Application represents a per-web-application singleton object..." FactoryFinder has a ClassLoader-Factory Map.
     * Since each webapp has it's own ClassLoader, each webapp will have it's own private factory instances.
     */
    private Application _application;
    
    private boolean _myfacesInstanceAddedToApplicationMap = false;

    public ApplicationFactoryImpl()
    {
        createAndLogNewApplication();
    }

    private void createAndLogNewApplication()
    {
        _application = new ApplicationImpl();
        putApplicationOnMap();
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New ApplicationFactory instance created");
        }
    }

    public void purgeApplication()
    {
        createAndLogNewApplication();
    }

    @Override
    public Application getApplication()
    {
        //Put it on ApplicationMap, so javax.faces.application.Application
        //class can find it. This allows wrapped jsf 1.1 application instances
        //to work correctly in jsf 1.2 as ri does.
        if (_application != null && !_myfacesInstanceAddedToApplicationMap)
        {
            putApplicationOnMap();
        }

        return _application;
    }

    @Override
    public void setApplication(Application application)
    {
        if (application == null)
        {
            throw new NullPointerException("Cannot set a null application in the ApplicationFactory");
        }
        _application = application;
        putApplicationOnMap();
    }
    
    private void putApplicationOnMap()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null)
            {
                externalContext.
                    getApplicationMap().put("org.apache.myfaces.application.ApplicationImpl", _application);
                _myfacesInstanceAddedToApplicationMap = true;
            }
        }        
    }

}
