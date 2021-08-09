package com.ibm.json.xml.internal;

/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.logging.*;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Properties;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Writer;


/**
 * This class is lightweight representation of an XML tag as a JSON object.
 * TODO:  Look at using HashMap and collections to store the data instead of sync'ed objects.
 * TODO:  See if the indent/newline handling could be cleaned up.  the repeated checks for compact is rather ugly.
 * TODO:  Look at maybe using the Java representation object as store for the XML data intsead of this customized object.
 */
public class JSONObject
{
    /**
     * Logger.
     */
    private static String  className              = "com.ibm.json.xml.internal.JSONObject";
    private static Logger logger                  = Logger.getLogger(className,null);
    private static final String indent            = "   ";

    /**
     * The JSON object name.  Effectively, the XML tag name.
     */
    private String objectName     = null;

    /**
     * All basic JSON object properties.  Effectively same as XML tag attributes.
     */
    private Properties attrs      = null;

    /**
     * All children JSON objects referenced.  Effectively the child tags of an XML tag.
     */
    private Hashtable jsonObjects = null;

    /**
     * Any XML freeform text to associate with the JSON object,
     */
    private String tagText        = null;
    
    /**
     * System property to set to "true" if you want non-ASCII characters unescaped - escape is default
     */
    private static final String ESCAPE_ENV_VAR = "JSON4J_XML_RELAXED_ESCAPE_MODE";
    
    /**
     * Flag to determine whether non-ASCII (127 and above) characters should be escaped
     */
    private boolean escapeNonAsciiChars = true;

    /**
     * Constructor.
     * @param objectName The object (tag) name being constructed.
     * @param attrs A proprerties object of all the attributes present for the tag.
     */
    public JSONObject(String objectName, Properties attrs)
    {
        this.objectName  = objectName;
        this.attrs       = attrs;
        this.jsonObjects = new Hashtable();
        
        
        //Workaround for mashup center.  Temporary until permanent solution can be found.
        String escapeStr = System.getProperty(ESCAPE_ENV_VAR); 
        if (escapeStr != null)
        {
          if (escapeStr.equalsIgnoreCase("true")) 
          {
            escapeNonAsciiChars = false;
          }   
        }
    }

    /**
     * Method to add a JSON child object to this JSON object.
     * @param obj The child JSON object to add to this JSON object.
     */
    public void addJSONObject(JSONObject obj)
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "addJSONObject(JSONObject)");

        Vector vect = (Vector) this.jsonObjects.get(obj.objectName);
        if (vect != null)
        {
            vect.add(obj);
        }
        else
        {
            vect = new Vector();
            vect.add(obj);
            this.jsonObjects.put(obj.objectName, vect);
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "addJSONObject(JSONObject)");
    }

    /**
     * Method to set any freeform text on the object.
     * @param str The freeform text to assign to the JSONObject
     */
    public void setTagText(String str)
    {
        this.tagText = str;
    }

    /**
     * Method to get any freeform text on the object.
     */
    public String getTagText()
    {
        return this.tagText;
    }

    /**
     * Method to write out the JSON formatted object.  Same as calling writeObject(writer,indentDepth,contentOnly,false);
     * @param writer The writer to use when serializing the JSON structure.
     * @param indentDepth How far to indent the text for object's JSON format.
     * @param contentOnly Flag to debnnote whether to assign this as an attribute name, or as a nameless object.  Commonly used for serializing an array.  The Array itself has the name,   The contents are all nameless objects 
     * @throws IOException Trhown if an error occurs on write.
     */
    public void writeObject(Writer writer, int indentDepth, boolean contentOnly)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeObject(Writer, int, boolean)");
        writeObject(writer,indentDepth,contentOnly, false);
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeObject(Writer, int, boolean)");
    }


    /**
     * Method to write out the JSON formatted object.
     * @param writer The writer to use when serializing the JSON structure.
     * @param indentDepth How far to indent the text for object's JSON format.
     * @param contentOnly Flag to debnnote whether to assign this as an attribute name, or as a nameless object.  Commonly used for serializing an array.  The Array itself has the name,   The contents are all nameless objects 
     * @param compact Flag to denote to write the JSON in compact form.  No indentions or newlines.  Setting this value to true will cause indentDepth to be ignored.
     * @throws IOException Trhown if an error occurs on write.
     */
    public void writeObject(Writer writer, int indentDepth, boolean contentOnly, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeObject(Writer, int, boolean, boolean)");

        if (writer != null)
        {
            try
            {
                if (isEmptyObject())
                {
                    writeEmptyObject(writer,indentDepth,contentOnly, compact);
                }
                else if (isTextOnlyObject())
                {
                    writeTextOnlyObject(writer,indentDepth,contentOnly, compact);  
                }
                else
                {
                    writeComplexObject(writer,indentDepth,contentOnly,compact);
                }

            }
            catch (Exception ex)
            {
                IOException iox = new IOException("Error occurred on serialization of JSON text.");
                iox.initCause(ex);
                throw iox;
            }
        }
        else
        {
            throw new IOException("The writer cannot be null.");
        }

        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeObject(Writer, int, boolean, boolean)");
    }

    /**
     * Internal method to write out a proper JSON attribute string.
     * @param writer The writer to use while serializing
     * @param name The attribute name to use.
     * @param value The value to assign to the attribute.
     * @param depth How far to indent the JSON text.
     * @param compact Flag to denote whether or not to use pretty indention, or compact format, when writing.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeAttribute(Writer writer, String name, String value, int depth, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeAttribute(Writer, String, String, int)");

        if (!compact)
        {
            writeIndention(writer, depth);
        }

        try
        {
            if (!compact)
            {
                writer.write("\"" + name + "\"" + " : " + "\"" + escapeStringSpecialCharacters(value) + "\"");
            }
            else
            {
                writer.write("\"" + name + "\"" + ":" + "\"" + escapeStringSpecialCharacters(value) + "\"");
            }
        }
        catch (Exception ex)
        {
            IOException iox = new IOException("Error occurred on serialization of JSON text.");
            iox.initCause(ex);
            throw iox;
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeAttribute(Writer, String, String, int)");
    }

    /**
     * Internal method for doing a simple indention write.
     * @param writer The writer to use while writing the JSON text.
     * @param indentDepth How deep to indent the text.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeIndention(Writer writer, int indentDepth)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeIndention(Writer, int)");

        try
        {
            for (int i = 0; i < indentDepth; i++) 
            {
                writer.write(indent);
            }
        }
        catch (Exception ex)
        {
            IOException iox = new IOException("Error occurred on serialization of JSON text.");
            iox.initCause(ex);
            throw iox;
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeIndention(Writer, int)");
    }

    /**
     * Internal method to write out a proper JSON attribute string.
     * @param writer The writer to use while serializing
     * @param attrs The attributes in a properties object to write out
     * @param depth How far to indent the JSON text.
     * @param compact Whether or not to use pretty indention output, or compact output, format
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeAttributes(Writer writer, Properties attrs, int depth, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeAttributes(Writer, Properties, int, boolean)");

        if (attrs != null)
        {
            Enumeration props = attrs.propertyNames();

            if (props != null && props.hasMoreElements())
            {
                while (props.hasMoreElements())
                {
                    String prop = (String)props.nextElement();
                    writeAttribute(writer, escapeAttributeNameSpecialCharacters(prop), (String)attrs.get(prop), depth + 1, compact);
                    if (props.hasMoreElements())
                    {
                        try
                        {
                            if (!compact)
                            {
                                writer.write(",\n");
                            }
                            else
                            {
                                writer.write(",");
                            }
                        }
                        catch (Exception ex)
                        {
                            IOException iox = new IOException("Error occurred on serialization of JSON text.");
                            iox.initCause(ex);
                            throw iox;
                        }
                    }
                }
            }
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeAttributes(Writer, Properties, int, boolean)");
    }

    /**
     * Internal method to escape special attribute name characters, to handle things like name spaces.
     * @param str The string to escape the characters in.
     */
    private String escapeAttributeNameSpecialCharacters(String str)
    {
        if (str != null)
        {
            StringBuffer strBuf = new StringBuffer("");

            for (int i = 0; i < str.length(); i++)
            {
                char strChar = str.charAt(i);

                switch (strChar)
                {
                    case ':':
                        {
                            strBuf.append("_ns-sep_");
                            break;
                        }
                    default:
                        {
                            strBuf.append(strChar);
                            break;
                        }
                }
            }
            str = strBuf.toString();
        }
        return str;
    }


    /**
     * Internal method to escape special attribute name characters, to handle things like name spaces.
     * @param str The string to escape the characters in.
     */
    private String escapeStringSpecialCharacters(String str)
    {
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "escapeStringSpecialCharacters(String)");

        if (str != null)
        {
            StringBuffer strBuf = new StringBuffer("");

            for (int i = 0; i < str.length(); i++)
            {
                char strChar = str.charAt(i);

                switch (strChar)
                {
                    case '"':
                        {
                            strBuf.append("\\\"");
                            break;
                        }
                    case '\t':
                        {
                            strBuf.append("\\t");
                            break;
                        }
                    case '\b':
                        {
                            strBuf.append("\\b");
                            break;
                        }
                    case '\\':
                        {
                            strBuf.append("\\\\");
                            break;
                        }
                    case '\f':
                        {
                            strBuf.append("\\f");
                            break;
                        }
                    case '\r':
                        {
                            strBuf.append("\\r");
                            break;
                        }
                    case '/':
                        {
                            strBuf.append("\\/");
                            break;
                        }
                    default:
                        {
                            if ((strChar >= 32) && (strChar <= 126) )
                            {
                                strBuf.append(strChar);
                            }
                            else if (strChar > 126)
                            {
                              if ((escapeNonAsciiChars)) 
                              {
                                  strBuf.append("\\u");
                                  StringBuffer sb = new StringBuffer(Integer.toHexString(strChar));
                                  while (sb.length() < 4)
                                  {
                                      sb.insert(0,'0');
                                  }
                                  strBuf.append(sb.toString());
                              }
                              else {
                                strBuf.append(strChar);
                              }
                            }
                            else 
                            {
                                strBuf.append("\\u");
                                StringBuffer sb = new StringBuffer(Integer.toHexString(strChar));
                                while (sb.length() < 4)
                                {
                                    sb.insert(0,'0');
                                }
                                strBuf.append(sb.toString());
                            }

                            break;
                        }
                }
            }
            str = strBuf.toString();
        }
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "escapeStringSpecialCharacters(String)");
        return str;
    }

    /**
     * Internal method to write out all children JSON objects attached to this JSON object.
     * @param writer The writer to use while writing the JSON text.
     * @param depth The indention depth of the JSON text.
     * @param compact Flag to denote whether or not to write in nice indent format, or compact format.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeChildren(Writer writer, int depth, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeChildren(Writer, int, boolean)");

        if (!jsonObjects.isEmpty())
        {
            Enumeration keys = jsonObjects.keys();
            while (keys.hasMoreElements())
            {
                String objName = (String)keys.nextElement();
                Vector vect = (Vector)jsonObjects.get(objName);
                if (vect != null && !vect.isEmpty())
                {
                    /**
                     * Non-array versus array elements.
                     */
                    if (vect.size() == 1)
                    {
                        if (logger.isLoggable(Level.FINEST)) logger.logp(Level.FINEST, className, "writeChildren(Writer, int, boolean)", "Writing child object: [" + objName + "]");

                        JSONObject obj = (JSONObject)vect.elementAt(0);
                        obj.writeObject(writer,depth + 1, false, compact);
                        if (keys.hasMoreElements())
                        {
                            try
                            {
                                if (!compact)
                                {
                                    if (!obj.isTextOnlyObject() && !obj.isEmptyObject())
                                    {
                                        writeIndention(writer,depth + 1);
                                    }
                                    writer.write(",\n");
                                }
                                else
                                {
                                    writer.write(",");
                                }
                            }
                            catch (Exception ex)
                            {
                                IOException iox = new IOException("Error occurred on serialization of JSON text.");
                                iox.initCause(ex);
                                throw iox;
                            }
                        }
                        else
                        {
                            if (obj.isTextOnlyObject() && !compact)
                            {
                                writer.write("\n");
                            }
                        }
                    }
                    else
                    {
                        if (logger.isLoggable(Level.FINEST)) logger.logp(Level.FINEST, className, "writeChildren(Writer, int, boolean)", "Writing array of JSON objects with attribute name: [" + objName + "]");

                        try
                        {
                            if (!compact)
                            {
                                writeIndention(writer,depth + 1);
                                writer.write("\"" + objName + "\"");
                                writer.write(" : [\n");
                            }
                            else
                            {
                                writer.write("\"" + objName + "\"");
                                writer.write(":[");
                            }
                            for (int i = 0; i < vect.size(); i++)
                            {
                                JSONObject obj = (JSONObject)vect.elementAt(i);
                                obj.writeObject(writer,depth + 2, true, compact);

                                /**
                                 * Still more we haven't handled.
                                 */
                                if (i != (vect.size() -1) )
                                {
                                    if (!compact)
                                    {
                                        if (!obj.isTextOnlyObject() && !obj.isEmptyObject())
                                        {
                                            writeIndention(writer,depth + 2);
                                        }
                                        writer.write(",\n");
                                    }
                                    else
                                    {
                                        writer.write(",");
                                    }
                                }
                            }

                            if (!compact)
                            {
                                writer.write("\n");
                                writeIndention(writer,depth + 1);
                            }

                            writer.write("]");
                            if (keys.hasMoreElements())
                            {
                                writer.write(",");
                            }

                            if (!compact)
                            {
                                writer.write("\n");
                            }
                        }
                        catch (Exception ex)
                        {
                            IOException iox = new IOException("Error occurred on serialization of JSON text.");
                            iox.initCause(ex);
                            throw iox;
                        }
                    }
                }
            }
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeChildren(Writer, int, boolean)");
    }

    /**
     * Method to write an 'empty' XML tag, like <F/>
     * @param writer The writer object to render the XML to.
     * @param indentDepth How far to indent.
     * @param contentOnly Whether or not to write the object name as part of the output
     * @param compact Flag to denote whether to output in a nice indented format, or in a compact format.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeEmptyObject(Writer writer, int indentDepth, boolean contentOnly, boolean compact) throws IOException {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeEmptyObject(Writer, int, boolean, boolean)");

        if (!contentOnly) {
            if (!compact) {
                writeIndention(writer, indentDepth);
                writer.write("\"" + this.objectName + "\"");
                writer.write(" : \"\"");
            } else {
                writer.write("\"" + this.objectName + "\"");
                writer.write(" : \"\"");
            }

        } else {
            if (!compact) {
                writeIndention(writer, indentDepth);
                writer.write("\"\"");
            } else {
                writer.write("\"\"");
            }
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeEmptyObject(Writer, int, boolean, boolean)");
    }


    /**
     * Method to write a text ony XML tagset, like <F>FOO</F>
     * @param writer The writer object to render the XML to.
     * @param indentDepth How far to indent.
     * @param contentOnly Whether or not to write the object name as part of the output
     * @param compact Whether or not to write the ohject in compact form, or nice indent form.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeTextOnlyObject(Writer writer, int indentDepth, boolean contentOnly, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeTextOnlyObject(Writer, int, boolean, boolean)");

        if (!contentOnly)
        {
            writeAttribute(writer,this.objectName,this.tagText.trim(),indentDepth, compact);
        }
        else
        {
            if (!compact)
            {
                writeIndention(writer, indentDepth);
                writer.write("\"" + escapeStringSpecialCharacters(this.tagText.trim()) + "\"");
            }
            else
            {
                writer.write("\"" + escapeStringSpecialCharacters(this.tagText.trim()) + "\"");
            }
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeTextOnlyObject(Writer, int, boolean, boolean)");
    }

    /**
     * Method to write aa standard attribute/subtag containing object
     * @param writer The writer object to render the XML to.
     * @param indentDepth How far to indent.
     * @param contentOnly Whether or not to write the object name as part of the output
     * @param compact Flag to denote whether or not to write the object in compact form.
     * @throws IOException Trhown if an error occurs on write.
     */
    private void writeComplexObject(Writer writer, int indentDepth, boolean contentOnly, boolean compact)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "writeComplexObject(Writer, int, boolean, boolean)");

        boolean wroteTagText = false;

        if (!contentOnly)
        {
            if (logger.isLoggable(Level.FINEST)) logger.logp(Level.FINEST, className, "writeComplexObject(Writer, int, boolean, boolean)", "Writing object: [" + this.objectName + "]");

            if (!compact)
            {
                writeIndention(writer, indentDepth);
            }

            writer.write( "\"" + this.objectName + "\"");

            if (!compact)
            {
                writer.write(" : {\n");
            }
            else
            {
                writer.write(":{");
            }
        }
        else
        {
            if (logger.isLoggable(Level.FINEST)) logger.logp(Level.FINEST, className, "writeObject(Writer, int, boolean, boolean)", "Writing object contents as an anonymous object (usually an array entry)");

            if (!compact)
            {
                writeIndention(writer, indentDepth);
                writer.write("{\n");
            }
            else
            {
                writer.write("{");
            }
        }

        if (this.tagText != null && !this.tagText.equals("") && !this.tagText.trim().equals(""))
        {
            writeAttribute(writer,"content", this.tagText.trim(), indentDepth + 1, compact);
            wroteTagText = true;
        }

        if (this.attrs != null && !this.attrs.isEmpty() && wroteTagText)
        {
            if (!compact)
            {
                writer.write(",\n");
            }
            else
            {
                writer.write(",");
            }
        }

        writeAttributes(writer,this.attrs,indentDepth, compact);
        if (!this.jsonObjects.isEmpty())
        {
            if (this.attrs != null && (!this.attrs.isEmpty() || wroteTagText))
            {
                if (!compact)
                {
                    writer.write(",\n");
                }
                else
                {
                    writer.write(",");
                }

            }
            else
            {
                if (!compact)
                {
                    writer.write("\n");
                }
            }
            writeChildren(writer, indentDepth, compact);
        }
        else
        {
            if (!compact)
            {
                writer.write("\n");
            }
        }

        if (!compact)
        {
            writeIndention(writer, indentDepth);
            writer.write("}\n");
            //writer.write("\n");
        }
        else
        {
            writer.write("}");
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "writeComplexObject(Writer, int, boolean, boolean)");
    }


    /**
     * Internal Helper method for determining if this is an empty tag inan XML document, such as <F/>
     * @return boolean denoting whether or not the object being written contains any attributes, tags, or text.
     */
    private boolean isEmptyObject()
    {
        boolean retVal = false;

        /**
         * Check for no attributes
         */
        if (this.attrs == null || (this.attrs != null && this.attrs.isEmpty()))
        {
            /**
             * Check for no sub-children
             */
            if (this.jsonObjects.isEmpty())
            {
                /**
                 * Check for no tag text.
                 */
                if (this.tagText == null || (this.tagText != null && this.tagText.trim().equals("")))
                {
                    retVal = true;
                }
            }
        }

        return retVal;
    }

    /**
     * Internal Helper method for determining if this is an XML tag which contains only freeform text, like: <F>foo</F>
     * @return boolean denoting whether or not the object being written is a text-only XML tagset.
     */
    private boolean isTextOnlyObject()
    {
        boolean retVal = false;

        /**
         * Check for no attributes
         */
        if (this.attrs == null || (this.attrs != null && this.attrs.isEmpty()))
        {
            /**
             * Check for no sub-children
             */
            if (this.jsonObjects.isEmpty())
            {
                /**
                 * Check for tag text contents.
                 */
                if (this.tagText != null && !this.tagText.trim().equals(""))
                {
                    retVal = true;
                }
            }
        }

        return retVal;
    }
}
