/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.io.File;
import java.io.FileInputStream;
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

import com.ibm.websphere.simplicity.log.Log;

/**
 * Table of ignored FFDCs.  The table is read from the file named
 * by system property {@link #FILE_FFDC_PROPERTY_NAME}.  No values
 * are read if the property is unset, or if the named file does not
 * exist.
 * 
 * Ignored FFDC's are read during class initialization a static collection.
 * The ignored FFDC values are in addition to values supplied by
 * {@link componenttest.annotation.AllowedFFDC} and
 * @link componenttest.annotation.ExpectedFFDC} annotations.
 * 
 * {@link IgnoredFFDC} contain exception, message, and stack values.
 * The stack value is unused.  The exception value is used to match
 * actual FFDCs against the potential ignorable FFDCs.  The ignorable
 * FFDC message is used to check if an actual FFDC is to be ignored.
 * An actual FFDC matches an ignorable FFDC if the ignorable FFDC has a
 * null message, or if the FFDC header of the actual FFDC contains the
 * ignorable FFDC message.
 * 
 * Always matching when the ignorable message is null means that
 * specifying an ignorable FFDC with just an exception means that
 * all FFDC with that exception will be ignored, regardless of their
 * messages.
 *
 * See {@link FATRunner#methodBlock()}.
 */
//
// <IgnoredFFDC>
//   <FFDC>
//      <Exception>exception</Exception>
//      <Message>message</Message>
//      <Stack>stack</Stack>
//   </FFDC>
//   ...
// </IgnoredFFDC>
//
public class IgnoredFFDCs {
    private static final Class<? extends IgnoredFFDCs> c = IgnoredFFDCs.class;

    public static final Collection<IgnoredFFDC> FFDCs = new ArrayList<IgnoredFFDC>();
    
    public static class IgnoredFFDC {
        public final String exception; // used
        public final String message; // used; always match when null
        public final String stack; // ignored

        IgnoredFFDC(String exception, String message, String stack) {
            String methodName = "<init>";
            
            if ( message == null ) {
                Log.info(c, methodName,
                    "Ignore all FFDC with exception " + exception);
                if ( stack != null ) {
                    Log.info(c, methodName,
                        "Unused ignore FFDC stack " + stack + " for exception " + exception);
                }
            } else {
                Log.info(c, methodName,
                    "Ignore FFDC with exception " + exception + " and message + " + message);
                if ( stack != null ) {
                    Log.info(c, methodName,
                        "Unused ignore FFDC stack " + stack + " for exception " + exception + " and message " + message);
                }
            }
            
            this.exception = exception;
            this.message = message;
            this.stack = stack;
        }

        /**
         * Tell if an FFDC should be ignored, using the specified
         * values and the header of the actual FFDC.
         * 
         * Currently, only the message is checked: The exception and stack
         * are ignored.  However, the exception was used earlier to select
         * which ignorable FFDCs are to be checked against the FFDC header.
         *
         * The FFDC is ignored if the message is null, or if the header
         * contains the message.
         * 
         * @param ffdcHeader The header of the FFDC which is to be tested.
         *
         * @return True or false telling if the FFDC is to be ignored.
         */
        public boolean ignore(String ffdcHeader) {
            if ( message == null ) {
                return true;
            } else if ( ffdcHeader == null ) {
                return false;
            } else {
                return ffdcHeader.contains(message);
            }
        }

        @Override
        public String toString() {
            return "[exception=" + exception + ", message=" + message + ", stack=" + stack + "]";
        }
    }

    public static final String FILE_FFDC_PROPERTY_NAME = "fileFFDC";
    public static final String FILE_FFDC;

    static {
        String methodName = "<cinit>";

        FILE_FFDC = System.getProperty(FILE_FFDC_PROPERTY_NAME);
        if ( FILE_FFDC == null ) {
            Log.info(c, methodName,
                "File FFDC property (" + FILE_FFDC_PROPERTY_NAME + "): null");

        } else {        
            try {
                File file = new File(FILE_FFDC);
                if ( !file.exists() ) {
                    Log.info(c, methodName,
                        "File FFDC property (" + FILE_FFDC_PROPERTY_NAME + "): " + FILE_FFDC + " Does not exist");

                } else {
                    Log.info(c, methodName,
                        "File FFDC property (" + FILE_FFDC_PROPERTY_NAME + "): " + FILE_FFDC);

                    try ( InputStream is = new FileInputStream(file) ) { // throws IOException
                        InputSource src = new InputSource(is);
                        XMLReader rdr = XMLReaderFactory.createXMLReader(); // throws SAXException
                        rdr.setContentHandler( new FFDCHandler() );
                        rdr.parse(src); // throws IOException, SAXException

                    } catch ( Exception e ) {
                        System.out.println("--- Runtime Exception parsing " + FILE_FFDC + " ---");
                        e.printStackTrace();
                        System.out.println("----------------------------------------------------");
                        e.printStackTrace();
                    }
                    
                }

            } catch ( Error e ) {
                System.out.println("--- Error parsing " + FILE_FFDC + " ---");
                e.printStackTrace();
                System.out.println("----------------------------------------");
                throw e;
            }
        }
    }    
    
    private static class FFDCHandler extends DefaultHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        private String text;
        private String exception;
        private String message;
        private String stack;

        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            text = new String(chars, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ( localName.equals("IgnoredFFDC") ) {
                // Nothing to do
            } else if ( localName.equals("FFDC") ) {
                // Nothing to do
            } else if ( localName.equals("Exception") ) {
                // Nothing to do
            } else if ( localName.equals("Message") ) {
                // Nothing to do
            } else if ( localName.equals("Stack") ) {
                // Nothing to do
            } else {
                throw newParseException("Unknown element " + localName, locator);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ( localName.equals("IgnoredFFDC") ) {
                // nothing to do
            } else if ( localName.equals("FFDC") ) {
                FFDCs.add( new IgnoredFFDC(exception, message, stack) );

                exception = null;
                message = null;
                stack = null;
            } else if ( localName.equals("Exception") ) {
                exception = text;
                text = null;
            } else if ( localName.equals("Message") ) {
                message = text;
                text = null;
            } else if ( localName.equals("Stack") ) {
                stack = text;
                text = null;
            } else {
                text = null;
                throw newParseException("Unknown element " + localName, locator);
            }
        }
    }

    private static SAXException newParseException(String text, Locator locator) {
        return new SAXException( text + " (File: " + FILE_FFDC + ", Line: " + locator.getLineNumber() + ", Column: " + locator.getColumnNumber() + ")");
    }
}