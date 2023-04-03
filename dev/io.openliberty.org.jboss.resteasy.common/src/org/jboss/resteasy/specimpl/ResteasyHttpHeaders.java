package org.jboss.resteasy.specimpl;

import org.jboss.resteasy.util.CookieParser;
import org.jboss.resteasy.util.DateUtil;
import org.jboss.resteasy.util.MediaTypeHelper;
import org.jboss.resteasy.util.WeightedLanguage;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
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
    // Liberty change begin
    private final static List<MediaType> MEDIA_WILDCARD = Collections.singletonList(MediaType.WILDCARD_TYPE);
    private final static List<Locale> LANGUAGE_WILDCARD = Collections.singletonList(Locale.forLanguageTag("*"));

    private static final Map<String, List<MediaType>> mediaTypeCache;
    private static final Map<String, List<Locale>> languageCache;
    static {
        Map<String, List<MediaType>> mtCache = new HashMap<>();
        mtCache.put(MediaType.APPLICATION_ATOM_XML_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_ATOM_XML_TYPE));
        mtCache.put(MediaType.APPLICATION_FORM_URLENCODED_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        mtCache.put(MediaType.APPLICATION_JSON_PATCH_JSON_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_JSON_PATCH_JSON_TYPE));
        mtCache.put(MediaType.APPLICATION_JSON_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_JSON_TYPE)); 
        mtCache.put(MediaType.APPLICATION_OCTET_STREAM_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM_TYPE));
        mtCache.put(MediaType.APPLICATION_SVG_XML_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_SVG_XML_TYPE));
        mtCache.put(MediaType.APPLICATION_XHTML_XML_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_XHTML_XML_TYPE));
        mtCache.put(MediaType.APPLICATION_XML_TYPE.toString(), Collections.singletonList(MediaType.APPLICATION_XML_TYPE));
        mtCache.put(MediaType.MULTIPART_FORM_DATA_TYPE.toString(), Collections.singletonList(MediaType.MULTIPART_FORM_DATA_TYPE));
        mtCache.put(MediaType.SERVER_SENT_EVENTS_TYPE.toString(), Collections.singletonList(MediaType.SERVER_SENT_EVENTS_TYPE));
        mtCache.put(MediaType.TEXT_HTML_TYPE.toString(), Collections.singletonList(MediaType.TEXT_HTML_TYPE));
        mtCache.put(MediaType.TEXT_PLAIN_TYPE.toString(), Collections.singletonList(MediaType.TEXT_PLAIN_TYPE));
        mtCache.put(MediaType.TEXT_XML_TYPE.toString(), Collections.singletonList(MediaType.TEXT_XML_TYPE));
        mtCache.put(MediaType.WILDCARD_TYPE.toString(), MEDIA_WILDCARD);
        mediaTypeCache = Collections.unmodifiableMap(mtCache);

        Map<String,List<Locale>> langCache = new HashMap<>();
        langCache.put(Locale.CHINESE.toString(), Collections.singletonList(Locale.CHINESE));
        langCache.put(Locale.ENGLISH.toString(), Collections.singletonList(Locale.ENGLISH));
        langCache.put(Locale.FRENCH.toString(), Collections.singletonList(Locale.FRENCH));
        langCache.put(Locale.GERMAN.toString(), Collections.singletonList(Locale.GERMAN));
        langCache.put(Locale.ITALIAN.toString(), Collections.singletonList(Locale.ITALIAN));
        langCache.put(Locale.JAPANESE.toString(), Collections.singletonList(Locale.JAPANESE));
        langCache.put(Locale.KOREAN.toString(), Collections.singletonList(Locale.KOREAN));
        langCache.put(Locale.SIMPLIFIED_CHINESE.toString(), Collections.singletonList(Locale.SIMPLIFIED_CHINESE));
        langCache.put(Locale.TRADITIONAL_CHINESE.toString(), Collections.singletonList(Locale.TRADITIONAL_CHINESE));
        langCache.put("", LANGUAGE_WILDCARD);
        languageCache = Collections.unmodifiableMap(langCache);
    }
    // Liberty change end
    
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
          return MEDIA_WILDCARD;                                                // Liberty change
      } 
      // Liberty change begin
      if(vals.size() == 1){
          String type = vals.get(0).trim();
          if("*/*".equals(type)) {
              return MEDIA_WILDCARD;
          } else {
              List<MediaType> standard = mediaTypeCache.get(type);
              if(standard != null) {
                  return standard;
              }
          }
      }
      // Liberty change end
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

   @Override
   public List<Locale> getAcceptableLanguages()
   {
      List<String> vals = requestHeaders.get(ACCEPT_LANGUAGE);
      if (vals == null || vals.isEmpty()) {
          return LANGUAGE_WILDCARD;                                             // Liberty change
      }
      // Liberty change begin
      if(vals.size() == 1){
          String type = vals.get(0).trim();
          if("".equals(type)) {
              return LANGUAGE_WILDCARD;
          } else {
              List<Locale> standard = languageCache.get(type);
              if(standard != null) {
                  return standard;
              }
          }
      }
      // Liberty change end
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
