/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.logging.ffdc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 */
public class IgnoredFFDCs {

    public static class IgnoredFFDC {
        public final String exception;
        public final String message;
        final String stack;

        IgnoredFFDC(String exception, String message, String stack) {
            this.exception = exception;
            this.message = message;
            this.stack = stack;
        }

        public boolean ignore(String ffdcHeader) {
            return (message == null ? true : (ffdcHeader == null ? false : ffdcHeader.contains(message)));
        }

        @Override
        public String toString() {
            return "[exception=" + exception + ", message=" + message + ", stack=" + stack + "]";
        }
    }

    private static class TracingDefHandler extends DefaultHandler {
        private static class ParseException extends IllegalArgumentException {
            private static final long serialVersionUID = 20111018165653L;

            /* -------------------------------------------------------------------------- */
            /*
             * ParseException constructor
             * /* --------------------------------------------------------------------------
             */
            /**
             * Construct a new ParseException.
             * 
             * @param text The text of the exception
             * @param locator Where were we in the parse?
             */
            public ParseException(final String text, final Locator locator) {
                super(text + " (Line: " + locator.getLineNumber() + ", Column: " + locator.getColumnNumber() + ")");
            }
        }

        /** The plain text being parsed */
        private String _text;
        /** Where are we in the parse? */
        private Locator _locator;
        /** Which exception to ignore? */
        private String _exception;
        /** What message does the exception contain? */
        private String _message;
        /** A subset of the exception stack */
        private String _stack;

        /**
         * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
         * @param locator The current location in the parse
         */
        @Override
        public void setDocumentLocator(final Locator locator) {
            _locator = locator;
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         * @param text The characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the character array.
         */

        @Override
        public void characters(char[] text, int start, int length) throws SAXException {
            String textString = new String(text, start, length);
            setText(textString);
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         * @param uri The name space of the tag (ignored)
         * @param localName The name of the tag
         * @param qName The qName of the tag (ignored)
         * @param attribs The attributes of the tag
         */
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
            if (localName.equals("IgnoredFFDC")) {
                ; // Placeholder to allow the top level element to be present
            }

            else if (localName.equals("FFDC")) {
                ; // Placeholder to allow the FFDC element to be present
            }

            else if (localName.equals("Exception")) {
                ; // Placeholder to allow the Exception element to be present
            }

            else if (localName.equals("Message")) {
                ; // Placeholder to allow the Message element to be present
            }

            else if (localName.equals("Stack")) {
                ; // Placeholder to allow the Message element to be present
            } else {
                throw new ParseException("Unknown element " + localName, _locator);
            }
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         * @param uri The namespace of the end tag (ignored)
         * @param localName The name of the ending tag
         * @param qName The qName of the end tag (ignored)
         */
        @Override
        public void endElement(final String uri, final String localName, final String qName) {
            if (localName.equals("IgnoredFFDC")) {
                ; // Placeholder to allow the bottom level element to be present    
            } else if (localName.equals("FFDC")) {
                FFDCs.add(new IgnoredFFDC(_exception, _message, _stack));
                _exception = null;
                _message = null;
                _stack = null;
            } else if (localName.equals("Exception")) {
                _exception = _text;
            } else if (localName.equals("Message")) {
                _message = _text;
            } else if (localName.equals("Stack")) {
                _stack = _text;
            } else {
                throw new ParseException("Unknown element " + localName, _locator);
            }
        }

        /**
         * Set the string to e passed to _exception or _message
         * 
         * @param text The plain text that has been parsed
         */
        public void setText(String text) {
            this._text = text;
        }
    }

    /** The Exception and corresponding message to be ignored */
    public static Collection<IgnoredFFDC> FFDCs = new ArrayList<IgnoredFFDC>();

    static {
        try {
            String fileName = System.getProperty("fileFFDC", "");
            File file = new File(fileName);
            if (file != null && file.exists()) {
                InputStream is = new FileInputStream(file);
                if (is != null) {
                    InputSource src = new InputSource(is);
                    try {
                        XMLReader rdr = XMLReaderFactory.createXMLReader();
                        rdr.setContentHandler(new TracingDefHandler());
                        rdr.parse(src);
                    } catch (SAXException e) {
                        throw new IllegalStateException("SAXException: ", e);
                    } catch (IOException e) {
                        throw new IllegalStateException("IOExcetion: ", e);
                    }
                }
            }
        } catch (Exception re) {
            System.out.println("--- Runtime Exception while trying to initialise ---");
            re.printStackTrace();
            System.out.println("----------------------------------------------------");
            try {
                throw re;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Error e) {
            System.out.println("--- Error while trying to initialise ---");
            e.printStackTrace();
            System.out.println("----------------------------------------");
            throw e;
        }
    }
}