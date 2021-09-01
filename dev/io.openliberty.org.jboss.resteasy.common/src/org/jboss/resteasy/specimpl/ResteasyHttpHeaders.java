package org.jboss.resteasy.specimpl;

import org.jboss.resteasy.util.CookieParser;
import org.jboss.resteasy.util.DateUtil;
import org.jboss.resteasy.util.MediaTypeHelper;
import org.jboss.resteasy.util.WeightedLanguage;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyHttpHeaders implements HttpHeaders
{

   private MultivaluedMap<String, String> requestHeaders;
   private MultivaluedMap<String, String> unmodifiableRequestHeaders;
   private Map<String, Cookie> cookies;

   public ResteasyHttpHeaders(final MultivaluedMap<String, String> requestHeaders)
   {
      this(requestHeaders, new HashMap<String, Cookie>());
   }

   public ResteasyHttpHeaders(final MultivaluedMap<String, String> requestHeaders, final boolean eagerlyInitializeEntrySet)
   {
      this(requestHeaders, new HashMap<String, Cookie>(), eagerlyInitializeEntrySet);
   }

   public ResteasyHttpHeaders(final MultivaluedMap<String, String> requestHeaders, final Map<String, Cookie> cookies)
   {
      this(requestHeaders, cookies, true);
   }

   public ResteasyHttpHeaders(final MultivaluedMap<String, String> requestHeaders, final Map<String, Cookie> cookies, final boolean eagerlyInitializeEntrySet)
   {
      this.requestHeaders = requestHeaders;
      this.unmodifiableRequestHeaders = new UnmodifiableMultivaluedMap<>(requestHeaders, eagerlyInitializeEntrySet);
      this.cookies = (cookies == null ? new HashMap<>() : cookies);
   }

   @Override
   public MultivaluedMap<String, String> getRequestHeaders()
   {
      return unmodifiableRequestHeaders;
   }

   public MultivaluedMap<String, String> getMutableHeaders()
   {
      return requestHeaders;
   }

   public void testParsing()
   {
      // test parsing should throw an exception on error
      getAcceptableMediaTypes();
      getMediaType();
      getLanguage();
      getAcceptableLanguages();

   }

   @Override
   public List<String> getRequestHeader(String name)
   {
      List<String> vals = unmodifiableRequestHeaders.get(name);
      return vals == null ? Collections.<String>emptyList() : vals;
   }

   @Override
   public Map<String, Cookie> getCookies()
   {
      mergeCookies();
      return Collections.unmodifiableMap(cookies);
   }

   public Map<String, Cookie> getMutableCookies()
   {
      mergeCookies();
      return cookies;
   }

   public void setCookies(Map<String, Cookie> cookies)
   {
      this.cookies = cookies;
   }

   @Override
   public Date getDate()
   {
      String date = requestHeaders.getFirst(DATE);
      if (date == null) return null;
      return DateUtil.parseDate(date);
   }

   @Override
   public String getHeaderString(String name)
   {
      List<String> vals = requestHeaders.get(name);
      if (vals == null) return null;
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (String val : vals)
      {
         if (first) first = false;
         else builder.append(",");
         builder.append(val);
      }
      return builder.toString();
   }

   @Override
   public Locale getLanguage()
   {
      String obj = requestHeaders.getFirst(HttpHeaders.CONTENT_LANGUAGE);
      if (obj == null) return null;
      return new Locale(obj);
   }

   @Override
   public int getLength()
   {
      String obj = requestHeaders.getFirst(HttpHeaders.CONTENT_LENGTH);
      if (obj == null) return -1;
      return Integer.parseInt(obj);
   }

   // because header string map is mutable, we only cache the parsed media type
   // and still do hash lookup
   private String cachedMediaTypeString;
   private MediaType cachedMediaType;
   @Override
   public MediaType getMediaType()
   {
      String obj = requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
      if (obj == null) return null;
      if (obj == cachedMediaTypeString) return cachedMediaType;
      cachedMediaTypeString = obj;
      cachedMediaType = MediaType.valueOf(obj);
      return cachedMediaType;
   }

   @Override
   public List<MediaType> getAcceptableMediaTypes()
   {
      List<String> vals = requestHeaders.get(ACCEPT);
      if (vals == null || vals.isEmpty()) {
         return Collections.singletonList(MediaType.WILDCARD_TYPE);
      } else {
         List<MediaType> list = new ArrayList<MediaType>();
         for (String v : vals) {
            StringTokenizer tokenizer = new StringTokenizer(v, ",");
            while (tokenizer.hasMoreElements()) {
               String item = tokenizer.nextToken().trim();
               list.add(MediaType.valueOf(item));
            }
         }
         MediaTypeHelper.sortByWeight(list);
         return Collections.unmodifiableList(list);
      }
   }

   @Override
   public List<Locale> getAcceptableLanguages()
   {
      List<String> vals = requestHeaders.get(ACCEPT_LANGUAGE);
      if (vals == null || vals.isEmpty()
                      || (vals.size() == 1 && "".equals(vals.get(0).trim()))) { // liberty change
         return Collections.singletonList(Locale.forLanguageTag("*"));
      }
      List<WeightedLanguage> languages = new ArrayList<WeightedLanguage>();
      for (String v : vals) {
         StringTokenizer tokenizer = new StringTokenizer(v, ",");
         while (tokenizer.hasMoreElements()) {
            String item = tokenizer.nextToken().trim();
            languages.add(WeightedLanguage.parse(item));
         }
      }
      Collections.sort(languages);
      List<Locale> list = new ArrayList<Locale>(languages.size());
      for (WeightedLanguage language : languages) list.add(language.getLocale());
      return Collections.unmodifiableList(list);
   }

   private void mergeCookies()
   {
      List<String> cookieHeader = requestHeaders.get(HttpHeaders.COOKIE);
      if (cookieHeader != null && !cookieHeader.isEmpty())
      {
         for (String s : cookieHeader)
         {
            List<Cookie> list = CookieParser.parseCookies(s);
            for (Cookie cookie : list)
            {
               cookies.put(cookie.getName(), cookie);
            }
         }
      }
   }
}
