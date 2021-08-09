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
 
package com.ibm.ws.sib.api.jms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.jms.JMSException;

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;

/**
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 * 
 * This class provides a conceptual wrappering around an array of Strings representing
 * the names of the destinations through which the message will be sent. Slightly ironically,
 * the object that it actually wraps is not an array, and isn't of type String.
 * 
 * The reason for this (241555) is that we also need to retain the busName attribute
 * of the SIDestinationAddress object representing the destination. Thankfully, through
 * the wonders of object oriented programming and encapsulation, no-one will ever know
 * the difference...
 * 
 * @author matrober 
 */
public class StringArrayWrapper implements Serializable
{
  // suid assigned at version 1.12
  private static final long serialVersionUID = 3870265914027831694L;
  
  // This is the full list of destinations that will be passed through.
  // The corePath is all but the last element of this list.
  // The msgForwardRoutingPath is all but the first element of this list.
  private List fullMsgPath = null;
  
  
  // Used to separate the bus names from the destination names.
  public static final String BUS_SEPARATOR = ":";
  
  /**
   * This constructor takes a full path of destinations including the destination
   * to which the producer will attach, and the 'big' destination on which the
   * message will end up.
   * 
   * This will have been used to fully configure the destination including the
   * advertised destination name and bus.
   * 
   * @param siDests The full list of SIDestinationAddress objects representing
   *           the intended message path.
   * @throws JMSException
   */
  public StringArrayWrapper(List siDests) throws JMSException
  {
    
    fullMsgPath = siDests;
    
  }//constructor
  
  /**
   * This method is used for unit test purposes to simulate the creation of a
   * StringArrayWrapper whose destinations are all on the local bus.
   * 
   * The individual strings of the first parameter may be of the following form;
   *     destName       - just the destination name to be used.
   *     destName:bus   - destination name and associated bus name.
   * 
   * @param data The producer destination and elements up to but not including
   *             the big destination.
   * @param bigDestName The name of the 'big' destination that the message will
   *             end up at.
   * @throws JMSException
   */
  public static StringArrayWrapper create(String[] data, String bigDestName) throws JMSException
  {
            
    int size = 0;
    if (data != null) size = data.length;

    List fakedFullMsgPath = new ArrayList(size+1);
      
    if (size > 0)
    {      
        
      for (int i = 0; i < size; i++)
      {
          
        // Create the appropriate List element.
        String destName = data[i];
        String busName = null;
        SIDestinationAddress sida;
        
        // If this is of the form dest:bus
        if (destName.indexOf(BUS_SEPARATOR) != -1)
        {
          busName = destName.substring(destName.indexOf(BUS_SEPARATOR)+1);
          destName = destName.substring(0, destName.indexOf(BUS_SEPARATOR));
        }
        
        try
        {
          sida = JmsServiceFacade.getSIDestinationAddressFactory().createSIDestinationAddress(destName,busName);
                  
          fakedFullMsgPath.add(sida);
    
        } catch (Exception e)
        {   
          // No FFDC code needed
          
          // This makes it the responsibility of the calling function to handle this
          // problem. Note that the StringArrayWrapper is used only to handle forward
          // and reverse routing paths, which are not supported function so this
          // code should never be driven in normal product operation.
          JMSException jmse = new JMSException(e.getMessage());
          jmse.setLinkedException(e); 
          jmse.initCause(e);    
          
        }//try
                    
          
      }//for
        
      if (bigDestName != null)
      {
        
        // Make sure we add the real destination on the end of the msg FRP.
        try
        {
          SIDestinationAddress sida =
        	  ((SIDestinationAddressFactory)JmsServiceFacade.getSIDestinationAddressFactory()).createSIDestinationAddress(
              bigDestName,
              null);
                          
          fakedFullMsgPath.add(sida);
                
        } catch (Exception e)
        {   
          // No FFDC code needed

          // This makes it the responsibility of the calling function to handle this
          // problem. Note that the StringArrayWrapper is used only to handle forward
          // and reverse routing paths, which are not supported function so this
          // code should never be driven in normal product operation.
          JMSException jmse = new JMSException(e.getMessage());
          jmse.setLinkedException(e); 
          jmse.initCause(e);   
          
        }//try
          
      }//if
        
    }//if size > 0
    
    StringArrayWrapper newSAW = new StringArrayWrapper(fakedFullMsgPath);
    return newSAW;
      
  }//unit test factory method.
    
  /**
   * This method returns the forward routing path that should be set into the message.
   * 
   * This excludes the first element in the list, which the producer will be attached to,
   * but does include the 'big' destination name that the message will end up at.
   */
  public List getMsgForwardRoutingPath()
  {    
    return fullMsgPath.subList(1, fullMsgPath.size());
    
  }
    
  /**
   * Returns a list of the destination names not including the 'big'
   * destination name.   
   */
  public String[] getArray()
  {
    
    // Create a copy to return to provide isolation.
    String[] newArray = null;
        
    // Returning a list of the destination names excluding the
    // 'big' destination name.
    newArray = new String[fullMsgPath.size()-1];
    for (int i = 0; i < newArray.length; i++)
    {
      newArray[i] = ((SIDestinationAddress)fullMsgPath.get(i)).getDestinationName();
    }//for
    
    return newArray;
  }
    
  public boolean equals(Object that)
  {
    if (that == null) return false;
    if (this == that) return true;
      
    if (that instanceof StringArrayWrapper)
    {
      StringArrayWrapper thatSaw = (StringArrayWrapper)that;
        
      if (thatSaw.fullMsgPath == this.fullMsgPath) return true;        
      if (thatSaw.fullMsgPath.size() != this.fullMsgPath.size()) return false;
        
      for (int i = 0; i < fullMsgPath.size(); i++)
      {
        if (!thatSaw.fullMsgPath.get(i).equals(this.fullMsgPath.get(i))) return false; 
      }//for
        
    } else
    {
      return false; 
    }
      
    return true;
      
  }//equals
    

  /**
   * The actual routing path being set (in the old pattern).
   * 
   * This includes the first element to which the producer will be attached,
   * but doesn't include the 'big' destination name.
   */
  public List getCorePath()
  {    
    return fullMsgPath.subList(0, fullMsgPath.size()-1);
  }
  
  
  /**
   * Returns the first element in the full path, which is where the producer
   * should connect to.   
   */
  public SIDestinationAddress getProducerSIDestAddress()
  {
    return (SIDestinationAddress)fullMsgPath.get(0);  
  }
  
  
  /**
   * Custom toString method used for the URI string output.
   */
  public String toString()
  {
    
    
    List arrayStrings = getCorePath();
    ListIterator iter = arrayStrings.listIterator();
    
    String result = "";
    
    while (iter.hasNext())
    {
      SIDestinationAddress thisSida = ((SIDestinationAddress)iter.next());
      
      result += thisSida.getDestinationName();
      if ((thisSida.getBusName() != null) && (!"".equals(thisSida.getBusName())))
      {
        result += BUS_SEPARATOR+thisSida.getBusName();
      }
      
      if (iter.hasNext()) result += JmsraConstants.PATH_ELEMENT_SEPARATOR;
      
    }//while    
    
    return result;
  }

}//StringArrayWrapper class
