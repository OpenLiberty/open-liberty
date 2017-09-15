/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.renderkit.html;

/**
 * The ScriptContext offers methods and fields
 * to help with rendering out a script and keeping a
 * proper formatting.
 */
public class JavascriptContext
{
    private static final String LINE_SEPARATOR = System.getProperty(
            "line.separator", "\r\n");
    private static final char TABULATOR = '\t';

    private long currentIndentationLevel;
    private StringBuilder buffer = new StringBuilder();
    private boolean prettyPrint = false;
    /**
     * automatic formatting will render
     * new-lines and indents if blocks are opened
     * and closed - attention: you need to append
     * opening and closing brackets of blocks separately in this case!
     */
    private boolean automaticFormatting = true;

    public JavascriptContext()
    {

    }

    public JavascriptContext(boolean prettyPrint)
    {
        this.prettyPrint = prettyPrint;
    }

    public JavascriptContext(StringBuilder buf, boolean prettyPrint)
    {
        this.prettyPrint = prettyPrint;
        this.buffer = buf;
    }

    public void increaseIndent()
    {
        currentIndentationLevel++;
    }

    public void decreaseIndent()
    {
        currentIndentationLevel--;

        if (currentIndentationLevel < 0)
        {
            currentIndentationLevel = 0;
        }
    }

    public void prettyLine()
    {
        if (prettyPrint)
        {
            append(LINE_SEPARATOR);

            for (int i = 0; i < getCurrentIndentationLevel(); i++)
            {
                append(TABULATOR);
            }
        }
    }

    public void prettyLineIncreaseIndent()
    {
        increaseIndent();
        prettyLine();
    }

    public void prettyLineDecreaseIndent()
    {
        decreaseIndent();
        prettyLine();
    }

    public long getCurrentIndentationLevel()
    {
        return currentIndentationLevel;
    }

    public void setCurrentIndentationLevel(long currentIndentationLevel)
    {
        this.currentIndentationLevel = currentIndentationLevel;
    }

    public JavascriptContext append(String str)
    {

        if (automaticFormatting && str.length() == 1)
        {
            boolean openBlock = str.equals("{");
            boolean closeBlock = str.equals("}");

            if (openBlock)
            {
                prettyLine();
            }
            else if (closeBlock)
            {
                prettyLineDecreaseIndent();
            }

            buffer.append(str);

            if (openBlock)
            {
                prettyLineIncreaseIndent();
            }
            else if (closeBlock)
            {
                prettyLine();
            }
        }
        else
        {
            buffer.append(str);
        }
        return this;
    }

    public JavascriptContext append(char c)
    {
        buffer.append(c);
        return this;
    }

    public JavascriptContext append(int i)
    {
        buffer.append(i);
        return this;
    }

    public String toString()
    {
        return buffer.toString();
    }
}
