/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.disk.DiskFileItem;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class SRTServletRequestPart implements Part {

    protected DiskFileItem _part;
    private static final TraceNLS nls = TraceNLS.getTraceNLS(SRTServletRequestPart.class, "com.ibm.ws.webcontainer.resources.Messages");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTServletRequestPart";
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");


    public SRTServletRequestPart(DiskFileItem item) {
        _part = item;
    }
    @Override
    public void delete() throws IOException {
        if (System.getSecurityManager() != null) {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                logger.logp(Level.FINE, CLASS_NAME,"delete", "via security manager. Location from part ["+ _part.getStoreLocation() +"]");
            }
            
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
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                logger.logp(Level.FINE, CLASS_NAME,"delete", "via DiskFileItem.delete. Location from part ["+ _part.getStoreLocation() +"]");
            }
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getContentType", " ["+returnValue+"]");
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getHeader", " headerName ["+ headerName +"] , return value ["+returnValue+"]");
        }
        return returnValue;
    }

    @Override
    public Collection<String> getHeaderNames() {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getHeaderNames", "");
        }
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
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getHeaders", "");
        }
        
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getInputStream", " ["+returnValue+"]");
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getName", " ["+returnValue+"]");
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getSize", " ["+returnValue+"]");
        }
        
        return returnValue;
    }

	@Override
	public void write(String fileName) throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
			logger.logp(Level.FINE, CLASS_NAME, "write", "fileName -> " + fileName + ", this->" + this);
		}
		if (System.getSecurityManager() != null) {
			try {
				final String finalFileName = fileName;
				AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
					public Object run() throws Exception {
						File f;
						Path path = Paths.get(finalFileName);
						if (WCCustomProperties.ALLOW_ABSOLUTE_FILENAME_FOR_WRITE && path.isAbsolute()) { // PH62271
                            f = new File(finalFileName);
							if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
								logger.logp(Level.FINE, CLASS_NAME, "write",
										"location [" + f + "] isAbsolute");
							}
						} else {
                            f = new File(_part.getStoreLocation().getParentFile(), finalFileName);
							if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
								logger.logp(Level.FINE, CLASS_NAME, "write",
										"location from part.getStoreLocation [" + _part.getStoreLocation() + "]");
							}
						}
						_part.write(f);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				Exception e1 = e.getException();
				throw new IOException(e1);
			}
		} else {
			File f;
			Path path = Paths.get(fileName);
			if (WCCustomProperties.ALLOW_ABSOLUTE_FILENAME_FOR_WRITE && path.isAbsolute()) { // PH62271
                f = new File(fileName);
				if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
					logger.logp(Level.FINE, CLASS_NAME, "write", "location [" + f + "] isAbsolute");
				}
			} else {
                f = new File(_part.getStoreLocation().getParentFile(), fileName);
				if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
					logger.logp(Level.FINE, CLASS_NAME, "write",
							"location from part.getStoreLocation [" + _part.getStoreLocation() + "]");
				}
			}
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
        
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"isFormField", " ["+returnValue+"]");
        }
        
        return returnValue;
    }
}
