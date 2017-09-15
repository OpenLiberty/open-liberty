/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.util.Properties;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.wsspi.resource.ResourceFactory;

//------------------------------------------------------------------------------
// Class: CustomLogProperties
//------------------------------------------------------------------------------
/**
* <p>
* An implementation of the LogProperties interface that defines the physical 
* characteristics of a generic or custom recovery log.
* </p>
*
* <p>
* Custom recovery log implementations use an eclipse extension point to add
* functionality to the recovery log service.  Each implementation is identified
* by its extension id and consists of a set of properties
* </p>
*/                                                                          
public class CustomLogProperties implements LogProperties
{
  /**
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(CustomLogProperties.class,
                                           TraceConstants.TRACE_GROUP,
                                           TraceConstants.NLS_FILE);
                                           
  /**
   * Indicates that a log that may contain multiple
   * failure scopes is required.
   */
  protected static final int LOG_TYPE_MULTIPLE_SCOPE = 0;
  
  /**
   * Indicates that a log that will only contain records
   * for a single failure scope is required.  
   */
  protected static final int LOG_TYPE_SINGLE_SCOPE = 1;

  /**
   * default log type.  Set during initalization.
   */
  static int defaultLogType = LOG_TYPE_SINGLE_SCOPE;

  /**
  * The unique RLI value.
  */
  private int _logIdentifier;

  /**
  * The unique RLN value.
  */
  private String _logName;

  /**
   * The identifier of the log that is required.  Corresponds to the eclipse plugin id
   */
  private String _pluginId;

  /**
   * The properties associated with the log
   */
  private Properties _props;

  /**
   * The type of the log that is required (single or multi-scope).
   */
  private int _logType;
 
  /**
   * The Resource Factory associated with this log implementation 
   */
  private ResourceFactory _resourceFactory;

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.CustomLogProperties
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Constructor for a new CustomLogProperties object.   
  * </p>
  *
  * <p>
  * The logIdentifier and logName both uniquely identify a recovery log within
  * the client service.
  * </p>
  *
  * @param logIdentifier The unique RLI value.
  * @param logName The unique RLN value.
  */
  public CustomLogProperties(int logIdentifier,String logName, String pluginId, Properties props)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "CustomLogProperties",new java.lang.Object[]{new Integer(logIdentifier),
                                                              logName,
                                                              pluginId,
                                                              props});

    // Cache the supplied information.
    _logIdentifier = logIdentifier;
    _logName       = logName;
    _pluginId      = pluginId;
    _props         = props;
    _logType = defaultLogType;
 
    if (tc.isEntryEnabled()) Tr.exit(tc, "CustomLogProperties", this);
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.logIdentifier
  //------------------------------------------------------------------------------
  /**
  * Returns the unique (within service) "Recovery Log Identifier" (RLI) value.
  *
  * @return int The unique RLI value.
  */
  public int logIdentifier()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "logIdentifier",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "logIdentifier",new Integer(_logIdentifier));
    return _logIdentifier;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.logName
  //------------------------------------------------------------------------------
  /**
  * Returns the unique (within service) "Recovery Log Name" (RLN). 
  *
  * @return String The unique RLN value.
  */
  public String logName()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "logName",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "logName",_logName);
    return _logName;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.pluginId
  //------------------------------------------------------------------------------
  /**
  * Returns the pluginId associated with this type of log
  * @return int eclipse plugin id
  */
  public String pluginId()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "pluginId",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "pluginId",_pluginId);
    return _pluginId;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.properties
  //------------------------------------------------------------------------------
  /**
  * Returns the set of properties associated with this log implementation
  * 
  * @return Properties custom properties associated with this log implementation
  */
  public Properties properties()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "propertis",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "properties", _props);
    return _props;
  }

  
  protected int logType()
  {
  	if (tc.isEntryEnabled()) Tr.entry(tc, "logType", this);  
  	if (tc.isEntryEnabled()) Tr.exit(tc, "logType", new Integer(_logType));	
  	return _logType;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.equals
  //---------------------------------------------------------------------
  /**
   * Determine if two LogProperties references are the same.
   * @param logProps The log properties to be checked
   * @return boolean true If compared objects are equal.
   */
  public boolean equals(Object lp)
  {
      if(lp == null) return false;
      else if (lp == this) return true;
      else if (lp instanceof CustomLogProperties)
      {
          CustomLogProperties clp = (CustomLogProperties)lp;

          if (clp.logIdentifier()== this.logIdentifier()    &&
              clp.logType() == this.logType() &&
              clp.pluginId() == (this.pluginId()))
          {
              return true;
          }
      }

      return false;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.hashCode
  //--------------------------------------------------------------------
  /**
   * HashCode implementation.
   * @return int The hash code value.
   */
  public int hashCode()
  {
       int hashCode = 0;

       hashCode += _logIdentifier/5;
       hashCode += _logName.hashCode()/5;
       hashCode += _logType;
       hashCode += _props.hashCode()/5;

       return hashCode;
  }

  public static void setDefaultLogType(int t)
  {
      defaultLogType = t;
  }

  public void setResourceFactory(ResourceFactory fac)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "setResourceFactory", this);  
      if (tc.isEntryEnabled()) Tr.exit(tc, "setResourceFactory", fac);
      _resourceFactory = fac;
  }

  //------------------------------------------------------------------------------
  // Method: CustomLogProperties.resourceFactory
  //------------------------------------------------------------------------------
  /**
  * Returns the Resource Factory associated with this log 
  * implementation 
  * 
  * @return ResourceFactory associated with this log
  *         implementation
  */
  public ResourceFactory resourceFactory()
  {
  	if (tc.isEntryEnabled()) Tr.entry(tc, "resourceFactory", this);  
  	if (tc.isEntryEnabled()) Tr.exit(tc, "resourceFactory", _resourceFactory);	
  	return _resourceFactory;
  }
}
