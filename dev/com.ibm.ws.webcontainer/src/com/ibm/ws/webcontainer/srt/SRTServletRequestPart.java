/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.disk.DiskFileItem;

import com.ibm.ejs.ras.TraceNLS;

public class SRTServletRequestPart implements Part {

	protected DiskFileItem _part;
	private static final TraceNLS nls = TraceNLS.getTraceNLS(SRTServletRequestPart.class, "com.ibm.ws.webcontainer.resources.Messages");
	
	public SRTServletRequestPart(DiskFileItem item) {
		_part = item;
	}
	@Override
	public void delete() throws IOException {
	    if (System.getSecurityManager() != null) {
	        try {
	            AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
                    public Object run() throws IOException {
                        _part.delete();
                        return null;
                    }
                });
	        } catch (PrivilegedActionException e) {
	            Exception e1 = e.getException();
	            if (e1 instanceof IOException) {
	                throw (IOException) e1;
	            } else if (e1 instanceof RuntimeException) {
	                throw (RuntimeException) e1;
	            } else {
	                throw new IOException(e1);
	            }
	        }
	    } else {
	        _part.delete();
	    }
	}

	@Override
	public String getContentType() {
	    String returnValue;
	    if (System.getSecurityManager() != null) {
            returnValue = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                public String run() {
                    return _part.getContentType();
                }
            });
	    } else {
	        returnValue = _part.getContentType();
	    }
	    return returnValue;
	}

	@Override
	public String getHeader(String headerName) {
	    String returnValue;
	    if (System.getSecurityManager() != null) {
            final String finalHeaderName = headerName;
	        returnValue = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                public String run() {
                    FileItemHeaders headers =_part.getHeaders();
                    return headers.getHeader(finalHeaderName);
                }
            });
        } else {
            FileItemHeaders headers =_part.getHeaders();
            returnValue = headers.getHeader(headerName);
        }
		return returnValue;
	}
	
	@Override
	public Collection<String> getHeaderNames() {
	    final ArrayList<String> headerNamesList = new ArrayList<String>();;
	    if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    FileItemHeaders headers =_part.getHeaders();
                    for (Iterator it =headers.getHeaderNames();it.hasNext();) {
                        headerNamesList.add((String)it.next());
                    }
                    return null;
                }
            });
        } else {
            FileItemHeaders headers =_part.getHeaders();
            for (Iterator it =headers.getHeaderNames();it.hasNext();) {
                headerNamesList.add((String)it.next());
            }
        }
	    return headerNamesList;
	}

	@Override
	public Collection<String> getHeaders(String headerName) {
	    final ArrayList<String> headersList = new ArrayList<String>();
	    if (System.getSecurityManager() != null) {
	        final String finalHeaderName = headerName;
	        AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
	            public Object run() {
	                FileItemHeaders headers =_part.getHeaders();
	                for (Iterator it =headers.getHeaders(finalHeaderName);it.hasNext();) {
	                    headersList.add((String)it.next());
	                }
                    return null;
                }
            });
        } else {
            FileItemHeaders headers =_part.getHeaders();
            for (Iterator it =headers.getHeaders(headerName);it.hasNext();) {
                headersList.add((String)it.next());
            }
        }
		return headersList;
	}

	@Override
	public InputStream getInputStream() throws IOException {
	    InputStream returnValue;
	    if (System.getSecurityManager() != null) {
            try {
                returnValue = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<InputStream>() {
                    public InputStream run() throws IOException {
                        return _part.getInputStream(); 
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception e1 = e.getException();
                if (e1 instanceof IOException) {
                    throw (IOException) e1;
                } else if (e1 instanceof RuntimeException) {
                    throw (RuntimeException) e1;
                } else {
                    throw new IOException(e1);
                }
            }
        } else {
            returnValue = _part.getInputStream(); 
        }
	    return returnValue; 
	}

	@Override
	public String getName() {
	    String returnValue;
	    if (System.getSecurityManager() != null) {
            returnValue = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                public String run() {
                    return _part.getFieldName();
                }
            });
        } else {
            returnValue = _part.getFieldName();
        }
	    return returnValue;
	}

	@Override
	public long getSize() {
	    long returnValue;
        if (System.getSecurityManager() != null) {
            Long longObjectValue = AccessController.doPrivileged(new java.security.PrivilegedAction<Long>() {
                public Long run() {
                    return new Long(_part.getSize());
                }
            });
            returnValue = longObjectValue.longValue();
        } else {
            returnValue = _part.getSize();
        }
        return returnValue;
	}

	@Override
	public void write(String fileName) throws IOException {
	    if (System.getSecurityManager() != null) {
            try {
                final String finalFileName = fileName;
                AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        File f = new File(_part.getStoreLocation().getParentFile(), finalFileName);
                        _part.write(f);
                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception e1 = e.getException();
                throw new IOException(e1);
            }
        } else {
            File f = new File(_part.getStoreLocation().getParentFile(), fileName);
            try {
                _part.write(f);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }	    
	}
	
	public boolean isFormField() {
	    boolean returnValue;
        if (System.getSecurityManager() != null) {
            Boolean booleanObjectValue = AccessController.doPrivileged(new java.security.PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return new Boolean(_part.isFormField());
                }
            });
            returnValue = booleanObjectValue.booleanValue();
        } else {
            returnValue = _part.isFormField();
        }
        return returnValue;
	}

}
