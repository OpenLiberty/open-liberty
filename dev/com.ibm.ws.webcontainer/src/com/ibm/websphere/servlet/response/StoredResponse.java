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
package com.ibm.websphere.servlet.response;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.servlet.IncludedResponse;
import com.ibm.ws.webcontainer.srt.SRTOutputStream;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IOutputMethodListener;
import com.ibm.wsspi.webcontainer.servlet.ServletResponseExtended;
import com.ibm.wsspi.webcontainer.util.ByteBufferOutputStream;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;
import com.ibm.wsspi.webcontainer.util.IOutputStreamObserver;
import com.ibm.wsspi.webcontainer.util.IResponseOutput;

/**
 * 
 * StoredResponse is a response object that can be instantiated
 * by any servlet and used/passed as a standard HttpResponse. The
 * data that is written to this response will be stored for later use.
 * 
 * @ibm-api
 */
@SuppressWarnings("unchecked")
public class StoredResponse extends HttpServletResponseWrapper implements StoredResponseCompat, HttpServletResponse, Serializable, IResponseOutput, IExtendedResponse, IOutputStreamObserver
{
  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 3257847662609707832L;

  protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.websphere.servlet.response");
  private static final String CLASS_NAME = "com.ibm.websphere.servlet.response.StoredResponse";
  protected static final TraceNLS nls = TraceNLS.getTraceNLS(StoredResponse.class, "com.ibm.ws.webcontainer.resources.Messages");

  private static final int DEFAULT_STATUS_CODE = SC_OK;

  protected PrintWriter _writer;
  private SRTOutputStream _outInternal;
  protected ServletOutputStream _out;
  private ByteBufferOutputStream _bbOutInternal;
  protected ServletOutputStream _bbOut;
  private boolean _isFinished;

  // These fields are used for storing the final response data
  // in fast-access fields.
  private byte[] _outputBuffer;
  private int _statusCode = DEFAULT_STATUS_CODE;
  private String _statusMessage;
  private String _redirectURI;
  private Cookie[] _cookies = new Cookie[0];
  private long _lastModified;
  private ResponseErrorReport _error;
  private StoredHeader _header;
  protected boolean _writerObtained = false;
  protected boolean _outputStreamObtained = false;
  private static final String _defaultEncoding = "ISO-8859-1";
  private String _contentType = null;
  private String _encoding;
  private Locale _locale;
  private Locale _defaultLocale;
  // Code added for defect 112206
  protected int bufferSize = 4096; // default value
  private boolean isCommitted = false;
  private List<WsByteBuffer> _bbList = null;

  private IOutputMethodListener outputMethodListener;

  protected boolean isInclude = false;

  private Vector[] headerTable;

  protected boolean outputWritten;

  private boolean internalHeaderSettable;

  private boolean outputFlushed;

  private boolean finishedOutput;

  protected boolean dummyResponse = true;

  private IExtendedResponse extResponse;

  public StoredResponse()
  {
    super(new DummyResponse()); //
    init();
  }

	public StoredResponse(boolean isInclude) {
    super(new DummyResponse());
    this.isInclude = isInclude;
    init();
  }

	public StoredResponse(ServletResponseExtended extResponse,boolean isInclude) { //delegate everything but output stream/writer?
    super((HttpServletResponse) extResponse);
    this.extResponse = (IExtendedResponse) extResponse;
    dummyResponse = false;
    this.isInclude = isInclude;
    init();
  }

	private void init() {
    if (dummyResponse)
      _header = new StoredHeader();
        if (!isInclude){
      setContentType("text/html");
      _encoding = _defaultEncoding;
      _defaultLocale = Locale.getDefault();
      _locale = _defaultLocale;
    }
  }

	private void initOutputStream() {
    _bbOutInternal = new ByteBufferOutputStream();
    _outInternal = new SRTOutputStream();
    _out = _outInternal;
    _bbOut = _bbOutInternal;
    _outInternal.init(_bbOutInternal);
    _bbOutInternal.setObserver(this);
  }

  public boolean writerObtained()
  {
    return _writerObtained;
  }

  public boolean outputStreamObtained()
  {
    return _outputStreamObtained;
  }

  public void addCookie(Cookie cookie) // ARD: Unneccesary for includes
  {
    if (!dummyResponse)
      super.addCookie(cookie);
    	else if (isInclude){
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                 logger.logp(Level.FINE, CLASS_NAME,"addCookie", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addCookie cookie --> " + cookie.getName());  
    }
        else {
      Cookie[] newCookies = new Cookie[_cookies.length + 1];
      System.arraycopy(_cookies, 0, newCookies, 0, _cookies.length);
      newCookies[_cookies.length] = cookie;
      _cookies = newCookies;
    }
  }

    public IResponse getIResponse(){
    if (!dummyResponse)
      return extResponse.getIResponse();
    else
      return null;
  }

  public ServletOutputStream getOutputStream() throws IllegalStateException, IOException
  {
    if (_writerObtained)
      throw new IllegalStateException("Writer as already been obtained for this Response object");

    if (_outputStreamObtained)
      return _outInternal;
    initOutputStream();
    _outputStreamObtained = true;
    this.fireOutputStreamRetrievedEvent(_outInternal);
    return _outInternal;
  }

  public void setContentLength(int len)// ARD: Unneccesary for includes
  {
    if (!dummyResponse)
      super.setContentLength(len);
    else if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setContentLength", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setContentLength length --> " + len);  
      }
    }
    else
      setIntHeader("content-length", len);
  }

  public void setContentType(String type)// ARD: Unneccesary for includes
  {
    if (!dummyResponse)
      super.setContentType(type);
    else if (isInclude)
    {
    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setContentType", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setContentType type --> " + type);
      }
    }
    else
      setHeader("content-type", type);
  }

    public void setBufferSize(int size) throws IllegalStateException //ARD: how do we keep this in sync with top levle writer
  {

        
        if (_writer!=null){
      _writer.flush();
    }
        if (_bbOutInternal!=null){ // Call reset instead of flushing the SRTOutputStream which commits the response
            _bbOutInternal.reset();
    }

        //If the writer flushed output to the bbOut or the bbOut was written to, you'll get an exception here.
    if (outputWritten)
      throw new IllegalStateException("Cannot set buffer size after write");
    if (!dummyResponse)
      super.setBufferSize(size);
    else
      bufferSize = size;
  }

  public int getBufferSize()
  {
    if (!dummyResponse)
      return super.getBufferSize();
    else
      return bufferSize;
  }

  public void flushBuffer() throws IOException
  {
    flushBuffer(true);
  }

  public void flushBuffer(boolean flushToWire) throws IOException
  {
    if (_writer != null)
      _writer.flush();
    if (_outInternal != null)
      _outInternal.flush();

    isCommitted = true;
  }

  public boolean isCommitted()
  {
    return isCommitted;
  }

  public void reset() throws IllegalStateException
  {
    if (isCommitted)
      throw new IllegalStateException("Response cannot be reset after it has been committed");

    // PrintWriter doesn't have a clear() or a reset()
    // Hence you have to flush it and clear the underlying stream

    if (!dummyResponse)
      super.reset();

    if (_writer != null)
      _writer.flush();
    if (_bbOutInternal != null)
      _bbOutInternal.reset();

    isCommitted = false;
  }

  public java.util.Vector[] getHeaderTable()
  {
    if (!dummyResponse)
      return extResponse.getHeaderTable();
    else
      return this.headerTable;
  }

  public void addSessionCookie(Cookie cookie)
  {
    	if (!dummyResponse){
      extResponse.addSessionCookie(cookie);
    }
    else if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"addSessionCookie", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addSessionCookie cookie --> " + cookie);
      }
      throw new IllegalStateException("cannot add session from a stored/async response");
    }
    else
      throw new IllegalStateException("This method is not supported, use HttpServletResponse.addCookie(Cookie cookie) instead");
  }

  public void setLocale(Locale loc)
  {
    if (!dummyResponse)
      super.setLocale(loc);
    else if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setLocale", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setLocale locale --> " + loc);
      }
    }
    	else {
      _locale = loc;
      setCharacterEncoding(EncodingUtils.getEncodingFromLocale(_locale));
    }
  }

  public void setCharacterEncoding(String encoding)
  {
    if (!dummyResponse)
      super.setCharacterEncoding(encoding);
    	else if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setCharacterEncoding", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setCharacterEncoding encoding --> " + encoding);
      }
    }
        else{
      // PQ77854 -- strip possible double quotes
			if(encoding != null) {
				if(encoding.startsWith("\"") || encoding.startsWith("\'")){
          encoding = encoding.substring(1);
        }
				if(encoding.endsWith("\"") || encoding.endsWith("\'")) {
          encoding = encoding.substring(0, encoding.length() - 1);
        }
      }
      // PQ77854 ends
      _encoding = encoding;
    }
  }

  public Locale getLocale()
  {
    if (!dummyResponse)
      return super.getLocale();
    else
      return _locale;
  }

  public void addDateHeader(String name, long date)
  {
    if (!dummyResponse)
      super.addDateHeader(name, date);
    	else if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "addDateHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->" + name + ", date->" + date);
      }
    }
  }

  public void addHeader(String name, String value)
  {
    if (!dummyResponse)
      super.addHeader(name, value);
    	else if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "addHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->" + name + ", value->" + value);
      }
    }
  }

  public void addIntHeader(String name, int value)
  {
    if (!dummyResponse)
      super.addIntHeader(name, value);
    	else if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "addIntHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->" + name + ", value->" + value);
      }
    }
  }

  public void setHeader(String name, String value)
  {
    setHeader(name, value, true);
  }


    public void setInternalHeader(String name, String value){ //TODO: cast and set on underlying response explicitly set
    	if (!internalHeaderSettable){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setInternalHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->"+name+", value->"+value);
      }
    }
    else
      setHeader(name, value, false);
  }

    public void setHeader(String name, String value, boolean checkInclude){
    	if (checkInclude&&isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->"+name+", value->"+value+", checkInclude->"+checkInclude);
      }
    }
    	else  {
    		if (!dummyResponse){
        extResponse.setHeader(name, value, false);
        if (name.equalsIgnoreCase("content-type"))
          _encoding = super.getCharacterEncoding();
      }
    		else {
        _header.setHeader(name, value);

        if (name.toLowerCase().equals("content-type"))
        {
          if (value != null)
          {
            String val = value.toLowerCase();
            int index = val.indexOf("charset=");
            if (index != -1)
            {
              String encoding = val.substring(index + 8);
	                        if(encoding.startsWith("\"") || encoding.startsWith("\'")){
                encoding = encoding.substring(1);
              }
	                        if(encoding.endsWith("\"") || encoding.endsWith("\'")) {
                encoding = encoding.substring(0, encoding.length() - 1);
              }
              setCharacterEncoding(encoding);
              value = val.substring(0, index + 8) + encoding;
            }
          }
          _contentType = value;
        }
      }

    }
  }

  public void setIntHeader(String name, int value)
  {
    	if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setIntHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->" + name + ", value->" + value);
      }
    }
    else
      _header.setIntHeader(name, value);
  }

  public void setDateHeader(String name, long value)
  {
    	if (isInclude){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setDateHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "name->" + name + ", value->" + value);
      }
    }
    else
      _header.setDateHeader(name, value);
  }

  public boolean containsHeader(String name)
  {
    if (!dummyResponse)
      return super.containsHeader(name);
    	else{
    		if (_header!=null){
    			if (_header.containsHeader(name)){
          return true;
        }
      }

    		if (headerTable!=null){
        if (headerTable[0].contains(name))
          return true;
      }
      return false;
    }
  }

  public PrintWriter getWriter() throws IOException, IllegalStateException
  {
    if (_outputStreamObtained)
      throw new IllegalStateException("OutputStream already obtained for this Response object");

    if (_writerObtained)
      return _writer;

    initOutputStream();
    OutputStreamWriter _outWriter = null;
    try
    {
      String encoding = getCharacterEncoding();
      if (encoding == null)
      {
        _outWriter = new OutputStreamWriter(_outInternal);
      }
      else
      {
        _outWriter = new OutputStreamWriter(_outInternal, encoding);
      }
    }
    catch (UnsupportedEncodingException e)
    {
      com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.websphere.servlet.response.StoredResponse.getWriter", "248", this);
      _outWriter = new OutputStreamWriter(_outInternal);
    }

    _writer = new PrintWriter(_outWriter);
    _outWriter = null;
    _writerObtained = true;
    this.fireWriterRetrievedEvent(_writer);
    return _writer;
  }

  public void setStatus(int sc)
  {
    if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setStatus", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "status->" + sc);
      }
    }
    else
      _statusCode = sc;
  }

  public void setStatus(int sc, String message)
  {
    if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "setStatus", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "status->" + sc + ", message->" + message);
      }
    }
        else{
      _statusCode = sc;
      _statusMessage = message;
    }
  }

    public void sendError(int sc) throws IOException{
    	if (!dummyResponse){
      super.sendError(sc);
    }
    else if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "sendError", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "status->" + sc);
      }
    }
    	else {
      _error = new ResponseErrorReport();
      _error.setErrorCode(sc);
    }
  }

    public void sendError(int sc, String message) throws IOException{
    	if (!dummyResponse){
      super.sendError(sc, message);
    }
    else if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "sendError", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "status->" + sc + ", message->" + message);
      }
    }
    	else {
      _error = new ResponseErrorReport(message);
      _error.setErrorCode(sc);
    }
  }

  public String encodeURL(String url)
  {
    if (!dummyResponse)
      return super.encodeURL(url);
    else
      return url;
  }

  public String encodeUrl(String url)
  {
    if (!dummyResponse)
      return super.encodeUrl(url);
    else
      return encodeUrl(url);
  }

  public void sendRedirect(String uri) throws java.io.IOException
  {
    if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "uri->" + uri);
      }
    }
    else
      _redirectURI = uri;
  }

  // PQ97429
  public void sendRedirect303(String uri) throws java.io.IOException
  {
    if (isInclude)
    {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
        logger.logp(Level.FINE, CLASS_NAME, "sendRedirect303", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "uri->" + uri);
      }
    }
    else
      _redirectURI = uri;
  }

  // PQ97429

  public String encodeRedirectURL(String url)
  {
    if (!dummyResponse)
      return super.encodeRedirectURL(url);
    else
      return url; // pointless for includes since you won't be able to redirect
  }

  public String encodeRedirectUrl(String url)
  {
    if (!dummyResponse)
      return super.encodeRedirectUrl(url);
    else
      return url; // pointless for includes since you won't be able to redirect
  }

    

    protected void finishOutput(boolean toByteArray) throws IOException{
    	if (!finishedOutput){
      finishedOutput = true;
      if (_writer != null)
        _writer.flush();
	        if (_outInternal!=null){
        _outInternal.close();
        _bbOutInternal.flushBuffer();
		        if (toByteArray) {
          _outputBuffer = _bbOutInternal.toByteArray();
          _bbOutInternal.reset();
        }
		        else {
          _bbList = _bbOutInternal.getByteBufferList();
        }
      }
    }
  }

  public void finish() throws IOException {
    _finish();
  }

    private void _finish() throws IOException{
    if (_isFinished)
    {// dirty read
      return;
    }
    synchronized (this)
    {
      if (_isFinished)
      {
        return;
      }
      _isFinished = true;
      finishOutput(true);
      if (!isInclude && _outputBuffer != null)
        setContentLength(_outputBuffer.length);
      if (_header != null)
        _lastModified = _header.getDateHeader("last-modified");

      // clear out the unused fields
      _writer = null;
      _outInternal = null;
      _bbOutInternal = null;
      _out = null;
      _bbOut = null;
// The following is only neccesary if pooling and this isn't being done yet.
      // _writerObtained = false;
      // _outputStreamObtained = false;
    }
  }

    public void close() throws IOException{
    _finish();
  }


    public byte[] getOutputBuffer() throws IOException{
    finishOutput(true);
    return _outputBuffer;
  }

  public List<WsByteBuffer> getByteBufferList() throws IOException {
    finishOutput(false);
    return _bbList;
  }

  /**
     * Get the output from the response outputstream as a String.
     * This method should only be used to retrieve content that is known
     * to be text based.  Using this method to retrieve binary data will
     * corrupt the response data.
   */
  public String getOutputBufferAsString() throws IOException
  {
    byte[] buffer = getOutputBuffer();
    if (buffer != null)
      return new String(buffer, this.getCharacterEncoding());
    else
      return null;
  }

  /**
   * Retrieve the names of the header fields that have been set on this
   * response.
   */
  // public Collection<String> getHeaderNames()
  // {
  // if (_header!=null&&this.headerTable!=null){
  // InnerEnumeration innerEnum = new
  // InnerEnumeration(_header.getHeaderNames(),headerTable[0].elements());
  // return Collections.list(innerEnum);
  // }
  // else if (this.headerTable!=null){
  // return headerTable[0];
  // }
  // else if (_header!=null){
  // return Collections.list(_header.getHeaderNames());
  // }
  // else return null;
  // }
  public CollectionEnumerationHybrid<String> getHeaderNames()
  {
    CollectionEnumerationHybrid colEnumHybrid = new CollectionEnumerationHybrid<String>();
    Enumeration tempEnum;

    if (_header != null)
    {
      tempEnum = _header.getHeaderNames();
      while (tempEnum.hasMoreElements())
      {
        colEnumHybrid.add(tempEnum.nextElement());
      }
    }

    if (this.headerTable != null)
    {
      tempEnum = headerTable[0].elements();
      while (tempEnum.hasMoreElements())
      {
        colEnumHybrid.add(tempEnum.nextElement());
      }
    }
    return colEnumHybrid;
  }

  /**
   * Retrieve only the headers set in the include
   */
  public Enumeration getAddedHeaderNames()
  {
    if (_header != null)
    {
      return _header.getHeaderNames();
    }
    else
      return null;
  }

  private class InnerEnumeration implements Enumeration
  {
    private Enumeration enum1;
    private Enumeration enum2;

    public InnerEnumeration(Enumeration enum1, Enumeration enum2)
    {
      this.enum1 = enum1;
      this.enum2 = enum2;
    }

    public boolean hasMoreElements()
    {
      return (enum1.hasMoreElements() || enum2.hasMoreElements());
    }

    public Object nextElement()
    {
      if (enum1.hasMoreElements())
      {
        return enum1.nextElement();
      }
      else if (enum2.hasMoreElements())
      {
        return enum2.nextElement();
      }
      else
      {
        throw new NoSuchElementException();
      }
    }
  }

  /**
   * Retrieve a response header field by name.
   */
  public String getHeader(String name)
  {
    if (_header != null)
    {
      String strVal = _header.getHeader(name);
      if (strVal != null)
        return strVal;
    }
    if (this.headerTable != null)
    {
      int i = 0;
      for (Object obj : headerTable[0])
      {
        String strVal = (String) obj;
        if (name.equals(strVal))
        {
          return (String) headerTable[1].get(i);
        }
        i++;
      }
    }
    return null;
  }

  /**
   * Retrieve a response header as an int.
   */
  public int getIntHeader(String name)
  {
    if (_header != null)
    {
      int headerVal = _header.getIntHeader(name);
      if (headerVal != -1)
        return headerVal;
    }
    if (this.headerTable != null)
    {
      int i = 0;
      for (Object obj : headerTable[0])
      {
        String strVal = (String) obj;
        if (name.equals(strVal))
        {
          return Integer.valueOf((String) headerTable[1].get(i));
        }
        i++;
      }
    }
    return -1;
  }

  /**
   * Retrieve a response header as a date.
   */
  public long getDateHeader(String name)
  {
    if (_header != null)
    {
      long longVal = _header.getDateHeader(name);
      if (longVal != -1)
      {
        return longVal;
      }
    }
    if (this.headerTable != null)
    {
      int i = 0;
      for (Object obj : headerTable[0])
      {
        String strVal = (String) obj;
        if (name.equals(strVal))
        {
          try
          {
            return Long.valueOf((String) headerTable[1].get(i));
          }
          catch (NumberFormatException nfe)
          {
            throw new IllegalArgumentException(nfe);
          }
        }
        i++;
      }
    }
    return -1;
  }

  /**
   * Returns true if sendError() was called on this response.
   */
  public boolean containsError()
  {
    return _error != null;
  }

  public ServletErrorReport getError()
  {
    return _error;
  }

  /**
   * Returns the error message as reported by the sendError() method.
   */
  public String getErrorMessage()
  {
    if (containsError())
    {
      return getError().getMessage();
    }
    return null;
  }

  /**
   * Returns the status code as reported by the sendError() method.
   * 
   * @throw IllegalStateException if sendError() was not called.
   */
  public int getErrorStatusCode()
  {
    if (!containsError())
    {
      throw new IllegalStateException("sendError() was not called");
    }
    return getError().getErrorCode();
  }

  /**
   * Returns the status code as specifed by the setStatus() method.
   */
  public int getStatusCode()
  {
    return _statusCode;
  }

  /**
   * Returns the status message as reported by the setStatus() method.
   */
  public String getStatusMessage()
  {
    return _statusMessage;
  }

  /**
   * Returns the target URI of the redirect as specified by the sendRedirect()
   * method.
   * 
   * @throw IllegalStateException if sendRedirect() was not called.
   */
  public String getRedirectURI()
  {
    if (!isRedirected())
    {
      throw new IllegalStateException("sendRedirect() was not called");
    }
    return _redirectURI;
  }

  /**
   * Returns true if sendRedirect() was called.
   */
  public boolean isRedirected()
  {
    return _redirectURI != null;
  }

  /**
   * Return the character encoding as specified in the charset flag of the
   * content-type.
   */
  public String getCharacterEncoding()
  {
    if (!dummyResponse)
      return super.getCharacterEncoding();
    else
      return _encoding; // don't need to check dummy response because it is kept
    // up to date locally
  }

  public String getContentType()
  {
    if (!dummyResponse)
      return super.getContentType();
    else
      return _contentType;
  }

  /**
   * Return the cookies that were set using the addCookie() method.
   */
  public Cookie[] getCookies()
  {
    return _cookies;
  }

  /**
   * Returns the date that the data was last modifed in this response or -1 if
   * unknown. This method returns the date value specified in the last-modified
   * header.
   */
  public long getLastModified()
  {
    return _lastModified;
  }

  /**
   * Return true if the data in this response has expired. This method is useful
   * for caches to determine if the response is up-to-date or if if it should be
   * cleared from the cache. This method returns false by default, subclasses
   * should overload this method to provide their own expiration policies.
   */
  public boolean isExpired()
  {
    return false;
  }

  /**
   * Copy the contents of this response to another HttpServletResponse. This
   * method is optimized to quickly transfer the contents of this response into
   * another response. This method is useful when this response is cached to
   * generate the same response later.
   */
  public void transferResponse(HttpServletResponse target) throws IOException // never
  // called
  // for
  // ARD.
  // would
  // be
  // in
  // trouble
  // with
  // a
  // bunch
  // of
  // include
  // warnings
  {
    _finish();

    if (containsError())
    {
      // transfer error
      String message = getErrorMessage();
      int sc = getErrorStatusCode();
      if (message == null)
      {
        target.sendError(sc);
      }
      else
      {
        target.sendError(sc, message);
      }
    }
    else if (isRedirected())
    {
      // transfer cookies
      Cookie[] cookies = getCookies();
      for (int i = 0; i < cookies.length; i++)
      {
        target.addCookie(cookies[i]);
      }

      // transfer redirect
      target.sendRedirect(getRedirectURI());
    }
    else
    {
      // transfer status code
      if (getStatusMessage() == null)
      {
        target.setStatus(getStatusCode());
      }
      else
      {
        target.setStatus(getStatusCode(), getStatusMessage());
      }

      // transfer headers
      _header.transferHeader(target);

      // transfer cookies
      Cookie[] cookies = getCookies();
      for (int i = 0; i < cookies.length; i++)
      {
        target.addCookie(cookies[i]);
      }

      if (this.getOutputBuffer() != null)
      { // PM17019
        // transfer data
        ServletOutputStream out;
        try
        {
          out = target.getOutputStream();
        }
        catch (IllegalStateException i)
        {
          while (!(target instanceof StoredResponse))
          {
            while (target instanceof HttpServletResponseWrapper)
            {
              target = ((HttpServletResponse) ((HttpServletResponseWrapper) target).getResponse());
            }

            while (target instanceof HttpServletResponseProxy)
            {
              target = ((HttpServletResponse) ((HttpServletResponseProxy) target).getProxiedHttpServletResponse());
            }

            while (target instanceof IncludedResponse)
            {
              target = ((IncludedResponse) target).getProxiedHttpServletResponse();
            }

            // PQ88880 begin
            if (target instanceof SRTServletResponse)
            {
              target.getWriter().write(this.getOutputBufferAsString().toCharArray());
              return;
            }
            // PQ88880 end
          }

          StoredResponse s = (StoredResponse) target;

          out = s._outInternal;
        }

        out.write(this.getOutputBuffer());
      }
    }
  }

  // LIDB1234.3 - added method below
  /**
   * Clears the content of the underlying buffer in the response without
   * clearing headers or status code.
   * 
   * @since 2.3
   */
  public void resetBuffer()
  {
    // just need to clear the internal buffer since this response cannot be
    // committed
    if (_writer != null)
      _writer.flush();
    if (_bbOutInternal != null)
      this._bbOutInternal.reset();
  }

  public void fireOutputStreamRetrievedEvent(ServletOutputStream sos)
  {
    if (outputMethodListener != null)
      outputMethodListener.notifyOutputStreamRetrieved(sos);
  }

  public void fireWriterRetrievedEvent(PrintWriter pw)
  {
    if (outputMethodListener != null)
      outputMethodListener.notifyWriterRetrieved(pw);
  }

  public void registerOutputMethodListener(IOutputMethodListener listener)
  {
    this.outputMethodListener = listener;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wsspi.webcontainer.servlet.IExtendedResponse#destroy()
   */
  public void destroy()
  {
    // empty inherited methods

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.wsspi.webcontainer.servlet.IExtendedResponse#initForNextResponse
   * (com.ibm.wsspi.webcontainer.IResponse)
   */
  public void initForNextResponse(IResponse res)
  {
    // empty inherited methods

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wsspi.webcontainer.servlet.IExtendedResponse#start()
   */
  public void start()
  {
    // empty inherited methods

  }

  // Added for ARD
  public void setInclude(boolean isInclude)
  {
    this.isInclude = true;
  }

  public void setInternalHeaderSettable(boolean internalHeaderSettable)
  {
    this.internalHeaderSettable = internalHeaderSettable;
  }

  public void buildResponseData(ServletResponseExtended response)
  {
    headerTable = response.getHeaderTable().clone();
    buildWriterData(response);
    _contentType = response.getContentType();
    _locale = response.getLocale();
    _statusCode = response.getStatusCode();
    // TODO:getStatusMessage as well?
  }

  // Added for ARD

  public void buildWriterData(ServletResponseExtended response)
  {
    isCommitted = response.isCommitted();
    _encoding = response.getCharacterEncoding();
  }

  public void alertClose()
  {

  }

  public void alertException()
  {
  }

  public void alertFirstFlush()
  {
    isCommitted = true;
  }

  public void alertFirstWrite()
  {
    outputWritten = true;
  }

  public void closeResponseOutput(boolean b)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeCookie(String cookieName)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isOutputWritten()
  {
    return outputWritten;
  }
}
