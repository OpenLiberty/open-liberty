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
package org.apache.myfaces.webapp;

import javax.servlet.ServletContextEvent;


/**
 * Startup Listener for the myfaces init process
 * This interface allows to implement
 * Plugins which then can be hooked into the various stages
 * of our initialisation process to add various plugins
 * which depend on the various phases of the init
 *
 * @author Werner Punz
 */
public interface StartupListener
{
    /**
     * This method is called before myfaces initializes
     *
     * @param evt the corresponding servlet context event keeping all the servlet context data and references
     */
    public void preInit(ServletContextEvent evt);

    /**
     * This method is called after myfaces has initialized
     *
     * @param evt the corresponding servlet context event keeping all the servlet context data and references
     */
    public void postInit(ServletContextEvent evt);

    /**
     * This method is called before myfaces is destroyed
     *
     * @param evt the corresponding servlet context event keeping all the servlet context data and references
     */
    public void preDestroy(ServletContextEvent evt);

    /**
     * This method is called after myfaces is destroyed
     *
     * @param evt the corresponding servlet context event keeping all the servlet context data and references
     */
    public void postDestroy(ServletContextEvent evt);

}

