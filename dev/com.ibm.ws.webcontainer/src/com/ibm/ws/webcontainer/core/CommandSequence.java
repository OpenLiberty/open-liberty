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
package com.ibm.ws.webcontainer.core;



/**
 * Sequence of Commands to be executed in the specified sequence
 */
public interface CommandSequence 
{
   
   /**
    * @param command
    */
   public void addCommand(Command command);
   
   /**
    * @param command
    */
   public void removeCommand(Command command);
   
   /**
    * @param req
    * @param res
    */
   public void execute(Request req, Response res);
}
