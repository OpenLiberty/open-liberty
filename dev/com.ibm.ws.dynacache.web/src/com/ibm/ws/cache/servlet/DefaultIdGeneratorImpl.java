/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.cache.DynamicCacheAccessor;
import com.ibm.websphere.servlet.cache.CacheConfig;
import com.ibm.websphere.servlet.cache.ConfigElement;
import com.ibm.websphere.servlet.cache.IdGenerator;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;
import com.ibm.ws.cache.ServerCache;

public class DefaultIdGeneratorImpl implements IdGenerator {

   private static TraceComponent tc = Tr.register(DefaultIdGeneratorImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   // Fields set inside the config file.
   // Should handle everything that might determine a cacheID

   //config supplied name
   String name = null;

   // sharing policy obtained from CacheConfig if set for specific cache entry,
   // but we start off with the global setting as the default
   int sharingPolicy = ServerCache.getSharingPolicy();

   //is this servlet externally cacheable?
   boolean external = false;
   //when this servlet is externally cacheable, use this quick set of config elements for fast lookups
   HashMap reqParmHash = null;

   //invalidations need to be done quickly.
   ConfigElement[] invalidations = null;
   boolean invalidateonly = false;

   ConfigElement requestParameters[] = null;
   ConfigElement requestAttributes[] = null;
   ConfigElement sessionParameters[] = null;
   ConfigElement cookies[] = null;

   //cache request attribute and session param methods...
   protected Method[] rAttrMethodCache = null;
   protected Method[] sParamMethodCache = null;

   public String getId(ServletCacheRequest request) {

      boolean debugEnabled = tc.isDebugEnabled();
      StringBuffer sb = new StringBuffer();

      ArrayList invalidations = new ArrayList();
      ArrayList dataIds = new ArrayList();

      //103045 -- we will use this boolean to track whether to bother looking for anything other
      //          than invalidations.  If true, we will build the id, otherwise we'll skip that step.
      boolean buildId = true;

      //104287 handle externally cacheable entries specially
      if (external) {
         //generate an id using the query string and nothing else
         if (debugEnabled)
            Tr.debug(tc, "Special id processing for this exteranally cacheable servlet...");

         Enumeration parms = request.getParameterNames();

         while (parms.hasMoreElements()) {
            String parm = (String) parms.nextElement();
            ConfigElement ce = (ConfigElement) reqParmHash.get(parm);

            String[] values = request.getParameterValues(parm);

            for (int i = 0; i < values.length; i++) {
               String value = values[i];
               if (ce != null) {
                  //then do some special processing
                  if (ce.exclude != null) {
                     if (ce.exclude.contains(values[i])) {
                        if (debugEnabled)
                           Tr.debug(tc, ce.id + " has value " + value + " present on request, and is marked \"exclude\". Not Caching, though processing will continue to find invalidations.");
                        buildId = false;
                     }
                  }
                  //invalidations triggered by this variable
                  if (ce.invalidate != null) {
                     invalidations.add(new StringBuffer(ce.invalidate).append("=").append(value).toString());
                  }
                  if (ce.dataId != null) {
                     if (debugEnabled)
                        Tr.debug(tc, "adding data id " + ce.dataId);
                     dataIds.add(new StringBuffer(ce.dataId).append("=").append(value).toString());
                  }

               }
            }

         }
         if (buildId) {
            //sb.append((String) request.getAttribute(CacheManager.CACHE_ABSOLUTE_URI));
            String queryString = request.getQueryString();
            if (queryString != null) {
               sb.append("?").append(queryString);
            }
         }
      } else {

         if (name != null && !name.equals("")) {
            sb.append(name).append(":");
         } else {
            //sb.append((String) request.getAttribute(CacheManager.CACHE_ABSOLUTE_URI)).append(":");
         }

         try {
            //request parameters
            if (requestParameters != null) {
               for (int i = 0; i < requestParameters.length; i++) {
                  ConfigElementImpl ce = (ConfigElementImpl) requestParameters[i];

                  String[] vals = request.getParameterValues(ce.id);

                  if (ce.excludeAll && vals != null) {
                     if (debugEnabled)
                        Tr.debug(tc, ce.id + " present on request, and is marked \"Exclude All\". Not Caching, though processing will continue to find invalidations.");
                     buildId = false;
                  }

                  if (vals != null) {
                     for (int j = 0; j < vals.length; j++) {
                        String value = vals[j];

                        if (ce.exclude != null) {
                           if (ce.exclude.contains(value)) {
                              if (debugEnabled)
                                 Tr.debug(tc, ce.id + " has value " + value + " present on request, and is marked \"exclude\". Not Caching, though processing will continue to find invalidations.");
                              buildId = false;
                           }
                        }

                        if (ce.ignoreValue) {
                           if (debugEnabled)
                              Tr.debug(tc, "ignoring value of " + ce.id);
                           value = "";
                        }

                        //entry id component of this variable
                        if (buildId)
                           sb.append(ce.id).append("=").append(value).append(":");
                        //invalidations triggered by this variable
                        if (ce.invalidate != null) {
                           invalidations.add(new StringBuffer(ce.invalidate).append("=").append(value).toString());
                        }
                        //data id groups joined with this variable
                        if (buildId)
                           if (ce.dataId != null) {
                              if (debugEnabled)
                                 Tr.debug(tc, "adding data id " + ce.dataId);
                              dataIds.add(new StringBuffer(ce.dataId).append("=").append(value).toString());
                           }
                     }
                  } else if (ce.required) {
                     //then this parameter should have been there.  Don't cache it
                     if (debugEnabled)
                        Tr.debug(tc, ce + "\" missing from request. Not Caching.");
                     return null;
                  }

                  //note: getParameterValues returns a String[].  This will put some junk into the Id, and
                  //different permutations of the same parameter set will lead to different cacheIds.
               }
            }
         } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "194", this);
            if (debugEnabled) 
                Tr.debug(tc, "problem getting params: " + e.getMessage());
         }

         //request attributes
         if (requestAttributes != null) {
            for (int i = 0; i < requestAttributes.length; i++) {
               ConfigElementImpl ce = (ConfigElementImpl) requestAttributes[i];

               Object o = request.getAttribute(ce.id);

               if (ce.excludeAll && o != null) {
                  if (debugEnabled)
                     Tr.debug(tc, ce.id + " present on request, and is marked \"Exclude All\". Not Caching, though processing will continue to find invalidations.");
                  buildId = false;
               }

               Method m = null;
               if (o != null) {

                  if (rAttrMethodCache[i] != null) {
                     m = rAttrMethodCache[i];
                  } else {
                     //the method cache was empty.  Get a new message with reflection
                     if (debugEnabled)
                        Tr.debug(tc, "caching " + ce.id + "." + ce.method + "()");
                     try {
                        m = o.getClass().getMethod(ce.method,  new Class[]{});
                     } catch (Exception e) {
                        //this signifies some misconfiguration..the config file says to look for a method that is not
                        //in this object, i.e. the app doesn't work this way.
                        //
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "224", this);
                        if (ce.required) {
                           //then this attribute should have been had this method.  Don't cache it
                           if (debugEnabled)
                              Tr.debug(tc, "Required object " + ce.id + "\" was missing method \"" + ce.method + "\". Not Caching.");
                           return null;
                        } else {
                           if (debugEnabled)
                              Tr.debug(tc, ce.id + "\" was missing method \"" + ce.method + "\". Continuing.");
                           continue;
                        }
                     }
                     rAttrMethodCache[i] = m;
                  }

                  String result = null;
                  Object tmp = null;
                  try {
                     tmp = m.invoke(o, new Object[]{});
                  } catch (InvocationTargetException e) {
                     com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "242", this);
                  } catch (IllegalAccessException e) {
                     com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "245", this);
                  }
                  if (tmp != null)
                     result = tmp.toString();

                  if (ce.exclude != null) {
                     if (ce.exclude.contains(result)) {
                        if (debugEnabled)
                           Tr.debug(
                              tc,
                              ce.id
                                 + "."
                                 + ce.method
                                 + "() has value "
                                 + result
                                 + " present on request, and is marked \"exclude\". Not Caching, though processing will continue to find invalidations.");
                        buildId = false;
                     }
                  }

                  if (ce.ignoreValue) {
                     if (debugEnabled)
                        Tr.debug(tc, "ignoring value of " + ce.id);
                     result = "";
                  }

                  //entry id component of this variable
                  if (buildId)
                     sb.append(ce.id).append(".").append(ce.method).append("=").append(result).append(":");

                  if (result != null) {
                     //invalidations triggered by this page
                     if (ce.invalidate != null) {
                        invalidations.add(new StringBuffer(ce.invalidate).append("=").append(result).toString());
                     }
                     //data id groups joined with this variable
                     if (buildId)
                        if (ce.dataId != null) {
                           dataIds.add(new StringBuffer(ce.dataId).append("=").append(result).toString());
                        }
                  }
               } else if (ce.required) {
                  //then this parameter should have been there.  Don't cache it
                  if (debugEnabled)
                     Tr.debug(tc, ce + "\" missing from request. Not Caching.");
                  return null;
               }
            }
         }

         //session parameters..same procedure as request attributes.
         if (sessionParameters != null) {
            HttpSession session = request.getSession(false);
            for (int i = 0; i < sessionParameters.length; i++) {
               ConfigElementImpl ce = (ConfigElementImpl) sessionParameters[i];

               if (session == null) {
                  if (ce.required) {
                     //then this session should have already been instantiated.  Don't cache it
                     if (debugEnabled)
                        Tr.debug(tc, "No session exists, but session parameter \"" + ce.id + "\" is required. Not Caching.");
                     return null;
                  } else {
                     if (debugEnabled)
                        Tr.debug(tc, "No session exists, ignoring session parameter \"" + ce.id);
                     continue;
                  }
               }

               Object o = session.getAttribute(ce.id);

               if (ce.excludeAll && o != null) {
                  if (debugEnabled)
                     Tr.debug(tc, ce.id + " present on request, and is marked \"Exclude All\". Not Caching, though processing will continue to find invalidations.");
                  buildId = false;
               }

               Method m = null;
               if (o != null) {
                  if (sParamMethodCache[i] != null) {
                     m = sParamMethodCache[i];
                  } else {
                     try {
                        m = o.getClass().getMethod(ce.method,  new Class[]{});
                     } catch (Exception e) {
                        //this signifies some misconfiguration..the config file says to look for a method that is not
                        //in this object, i.e. the app doesn't work this way.
                        //
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "321", this);
                        if (ce.required) {
                           //then this attribute should have been had this method.  Don't cache it
                           if (debugEnabled)
                              Tr.debug(tc, "required session parameter \"" + ce.id + "\" was missing method \"" + ce.method + "\". Not Caching.");
                           return null;
                        } else {
                           if (debugEnabled)
                              Tr.debug(tc, "sesison parameter \"" + ce.id + "\" was missing method \"" + ce.method + "\".  Continuing.");
                           continue;
                        }
                     }
                     sParamMethodCache[i] = m;
                  }

                  String result = null;
                  Object tmp = null;
                  try {
                     tmp = m.invoke(o, new Object[]{});
                  } catch (InvocationTargetException e) {
                     com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "339", this);
                  } catch (IllegalAccessException e) {
                     com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.DefaultIdGeneratorImpl.getId", "342", this);
                  }
                  if (tmp != null)
                     result = tmp.toString();
                  //check for exclude
                  if (ce.exclude != null) {
                     if (ce.exclude.contains(result)) {
                        if (debugEnabled)
                           Tr.debug(
                              tc,
                              ce.id
                                 + "."
                                 + ce.method
                                 + "() has value "
                                 + result
                                 + " present on request, and is marked \"exclude\". Not Caching, though processing will continue to find invalidations.");
                        buildId = false;
                     }
                  }

                  if (ce.ignoreValue) {
                     if (debugEnabled)
                        Tr.debug(tc, "ignoring value of " + ce.id);
                     result = "";
                  }

                  if (buildId)
                     sb.append(ce.id).append(".").append(ce.method).append("=").append(result).append(":");
                  if (result != null) {

                     //invalidations triggered by this page
                     if (ce.invalidate != null) {
                        invalidations.add(new StringBuffer(ce.invalidate).append("=").append(result).toString());
                     }
                     //data id groups joined with this variable
                     if (buildId)
                        if (ce.dataId != null) {
                           dataIds.add(new StringBuffer(ce.dataId).append("=").append(result).toString());
                        }
                  }
               } else if (ce.required) {
                  //then this parameter should have been there.  Don't cache it
                  if (debugEnabled)
                     Tr.debug(tc, "required session parameter \"" + ce.id + "\" missing from session. Not Caching.");
                  return null;
               }
            }
         }

         //cookies
         if (cookies != null) {
            Cookie[] cookieArray = request.getCookies();

            for (int i = 0; i < cookies.length; i++) {
               ConfigElementImpl ce = (ConfigElementImpl) cookies[i];

               boolean found = false;

               int j = 0;
               if (cookieArray.length > 0) {
                  do {
                     found = cookieArray[j].getName().equals(ce.id);
                  } while (!found && ++j < cookieArray.length);
               }

               if (found) {

                  if (ce.excludeAll) {
                     if (debugEnabled)
                        Tr.debug(tc, ce.id + " present on request, and is marked \"Exclude All\". Not Caching, though processing will continue to find invalidations.");
                     buildId = false;
                  }

                  String value = cookieArray[j].getValue();

                  if (ce.exclude != null) {
                     if (ce.exclude.contains(value)) {
                        if (debugEnabled)
                           Tr.debug(tc, ce.id + " has value " + value + " present on request, and is marked \"exclude\". Not Caching, though processing will continue to find invalidations.");
                        buildId = false;
                     }
                  }

                  if (ce.ignoreValue) {
                     if (debugEnabled)
                        Tr.debug(tc, "ignoring value of " + ce.id);
                     value = "";
                  }

                  //entry id component of this variable
                  if (buildId)
                     sb.append(ce.id).append("=").append(value).append(":");
                  //invalidations triggered by this variable
                  if (ce.invalidate != null) {
                     invalidations.add(new StringBuffer(ce.invalidate).append("=").append(value).toString());
                  }
                  //data id groups joined with this variable
                  if (buildId)
                     if (ce.dataId != null) {
                        if (debugEnabled)
                           Tr.debug(tc, "adding data id " + ce.dataId);
                        dataIds.add(new StringBuffer(ce.dataId).append("=").append(value).toString());
                     }
               } else if (ce.required) {
                  //then this parameter should have been there.  Don't cache it
                  if (debugEnabled)
                     Tr.debug(tc, ce + "\" missing from request. Not Caching.");
                  return null;
               }
            }
         }
      }

      if (invalidations.size() > 0) {
         processInvalidationList(invalidations);
         if (debugEnabled)
            Tr.debug(tc, "processed all invalidations, excluding this entry from caching");
         return null; //103045
      }
      //all invalidations are done at this point, so we can cut out the rest of the
      //operations if we are only concerned with invalidating
      if (invalidateonly || !buildId) {
         return null;
      }
      if (dataIds.size() > 0) {
         processDataIds(dataIds, request);
         if (debugEnabled)
            Tr.debug(tc, "processed all dataids");
      }

      String encoding = request.getCharacterEncoding();
      if (encoding != null) {
         sb.append(encoding); //@q1a
      }

      String out = sb.toString();
      if (debugEnabled)
         Tr.debug(tc, "generated cacheId " + out);
      if (out.equals("")) {
         return null;
      }

      return out;
   }

   public int getSharingPolicy(ServletCacheRequest request) {
      return sharingPolicy;
   }

   public void initialize(CacheConfig cc) {
      name = cc.getName();

      sharingPolicy = cc.getSharingPolicy();

      external = cc.getExternalCache() != null;

      if (external) {
         if (tc.isDebugEnabled())
            Tr.debug(tc, "This servlet is externally cacheable, creating the hash of request parameter elements");
         reqParmHash = new HashMap();
         requestParameters = cc.getRequestParameters();
         if (requestParameters != null) {
            int i;
            for (i = 0; i < requestParameters.length; i++) {
               reqParmHash.put(requestParameters[i].id, requestParameters[i]);
            }
            if (tc.isDebugEnabled())
               Tr.debug(tc, "Finished...created " + i + " parameters in the hash");
         } else {
            if (tc.isDebugEnabled())
               Tr.debug(tc, "No request parameters...using an empty hash");
         }
      } else {
         invalidateonly = cc.getInvalidateonly();

         requestParameters = cc.getRequestParameters();
         requestAttributes = cc.getRequestAttributes();
         sessionParameters = cc.getSessionParameters();
         cookies = cc.getCookies();

         //initialize the method caches
         if (requestAttributes != null)
            rAttrMethodCache = new Method[requestAttributes.length];
         if (sessionParameters != null)
            sParamMethodCache = new Method[sessionParameters.length];
      }

   }

   //executeBatchedInvalidations()
   void processInvalidationList(ArrayList invalidations) {
      for (int i = 0; i < invalidations.size(); i++) {
         DynamicCacheAccessor.getCache().invalidateById((String) invalidations.get(i), i == (invalidations.size() - 1));
      }
   }

   void processDataIds(ArrayList dataIds, ServletCacheRequest request) {
      FragmentInfo fragmentInfo = (FragmentInfo) request.getFragmentInfo();

      for (int i = 0; i < dataIds.size(); i++) {
         fragmentInfo.addDataId((String) dataIds.get(i));
      }

   }

}
