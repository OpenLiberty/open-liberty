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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.command.CommandException;

/**
 * The TargetableCommandImpl class implements the generic methods in the
 * TargetableCommand interface. Application programmers must implement the
 * application-specific methods. The TargetableCommand interface provides
 * support for the remote execution of commands, and the TargetableCommandImpl
 * class provides a runtime for command execution.</p>
 * <p>
 * All targetable commands extend the TargetableCommandImpl class.</p>
 * 
 * @ibm-api
 */
public abstract class TargetableCommandImpl implements TargetableCommand {

   private static final TraceComponent _tc = Tr.register(TargetableCommandImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   /**
         * A target object that executes a command within the client's JVM.
         * This implementation is provided in the LocalTarget class.</p>
         * <p>
         * This is static so that it is easy to obtain.</p>
    */
   public static String LOCAL = "com.ibm.websphere.command.LocalTarget";

   /**
         * The target policy used by the command to determine the
         * target associated with the command. The default value is an
         * instance of the TargetPolicyDefault class.</p>
         * <p>
         * This is static so that it is easy to obtain.
    */
   protected static TargetPolicy targetPolicy = new TargetPolicyDefault();

   /**
         * The target object for the command.
    */
   protected transient CommandTarget commandTarget = null;

   /**
         * The name of the target object for the command. The name
         * is a fully qualified name for a Java class, for example,
         * mypkg.bp.MyBusinessCmdTarget.
       */
   protected String commandTargetName = null;

   /**
         * Indicates if the command has output properties. Defaults to
         * <code>true</code>. Can be set to <code>false</code> to eliminate
         * unecessary copying and remote invocations.
    */
   protected boolean hasOutputProperties = true;

   /**
         * Executes the task encapsulated by the command. This is the
         * default implementation for the execute() method in the Command
         * interface, but it can be overridden by the application programmer.</p>
         * <p>
         * This implementation of the execute() method does the following:
         * <ul>
         * <li>Throws an UnsetInputPropertiesException if the isReadyToCallExecute
         *     method returns <code>false</code> for the command.</li>
         * <li>Gets the target for the command according to the target
         *     policy.</li>
         * <li>Invokes the executeCommand() method on the target, which in turn
         *     calls the performExecute() method on the command.</li>
         * <li>Copies output values by using the setOutputProperties() method
         *     if the hasOutputProperties() method returns <code>true</code> and
         *     if the returned command is not the same instance as this
         *     command.</li>
         * </ul></p>
         * @exception CommandException The superclass for all command exceptions.
         *            Specificially, UnsetInputPropertiesException
         *            is thrown if this command's
         *            isReadyToCallExecute() method returns
         *            <code>false</code>.
    */
   public void execute() throws CommandException {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "execute");

      if (!isReadyToCallExecute()) {
         UnsetInputPropertiesException ex = new UnsetInputPropertiesException();
         if (_tc.isEventEnabled())
            Tr.event(_tc, "execute", ex);
         if (_tc.isDebugEnabled())
            Tr.debug(_tc, "execute", "The command is not ready to execute");
         throw ex;
      }
      try {
         if (targetPolicy == null) {
            IllegalStateException ex = new IllegalStateException("TargetPolicy is not set");

            if (_tc.isEventEnabled())
               Tr.event(_tc, "execute", ex);
            if (_tc.isDebugEnabled())
               Tr.debug(_tc, "execute", "Target policy is null");
            throw ex;
         }

         CommandTarget commandTarget = targetPolicy.getCommandTarget(this);
         // Control will not come to this portion of the code
         // if the commandTarget is null then LocalTarget is set as the command Target
         /*if (commandTarget == null) {
            throw new UnregisteredCommandException
               ("There was no registered commandTarget for Command " +
                this.getClass().getName() + ".");
         } */

         TargetableCommand returnedCommand = commandTarget.executeCommand(this);

         if (hasOutputProperties() && (this != returnedCommand)) {
            if (returnedCommand == null) {
               CommandException ex = new CommandException("Command returned from CommandTarget was null and this command has output properties.");
               if (_tc.isEventEnabled())
                  Tr.event(_tc, "execute", ex);
               if (_tc.isDebugEnabled())
                  Tr.debug(_tc, "execute", "returned command is null");
               throw ex;
            }
            setOutputProperties(returnedCommand);
         }

      } catch (CommandException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.TargetableCommandImpl.execute", "133", this);
         if (_tc.isEventEnabled())
            Tr.event(_tc, "execute", ex);
         if (_tc.isDebugEnabled())
            Tr.debug(_tc, "execute", ex);
         throw ex;
      } catch (Exception ex) {
         //Avoid wrappering layers upon layers
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.TargetableCommandImpl.execute", "137", this);
         if (ex instanceof RemoteException) {
            RemoteException remoteException = (RemoteException) ex;
            if (remoteException.detail != null) {
               CommandException e = new CommandException(remoteException.detail);
               if (_tc.isEventEnabled())
                  Tr.event(_tc, "execute", e);
               if (_tc.isDebugEnabled())
                  Tr.debug(_tc, "execute", e);
               throw e;
            }
         }

         CommandException exc = new CommandException(ex);
         if (_tc.isEventEnabled())
            Tr.event(_tc, "execute", exc);
         if (_tc.isDebugEnabled())
            Tr.debug(_tc, "execute", exc);
         throw exc;
      }

      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "execute");
   }

   /**
         * Returns the target object for the command. This implements the
         * getCommandTarget() method declared in the TargetableCommand interface.
         *
         * @return The target object, which locates the server where the
         *         command will run.
    */
   public CommandTarget getCommandTarget() {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "getCommandTarget");
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "getCommandTarget", commandTarget);
      return commandTarget;
   }

   /**
         * Returns the name of the target object for the command. This implements
         * the getCommandTargetName() method declared in the TargetableCommand
         * interface.
         *
         * @return The fully qualified name of the Java class of the target object.
    */
   public String getCommandTargetName() {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "getCommandTargetName");
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "getCommandTargetName", commandTargetName);
      return commandTargetName;
   }
   /**
       * Returns the target policy for the command.
    */
   public static TargetPolicy getTargetPolicy() {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "getTargetPolicy");
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "getTargetPolicy", TargetableCommandImpl.targetPolicy);
      return TargetableCommandImpl.targetPolicy;
   }

   /**
         * Indicates if the command has any output properties that will
         * have to be returned to the client. This implements the
         * hasOutputProperties() method in the TargetableCommand interface.
         *
         * @return The value <code>true</code> if the command has output
         *         properties that must be copied back to the client.
    */
   public final boolean hasOutputProperties() {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "hasOutputProperties");
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "hasOutputProperties", new Boolean(hasOutputProperties));
      return hasOutputProperties;
   }

   /**
         * Indicates if all required input properties have been set. This
         * abstract method is declared in the Command interface and must be
         * implemented by the application programmer.</p>
         * <p>
         * A typical implementation simply checks whether the required input
         * properties are set:
         * <pre>
         *      return (inputProperties != null)
         * </pre></p>
         * @return The value <code>true</code> if all required input properties
         *         are set and the command can be run.
    */
   public abstract boolean isReadyToCallExecute();
   /**
         * Runs the business logic that makes up the command. This abstract
         * method is declared in the TargetableCommand interface and must be
         * implemented by the application programmer. It is called by the
         * executeCommand() method on the command target.
         *
         * @exception Exception Any exception that occurs in this method is
         *            thrown as an Exception or CommandException.
    */
   public abstract void performExecute() throws Exception;
   /**
         * Sets the output properties to the values they had before the
         * the execute method was run. This abstract method is declared in
         * the Command interface and must be implemented by the application
         * programmer.</p>
         * <p>
         * A typical implementation just resets the output property variables:
         * <pre>
         *      outputPropertyP = null;
         *      outputPropertyQ = 0;
         * </pre></p>
    */
   public abstract void reset();
   /**
         * Sets the target object on the command. This implements the
         * setCommandTarget() method declared in the TargetableCommand interface.
         *
         * @param commandTarget The target object, which locates the server where
         *        the command will run.
    */
   public void setCommandTarget(CommandTarget commandTarget) {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "setCommandTarget", commandTarget);
      this.commandTarget = commandTarget;
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setCommandTarget");
   }
   /**
         * Sets the name of the target object on the command. This implements the
         * setCommandTargetName() method declared in the TargetableCommand
         * interface.
         *
         * @param commandTargetName The fully qualified name of the Java class
         *        of the target object.
    */
   public void setCommandTargetName(String commandTargetName) {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "setCommandTargetName", commandTargetName);
      this.commandTargetName = commandTargetName;
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setCommandTargetName");
   }

   /**
    * Sets the hasOutputProperties class variable to indicate if
    * the command has output properties that must be returned to the client.
    *
    * @param inProp A boolean indicating if there are output properties
    *        to be returned to the client.
   */
   public final void setHasOutputProperties(boolean inProp) {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "setHasOutputProperties", new Boolean(inProp));
      hasOutputProperties = inProp;
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setHasOutputProperties");
   }

   /**
         * Sets the return values on the command. This is the default
         * implementation for the setOutputProperties() method in the
         * TargetableCommand interface, but it can be overridden by the
         * application programmer.</p>
         * <p>
         * This implementation uses introspection to copy all instance
         * variables, provided that they are non-private and non-package;
         * that is, must be public or protected. This implementation does
         * not copy the final, static or transient fields.</p>
         * <p>
         * If this implementation is not acceptable, the programmer
         * can override this method. A typical re-implementation does the
         * following:
         * <pre>
         *      this.outputPropertyA = fromCommand.outputPropertyA;
         *      this.outputPropertyB = fromCommand.outputPropertyB;
         * </pre></p>
         *
         * @param fromCommand The command from which the output properties are
         *        copied.
    */
   public void setOutputProperties(final TargetableCommand fromCommand) {
      if (_tc.isEntryEnabled())
         Tr.entry(_tc, "setOutputProperties", fromCommand);
      try {
         final Class theClass = this.getClass();
         final Object outerThis = this;
         if (!fromCommand.getClass().equals(theClass)) {
            IllegalStateException ex = new IllegalStateException("fromCommand is not of the same class");
            if (_tc.isEventEnabled())
               Tr.event(_tc, "setOutputProperties", ex);
            if (_tc.isDebugEnabled())
               Tr.debug(_tc, "setOutputProperties", "fromCommand is not of the same class");
            throw ex;
         }

         AccessController.doPrivileged(new PrivilegedExceptionAction(){
            public Object run() throws Exception {
               Field[] fields = theClass.getDeclaredFields();
               Field.setAccessible(fields,true);
               for (int i = 0; i < fields.length; i++) {
                  int modifiers = fields[i].getModifiers();
                  if (Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
                     continue;
                  }
                  fields[i].set(outerThis, fields[i].get(fromCommand));
               }
               Field.setAccessible(fields,false);
               return null;
            }
         });
      } catch (PrivilegedActionException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.TargetableCommandImpl.setOutputProperties", "371", this);
         IllegalStateException e = new IllegalStateException("Since not all your variables are public in your command, you need to override the default implementation of TargetableCommand.setOutputProperties().");
         if (_tc.isEventEnabled())
            Tr.event(_tc, "setOutputProperties", e);
         throw e;
      }

      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setOutputProperties");
   }

   /**
         * Sets the target policy for the command.
         *
         * @param targetPolicy The target policy for the command.
    */
   public static void setTargetPolicy(TargetPolicy targetPolicy) {
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setTargetPolicy", targetPolicy);
      TargetableCommandImpl.targetPolicy = targetPolicy;
      if (_tc.isEntryEnabled())
         Tr.exit(_tc, "setTargetPolicy");
   }
}
