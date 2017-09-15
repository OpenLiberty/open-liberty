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

package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.util.Stack;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: RLSUtils
//------------------------------------------------------------------------------
/**
* Common utility functions for the Recovery Log Service
*/
public class RLSUtils
{
  private static final TraceComponent tc = Tr.register(RLSUtils.class,
                                           TraceConstants.TRACE_GROUP, null);

  /**
  * Lookup string that allows character digit lookup by index value.
  * ie _digits[9] == '9' etc.
  */
  private final static String _digits = "0123456789abcdef";

  /**
  * This field is intended to be used by callers of toHexString to limit the
  * maximum number of bytes that will be output from trace points. If RAS
  * trace points try and output large amounts of information, OutOfMemory
  * failures can occur.
  */
  public static final int MAX_DISPLAY_BYTES = 32;
  
  /** The size, in bytes, of the data used to persist a boolean value
   */
  protected static final int BOOLEAN_SIZE = 1;
  /** The size, in bytes, of the data used to persist a short value
     */
  protected static final int SHORT_SIZE = 2;
  /** The size, in bytes, of the data used to persist a int value
   */
  protected static final int INT_SIZE = 4;
  /** The size, in bytes, of the data used to persist a long value
   */
  protected static final int LONG_SIZE = 8;

  /**
   * The string delimeter for UNC file name
   */
  protected static final String UNC_HEADER = new String(File.separator + File.separator);

  /**
  * It is not safe to access different instances of a File object to test for and
  * then create a directory hierarchy from different threads. This can occur when
  * the user is issuing multiple concurrent openLog calls. In order to make this
  * safe, we must synchronize on a static object when we prepare the directory 
  * structure for a recovery log in the openLog method.
  */
  private static Object _directoryCreationLock = new Object();
  
  //------------------------------------------------------------------------------
  // Method: Utils.toHexString
  //------------------------------------------------------------------------------
  /**
  * Converts a byte array into a printable hex string.
  *
  * @param byteSource The byte array source.
  *
  * @return String printable hex string or "null"
  */
  public static String toHexString(byte [] byteSource)
  {
    if (byteSource == null)
    {
      return "null";
    }
    else
    {
      return toHexString(byteSource,byteSource.length);
    }
  }  

  //------------------------------------------------------------------------------
  // Method: Utils.toHexString
  //------------------------------------------------------------------------------
  /**
  * Converts a byte array into a printable hex string.
  *
  * @param byteSource The byte array source.
  * @param bytes The number of bytes to display.
  * 
  * @return String printable hex string or "null"
  */
  public static String toHexString(byte [] byteSource,int bytes)
  {
    StringBuffer result = null;
    boolean truncated = false;

    if (byteSource != null)
    {
      if (bytes > byteSource.length)
      {
        // If the number of bytes to display is larger than the available number of
        // bytes, then reset the number of bytes to display to be the available
        // number of bytes.
        bytes = byteSource.length;
      }
      else if (bytes < byteSource.length)
      {
        // If we are displaying less bytes than are available then detect this
        // 'truncation' condition.
        truncated = true;
      }

      result = new StringBuffer(bytes*2);
      for (int i = 0; i < bytes; i++)
      {
        result.append(_digits.charAt((byteSource[i] >> 4) & 0xf));
        result.append(_digits.charAt(byteSource[i] & 0xf));
      }

      if (truncated)
      {
        result.append("... (" + bytes + "/" + byteSource.length + ")");
      }
      else
      {
        result.append("(" + bytes + ")");
      }
    }
    else
    {
      result = new StringBuffer("null");
    }

    return(result.toString());
  }  


  public static String FQHAMCompatibleServerName(String cell,String node,String server)
  {
    return cell + "\\" + node + "\\" + server;
  }

  public static boolean createDirectoryTree(String requiredDirectoryTree)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "createDirectoryTree",requiredDirectoryTree);

    boolean exists = true;

    // Check to see if the required log directory already exists. If not then create
    // it. This must be serialized as this type of File access is not thread safe. Note
    // also that this is created a single directory at a time. This may seem odd, but it
    // provides protection against two servers trying to create a common directory path
    // at a sime time. One will create the directory, the other will detect that its create
    // failed but the directory now exists and continue on. Only if an attempt to create the
    // directory fails and the directory still does not exist will this be reported as an
    // exception.
    // 
    // Code added by PK35957 to handle UNC file specification
    // For example //server/share/directory path
    synchronized(_directoryCreationLock)
    {
      File target = new File(requiredDirectoryTree);

      // Only proceed if the directory does not exist.
      if (!target.exists())
      {
        if (tc.isEventEnabled()) Tr.event(tc, "Creating directory tree", requiredDirectoryTree);

        Stack pathStack = new Stack();

        while (target != null)
        {
          pathStack.push(target);
          target = target.getParentFile();
        }

        while (!pathStack.empty() && exists)
        {
          target = (File)pathStack.pop();
          if (tc.isDebugEnabled()) Tr.debug(tc, "Checking path to " + target.getAbsolutePath());

          if (!target.exists())
          {
            // Don't try if the target is just //
            if (target.getAbsolutePath().equals(UNC_HEADER))                                                                           // PK35957
            {                                                                                                                          // PK35957
             if (tc.isDebugEnabled()) Tr.debug(tc, "Ignoring " + target.getAbsolutePath() + " - is " + UNC_HEADER);                    // PK35957
             continue;                                                                                                                 // PK35957
            }                                                                                                                          // PK35957
            if (tc.isDebugEnabled()) Tr.debug(tc, "Creating path to " + target.getAbsolutePath());
            boolean created = target.mkdirs();

            if (created)
            {
              if (tc.isDebugEnabled()) Tr.debug(tc, "Created path to " + target.getAbsolutePath());
            }
            else
            {
              if (target.getAbsolutePath().startsWith(UNC_HEADER))                                                                     // PK35957
              {                                                                                                                        // PK35957
                if (tc.isDebugEnabled()) Tr.debug(tc, "Ignoring " + target.getAbsolutePath() + " - starts with " + UNC_HEADER);        // PK35957                                                                                                // PK35957
                continue;                                                                                                              // PK35957
              }                                                                                                                        // PK35957
              if (tc.isDebugEnabled()) Tr.debug(tc, "Did not create path to " + target.getAbsolutePath());

              if (!target.exists())
              {
                if (tc.isEventEnabled()) Tr.event(tc, "Unable to create directory tree");
                exists = false;
              }
              else
              {
                if (tc.isDebugEnabled()) Tr.debug(tc, "Path to " + target.getAbsolutePath() + " already exists");
              }
            }
          }
          else
          {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Path to " + target.getAbsolutePath() + " already exists");
          }
        }
      }
      // This check is in case a UNC specified file could not be created. We do not set exists to false in the creation path,
      // since we get failures trying to create the server and share part of the path, but need to continue trying
      if (exists && !target.exists())                                                                                                    // PK35957
      {                                                                                                                                  // PK35957
          if (tc.isDebugEnabled()) Tr.debug(tc, "Did not create path to " + target.getAbsolutePath());                                   // PK35957
          exists = false;                                                                                                                // PK35957
      }                                                                                                                                  // PK35957

    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "createDirectoryTree",new Boolean(exists));
    return exists;
  }
}

