/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.command;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.websphere.command.CacheableCommand;
import com.ibm.websphere.command.CommandIdGenerator;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.config.CacheId;
import com.ibm.ws.cache.config.CacheProcessor;
import com.ibm.ws.cache.config.Component;
import com.ibm.ws.cache.config.ConfigEntry;
import com.ibm.ws.cache.config.Field;
import com.ibm.ws.cache.config.Invalidation;
import com.ibm.ws.cache.config.Method;
import com.ibm.ws.cache.util.SerializationUtility;

public class CommandCacheProcessor extends CacheProcessor {

   private static TraceComponent tc = Tr.register(CommandCacheProcessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   protected CacheableCommand command;

   // We also have a variable in the superclass called sharingPolicy. We never use that variable, but we may 
   // want to consider consolidating, depend on what we're trying to achieve, functionally.
   private int commandCacheSharingPolicy;

   public CommandCacheProcessor() {
   }

   // returns the basename for the cache id
   public String getBaseName() {
      return command.getClass().getName();
   }

   public void reset(ConfigEntry ce) {
      super.reset(ce);
      if (ce != null) {
    	  commandCacheSharingPolicy = ce.sharingPolicy;
      }
      
      command = null;
   }

   public void setCacheableCommand(CacheableCommand command) {
      this.command = command;
   }

   protected Object getComponentValue(Component c) {
      Object result = null;
      if (c.idMethod != null)
         result = processMethod(c.idMethod, command);
      else if (c.idField != null) {
         result = processField(c.idField, command);
      } else if (c.type.equals("method")) {
         c.idMethod = new Method();
         c.idMethod.name = c.id;
         c.idMethod.index = c.index;
         result = processMethod(c.idMethod, command);
      } else if (c.type.equals("field")) {
         c.idField = new Field();
         c.idField.name = c.id;
         c.idField.index = c.index;
         result = processField(c.idField, command);
      } else {
         Tr.error(tc, "DYNA0038E", new Object[] { c.type, command.getClass().getName()});
      }
      if (result != null) {
         if (c.method != null)
            result = processMethod(c.method, result);
         else if (c.field != null)
            result = processField(c.field, result);
      }
      return result;
   }

   public int getSharingPolicy() {
      return commandCacheSharingPolicy;
   }

   protected String processIdGenerator(CacheId cacheid) {
      if (cacheid.idGeneratorImpl == null) {
         try {
            Class c = Class.forName(cacheid.idGenerator, true, SerializationUtility.getContextClassLoader());
            cacheid.idGeneratorImpl = c.newInstance();
         } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CommandCacheProcessor.processIdGenerator", "121", this);
            //ex.printStackTrace();
            Tr.error(tc, "dynacache.idgeneratorerror", new Object[] { cacheid.idGenerator });
         }
      }
      if (groupIds == null)
         groupIds = new ArrayList();
      return ((CommandIdGenerator) (cacheid.idGeneratorImpl)).getId(command, groupIds);
   }

   protected void processMetaDataGenerator(CacheId cacheid) {
      Tr.error(tc, "DYNA0039E");
   }

   protected String[] processInvalidationGenerator(Invalidation invalidation) {  
       if (tc.isDebugEnabled()) {
           Tr.debug(tc, "Commands do not support custom invalidation generating classes.");
       }
       return null;
   }
   protected Object processMethod(Method m, Object o) {
		  if (o == null)
		     return null;
		     try {
		        m.methodImpl = o.getClass().getMethod(m.name, new Class[]{});
		     } catch (Exception ex) {
		        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheProcessor.processMethod", "275", this);
		        Tr.error(tc, "DYNA0033E", new Object[] { m.name, o.getClass().getName(), ex.getMessage()});
		        if (tc.isDebugEnabled())
		           Tr.debug(tc, "Exception", ex);
		     }
	      Object result = null;
	      try {
	         result = m.methodImpl.invoke(o, emptyArgs);
	      } catch (RuntimeException rex) {
	         // may be caused by reload... try to get new class version
	      	 //com.ibm.ws.ffdc.FFDCFilter.processException(rex, "com.ibm.ws.cache.config.CacheProcessor.processMethod", "284", this);
	      	if (tc.isDebugEnabled())
	            Tr.debug(tc, "Exception", rex);
	      	try {
	      	
	      		CacheableCommand  cc = 	(CacheableCommand)o;
	      	    byte b[] = SerializationUtility.serialize(cc);
	                o = SerializationUtility.deserialize(b, configEntry.instanceName);
	            m.methodImpl = o.getClass().getMethod(m.name, new Class[]{});
	            result = m.methodImpl.invoke(o, emptyArgs);	       
	      	 } catch (Exception ex) {
	            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheProcessor.processMethod", "284", this);
	            Tr.error(tc, "DYNA0034E", new Object[] { m.name, o.getClass().getName(), ex.getMessage()});
	            if (tc.isDebugEnabled())
	               Tr.debug(tc, "Exception", ex);
	         }
	      } catch (Exception ex) {
	         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheProcessor.processMethod", "284", this);
	         Tr.error(tc, "DYNA0034E", new Object[] { m.name, o.getClass().getName(), ex.getMessage()});
	         if (tc.isDebugEnabled())
	            Tr.debug(tc, "Exception", ex);
	      }
	      if (m.method != null)
	         result = processMethod(m.method, result);
	      else if (m.field != null)
	         result = processField(m.field, result);
	      if (m.index > -1) {
	         if (result instanceof Collection) { 
	            result = ((Collection) result).toArray();
	         }
	         if (result instanceof Object[]) {
	            if (((Object[]) result).length > m.index)
	               result = ((Object[]) result)[m.index];
	         }
	      }
	      return result;
	   }

	   protected Object processField(Field f, Object o) {
	      if (o == null)
	         return null;
	      if (f.fieldImpl == null) {
	         try {
	            f.fieldImpl = o.getClass().getField(f.name);
	         } catch (Exception ex) {
	            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheProcessor.processField", "301", this);
	            Tr.error(tc, "DYNA0035E", new Object[] { f.name, o.getClass().getName(), ex.getMessage()});
	            if (tc.isDebugEnabled())
	               Tr.debug(tc, "Exception", ex);
	         }
	      }
	      Object result = null;
	      try {
	         result = f.fieldImpl.get(o);
	      } catch (Exception ex) {
	         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheProcessor.processField", "310", this);
	         Tr.error(tc, "DYNA0036E", new Object[] { f.name, o.getClass().getName(), ex.getMessage()});
	         if (tc.isDebugEnabled())
	            Tr.debug(tc, "Exception", ex);
	      }
	      if (f.method != null)
	         result = processMethod(f.method, result);
	      else if (f.field != null)
	         result = processField(f.field, result);
	      if (f.index > -1) {
	         if (result instanceof Collection) {
	            result = ((Collection) result).toArray();
	         }
	         if (result instanceof Object[]) {
	            if (((Object[]) result).length > f.index)
	               result = ((Object[]) result)[f.index];
	         }
	      }
	      return result;
	   }

}
