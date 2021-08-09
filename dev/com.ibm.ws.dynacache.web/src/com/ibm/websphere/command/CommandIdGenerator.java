/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import java.util.ArrayList;

/**
 * The CommandIdGenerator is responsible for generating cache 
 * entry ids and data ids, and invalidating data ids for command objects. <p>
 * One IdGenerator instance will exist for each cacheable command object 
 * identified in WebSphere.  When implementing this interface, be
 * aware that multiple threads may be using the same IdGenerator
 * concurrently. 
 * @ibm-api 
 */
public interface CommandIdGenerator {

    /**
     * This gets the cache id for the command.
     * It typically has a simple relationship to the
     * command class and input properties.
     * However, it can involve any logic that can be invoked in the
     * command client's JVM.
     *
     * @param  command     The command used to generate a cache id
     * @param  groupIds    Add any additional groupIds to the command
     *                     using the following ArrayList
     * @return The cache id or null if the command is not cacheable
     * @ibm-api 
     */
   public String getId(CacheableCommand command, ArrayList groupIds);

}
