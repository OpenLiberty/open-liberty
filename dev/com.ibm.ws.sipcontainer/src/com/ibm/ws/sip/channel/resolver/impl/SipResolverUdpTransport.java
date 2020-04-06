/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import com.ibm.wsspi.udpchannel.UDPReadCompletedCallback;
import com.ibm.wsspi.udpchannel.UDPReadRequestContext;
import com.ibm.wsspi.udpchannel.UDPWriteCompletedCallback;
import com.ibm.wsspi.udpchannel.UDPWriteRequestContext;
import com.ibm.wsspi.udpchannel.UDPContext;

/**
 * 
 * This class handles the details of using the UDP and TCP channel 
 * for network tranport for the SipResolver
 *
 */
class SipResolverUdpTransport implements ConnectionReadyCallback,
                                         UDPReadCompletedCallback,
                                         UDPWriteCompletedCallback,
                                         SipResolverTransport {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipResolverUdpTransport.class);
	        
	private static ChannelFramework         _framework;
		
	private static final int				WRITE_STATE_DISCONNECTED = 0;        
	private static final int				WRITE_STATE_CONNECTING = 1;        
	private static final int				WRITE_STATE_IDLE = 2;        
	private static final int				WRITE_STATE_WRITE_ACTIVE = 3;        
	private static final int				WRITE_STATE_SHUTDOWN = 4;  
	    
	private static final int				READ_STATE_READY = 1;
	private static final int				READ_STATE_DISCONNECTED = 2;
	private static final int				READ_STATE_SHUTDOWN = 3;

	private static final int				MAX_WRITE_QUEUE_SIZE = 5000;
	
	private	SipResolverTransportListener	_transportListener = null;
	private Vector<InetSocketAddress>  		_nameServers = null;
	private	Iterator<InetSocketAddress>		_nameServerIterator = null;
	private	int								_writeState = WRITE_STATE_DISCONNECTED;
	private	int								_readState = READ_STATE_DISCONNECTED;
	private	boolean							_shutdown = false;
	private Queue<WsByteBuffer>        		_requestQueue = new LinkedList<WsByteBuffer>();
	private OutboundVirtualConnection 		_outboundVirtualContext;

	private UDPReadRequestContext           _reader;
	private UDPWriteRequestContext          _writer;
	
	private	static boolean					_channelInitialized = false;
    protected InetSocketAddress             _currentSocketAddress = null;
	
	private static String                   CHAINNAME = "SipResolver-udp-outbound";

    private boolean                         reConnectAllowed = true;
    private int                             _connectionFailedCount = -1;
    private int                             _transportErrorCount = 0;
    private boolean                         connectDone = false;
    
    // these allowed threasholds will be re-determine, using the number of avaliable
    // DNS servers, when the object is instantiated
    private int                             _ConnectFailuresAllowed = 2;
    private int                             _TransportErrorsAllowed = 3;
    
//    protected int                           STATE_OFF = 0;
//    protected int                           STATE_STARTED = 1;
//    protected int                           timeoutState = STATE_OFF;
//    protected int                           timeoutThreadCount = 0;
//    protected Timeout                       timeout = null;
    
//    protected Object                        accessTimeout = new Object();
    protected int                           timeoutIdleCount = 0;
    protected int                           TIMEOUT_IDLE_COUNT_MAX = 3;
//    protected int                           timeoutRequests = 0;
    protected boolean                       writeNeedsResponse = false;
    
    // if DNS has not responded to our request for TIMEOUT_TIME, then try to
    // rollover to new DNS
    protected static final int              TIMEOUT_TIME = 3000;



    
	protected SipResolverUdpTransport(){
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "SipResolverUdpTransport: constructor()");
            c_logger.traceExit(this, "SipResolverUdpTransport: constructor()");
        }    
	}
	
	synchronized protected void initialize(ChannelFramework framework){

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: initialize() _channelInitialized:" + _channelInitialized);

        if (_channelInitialized == false)
		{
			try {
			
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: initialize: getChannelFramewor()");

				/** Create the channel configuration */
                framework.addChannel(CHAINNAME,framework.lookupFactory("UDPChannel"), null, 10);
		
				/** Create the chain configuration */
				String [] channelNameList = {CHAINNAME};
				framework.addChain(CHAINNAME, FlowType.OUTBOUND, channelNameList);
				
				_framework = framework;

                _writeState = WRITE_STATE_DISCONNECTED;
                _readState = READ_STATE_DISCONNECTED;
                reConnectAllowed = true;
                
                VirtualConnectionFactory vcf = _framework.getOutboundVCFactory(CHAINNAME);
                _outboundVirtualContext = (OutboundVirtualConnection)vcf.createConnection();

                _reader = ((UDPContext)_outboundVirtualContext.getChannelAccessor()).getReadInterface();
                _writer = ((UDPContext)_outboundVirtualContext.getChannelAccessor()).getWriteInterface();
                    
            }
            
			catch (ChannelException e){
			    if (c_logger.isWarnEnabled()){
			    	c_logger.warn("Udp Resolver channel exception during init: " + e.getMessage());
					e.printStackTrace();
			    }
			}
			catch (ChainException e1){
			    if (c_logger.isWarnEnabled())
			    	c_logger.warn("Udp Resolver channel exception during init: " + e1.getMessage());
			}
			_channelInitialized = true;
		}
        
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: initialize()");
	}
	
	protected SipResolverUdpTransport(Vector<InetSocketAddress>	nameServers,
			                          SipResolverTransportListener transportListener, CHFWBundle chfwB){

	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverUdpTransport: constructor(Vector, SipResolverTransportListener): entry: id="+hashCode() + " " +chfwB);

	    /** setup the channel fw */
	    initialize(chfwB.getFramework());

		_nameServers = nameServers;
		_nameServerIterator = _nameServers.iterator();
		_transportListener = transportListener;
	    
        _ConnectFailuresAllowed = _nameServers.size() * 2;
        _TransportErrorsAllowed = _nameServers.size() * 3;
        
        if ( c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("SipResolverTcpTransport: contructor: _ConnectFailuresAllowed: " + _ConnectFailuresAllowed);
            c_logger.traceDebug("SipResolverTcpTransport: contructor: _TransportErrorsAllowed: " + _TransportErrorsAllowed);
        }
        
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverUdpTransport: constructor(Vector, SipResolverTransportListener): entry: ");
	}
	
	synchronized protected void shutdown()
	{
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverUdpTransport: shutdown: entry: id="+hashCode());

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
			c_logger.traceExit(this, "SipResolverUdpTransport: shutdown: exit: ");
	}

    /**
     * 
     */
    private void talkToDNS()
    {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: talkToDNS: entry: id="+hashCode());

        // move to next name server if last one failed, or the first server if
        // this is the first time in here.
        if ( c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: Find DNS Server in list");

        if (_nameServerIterator.hasNext() == false)
        {
            _nameServerIterator = _nameServers.iterator();
            _currentSocketAddress = _nameServerIterator.next();
        }
        else {
            _currentSocketAddress = _nameServerIterator.next();
        }

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: SIP Resolver nameserver target: " + _currentSocketAddress.getHostName() + ":" + _currentSocketAddress.getPort());

        if (connectDone) {
            
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: connectAsynch called go right to ready()");
            ready(_outboundVirtualContext);
            
        } else {
            
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: connectAsynch not called yet, do so now");

            /** open the listener socket */
            _outboundVirtualContext.connectAsynch(null, this);
            
        }
        
        
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: talkToDNS: exit: ");
    }


    public void ready(VirtualConnection vc ){

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: ready: entry: id="+hashCode());

        connectDone = true;
        _writeState = WRITE_STATE_IDLE;
        _readState = READ_STATE_READY;

        _connectionFailedCount = 0;
        reConnectAllowed = false;
        
        // start timeout processing if it hasn't started yet.
//        timeoutProcessingStart();
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: ready: UDP read request");
        _reader.read(this , true);

        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: ready: exit: ");

    }
    
    
    // This method is called by the connection callback when the connectAsynch fails
    public void destroy(Exception e){
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: destroy(Exception e)");

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: destroy: Socket destroyed " + e);

        _readState = READ_STATE_DISCONNECTED;
        _writeState = WRITE_STATE_DISCONNECTED;
        
        _connectionFailedCount++;

        if (_connectionFailedCount <= _ConnectFailuresAllowed) {
            if ( c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _connectionFailedCount: " + _connectionFailedCount);

            // try to rollover to the next name server
            _transportListener.transportError(e, this);
        }
        else {
            if ( c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: error: calling transportFailed - _connectionFailedCount: " + _connectionFailedCount);

            //  can't connect to any name serves.
            _connectionFailedCount = 0;
            _transportErrorCount = 0;
            _transportListener.transportFailed(e, this);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: destroy(Exception e)");
    }

    
    /**
	 * 
	 */
	synchronized public void writeRequest(WsByteBuffer requestBuffer) throws IOException
	{
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverUdpTransport: writeRequest: entry id="+hashCode());

	    if (_shutdown == true)
			throw new IllegalStateException("SIP UDP Resolver transport is shutdown.");
		
		switch (_writeState)
		{
			case WRITE_STATE_SHUTDOWN:
				//	unreachable?  debug only
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_SHUTDOWN");
                
				break;
				
			case WRITE_STATE_IDLE:
                VirtualConnection vc = null;
                                
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_IDLE");
					
//                timeoutProcessingStart();                
 
                _writer.setBuffer(requestBuffer);
                
//                synchronized(accessTimeout) {
//                    writeToBeRequested();
                    vc = _writer.write(_currentSocketAddress, this, false);
//                }
                
                if (vc == null) {
                    // write did not complete right away
                    _writeState = WRITE_STATE_WRITE_ACTIVE;
                } else {
                    // write completed so call complete from here
                    complete(vc, _writer);
                }
					
				break;

			case WRITE_STATE_WRITE_ACTIVE:
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_WRITE_ACTIVE");
                
				if (_requestQueue.size() > MAX_WRITE_QUEUE_SIZE)
					throw new IOException ("Maximum write queue size is being exceeded");
				_requestQueue.add(requestBuffer);
				break;
				
			case WRITE_STATE_CONNECTING:
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_CONNECTING");
                
				_requestQueue.add(requestBuffer);
				break;

			case WRITE_STATE_DISCONNECTED:
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_DISCONNECTED");

//                timeoutProcessingStart();                

				_requestQueue.add(requestBuffer);
                
                // only rollover, or try again, once the request queue has been reset
                if (reConnectAllowed) {
				    // open();
                    talkToDNS();
                }    
				break;
		}
        
	    if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceExit(this, "SipResolverUdpTransport: writeRequest: exit: ");
	}
			

    // This method is called by the timeout watcher when no response has been received.
    public void destroyFromTimeout(Exception e){
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: destroyFromTimeout(Exception e)");

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: destroyFromTimeout: Socket destroyed " + e);

        _readState = READ_STATE_DISCONNECTED;
        _writeState = WRITE_STATE_DISCONNECTED;
        
        _transportErrorCount ++;

        if (_transportErrorCount <= _TransportErrorsAllowed) {
            if ( c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: destroyFromTimeout: calling transportError - _transprtErrorCount: " + _transportErrorCount);

            _transportListener.transportError(e, this);
        } else {
            if ( c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: destroyFromTimeout: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);

            _transportErrorCount = 0;
            _transportListener.transportFailed(e, this);
        }
        
        
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: destroyFromTimeout(Exception e)");
    }
    
    
    public void prepareForReConnect() {
        if ( c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: prepareForReConnect");

        //  clear the request queue of all outstanding request.
        //  We can only clear when the object using us tell us to clear,
        //  ohterwise we risk timing windows whereby we clear out requests
        //  which were menat for rollover, or we fail to clear out an attempt
        //  the will later be retried. 
        if ( c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: prepareForReConnect: clear out _requestQueue: # of items: " + _requestQueue.size());
        _requestQueue.clear();

        // only allow reconnects once we know the request queue will only hold
        // new valid attempts.
        reConnectAllowed = true;

        if ( c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: prepareForReConnect");
    }
    
    
	public void complete(VirtualConnection vc, UDPReadRequestContext rsc){
		
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: complete(VirtualConnection vc, UDPReadRequestContext rsc) _readState: " + _readState);

        _transportErrorCount = 0;
        vc = null;
        
		// let the time out thread know we received a DNS response 
//        writeRespondedTo();
        
		_reader.getUDPBuffer().getBuffer().flip();
		_transportListener.responseReceived(_reader.getUDPBuffer().getBuffer());
		
		if (_readState != READ_STATE_DISCONNECTED){
			_readState = READ_STATE_READY;
            
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: complete: read message body");

//            timeoutProcessingStart();
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: complete: UDP read request");
            vc = _reader.read(this , false);
            
            while (true) {
                if (vc != null) {
                    // let the time out thread know we received a DNS response 
//                    writeRespondedTo();
              
                    if (_shutdown == false) {
                        
                        _reader.getUDPBuffer().getBuffer().flip();
                        _transportListener.responseReceived(_reader.getUDPBuffer().getBuffer());

                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug("SipResolverUdpTransport: complete: read next response");

//                        timeoutProcessingStart();
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug("SipResolverUdpTransport: complete: looping  UDP read request");
                        vc = _reader.read(this , false);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        
        
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: complete(VirtualConnection vc, UDPReadRequestContext rsc)");
	}

     
	public void error(VirtualConnection vc, UDPReadRequestContext rrc, IOException ioe) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: error(VirtualConnection vc, UDPReadRequestContext rrc, IOException ioe)");

        if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipResolverUdpTransport: Read error: " + ioe);
		
        // let the time out thread know we received a DNS response 
//        writeRespondedTo();
        
		if (_shutdown == false)
		{	
            _readState = READ_STATE_DISCONNECTED;
            _writeState = WRITE_STATE_DISCONNECTED;

            _transportErrorCount ++;
            if (_transportErrorCount < _TransportErrorsAllowed) {
                if ( c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _transprtErrorCount: " + _transportErrorCount);

                _transportListener.transportError(ioe, this);

            } else {
                if ( c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);

                _transportErrorCount = 0;
                _transportListener.transportFailed(ioe, this);
            }
		}

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: error(VirtualConnection vc, UDPReadRequestContext rrc, IOException ioe)");
	}
	
	public void complete(VirtualConnection vc, UDPWriteRequestContext wrc){
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: complete(VirtualConnection vc, UDPWriteRequestContext wrc)");

        _transportErrorCount = 0;

        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: complete(VirtualConnection vc, UDPWriteRequestContext wrc)");
	}
	
	public void error(VirtualConnection vc, UDPWriteRequestContext wrc, IOException ioe){
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: error(VirtualConnection vc, UDPWriteRequestContext wrc, IOException ioe)");
	    if (c_logger.isTraceDebugEnabled())
		    c_logger.traceDebug("SipResolverUdpTransport: Write error: " + ioe);
        
        if (_shutdown == false)
        {   
            _readState = READ_STATE_DISCONNECTED;
            _writeState = WRITE_STATE_DISCONNECTED;
                    
            _transportErrorCount ++;
            if (_transportErrorCount < _TransportErrorsAllowed) {
                if ( c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _transprtErrorCount: " + _transportErrorCount);

                _transportListener.transportError(ioe, this);

            } else {
                if ( c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportFailed - _transprtErrorCount: " + _transportErrorCount);

                _transportErrorCount = 0;
                _transportListener.transportFailed(ioe, this);
            }
        }
            
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "SipResolverUdpTransport: error(VirtualConnection vc, UDPWriteRequestContext wrc, IOException ioe)");
        }    
	}
	
	/**
	 *
	 */
	synchronized private void drainRequestQueue()
	{
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverUdpTransport: drainRequestQueue: entry _writeState: " + _writeState);

		while (true)
		{
			WsByteBuffer requestBuffer = _requestQueue.poll();
			
			if (requestBuffer != null)
			{
                VirtualConnection vc = null;
				_writer.setBuffer(requestBuffer);
						
			    if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug("SipResolverUdpTransport:drainRequestQueue: writing new message, length = " + requestBuffer.limit());
                
//                synchronized(accessTimeout) {
//                    writeToBeRequested();
                    vc = _writer.write(_currentSocketAddress, this, false);
//                }
                
				if (vc == null) 
				{
				    if (c_logger.isTraceDebugEnabled())
						c_logger.traceDebug("SipResolverUdpTransport:drainRequestQueue: waiting for write to complete");

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
			c_logger.traceExit(this, "SipResolverUdpTransport: drainRequestQueue: exit _writeState: " + _writeState);
	}



    
//    protected void timeoutProcessingStart() {
//        synchronized(accessTimeout) {
//            
//            if (c_logger.isTraceDebugEnabled())
//                c_logger.traceDebug("SipResolverUdpTransport: timeoutProcessingStart: writeNeedsResponse= " + writeNeedsResponse);
//
//            if (timeoutState == STATE_OFF) {
//                timeoutState = STATE_STARTED;
//                timeoutIdleCount = 0;
//                timeout = new Timeout();
//                
//                PrivilegedThreadStarter privThread = new PrivilegedThreadStarter();
//                AccessController.doPrivileged(privThread);
//            }
//        }    
//    }
    
//    private void writeToBeRequested() {
//        // caller must be holding accessTimeout lock
//        writeNeedsResponse = true;
//        if (c_logger.isTraceDebugEnabled())
//            c_logger.traceDebug("SipResolverUdpTransport: writeToBeRequested:  writeNeedsResponse = true");
//    }

//    private void writeRespondedTo() {
//        synchronized(accessTimeout) {
//            timeoutIdleCount = 0;
//            writeNeedsResponse = false;
//            if (c_logger.isTraceDebugEnabled())
//                c_logger.traceDebug("SipResolverUdpTransport: writeRespondedTo:  writeNeedsResponse = false");
//        }    
//    }

//    class PrivilegedThreadStarter implements PrivilegedAction {
//        public PrivilegedThreadStarter() {
//            // do nothing
//        }
//        
//        public Object run() {
//            Thread t = new Thread(timeout);
//            timeoutThreadCount++;
//            String name = "SipResolverUDPTimeout-" + timeoutThreadCount;
//            t.setName(name);
//
//            // daemon threads will allow JVM to exit
//            t.setDaemon(true);
//
//            if (c_logger.isTraceDebugEnabled())
//                c_logger.traceDebug("SipResolverUdpTransport: start Timeout thread: " + name);
//            
//            t.start();
//            return null;
//        }
//    }
//
//
//    
//    class Timeout implements Runnable {
//        
//        public void run() {
//            if (c_logger.isTraceEntryExitEnabled())
//                c_logger.traceEntry(this, "SipResolverUdpTransport: Timeout run()");
//            long oldTime; 
//            long newTime;
//            long sleepTime;
//            
//            while (true) {
//                sleepTime = TIMEOUT_TIME;
//                oldTime = QuickApproxTime.getRef().getApproxTime();
//                
//                do {
//                    try {
//                        Thread.sleep(sleepTime);
//                    } catch (InterruptedException x) {
//                        // ignore
//                    }
//                    newTime = QuickApproxTime.getRef().getApproxTime();
//                    sleepTime = 1000;
//                } while(newTime - oldTime < TIMEOUT_TIME);
//          
//                if (checkTimeoutOccurred() == true) {
//                    // told to quit by called method due to error or inactivity
//                    break;
//                }
//            }
//            
//            if (c_logger.isTraceEntryExitEnabled())
//                c_logger.traceExit(this, "SipResolverUdpTransport: Timeout run()");
//        }
//    
//        public boolean checkTimeoutOccurred() {
//            synchronized(accessTimeout) {
//                timeoutIdleCount++;
//                
//                if (c_logger.isTraceDebugEnabled()) {
//                    c_logger.traceDebug("SipResolverUdpTransport: checkTimeoutOccurred: writeNeedsResponse= " + writeNeedsResponse);
//                    c_logger.traceDebug("SipResolverUdpTransport: checkTimeoutOccurred: timeoutIdleCount= " + timeoutIdleCount);
//                }
//                
//                if (timeoutIdleCount > 1) {
//                    // It has been at least one timeout period without hearing from the DNS
//                    if (writeNeedsResponse) {
//                        // We have at least one request to the DNS outstanding.  Assume
//                        // it is not responding
//                        destroyFromTimeout(new Exception("Name server not responding"));
//                        
//                        // exit timeout thread due to error
//                        if (c_logger.isTraceDebugEnabled()) {
//                            c_logger.traceDebug("SipResolverUdpTransport: checkTimeoutOccurred: exit timeout due to error");
//                        }
//                        timeoutState = STATE_OFF;
//                        return true;
//                        
//                    } else if (timeoutIdleCount > TIMEOUT_IDLE_COUNT_MAX) {
//                        // exit due to inactivity
//                        if (c_logger.isTraceDebugEnabled()) {
//                            c_logger.traceDebug("SipResolverUdpTransport: checkTimeoutOccurred: exit timeout due to inactivity");
//                        }
//                        timeoutState = STATE_OFF;
//                        return true;
//                    }    
//                }
//            }
//            return false;
//        }        
//        
//    }


}