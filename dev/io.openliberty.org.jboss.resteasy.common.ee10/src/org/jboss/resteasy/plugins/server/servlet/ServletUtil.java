package org.jboss.resteasy.plugins.server.servlet;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.specimpl.ResteasyUriInfo.InitData;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.resteasy.util.MediaTypeHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletUtil
{
   private static final Map<InitDataCacheKey, InitData> initDataCache = new ConcurrentHashMap<InitDataCacheKey, InitData>();      // Liberty change

   public static ResteasyUriInfo extractUriInfo(HttpServletRequest request, String servletPrefix)
   {
      String contextPath = request.getContextPath();
      if (servletPrefix != null && servletPrefix.length() > 0 && !servletPrefix.equals("/"))
      {
         if (!contextPath.endsWith("/") && !servletPrefix.startsWith("/"))
            contextPath += "/";
         contextPath += servletPrefix;
      }
      String queryString = request.getQueryString();
      String absolute;
      StringBuffer requestURL = request.getRequestURL();                                                // Liberty change
      String requestURLstring = requestURL.toString();                                                  // Liberty change
      if (queryString != null && queryString.length() > 0)
      {
         absolute = requestURL.append('?').append(queryString).toString();                              // Liberty change
      }
      else
      {
         absolute = requestURLstring;                                                                   // Liberty change
      }
      if (!absolute.contains(contextPath))
      {
         String encodedContextPath = Arrays.stream(contextPath.substring(1).split("/"))
                                           .map(s -> {
                                              try {
                                                 return URLEncoder.encode(s, "UTF-8");
                                                } catch (UnsupportedEncodingException ex) {
                                                 return s;
                                              }
                                             })
                                           .collect(Collectors.joining("/", "/", ""));
         if (absolute.contains(encodedContextPath))
         {
            absolute = absolute.replace(encodedContextPath, contextPath);
         }
      }
      // Liberty change begin      
      ResteasyUriInfo myResteasyUriInfo;
      if(InitData.canBeCached(absolute)) {
          InitDataCacheKey cacheKey = new InitDataCacheKey(requestURLstring, contextPath);
          InitData myInitData = initDataCache.get(cacheKey);
          if(myInitData == null) {
              myInitData = new InitData(absolute, contextPath);
              initDataCache.put(cacheKey, myInitData);
          }
          myResteasyUriInfo = new ResteasyUriInfo(absolute, contextPath, myInitData);
      } else {
          myResteasyUriInfo = new ResteasyUriInfo(absolute, contextPath);
      }
      
      return myResteasyUriInfo;
      // Liberty change end
   }

   public static ResteasyHttpHeaders extractHttpHeaders(HttpServletRequest request)
   {

      MultivaluedMap<String, String> requestHeaders = extractRequestHeaders(request);
      ResteasyHttpHeaders headers = new ResteasyHttpHeaders(requestHeaders);

      String contentType = request.getContentType();
      if (contentType != null)
         headers.getMutableHeaders().putSingle(HttpHeaders.CONTENT_TYPE, contentType);

      Map<String, Cookie> cookies = extractCookies(request);
      headers.setCookies(cookies);

      // test parsing should throw an exception on error
      headers.testParsing();

      return headers;

   }

   static Map<String, Cookie> extractCookies(HttpServletRequest request)
   {
      Map<String, Cookie> cookies = new HashMap<String, Cookie>();
      if (request.getCookies() != null)
      {
         for (jakarta.servlet.http.Cookie cookie : request.getCookies())
         {
            Cookie ck1 = new Cookie.Builder(cookie.getName())
                    .value(cookie.getValue())
                    .path(cookie.getPath())
                    .domain(cookie.getDomain())
                    .version(cookie.getVersion())
                    .build();
            cookies.put(cookie.getName(), ck1);

         }
      }
      return cookies;
   }

   public static List<MediaType> extractAccepts(MultivaluedMap<String, String> requestHeaders)
   {
      List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
      List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT);
      if (accepts == null)
         return acceptableMediaTypes;

      for (String accept : accepts)
      {
         acceptableMediaTypes.addAll(MediaTypeHelper.parseHeader(accept));
      }
      return acceptableMediaTypes;
   }

   public static List<String> extractLanguages(MultivaluedMap<String, String> requestHeaders)
   {
      List<String> acceptable = new ArrayList<String>();
      List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT_LANGUAGE);
      if (accepts == null)
         return acceptable;

      for (String accept : accepts)
      {
         String[] splits = accept.split(",");
         for (String split : splits)
            acceptable.add(split.trim());
      }
      return acceptable;
   }

   public static MultivaluedMap<String, String> extractRequestHeaders(HttpServletRequest request)
   {
      Headers<String> requestHeaders = new Headers<String>();

      Enumeration<String> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements())
      {
         String headerName = headerNames.nextElement();
         Enumeration<String> headerValues = request.getHeaders(headerName);
         while (headerValues.hasMoreElements())
         {
            String headerValue = headerValues.nextElement();
            requestHeaders.add(headerName, headerValue);
         }
      }
      return requestHeaders;
   }

   // Liberty change begin
   private static class InitDataCacheKey {
       private final String requestUrl;
       private final String contextPath;
       
       InitDataCacheKey (final String url, final String path) {
           this.requestUrl = url;
           this.contextPath = path;
       }
       
       @Override
       public boolean equals(Object o) {
           if(this == o)
               return true;
           if(o == null)
               return false;
           if (o.getClass() != InitDataCacheKey.class)
               return false;
           InitDataCacheKey that = (InitDataCacheKey) o;
           return this.requestUrl.equals(that.requestUrl) && this.contextPath.equals(that.contextPath);
       }
       
       @Override
       public int hashCode() {
           return Objects.hash(this.requestUrl, this.contextPath);
       }
   }
   // Liberty change end
}