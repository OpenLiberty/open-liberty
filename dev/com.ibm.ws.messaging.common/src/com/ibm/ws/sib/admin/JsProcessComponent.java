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
 * @author philip
 *
 * Interface which Jetstream process components which are to be started by the JsMain
 * "bootstrap" class must implement. A "process component" has server (ie JVM) scope
 * and is instantiated even when no Messaging Engines are configured on the server
 * process. During this startup procedure, all WAS components (including
 * Jetstream components implementing this interface) are invoked in a two-step
 * manner, via the initialize() and start() calls. When the WAS server process
 * is then shutdown, then the stop() and destroy() methods are invoked.
 *
 */
public interface JsProcessComponent {

  /**
   * Method initialize.
   * 
   * Initialize the Jetstream process component. This method will be invoked 
   * once only, during WAS server process startup, once an instance of the class
   * implementing this interface has been instantiated via JsMain.
   */
  public void initialize();
  
  /**
   * Method start.
   * 
   * Starts the Jetstream process component. 
   */
  public void start();

  /**
   * Method stop.
   * 
   * Stop the Jetstream process component
   */
  public void stop();

  /**
   * Method destroy.
   * 
   * Destroy the Jetstream process component.
   */
  public void destroy();

  /**
   * Receive any custom properties and their values during the
   * initialization of the SIB. Classes implementing this method
   * will receive all of the custom properties that have been
   * set in the WAS XML configuration documents.
   * <p>
   * The implementation of this method should check for existence
   * of any expected properties as required. Unexpected properties
   * should simply be ignored. The use of a default "null"
   * implementation will be sufficient if the class does not
   * use any custom properties. 
   * 
   * @param name  the name of the custom property
   * @param value the value of the custom property
   */
  public void setCustomProperty(String name, String value);
  
  /**
   * Receive any configuration attributes and their values
   * during the initialization of the SIB. Classes implementing
   * this method will receive all of the attributes which have
   * been set in the XML configuration documents.
   * <p>
   * The implementation of this method should check for the 
   * existence of any expected attributes as required. Unexpected
   * attributes should simply be ignored. The use of a default
   * "null" implementation will be sufficient if the class does
   * not use any attributes.
   * 
   * @param name  the name of the configuration attribute
   * @param value the value of the configuration attribute
   */
  public void setAttribute(String name, String value);
}
