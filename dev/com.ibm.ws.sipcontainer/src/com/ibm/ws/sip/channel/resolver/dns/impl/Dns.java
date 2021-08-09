/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.dns.impl;

/**
 * Constants Class for interfacing with a DNS naming Service .
 * <p>
 * See RFC 1035
 */
public final class Dns {

	  /** DNS Resource Record Types RFC 1035 */
	  public static final short A     = 1;
	  public static final short NS    = 2;
	  public static final short CNAME = 5;
	  public static final short SOA   = 6;   
	  public static final short WKS   = 7;   
	  public static final short PTR   = 12;
	  public static final short HINFO = 13;
	  public static final short MX    = 15;
	  public static final short TXT   = 16;
	  public static final short AAAA  = 28;   
	  public static final short SRV   = 33;   
	  public static final short NAPTR = 35;
	  public static final short OPT   = 41;
	  public static final short AXFR  = 252;
	  public static final short MAILB = 253;
	  public static final short MAILA = 254;
	  public static final short ALL   = 255;
	  
	  public static String [] TYPESTRING = new String [] 
	  {   "","A","NS","","","CNAME","SOA","WKS","","","","","PTR","HINFO","","MX","TXT","","","",
		  "","","","","","","","","AAAA","","","","","SRV","","NAPTR","","","","",
		  "","OPT","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","","","","","","","","",
		  "","","","","","","","","","","","","MAILB","MAILA","ALL"
		  };
		  
	  
	  /** Bit Fields in the flags section of a Dns Header */
	  public static final int QR = 0;
	  public static final int AA = 5;
	  public static final int TC = 6;
	  public static final int RD = 7;
	  public static final int RA = 8;
	  public static final int RCODE0 = 12;
	  public static final int RCODE1 = 13;
	  public static final int RCODE2 = 14;
	  public static final int RCODE3 = 15;
	  
	  /** Dns Class types */
	  public static final short IN = 1;
	  public static final short CS = 2;
	  public static final short CH = 3;
	  public static final short HS = 4;
	  /** Dns QClass types */
	  public static final short STAR = 255;
	  
	  /** Dns RCODES */
	  public static final int NO_ERROR    = 0;
	  public static final int FORM_ERROR  = 1;
	  public static final int SERVER_FAIL = 2;
	  public static final int NAME_ERROR  = 3;
	  public static final int NOT_IMPL    = 4;
	  public static final int REFUSED     = 5;
	  public static final int TRY_TCP     = 6;
	  /** 6-15 reserved for future use */
	  
	 
}

