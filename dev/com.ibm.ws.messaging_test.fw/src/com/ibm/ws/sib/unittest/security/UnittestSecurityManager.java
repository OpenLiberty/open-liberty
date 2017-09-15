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
package com.ibm.ws.sib.unittest.security;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>A security manager for use in unit tests. This security manager works in
 *   the same way as the default security manager supplied with the JVM, except
 *   in one important way. When a class that does not have authority is 
 *   encountered the security manager will look at the class making the call
 *   to see if it is annotated with the permission. If it is permission is 
 *   granted. Also it makes use of an annotation that can be used to insert
 *   a doPrivileged call around a method.
 * </p>
 *
 * <p>SIB build component: sib.unittest.security</p>
 *
 * @author nottinga
 * @version 1.2
 * @since 1.0
 */
public class UnittestSecurityManager extends SecurityManager
{
  /** A cache of the permissions a class has, read in from the class's annotation */
  private static ConcurrentMap<String, Permissions> _annotatedClassPermissionCache = new ConcurrentHashMap<String, Permissions>();
  /** A cache of the permissions a method has, read in from the method's annotation */
  private static ConcurrentMap<MethodKey, MethodPermissions> _annotatedMethodPermissionCache = new ConcurrentHashMap<MethodKey, MethodPermissions>();
  /** A cache of protection domains associated with classes */
  private static ConcurrentMap<Class<?>, ProtectionDomain> _cacheOfProtectionDomains = new ConcurrentHashMap<Class<?>, ProtectionDomain>();
  /** A thread local which if set to false causes an early return from the 
   *  checkPermission method (used when a call made by the security manager
   *  causes another check to occur). */
  private static ThreadLocal<Boolean> _shouldPerformCheck = new ThreadLocal<Boolean>()
  {
    public Boolean initialValue()
    {
      return true;
    }
  };
  
  /**
   * <p>This class holds information loaded from the Permission annotation of
   *   a method. Made static as it does not access any methods or
   *   variables in its encasing class.
   * </p>
   */
  private static class MethodPermissions
  {
    /** The permissions laoded from the method */
    private Permissions _perms;
    /** If the method was annotated with @DoPriviledged */
    private boolean _stopIfAllowed;
    
    /* ---------------------------------------------------------------------- */
    /* MethodPermissions method                                    
    /* ---------------------------------------------------------------------- */
    /**
     * The constructor.
     * 
     * @param perms         the permissions
     * @param stopIfAllowed if a pass here should cause the rest of the stack 
     *                       to be checked.
     */
    public MethodPermissions(Permissions perms, boolean stopIfAllowed)
    {
      _perms = perms;
      _stopIfAllowed = stopIfAllowed;
    }
    
    /* ---------------------------------------------------------------------- */
    /* implies method                                    
    /* ---------------------------------------------------------------------- */
    /**
     * This method works out if the permission passed in is implied by the 
     * permissions the method has.
     * 
     * @param perm the permission to check.
     * @return true if the method has the permission, false otherwise.
     */
    private boolean implies(Permission perm)
    {
      if (_perms == null) return false;
      return _perms.implies(perm);
    }
    
    /* ---------------------------------------------------------------------- */
    /* isMarkedDoPriviledged method                                    
    /* ---------------------------------------------------------------------- */
    /**
     * @return true if checking should stop at this point.
     */
    private boolean isMarkedDoPriviledged()
    {
      return _stopIfAllowed;
    }
    
    /* ---------------------------------------------------------------------- */
    /* add method                                    
    /* ---------------------------------------------------------------------- */
    /**
     * Adds a new permission to the methods permissions.
     * 
     * @param perm
     */
    public void add(Permissions perm)
    {
      Enumeration<Permission> thePerms = perm.elements();
      while (thePerms.hasMoreElements())
        _perms.add(thePerms.nextElement());
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* checkPermission method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method works out if a check should be run. If the check should be 
   * skipped the method exits immediately. If it should be run it marks future
   * calls to checkPermission to be skipped and then calls the 
   * internalCheckPermission method. When internalCheckPermission exists it
   * is guaranteed to mark future calls to checkPermission as needing work done.
   * This is a performance optimisation.
   * 
   * @see java.lang.SecurityManager#checkPermission(java.security.Permission)
   */
  @Override
  public void checkPermission(Permission perm)
  {
    if (_shouldPerformCheck.get()) 
    {
      try
      {
        _shouldPerformCheck.set(false);
        internalCheckPermission(perm);
      }
      finally
      {
        _shouldPerformCheck.set(true);
      }
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* internalCheckPermission method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method processes the permission check.
   * 
   * @param perm the permission to check.
   */
  private void internalCheckPermission(Permission perm)
  {
    StackTraceElement[] callStack = null;
    
    // get the classes on the stack
    Class<?>[] classStack = getClassContext();

    // for each class on the stack
    for (int i = 0, j = 2; i < classStack.length && j > 0; i++)
    {
      // get the class name
      String className = classStack[i].getName();

      // see if the class name is one we have been configured to ignore.
      if (matches(className)) return;
      
      // get the ProtectionDomain from the cache
      ProtectionDomain domain = _cacheOfProtectionDomains.get(classStack[i]);
      // if we got a cache miss grab it from the cache and ensure it is there next time 
      if (domain == null)
      {
  	    domain = classStack[i].getProtectionDomain();
      	_cacheOfProtectionDomains.putIfAbsent(classStack[i], domain);
      }
      
      // work out if the domain allows the permission.
      boolean allowed = domain.implies(perm);
      
      if (!allowed)
      {
        // if the domain doesn't have permission get hold of the annotated
        // permissions from the class.
        Permissions annotatedPermissions = getAnnotatedPermissions(classStack[i]);
        
        // if we found annotated permissions see if we are allowed access.
        if (annotatedPermissions != null)
        {
          allowed = annotatedPermissions.implies(perm);
        }
        
        if (!allowed)
        {
          // if we still do not have access we look at the method annotations.
          
          // if the call stack has not been obtained get it.
          if (callStack == null) callStack = Thread.currentThread().getStackTrace();
          
          // create a method key for the method on the stack at the point we are checking.
          MethodKey mapLookup = new MethodKey(className, callStack[i+2].getMethodName());

          // get the method permissions
          MethodPermissions methodPermissions = getAnnotatedPermissions(classStack[i], mapLookup);
          
          if (methodPermissions != null)
          {
            // if we have the permissions check to see if we are allowed
            allowed = methodPermissions.implies(perm);
            
            // if we are allowed and the method has the DoPriviledged marker
            // we stop at this point.
            if (allowed && methodPermissions.isMarkedDoPriviledged()) break;
          }
        }
        
        if (!allowed)
        {
          // if we are not allowed at this point we do some debug and throw an
          // AccessControlException.
          if (callStack == null) callStack = Thread.currentThread().getStackTrace();
          
          debugFailure(perm, callStack[i+2], callStack, domain);
          
          throw new AccessControlException("failed " + perm);
        }
      }
      
      // if the class is java.security.AccessController then we check the
      // caller and if it has access exit. We do this by decrementing j
      if ("java.security.AccessController" == className)
      {
        j--;
      }
      // if the j is 1 we decrement again which will casue the loop to exit.
      else if (j == 1)
      {
        j--;
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* matches method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method checks for some well known classes that should always be 
   * treated as priviledged code. It returns true if the className passes in is
   * one of the priviledged classes.
   * 
   * @param className the name of the class.
   * @return          true if it matches, false otherwise.
   */
  private boolean matches(String className)
  {
    if ("com.ibm.js.test.LoggingTestCase" == className) return true;
    if ("com.ibm.websphere.ws.sib.unittest.ras.Trace" == className) return true;
    if ("com.ibm.ws.sib.utils.ras.SibTr" == className) return true;
    if ("com.ibm.ejs.ras.Tr" == className) return true;
    if ("com.ibm.ws.sib.utils.ras.SibTr$Suppressor" == className) return true;
    if ("com.ibm.ws.sib.utils.SIBUuidLength" == className) return true;
    if ("com.ibm.ws.sib.mediation.ut.UsurpingClassLoader" == className) return true;
    if ("org.eclipse.core.internal.runtime.Messages" == className) return true;
    return false;
  }

  /* ------------------------------------------------------------------------ */
  /* getAnnotatedPermissions method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method gets the method permissions from the method either by getting
   * it from the cache, or by loading the data anew.
   * 
   * @param class1    the class to introspect.
   * @param methodKey the method key for the method.
   * @return          the appropriate MethodPermissions.
   */
  private MethodPermissions getAnnotatedPermissions(Class<?> class1, MethodKey methodKey)
  {
    // we need to synchronize on the class passed in because this logic does
    // not cope with the MethodPermissions being half filled in.
    synchronized (class1)
    {
      MethodPermissions testPermissions = _annotatedMethodPermissionCache.get(methodKey);
      
      if (testPermissions == null)
      {
        String clazzName = class1.getName();
        Method[] m = class1.getDeclaredMethods();
        
        for (int i = 0; i < m.length; i++)
        {
          boolean stopIfAllowed = m[i].isAnnotationPresent(DoPrivileged.class);
          
          com.ibm.ws.sib.unittest.security.Permission permissionAnnotation = m[i].getAnnotation(com.ibm.ws.sib.unittest.security.Permission.class);
          
          Permissions perms = extractPermission(permissionAnnotation);
          
          MethodKey thisMethodKey = new MethodKey(clazzName, m[i].getName());
          
          MethodPermissions methodPerms = _annotatedMethodPermissionCache.get(thisMethodKey);
          if (methodPerms == null)
          {
            methodPerms = new MethodPermissions(perms, stopIfAllowed);
            _annotatedMethodPermissionCache.putIfAbsent(thisMethodKey, methodPerms);
            
            if (thisMethodKey.equals(methodKey)) testPermissions = methodPerms;
          }
          else
          {
            methodPerms.add(perms);
          }
        }
      }
      
      return testPermissions;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* getAnnotatedPermissions method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method gets the Permissions from the annotation on a class.
   * 
   * @param clazz the class to look for the annotation on.
   * @return      the Permissions found.
   */
  private Permissions getAnnotatedPermissions(Class<?> clazz)
  {
    Permissions testPermissions = _annotatedClassPermissionCache.get(clazz.getName());
    
    if (testPermissions == null)
    {
      com.ibm.ws.sib.unittest.security.Permission permission = clazz.getAnnotation(com.ibm.ws.sib.unittest.security.Permission.class);
  
      testPermissions = extractPermission(permission);
      
      _annotatedClassPermissionCache.putIfAbsent(clazz.getName(), testPermissions);
    }
    
    return testPermissions;
  }

  /* ------------------------------------------------------------------------ */
  /* extractPermission method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method extracts from a Permission annotation the Permissions granted
   * in the annotation.
   * 
   * @param permission the Permission annotation to process.
   * @return           the extracted Permissions.
   */
  private Permissions extractPermission(com.ibm.ws.sib.unittest.security.Permission permission)
  {
    Permissions perms = new Permissions();
    if (permission != null)
    {
      String[] grantedPermissions = permission.value();
      
      if (grantedPermissions != null)
      {
        for (String aPermission : grantedPermissions)
        {
          perms.add(extractPermission(aPermission));
        }
      }
    }
    return perms;
  }

  /* ------------------------------------------------------------------------ */
  /* debugFailure method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method prints a useful debug message to System.err when a permission
   * check fails, this is to make debug easier.
   * 
   * TODO we should make this reflect the annottated permissions in addition to the ProtectionDomain ones.
   * 
   * @param perm      the permission that was being checked when it failed.
   * @param element   the element in the stack trace when the failure occured.
   * @param callStack the full stack trace.
   * @param domain    the ProtectionDomain.
   */
  private void debugFailure(Permission perm, StackTraceElement element, StackTraceElement[] callStack, ProtectionDomain domain)
  {
    for (int i = 2; i < 80; i++) System.err.print('=');

    System.err.println();
    System.err.println("Code: " + element + " loaded from " + domain.getCodeSource());
    System.err.println("does not have permission: " + perm);
    System.err.println();
    System.err.println("Permissions the code has: " + domain.getPermissions());
    System.err.println();
    System.err.println("Code stack at the point of failure:");
    
    for (int j = 0; j < callStack.length; j++)
    {
      System.err.println(callStack[j].toString());
    }
    

    for (int i = 0; i < 80; i++) System.err.print('=');
    System.err.println();
  }
  
  /* ------------------------------------------------------------------------ */
  /* extractPermission method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method takes a String and converts that to a java Permission. The 
   * String takes the format from the java.policy file which is:
   * 
   *   <Java Permission class name> [<resource>] [<action>];
   * 
   * @param permText the text.
   * @return         the permission.
   */
  @SuppressWarnings("unchecked")
  public static Permission extractPermission(String permText)
  {
    StreamTokenizer tokens = new StreamTokenizer(new StringReader(permText));
    
    String className = null;
    String resourceName = "";
    String actions = "";
    
    try
    {
      tokens.nextToken();
      className = getStringToken(tokens);
      if (tokens.nextToken() != StreamTokenizer.TT_EOF)
      {
        resourceName = getStringToken(tokens);
      }
      
      if (tokens.nextToken() != StreamTokenizer.TT_EOF)
      {
        actions = getStringToken(tokens);
      }
      
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }

    try
    {
      Class<? extends Permission> permClazz = (Class<? extends Permission>)Class.forName(className);
      Constructor<? extends Permission>  cons = permClazz.getConstructor(String.class, String.class);
      return cons.newInstance(resourceName, actions);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
      
    return null;
  }
  
  /* ------------------------------------------------------------------------ */
  /* getStringToken method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Gets a String token from a StreamTokenizer. It copes with the tokens being
   * either a word or are String literals.
   * 
   * @param tokenizer    the StreamTokenizer.
   * @return             the token.
   * @throws IOException if a failure occurs reading from the StreamTokenizer.
   */
  private static String getStringToken(StreamTokenizer tokenizer) throws IOException
  {
    if (tokenizer.ttype == StreamTokenizer.TT_WORD)
    {
      return tokenizer.sval;
    }
    else if (tokenizer.ttype == '\"')
    {
      return tokenizer.sval;
    }
    else
    {
      throw new IOException("Unexpected token type. Expected a word on line " + tokenizer.lineno() + " but got " + (char)tokenizer.ttype);
    }
  }
}