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
package com.ibm.ws.sib.utils.ffdc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.Traceable;
import com.ibm.ws.ffdc.DiagnosticModule;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.sib.utils.ras.SibTr;

/* ************************************************************************** */
/**
 * This class should be the root class for all of Jetstream's diagnostic modules.
 * It provides two methods for use by the subclasses:
 * <dl>
 * <dt>register(SibDiagnosticModule,String[])
 * <dd>This registers the diagnostic module provided. It registers the module for the
 * specified list of packages and registers a singleton instance for the Jetstream
 * packages:
 * <ul>
 * <li>com.ibm.ws.sib
 * <li>com.ibm.wsspi.sib
 * <li>com.ibm.websphere.sib
 * </ul>
 * <p>When searching for a diagnostic module, the FFDC code searches for the
 * most specific package first. Hence the subclass's diagnostic module will
 * be invoked for the classes registered by the subclass and the default
 * provided by this class will be used for all other Jetstream code
 * <dt>captureDefaultInformation(IncidentStream)
 * <dd>This should be called by the subclass to allow capture of the general
 * Jetstream information (such as engine name etc) before proceeding with the
 * capture of the more specific diagnostic information.
 * </dl>
 *
 */
/* ************************************************************************** */
public class SibDiagnosticModule extends DiagnosticModule
{
  /** The trace component for this class */
  private static TraceComponent _tc = SibTr.register(SibDiagnosticModule.class, "", null);
 
  /** A fragment of a class name that indicates that it is a sib class */
  private static final String SIB_PACKAGE_NAME = ".sib.";

  /* ************************************************************************** */
  /**
   * This PrivilegedOperation allows us to obtain the SCCSID information
   * It needs to be privileged so that it can ensure it can get access to the
   * field even if it is private!
   *
  /* ************************************************************************** 
  private static final class GetClassSccsidPrivilegedOperation implements PrivilegedExceptionAction
  {

    private final String _className;
    private GetClassSccsidPrivilegedOperation(String className)
    {
      this._className = className;
    }

    /* -------------------------------------------------------------------------- */
    /* run method
    /* -------------------------------------------------------------------------- */
    /**
     * Return the SCCSID of the class named when this Privileged Operation was constructed.
     *
     * @see java.security.PrivilegedExceptionAction#run()
     * @return The SCCSID
     * @throws Exception
     
    public Object run() throws Exception
    {
      return ClassUtil.getSccsId(_className);
    }
  }*/
  /** Have we registered the main diagnostic module? */
  private static boolean _registeredMainDiagnosticModule = false;

  /** The list of packages we register as the global SIB packages */
  private static final String[] SIB_PACKAGE_LIST = { "com.ibm.ws.sib", "com.ibm.wsspi.sib", "com.ibm.websphere.sib" };


  /** Helper for getting line separator on the executing platform */                        //255802
  protected static final String lineSeparator = System.lineSeparator();

  /** In Collections, Maps and Arrays - how many objects to trace */                        //255802
  protected int multiple_object_count_to_ffdc = 20;                                         //255802

  /* -------------------------------------------------------------------------- */
  /* SibDiagnosticModule null constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new SibDiagnosticModule.
   *
   */
  /* -------------------------------------------------------------------------- */
  protected SibDiagnosticModule()
  {
     // Null constructor (which, incidentally, ensures we must have a subclass
     // somewhere that instantiates this class
  }

  /* -------------------------------------------------------------------------- */
  /* register method
  /* -------------------------------------------------------------------------- */
  /**
   * Register a subclass of this diagnostic module with FFDC
   *
   * @param specificPackageList The package list for which it is being registered
   */
  protected void register(String[] specificPackageList)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(_tc, "register", new Object[] { this, specificPackageList });

    synchronized(SibDiagnosticModule.class)
    {
      if (!_registeredMainDiagnosticModule)
      {
        SibDiagnosticModule mainModule = new SibDiagnosticModule();
        mainModule.registerModule(SIB_PACKAGE_LIST);
        _registeredMainDiagnosticModule = true;
      }
    }

    this.registerModule(specificPackageList);

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(_tc, "register");
  }

  /* -------------------------------------------------------------------------- */
  /* captureDefaultInformation method
  /* -------------------------------------------------------------------------- */
  /**
   * Capture the default information about the messaging engine etc.
   *
   * @param is The incident stream into which to place the information
   */
  protected void captureDefaultInformation(IncidentStream is)
  {
    captureDefaultInformation(is,null);                                       //255802
  }

  //Method added by defect 255802
  /**
   * Capture the default information about the messaging engine, exception etc.
   *
   * @param is  The incident stream into which to place the information
   * @param th  The Throwable that caused the FFDC (may be null)
   */
  protected void captureDefaultInformation(IncidentStream is, Throwable th)
  {
    is.writeLine("Platform Messaging :: Messaging engine:", SibTr.getMEName(null));
 //   is.writeLine("Platform Messaging :: Release name:    ", BuildInfo.getBuildRelease());
 //   is.writeLine("Platform Messaging :: Level name:      ", BuildInfo.getBuildLevel());

    if (th != null)
    {
      StackTraceElement[] ste = th.getStackTrace();

      Set classes = new HashSet();
      for (int i = 0; i < ste.length; i++)
      {
        final StackTraceElement elem = ste[i];

        try
        {
          String className = elem.getClassName();

          // We only care about .sib. classes
          if (className.indexOf(SIB_PACKAGE_NAME)>=0)
          {
            if (!classes.contains(className))
            {
            	
            	//Kavitha - commenting out as ClassUtil removed
          /*    String sccid = (String)AccessController.doPrivileged(new GetClassSccsidPrivilegedOperation(className)) ;
              if (sccid != null)
              {
                is.writeLine(className, sccid);
              }*/
              classes.add(className);
            }
          }
        }
        catch (Exception exception)
        {
          // No FFDC code needed
        }
      }
    }
  }

  /* -------------------------------------------------------------------------- */
  /* registerModule method
  /* -------------------------------------------------------------------------- */
  /**
   * Register an instance of this class with FFDC.
   *
   * @param packageList         The list of packages for which to register the module
   */
  /* -------------------------------------------------------------------------- */
  private void registerModule(String[] packageList)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(_tc, "registerModule", new Object[] { this, packageList });

    for (int i = 0, rc = 0;(rc == 0) && (i < packageList.length); i++)
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) SibTr.debug(_tc, "Registering diagnostic module " + this +" for package: " + packageList[i]);

      rc = FFDC.registerDiagnosticModule(this, packageList[i]);

      if (rc != 0)
      {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) SibTr.debug(_tc, "Registration failed, return code=" + rc);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(_tc, "registerModule");
  }

  /* -------------------------------------------------------------------------- */
  /* ffdcDumpDefault method
  /* -------------------------------------------------------------------------- */
  /**
   * Capture information about this problem into the incidentStream
   *
    * @param t          The exception which triggered the FFDC capture process.
    * @param is         The IncidentStream. Data to be captured is written to this stream.
    * @param callerThis The 'this' pointer for the object which invoked the filter. The value
    *                   will be null if the method which invoked the filter was static, or if
    *                   the method which invoked the filter does not correspond to the DM
    *                   being invoked.
    * @param objs       The value of the array may be null. If not null, it contains an array of
    *                   objects which the caller to the filter provided. Since the information in
    *                   the array may vary depending upon the location in the code, the first
    *                   index of the array may contain hints as to the content of the rest of the
    *                   array.
    * @param sourceId   The sourceId passed to the filter.
   */
  public void ffdcDumpDefault(Throwable t, IncidentStream is, Object callerThis, Object[] objs, String sourceId)
  {
    is.writeLine("SIB FFDC dump for:", t);

    captureDefaultInformation(is,t);

    if (callerThis != null)
    {
      is.writeLine("SibDiagnosticModule :: Dump of callerThis (DiagnosticModule)", toFFDCString(callerThis)); //255802
      is.introspectAndWriteLine("Introspection of callerThis:", callerThis);            //255802
    }

    if (objs != null)
    {
      for (int i = 0; i < objs.length; i++)
      {
        is.writeLine("callerArg (DiagnosticModule) [" + i + "]",                        //255802
                toFFDCString(objs[i]));                                                 //287897
        is.introspectAndWriteLine("callerArg [" + i + "] (Introspection)", objs[i]);    //255802
      }
    }

  }

  //Method added by defect 255802
  /**
   * Generates a string representation of the object for FFDC.
   * If the object is an Object Array, Collection or Map the
   * elements are inspected individually up to a maximum of
   * multiple_object_count_to_ffdc
   *
   * @param obj
   *      Object to generate a string representation of
   * @return
   *      The string representation of the object
   */
  public final String toFFDCString(Object obj)
  {
      if (obj instanceof Map)
      {
        return toFFDCString((Map) obj);
      }
      else if (obj instanceof Collection)
      {
        return toFFDCString((Collection) obj);
      }
      else if (obj instanceof Object[])
      {
        return toFFDCString((Object[]) obj);
      }
      return toFFDCStringSingleObject(obj);
  }

  //Method added by defect 255802
  /**
   * Generates a string representation of an object for FFDC.
   *
   * @param obj
   *      Object to generate a string representation of
   * @return
   *      The string representation of the object
   */
  protected String toFFDCStringSingleObject(Object obj)
  {
    if (obj == null)
    {
      return "<null>";
    }
    else if (obj instanceof Traceable)
    {
      return ((Traceable) obj).toTraceString();
    }
    else if (obj instanceof String)
    {
      return ((String) obj);
    }
    else if (obj instanceof byte[])
    {
      return toFFDCString((byte[]) obj);
    }
    else
    {
      return obj.toString();
    }
  }

  //Method added by defect 255802
  /**
   * Generates a string representation of a byte[].
   *
   * If the byte[] contains non-printable characters a hex dump
   * is produced otherwise a String will be produced
   *
   * @param toDump
   *      byte[] to generate a string representation of
   * @return
   *      The string representation of the object
   */
  protected final String toFFDCString(byte[] toDump)
  {
    String result;
    boolean text = true;
    for (int i = 0; i < toDump.length; i++)
    {
      byte b = toDump[i];
      if ((b >= 0x20 && b <= 0x7e))
      {
        text = true;
      }
      else
      {
        text = false;
        break;
      }
    }

    if (text)
    {
      result = new String(toDump);
    }
    else
    {
      result = toFFDCString(toDump, 0, toDump.length);
    }

    return result;
  }

  //Method added by defect 255802
  /**
   * Generates a string representation of a byte[] from an offset for a length
   *
   * @param toDump
   *        byte[] to generate a String representation of
   * @param start
   *        Start offset
   * @param length
   *        Length to dump
   * @return the string representation of the byte array.
   */
  protected final String toFFDCString(byte[] toDump, int start, int length)
  {
    StringBuffer result = new StringBuffer();
    for (int lineStart = 0; lineStart < length; lineStart += 32)
    {
      int lineEnd = Math.min(lineStart + 32, length);
      StringBuffer hex = new StringBuffer();
      StringBuffer ascii = new StringBuffer();
      for (int i = lineStart; i < lineEnd; i++)
      {
        int b = toDump[start + i];
        b = (b + 256) % 256; // Make sure bytes become unsigned

        // Add b to the hex StringBuffer
        int c1 = b / 16;
        int c2 = b % 16;

        hex.append((char) (c1 < 10 ? '0' + c1 : 'a' + c1 - 10));
        hex.append((char) (c2 < 10 ? '0' + c2 : 'a' + c2 - 10));

        // Add b to the ascii StringBuffer, if printable
        if ((b >= 0x20 && b <= 0x7e))
          ascii.append((char) b);
        else
          ascii.append('.');
        if (i % 32 == 15)
        {
          hex.append("  ");
            ascii.append("  ");
        }
      }

      // We need to pad the hex string with sufficient spaces
      // to align the ascii string nicely.
      int pad = 32 - (lineEnd - lineStart);

      // This formula aligns to the end of the hex block, plus a
      // trailing space. Honest.
      for (int i = 0; i < pad; i++)
      {
        hex.append("  ");
        if (i % 32 == 16)
        {
          hex.append("  ");
        }
      }

      hex.append("   ");

      // Before we output the line, output the offset of the
      // start of the line.
      String offset = "0000" + Integer.toHexString(lineStart + start);
      offset = offset.substring(offset.length() - 4);
      result.append(offset);
      result.append(":  ");

      // Finally, output the line
      result.append(hex.toString());
      result.append(ascii.toString());
      result.append(lineSeparator);
    }

    return result.toString();
  }

  //Method added by defect 255802
  /**
   * Generates a String representation of a Collection, calling toFFDCStringObject for the first multiple_object_count_to_ffdc
   * elements
   *
   *  @param aCollection
   * @return the string representation of a Collection.
   */
  public final String toFFDCString(Collection aCollection)
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('{');
    if (aCollection == null)
    {
      buffer.append("<null>");
    }
    else
    {
      Iterator i = aCollection.iterator();
      boolean hasNext = i.hasNext();
      int ctr = 0;
      while (hasNext)
      {
        Object value = i.next();
        buffer.append((value == aCollection ? "<this list>" : toFFDCStringSingleObject(value)) + lineSeparator);

        hasNext = i.hasNext();
        if (ctr > multiple_object_count_to_ffdc)
        {
          buffer.append("........contd");
          hasNext = false;
        }
      }
    }

    buffer.append('}');

    return buffer.toString();
  }

  //Method added by defect 255802
  /**
   * Generates a String representation of a Object[], calling toFFDCStringObject for the first multiple_object_count_to_ffdc
   * elements
   *
   * @param objects
   * @return the string representation of the Object[]
   */
  public final String toFFDCString(Object[] objects)
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('{');
    if (objects == null)
    {
      buffer.append("<null>");
    }
    else
    {
      for (int i = 0; i < objects.length; i++)
      {
        Object value = objects[i];
        buffer.append((value == objects ? "<this array>" : toFFDCStringSingleObject(value)) + lineSeparator);

        if (i > multiple_object_count_to_ffdc)
        {
          buffer.append("........contd");
          break;
        }
      }
    }
    buffer.append('}');

    return buffer.toString();
  }

  //Method added by defect 255802
  /**
   * Generates a String representation of a Map, calling toFFDCStringObject for the first multiple_object_count_to_ffdc
   * elements
   *
   * @param aMap
   * @return the string representation of the map.
   */
  public final String toFFDCString(Map aMap)
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('{');
    if (aMap == null)
    {
      buffer.append("<null>");
    }
    else
    {
      Iterator i = aMap.entrySet().iterator();
      boolean hasNext = i.hasNext();
      int ctr = 0;
      while (hasNext)
      {
        Map.Entry entry = (Map.Entry) (i.next());
        Object key = entry.getKey();
        Object value = entry.getValue();
        buffer.append((key == aMap ? "<this map>" : toFFDCStringSingleObject(key)) + "="
                      + (value == aMap ? "<this map>" : toFFDCStringSingleObject(value)) + lineSeparator);

        hasNext = i.hasNext();
        if (ctr > multiple_object_count_to_ffdc)
        {
          buffer.append("........contd");
          hasNext = false;
        }
      }
    }

    buffer.append('}');

    return buffer.toString();
  }
}
