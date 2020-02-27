/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.security;

import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ViaHeader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.servlet.sip.SipServletMessage;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * The IPAuthenticator role is to bypass authentication for specific IPs.
 * the  config parameter name (which hold the IPs that can bypass security) is
 *	com.ibm.ws.sip.security.trusted.iplist
 *
 *	should be looked like something as :
 *	<property>
 *		<name>com.ibm.ws.sip.security.trusted.iplist</name>
 *		<value>121.55.44.33,               // ip
 *             192.168.12.1/255.255.255.0, //ip/subnet
 *             192.161.12.3/255.255.0.0,
 *             10.10.1.1,
 *				oakland.haifa.ibm.com      //full dns name
 *		</value>
 *	</property>
 *  
 *  calulation of subnet:
 *   suppose ip1 and subnet1 are given authentication bypass, when we wish to check that ip2 is on the subnet we do the following:
 *   m1= ip2int(ip1)& ip2int(subnet1)
 *   m2= ip2int(ip2)& ip2int(subnet1)
 *   if m1==m2 then on the same subnet
 *   
 * this calls does not support IPv6
 */
public class IPAuthenticator {
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(IPAuthenticator.class);
    
	private static IPAuthenticator s_instance = null;
	private InetAddress[] m_ips =null;
	private int[] m_masks=null;
	private int[] m_subnets=null;
	private boolean m_hasCheck=false;
	
	static{
		s_instance = new IPAuthenticator();
		//s_instance = new IPAuthenticator("121.55.44.33,192.168.12.1/255.255.255.0,192.161.12.3/255.255.0.0,10.10.1.1");
	}
	
	
	private static int ip2Int(InetAddress address){
		
		byte[] bytes = address.getAddress();
		int j=0;
		int k=0;
		for (int i = 0 ; i<4 ; i++)
		{
			j <<= 8;
			k=bytes[i];
			if(k<0) k+=256;
			j += k;
		}
		return j;
	}

	private static String int2Ip(int ip) throws UnknownHostException{
		
		byte[] bytes = {0,0,0,0};
		for (int i = 0 ; i < 4 ; i++)
		{			
			bytes[3-i] = (byte)(ip);
			ip >>=8;
		}
		return InetAddressCache.getHostAddress(InetAddress.getByAddress(bytes));
	}
	
	/**
	 * parse config param
	 * @param ipList
	 */
	private void parseList(String ipList){
		
		ArrayList ips = new ArrayList(2);
		ArrayList masks = new ArrayList(2);
		ArrayList subnets = new ArrayList(2);
		
		StringTokenizer ipListTokenizer = new StringTokenizer(ipList,",");
		String token = null;
		int index = -1;
		while (ipListTokenizer.hasMoreTokens()){
			token = ipListTokenizer.nextToken();
			token = token.trim();
			index = token.indexOf('/');
			if(index==-1){		
				try{
					InetAddress address = InetAddressCache.getByName(token);
					ips.add(address);
				}catch(Exception e){
					
					if(c_logger.isTraceDebugEnabled()){
						c_logger.traceDebug(this,"IPAuthenticator","not valid address ["+token+"]");
					}
					
					System.out.println("IPAuthenticator - not valid address ["+token+"]");
				}
				
			}else{
				String ip = token.substring(0,index);
				String mask = token.substring(index+1);
				try{
					int intIp = ip2Int(InetAddressCache.getByName(ip));
					int intMask= ip2Int(InetAddressCache.getByName(mask));
					int intSubnet=intIp&intMask;
					masks.add(new Integer(intMask));
					subnets.add(new Integer(intSubnet));
				}catch (UnknownHostException e) {
					if(c_logger.isErrorEnabled())
		 		    {
		                 c_logger.error(
		                     "error.ip.host",
		                     Situation.SITUATION_START,
		                     null,
		                     e);
		 		    }
					System.out.println("IPAuthenticator - not valid subnet ["+ip+"/"+mask+"]");
				}catch(Exception e){
					if(c_logger.isTraceDebugEnabled()){
						c_logger.traceDebug(this,"IPAuthenticator","not valid subnet ["+ip+"/"+mask+"]");
					}
					System.out.println("IPAuthenticator - not valid subnet ["+ip+"/"+mask+"]");
				}
			}
		}
		
		int len=ips.size();
		m_ips = new InetAddress[len];
		for(int i=0;i<len;i++){
			m_ips[i]=(InetAddress) ips.get(i);
		}
		len=masks.size();
		m_masks = new int[len];
		m_subnets=new int[len];
		for(int i=0;i<len;i++){
			m_masks[i]=((Integer)masks.get(i)).intValue();
			m_subnets[i]=((Integer)subnets.get(i)).intValue();
		}			 
	}
	
	private IPAuthenticator(){
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"<init>");
		}
		
		String ipList = PropertiesStore.getInstance().getProperties().getString(CoreProperties.IP_LIST_PROPERTY);
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this,"IPAuthenticator","iplist=["+ipList+"]");
		}
		
		if(ipList!=null){
			parseList(ipList);
			if((m_ips!=null)||(m_subnets!=null)){
				m_hasCheck = true;
			}
		}
			
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this,"<init>");
		}
	}
	
	private IPAuthenticator(String ipList){
		parseList(ipList);
	}
	
	private static boolean checkIp(String ip){
		//check if in secure subnets
		try {
			InetAddress address = InetAddressCache.getByName(ip);
			int intIp = ip2Int(address);
			int tmp;
			for (int i=0;i<s_instance.m_masks.length;i++){
				tmp = intIp&s_instance.m_masks[i];
				if(tmp==s_instance.m_subnets[i]){
					return true;
				}
			}	
			//	check if in ip list
			
			//System.out.println("wiil check A - for " + address.getHostAddress());
			//System.out.println("will check B - for " + address.getHostName());
			for (int i=0;i<s_instance.m_ips.length;i++){
				//System.out.println("check A - " + s_instance.m_ips[i].getHostAddress());
				String ip1 = InetAddressCache.getHostAddress(address);
				String ip2 = InetAddressCache.getHostAddress(s_instance.m_ips[i]);
				if(ip1.equals(ip2)){
					return true;
				}

				if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.IPAUTHENTICATOR_CHECK_HOST_NAMES)){
					//System.out.println("check B - " + s_instance.m_ips[i].getHostName());
					if(address.getHostName().equals(s_instance.m_ips[i].getHostName())){
						return true;
					}
				}else{
					if(c_logger.isTraceDebugEnabled()){
						c_logger.traceDebug(IPAuthenticator.class, "checkIp", "skipping host name comparison");
					}
				}
			}
		} catch (UnknownHostException e) {
			if(c_logger.isErrorEnabled())
 		    {
                 c_logger.error(
                     "error.ip.host",
                     Situation.SITUATION_START,
                     null,
                     e);
 		    }
		}
		
		return false;
	}
	public static boolean isIPAuthenticated(SipServletMessage message){
		if(s_instance.m_hasCheck){
			SipServletMessageImpl msg = (SipServletMessageImpl) message;
			HeaderIterator viaHeaders = msg.getMessage().getViaHeaders();
			if (viaHeaders != null) {
				for(int i=0;i<2;i++){
					if (viaHeaders.hasNext()) {
						try {
							ViaHeader via = (ViaHeader) viaHeaders.next();
							String host = (String) via.getHost();
							boolean checkVia = checkIp(host);
							if(checkVia){
								return true;
							}
						} catch (HeaderParseException e) {
							if(c_logger.isErrorEnabled()) {
								c_logger.error("error.ip.header.parse",Situation.SITUATION_START,null,e);
							}
						} catch (NoSuchElementException e) {
							if(c_logger.isErrorEnabled()) {
								c_logger.error("error.ip.header.exception",Situation.SITUATION_START,null,e);
							}
						}
					}
				}			

			}
		}		
		return false;
	}
	
	public static void main(String[] args) {
		System.out.println(checkIp("121.55.44.33"));
		System.out.println(checkIp("10.10.1.1"));
		System.out.println(checkIp("122.55.44.33"));
		System.out.println(checkIp("192.168.12.3"));
		System.out.println(checkIp("192.168.13.1"));
		System.out.println(checkIp("192.161.1.2"));
		System.out.println(checkIp("192.162.5.5"));
		System.out.println(checkIp("192.161.133.122"));
	}
}
