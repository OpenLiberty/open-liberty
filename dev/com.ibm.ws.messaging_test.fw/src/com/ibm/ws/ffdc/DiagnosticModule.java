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
package com.ibm.ws.ffdc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* ************************************************************************** */
/**
 * A Mock/Dummy DiagnosticModule for use when unit testing
 */
/* ************************************************************************** */
public class DiagnosticModule
{
  // Dummy class implementation - if you want any more of the methods from the
  // real DiagnosticModule, please let me know or add them yourself :-)
  String[]                     defaultDirectives;
  Map<String,Method>           methodTable      = new HashMap<String, Method>();

  //Signature of ffdcDump methods must fit the signature below: Exception,
  // IncidentStream, Object[], String
  static final java.lang.Class ffdcDumpParams[] = { Throwable.class,
      IncidentStream.class, Object.class, (new Object[0]).getClass(),
      String.class                             };

  static final String          ffdcDumpPrefix   = "ffdcdump";
  static final String          ffdcDumpDefault  = "default";
  static final boolean         debug            = false;
  static final int             PREFLEN          = ffdcDumpPrefix.length();

  //This method is called when the Diagnostic Module is registered with FFDC
  protected final void init()
  {
    Method[] methods = null;

    try
    {
      methods = getClass().getMethods();
      buildMethods(methods);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Method can be used as a simple validation of a components diagnostic
   * module. The information printed can be used during the development of the
   * DM.
   *
   * @return Return used to indicate if the DM looked like it was in good shape.
   */
  public boolean validate()
  {
    System.out.println("This method is NOT intended to be called from the runtime");
    System.out.println("but is provided as part of unit test for diagnostic modules");
    try
    {
      init();
    }
    catch(Throwable th)
    {
      System.out.println("Some unknown failure occured");
      th.printStackTrace();
      return false;
    }
    System.out.println("ffdc directives on this diagnostic module : ");

    for(String directive : methodTable.keySet())
    {
      System.out.println("    " + directive);
    }

    System.out.println("The default directives are : ");
    if (defaultDirectives == null || defaultDirectives.length == 0)
    {
      System.out.println("No default directives were found");
      System.out.println("This diagnostic module would fail to register");
      return false;
    }
    else
    {
      for(String directive : defaultDirectives)
      {
        if (directive == null)
        {
          break;
        }
        System.out.println("    " + directive);
      }
    }
    return true;
  }

  //Called by init() to build a table of methods which comprise the default
  // dump methods and the directives supported
  //by the component.
  void buildMethods(Method[] methods)
  {
    for(Method method : methods)
    {
      String name = method.getName().toLowerCase();
      if (name.length() > PREFLEN && name.startsWith(ffdcDumpPrefix))
      {
        Class[] params = method.getParameterTypes();

        if (params.length != ffdcDumpParams.length)
        {
          throw new IllegalStateException("Error: "
                                          + method
                                          + " starts with "
                                          + ffdcDumpPrefix
                                          + " but takes "
                                          + params.length
                                          + " parameters.  It is supposed to have "
                                          + ffdcDumpParams.length
                                          + " parameters and have the signature "
                                          + ffdcDumpPrefix
                                          + "<....>("
                                          + buildParamList()
                                          + ");"
                                          + " Method skipped!");
          // continue;
        }

        boolean error = false;
        for(int j = 0; j < params.length; j++)
        {
          if (params[j] != ffdcDumpParams[j])
          {
            throw new IllegalStateException("Error: "
                                            + method
                                            + " starts with "
                                            + ffdcDumpPrefix
                                            + " but does not conform to the signature\n\t"
                                            + ffdcDumpPrefix
                                            + "<....>("
                                            + buildParamList()
                                            + "); \n\tParameter "
                                            + (j + 1)
                                            + " does not match.  Method skipped!");
            // error = true;
            // break;
          }

        } // iterate thru parameters

        if (error) continue;

        // We get here when the method is a cool ffdcDump method that meets the
        // signature. We can now safely add it to the list
        methodTable.put(name.substring(PREFLEN), method);

      } // if this is a ffdcDump<...> method
    } // iterate thru all methods

    // Now that all methods are in the properties object complete the list of
    // directives.
    List<String> defaultv = new ArrayList<String>();

    for(String directive : methodTable.keySet())
    {
      if (directive.startsWith(ffdcDumpDefault))
        defaultv.add(directive);
    }

    // Make sure that there is at least 1 default dumping method.
    if (defaultv.isEmpty())
        throw new IllegalStateException("Error: class "
                                        + getClass()
                                            .getName()
                                        + " must have at least one DEFAULT dumping method of the form:\n\t"
                                        + ffdcDumpPrefix
                                        + ffdcDumpDefault
                                        + "<....>("
                                        + buildParamList()
                                        + ");");

    // Convert vector of default dump method names to a string array.
    defaultDirectives = defaultv.toArray(new String[0]);

  }

  //Helper to show a method signature - compiler style - in case a diagnostic module has a method with a bad signature.
  String buildParamList()
  {
   StringBuffer sb = new StringBuffer();

   for (int j = 0; j < ffdcDumpParams.length; j++)
    {
     sb.append(getTypeName(ffdcDumpParams[j]));
     if (j < (ffdcDumpParams.length - 1))
      sb.append(",");
   }
   return sb.toString();
  }

  //Helper for the Helper to show class/type name and handle array cases e.g. xyz(String [][][], etc...)
  static String getTypeName(Class type)
  {
   if(type.isArray())
    {
     try
      {
       Class cl = type;
       int dimensions = 0;
       while (cl.isArray())
        {
         dimensions++;
         cl = cl.getComponentType();
        }
       StringBuffer sb = new StringBuffer();
       sb.append(cl.getName());
       for (int i = 0; i < dimensions; i++)
        sb.append("[]");
       return sb.toString();
      }
     catch(Throwable e)
      { /*FALLTHRU*/ }
    }
   return type.getName();
  }

  /* -------------------------------------------------------------------------- */
  /* dumpComponentData method
  /* -------------------------------------------------------------------------- */
  /**
   * This method is invoked to instruct the diagnostic module to capture all
   * relevant information that it has about a particular incident
   *
   * @param input_directives The directives to be processed for this incident
   *        (ignored for this implementation, the default directive is always applied)
   * @param ex               The exception that caused this incident
   * @param ffdcis           The incident stream to be used to record the relevant information
   * @param callerThis       The object reporting the incident
   * @param catcherObjects   Additional objects that might be involved
   * @param sourceId         The source id of the class reporting the incident
   * @param callStack        The list of classes on the stack
   * @return true if more diagnostic modules should be invoked after this one
   */
  public boolean dumpComponentData(String[] input_directives, Throwable ex, IncidentStream ffdcis,
                                   Object callerThis, Object[] catcherObjects, String sourceId, String [] callStack)
  {
    try
    {
      ffdcis.writeLine("==> Performing default dump from "+getClass().getName()+" ",new Date());
      for(String directive : defaultDirectives)
      {
        try
        {
          methodTable.get(directive).invoke(this,new Object[] {ex,ffdcis,callerThis,catcherObjects,sourceId});
          ffdcis.writeLine("+Data for directive ["+directive+"] obtained.","");
        }
        catch(Throwable t)
        {
          // No FFDC code needed - we're in the middle of FFDC'ing!
          ffdcis.writeLine("Error while processing directive ["+directive+"] !!!","");
        }
      }
      ffdcis.writeLine("==> Dump complete for "+getClass().getName()+" ",new Date());
    }
    catch(Throwable th)
    {
      // No FFDC code needed - we're in the middle of FFDC'ing!
      ffdcis.writeLine("==> Dump did not complete for "+getClass().getName()+" ",new Date());
    }

    return true;
  }

}
