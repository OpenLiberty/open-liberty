/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.ServerCache;
import com.ibm.wsspi.cache.Constants;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

//
// Container for ESI processing code
//
public class ESISupport {

    private static TraceComponent tc = Tr.register(ESISupport.class,
                                                   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    // level of ESI supported by the surrogate
    public static int NO_ESI = 0;
    public static int ESI_10 = 100;
    public static int ESI_10PLUS = 101;
    public static int ESI_09 = 90; //ESI version for caching proxy LI3534-2
    public static int ESI_08 = 80; //ESI version for RRD LI3251-13

    // maximum time in seconds we will allow edge content to exist.  this
    // is used when an infinite timeout has been selected in dynacache
    static int maxTimeLimitInSeconds = 86400;

    //-----------------------------------------------------------------
    //
    //  ESI decision point.  When ESI 1.0 req header present....
    //
    //                    Edgeable                         !Edgeable
    //
    //  External request    set response header cacheable    set response header no-store
    //                      turn on ESI processing           normal processing
    //                      consumption is on
    //
    //  included page       return ESI fraglink              caching is on, subfragment consumption on
    //
    //-----------------------------------------------------------------
    public static final void handleESIPreProcessing(CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "handleESIPreProcessing()");
        FragmentComposer fragmentComposer = response.getFragmentComposer();
        FragmentComposer parentFragmentComposer = fragmentComposer.parent;
        int esiVersion = NO_ESI;
        if (parentFragmentComposer == null) {
            //external request, check ESI information...
            String s = getHeaderDirect(request, "Surrogate-Capability");
            if (s != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "got Surrogate-Capability header: " + s);
                int esiIndex = s.indexOf("ESI/1.0");
                if (esiIndex != -1) {
                    esiVersion = ESI_10;
                    if (s.indexOf('+', esiIndex) != -1)
                        esiVersion = ESI_10PLUS;
                } else if (s.indexOf("ESI/0.9+") != -1) {
                    esiVersion = ESI_09;
                } else if (s.indexOf("ESI/0.8") != -1) {
                    esiVersion = ESI_08;
                }
            }
        } else {
            esiVersion = parentFragmentComposer.getESIVersion();
        }
        fragmentComposer.setESIVersion(esiVersion);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "esiVersion: " + esiVersion);

        //LI3251-13
        if (esiVersion == ESI_08) {
            if (request.getAttribute(Constants.IBM_DYNACACHE_RRD_BUFFERING) != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "rrd request, set buffering for next include");
                response.setBufferingOutput(true);
                request.removeAttribute(Constants.IBM_DYNACACHE_RRD_BUFFERING);
            }
        } else if (esiVersion != NO_ESI) {
            // if we are in a forward or an external request, must buffer output
            // at this level
            String dispatcherType = (String) request.getAttribute(WebContainerConstants.DISPATCH_TYPE_ATTR);

            boolean forward = false;
            if (dispatcherType != null && dispatcherType.equals(WebContainerConstants.FORWARD)) {
                forward = true;
            }

            if (forward && parentFragmentComposer != null) {
                if (!parentResponseIsJSFFacesServlet(parentFragmentComposer)) {
                    //we are in a forward... turn off buffering in the response above.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "forward occuring on esi request, moving output buffering");
                    parentFragmentComposer.getResponse().setBufferingOutput(false);
                    response.setBufferingOutput(true);
                }
            } else if (parentFragmentComposer == null) {
                response.setBufferingOutput(true);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "handleESIPreProcessing()");
    }

    private static boolean parentResponseIsJSFFacesServlet(FragmentComposer parentFragmentComposer) {

        boolean isFacesServlet = false;

        if (null != parentFragmentComposer) {

            String servletClassName = parentFragmentComposer.getRequest().getServletClassName();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ServletClassName: " + servletClassName);
            }

            isFacesServlet = (servletClassName.indexOf("javax.faces.webapp.FacesServlet") != -1);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "parentFragmentComposerisFacesServlet: " + isFacesServlet);
            }

        }
        return isFacesServlet;
    }

    public static final void handleESIPostProcessing(CacheProxyResponse response, FragmentInfo fragmentInfo, boolean exceptionThrown) throws IOException {

        int esiVersion = response.getFragmentComposer().getESIVersion();
        if (tc.isEntryEnabled())
            Tr.entry(tc, "handleESIPostProcessing() esiVersion=" + esiVersion);

        //check if isSendRedirect is set for either the current composer or
        // any of the composers in its parent tree
        FragmentComposer composer = response.getFragmentComposer();
        boolean isSendRedirect = false;
        while (!isSendRedirect && composer != null) {
            isSendRedirect |= composer.isSendRedirect;
            composer = composer.parent;
        }

        //if there is an esi surrogate present and we are buffering output then
        //we are the outermost fragment that is buffering... need to set
        //surrogate control header
        if (esiVersion != NO_ESI && response.isBufferingOutput() && !isSendRedirect) {

            JSPCache jspCache = (JSPCache) ServerCache.getJspCache(fragmentInfo.getInstanceName());
            if (!response.containsHeader("Surrogate-Control") || jspCache.alwaysSetSurrogateControlHdr()) {
                setSurrogateControl(response, fragmentInfo, exceptionThrown, esiVersion);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Surrogate-Control already set");
            }
        }

        //PM75050 - always flush the output.  If we aren't buffering flushOutput won't do anything so there is no harm in always invoking it. 
        response.flushOutput();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "handleESIPostProcessing()");
        return;
    }

    private static void setSurrogateControl(CacheProxyResponse response, FragmentInfo fragmentInfo, boolean exceptionThrown, int esiVersion) {

        StringBuffer sb = null;
        // need to check parent to see if parent fragmentinfo was edgeable, if so
        // then use it
        FragmentComposer parentFragmentComposer = response.getFragmentComposer().parent;
        while (parentFragmentComposer != null) {
            if (parentFragmentComposer.fragmentInfo != null && parentFragmentComposer.fragmentInfo.isEdgeable()
                   && !parentFragmentComposer.fragmentInfo.isUncacheable())
                fragmentInfo = parentFragmentComposer.fragmentInfo;
            parentFragmentComposer = parentFragmentComposer.parent;
        }

        HttpServletResponse surrogateResponse = response;
        while (surrogateResponse instanceof HttpServletResponseWrapper)
            surrogateResponse = (HttpServletResponse) ((HttpServletResponseWrapper) surrogateResponse).getResponse();

		if (!surrogateResponse.isCommitted()) {
			/*
			 * If we have do-not-cache on an edgeable fragment, we need to use no-store so that it is
			 * not cached on the edge.
			 */
			if (fragmentInfo == null || !fragmentInfo.isEdgeable() || fragmentInfo.isUncacheable() || exceptionThrown || 
					(fragmentInfo.getDoNotCache() && fragmentInfo.isEdgeable ())) {
				sb = new StringBuffer(128).append("no-store");
				if (response.getContainsESIContent()) {
					sb.append(", content=\"ESI/1.0");
					if (esiVersion == ESI_10PLUS)
						sb.append("+\"");
					else
						sb.append("\"");
				}
			} else {
				if (fragmentInfo.getExpirationTime() > 0) {
					sb = new StringBuffer(128).append("max-age=").append((fragmentInfo.getExpirationTime() - System.currentTimeMillis()) / 1000);
				} else {
					sb = new StringBuffer(128).append("max-age=").append(maxTimeLimitInSeconds);
				}
				if (response.getContainsESIContent()) {
					if (esiVersion == ESI_10PLUS)
						sb.append(",content=\"ESI/1.0+\"");
					else {
						sb.append(",content=\"ESI/1.0\"");
					}
				}
				//add esi dep ids (1.0 and 1.0+)
				sb.append(", depid=\"").append(fragmentInfo.getId());
				Enumeration vEnum = fragmentInfo.getTemplates();
				while (vEnum.hasMoreElements()) {
					sb.append("\", depid=\"").append((String) vEnum.nextElement());
				}
				vEnum = fragmentInfo.getDataIds();
				while (vEnum.hasMoreElements()) {
					sb.append("\", depid=\"").append((String) vEnum.nextElement());
				}
				sb.append("\"");
				if (esiVersion == ESI_10PLUS || esiVersion == ESI_09 || esiVersion == ESI_08) {
					sb.append(",").append(fragmentInfo.getESICacheId());
				}
			}
			//need to bypass the response proxies so the surrogate control
			//is not in the cached response            
			//LI3251-13
			if (surrogateResponse instanceof IExtendedResponse) 
				((IExtendedResponse)surrogateResponse).setInternalHeader("Surrogate-Control",sb.toString());
			else
				surrogateResponse.setHeader("Surrogate-Control", sb.toString());
		} else {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "handleESIPostProcessing, Surrogate-Control already set");			
		}
		
        if (tc.isDebugEnabled())
            Tr.debug(tc, "set surrogate-control to:" + sb.toString());
    }

    //question:does ESI processor know about what webapp to use?
    //A) http://   B) /uri...  C) reluri/... is B understood?
    //Not supporting ESI includes across webapps right now, i.e
    //clarify when to use URL vs relative URI
    private static final void buildESIInclude(CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo, int esiVersion) throws IOException { //NK2C
        if (tc.isEntryEnabled())
            Tr.entry(tc, "buildESIInclude() esiVersion=" + esiVersion);

        // try to write using a writer.  if the outputstream has already
        // been obtained, switch to using the outputstream
        PrintWriter pw = null;
        try {
            pw = response.getWriter();
        } catch (IllegalStateException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheHook.buildESIInclude", "719");
            String enc = response.getCharacterEncoding();
            if (enc != null) {
                pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), enc));
            } else {
                pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
            }
        }

        //build the URL ourselves, since HttpUtils doesn't take includes into account.
        StringBuffer sb = new StringBuffer(128).append("<esi:include src=\"");
        if (fragmentInfo.getAlternateUrl() != null) { //NK2 begin

            String altUrl = fragmentInfo.getAlternateUrl();
            if (altUrl.startsWith("/"))
                altUrl = request._getContextPath() + altUrl;
            else {
                int idx = (request.getAbsoluteUri()).indexOf(request.getRelativeUri());
                String path = request.getAbsoluteUri().substring(0, idx);
                if (!path.endsWith("/"))
                    path = path + "/";
                altUrl = path + altUrl;

            }
            //append the original uri query parameters to the alternate url
            String fragmentInfoUri = fragmentInfo.getURI();
            if (esiVersion == ESI_10) {
                int index1 = fragmentInfoUri.indexOf('?');
                if (index1 > -1)
                    fragmentInfoUri = fragmentInfoUri.substring(0, index1);
                fragmentInfoUri = fragmentInfoUri + fragmentInfo.getESIQueryString();

            }
            int index2 = fragmentInfoUri.indexOf('?');
            if (index2 > -1)
                fragmentInfoUri = fragmentInfoUri.replace('?', '&');

            if (altUrl.indexOf("?") == -1)
                altUrl = altUrl + "?dynacache_alturl=" + fragmentInfoUri;
            else
                altUrl = altUrl + "&dynacache_alturl=" + fragmentInfoUri;
            sb.append(altUrl);
        } //NK2 end

        else if (esiVersion == ESI_10) {
            int index = fragmentInfo.getURI().indexOf('?');
            if (index > -1)
                sb.append(fragmentInfo.getURI().substring(0, index));
            else
                sb.append(fragmentInfo.getURI());
            sb.append(fragmentInfo.getESIQueryString());
        } else {
            sb.append(fragmentInfo.getURI());
        }
        pw.print(sb.append("\" />").toString());
        pw.flush();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "buildESIInclude() " + sb.toString());
    }

    /*
     * Make decision whether or not we should build an ESI include
     * returns: true if esi include was written to the stream
     */
    static public boolean shouldBuildESIInclude(CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo) throws IOException {
        int esiVersion = response.getFragmentComposer().getESIVersion();
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shouldBuildESIInclude() esiVersion=" + esiVersion + " isEdgeable=" + fragmentInfo.isEdgeable());
        if (esiVersion != NO_ESI && esiVersion != ESI_09 && esiVersion != ESI_08) {
            String alternatedUrl = request.getParameter("dynacache_alturl"); //NK2 begin
            String fragmentInfoUrl;
            int j = fragmentInfo.getURI().indexOf('?');
            if (j > -1)
                fragmentInfoUrl = fragmentInfo.getURI().substring(0, j);
            else
                fragmentInfoUrl = fragmentInfo.getURI();
            boolean isAltUrl = false;
            if (alternatedUrl != null && alternatedUrl.equals(fragmentInfoUrl))
                isAltUrl = true; //NK2 end

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "shouldBuildESIInclude() alternatedUrl=" + alternatedUrl + " fragmentInfoUrl=" + fragmentInfoUrl + " isAltUrl=" + isAltUrl);
            }

            // generate esi include if we are edgeable and then is an include/forward from the parent
            if (fragmentInfo.isEdgeable() && !response.getFragmentComposer().isExternalPage() && !isAltUrl) { //NK2C
                //then generate ESI include for this page and return
                buildESIInclude(request, response, fragmentInfo, esiVersion);//NK2C
                fragmentInfo.setBuildEsiInclude(true); //WCS
                response.setContainsESIContent(true);
                response.getFragmentComposer().setCacheType(FragmentComposer.POPULATED_CACHE);
                //turn off edgeable flag for this fragment so in case we were
                //forwarded here, the esipostprocessing will not write this fragment's
                //cache policy for the parent
                fragmentInfo.setEdgeable(false);
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "shouldBuildESIInclude() return=true");
                }
                return true;
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "shouldBuildESIInclude() return=false");
        }
        return false;
    }

    /**
     * Standard getHeader method is *very* slow... use the
     * getHeaderDirect method on SRTServletRequest
     */
    static final String getHeaderDirect(HttpServletRequest req, String key) {
        return req.getHeader(key);
    }

}
