/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container;

import java.util.Iterator;

/**
 * Interface that is implemented by objects that
 * act as container for other objects
 */
public interface Container 
{
	
	public Container getParent();
   
   /**
    * Check if container is alive or not
    * @return boolean
    */
   public boolean isAlive();
   
   /**
    * Get name
    * @return String
    */
   public String getName();
   
   /**
    * Start the container
    * This will start all the contained elements also.
    */
   public void start();
   
   /**
    * Stop the container. This will stop all the
    * contained elements also.
    */
   public void stop();
   
   /**
    * Destroy the container
    */
   public void destroy();
   
   /**
    * Remove sub container
    * @param name
    * @return Container
    */
   public Container removeSubContainer(String name);
   
   /**
    * Get at given sub container
    * @param name
    * @return Container
    */
   public Container getSubContainer(String name);
   
   /**
    * Get at all the sub containers
    * @return java.util.Iterator
    */
   @SuppressWarnings("unchecked")
   public Iterator subContainers();
   
   /**
    * Called to initialze the object
    * @param config
    */
   public void initialize(Configuration config);
   
   /**
    * Add sub container
    * @param con
    */
   public void addSubContainer(Container con);
}
