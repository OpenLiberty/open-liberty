package com.ibm.ws.sib.utils.ras;
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


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * This class provides XML style tag formatting helper methods. These methods
 * are typically used to help the writing dump data to a Writer.
 */

public class FormattedWriter extends BufferedWriter {

  private int _indent = 0;        // Current line indentation in spaces
  private String _namespace = ""; // Current name space prefix

  /**
   * Constructor
   *
   * @param out A non-null Writer to which buffered characters are eventually written
   */

  public FormattedWriter(Writer out) {
    super(out);
  }

  /**
   * Constructor
   *
   * @param out A non-null Writer to which buffered characters are eventually written
   * @param sz The non-negative size of the buffer to use
   */

  public FormattedWriter(Writer out, int sz) {
    super(out, sz);
  }

  /**
   * Write a one line comment
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void comment (String comment) throws IOException {
    startComment();
    write(comment);
    endComment();
  }

  /**
   * End a comment
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void endComment () throws IOException {
    write("-->");
  }

  /**
   * Write an empty XML tag, for instance &lttag/&gt
   *
   * @param tag The name of the tag to be written
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void emptyTag(String tag) throws IOException {
    write('<');
    write(_namespace + tag);
    write("/>");
  }

  /**
   * Write an end XML tag, for instance &lt/tag&gt
   *
   * @param tag The name of the tag to be written
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void endTag(String tag) throws IOException {
    write("</");
    write(_namespace + tag);
    write('>');
  }

  /**
   * Increase the line indentation by 1 unit
   */

  public final void indent() {
    _indent++;
  }

  /**
   * Write an XML introducer, for instance &lt? version="1.0" encoding="UTF-8" ?&gt. This
   * is usually the first line of the XML file.
   *
   * @param text The text of the introducer
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void introducer (String text) throws IOException {
    write("<?" + text + "?>");
    newLine();
  }

  /**
   * Flush the currently buffered data and start a new line indenting to the
   * current indentation setting.
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public void newLine() throws IOException {
    flush();
    super.newLine();
    for (int i = 0; i < _indent; i++) {
      write("  ");
    }
  }

  /**
   * Decrease the line indentation by 1 unit
   */

  public final void outdent() {
      _indent--;
  }

  /**
   * Set the namespace prefix to be prefixed to subseqyent tags. The namespace prefix
   * allow different components to use the same tag without risk of confusion.
   *
   * @param namespace The name of the namespace prefix to be set
   */

  public final void nameSpace (String namespace) {
    if (namespace == null || namespace.equals("")) {
      _namespace = "";
    } else {
      _namespace = namespace + ":";
    }
  }

  // Defect 358424
  // This getter will allow classes that construct tags with 
  // properties to use the correct namespace for the tag. 
  // i.e. instead of writer.startTag()
  //
  // startTag()
  // {
  //      writer.write('<');
  //      writer.write(writer.getNameSpace());     <-- Allows correct use of
  //      writer.write(tagName);                       nameSpace when making 
  //      writer.write(" ID=\"");                      custom tags.
  //      writer.write(Long.toString(idValue));
  //      writer.write("\" >");
  // }
  public final String getNameSpace()
  {
      return _namespace;
  }

  /**
   * Start a comment
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void startComment () throws IOException {
    write("<!--");
  }

  /**
   * Write an XML start tag, for instance &lttag&gt
   *
   * @param tag The name of the tag to be written
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void startTag(String tag) throws IOException {
    write('<');
    write(_namespace + tag);
    write('>');
  }

  /**
   * Write out a one-line XML tag with a long datatype, for instance &lttag&gt123456&lt/tag&gt
   *
   * @param tag The name of the tag to be written
   *
   * @param value The data value to be written
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void taggedValue(String tag, long value) throws IOException {
    startTag(tag);
    write(Long.toString(value));
    endTag(tag);
  }

  /**
   * Write out a one-line XML tag with a Object datatype, for instance &lttag&gtobject&lt/tag&lt
   *
   * @param tag The name of the tag to be written
   *
   * @param value The data value to be written
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void taggedValue(String tag, Object value) throws IOException {
    startTag(tag);
    if (value == null) {
      write("null");
    } else {
      write(value.toString());
    }
    endTag(tag);
  }

  /**
   * Write a throwable, used to indicate a problem during data collection, not formatted.
   *
   * @param e The throwable object
   *
   * @throws IOException If an I/O error occurs while attempting to write the characters
   */

  public final void write(Throwable e) throws IOException {
    indent();
    newLine();

    write(e.toString());

    StackTraceElement[] elements = e.getStackTrace();
    for (int i = 0; i < elements.length; i++) {
      newLine();
      write(elements[i].toString());
    }

    Throwable cause = e.getCause();
    if (cause != null) {
      newLine();
      write(cause.toString());
    }

    outdent();
  }

}
