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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

/**
 * Convenience class for parsing HTML that may not be well formatted. Since
 * JTidy is required for HttpUnit, we might as well use it
 * 
 * @author Tim Burns
 */
public class HtmlParser {

    private static Logger LOG = Logger.getLogger(HtmlParser.class.getName());

    protected static HtmlParser INSTANCE;

    /**
     * If you only intend to parse one HTML document at a time, you only need
     * one instance. This method allows you to access a global instance.
     * 
     * @return a global instance for this class
     */
    public static HtmlParser getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HtmlParser();
        }
        return INSTANCE;
    }

    protected Tidy tidy;
    protected ByteArrayOutputStream errorStream;
    protected PrintWriter errorWriter;

    /**
     * Initializes a new parser and configures default settings
     */
    public HtmlParser() {
        this.tidy = new Tidy(); // NOTE: makes all HTML tags and attributes lower case by default
        this.tidy.setShowWarnings(false);
        this.errorStream = new ByteArrayOutputStream();
        this.errorWriter = new PrintWriter(this.errorStream);
        this.tidy.setErrout(this.errorWriter);
        this.tidy.setQuiet(true);
        this.tidy.setTidyMark(false);
        this.tidy.setSmartIndent(true);
    }

    /**
     * Parses a input String as HTML. The HTML does not need to be well
     * formatted.
     * 
     * @param html
     *            the document you want to parse. Must not be null.
     * @return a parsed version of the document
     * @throws Throwable
     *             If the document can't be parsed. throws an NPE if the input
     *             String is null
     */
    public synchronized Document parse(String html) throws Throwable {
        ByteArrayOutputStream parsedHtmlStream = null;
        if (LOG.isLoggable(Level.FINE)) {
            parsedHtmlStream = new ByteArrayOutputStream();
            LOG.fine("Parsing HTML using JTidy ...");
        }
        Document document = this.tidy.parseDOM(new ByteArrayInputStream(html.getBytes()), parsedHtmlStream);
        this.errorWriter.flush();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("~~~ Beginning of parsed HTML document ~~~");
            LOG.fine(parsedHtmlStream.toString());
            LOG.fine("~~~ End of parsed HTML document ~~~~~~~~~");
            int numParseErrors = this.tidy.getParseErrors();
            if (numParseErrors > 0) {
                LOG.fine("JTidy detected " + numParseErrors + " parser errors");
                LOG.fine("~~~ Beginning of parser errors ~~~");
                LOG.fine(this.errorStream.toString());
                LOG.fine("~~~ End of parser errors ~~~~~~~~~");
            }
        }
        this.errorStream.reset();
        return document;
    }

}
