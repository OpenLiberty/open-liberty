/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.browser;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.ws.fat.util.StopWatch;
import com.ibm.ws.fat.util.browser.WebResponse.WebLinkFinder.FindLinkById;
import com.ibm.ws.fat.util.browser.WebResponse.WebLinkFinder.FindLinkByIndex;
import com.ibm.ws.fat.util.browser.WebResponse.WebLinkFinder.FindLinkByName;
import com.ibm.ws.fat.util.browser.WebResponse.WebLinkFinder.FindLinkByText;

/**
 * Encapsulates all the information returned from a server by submitting an HTTP
 * request. Instances are pre-populated; in other words, all response
 * information is cached in memory before the user is able to call any methods
 * on a particular instance.<br>
 * <br>
 * WARNING: Request Headers are not fully supported (In some cases, HttpUnit
 * does not allow you to view them). The requested URL of WebResponses from
 * frames is not always supported (again, HttpUnit's support of this feature is
 * limited).
 *
 * @author Tim Burns
 *
 */
public class WebResponse {

    protected static final String TOP_LEVEL_FRAME_NAME = null;
    protected static final String DEFAULT_FRAME_NAME = "";
    protected static final int TOP_LEVEL_FRAME_INDEX = -1;
    private static final String CLASS_NAME = WebResponse.class.getName();
    private static Logger LOG = Logger.getLogger(CLASS_NAME);

    private final WebBrowser browser;
    private final int number;
    protected String url;
    protected String requestedUrl;
    protected String responseBody;
    protected Document parsedResponseBody;
    protected final List<MyNameValuePair> cookies = new ArrayList<MyNameValuePair>();
    protected final List<MyNameValuePair> requestHeaders = new ArrayList<MyNameValuePair>();
    protected final List<MyNameValuePair> responseHeaders = new ArrayList<MyNameValuePair>();
    protected final String frameName;
    protected final int frameIndex;
    protected List<WebResponse> frames;
    protected List<WebLink> anchors;
    protected int responseCode;
    protected String humanReadableString;
    protected File lastFile;
    protected Date start;
    protected Date stop;

    /**
     * Convenience constructor for top-level frames
     *
     * @param browser
     *            the parent web browser
     * @param requestNum
     *            the number of this request (from the user)
     */
    protected WebResponse(WebBrowser browser, int requestNum) {
        this(browser, requestNum, TOP_LEVEL_FRAME_NAME, TOP_LEVEL_FRAME_INDEX);
    }

    /**
     * Convenience constructor for frames with no name
     *
     * @param browser
     *            the parent web browser
     * @param requestNum
     *            the number of this request (from the user)
     */
    protected WebResponse(WebBrowser browser, int requestNum, int frameIndex) {
        this(browser, requestNum, DEFAULT_FRAME_NAME, frameIndex);
    }

    /**
     * Convenience constructor for frame with a name
     *
     * @param browser
     *            the parent web browser
     * @param requestNum
     *            the number of this request (from the user)
     * @param frameName
     *            the name of this frame
     * @param frameIndex
     *            the index of this frame in the parent page
     */
    protected WebResponse(WebBrowser browser, int requestNum, String frameName, int frameIndex) {
        if (browser == null) {
            throw new IllegalArgumentException("Unable to construct an instance of " + this.getClass().getName() + " becuase the parent WebBrowser is null.");
        }
        this.browser = browser;
        this.number = requestNum;
        this.url = null;
        this.requestedUrl = null;
        this.responseBody = null;
        //this.cookies = null;
        //this.requestHeaders = null;
        //this.responseHeaders = null;
        this.frames = new Vector<WebResponse>(0, 1);
        this.responseCode = -1;
        this.humanReadableString = null; // initialize upon first invocation of toString()
        this.frameName = frameName;
        this.frameIndex = frameIndex;
        this.anchors = null;
        this.lastFile = null;
        this.setStart();
        this.setStop();
    }

    @Override
    public String toString() {
        if (this.humanReadableString == null) {
            this.humanReadableString = this.getHumanReadableString();
        }
        return this.humanReadableString;
    }

    /**
     * Produces a human-readable identifier for this instance; internally used
     * for logging.
     *
     * @return a human-readable identifier for this instance. Must not return
     *         null!
     */
    protected String getHumanReadableString() {
        StringBuffer name = new StringBuffer();
        name.append(this.getBrowser().toString());
        name.append(" Response ");
        name.append(this.getNumber());
        if (this.frameIndex != TOP_LEVEL_FRAME_INDEX) {
            name.append(" Frame ");
            if (DEFAULT_FRAME_NAME.equals(this.frameName)) {
                name.append("Index ");
                name.append(this.frameIndex);
            } else {
                name.append("Name \"");
                name.append(this.frameName);
                name.append("\"");
            }
        }
        return name.toString();
    }

    /**
     * Retrieves the browser that submitted the request responsible for this
     * instance
     *
     * @return the browser associated with this instance
     */
    public WebBrowser getBrowser() {
        return this.browser;
    }

    /**
     * Retrieve the unique identifier for this instance
     *
     * @return a unique identifier for this instance
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Convenience method for saving response information to the web browser's
     * default result directory. Two files will be created in the specified
     * directory, uniquely named for this instance.
     *
     * @throws WebBrowserException
     *             if response information cannot be saved
     */
    public void save() throws WebBrowserException {
        this.save(null);
    }

    /**
     * Convenience method for saving response information to a directory. Two
     * files will be created in the specified directory, uniquely named for this
     * instance.
     *
     * @param directory
     *            the directory where you want to store response information.
     *            null indicates that the browser's default result directory
     *            should be used (if the browser's default result directory is
     *            null, then no save will occur.
     *
     * @throws WebBrowserException
     *             if response information cannot be saved
     */
    public void save(File directory) throws WebBrowserException {
        File dir = directory;
        if (dir == null) {
            dir = this.getBrowser().getResultDirectory(); // may return null
        }
        if (dir == null) {
            return;
        }
        // start with a human-readable String for this instance
        String name = this.toString();

        // replace the non-word characters with the empty String
        name = name.replaceAll("\\W", ""); // "\W" is a regular expression for a non-word character; the opposite is "\w"=[a-zA-Z_0-9]

        // chose an extension that matches the content type (only supports XML and HTML)
        List<String> contentTypeList = this.getResponseHeader("Content-Type", true);
        String contentType = (contentTypeList == null || contentTypeList.size() == 0) ? null : contentTypeList.get(0);
        String ext = (contentType != null && contentType.contains("text/xml")) ? ".xml" : ".html"; // default to html

        // write the response body to the File
        File body = new File(dir, name + "Body" + ext);
        this.saveResponseBody(body);

        // write response information to the File
        File info = new File(dir, name + "Info.txt");
        this.saveResponseInformation(info);
    }

    /**
     * Saves the body of this response to the specified File
     *
     * @param file
     *            the File where you want to store response information.
     *            null indicates that no save should be performed.
     * @throws WebBrowserException
     *             if response information cannot be saved
     */
    public void saveResponseBody(File file) throws WebBrowserException {
        if (file == null) {
            return;
        }
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Saving the body of " + this.toString() + " to: " + file);
        }
        Vector<String> body = new Vector<String>(0, 1);
        body.addElement(this.responseBody);
        this.writeStringsToFile(file, body, false, false); // don't call getResponseBody(); that operation logs extra information
        this.lastFile = file;
    }

    /**
     * Saves information about this response to the specified File (headers,
     * cookies, etc)
     *
     * @param file
     *            the File where you want to store response information. null
     *            indicates that no save should be performed.
     * @throws WebBrowserException
     *             if response information cannot be saved
     */
    public void saveResponseInformation(File file) throws WebBrowserException {
        if (file == null) {
            return;
        }
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Saving information about " + this.toString() + " to: " + file);
        }
        Vector<String> info = new Vector<String>(0, 1);
        info.addElement(this.toString());
        info.addElement("Request URL:  " + this.requestedUrl); // don't call getRequestedUrl(); that operation logs extra information (same logic applies for others below)
        info.addElement("Response URL: " + this.url);
        info.addElement("Response Code: " + this.responseCode);
        info.addElement("Request Sent:    " + StopWatch.formatTime(this.start));
        info.addElement("Response Parsed: " + StopWatch.formatTime(this.stop));
        info.addElement("Time Elapsed: " + StopWatch.convertMillisecondsToString(this.stop.getTime() - this.start.getTime()));
        info.addElement("Request Headers:");
        for (String line : this.getSummary(this.requestHeaders)) {
            info.addElement(line);
        }
        info.addElement("Response Headers:");
        for (String line : this.getSummary(this.responseHeaders)) {
            info.addElement(line);
        }
        info.addElement("Cookies:");
        for (String line : this.getSummary(this.cookies)) {
            info.addElement(line);
        }
        this.writeStringsToFile(file, info, false, true);
    }

    /**
     * Retrieves a File object that represents the last File containing the
     * response body that was saved to the local file system using one of the
     * save(...) methods. If no File was recently saved, returns null.
     *
     * @return the last File containing the response body that was saved to the
     *         local file system using one of the save(...) methods
     */
    public File getResponseBodyAsFile() {
        return this.lastFile;
    }

    protected void writeStringsToFile(File file, List<String> lines, boolean append, boolean appendEndLines) throws WebBrowserException {
        if (file == null) {
            return;
        }
        PrintWriter writer;
        try {
            File directory = file.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            writer = new PrintWriter(new FileWriter(file, append), true);
        } catch (Throwable cause) {
            throw new WebBrowserException(this.toString() + " failed to open a " + FileWriter.class.getName() + " for the File " + file, cause);
        }
        try {
            if (lines != null) {
                for (String line : lines) {
                    if (appendEndLines) {
                        writer.println(line);
                    } else {
                        writer.print(line);
                    }
                }
            }
        } catch (Throwable cause) {
            throw new WebBrowserException(this.toString() + " failed to write to the File " + file, cause);
        } finally {
            try {
                writer.close();
            } catch (Throwable cause) {
                throw new WebBrowserException(this.toString() + " failed to close the " + FileWriter.class.getName() + " for the File " + file, cause);
            }
        }
    }

    /**
     * Retrieves the value of the specified response header, ignoring case if
     * desired
     *
     * @param key
     *            the name of the response header to search for
     * @param ignoreCase
     *            true indicates that the case of each character in the
     *            specified header should be ignored while searching for a match
     * @return the value of the specified response header, or null if no such
     *         header is found
     */
    public List<String> getResponseHeader(String key, boolean ignoreCase) {
        return this.getValue(this.responseHeaders, key, ignoreCase);
    }

    /**
     * Retrieves the value of the specified request header, ignoring case if
     * desired
     *
     * @param key
     *            the name of the request header to search for
     * @param ignoreCase
     *            true indicates that the case of each character in the
     *            specified header should be ignored while searching for a match
     * @return the value of the specified request header, or null if no such
     *         header is found
     */
    public List<String> getRequestHeader(String key, boolean ignoreCase) {
        return this.getValue(this.requestHeaders, key, ignoreCase);
    }

    /**
     * Retrieves the value of the specified cookie, ignoring case if desired
     *
     * @param key
     *            the name of the cookie to search for
     * @param ignoreCase
     *            true indicates that the case of each character in the
     *            specified cookie key should be ignored while searching for a
     *            match
     * @return the value of the specified cookie, or null if no such cookie is
     *         found
     */
    public List<String> getCookie(String key, boolean ignoreCase) {
        return this.getValue(this.cookies, key, ignoreCase);
    }

    protected List<String> getValue(List<MyNameValuePair> map, String key, boolean ignoreCase) {
        ArrayList<String> returnedList = new ArrayList<String>();
        if (map == null || key == null) {
            return null;
        }
        for (MyNameValuePair p : map) {
            String name = p.getName();
            if (ignoreCase) {
                if (key.equalsIgnoreCase(name)) {
                    returnedList.add(p.getValue());
                }
            } else {
                if (key.equals(name)) {
                    returnedList.add(p.getValue());
                }
            }
        }
        if (returnedList.size() > 0) {
            return returnedList;
        }
        return null;
    }

    protected void setStart() {
        this.setStart(new Date(System.currentTimeMillis()));
    }

    protected void setStart(Date time) {
        this.start = time;
    }

    protected void setStop() {
        this.stop = new Date(System.currentTimeMillis());
    }

    protected void setUrl(String url) {
        this.url = url;
    }

    protected void setRequestedUrl(String url) {
        this.requestedUrl = url;
    }

    protected void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    protected void addCookie(String name, String value) {
        this.cookies.add(new MyNameValuePair(name, value));
    }

    protected void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Retrieves the request headers associated with the current page.
     *
     * @return the request headers associated with the current page.
     */
    public List<MyNameValuePair> getRequestHeaders() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Request headers associated with " + this.toString() + ":");
            for (String line : this.getSummary(this.requestHeaders)) {
                LOG.info(line);
            }
        }
        return this.requestHeaders;
    }

    protected void addRequestHeader(String name, String value) {
        this.requestHeaders.add(new MyNameValuePair(name, value));
    }

    protected void addResponseHeader(String name, String value) {
        this.responseHeaders.add(new MyNameValuePair(name, value));
    }

    /**
     * Parses the response body as an HTML document,
     * and caches the result for future reference.
     *
     * @return he response body as an HTML document
     * @throws WebBrowserException if the response body does not contain valid HTML
     */
    public Document getResponseBodyAsDocument() throws WebBrowserException {
        if (this.parsedResponseBody == null) {
            String method = "parseResponseBody";
            LOG.logp(Level.FINE, CLASS_NAME, method, "Parsing the body of " + this.toString());
            if (this.responseBody == null) {
                throw new WebBrowserException("Unable to parse the body of " + this.toString() + " becuase the body is null");
            }
            try {
                return this.parsedResponseBody = HtmlParser.getInstance().parse(this.responseBody);
            } catch (Throwable e) {
                throw new WebBrowserException("Unable to parse the body of " + this.toString(), e);
            }
        }
        return this.parsedResponseBody;
    }

    /**
     * Returns the name of the frame containing this page. If the frame has no
     * name, then the empty String is returned. If this instance represents the
     * top-level frame, null is returned.
     *
     * @return Returns the name of the frame containing this page.
     */
    public String getFrameName() {
        return this.frameName;
    }

    /**
     * Returns the index of the frame that contains this response, where frame
     * indexes match the order that they appear in the parent frame. For
     * example, if this response is contained by the first frame in the parent
     * frame, then this method would return 0. If this response is the top-level
     * frame, -1 is returned.
     *
     * @return the index of this frame on the parent page.
     */
    public int getFrameIndex() {
        return this.frameIndex;
    }

    protected void addFrame(WebResponse frame) {
        this.frames.add(frame);
    }

    public List<WebResponse> getFrames() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("The response body of " + this.toString() + " contains " + this.frames.size() + " frame(s)");
        }
        return this.frames;
    }

    /**
     * Parses the response body to construct one WebResponse object for each
     * HTML frame found in the body. The URL for each WebResponse has
     * corresponds to the frame source of each frame, but no other response
     * information is populated in the object. It is left to the caller to
     * populate each WebResponse object, perhaps by making a request to the
     * given URL. Each WebResponse is added to the cached List of frames for
     * this instance.
     *
     * @throws WebBrowserException
     *             if a problem occurs while parsing the response body and
     *             constructing empty WebResponse objects
     */
    protected void parseFrames() throws WebBrowserException {
        String method = "parseFrames";
        try {
            Document document = this.getResponseBodyAsDocument();
            LOG.logp(Level.FINE, CLASS_NAME, method, "Searching the body of " + this.toString() + " to locate all frame elements");
            NodeList elements = document.getElementsByTagName("frame");
            int numElements = elements.getLength();
            LOG.logp(Level.FINE, CLASS_NAME, method, "Found " + numElements + " frame(s)");
            for (int i = 0; i < numElements; i++) {
                Element element = (Element) elements.item(i);
                String src = element.getAttribute("src");
                if (src == null || src.trim().length() == 0) {
                    throw new IllegalArgumentException("Frame element " + i + " in " + this.toString() + " does not contain a 'src' attribute.");
                }
                String url = this.resolveRelativeUrl(src);
                String name = element.getAttribute("name");
                WebResponse frame = null;
                if (name == null || name.length() == 0) {
                    frame = new WebResponse(this.getBrowser(), this.getNumber(), i);
                } else {
                    frame = new WebResponse(this.getBrowser(), this.getNumber(), name, i);
                }
                frame.setUrl(url);
                frame.setRequestedUrl(url);
                this.addFrame(frame);
            }
        } catch (Throwable e) {
            throw new WebBrowserException("Failed to request frame elements in " + this.toString(), e);
        }
    }

    protected String resolveRelativeUrl(String relativeUrl) throws WebBrowserException {
        URL url = null;
        try {
            url = new URL(relativeUrl); // see if the url isn't relative
        } catch (MalformedURLException e) {
            // usually, getting here indicates that theURL is relative
            URL context = null;
            try {
                context = new URL(this.url); // don't call this.getUrl() to avoid unnecessary logging
            } catch (MalformedURLException e1) {
                throw new WebBrowserException("Failed to construct a URL object representing the url of " + this.toString() + ": " + this.url, e);
            }
            try {
                url = new URL(context, relativeUrl);
            } catch (MalformedURLException e1) {
                throw new WebBrowserException("Failed to construct a URL object representing a relative url in " + this.toString() + ": " + relativeUrl, e);
            }
        }
        return url.toString();
    }

    /**
     * Parses the HTML of the current page to locate all anchor elements, and
     * constructs one WebLink object to represent each anchor element. Caches
     * the resulting list for future reference. If an error occurs while
     * initializing one particular WebLink instance, a message will be logged,
     * but an attempt will be made to initialize all other WebLink instances.
     *
     * @return a List of WebLink objects representing each anchor on this page.
     *         Never returns null, and each member of the list is not null.
     * @throws WebBrowserException
     *             if the links can not be determined
     */
    public List<WebLink> getLinks() throws WebBrowserException {
        if (this.anchors == null) {
            this.anchors = new Vector<WebLink>(0, 1);
            Document document = this.getResponseBodyAsDocument();
            String method = "getAnchors";
            LOG.info("Searching the body of " + this.toString() + " to locate all anchor elements");
            NodeList anchors = document.getElementsByTagName("a");
            int numAnchors = anchors.getLength();
            for (int i = 0; i < numAnchors; i++) {
                Element anchor = (Element) anchors.item(i);
                try {
                    this.anchors.add(new WebLink(this, anchor, i));
                } catch (Exception e) {
                    // do not quit; try to initialize as many anchors as possible
                    LOG.log(Level.INFO, this.toString() + " failed to construct anchor element index " + i, e);
                }
            }
            LOG.logp(Level.FINE, CLASS_NAME, method, "Found " + numAnchors + " anchor(s)");
        }
        return this.anchors;
    }

    /**
     * Submits an HTTP request to the hyperlink indicated by the specified
     * index. Hyperlinks are indexed by their relative position on the current
     * response body.
     *
     * @param index
     *            the index of the desired hyperlink in the current response
     *            body
     * @return the response received from submitting this request
     * @throws WebBrowserException
     *             if a problem happens while submitting the HTTP request
     */
    public WebResponse clickOnLinkByIndex(int index) throws WebBrowserException {
        return new FindLinkByIndex(this, index).click();
    }

    /**
     * Submits an HTTP request to the hyperlink indicated by the specified name.
     * If multiple anchors share the same name, the first anchor found on the
     * page will be clicked.
     *
     * @param name
     *            the name of the desired hyperlink in the current response body
     * @return the response received from submitting this request
     * @throws WebBrowserException
     *             if a problem happens while submitting the HTTP request
     */
    public WebResponse clickOnLinkByName(String name) throws WebBrowserException {
        return new FindLinkByName(this, name).click();
    }

    /**
     * Submits an HTTP request to the hyperlink indicated by the specified ID.
     * If multiple anchors share the same ID, the first anchor found on the
     * page will be clicked.
     *
     * @param id
     *            the ID of the desired hyperlink in the current response body
     * @return the response received from submitting this request
     * @throws WebBrowserException
     *             if a problem happens while submitting the HTTP request
     */
    public WebResponse clickOnLinkById(String id) throws WebBrowserException {
        return new FindLinkById(this, id).click();
    }

    /**
     * Submits an HTTP request to the hyperlink indicated by the specified text.
     * If multiple anchors share the same text, the first anchor found on the
     * page will be clicked.
     *
     * @param text
     *            the text of the desired hyperlink in the current response
     *            body; this is the text found within the anchor element.
     * @return the response received from submitting this request
     * @throws WebBrowserException
     *             if a problem happens while submitting the HTTP request
     */
    public WebResponse clickOnLinkByText(String text) throws WebBrowserException {
        return new FindLinkByText(this, text).click();
    }

    /**
     * Retrieves the URL of the current page. Note that this URL may represent
     * the redirected URL if the URL that was used to construct this object was
     * itself a redirect page. To retrieve the original URL, use <code>this.getRequestedUrl()</code>.
     *
     * @return the URL of the current page
     */
    public String getUrl() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("The URL associated with " + this.toString() + " is: " + this.url);
        }
        return this.url;
    }

    /**
     * Retrieves the URL that was used to submit the original request for this
     * instance. To retrieve the redirected URL, use <code>this.getUrl()</code>.
     *
     * @return the URL of the original request
     */
    public String getRequestedUrl() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("The request URL associated with " + this.toString() + " is: " + this.requestedUrl);
        }
        return this.requestedUrl;
    }

    protected List<String> getSummary(List<MyNameValuePair> pairs) {
        Vector<String> summary = new Vector<String>(0, 1);
        if (pairs == null || pairs.size() == 0) {
            summary.addElement("(None)");
        } else {
            int count = 1;
            for (MyNameValuePair p : pairs) {
                String name = p.getName();
                StringBuffer buffer = new StringBuffer();
                buffer.append(count);
                buffer.append(": ");
                buffer.append(name);
                buffer.append("=");
                buffer.append(p.getValue());
                summary.addElement(buffer.toString());
                count++;
            }
        }
        return summary;
    }

    /**
     * Retrieves the cookies associated with the current page.
     *
     * @return the cookies associated with the current page.
     */
    public List<MyNameValuePair> getCookies() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Cookies associated with " + this.toString() + ":");
            for (String line : this.getSummary(this.cookies)) {
                LOG.info(line);
            }
        }
        return this.cookies;
    }

    /**
     * Retrieves the response headers associated with the current page.
     *
     * @return the response headers associated with the current page.
     */
    public List<MyNameValuePair> getResponseHeaders() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Response headers associated with " + this.toString() + ":");
            for (String line : this.getSummary(this.responseHeaders)) {
                LOG.info(line);
            }
        }
        return this.responseHeaders;
    }

    /**
     * This method filters the response headers to the header that we're looking for, and returns them in a String array.
     *
     * @param headerToFind - The header to find
     * @return - A String array of any found headers.
     */
    public String[] getResponseHeaders(String headerToFind) {
        List<String> selectedHeaders = new ArrayList<String>();
        for (MyNameValuePair header : getResponseHeaders()) {
            String headerName = header.getName();
            if (headerName.equalsIgnoreCase(headerToFind)) {
                selectedHeaders.add(header.getValue());
            }
        }
        return selectedHeaders.toArray(new String[] {});
    }

    public int getStatusCode() {
        return this.responseCode;
    }

    /**
     * Retrieves the status code of the response
     *
     * @return the status code of the response
     */
    public int getResponseCode() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("The response code associated with " + this.toString() + " is " + this.responseCode + ".");
        }
        return this.responseCode;
    }

    /**
     * Retrieves the body of the response as a String
     *
     * @return the body of the response as a String
     */
    public String getResponseBody() {
        if (LOG.isLoggable(Level.INFO)) {
            String method = "getResponseBody";
            LOG.info("Retrieving the response body from " + this.toString() + ".");
            // it's usually not necessary to log this, since all responses can be saved anyway
            if (LOG.isLoggable(Level.FINE)) {
                LOG.logp(Level.FINE, CLASS_NAME, method, "The response body retrieved is:");
                LOG.logp(Level.FINE, CLASS_NAME, method, "~~~ Beginning of body ~~~");
                LOG.logp(Level.FINE, CLASS_NAME, method, this.responseBody);
                LOG.logp(Level.FINE, CLASS_NAME, method, "~~~ End of body ~~~~~~~~~");
            }
        }
        return this.responseBody;
    }

    /**
     * Verifies that the response body contains the specified text
     *
     * @param text
     *            The text that should be found within the response body
     * @return the response body
     * @throws WebBrowserException
     *             if the response body does not contain the expected text
     */
    public String verifyResponseBodyContains(String text) throws WebBrowserException {
        return this.verifyResponseBody(text, true);
    }

    /**
     * Verifies that the response body does not contain the specified text
     *
     * @param text
     *            The text that shouldn't be found within the response body
     * @return the response body
     * @throws WebBrowserException
     *             if the validation fails
     */
    public String verifyResponseBodyDoesNotContain(String text) throws WebBrowserException {
        return this.verifyResponseBody(text, false);
    }

    /**
     * Verifies that the response body contains (or does not contain) the
     * specified text
     *
     * @param text
     *            The text that should (or shouldn't) be found within the
     *            response body
     * @param contained
     *            true indicates that the text should be found within the
     *            response body; false indicates that the text should not be
     *            found
     * @return the response body
     * @throws WebBrowserException
     *             if the validation fails
     */
    public String verifyResponseBody(String text, boolean contained) throws WebBrowserException {
        String msg = contained ? "contains" : "does not contain";
        LOG.info("Verifying that the body of " + this.toString() + " " + msg + ": " + text);
        if (text == null) {
            throw new IllegalArgumentException("Unable to verify the body of " + this.toString() + " because the search text is null.");
        }
        if (this.responseBody == null) {
            throw new WebBrowserException("The body of " + this.toString() + " is null.");
        }
        boolean foundText = this.responseBody.indexOf(text) >= 0;
        if (contained) {
            if (!foundText) {
                throw new WebBrowserException("The body of " + this.toString() + " does not contain: " + text +
                                              " - Response body: " + this.responseBody);
            }
        } else {
            if (foundText) {
                throw new WebBrowserException("The body of " + this.toString() + " contains: " + text);
            }
        }
        LOG.info("Yep!");
        return this.responseBody;
    }

    /**
     * Verifies that the response body contains (or does not contain) the
     * specified text
     *
     * @param text
     *            The text that should be found within the response body
     * @param desiredMatches
     *            The number of times the matching text should be found
     * @param extraMatch
     *            (Optional) additional text to match, this can be null if
     *            there is no more text to match.
     * @return the response body
     * @throws WebBrowserException
     *             if the validation fails
     */
    public String verifyResponseBodyWithRepeatMatchAndExtra(String match, int desiredMatches, String extraMatch) throws WebBrowserException {
        LOG.info("Verifying that the body of " + this.toString() + " :Contains: " + match);
        LOG.info(" :Exact Number of matches needed is: " + desiredMatches);
        LOG.info(" :Extra matching string is: " + extraMatch);

        if (match == null) {
            throw new IllegalArgumentException("Unable to verify the body of " + this.toString() + " because the search text is null.");
        }
        if (this.responseBody == null) {
            throw new WebBrowserException("The body of " + this.toString() + " is null.");
        }

        int fromIndex = 0;
        int foundMatches = 0;
        while (true) {
            fromIndex = this.responseBody.indexOf(match, fromIndex);
            if (fromIndex >= 0) {
                foundMatches++;
                fromIndex++;
            } else {
                break;
            }
        }

        // boolean foundText = this.responseBody.indexOf(text) >= 0;
        if (foundMatches != desiredMatches) {
            throw new WebBrowserException("The body of " + this.toString() +
                                          " for search text of: " + match +
                                          " number of matches found was: " + foundMatches +
                                          " but desired number to find was: " + desiredMatches +
                                          ". The full text of the response was: " +
                                          System.getProperty("line.separator") + responseBody);
        }

        if (extraMatch != null) {
            boolean foundExtraText = this.responseBody.indexOf(extraMatch) >= 0;

            if (!foundExtraText) {
                throw new WebBrowserException("The body of " + this.toString() + " does not contain: " + extraMatch);
            }
        }

        LOG.info("Yep!");
        return this.responseBody;
    }

    /**
     * Verifies that the response code matches the specified response code
     *
     * @param expectedResponseCode
     *            the value that should match the response code
     * @return the response code
     * @throws WebBrowserException
     *             if the response code does not match the expected code
     */
    public int verifyResponseCodeEquals(int expectedResponseCode) throws WebBrowserException {
        LOG.info("Verifying that the response code of " + this.toString() + " is: " + expectedResponseCode);
        if (this.responseCode != expectedResponseCode) {
            throw new WebBrowserException("The response code of " + this.toString() + " is " + this.responseCode + "; expected " + expectedResponseCode);
        }
        LOG.info("Yep!");
        return this.responseCode;
    }

    /**
     * Verifies that the URL matches the specified URL
     *
     * @param expectedUrl
     *            the value that should match the URL
     * @return the URL
     * @throws WebBrowserException
     *             if the URL does not match the expected URL
     */
    public String verifyUrlEquals(String expectedUrl) throws WebBrowserException {
        LOG.info("Verifying that the URL of " + this.toString() + " is: " + expectedUrl);
        boolean notEqual = (this.url == null) ? (expectedUrl != null) : !this.url.equals(expectedUrl);
        if (notEqual) {
            throw new WebBrowserException("The URL of " + this.toString() + " is " + this.url + "; expected " + expectedUrl);
        }
        LOG.info("Yep!");
        return this.url;
    }

    /**
     * Verifies that a response header is formatted correctly.
     *
     * @param key
     *            The key of the response header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param exists
     *            true indicates that the specified response header should
     *            exist; false indicates that the specified response header
     *            should <b>not</b> exist
     * @throws WebBrowserException
     *             if the response header is not formatted correctly
     * @return the value of the response header that was checked
     */
    public List<String> verifyResponseHeaderExists(String key, boolean ignoreKeyCase, boolean exists) throws WebBrowserException {
        return this.verifyKeyExists(this.responseHeaders, "Response Header", key, ignoreKeyCase, exists);
    }

    /**
     * Verifies that a response header is formatted correctly.
     *
     * @param key
     *            The key of the response header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) match the specified
     *            response header. null indicates that the header should not
     *            exist.
     * @param equals
     *            true indicates that the specified response header should match
     *            the input value; false indicates that the specified response
     *            header should <b>not</b> match the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @throws WebBrowserException
     *             if the response header is not formatted correctly
     * @return the value of the response header that was checked
     */
    public String verifyResponseHeaderEquals(String key, boolean ignoreKeyCase, String value, boolean equals, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyEquals(this.responseHeaders, "Response Header", key, ignoreKeyCase, value, equals, ignoreValueCase);
    }

    /**
     * Verifies that a response header is formatted correctly.
     *
     * @param key
     *            The key of the response header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) be contained in the
     *            specified response header. null indicates that the header
     *            should not exist.
     * @param contains
     *            true indicates that the specified response header should
     *            contain the input value; false indicates that the specified
     *            response header should <b>not</b> contain the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @return the value of the response header that was checked
     * @throws WebBrowserException
     *             if the response header is not formatted correctly
     */
    public String verifyResponseHeaderContains(String key, boolean ignoreKeyCase, String value, boolean contains, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyContains(this.responseHeaders, "Response Header", key, ignoreKeyCase, value, contains, ignoreValueCase);
    }

    /**
     * Verifies that a request header is formatted correctly.
     *
     * @param key
     *            The key of the request header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param exists
     *            true indicates that the specified request header should
     *            exist; false indicates that the specified request header
     *            should <b>not</b> exist
     * @return the value of the request header that was checked
     * @throws WebBrowserException
     *             if the request header is not formatted correctly
     */
    public List<String> verifyRequestHeaderExists(String key, boolean ignoreKeyCase, boolean exists) throws WebBrowserException {
        return this.verifyKeyExists(this.requestHeaders, "Request Header", key, ignoreKeyCase, exists);
    }

    /**
     * Verifies that a request header is formatted correctly.
     *
     * @param key
     *            The key of the request header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) be contained in the
     *            specified request header. null indicates that the header
     *            should not exist.
     * @param contains
     *            true indicates that the specified request header should
     *            contain the input value; false indicates that the specified
     *            request header should <b>not</b> contain the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @return the value of the request header that was checked
     * @throws WebBrowserException
     *             if the request header is not formatted correctly
     */
    public String verifyRequestHeaderContains(String key, boolean ignoreKeyCase, String value, boolean contains, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyContains(this.requestHeaders, "Request Header", key, ignoreKeyCase, value, contains, ignoreValueCase);
    }

    /**
     * Verifies that a request header is formatted correctly.
     *
     * @param key
     *            The key of the request header to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) match the specified
     *            request header. null indicates that the header should not
     *            exist.
     * @param equals
     *            true indicates that the specified request header should match
     *            the input value; false indicates that the specified request
     *            header should <b>not</b> match the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @return the value of the request header that was checked
     * @throws WebBrowserException
     *             if the request header is not formatted correctly
     */
    public String verifyRequestHeaderEquals(String key, boolean ignoreKeyCase, String value, boolean equals, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyEquals(this.requestHeaders, "Request Header", key, ignoreKeyCase, value, equals, ignoreValueCase);
    }

    /**
     * Verifies that a cookie is formatted correctly.
     *
     * @param key
     *            The key of the cookie to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param exists
     *            true indicates that the specified cookie should exist; false
     *            indicates that the specified cookie should <b>not</b> exist
     * @return the value of the cookie that was checked
     * @throws WebBrowserException
     *             if the cookie is not formatted correctly
     */
    public List<String> verifyCookieExists(String key, boolean ignoreKeyCase, boolean exists) throws WebBrowserException {
        return this.verifyKeyExists(this.cookies, "Cookie", key, ignoreKeyCase, exists);
    }

    /**
     * Verifies that a cookie is formatted correctly.
     *
     * @param key
     *            The key of the cookie to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) be contained in the
     *            specified cookie. null indicates that the cookie should not
     *            exist.
     * @param contains
     *            true indicates that the specified cookie should contain the
     *            input value; false indicates that the specified cookie should
     *            <b>not</b> contain the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @return the value of the cookie that was checked
     * @throws WebBrowserException
     *             if the cookie is not formatted correctly
     */
    public String verifyCookieContains(String key, boolean ignoreKeyCase, String value, boolean contains, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyContains(this.cookies, "Cookie", key, ignoreKeyCase, value, contains, ignoreValueCase);
    }

    /**
     * Verifies that a cookie is formatted correctly.
     *
     * @param key
     *            The key of the cookie to check
     * @param ignoreKeyCase
     *            Ignore the case of the specified key (true indicates that
     *            capital letters don't matter)
     * @param value
     *            The value that should (or should not) match the specified
     *            cookie. null indicates that the cookie should not exist.
     * @param equals
     *            true indicates that the specified cookie should match the
     *            input value; false indicates that the specified cookie should
     *            <b>not</b> match the input value
     * @param ignoreValueCase
     *            Ignore the case of the specified value (true indicates that
     *            capital letters don't matter)
     * @return the value of the cookie that was checked
     * @throws WebBrowserException
     *             if the cookie is not formatted correctly
     */
    public String verifyCookieEquals(String key, boolean ignoreKeyCase, String value, boolean equals, boolean ignoreValueCase) throws WebBrowserException {
        return this.verifyKeyEquals(this.cookies, "Cookie", key, ignoreKeyCase, value, equals, ignoreValueCase);
    }

    protected String getKeyName(String type, String key, boolean ignoreKeyCase) {
        StringBuffer keyNameBuffer = new StringBuffer();
        keyNameBuffer.append(type);
        keyNameBuffer.append(" with key=");
        if (key == null) {
            keyNameBuffer.append("(null)");
        } else {
            keyNameBuffer.append("\"");
            keyNameBuffer.append(key);
            keyNameBuffer.append("\"");
            if (ignoreKeyCase) {
                keyNameBuffer.append(" (ignoring case)");
            }
        }
        return keyNameBuffer.toString();
    }

    protected String getValueDescription(String value, boolean ignoreValueCase) {
        StringBuffer description = new StringBuffer();
        if (value == null) {
            description.append("(null)");
        } else {
            description.append("\"");
            description.append(value);
            description.append("\"");
            if (ignoreValueCase) {
                description.append(" (ignoring case)");
            }
        }
        return description.toString();
    }

    protected List<String> verifyKeyExists(List<MyNameValuePair> map, String type, String key, boolean ignoreKeyCase, boolean exists) throws WebBrowserException {
        String methodName = "verifyKeyExists";
        List<String> actualValue = this.getValue(map, key, ignoreKeyCase);
        String keyName = this.getKeyName(type, key, ignoreKeyCase);
        if (exists) {
            LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that " + this.toString() + " has a " + keyName);
            if (actualValue == null) {
                throw new WebBrowserException(this.toString() + " should have a " + keyName);
            }
            LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
        } else {
            LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that " + this.toString() + " does not have a " + keyName);
            if (actualValue == null) {
                LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
            } else {
                throw new WebBrowserException(this.toString() + " should not have a " + keyName);
            }
        }
        return actualValue;
    }

    protected String verifyKeyContains(List<MyNameValuePair> map, String type, String key, boolean ignoreKeyCase, String value, boolean contains, boolean ignoreValueCase) throws WebBrowserException {
        List<String> actualValues = this.verifyKeyExists(map, type, key, ignoreKeyCase, (value != null));
        //List<String> actualValues = this.getValues(map, type, key, ignoreKeyCase, (value!=null));
        String keyName = this.getKeyName(type, key, ignoreKeyCase);
        boolean passed = false;
        String errorText = "";
        String resultActualValue = null;
        if (actualValues != null) {
            for (String actualValue : actualValues) {
                if (actualValue != null) { // if actualValue==null when it shouldn't, this.verifyKeyExists(...) will throw an appropriate exception
                    String methodName = "verifyKeyContains";
                    String actualValueUpper = ignoreValueCase ? actualValue.toUpperCase() : actualValue;
                    String valueUpper = (ignoreValueCase && value != null) ? value.toUpperCase() : value;
                    boolean isContained = actualValueUpper.contains(valueUpper);
                    String valueDescription = this.getValueDescription(value, ignoreValueCase);
                    if (contains) {
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that the " + keyName + " of " + this.toString() + " contains the text " + valueDescription
                                                                     + " ...");
                        if (isContained) {
                            LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
                            resultActualValue = actualValue;
                            passed = true;
                        } else {
                            errorText = this.toString() + " has a " + keyName + ", but the value should contain the text " + valueDescription + ".  Value=" + actualValue;
                        }
                    } else {
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that the " + keyName + " of " + this.toString() + " does not contain the text "
                                                                     + valueDescription + " ...");
                        if (isContained) {
                            errorText = this.toString() + " has a " + keyName + ", but the value should not contain the text " + valueDescription + ".  Value=" + actualValue;
                        } else {
                            resultActualValue = actualValue;
                            passed = true;
                        }
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
                    }
                }
            }
        } else { //actualValues was null.  pass if the value is null
            if (value == null) {
                passed = true;
            }
        }
        if (!passed) {
            throw new WebBrowserException(errorText);
        } else {
            return resultActualValue;
        }
    }

    protected String verifyKeyEquals(List<MyNameValuePair> map, String type, String key, boolean ignoreKeyCase, String value, boolean equals, boolean ignoreValueCase) throws WebBrowserException {
        List<String> actualValues = this.verifyKeyExists(map, type, key, ignoreKeyCase, (value != null));
        String keyName = this.getKeyName(type, key, ignoreKeyCase);
        boolean passed = false;
        String errorText = "";
        String resultActualValue = null;
        if (actualValues != null) {
            for (String actualValue : actualValues) {
                if (actualValue != null) { // if actualValue==null when it shouldn't, this.verifyKeyExists(...) will throw an appropriate exception
                    String methodName = "verifyKeyEquals";
                    boolean equal = ignoreValueCase ? actualValue.equalsIgnoreCase(value) : actualValue.equals(value);
                    String valueDescription = this.getValueDescription(value, ignoreValueCase);
                    if (equals) {
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that the " + keyName + " of " + this.toString() + " equals " + valueDescription + " ...");
                        if (equal) {
                            LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
                            passed = true;
                            resultActualValue = actualValue;
                        } else {
                            errorText = this.toString() + " has a " + keyName + ", but the value should equal " + valueDescription + ".  Value=" + actualValue;
                        }
                    } else {
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Verifying that the " + keyName + " of " + this.toString() + " does not equal " + valueDescription + " ...");
                        if (equal) {
                            errorText = this.toString() + " has a " + keyName + ", but the value should not equal " + valueDescription + ".  Value=" + actualValue;
                        } else {
                            passed = true;
                            resultActualValue = actualValue;
                        }
                        LOG.logp(Level.INFO, CLASS_NAME, methodName, "Yep!");
                    }
                }
            }
        } else {//actualValues was null.  pass if the value is null
            if (value == null) {
                passed = true;
            }
        }
        if (!passed) {
            throw new WebBrowserException(errorText);
        } else {
            return resultActualValue;
        }
    }

    /**
     * Convenience class for HTML anchor elements; provides assistance with
     * finding a specific anchor element and clicking on it.
     *
     * @author Tim Burns
     *
     */
    static class WebLinkFinder {

        final WebResponse response;
        final String identifier;
        final String description;

        /**
         * Primary constructor
         *
         * @param response
         *            the WebResponse where this hyperlink is located. Must not
         *            be null.
         * @param identifier
         *            information that uniquely identifies this hyperlink on the
         *            page
         * @param description
         *            a human-readable description of the information that
         *            uniquely identifies this hyperlink
         * @throws IllegalArgumentException
         *             if the specified WebResponse is null
         */
        WebLinkFinder(WebResponse response, String identifier, String description) throws IllegalArgumentException {
            if (response == null) {
                throw new IllegalArgumentException("Unable to construct a " + this.getClass().getName() + " instance because the specified input argument is null: "
                                                   + WebResponse.class.getName());
            }
            this.response = response;
            this.identifier = (identifier == null) ? new String() : identifier;
            this.description = description + "=\"" + identifier + "\"";
        }

        /**
         * Locates this hyperlink inside the encapsulated WebResponse. Default
         * implementation iterates over all anchor elements found in the
         * WebResponse, and calls this.matches(element) to determine if the
         * current anchor is the desired anchor.
         *
         * @return an anchor element representing this hyperlink
         * @throws WebBrowserException if there is a problem
         *             detecting the requested hyperlink
         */
        WebLink find() throws WebBrowserException {
            List<WebLink> links = this.response.getLinks();
            for (WebLink link : links) {
                if (this.matches(link)) {
                    return link;
                }
            }
            return null;
        }

        /**
         * Determines if the specified anchor element matches the desired anchor
         * element. The default implementation always returns true.
         *
         * @param link
         *            the WebLink to check
         * @return true if the specified anchor matches the desired anchor
         */
        boolean matches(WebLink link) {
            return true;
        }

        /**
         * Locates the this hyperlink on the encapsulated WebResponse and makes
         * an HTTP request for the resource defined by that hyperlink
         *
         * @return the response from the server from clicking on this hyperlink
         * @throws IllegalArgumentException
         *             if the specified link cannot be found
         * @throws IllegalStateException
         *             if any problem occurs while clicking the link
         */
        WebResponse click() throws IllegalArgumentException, WebBrowserException {
            try {
                WebLink link = this.find();
                if (link == null) {
                    throw new IllegalArgumentException(this.response.toString() + " does not contain an anchor element with " + this.description);
                }
                return link.click();
            } catch (Exception e) {
                throw new WebBrowserException(this.response.toString() + " failed to click on the link with " + this.description, e);
            }
        }

        static class FindLinkByText extends WebLinkFinder {

            FindLinkByText(WebResponse response, String text) throws IllegalArgumentException {
                super(response, text, "text content");
            }

            @Override
            boolean matches(WebLink link) {
                return this.identifier.equals(link.getText());
            }

        }

        static class FindLinkByName extends WebLinkFinder {

            FindLinkByName(WebResponse response, String name) throws IllegalArgumentException {
                super(response, name, "name");
            }

            @Override
            boolean matches(WebLink link) {
                return this.identifier.equals(link.getName());
            }

        }

        static class FindLinkById extends WebLinkFinder {

            FindLinkById(WebResponse response, String id) throws IllegalArgumentException {
                super(response, id, "ID");
            }

            @Override
            boolean matches(WebLink link) {
                return this.identifier.equals(link.getId());
            }

        }

        static class FindLinkByIndex extends WebLinkFinder {

            FindLinkByIndex(WebResponse response, int index) throws IllegalArgumentException {
                super(response, Integer.toString(index), "index");
            }

            @Override
            WebLink find() throws WebBrowserException {
                int index = Integer.parseInt(this.identifier);
                List<WebLink> links = this.response.getLinks();
                int size = links.size();
                if (size <= index) {
                    throw new IllegalArgumentException(this.toString() + " failed to locate an anchor element with " + this.description + "; total anchor elements found on page: "
                                                       + size);
                }
                return links.get(index);
            }

        }

    }

}