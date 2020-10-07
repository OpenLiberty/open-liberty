/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip.ar;

import java.io.Serializable;

/**
 * A class that represents the application routing region. 
 * It uses the predefined regions in the Enum SipApplicationRoutingRegionType 
 * and also allows for implementations to have additional or new regions 
 * if it is so required. This could be useful in non telephony domains where 
 * the concept of of a caller and callee is not applicable.
 * 
 * @since 1.1
 */
public class SipApplicationRoutingRegion implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The NEUTRAL region contains applications that do not service a 
	 * specific subscriber.
	 */
	public static final SipApplicationRoutingRegion NEUTRAL_REGION = 
				new SipApplicationRoutingRegion("sd",SipApplicationRoutingRegionType.NEUTRAL);
	
	/**
	 * The ORIGINATING region contains applications that service the caller.
	 */
	public static final SipApplicationRoutingRegion ORIGINATING_REGION  = 
				new SipApplicationRoutingRegion("sd",SipApplicationRoutingRegionType.ORIGINATING);
	
	/**
	 * The TERMINATING region contains applications that service the callee.
	 */
	public static final SipApplicationRoutingRegion TERMINATING_REGION  = 
				new SipApplicationRoutingRegion("sd",SipApplicationRoutingRegionType.TERMINATING);
	
	
	private String label;
	private SipApplicationRoutingRegionType type;
	
	
	/**
	 * Deployer may define new routing region by constructing a new 
	 * SipApplicationRoutingRegion object. The SipApplicationRoutingRegionType  
	 * may be null in cases when a custom region is defined.
	 * @param label
	 * @param type
	 */
	public SipApplicationRoutingRegion(String label, SipApplicationRoutingRegionType type)
	{
		this.label = label;
		this.type = type;		
	}
	
	/**
	 * Each routing region has a String label.
	 * 
	 * @return The label of the routing region
	 */
	public String getLabel(){
		return label;
	}
	
	/**
	 * Each routing region is either ORIGINATING, TERMINATING, or NEUTRAL type.
	 * @return The routing region type, a null return indicates a custom region.
	 */
	public final SipApplicationRoutingRegionType getType(){
		return type;
	}
	
	/**
	 *  Overrides toString in class Object
	 *  return string for log purposes
	 *  
	 *  @return String for object representation
	 */
	public String toString(){

		String outputString = null;
		
		StringBuffer buff = new StringBuffer();
		buff.append("Label = " + label);
		buff.append(" Type = " + type);
		outputString = buff.toString();
			
		return outputString;
	}
}
