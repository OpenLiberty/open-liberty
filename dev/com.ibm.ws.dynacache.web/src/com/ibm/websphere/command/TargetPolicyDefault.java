/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import java.beans.Beans;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.util.SerializationUtility;

/**
 * The TargetPolicyDefault class provides an implementation of the
 * TargetPolicy interface. It provides serveral way to specify the target
 * for a command, and it implements an evaluation routine for retrieving
 * the target in its implementation of the getCommandTarget() method.</p>
 * <p>
 * The TargetPolicyDefault class allows a client to set the target
 * of a command in several ways:
 * <ul>
 * <li>The target can be set directly on the command by using the
 *     setCommandTarget() method in the TargetableCommand interface.</li>
 * <li>The name of the target can be set directly on the command
 *     by using the setCommandTargetName() method on the TargetableCommand
 *     interface.</li>
 * <li>A command can be mapped to a target using the local registerCommand()
 *     method. The TargetPolicyDefault class also provides methods for
 *     managing these mappings.</li>
 * <li>A default target can be established by using the local
 *     setDefaultTargetName() method.</li>
 * </ul></p>
 * <p>
 * The getCommandTarget() method implements the following <em>ordered</em> steps 
 * for determining the target that to be returned for a given command.
 * The getCommandTarget() method stops when it finds a target. 
 * <ol>
 *     <li>If the command contains a target object, use it.</li>
 *     <li>If the command contains a name for a target, use it.</li>
 *     <li>If there is a mapping registered on the TargetPolicyDefault object
 *         between the command and a target, use it.</li>
 *     <li>If there is a default target name defined on the TargetPolicyDefault
 *         object, use it.</li>
 *     <li>Return null if no target can be found.</li>
 * </ol></p>
 * <p>
 * The class sets the default target name to the LocalTarget class. 
 * 
 * @ibm-api
 */
public class TargetPolicyDefault implements TargetPolicy, Serializable {
    private static final TraceComponent _tc = Tr.register(TargetPolicyDefault.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
    private static final long serialVersionUID = 8533555590544760338L;
	
  /**
   * Contains the registered mappings between commands and targets.
   * The key is the name of the command; the value is the name of the target.
   */
  protected Dictionary mapping = new Hashtable();

  /**
   * Contains active targets.
   * The key is the name of the target; the value is the target object.
   */
  protected Hashtable targetTable = new Hashtable();

  /**
   * The default target name, used if no other target is found.
   */
  protected String defaultTargetName = "com.ibm.websphere.command.LocalTarget";

  /**
   * The default target object, used if no other target is found.
   */
  protected transient CommandTarget defaultTarget = null;


  /**
   * The getCommandTarget() method implements the method in the TargetPolicy
   * interface. This implementation uses the following <em>ordered</em> steps 
   * for determining the target that to be returned for a given command.
   * It stops when it finds a target. 
   * <ol>
   *     <li>If the command contains a target object, use it.</li>
   *     <li>If the command contains a name for a target, use it.</li>
   *     <li>If there is a mapping registered on the TargetPolicyDefault object
   *         between the command and a target, use it.</li>
   *     <li>If there is a default target name defined on the 
   *         TargetPolicyDefault object, use it.</li>
   *     <li>Return null if no target can be found.</li>
   * </ol></p>
   *
   * @param command The TargetableCommand whose CommandTarget is needed.
   * @return The CommandTarget for the command. 
   */
  public CommandTarget getCommandTarget(TargetableCommand command) {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "getCommandTarget", command);

	CommandTarget target = null;

	// First, try asking the command itself for a CommandTarget.
	target = command.getCommandTarget();
	if (target != null) {
          if (_tc.isEntryEnabled()) Tr.exit(_tc, "getCommandTarget", target);
	  return target;
	}
	// Second, try asking the command itself for a CommandTarget bean name.
	String targetName = command.getCommandTargetName();
	if (targetName == null) {
	  // Third, see if a CommandTarget bean name has been registered 
	  // for the command.
	  String commandName = command.getClass().getName();
	  targetName = (String) mapping.get(commandName);
	}

	if (targetName == null) {
	  // Fourth, use the default
	  if (defaultTarget == null && 
		  defaultTargetName != null) {
		try {
		  defaultTarget = (CommandTarget) Beans.instantiate(SerializationUtility.getContextClassLoader(), defaultTargetName);  

		} 
                catch (Exception e) { // return default on error

		  com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.websphere.command.TargetPolicyDefault.getCommandTarget", "134", this);
                  if (_tc.isEventEnabled()) Tr.event(_tc, "executeCommand", e);
                  if (_tc.isDebugEnabled()) Tr.debug(_tc, "executeCommand", "Beans.instantiate() failed "+ e.getMessage());
		   
                  if (_tc.isEntryEnabled()) Tr.exit(_tc, "getCommandTarget", defaultTarget);
	          return defaultTarget;
		}
	  }

          if (_tc.isEntryEnabled()) Tr.exit(_tc, "getCommandTarget", defaultTarget);
	  return defaultTarget;
	}

	// Look for a cached instance of the CommandTarget
	target = (CommandTarget) 
			 targetTable.get(targetName);

	// Create a new instance of CommandTarget, if (one wasn't cached) {
	if (target == null) {
	  Class targetClass = null; 

	  try {
		Object targetObj = Beans.instantiate(SerializationUtility.getContextClassLoader(), targetName);
		target = (CommandTarget)targetObj;

	  } 
          catch (Exception e) { // return default on error
	    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.websphere.command.TargetPolicyDefault.getCommandTarget", "157", this);
            if (_tc.isEventEnabled()) Tr.event(_tc, "executeCommand", e);
            if (_tc.isDebugEnabled()) Tr.debug(_tc, "executeCommand", "Beans.instantiate() failed "+ e.getMessage());
            if (_tc.isEntryEnabled()) Tr.exit(_tc, "getCommandTarget", defaultTarget);
            return defaultTarget;
	  }
	  targetTable.put(targetName, target);
	}

        if (_tc.isEntryEnabled()) Tr.exit(_tc, "getCommandTarget", target);
	return target;
  }  

  /**
   * Lists all the command-to-target mappings.
   * 
   * @return A Dictionary of mappings between command names and target names.
   * The key is the name of the command; the value is the name of the target.
   */
  public Dictionary listMappings() {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "listMappings");
      if (_tc.isEntryEnabled()) Tr.exit(_tc, "listMappings", mapping);
      return mapping;
  }  

  /**
   * Registers a single command-to-target mapping.
   *      
   * @param commandBeanName The name of the command.
   * @param targetBeanName The name of the target.
   */
  public void registerCommand(String commandBeanName, String targetBeanName) {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "registerCommand", new Object[] {commandBeanName, targetBeanName});
      mapping.put(commandBeanName, targetBeanName);
      if (_tc.isEntryEnabled()) Tr.exit(_tc, "registerCommand");
  }  

  /**
   * Sets the default target name, used if no other target is found.
   * This implementation uses the LocalTarget class as the default
   * target name.
   * 
   * @param defaultTargetName The name of the default target.
   */
  public void setDefaultTargetName(String defaultTargetName) {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "setDefaultTargetName", defaultTargetName);
      this.defaultTargetName = defaultTargetName; 
      if (_tc.isEntryEnabled()) Tr.exit(_tc, "setDefaultTargetName");
  }  

  /**
   * Unregisters a single command-to-target mapping.
   *      
   * @param commandBeanName The name of the command.
   */
  public void unregisterCommand(String commandBeanName) {
        if (_tc.isEntryEnabled()) Tr.entry(_tc, "unregisterCommand", commandBeanName);
	mapping.remove(commandBeanName);
        if (_tc.isEntryEnabled()) Tr.exit(_tc, "unregisterCommand");
  }  
}
