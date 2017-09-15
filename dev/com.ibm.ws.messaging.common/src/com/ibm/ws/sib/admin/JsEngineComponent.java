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
package com.ibm.ws.sib.admin;

/**
 * Interface to support a "first class" component of a SIB Messaging Engine.
 * Classes which implement this interface and which require some bootstrap into
 * a WAS server process (typically as a result of the existence of some WCCM
 * configuration objects) need to be directly instantiated by the SM (Admin)
 * component.
 */
public interface JsEngineComponent {

    /**
     * Initialize the engine component.
     * 
     * @param engine
     *            the messaging engine instance in which the instance of the
     *            class which implements this interface is running.
     */
    public void initialize(JsMessagingEngine engine) throws Exception;

    /**
     * Start the engine component.
     * 
     * @param mode
     *            specifies the type of start operation which is to be
     *            performed. The valid start modes are defined in JsConstants.
     * @throws Exception
     */
    public void start(int mode) throws Exception;

    /**
     * Indicate that the WAS server in which the instance is contained has
     * started. This notification is performed once only, when both the WAS
     * server is started and the instance of the engine has entered the STARTED
     * state.
     */
    public void serverStarted();

    /**
     * Indicate that the WAS server in which the object is contained is
     * stopping. This notification is performed once only, when both the WAS
     * server is stopping and the instance of the engine has entered the STARTED
     * state.
     */
    public void serverStopping();

    /**
     * Stop the engine component.
     * 
     * @param mode
     *            specifies the type of stop operation which is to be performed.
     *            The valid stop modes are defined in JsConstants.
     */
    public void stop(int mode);

    /**
     * Destroy the engine component.
     */
    public void destroy();

    /**
     * Set the WCCM configuration. This method will be invoked with the
     * ConfigObject of the given engine component. If it does not have its own
     * config object in WCCM, then it will receive the Messaging Engine config
     * object.
     * <p>
     * The implementation of this method should get their expected attributes
     * from their ConfigOjbect.
     * 
     * @param config
     *            the configuration object
     */
    public void setConfig(LWMConfig config);

    /**
     * Set any configured custom properties and their values. This method will
     * be invoked for all custom properties defined on the Messaging Engine in
     * the WCCM configuration.
     * <p>
     * The implementation of this method should check for the existence of any
     * expected properties as required. Unexpected properties should simply be
     * ignored, as these may be intended for use by other JsEngineComponents.
     * The use of a default "null" implementation will be sufficient if the
     * class does not use any custom properties.
     * 
     * @param name
     *            the name of the custom property
     * @param value
     *            the value of the custom property
     */
    public void setCustomProperty(String name, String value);

    /**
     * Indicates if the WCCM configuration has been modified for the bus in
     * which this engine resides.
     * 
     * @param newBus
     *            new config object loaded from the config files
     * @param busChanged
     *            flag set to true if the bus config file has changed
     * @param destChg
     *            flag set to true if the destination config file has changed
     * @param medChg
     *            flag set to true if the mediation config file has changed
     */
    public void busReloaded(Object newBus, boolean busChanged, boolean destChg,
                            boolean medChg);

    /**
     * Denotes the end of the dynamic config reload cycle and as such will be
     * call if busReload was called previously, even if the engine config files
     * have not changed. If the engine config files have changed the engine
     * parameter will be a newly created JsMessagingEngineImpl object allowing
     * access to the new data. If the engine config files have not changed the
     * engine parameter will contain the existing JsMessagingEngineImpl object.
     * All points localized on this message engine, which have changed, will
     * have been communicated to MP before this call is made.
     * 
     * @param engine
     *            The current message engine, either a new one or the existing
     *            one.
     */
    public void engineReloaded(Object engine);

}
