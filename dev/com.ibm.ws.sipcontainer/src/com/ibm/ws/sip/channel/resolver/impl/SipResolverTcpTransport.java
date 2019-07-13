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
package com.ibm.ws.sip.channel.resolver.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContextFactory;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * 
 * This class handles the details of using the UDP and TCP channel 
 * for network tranport for the SipResolver
 *
 */
class SipResolverTcpTransport implements ConnectionReadyCallback,
										TCPReadCompletedCallback, 
										TCPWriteCompletedCallback,
										SipResolverTransport 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipResolverTcpTransport.class);
	
	private static final int				READ_TIMEOUT = 	1500;
	private static final int				WRITE_TIMEOUT = 5000;
	private static final int				MAX_READ_TIMEOUT_COUNT = 5;
	private static final int				MAX_WRITE_QUEUE_SIZE = 5000;
	
	private static final int				WRITE_STATE_DISCONNECTED = 0;        
	private static final int				WRITE_STATE_CONNECTING = 1;        
	private static final int				WRITE_STATE_IDLE = 2;        
	private static final int				WRITE_STATE_WRITE_ACTIVE = 3;        
	private static final int				WRITE_STATE_SHUTDOWN = 4;  
	
	private static final int				READ_STATE_READING_LENGTH = 0;        
	private static final int				READ_STATE_READING_BODY = 1;
	private static final int				READ_STATE_DISCONNECTED = 2;
	private static final int				READ_STATE_SHUTDOWN = 3;

	private static String                   CHAINNAME = "SipResolver-tcp-outbound";        
	private static ChannelFramework         _framework;
	private	static boolean					_channelInitialized = false;
	
	private	boolean							_shutdown = false;
	
	private Vector<InetSocketAddress>  		_nameServers = null;
	private	Iterator<InetSocketAddress>		_nameServerIterator = null;
	private Queue<WsByteBuffer>        		_requestQueue = new LinkedList<WsByteBuffer>();
	private WsByteBuffer [] 				_bufferArray = new WsByteBuffer[2];
	private WsByteBuffer 					_lengthBuffer;
	private int			 					_outstandingRequestCount = 0;
	private int			 					_readTimeoutCount = 0;
    
    private boolean                         reConnectAllowed = false;
    
	private int			 					_connectionFailedCount = -1;
    private int                             _transportErrorCount = 0;

    // these allowed threasholds will be re-determine, using the number of avaliable
    // DNS servers, when the object is instantiated
	private int                             _ConnectFailuresAllowed = 2;
    private int                             _TransportErrorsAllowed = 3;
    
	private TCPWriteRequestContext          _writer = null;
	private TCPReadRequestContext           _reader = null;
	private	SipResolverTransportListener	_transportListener = null;
	private OutboundVirtualConnection 		_outboundVirtualContext;
	
	private	int								_writeState = WRITE_STATE_DISCONNECTED;
	private	int								_readState = READ_STATE_DISCONNECTED;
	
	private InetSocketAddress 				_currentSocketAddress = null;

    
	synchronized protected static void initialize(ChannelFramework framework){
		if (_channelInitialized == false)
		{
			try {
				
				
			
		
				/** Create the channel configuration */
			//	framework.addChannel(CHAINNAME, TCPChannelFactory.class, null, 10);
				framework.addChannel(CHAINNAME,framework.lookupFactory("TCPChannel"), null, 10);
				/** Create the chain configuration */
				String [] channelNameList = {CHAINNAME};
				framework.addChain(CHAINNAME, FlowType.OUTBOUND, channelNameList);
				
				_framework = framework;
			//	framework.get
			}
			catch (ChannelException e){
			    if (c_logger.isWarnEnabled())
			    	c_logger.warn("Resolver channel exception during init: " + e.getMessage());
			}
			catch (ChainException e1){
			    if (c_logger.isWarnEnabled())
					c_logger.warn("Resolver channel exception during init: " + e1.getMessage());
			}
			
			_channelInitialized = true;
		}
	}
	
	protected SipResolverTcpTransport(	Vector<InetSocketAddress> 		nameServers,
			SipResolverTransportListener	transportListener, CHFWBundle chfwB){

	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: constructor: entry: id="+hashCode());

	   
	    initialize(chfwB.getFramework());
	    _lengthBuffer = chfwB.getBufferManager().allocate(2);
		_nameServers = nameServers;
		_nameServerIterator = _nameServers.iterator();
		_transportListener = transportListener;
		
        _ConnectFailuresAllowed = _nameServers.size() * 2;
        _TransportErrorsAllowed = _nameServers.size() * 3;

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("SipResolverTcpTransport: contructor: _ConnectFailuresAllowed: " + _ConnectFailuresAllowed);
            c_logger.traceDebug("SipResolverTcpTransport: contructor: _TransportErrorsAllowed: " + _TransportErrorsAllowed);
        }

        // prepare to connect on next write request, but don't connect now since
        // no request has been written yet.
        reConnectAllowed = true;
        _writeState = WRITE_STATE_DISCONNECTED;
	    
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: constructor: entry");
	}
	
	synchronized protected void shutdown()
	{
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: shutdown: entry: id="+hashCode());

	    _shutdown = true;
		_requestQueue.clear();
		_writeState = WRITE_STATE_SHUTDOWN;
		_readState = READ_STATE_SHUTDOWN;
        
		if (_outboundVirtualContext != null)
		{
            _outboundVirtualContext.close(new IOException("SIP Resolver is being shutdown"));
			_outboundVirtualContext = null;
		}

	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: shutdown: exit");
	}

	/**
	 * 
	 */
	synchronized private void connect()
	{
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: connect: entry: id="+hashCode());

		if (_outboundVirtualContext != null)
		{
			_outboundVirtualContext.close(new IOException("Connection not responding properly"));
			_outboundVirtualContext = null;
		}
		
		//	move to next name server if last one failed, or we have just been instantiated.
        if(_connectionFailedCount != 0)
		{
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: connect: Find DNS Server in list");

            _connectionFailedCount = 0;
            
			if (_nameServerIterator.hasNext() == false)
			{
				_nameServerIterator = _nameServers.iterator();
				_currentSocketAddress = _nameServerIterator.next();
			}
			else
				_currentSocketAddress = _nameServerIterator.next();
		}

		try {
			_writeState = WRITE_STATE_CONNECTING;
			_readState = READ_STATE_DISCONNECTED;
			_outstandingRequestCount = 0;
			_readTimeoutCount = 0;
			
			VirtualConnectionFactory vcf = _framework.getOutboundVCFactory(CHAINNAME);
			_outboundVirtualContext = (OutboundVirtualConnection)vcf.createConnection();
			
		    if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("SipResolverTcpTransport:connect: SIP Resolver is connecting to: " + _currentSocketAddress.getHostName() + ":" + _currentSocketAddress.getPort());
			
			TCPConnectRequestContext context = TCPConnectRequestContextFactory.getRef().createTCPConnectRequestContext(_currentSocketAddress.getHostName(),
					_currentSocketAddress.getPort(), 10);
			
			_reader = ((TCPConnectionContext) _outboundVirtualContext.getChannelAccessor()).getReadInterface();
			_writer = ((TCPConnectionContext) _outboundVirtualContext.getChannelAccessor()).getWriteInterface();

			_outboundVirtualContext.connectAsynch(context, this);
		}

        catch (ChannelException e){
		    if (c_logger.isWarnEnabled())
				c_logger.warn("Resolver channel exception during connect: " + e.getMessage());
		}
		catch (ChainException e1){
		    if (c_logger.isWarnEnabled())
				c_logger.warn("Resolver chain exception during connect: " + e1.getMessage());
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: connect: exit: id="+hashCode());
	}

	/**
     * 
     */
    synchronized public void writeRequest(WsByteBuffer requestBuffer) throws IOException
    {
        if (c_logger.isTraceEntryExitEnabled())
    		c_logger.traceEntry(this, "SipResolverTcpTransport: writeRequest: entry: id="+hashCode());
    
        if (_shutdown == true)
    		throw new IllegalStateException("SIP TCP Resolver transport is shutdown.");
    	
    	switch (_writeState)
    	{
    		case WRITE_STATE_SHUTDOWN:
    			//	unreachable? debug only
    		    if (c_logger.isTraceDebugEnabled())
    				c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_SHUTDOWN");
    			break;
    			
    		case WRITE_STATE_IDLE:
                VirtualConnection vc = null;

    		    if (c_logger.isTraceDebugEnabled())
    				c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_IDLE");
    			_lengthBuffer.clear();
    			_lengthBuffer.limit(2);
    			_lengthBuffer.putShort((short)requestBuffer.limit());
    			_lengthBuffer.position(0);
    
    			_bufferArray[0] = _lengthBuffer;
    			_bufferArray[1] = requestBuffer;
    			
    			_writer.setBuffers(_bufferArray);
    			_outstandingRequestCount++;

                vc =_writer.write(TCPWriteRequestContext.WRITE_ALL_DATA, this, false, WRITE_TIMEOUT);
                if (vc == null) {
                    // write did not complete right away
                    _writeState = WRITE_STATE_WRITE_ACTIVE;
                } else {
                    // write compelted so call complete from here
                    complete(vc, _writer);
                }

    			break;
    
    		case WRITE_STATE_WRITE_ACTIVE:
    		    if (c_logger.isTraceDebugEnabled())
    				c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_WRITE_ACTIVE");
    			if (_requestQueue.size() > MAX_WRITE_QUEUE_SIZE)
    				throw new IOException ("Maximum write queue size is being exceeded");
    			_requestQueue.add(requestBuffer);
    			break;
    			
    		case WRITE_STATE_CONNECTING:
    		    if (c_logger.isTraceDebugEnabled())
    				c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_CONNECTING");
    			_requestQueue.add(requestBuffer);
    			break;
    
    		case WRITE_STATE_DISCONNECTED:
    		    if (c_logger.isTraceDebugEnabled())
    				c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_DISCONNECTED");
    			_requestQueue.add(requestBuffer);

                if (reConnectAllowed) {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport:writeRequest: (re)connect to DNS server");
                    connect();
                }
    			
                break;
    	}
        if (c_logger.isTraceEntryExitEnabled())
    		c_logger.traceExit(this, "SipResolverTcpTransport: writeRequest: exit");
    }

    /*
	 * This method is called when the socket is set up and ready to send and receive data on.
	 * 
	 * @see com.ibm.wsspi.channel.ConnectionReadyCallback#ready(com.ibm.wsspi.channel.framework.VirtualConnection)
	 */
	public void ready(VirtualConnection vc ){
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: ready: entry: id="+hashCode());

	    if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverTcpTransport:ready: socket is ready");
	    
	    if (c_logger.isEventEnabled())
			c_logger.info("info.sip.resolver.established.connection", null, _currentSocketAddress.toString());
	    
	    
        reConnectAllowed = false;
        _connectionFailedCount = 0;
        
        //  First, issue a read with a forced callback
		_readState = READ_STATE_READING_LENGTH;
		_reader.setJITAllocateSize(2);
		_reader.setBuffer(null);

		_reader.read(2, this , true, READ_TIMEOUT);
		
		drainRequestQueue();
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: ready: exit");
	}
	
	/*
	 * This method is called when the socket connection fails during setup.
	 * 
	 * @see com.ibm.wsspi.channel.ConnectionReadyCallback#ready(com.ibm.wsspi.channel.framework.VirtualConnection)
	 */
	public void destroy(Exception e){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: destroy: entry: id="+hashCode());

		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverTcpTransport: Connection failed to establish: " + e);

	    if (c_logger.isWarnEnabled())
			c_logger.warn("warn.sip.resolver.failed.connection", null, _currentSocketAddress.toString());

	    _connectionFailedCount++;
	    _writeState = WRITE_STATE_DISCONNECTED;
		_outboundVirtualContext = null;
		
        if (_connectionFailedCount <= _ConnectFailuresAllowed)
		{
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: calling transportError - _connectionFailedCount: " + _connectionFailedCount);
            
            // try to rollover to the next name server
            _transportListener.transportError(e, this);
		}
		else {
            //  can't connect to any name serves.
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: calling transportFailed - _connectionFailedCount: " + _connectionFailedCount);
            
            _transportErrorCount = 0;
            _connectionFailedCount = 0;
            _transportListener.transportFailed(e, this);
        }
        
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: destroy: exit");
	}


	/**
	 * 
	 */
	public void complete(VirtualConnection vc, TCPReadRequestContext rsc){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: complete: entry: id="+hashCode());

		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverTcpTransport: complete: _readState: "+ _readState);

		_readTimeoutCount = 0;	// Set this back to 0 so no errors are detected.

        _connectionFailedCount = 0;
        _transportErrorCount = 0;
        
		boolean exit = false;
		while (exit == false)
		{
			switch (_readState)
			{
				case READ_STATE_READING_LENGTH:
					_reader.getBuffer().flip();
					short length = _reader.getBuffer().getShort();
					
					_readState = READ_STATE_READING_BODY;
					_reader.setJITAllocateSize(length);
					_reader.setBuffer(null);

                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport: complete: doing read length of: " + length);
                    
					if (_reader.read(length, this , false, READ_TIMEOUT) == null) {
						exit = true;
					}
					
					break;
					
				case READ_STATE_READING_BODY:
					if (_outstandingRequestCount != 0)
						_outstandingRequestCount--;
					else
					{
						if (c_logger.isTraceDebugEnabled())
							c_logger.traceDebug("SipResolverTcpTransport: complete: error: outstandingRequestCount can't decrement past 0");
					}
					
					_reader.getBuffer().flip();
					
					_transportListener.responseReceived(_reader.getBuffer());
					
					_readState = READ_STATE_READING_LENGTH;
					_reader.setJITAllocateSize(2);
					_reader.setBuffer(null);

                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport: complete: doing new read for length");
					
                    if (_reader.read(2, this , false, READ_TIMEOUT) == null)
						exit = true;
					break;
					
				case READ_STATE_DISCONNECTED:
				case READ_STATE_SHUTDOWN:
						exit = true;
					break;
			}
		}
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: complete: exit: id="+hashCode());
	}

	/**
	 * 
	 */
	public void error(VirtualConnection vc, TCPReadRequestContext rrc, IOException ioe) {
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: error(vc, read context, exception) ");

		if (_shutdown == false)
		{	
			if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug("SipResolverTcpTransport: read error: exception "+ioe);
            
			if ((ioe instanceof java.net.SocketTimeoutException) ||
				(ioe instanceof com.ibm.io.async.AsyncTimeoutException))
			{
				if ((_outstandingRequestCount > 0) || (_readTimeoutCount > MAX_READ_TIMEOUT_COUNT) || (_readState == READ_STATE_READING_BODY))
				{
					if (c_logger.isWarnEnabled() && (_outstandingRequestCount > 0))
						c_logger.warn("warn.sip.resolver.server.not.responding", null, _currentSocketAddress.toString());

                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport: error: consecutive read timeouts: " + _readTimeoutCount);
                    
					//	Here we need to tear down the connection and notify the user of this class
					IOException exception = new IOException("Server stopped responding. Closing connection.");
					_outboundVirtualContext.close(exception);
					_outboundVirtualContext = null;

                    _transportErrorCount++;

                    /** try the next nameserver on the connect if these aren't idle timeouts*/
					if ((_outstandingRequestCount > 0) || (_readState == READ_STATE_READING_BODY)){
						_connectionFailedCount++;
					}
                    
					_readState = READ_STATE_DISCONNECTED;
					_writeState = WRITE_STATE_DISCONNECTED;
                    
                    if (_transportErrorCount < _TransportErrorsAllowed) {
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug("SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: " + _transportErrorCount);

                        _transportListener.transportError(ioe, this);
                        
                    } else {
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug("SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);

                        _transportErrorCount = 0;
                        _connectionFailedCount = 0;
                        _transportListener.transportFailed(ioe, this);
                    }
					
				}
				else 
				{
					if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("SipResolverTcpTransport: error: incrementing readTimeoutCount: " + _readTimeoutCount);
	
					_readTimeoutCount++;
					
					//	Reissue the read and keep going
					_reader.read(2, this , true, READ_TIMEOUT);
				}
			}
			else
			{
				_readState = READ_STATE_DISCONNECTED;
				_writeState = WRITE_STATE_DISCONNECTED;

                _transportErrorCount++;

                // increment connection failed count to force rollover.
                _connectionFailedCount++;
                
                if (_transportErrorCount < _TransportErrorsAllowed) {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: " + _transportErrorCount);

                    _transportListener.transportError(ioe, this);
                    
                } else {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);
                    
                    _transportErrorCount = 0;
                    _connectionFailedCount = 0;
                    _transportListener.transportFailed(ioe, this);
                }
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: error(vc, read context, exception)");
	}

    
    public void prepareForReConnect() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: prepareForReConnect");

        //  clear the request queue of all outstanding request.
        //  We can only clear when the object using us tell us to clear,
        //  ohterwise we risk timing windows whereby we clear out requests
        //  which were menat for rollover, or we fail to clear out an attempt
        //  that will be retried.. 
        _requestQueue.clear();

        // only allow reconnects once we know the request queue will only hold
        // new valid attempts.
        reConnectAllowed = true;

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: prepareForReConnect");
}
    
    
	/**
	 * 
	 */
	public void complete(VirtualConnection vc, TCPWriteRequestContext wrc){
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: complete: write complete id="+hashCode());

		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverTcpTransport: complete: write completed sucessfully: "+hashCode());
        
        _connectionFailedCount = 0;
        _transportErrorCount = 0;
		drainRequestQueue();

		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: complete: write complete id="+hashCode());
	}

	/**
	 * 
	 */
	public void error(VirtualConnection vc, TCPWriteRequestContext wrc, IOException ioe){		
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: error: write error id="+hashCode());

		if (_shutdown == true)
			return;

        _transportErrorCount++;

        // increment connection failed count to force rollover.
        _connectionFailedCount++;
        
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverTcpTransport: error: write failed: "+hashCode());

        
        _writeState = WRITE_STATE_DISCONNECTED;
        
        if (_transportErrorCount < _TransportErrorsAllowed) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: " + _transportErrorCount);
            
		    _transportListener.transportError(ioe, this);
            
        } else {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);

            _transportErrorCount = 0;
            _connectionFailedCount = 0;
            _transportListener.transportFailed(ioe, this);
        }

		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: error: write error id="+hashCode());
	}

	/**
	 *
	 */
	synchronized private void drainRequestQueue()
	{
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverTcpTransport: drainRequestQueue: entry: id="+hashCode());

		while (true)
		{
			WsByteBuffer requestBuffer = _requestQueue.poll();
			
			if (requestBuffer != null)
			{
				_lengthBuffer.clear();
				_lengthBuffer.limit(2);
				_lengthBuffer.putShort((short)requestBuffer.limit());
				_lengthBuffer.position(0);

				_bufferArray[0] = _lengthBuffer;
				_bufferArray[1] = requestBuffer;
						
				_writer.setBuffers(_bufferArray);
						
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverTcpTransport:drainRequestQueue: writing new message, length = " + requestBuffer.limit());

				_outstandingRequestCount++;
			    VirtualConnection vc = _writer.write(-1, this, false, 60000);
				
				if (vc == null) 
				{
				    if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("SipResolverTcpTransport:drainRequestQueue: waiting for write to complete");

				    _writeState = WRITE_STATE_WRITE_ACTIVE;
					break;
				}
			}
			else
			{
				_writeState = WRITE_STATE_IDLE;
				break;
			}
		}

		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverTcpTransport: drainRequestQueue: exit");
	}

	

	
}
