/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import java.rmi.RemoteException;
import java.util.ArrayList;

import com.ibm.websphere.cache.Sizeable;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.command.CommandCacheProcessor;
import com.ibm.ws.cache.config.ConfigEntry;
import com.ibm.ws.cache.servlet.CacheHook;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.cache.web.config.ConfigManager;

/**
 * This CacheableCommandImpl abstract class provides an implementation
 * for all CacheableCommand interface methods except those that the
 * command writer must write.
 * This class provides a runtime for command execution that interacts
 * with the CommandCache.
 * It also provides the contract between this command runtime and
 * the command writer.
 * <p>
 * CacheableCommandImpl is a super class of all CacheableCommands.
 * @ibm-api 
 */
public abstract class CacheableCommandImpl extends TargetableCommandImpl implements CacheableCommand, Sizeable {

   /**
    * The command cache for this command.
    */
   transient private  com.ibm.ws.cache.command.CommandCache commandCache = (com.ibm.ws.cache.command.CommandCache) ServerCache.commandCache;
   transient private boolean delayInvalidations = false;
   transient private boolean doNotCache = false;
   transient private CommandCacheProcessor commandCacheProcessor = null;
   transient private long objectSize = -1; 
   
   /**
    * The EntryInfo object for this command.
    */
   private EntryInfo entryInfo = new EntryInfo();

   /**
    * The command or JSP that called this command.
    */
   private transient CommandCaller caller = null;

   /**
    * This is the method in the CacheableCommand interface.
    * This must be implemented by the command writer.
    *
    * @return The cache id.
    * @ibm-api 
    */
   public final String getId() {
      return entryInfo.getId();
   }

   /**
    * This is the method in the CacheableCommand interface.
    * This should be implemented by the command writer only if the default
    * of EntryInfo.SHARED_PULL is not desired.
    *
    * @return The sharing policy id.
    * @ibm-api 
    */
   public final int getSharingPolicy() {
      return entryInfo.getSharingPolicy();
   }

   /**
    * This implements the CacheableCommand interface.
    * This should be implemented by the command writer only if there is
    * something that needs to be done prior to executing the command
    * on the server.
    * @ibm-api 
    */
   public boolean preExecute() {
      return false;
   }

   /**
    * Implements the CacheableCommand interface.
    * This should be implemented by the command writer only if there is
    * something that needs to be done after executing the command
    * on the server.
    * @ibm-api 
    */
   public void postExecute() {
   }

   /**
    * This implements the method in the Command interface, overriding
    * the implementation in TargetableCommandImpl.
    * <p>
    * It does the following:
    * <ul>
    * <li>Throws an UnsetInputPropertiesException if this command's
    *     isReadyToCallExecute method returns false.
    * <li>Get the CommandTarget for this command from the targetPolicy.
    * <li>If it is cached, return the cached command. The command may be
    *     cached locally or in the coordinator for the command.
    * <li>If it is not cached, call the CommandTargetProxy.executeCommand
    *     method to execute the command, which calls the
    *     TargetableCommand.performExecute method, and cache it. The
    *     command may be run locally or in the coordinator for the command.
    *     The command may be cached after execution, depending on the
    *     sharing policy.
    * <li>If the hasOutputProperties method returns true and the returned
    *     command is not the same instance as this command,
    *     it calls the setOutputProperties method so that the results will be
    *     copied into this command.
    * <li>Set the time of execution of the command.
    * </ul>
    *
    * @exception CommandException The superclass for all command exceptions.
    * @ibm-api 
    */
   public void execute() throws CommandException {
      // if cache disabled then do TargetableCommand processing
      if (commandCache == null ||
    		  (! commandCache.getCache().getCacheConfig().alwaysTriggerCommandInvalidations() &&  CacheHook.isSkipCache())) {
         super.execute();
         return;
      }
      
      if (!isReadyToCallExecute()) {
         throw new UnsetInputPropertiesException();
      }
      if (entryInfo.getId() != null) {
         throw new IllegalStateException("EntryInfo id is non-null: Command " + this.toString() + " has already been executed.");
      }
      try {
         if (targetPolicy == null) {
            throw new IllegalStateException("TargetPolicy is not set");
         }

         CommandTarget target = targetPolicy.getCommandTarget(this);

         // Cache-specific code
         CacheableCommand returnedCommand = null;
         prepareMetadata();
         
         //do-not-cache
         if (doNotCache) {
            super.execute();
            return;
         }
         
         if (getId() == null) {
            returnedCommand = commandCache.executeCommand(this, target);
         } else {
            returnedCommand = commandCache.getCommand(this, true);
         }
         CommandCaller commandCaller = getCaller();
         if (commandCaller != null) {
            com.ibm.websphere.cache.EntryInfo entryInfo = returnedCommand.getEntryInfo();
            commandCaller.unionDependencies(entryInfo);
         }
         if (hasOutputProperties() && (this != returnedCommand)) {
            if (returnedCommand == null) {
               throw new CommandException("Command returned from CommandTarget was null and " + "this command has output properties.");
            }
            try {
              setOutputProperties(returnedCommand);
            } catch (RuntimeException ex) {
               byte b[] = SerializationUtility.serialize(returnedCommand);
               returnedCommand = (CacheableCommand) SerializationUtility.deserialize(b, commandCache.getCache().getCacheName());
               setOutputProperties(returnedCommand);
               commandCache.setCommand(this);
            }
         }
         if (delayInvalidations) {
             invalidateEntries();
         }
      } catch (CommandException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.CacheableCommandImpl.execute", "203", this.getClass().getName());
         throw ex;
      } catch (Exception ex) {
         //Avoid wrappering layers upon layers
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.CacheableCommandImpl.execute", "207", this.getClass().getName());
         if (ex instanceof RemoteException) {
            RemoteException remoteException = (RemoteException) ex;
            if (remoteException.detail != null) {
               throw new CommandException(remoteException.detail);
            }
         }
         throw new CommandException(ex);
      }
   }

   /**
    * Implements the CacheableCommand interface method.
    *
    * @param caller The command that called this command.
    * @ibm-api 
    */
   public void setCaller(CommandCaller caller) {
      this.caller = caller;
   }

   /**
    * Implements the CacheableCommand interface method.
    *
    * @return The caller of the command.
    * @ibm-api 
    */
   public CommandCaller getCaller() {
      return caller;
   }

   /**
    * This is called by the CommandCache to add dependencies to this command.
    *
    * @param entryInfo This command's entryInfo.
    * @ibm-api 
    */
   public void unionDependencies(com.ibm.websphere.cache.EntryInfo entryInfo) {
      if (this.entryInfo == null) {
         return;
      }
      if (entryInfo == null) {
         throw new IllegalArgumentException("entryInfo is null");
      }
      this.entryInfo.unionDependencies(entryInfo);
   }

   /**
    * The gets this command's EntryInfo object, which holds its caching
    * metadata.
    *
    * @return The EntryInfo object.
    * @ibm-api 
    */
   public com.ibm.websphere.cache.EntryInfo getEntryInfo() {
      if (entryInfo.getTemplate() == null) {
          entryInfo.addTemplate(getClass().getName());
      }
      return entryInfo;
   }

   /**
    * Reset the command for reuse.
    * @ibm-api 
    */
   public void reset() {
      entryInfo = new EntryInfo();
      caller = null;
      commandCacheProcessor = null;
      delayInvalidations = false;
      doNotCache = false;
   }

   /**
    * executeFromCache This method will check the cache to see if the
    * given command is present.  If so then the command is populated with the
    * cached results and true is returned.  If the command is not cached, then
    * false is returned and no change is made to the state of the command.
    *
    * @return true if the command was retrieved from cache
    * @ibm-api 
    */
   public boolean executeFromCache() throws CommandException {
      if (commandCache == null) {
         return false;
      }
      if (!isReadyToCallExecute()) {
         entryInfo = new EntryInfo(); //111700: new up a fresh EntryInfo(), so that the command can still be executed later.
         throw new UnsetInputPropertiesException();
      }
      CacheableCommand returnedCommand = null;
      prepareMetadata();
      if (getId() == null) {
         entryInfo = new EntryInfo(); //111700: new up a fresh EntryInfo(), so that the command can still be executed later.
         return false;
      }

      returnedCommand = commandCache.getCommand(this, false);

      if (returnedCommand == null) {
         entryInfo = new EntryInfo(); //111700: new up a fresh EntryInfo(), so that the command can still be executed later.
         return false;
      }
      if (hasOutputProperties() && (this != returnedCommand)) {
         setOutputProperties(returnedCommand);
      }
      if (delayInvalidations) {
         invalidateEntries();
      }
      return true;
   }

   /**
    * This method will cause the current command to be placed into the cache.
    * Any existing entry with the same cache id will be replaced.
    * @ibm-api 
    */
   public void updateCache() {
      if (commandCache == null) {
         return;
      }
      if (!isReadyToCallExecute()) {
         throw new IllegalStateException("cannot call updateCache() on commands that are not ready for execution");
      }
      if (getId() == null) {
         //the command has not been executed...must build the entryinfo
         prepareMetadata();
      }
      if (entryInfo.getId() == null) {
         throw new IllegalStateException("cannot call updateCache() on commands that are not cacheable");
      }
      commandCache.setCommand(this);
      if (delayInvalidations) {
          invalidateEntries();
      }         
   }

   /**
    * This method implements the default command cache policy
    *
    *
    */
   protected void prepareMetadata() {
      
	  ConfigEntry cacheEntry = ConfigManager.getInstance().getConfigEntry(this, null);

      if (cacheEntry != null) {
    	 commandCache = (com.ibm.ws.cache.command.CommandCache)ServerCache.getCommandCache(cacheEntry.instanceName);  
         commandCacheProcessor = (CommandCacheProcessor) ConfigManager.getInstance().getCacheProcessor(cacheEntry);
         commandCacheProcessor.setCacheableCommand(this);
         commandCacheProcessor.execute();
         delayInvalidations = commandCacheProcessor.isDelayInvalidations();
         doNotCache = commandCacheProcessor.getDoNotCache();
         commandCacheProcessor.setEntryInfo(getEntryInfo());
         
         // Must check for delayInvalidations and call invalidateEntries() after using prepareMetadata 
         if (!delayInvalidations) {
             invalidateEntries();
         }
      }      
   }

   protected void invalidateEntries() {
      if (commandCacheProcessor != null) {
          commandCacheProcessor.setInvalidationIds();
          ArrayList invalidations = commandCacheProcessor.getInvalidationIds();
          if (invalidations != null) {
              int sz = invalidations.size();
              for (int i = 0; i < sz; i++)
            	  commandCache.invalidateById((String) invalidations.get(i), i == sz - 1);  // PK58295
          }
          ConfigManager.getInstance().returnCacheProcessor(commandCacheProcessor);
      }
   }
   
	public long getObjectSize() {
		return objectSize;
	}

	public void setObjectSize(long objectSize) {
		this.objectSize = objectSize;
	}
}
