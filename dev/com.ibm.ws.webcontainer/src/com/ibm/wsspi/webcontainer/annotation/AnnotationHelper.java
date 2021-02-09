/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.annotation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebApp.ANNOT_TYPE;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class AnnotationHelper {
    
    WebApp wrapper = null;
    
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.annotation");
    protected static final String CLASS_NAME = "com.ibm.wsspi.webcontainer.annotation.AnnotationHelper";

    private ConcurrentHashMap<Object, ManagedObject> cdiCreationContextMap = new ConcurrentHashMap<Object, ManagedObject>();
    
    public AnnotationHelper(WebApp wrapWithThis) {
        wrapper = wrapWithThis;
    }
    
    // PI30335: this inject() provides options to delay post construct invocation
    // If delayPostConstruct is true, doPostConstruct will not be called
    public ManagedObject inject(Object obj, boolean delayPostConstruct) throws RuntimeException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            logger.entering(CLASS_NAME, "inject(Object,boolean)", "obj = " + obj + ", delayPostConstruct = " + delayPostConstruct);
        
        ManagedObject mo = null;
        Throwable th = null;
        try {
            if (wrapper != null) {
                if (!delayPostConstruct) {
                    // Post construct before returning
                    mo = wrapper.injectAndPostConstruct(obj);
                } else {
                    // Inject independently of post construct
                    mo = wrapper.inject(obj);
                }
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp (Level.FINE, CLASS_NAME, "inject", "injection not attempted because wrapper is null ");
               }
            }
        } catch (InjectionException e) {
            // in tWAS during inject() this exception is not thrown back, and doPostConstruct would do nothing.
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "inject", "caught injection exception: " + e);
            }
            if (!delayPostConstruct) {
                th = e;
            } else {
                throw new RuntimeException(e);
            }
        } catch (RuntimeException e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "inject", "caught runtime exception: " + e);
            }
            if (!delayPostConstruct) {
                th = e;
            } else {
                throw e;
            }
        }
        // We've invoked post construct on this 
        if (!delayPostConstruct) {
            // in tWAS on doPostConstruct all exceptions get re-thrown as RuntimeExceptions
            // in WAS7 no RuntimeException is thrown at all, set THROW_POSTCONSTRUCT_EXCEPTION=false to reverse back to V7
            if (th!=null && WCCustomProperties.THROW_POSTCONSTRUCT_EXCEPTION) {     //PM63754
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {        
                    logger.logp (Level.FINE, CLASS_NAME, "inject", "doPostConstruct exceptions are re-thrown as RuntimeException");  
                }
                if (th instanceof RuntimeException) {
                        throw (RuntimeException) th;
                } else {
                        throw new RuntimeException(th);
                }
            }
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.exiting(CLASS_NAME, "inject(Object,boolean)");
        
        return mo;
    }
    
    // need to support this from tWAS compiled jsp, always called together 
    // _jspx_iaHelper.inject(tagStartWriter.print (tagHandlerVar) ; );
    // _jspx_iaHelper.doPostConstruct(tagStartWriter.print (tagHandlerVar); );    
    public ManagedObject inject(Object obj) throws RuntimeException {
        
        // PI30335: default inject method: post construct invoked before returning
        return inject(obj, false);
    }

    // inject with class for cdi 1.2 support.
    public ManagedObject inject(Class<?> Klass, boolean delayPostConstruct) {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            logger.entering(CLASS_NAME, "inject(Class<?>,boolean)", "class = " + Klass.getName() + ", delayPostConstruct = " + delayPostConstruct);

        ManagedObject mo = null;
        Throwable th = null;
        try {
            if (wrapper != null) {
                if (!delayPostConstruct) {
                    // Post construct before returning
                    mo = wrapper.injectAndPostConstruct(Klass);
                } else {
                    // Inject independently of post construct
                    mo = wrapper.inject(Klass);
                }
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp (Level.FINE, CLASS_NAME, "inject", "injection not attempted because wrapper is null ");
               }
            }
        } catch (InjectionException e) {
            // in tWAS during inject() this exception is not thrown back, and doPostConstruct would do nothing.
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "inject", "caught injection exception: " + e);
            }
            if (!delayPostConstruct) {
                th = e;
            } else {
                throw new RuntimeException(e);
            }
        } catch (RuntimeException e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "inject", "caught runtime exception: " + e);
            }
            if (!delayPostConstruct) {
                th = e;
            } else {
                throw e;
            }
        }
        // We've invoked post construct on this 
        if (!delayPostConstruct) {
            // in tWAS on doPostConstruct all exceptions get re-thrown as RuntimeExceptions
            // in WAS7 no RuntimeException is thrown at all, set THROW_POSTCONSTRUCT_EXCEPTION=false to reverse back to V7
            if (th!=null && WCCustomProperties.THROW_POSTCONSTRUCT_EXCEPTION) {     //PM63754
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {        
                    logger.logp (Level.FINE, CLASS_NAME, "inject", "doPostConstruct exceptions are re-thrown as RuntimeException");  
                }
                if (th instanceof RuntimeException) {
                        throw (RuntimeException) th;
                } else {
                        throw new RuntimeException(th);
                }
            }
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.exiting(CLASS_NAME, "inject(Class<?>,boolean)");

        return mo;
    }
    
    // inject with class for cdi 1.2 support.
    public ManagedObject inject(Class<?> klass) {
          return inject(klass, false);
    }

    public void doPostConstruct(Object obj) {
            // post construct is done during inject
    }

    // PI30335: Perform post construct independently of inject().
    public void doDelayedPostConstruct(Object obj) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            logger.entering(CLASS_NAME, "doDelayedPostConstruct", "obj = " + obj);
        
        // after injection then PostConstruct annotated methods on the host object needs to be invoked.
        // in WAS7 no RuntimeException is thrown at all, set THROW_POSTCONSTRUCT_EXCEPTION=false to reverse back to V7
        Throwable t = wrapper.invokeAnnotTypeOnObjectAndHierarchy(obj, ANNOT_TYPE.POST_CONSTRUCT);
        if (null != t && WCCustomProperties.THROW_POSTCONSTRUCT_EXCEPTION){      //PM63754
            // log exception - and process InjectionExceptions so the error will be returned to the client as an error. 
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "doDelayedPostConstruct", "Exception caught during post construct processing: " + t);
            } 
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                    throw new RuntimeException(t);
            }
    	}
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "doDelayedPostConstruct", t);
        }
    }
    
    public void doPreDestroy(Object obj) {
        try { 
            if (wrapper != null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp (Level.FINE, CLASS_NAME, "doPreDestroy", "obj = "+ obj);
                }
 
                wrapper.performPreDestroy(obj);
        
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp (Level.FINE, CLASS_NAME, "doPreDestroy", "doPreDestroy not attempted because wrapper is null ");
                }
            }
        } catch (Throwable t) {
            // tWAS catches throwable on this path, and does not re-throw.
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "doPreDestroy", "doPreDestroy caught throwable: " + t);
            }
        }
    }
    
    public void addTagHandlerToCdiMap(Object o, ManagedObject mo) {
        if (o == null || mo == null) {
            throw new IllegalArgumentException("Neither the tag nor the managed object may be null");
        }

        cdiCreationContextMap.put(o, mo);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp (Level.FINE, CLASS_NAME, "addTagHandlerToCdiMap", "add managedObject=[" + mo + "], object=[" + mo.getObject() +"], mapSize ["+cdiCreationContextMap.size()+"], this ["+ this +"]");
        }
    }
    
    public void cleanUpTagHandlerFromCdiMap(Object o) {
        ManagedObject mo = cdiCreationContextMap.remove(o);
        if (mo!=null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "cleanUpTagHandlerFromCdiMap", "remove managedObject=[" + mo + "], object=[" + mo.getObject() +"], mapSize ["+cdiCreationContextMap.size()+"], this ["+ this +"]");
            }
            mo.release();
        }
    }

}
