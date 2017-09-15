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
package com.ibm.ws.sib.processor.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 *
 * This class exists to validate alias chains.  A chain is invalid if it forms
 * a loop of destinations.
 */
public class AliasChainValidator
{
  /**
   * Initialise trace for the component.
   */
  private static final TraceComponent tc =
    SibTr.register(
      AliasChainValidator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** NLS for component. */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  /** Collections the destinations already seen. */
  private Collector collector = new Collector();
  
  /**
   * Get the first compound name in any alias chain we're validating.
   * 
   * @return  Will return null if no chain has yet been built up.
   */      
  public CompoundName getFirstInChain()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getFirstInChain");
      
    CompoundName firstInChain = collector.getFirst();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getFirstInChain", firstInChain);
      
    return firstInChain;
  }
  
  /**
   * Check that the given destination, busname combination has not been seen
   * before when resolving an alias.  If it has not been seen, we add it to the
   * map for checking next time.
   * 
   * @param destName
   * @param busName
   * @throws SINotPossibleInCurrentConfigurationException  Thrown if a loop has
   * been detected.
   */
  public void validate(String destName, String busName) throws SINotPossibleInCurrentConfigurationException 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "validate", new Object[] { destName, busName }); 
          
    if (collector.contains(destName, busName))
    {
      // Throw out exception detailing loop    
      // Add the destination name which has triggered the exception to the end
      // of the list, making the problem obvious.
      String chainText = toStringPlus(destName, busName);
      
      CompoundName firstInChain = getFirstInChain();
      
      String nlsMessage = nls.getFormattedMessage(
        "ALIAS_CIRCULAR_DEPENDENCY_ERROR_CWSIP0621",
        new Object[] { firstInChain, chainText, busName },
        null); 
      
      SINotPossibleInCurrentConfigurationException e 
        = new SINotPossibleInCurrentConfigurationException(nlsMessage);
              
      SibTr.exception(tc, e);
        
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "validate", nlsMessage);

      throw e;
    }

    collector.add(destName, busName);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "validate");     
  }  
  
  /**
   * Returns a string representation of the alias chain.
   */
  public String toString()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "toString");
      
    Set destinations = collector.keySet();
    Iterator iter = destinations.iterator();
    StringBuffer sb = new StringBuffer();
    
    while (iter.hasNext())
    {
      sb.append((String)iter.next());
      
      if (iter.hasNext())
        sb.append(" -> ");
    }
    
    String returnString = sb.toString();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "toString", returnString);
      
    return returnString;
  }
  
  /**
   * Return a string representation of the alias chain appended with the
   * destination name given.
   * 
   * @param destName
   * @param busName
   * @return
   */
  public String toStringPlus(String destName, String busName)
  {
    StringBuffer sb = new StringBuffer(toString());
    
    // Add the destination name which has triggered the exception to the end
    // of the list, making the problem obvious.
    String compoundName = new CompoundName(destName, busName).toString();
    sb.append(" -> ");
    sb.append(compoundName);
    
    return sb.toString();
  }
  
  /**
   * @author caseyj
   *
   * This class represents destName:busName as a compound name.
   */
  public static class CompoundName
  {
    private String destName;
    private String busName;
    
    /**
     * The seperator character that will be used when we return a compound name
     */
    private final static String SEPERATOR = ":";
    
    public CompoundName(String myDestName, String myBusName)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(
          tc, "CompoundName", new Object[] { myDestName, myBusName }); 
        
      destName = myDestName;
      busName = myBusName;
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "CompoundName", this);
    }
    
    /**
     * Create a compound destination name.
     * 
     * @param destName
     * @param busName
     * @return The compound destination name
     */
    public static String compound(String destName, String busName)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "compound"); 
        
      String compound = destName + SEPERATOR + busName;
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "compound", compound); 
      
      return compound;
    }
    
    public String getBusName()
    {
      if (tc.isEntryEnabled())
      {
        SibTr.entry(tc, "getBusName"); 
        SibTr.exit(tc, "getBusName", busName);
      }
      
      return busName;
    }
        
    public String getDestName()
    {
      if (tc.isEntryEnabled())
      {
        SibTr.entry(tc, "getDestName");
        SibTr.exit(tc, "getDestName", destName);
      } 
        
      return destName;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "toString"); 
      
      String compound = compound(destName, busName);
    
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "toString", compound); 
      
      return compound;
    }
  }
  
  private class Collector extends LinkedHashMap
  {
    /** The serial version UID, for version to version compatability */
    private static final long serialVersionUID = 2130290956123711056L;
    
    /**
     * Add a destination to the chain.
     * 
     * @param destName
     * @param busName
     */
    private void add(String destName, String busName)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "add", new Object[] { destName, busName }); 
      
      collector.put(
        CompoundName.compound(destName, busName), 
        new CompoundName(destName, busName));
    
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "add"); 
    }  
      
    /**
     * Check if the chain contains a given destination.
     * 
     * @param destName
     * @param busName
     * @return true if the destination is in the chain
     */
    private boolean contains(String destName, String busName)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "contains", new Object[] { destName, busName }); 
      
      boolean result 
        = collector.containsKey(CompoundName.compound(destName, busName));
    
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "contains", new Boolean(result)); 
      
      return result;
    }  
      
    /**
     * Return the compound name of the first destination in the chain.  If there
     * is no chain, returns null.
     * 
     * @return
     */
    private CompoundName getFirst()
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "getFirst");
     
      CompoundName dest = null;      
      Collection collectorValues = collector.values();
      
      if (collectorValues != null)
      {
        dest = (CompoundName)collectorValues.toArray()[0];
      }
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getFirst", dest);
      
      return dest;
    } 
  }
}
