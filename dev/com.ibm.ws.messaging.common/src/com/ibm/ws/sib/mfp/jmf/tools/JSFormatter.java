/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.mfp.jmf.tools;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Date;

import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFFieldDef;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.jmf.impl.JSDynamic;
import com.ibm.ws.sib.mfp.jmf.impl.JSEnum;
import com.ibm.ws.sib.mfp.jmf.impl.JSField;
import com.ibm.ws.sib.mfp.jmf.impl.JSListCoder;
import com.ibm.ws.sib.mfp.jmf.impl.JSPrimitive;
import com.ibm.ws.sib.mfp.jmf.impl.JSType;
import com.ibm.ws.sib.mfp.jmf.impl.JSVariant;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
import com.ibm.ws.sib.mfp.jmf.impl.MessageMap;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.mfp.util.HexUtil;

/**
 * Contains functions to format various JMF entities in a human-readable way
 */

public final class JSFormatter {

  private static int MAX_FORMAT_SIZE = 10240;

  private JSFormatter() {
  }

  /**
   * Format a MessageMap
   *
   * @param map the MessageMap to be formatted
   * @param schema the JMFSchema to which the MessageMap conforms
   * @return a String representing the formatted MessageMap
   */
  public static String formatMap(MessageMap map, JMFSchema schema) {

    JmfTr.setTracing(false);

    StringBuffer ans = new StringBuffer();
    JSVariant[] vars = ((JSchema)schema).getVariants();
    JSField[] fields = ((JSchema)schema).getFields();
    ans.append("MultiChoice Code=").append(map.multiChoice);
    ans.append(", Offset Table Size=").append(map.offsetsNeeded);
    ans.append("\nVariant settings:\n");
    for (int i = 0; i < map.choiceCodes.length; i++) {
      int code = map.choiceCodes[i];
      if (code != -1) {
        JSVariant var = vars[i];
        String caseName = var.getCase(code).getFeatureName();
        if (caseName == null || caseName.length() == 0)
          caseName = "[" + code + "]";
        ans
          .append(" ")
          .append(getUsefulName((JSchema)schema, var))
          .append("=")
          .append(caseName)
          .append("\n");
      }
    }
    ans.append("Field offsets:\n");
    for (int i = 0; i < map.fields.length; i++) {
      MessageMap.Remap remap = map.fields[i];
      if (remap != null) {
        JSField field = fields[i];
        ans.append(" ");
        ans.append(getUsefulName((JSchema)schema, field));
        ans.append("(").append(getUsefulType(field)).append(")");
        ans.append(": ").append(formatRemap(remap));
        ans.append("\n");
      }
    }

    JmfTr.setTracing(true);

    return ans.toString();
  }


  /**
   * getUsefulName
   * Subroutine to get a useful name for a field or variant.  The pathName returned by the
   * JSchema is used unless it is empty, in which case we use the special name <root>.
   *
   * @param schema               The schema used to determine the name
   * @param field                The field whose name is wanted
   *
   * @return String The name of the field
   */
  // JSchema is used unless it is empty, in which case we use the special name <root>.
  private static String getUsefulName(JSchema schema, JSField field) {
    String ans = schema.getPathName(field);
    if (ans.length() == 0)
      ans = "<root>";
    return ans;
  }


  /**
   * getUsefulType
   * Subroutine to get a useful type name for a JSField.
   *
   * @param field             The field whose type is wanted.
   *
   * @return String           The type of the field.
   */
  private static String getUsefulType(JSField field) {
    StringBuilder list = new StringBuilder();
    for (int j = 0; j < field.getIndirection() + 1; j++)
      list.append("List of ");
    if (field instanceof JSEnum)
      return new String(list.append("Enum"));
    else if (field instanceof JSDynamic)
      return new String(list.append("Dynamic"));
    else if (field instanceof JSVariant)
      return new String(list.append("Variant Box"));
    else
      return new String(list.append(((JSPrimitive)field).getXSDTypeName()));
  }


  // Subroutine to format a MessageMap.Remap structure
  private static String formatRemap(MessageMap.Remap remap) {
    String ans = "";
    if (remap.offsetIndex > -1)
      ans += "[" + remap.offsetIndex + "]";
    ans += "+" + remap.fixedIncr;
    return ans;
  }


  /**
   * Produce a formatted MessageMap for a particular JMFSchema and multiChoice code
   *
   * @param schema the JMFSchema for which a MessageMap is desired
   *
   * @param multiChoice the multiChoice code for which a MessageMap is desired
   *
   * @return the formatted MessageMap
   */
  public static String formatSchema(JMFSchema schema, BigInteger multiChoice) {

    JmfTr.setTracing(false);
    String ans = formatMap(new MessageMap((JSchema)schema, multiChoice), schema);
    JmfTr.setTracing(true);

    return ans;
  }


  /**
   * Produce a formatted MessageMap for a particular JMFSchema, variant box, and
   * multiChoice code.
   *
   * @param schema the JMFSchema for which a MessageMap is desired
   *
   * @param box a pathname designating a variant box within schema for which a MessageMap
   * is desired
   *
   * @param multiChoice the multiChoice code (relative to the variant box) for which a
   * MessageMap is desired
   *
   * @return the formatted MessageMap, or null if box does not designate a variant box
   * within schema
   */
  public static String formatSchema(JMFSchema schema, String box, BigInteger multiChoice) {
    int accessor = schema.getAccessor(box);

    if (accessor == -1)
      return null;

    JMFFieldDef field = schema.getFieldDef(accessor);

    if (!(field instanceof JSVariant))
      return null;

    JSVariant boxedBy = ((JSVariant)field).getBoxedBy();

    if (boxedBy == null)
      return null;

    JSchema realSchema = (JSchema) boxedBy.getBoxed();

    if (realSchema == null)
      return null;

    JmfTr.setTracing(false);
    String ans = formatSchema(realSchema, multiChoice);
    JmfTr.setTracing(true);

    return ans;
  }


  /** Produce a 'book' of MessageMaps for a JMFSchema.  WARNING: if the schema has lots of
   * independent variants, its multiChoice count can be quite large and the resulting
   * schema book can be massive.  Consider using an "on demand" strategy of generating
   * individual MessageMaps from the schema as needed during debugging.
   *
   * @param schema the JMFSchema for which documentation of the complete set of
   * MessageMaps is desired
   *
   * @param wtr the PrintWriter to which the MessageMaps are to be formatted
   */
  public static void schemaBook(JMFSchema schema, PrintWriter wtr) {
    JmfTr.setTracing(false);
    wtr.println("Schema Book for " + formatSchema(schema));
    wtr.println();
    printBook((JSchema)schema, wtr);
    JSField[] fields = ((JSchema)schema).getFields();
    for (int i = 0; i < fields.length; i++) {
      if (fields[i] instanceof JSVariant) {
        JSVariant var = (JSVariant)fields[i];
        wtr.println("Maps for boxed variant " + schema.getPathName(var));
        wtr.println();
        printBook((JSchema) var.getBoxed(), wtr);
      }
    }
    JmfTr.setTracing(true);
  }


  // Print the set of message maps for a schema
  private static void printBook(JSchema schema, PrintWriter wtr) {
    for (BigInteger i = BigInteger.ZERO;
        i.compareTo(((JSType)schema.getJMFType()).getMultiChoiceCount()) < 0;
        i = i.add(BigInteger.ONE)) {
      wtr.println(formatMap(new MessageMap(schema, i), schema));
      wtr.println();
    }
  }


  /**
   * Format a JSMessage
   *
   * @param msg the JSMessage to be formatted
   *
   * @return the formatted form of the message
   */
  public static String format(JMFMessage msg) {
    return format(msg, true, true);
  }


  /**
   * Format a JSMessage, excluding the hashcodes of any object.
   * This method is used by some unit tests, which do want the payload data but
   * do not want the identityHashcode of Java instances as they foul up output
   * matching.
   *
   * @param msg the JSMessage to be formatted
   *
   * @return the formatted form of the message
   */
  public static String formatWithoutHashcodes(JMFMessage msg) {
    return format(msg, false, true);
  }


  /** Format a JSMessage without including the payload data
   *
   * @param msg the JSMessage to be formatted
   *
   * @return the formatted form of the message, without any payload data
   */
  public static String formatWithoutPayloadData(JMFMessage msg) {
    // Include the hashcode, but not the user's payload Data
    return format(msg, true, false);
  }


  /**
   * Format a JSMessage
   *
   * @param msg the JSMessage to be formatted
   * @param includeHashcode whether or not to include the messages.toString(), which includes
   *                     the identityHashcode. Including it means the output for 2 identical messages
   *                     doesn't match - not good for some unit tests.
   * @param includePayloadData whether or not to include the content of any payload/data field.
   *                     If this parameter is false, the user's payload data will not be included.
   *
   * @return the formatted form of the message
   */
  public static String format(JMFMessage msg, boolean includeHashcode, boolean includePayloadData) {
    StringBuffer ans = new StringBuffer();
    JmfTr.setTracing(false);
    try {
      ans.append("JMFMessage: ");
      if (includeHashcode) {
        ans.append(msg.toString()).append(", ");
      }
      ans.append("JMF version=").append(msg.getJMFEncodingVersion());
      JMFSchema[] schemata = msg.getSchemata();
      ans.append(", Schema=").append(formatSchema(schemata[0])).append("\n");
      if (schemata.length > 1) {
        ans.append("Also uses:");
        for (int i = 1; i < schemata.length; i++)
          ans.append(" ").append(formatSchema(schemata[i]));
        ans.append("\n");
      }
      ans.append("Length=");
      try {
        ans.append(msg.getEncodedLength()).append("\n");
      }
      catch (Exception e) {
        // No FFDC code needed
        ans.append("getEncodedLength failed, Exception: ");
        ans.append(e.toString()).append("\n");
      }
      // We can't have found any payloadData yet, so we can just pass in true for the last parameter
      formatPartToBuffer(ans, msg, "  ", includeHashcode, includePayloadData, true);
    } catch (Exception ex) {
      // No FFDC code needed
      // Return anything we have managed to format, plus the Exception */
      ans.append("\nMessage probably corrupted. Exception: ");
      ans.append(ex.toString());
    }

    JmfTr.setTracing(true);
    return ans.toString();
  }


  /**
   * Format a JMFNativePart
   *
   * @param part the JMFNativePart to be formatted
   *
   * @return the formatted form of the part
   */
  public static String formatPart(JMFNativePart part) {
    StringBuffer ans = new StringBuffer();
    JmfTr.setTracing(false);
    try {
      ans
        .append("JMFNativePart: ")
        .append(part.toString())
        .append(", Schema=")
        .append(formatSchema(part.getEncodingSchema()))
        .append("\n");
      formatPartToBuffer(ans, part, "  ", true, true, true);
    }
    catch (Exception ex) {
      // No FFDC code needed
      // Return anything we have managed to format, plus the Exception */
      ans.append("\nMessage probably corrupted. Exception: ");
      ans.append(ex.toString());
    }
    JmfTr.setTracing(true);
    return ans.toString();
  }


  // Subroutine to superficially format a JSchema, reporting its name and schema id
  private static String formatSchema(JMFSchema schema) {
    if(schema == null) return "null schema";
    byte[] buf = new byte[8];
    ArrayUtil.writeLong(buf, 0, schema.getID());
    return schema.getName() + "(" + HexUtil.toString(buf) + ")";
  }


  /**
   * formatPartToBuffer
   * Write a formatted view of the given JMFNativePart to the given StringBuffer
   *
   * @param ans                  The StringBuffer to format into
   * @param val                  The 'value' to format
   * @param indent               The indent blanks to prepend to any output record
   * @param includeHashcode      Whether to include the hashcode
   * @param includePayloadData   Whether to include the user data, if we find the payload data JMF message
   * @param includeValueData     Whether to include the actual data for field values for this part.
   *
   * @exception JMFException is thrown if JMF encounters a problem during the formatting of the message
   */
  private static void formatPartToBuffer(StringBuffer ans, JMFNativePart part, String indent, boolean includeHashcode, boolean includePayloadData, boolean includeValueData) throws JMFException {
    JSchema schema = (JSchema)part.getJMFSchema();
    JSField[] fields = schema.getFields();
    formatVariants(ans, part, fields.length, schema.getVariants(), indent);
    formatFields(ans, part, fields, indent, includeHashcode, includePayloadData, includeValueData);
  }


  /**
   * formatVariants
   * Iterate through the Variants of a JMFNativePart, formatting each one's name and
   * case name into the given StringBuffer.
   *
   * @param ans                  The StringBuffer to format into
   * @param part                 The JMFNativePart we're interested in
   * @param bias                 ????
   * @param vars                 An array containing the part's variants
   * @param indent               The indent blanks to prepend to any output record
   *
   * @exception JMFException is thrown if JMF encounters a problem during the formatting of the part
   */
  private static void formatVariants(StringBuffer ans, JMFNativePart part, int bias, JSVariant[] vars, String indent) throws JMFException {
    JSchema schema = (JSchema)part.getJMFSchema();
    ans.append(indent).append("Variants:\n");
    for (int i = 0; i < vars.length; i++) {
      if (part.isPresent(bias + i)) {
        JSVariant var = vars[i];
        String name = schema.getPathName(var);
        if (name == null || name.length() == 0)
          name = "<unnamed>";
        ans.append(indent).append(name).append("=");
        String caseName = var.getCase(part.getInt(bias + i)).getFeatureName();
        if (caseName != null && caseName.length() > 0)
          ans.append(caseName);
        else
          ans.append(part.getValue(bias + i));
        ans.append("\n");
      }
    }
  }


  /**
   * formatFields
   * Iterate through the fields of a JMFNativePart, formatting each one's name and
   * value into the given StringBuffer.
   *
   * @param ans                  The StringBuffer to format into
   * @param part                 The JMFNativePart we're interested in
   * @param fields               An array containing the fields for the part
   * @param indent               The indent blanks to prepend to any output record
   * @param includeHashcode      Whether to include the hashcode
   * @param includePayloadData   Whether to include the user data, if we find the payload data JMF message
   * @param includeValueData     Whether to include the actual data for field values for this part.
   *
   * @exception JMFException is thrown if JMF encounters a problem during the formatting of the message
   */
  private static void formatFields(StringBuffer ans, JMFNativePart part, JSField[] fields, String indent, boolean includeHashcode, boolean includePayloadData, boolean includeValueData) throws JMFException {
    JSchema schema = (JSchema)part.getJMFSchema();
    ans.append(indent).append("Fields:\n");
    for (int i = 0; i < fields.length; i++) {
      if (part.isPresent(i)) {
        String name = schema.getPathName(fields[i]);
        if (name == null || name.length() == 0) {
          name = "<unnamed>";
        }
        ans.append(indent).append(name).append("=");
        String fieldName = schema.getFieldDef(i).getFeatureName();

        // If we have already established that we don't want to include data in this
        // part, or we don't want payload data AND we've found the payload/data Dynamic field,
        // tell formatValue not to include value data below here
        if (  (includeValueData == false) ||
              ((includePayloadData == false) && name.equals("payload/data"))
           ) {
          formatValue(ans, part.getValue(i), indent, includeHashcode, false);
        }
        // If this is a password field, display the data as asterisks rather than the actual password
        else if(fieldName != null && fieldName.toLowerCase().equals("password")) {
          formatValue(ans, "********", indent, includeHashcode, true);
        }
        // Otherwise just percolate on down displaying data unless it is too big
        // to be reasonable to format. Always format a JSDynamic, or List, regardless
        // of its estimated overall size - any huge fields within it will be skipped.
        else {
          if (  (fields[i].getCoder() instanceof JSDynamic)
             || (fields[i].getCoder() instanceof JSListCoder)
             || (part.estimateUnassembledValueSize(i) < MAX_FORMAT_SIZE)
             ) {
            formatValue(ans, part.getValue(i), indent, includeHashcode, true);
          }
          else {
            ans.append("*** Value too large to format, estimated unassembled size: " + part.estimateUnassembledValueSize(i));
            ans.append("\n");
          }
        }
      }
    }
  }


  /**
   * formatValue
   * Subroutine to format a value, which may be another JMF Message, a JMF List
   * or a leaf value.
   *
   * @param ans                  The StringBuffer to format into
   * @param val                  The 'value' to format
   * @param indent               The indent blanks to prepend to any output record
   * @param includeHashcode      Whether to include the hashcode
   * @param includeValueData     Whether to include the actual data for the value and anything below it.
   *
   * @exception JMFException is thrown if JMF encounters a problem during the formatting of the part
   */
  private static void formatValue(StringBuffer ans, Object val, String indent, boolean includeHashcode, boolean includeValueData) throws JMFException {

    // If the field was a Dynamic, the value may be a JMFNativePart, in which case we recursively format it
    if (val instanceof JMFNativePart) {
      JMFNativePart subPart = (JMFNativePart)val;
      ans.append("JMFNativePart");
      if (includeHashcode) {
        ans.append(" ").append(subPart.toString());
      }
      ans.append(", Schema=")
         .append(formatSchema(subPart.getEncodingSchema()))
         .append(":\n");

      // At this point, we are below the 'interesting' payload/data field, so we already know whether
      // we do or don't want valueData to be included in all sub-parts.
      formatPartToBuffer(ans, subPart, indent + "  ", includeHashcode, includeValueData, includeValueData);
    }

    // If the value is a JMFList, use the specific List format method
    else if (val instanceof JMFList) {
      JMFList list = (JMFList)val;
      ans.append("JSList");
      if (includeHashcode) {
        ans.append(" ").append(list.toString());
      }
      ans.append(", size=").append(list.size()).append(":\n");
      formatList(ans, list, indent + "  ", includeHashcode, includeValueData);
    }

    // Otherwise we have a Date, a byte array or something other value which is a 'leaf'
    // as far as JMF is concerned and is either a primitive or something which can toString itself
    else {
      // Only include the value data if it is wanted
      if (includeValueData) {
        if (val instanceof Date)
          ans.append(((Date)val).toGMTString());
        else if (val instanceof byte[])
          ans.append(HexUtil.toString((byte[])val));
        else
          ans.append(val);
      }
      // Otherwise just announce that it is user data
      else {
        ans.append("<<*** user data ***>>");
      }
      ans.append("\n");
    }
  }


  /**
   * formatList
   * Format a JMF List, and its sub-values, into the given StringBuffer
   *
   * @param ans                  The StringBuffer to format into
   * @param list                 The JMFList to format
   * @param indent               The indent blanks to prepend to any output record
   * @param includeHashcode      Whether to include the hashcode
   * @param includeValueData     Whether to include the actual data for the value and anything below it.
   * @param list                 description of list
   *
   * @exception JMFException is thrown if JMF encounters a problem during the formatting of the part
   */
  private static void formatList(StringBuffer ans, JMFList list, String indent, boolean includeHashcode, boolean includeValueData) throws JMFException {
    for (int i = 0; i < list.size(); i++) {
      ans.append(indent).append("[").append(i).append("] ");
      if (list.isPresent(i))
        formatValue(ans, list.get(i), indent, includeHashcode, includeValueData);
      else
        ans.append("<missing>\n");
    }
  }
}
