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
package com.ibm.ws.sip.container.virtualhost;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Pattern;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;

/**
 * @author Nitzan, Aug 8, 2005
 * Implementation of a virtual host alias object
 */
public class VirtualHostAliasImpl implements VirtualHostAlias{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(VirtualHostAliasImpl.class);
    
    /**If true then the paticulare host doesn't matter*/
    private boolean _isAnyHost;
    
    /**If true then the paticulare port doesn't matter*/
    private boolean isAnyPort;
    
    /**If true then the its a regular expression, 3 modes are supported: *server, server*, *server* */
    private boolean _isRegExp; 
    
    /**If this is a regexp we are saving the pattern to improve performance*/
    private Pattern _regExp;
    
    /**Host name*/
    private String _host;
    
    /**Host ip*/
    private String _ip;
    
    /**InetAddress representation of host*/
    private InetAddress _inetAddress;
    
    /**host port*/
    private int _port = -1;
    
    /**Wildcard that represents 'any host'*/
    private static final String ANY_HOST = "*";
   
    /**JAVA  Wildcard that represents 'any character'*/
    private static final String ANY_CHAR = ".*";

    
    /** Representation of the virtual host as a host:port string*/ 
    private String hostPortStr;
    
    /**
     * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#init(java.lang.String)
     */
    public void init( String hostPortStr){
    	//if this ipv6 address, the port is just after the last :
        int dots = hostPortStr.lastIndexOf(':');

        String host = hostPortStr.substring( 0,dots);
        String port = hostPortStr.substring( dots+1);

        if (port.equals("*")) {
        	setAnyPort(true);
        	port = "-1";
        }
        
        init( host, Integer.parseInt( port));
    }
    
    
    /**
     * getter for isAnyPort
     */
    public boolean isAnyPort() {
		return isAnyPort;
	}


    /**
     * setter for isAnyPort
     * @param isAnyPort
     */
	private void setAnyPort(boolean isAnyPort) {
		this.isAnyPort = isAnyPort;
	}

	/**
     * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#init(java.lang.String, int)
     */
	public void init( String host, int port){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "init", host, port);
		}
        _port = port;
        _isAnyHost = host.equals( ANY_HOST);
        if (!_isAnyHost && (host.endsWith(ANY_HOST)|| host.startsWith(ANY_HOST))){
        	_isRegExp = true;
        	//the regExpStr is set to the java equivalent regular expression string.
            //*server -> .*server 
            //server* -> server.*
            //*server* -> .*server.*
        	String reExpStr = host.replace(ANY_HOST, ANY_CHAR);
        	_regExp = Pattern.compile(reExpStr);
        }
        _host = host;
       
        if( !_isAnyHost && !_isRegExp){
            InetAddress addr = null;
            try {
                addr = InetAddressCache.getByName(host);
                _ip = InetAddressCache.getHostAddress(addr);
                _inetAddress = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
            	if(c_logger.isErrorEnabled()){
            		c_logger.error( "error.cannot.lookup.host",null,host,e);
            	}
               _ip = host;
            }
        }
        hostPortStr = null;
        if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "init", "result: host="+_host + ", _ip=" +_ip + ", _isAnyHost="+_isAnyHost);
		}
        
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "init");
		}
    }
    
    /**
     * @return Returns the _isAnyHost.
     */
    public boolean isAnyHost() {
        return _isAnyHost;
    }
    
    /**
     * @return Returns the ip.
     */
    public String getIp() {
        return _ip;
    }
    
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return _port;
    }
    
    
    
    /**
     * Find if to aliases match
     */ 
	public boolean match( VirtualHostAlias vh)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "match", vh);
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "match", "Matching with this VH _ip=" + _ip + ", _host=" + _host + ", _port=" + _port);
		}
		boolean matches = false;
		
		//return true if it is the same object
		try {
			if (this == vh){
				return matches = true;
			}
			
			if (vh.isAnyHost() || this.isAnyHost()){ //check if one of them supports any host
				matches = true;
			}else if (this.isRegExp() && !vh.isRegExp()){ //check if one of the VHs is regular expression and do regExp comparison
				//check the host and the ip for reg exp comparison
				matches = (this.getRegExp().matcher(vh.getHost()).matches() ||
						   this.getRegExp().matcher(vh.getIp()).matches()); 
			}else if (!this.isRegExp() && vh.isRegExp()){//check if one of the VHs is regular expression and do regExp comparison
				//check the host and the ip for reg exp comparison
				matches = (vh.getRegExp().matcher(this.getHost()).matches() ||
						   vh.getRegExp().matcher(this.getIp()).matches());   
			}else {//both of them are regular VHs, do simple comparison
				//check if the host name or the ip address are equal
				matches = (this._ip.equals( vh.getIp()) || this._host.equals( vh.getHost()) || matchInetAddr(vh));
			}

			//check that the ports are equal
			if (matches){
				matches = (this.isAnyPort() || vh.isAnyPort() || 
						   this.getPort() == vh.getPort());
			}

			return matches;
		} finally {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(null, "match", matches);
			}
		}
	}
    
	/**
	 * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#matchInetAddr(com.ibm.ws.sip.container.virtualhost.VirtualHostAlias)
	 */
	public boolean matchInetAddr(VirtualHostAlias vh){
		boolean result = false;
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "matchInetAddr", vh);
		}
		try {
			if( _inetAddress == null || vh.getInetAddress() ==null){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(null, "matchInetAddr", "no match: _inetAddress=" + _inetAddress
							+ ", vh.getInetAddress()=" + vh.getInetAddress());
				}
				 return result = false;
			}
			
			String thisCanonicalName = _inetAddress.getCanonicalHostName();
			String otherCanonicalName = vh.getInetAddress().getCanonicalHostName();
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(null, "matchInetAddr", "thisCanonicalName="
						+ thisCanonicalName + ", otherCanonicalName=" + otherCanonicalName);
			}
			return result = thisCanonicalName.equalsIgnoreCase(otherCanonicalName);
		
		} finally {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(null, "matchInetAddr", result);
			}
		}
	}
	
	/**
	 * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#getInetAddress()
	 */
    public InetAddress getInetAddress() {
		return _inetAddress;
	}


	/**
     * @see java.lang.Object#toString()
     */
    public String toString(){
        if( hostPortStr == null){
        	
        	String hostToString = null;
        	String portToString = null;
        	
        	if (_isAnyHost ||  _isRegExp){
        		hostToString = _host;
        	}else {
        		hostToString = _ip;
        	}
        	
        	if (isAnyPort()) portToString = "*"; else portToString = Integer.toString(_port);
        	
        	hostPortStr = hostToString + ":" + portToString;
    		
        }
        return hostPortStr;
    }
    
    /**
     * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#getHost()
     */
	public String getHost() {
		return _host;
	}   
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#getRegExp()
	 */
	public Pattern getRegExp() {
		return _regExp;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.container.virtualhost.VirtualHostAlias#isRegExp()
	 */
	public boolean isRegExp() {
		return _isRegExp;
	}

	/**
	 * set the regexp flag
	 * 
	 * @param regExp
	 */
	public void setRegExp(boolean regExp) {
		_isRegExp = regExp;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof VirtualHostAliasImpl){
			VirtualHostAliasImpl other = (VirtualHostAliasImpl)o;
			
			return this._host.equals(other._host) && 
				   this._port == other._port;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(_host, _port);
	}
	
	/**
	 * Creates host alias string representation from input host and port.
	 * 
	 * @param host host alias host name
	 * @param port host alias port
	 * @return "host:port" host alias string
	 */
	public static String createHostAliasString(String host, String port){
		StringBuilder sb = new StringBuilder();
		sb.append(host).append(":").append(port);
		
		return sb.toString();
	}
}
