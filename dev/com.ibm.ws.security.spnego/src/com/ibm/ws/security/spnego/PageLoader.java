/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class PageLoader {
    private static final TraceComponent tc = Tr.register(PageLoader.class);

    public final static String DEFAULT_CONTENT_TYPE = "text/html";
    public final static String DEFAULT_CONTENT_ENCODING = "UTF-8";

    /** Marks the start of the contentEncoding within an HTTP contentType header. */
    private static final String HTTP_CONTENT_ENCODING_MARKER = "charset=";

    private transient String contentType;
    private transient String contentEncoding;
    private transient String content;
    private static String defaultContentEncoding = null;

    /*
     * This constructor will load custom error page URL. if customUrl is null or can not
     * load the custom error page from the customUrl, we will use the default error page.
     */
    PageLoader(String customUrl, String defaultErrorPageContent) {
        if (customUrl == null) {
            useDefaultErrorPage(defaultErrorPageContent);
        } else {
            boolean load = processCustomErrorPage(customUrl);
            if (!load) {
                useDefaultErrorPage(defaultErrorPageContent);
            }
        }
    }

    void useDefaultErrorPage(String defaultErrorPageContent) {
        content = defaultErrorPageContent;
        contentType = DEFAULT_CONTENT_TYPE;
        contentEncoding = DEFAULT_CONTENT_ENCODING;
    }

    boolean processCustomErrorPage(String customUrl) {
        boolean result = false;

        try {
            URL pageURL = new URL(customUrl);
            result = loadCustomErrorPage(pageURL, customUrl);
        } catch (MalformedURLException e) {
            Tr.error(tc, "SPNEGO_CUSTOM_ERROR_PAGE_MALFORMED", customUrl);
        }

        return result;
    }

    boolean loadCustomErrorPage(URL pageURL, String customUrl) {
        boolean result = false;
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            URLConnection pageConnection = pageURL.openConnection();
            if (pageConnection != null) {
                // Get input stream right away to facilitate proper cleanup.
                in = pageConnection.getInputStream();

                contentType = getContentType(pageConnection);

                contentEncoding = getContentEncoding(pageURL, pageConnection, contentType);

                isr = new InputStreamReader(in, contentEncoding);
                br = new BufferedReader(isr);
                content = getContent(br);
                result = true;
            }
        } catch (UnsupportedEncodingException e) {
            Tr.error(tc, "SPNEGO_LOAD_CUSTOM_ERROR_PAGE_ERROR", new Object[] { customUrl, e.getMessage() == null ? "UnsupportedEncodingException" : e.getMessage() });
        } catch (IOException e) {
            Tr.error(tc, "SPNEGO_LOAD_CUSTOM_ERROR_PAGE_ERROR", new Object[] { customUrl, e.getMessage() == null ? "IOException" : e.getMessage() });
        } finally {
            try {
                if (br != null)
                    br.close();
                if (isr != null)
                    isr.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "failed to clean up a stream: " + e);
                }
            }
        }
        return result;
    }

    protected String getContent(BufferedReader br) throws IOException {
        String line;
        StringBuffer pageBuffer = new StringBuffer();
        while ((line = br.readLine()) != null) {
            pageBuffer.append(line);
            pageBuffer.append("\r\n");
        }
        return pageBuffer.toString();
    }

    protected String getContentType(URLConnection pageConnection) {
        String ct = pageConnection.getContentType();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "contentType: " + contentType);
        }

        if (ct == null || "content/unknown".equalsIgnoreCase(ct)) {
            ct = "text/*";
        }
        return ct;
    }

    protected String getContentEncoding(URL pageURL, URLConnection pageConnection, String ct) {
        String ce = pageConnection.getContentEncoding();
        if (ce == null) {
            ce = getContentEncodingFromHttpContentType(ct);
            if (ce == null) {
                String input = getContentTypeFromPage(pageURL);
                ce = getContentEncodingFromHttpContentType(input);
                if (ce == null) {
                    ce = getDefaultContentEncoding();
                }
            }
        }

        return ce;
    }

    /**
     * Extracts the HTTP ContentEncoding from within the HTTP ContentType Header.
     * <p>
     * The HTTP ContentType header looks like <code>"text/html;charset=UTF-8"</code>.
     * This method extracts the contentEncoding portion (<code>"UTF-8"</code>) from
     * within the HTTP ContentEncoding header.
     * 
     * @param ct The complete HTTP ContentType header.
     * @return
     */
    protected final String getContentEncodingFromHttpContentType(String ct) {
        String encoding = null;
        if (ct != null) {
            StringTokenizer st = new StringTokenizer(ct, ";");
            while (st.hasMoreTokens() && encoding == null) {
                String value = st.nextToken();
                int pos = value.indexOf(HTTP_CONTENT_ENCODING_MARKER);
                if (pos > -1 && pos < value.length() - HTTP_CONTENT_ENCODING_MARKER.length()) {
                    encoding = value.substring(pos + HTTP_CONTENT_ENCODING_MARKER.length());
                    if (encoding != null) {
                        encoding = encoding.trim();
                    }
                }
            }
        }
        return encoding;
    }

    /**
     * Obtains the cached system default encoding.
     * <p>
     * The system default encoding is lazy evaluated. This unsynchronized
     * method gets the system default encoding the first time it is needed.
     * 
     * @return The system default encoding.
     */
    protected final String getDefaultContentEncoding() {
        if (defaultContentEncoding == null) {
            synchGetDefaultContentEncoding();
        }
        return defaultContentEncoding;
    }

    /**
     * Obtains the system default encoding the first time it is needed.
     * <p>
     * This synchronized method obtains the system default encoding the
     * first time it is needed. A synchronized method is not strictly
     * necessary (multiple threads would simply set {@link #defaultContentEncoding} multiple times.
     */
    private final synchronized void synchGetDefaultContentEncoding() {
        if (defaultContentEncoding == null) {
            defaultContentEncoding = System.getProperty("file.encoding");

            if (defaultContentEncoding == null) {
                defaultContentEncoding = DEFAULT_CONTENT_ENCODING;
            }
        }
    }

    /**
     ** Extract value of ContentType meta data from specified URL.
     ** the format of header is as follows:
     ** <head>
     ** <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
     ** </head>
     ** <body>....
     **/
    protected String getContentTypeFromPage(URL pageURL) {
        String output = null;
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            in = pageURL.openStream();
            isr = new InputStreamReader(in, "ISO-8859-1");
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                String lineLower = line.toLowerCase(Locale.ENGLISH);
                if ((lineLower.indexOf("meta") > 0) && (lineLower.indexOf("http-equiv") > 0) && (lineLower.indexOf("content-type") > 0)) {
                    // find Content-Type, content.
                    output = getContentTypeFromMeta(line);
                    if (output != null && output.length() > 0) {
                        break;
                    }
                }
                // don't use else if because everything might be in one line.
                if (lineLower.indexOf("</head>") > 0) {
                    // end of header, break
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Tr.error(tc, "SPNEGO_CUSTOM_ERROR_PAGE_CONTENT_TYPE_ERROR", new Object[] { e.getMessage() == null ? "UnsupportedEncodingException" : e.getMessage() });
        } catch (IOException e) {
            Tr.error(tc, "SPNEGO_CUSTOM_ERROR_PAGE_CONTENT_TYPE_ERROR", new Object[] { e.getMessage() == null ? "IOException" : e.getMessage() });
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "failed to clean up a stream", e);
                }
            }
        }
        return output;
    }

    /**
     ** Extract value of ContentType meta data from given String
     ** the format of Meta data is as follows:
     ** <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
     **/

    protected String getContentTypeFromMeta(String input) {
        String output = null;
        if (input != null && input.length() > 0) {
            String CONTENT = "content";
            String lower = input.toLowerCase(Locale.ENGLISH);
            String work = input;
            boolean exit = false;
            while (!exit) {
                int index = lower.indexOf(CONTENT);
                int offset = index + CONTENT.length();
                if ((index > 0) && (lower.length() > offset)) {
                    // found "content", check whether "=" is the next character other than whitespace
                    work = work.substring(offset).trim();
                    if (work.charAt(0) != '=') {
                        // this must be string of "Content-type", split the string and go on.
                        lower = work.toLowerCase(Locale.ENGLISH);
                    } else {
                        // found. at this point, work object starts with "="text/html; charset=UTF-8"......
                        String array[] = work.split("\"");
                        if (array.length > 2) {
                            work = array[1];
                            // make sure that there is at least one character from first and last index.
                            if (work.length() > 0) {
                                output = work.trim();
                            }
                        }
                        break;
                    }
                } else {
                    // no entry is found. exit.
                    exit = true;
                }
            }
        }
        return output;
    }

    String getContentType() {
        return this.contentType;
    }

    String getEncoding() {
        return this.contentEncoding;
    }

    String getContent() {
        return this.content;
    }
}
