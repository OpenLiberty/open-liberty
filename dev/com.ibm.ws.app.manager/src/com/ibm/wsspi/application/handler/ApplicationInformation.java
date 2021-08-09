/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.application.handler;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;

/**
 * This interface defines methods for accessing information about an application such as it's name and location.
 *
 * @param <T> This is the type of data that this application stores for the handler
 */
public interface ApplicationInformation<T> {

    /**
     * Returns the PID of the application as defined in the server configuration
     *
     * @return The PID
     */
    public String getPid();

    /**
     * Returns the name of the application as defined in the server configuration
     *
     * @return The name
     */
    public String getName();

    /**
     * Returns the location of the application. This should be the fully qualified absolute path to the application unless this was not available
     *
     * @return The location
     */
    public String getLocation();

    /**
     * <p>
     * This is the container that has been created for this application.
     * </p>
     * <p>
     * By default this will be set to a container created on the location on disk of this application. If the {@link #getLocation()} does not return an absolute location on disk
     * this will return <code>null</code>. The return value can be specified by {@link ApplicationHandler}s in the {@link #setContainer(Container)} method.
     * </p>
     *
     * @return The container for the application
     */
    public Container getContainer();

    /**
     * <p>This method will set a new Container onto this application. This allows {@link ApplicationHandler}s that work with an adapted (typically an {@link InterpretedContainer})
     * version of a container to only have to create it once and associate it with this application instance.</p>
     * <p>
     * The container for this application will be used to monitor when the application has been deleted so if this method is used the new container should still be for the location
     * on disk that will be used by the user.
     * </p>
     *
     * @param container The new container for this application
     */
    public void setContainer(Container container);

    /**
     * This returns the value of a property in the application configuration.
     *
     * @param propName
     * @return The value of the property.
     */
    public Object getConfigProperty(String propName);

    /**
     * This returns any information that the application handler has associated with this application.
     *
     * @return The application handler information
     */
    public T getHandlerInfo();

    /**
     * @param handlerInfo The application handler information
     */
    public void setHandlerInfo(T handlerInfo);

    /**
     * This indicates whether Jandex annotation indexes supplied in the application are to be used.
     *
     * @return
     */
    public boolean getUseJandex();
}
