// Search for IBM-FIX for any IBM updates.

package com.meterware.httpunit.cookies;
/********************************************************************************************************************
 * $Id$
 *
 * Copyright (c) 2002-2004,2008, Russell Gold
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 *******************************************************************************************************************/
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URL;
import java.util.*;


/**
 * A collection of HTTP cookies, which can interact with cookie and set-cookie header values.
 *
 * @author <a href="mailto:russgold@httpunit.org">Russell Gold</a>
 * @author <a href="mailto:drew.varner@oracle.com">Drew Varner</a>
 **/
public class CookieJar {

    private static final int DEFAULT_HEADER_SIZE = 80;

    private ArrayList _cookies = new ArrayList();
    private ArrayList _globalCookies = new ArrayList();
    private CookiePress _press;


    /**
     * Creates an empty cookie jar.
     */
    public CookieJar() {
        _press = new CookiePress( null );
    }


    /**
     * Creates a cookie jar which is initially populated with cookies parsed from the <code>Set-Cookie</code> and
     * <code>Set-Cookie2</code> header fields.
     * <p>
     * Note that the parsing does not strictly follow the specifications, but
     * attempts to imitate the behavior of popular browsers. Specifically,
     * it allows cookie values to contain commas, which the
     * Netscape standard does not allow for, but which is required by some servers.
     * </p>
     */
    public CookieJar( CookieSource source ) {
        _press = new CookiePress( source.getURL() );
        findCookies( source.getHeaderFields( "Set-Cookie" ), new RFC2109CookieRecipe() );
        findCookies( source.getHeaderFields( "Set-Cookie2" ), new RFC2965CookieRecipe() );
    }


    /**
     * find the cookies in the given Header String array
     * @param cookieHeader - the strings to look for cookies
     * @param recipe - the recipe to use
     */
    private void findCookies( String cookieHeader[], CookieRecipe recipe ) {
        for (int i = 0; i < cookieHeader.length; i++) {
            recipe.findCookies( cookieHeader[i] );
        }
    }


    /**
     * Empties this cookie jar of all contents.
     */
    public void clear() {
        _cookies.clear();
        _globalCookies.clear();
    }


    /**
     * Defines a cookie to be sent to the server on every request. This bypasses the normal mechanism by which only
     * certain cookies are sent based on their host and path.
     * @deprecated as of 1.6, use #putCookie
     **/
    public void addCookie( String name, String value ) {
        _globalCookies.add( new Cookie( name, value ) );
    }


    /**
     * Defines a cookie to be sent to the server on every request. This bypasses the normal mechanism by which only
     * certain cookies are sent based on their host and path.
     * 
     * Values of null will result in the cookie being removed.  Any other value will leave the
     * cookie unchanged expect for the value.
     * 
     * @since 1.6
     **/
    public void putCookie( String name, String value ) {
    	boolean foundCookie = false;
      for (Iterator iterator = _globalCookies.iterator(); iterator.hasNext();) {
          Cookie cookie = (Cookie) iterator.next();
            if (name.equals( cookie.getName() )) {
                foundCookie = true;
                if (value != null) {
                    cookie.setValue(value);
                } else {
                    iterator.remove();
                }
            }
        }
        
        for (Iterator iterator = _cookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();
            if (name.equals( cookie.getName() )) {
                foundCookie = true;
                if (value != null) {
                    cookie.setValue(value);
                } else {
                    iterator.remove();
                }
            }
        }
        
        // only add it if it does not already exist
        if (foundCookie == false) {
            _globalCookies.add( new Cookie( name, value ) );
        }
    }

    /**
     * Define a non-global cookie.  This cookie can be overwritten by subsequent cookie definitions
     * in http headers.  This cookie definition requires a domain and path.  If a global cookie is 
     * defined with the same name, this cookie is not added.
     */
    public void putSingleUseCookie(String name, String value, String domain, String path) {
    	for (Iterator iterator = _globalCookies.iterator(); iterator.hasNext();) {
    		Cookie cookie = (Cookie) iterator.next();
    		if (name.equals( cookie.getName() )) return;
    	}
     
     	for (Iterator iterator = _cookies.iterator(); iterator.hasNext();) {
     	    Cookie cookie = (Cookie) iterator.next();
     	    if (name.equals( cookie.getName() )) iterator.remove();
     	}
     
     	_cookies.add( new Cookie( name, value, domain, path) );
    }
    
    /**
     * Returns the name of all the active cookies in this cookie jar.
     **/
    public String[] getCookieNames() {
        final int numGlobalCookies = _globalCookies.size();
        String[] names = new String[ _cookies.size() + numGlobalCookies ];
        for (int i = 0; i < numGlobalCookies; i++) {
            names[i] = ((Cookie) _globalCookies.get(i)).getName();
        }
        for (int i = numGlobalCookies; i < names.length; i++) {
            names[i] = ((Cookie) _cookies.get( i-numGlobalCookies )).getName();
        }
        return names;
    }


    /**
     * Returns a collection containing all of the cookies in this jar.
     */
    public Collection getCookies() {
        final Collection collection = (Collection) _cookies.clone();
        collection.addAll( _globalCookies );
        return collection;
    }


    /**
     * Returns the value of the specified cookie.
     * @param name - the name of the cookie to get the value for
     * @return the value of the cookie
     **/
    public String getCookieValue( String name ) {
        Cookie cookie = getCookie( name );
        return cookie == null ? null : cookie.getValue();
    }


    /**
     * Returns the value of the specified cookie.
     **/
    public Cookie getCookie( String name ) {
        if (name == null) throw new IllegalArgumentException( "getCookieValue: no name specified" );
        for (Iterator iterator = _cookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();
            if (name.equals( cookie.getName() )) return cookie;
        }
        for (Iterator iterator = _globalCookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();
            if (name.equals( cookie.getName() )) return cookie;
        }
        return null;
    }


    /**
     * Returns the value of the cookie header to be sent to the specified URL.
     * Will return null if no compatible cookie is defined.
     **/
    public String getCookieHeaderField( URL targetURL ) {
        if (_cookies.isEmpty() && _globalCookies.isEmpty()) return null;
        StringBuffer sb = new StringBuffer( DEFAULT_HEADER_SIZE );
        HashSet restrictedCookies = new HashSet();
        for (Iterator i = _cookies.iterator(); i.hasNext();) {
            Cookie cookie = (Cookie) i.next();
            if (!cookie.mayBeSentTo( targetURL )) continue;
            restrictedCookies.add( cookie.getName() );
            if (sb.length() != 0) sb.append( "; " );
            sb.append( cookie.getName() ).append( '=' ).append( cookie.getValue() );
        }
        for (Iterator i = _globalCookies.iterator(); i.hasNext();) {
            Cookie cookie = (Cookie) i.next();
            if (restrictedCookies.contains( cookie.getName() )) continue;
            if (sb.length() != 0) sb.append( "; " );
            sb.append( cookie.getName() ).append( '=' ).append( cookie.getValue() );
        }
        return sb.length() == 0 ? null : sb.toString();
    }


    /**
     * Updates the cookies maintained in this cookie jar with those in another cookie jar. Any duplicate cookies in
     * the new jar will replace those in this jar.
     **/
    public void updateCookies( CookieJar newJar ) {
        for (Iterator i = newJar._cookies.iterator(); i.hasNext();) {
            addUniqueCookie( (Cookie) i.next() );
        }
    }


    /**
     * Add the cookie to this jar, replacing any previous matching cookie.
     */
    void addUniqueCookie( Cookie cookie ) {
        _cookies.remove( cookie );
        _cookies.add( cookie );
    }


    /**
     * base class for the cookie recipies - there are two different implementations
     * of this
     */
    abstract class CookieRecipe {

        /**
         * Extracts cookies from a cookie header. Works in conjunction with a cookie press class, which actually creates
         * the cookies and adds them to the jar as appropriate.
         *
         * 1. Parse the header into tokens, separated by ',' and ';' (respecting single and double quotes)
         * 2. Process tokens from the end:
         *    a. if the token contains an '=' we have a name/value pair. Add them to the cookie press, which
         *       will decide if it is a cookie name or an attribute name.
         *    b. if the token is a reserved word, flush the cookie press and continue.
         *    c. otherwise, add the token to the cookie press, passing along the last character of the previous token.
         */
        void findCookies( String cookieHeader ) {
            Vector tokens = getCookieTokens( cookieHeader );

            for (int i = tokens.size() - 1; i >= 0; i--) {
                String token = (String) tokens.elementAt( i );

                int equalsIndex = getEqualsIndex( token );
                if (equalsIndex != -1) {
                    _press.addTokenWithEqualsSign( this, token, equalsIndex );
                } else if (isCookieReservedWord( token )) {
                    _press.clear();
                } else {
                    _press.addToken( token, lastCharOf( (i == 0) ? "" : (String) tokens.elementAt( i - 1 ) ) );
                }
            }
        }


        private char lastCharOf( String string ) {
            return (string.length() == 0) ? ' ' : string.charAt( string.length()-1 );
        }


        /**
         * Returns the index (if any) of the equals sign separating a cookie name from the its value.
         * Equals signs at the end of the token are ignored in this calculation, since they may be
         * part of a Base64-encoded value.
         */
        private int getEqualsIndex( String token ) {
            if (!token.endsWith( "==" )) {
                return token.indexOf( '=' );
            } else {
                return getEqualsIndex( token.substring( 0, token.length()-2 ) );
            }
        }


        /**
         * Tokenizes a cookie header and returns the tokens in a
         * <code>Vector</code>.
         * handles the broken syntax for expires= fields ...
         * @param cookieHeader - the header to read
         * @return a Vector of cookieTokens as name=value pairs
         **/
        private Vector getCookieTokens(String cookieHeader) {
            StringReader sr = new StringReader(cookieHeader);
            StreamTokenizer st = new StreamTokenizer(sr);
            Vector tokens = new Vector();

            // clear syntax tables of the StreamTokenizer
            st.resetSyntax();

            // set all characters as word characters
            st.wordChars(0,Character.MAX_VALUE);

            // set up characters for quoting
            st.quoteChar( '"' ); //double quotes
            st.quoteChar( '\'' ); //single quotes

            // set up characters to separate tokens
            st.whitespaceChars(59,59); //semicolon
            // and here we run into trouble ...
            // see http://www.mnot.net/blog/2006/10/27/cookie_fun
            // ... Notice something about the above? It uses a comma inside of the date, 
            // without quoting the value. This makes it difficult for generic processors to handle the Set-Cookie header.
            st.whitespaceChars(44,44); //comma

            try {
                while (st.nextToken() != StreamTokenizer.TT_EOF) {
                	String tokenContent=st.sval;
                	// fix expires comma delimiter token problem
                	if (tokenContent.toLowerCase().startsWith("expires=")) {
                		if (st.nextToken() != StreamTokenizer.TT_EOF) {
                			tokenContent+=","+st.sval;
                		}	// if
                	} // if        		
                	tokenContent=tokenContent.trim();
                  tokens.addElement( tokenContent );
                }
            }
            catch (IOException ioe) {
                // this will never happen with a StringReader
            }
            sr.close();
            return tokens;
        }


        abstract protected boolean isCookieAttribute( String stringLowercase );


        abstract protected boolean isCookieReservedWord( String token );

    }


    /**
     * cookie Factory - creates cookies for URL s 
     *
     */
    class CookiePress {

        private StringBuffer _value = new StringBuffer();
        private HashMap _attributes = new HashMap();
        private URL     _sourceURL;


        /**
         * create a cookie press for the given URL
         * @param sourceURL
         */
        public CookiePress( URL sourceURL ) {
            _sourceURL = sourceURL;
        }


        void clear() {
            _value.setLength(0);
            _attributes.clear();
        }


        void addToken( String token, char lastChar ) {
            _value.insert( 0, token );
            if (lastChar != '=') _value.insert( 0, ',' );
        }


        /**
         * add from a token
         * @param recipe - the recipe to use
         * @param token - the token to use
         * @param equalsIndex - the position of the equal sign
         */
        void addTokenWithEqualsSign( CookieRecipe recipe, String token, int equalsIndex ) {
            String name = token.substring( 0, equalsIndex  ).trim();
            String value= token.substring( equalsIndex + 1 ).trim();
            _value.insert( 0, value );
            if (recipe.isCookieAttribute( name.toLowerCase() )) {
                _attributes.put( name.toLowerCase(), _value.toString() );
            } else {
                addCookieIfValid( new Cookie( name, _value.toString(), _attributes ) );
                _attributes.clear();
            }
            _value.setLength(0);
        }


        /**
         * add the given cookie if it is valid
         * @param cookie
         */
        private void addCookieIfValid( Cookie cookie ) {
            if (acceptCookie( cookie )) addUniqueCookie( cookie );
        }


        /**
         * accept the given cookie
         * @param cookie
         * @return
         */
        private boolean acceptCookie( Cookie cookie ) {
            if (cookie.getPath() == null) {
                cookie.setPath( getParentPath( _sourceURL.getPath() ) );
            } else {
                int status = getPathAttributeStatus( cookie.getPath(), _sourceURL.getPath() );
                if (status != CookieListener.ACCEPTED) {
                    reportCookieRejected( status, cookie.getPath(), cookie.getName() );
                    return false;
                }
            }

            if (cookie.getDomain() == null) {
                cookie.setDomain( _sourceURL.getHost() );
            } else if (!CookieProperties.isDomainMatchingStrict() && cookie.getDomain().equalsIgnoreCase( _sourceURL.getHost() )) {
                cookie.setDomain( _sourceURL.getHost() );
            } else {
                int status = getDomainAttributeStatus( cookie.getDomain(), _sourceURL.getHost() );
                if (status != CookieListener.ACCEPTED) {
                    reportCookieRejected( status, cookie.getDomain(), cookie.getName() );
                    return false;
                }
            }

            return true;
        }


        private String getParentPath( String path ) {
            int rightmostSlashIndex = path.lastIndexOf( '/' );
            return rightmostSlashIndex < 0 ? "/" : path.substring( 0, rightmostSlashIndex );
        }


        private int getPathAttributeStatus( String pathAttribute, String sourcePath ) {
            if (!CookieProperties.isPathMatchingStrict() || sourcePath.length() == 0 || sourcePath.startsWith( pathAttribute )) {
                return CookieListener.ACCEPTED;
            } else {
                return CookieListener.PATH_NOT_PREFIX;
            }
        }


        /**
         * get the domainAttribute Status for the given domainAttribute with the given sourceHost
         * @see http://wp.netscape.com/newsref/std/cookie_spec.html
         * @param domainAttribute
         * @param sourceHost
         * @return
         */
        private int getDomainAttributeStatus( String domainAttribute, String sourceHost ) {
        	// patch according to [ 1476380 ] Cookies incorrectly rejected despite valid domain
        	if (domainAttribute.equals(sourceHost)) {
        		return CookieListener.ACCEPTED;
        	}        	
          if (!domainAttribute.startsWith(".")) 
          	domainAttribute = '.' + domainAttribute;

          if (domainAttribute.lastIndexOf('.') == 0) {
            return CookieListener.DOMAIN_ONE_DOT;
          } else if (!sourceHost.endsWith( domainAttribute )) {
              return CookieListener.DOMAIN_NOT_SOURCE_SUFFIX;
          } else if (CookieProperties.isDomainMatchingStrict() &&
              sourceHost.lastIndexOf( domainAttribute ) > sourceHost.indexOf( '.' )) {
              return CookieListener.DOMAIN_TOO_MANY_LEVELS;
          } else {
              return CookieListener.ACCEPTED;
          }
        }
        
        private boolean reportCookieRejected( int reason, String attribute, String source ) {
            CookieProperties.reportCookieRejected( reason, attribute, source );
            return false;
        }

    }


    /**
     * Parses cookies according to
     * <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a>
     *
     * <br />
     * These cookies come from the <code>Set-Cookie:</code> header
     **/
    class RFC2109CookieRecipe extends CookieRecipe {

    	  /**
    	   * check whether the given lower case String is a cookie attribute
    	   * @param stringLowercase - the string to check
    	   * @return true - if the string is the name of a valid cookie attribute
    	   */
        protected boolean isCookieAttribute( String stringLowercase ) {
            return stringLowercase.equals("path") ||
                   stringLowercase.equals("domain") ||
                   stringLowercase.equals("expires") ||
                   stringLowercase.equals("comment") ||
                   stringLowercase.equals("max-age") ||
                   stringLowercase.equals("version");
        }


        protected boolean isCookieReservedWord( String token ) {
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            // IBM-FIX: Handle httponly reserved word. 
            // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	    //
	    // return token.equalsIgnoreCase( "secure" );
	    //

	    return (token.equalsIgnoreCase("secure")) || (token.equalsIgnoreCase("httponly"));

            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            // END IBM-FIX
            // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
        }
    }


    /**
     * Parses cookies according to
     * <a href="http://www.ietf.org/rfc/rfc2965.txt">RFC 2965</a>
     *
     * <br />
     * These cookies come from the <code>Set-Cookie2:</code> header
     **/
    class RFC2965CookieRecipe extends CookieRecipe {

        protected boolean isCookieAttribute( String stringLowercase ) {
            return stringLowercase.equals("path") ||
                   stringLowercase.equals("domain") ||
                   stringLowercase.equals("comment") ||
                   stringLowercase.equals("commenturl") ||
                   stringLowercase.equals("max-age") ||
                   stringLowercase.equals("version") ||
                   stringLowercase.equals("$version") ||
                   stringLowercase.equals("port");
        }


        protected boolean isCookieReservedWord( String token ) {
            return token.equalsIgnoreCase( "discard" ) || token.equalsIgnoreCase( "secure" );
        }
    }


}
