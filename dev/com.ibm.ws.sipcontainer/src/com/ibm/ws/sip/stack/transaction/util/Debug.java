/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.util;

import java.util.Date;

import com.ibm.ws.sip.properties.StackProperties;

/**
 * A utility class, used for debugging.
 */
public class Debug 
{
     
    /**
     * The debug level. 
     * 0 - No debug messages or assertions. 
     * 1 - Critical messages (Assertions on all levels except 0). 
     * 2 - The default level for all unassigned messages. 
     * 3 - 5 Messages levels where level 5 indicates output to console of 
     * all messages. 
     * 
     * The default is 0 which means no debug. 
     */
    public static int m_debugLevel = StackProperties.AGENT_TRACE_LEVEL_DEFAULT;    
    
    /**
     * Date object for adding timestamp to log messages. 
     */
    private static Date c_date = new Date(); 
        

	/**
	 * hex digits used for hex dumping
	 */
	private static final char hexDigits[] = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
		
    /**
     * Try to find a class that indicates the requested debug level. 
     * If not found use the default level. 
     */
    static
    {
    	//set the debug level
        m_debugLevel = ApplicationProperties.getProperties().getInt( StackProperties.AGENT_TRACE_LEVEL);
		if (m_debugLevel != StackProperties.AGENT_TRACE_LEVEL_DEFAULT)
		// @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:02 AM
			System.out.println("Debug level set to: " + m_debugLevel);
    }
    
    
    /**
     * Print the given string to the standard error.
     * 
     * @param message the string to print.
     */
    public final static void print(String message) 
    {
        print(StackProperties.AGENT_TRACE_LEVEL_DEFAULT, message);
    }
      
    
    /**
     * Print the given string to the standard error.
     * @param debugLevel The level of debug. If the level requested equals or is 
     * lower then the current debug level then the message will be printed. 
     * @param message the string to print.
     */
	public final static void print(int debugLevel, String message) 
    {
        if(debugLevel <= m_debugLevel)        
        {
            // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:05 AM
            System.err.print(message);
        }
    }

    
    /**
     * Print the given string to the standard error, followed by a newline.
     *
     * @param message the string to print.
     */
	public final static void println(String message) 
    {
        println(StackProperties.AGENT_TRACE_LEVEL_DEFAULT, message);
    }
  
    
    /**
     * Print the given string to the standard error.
     * @param debugLevel The level of debug. If the level requested equals or is 
     * lower then the current debug level then the message will be printed. 
     * @param message the string to print.
     */
	public final static void println(int debugLevel, String message) 
    {
        if(debugLevel <= m_debugLevel)
        {
			StringBuffer buf = new StringBuffer(getBasePrefix());
			buf.append(' ');
			buf.append( message );
			buf.append('\n');
        	// @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:03 AM
        	System.out.println(  buf.toString() );
        }
    }
    
    
    /**
     * Print the given string to the standard error with a time stamp
     * @param debugLevel The level of debug. If the level requested equals or is 
     * lower then the current debug level then the message will be printed. 
     * @param message the string to print.
     */
	public final static void printlnT(int debugLevel, String message) 
    {
        if(debugLevel <= m_debugLevel)
        {
            c_date.setTime(System.currentTimeMillis()); 
                        
            StringBuffer buffer = new StringBuffer(message.length() + 30); 
            buffer.append(c_date.toString());             
            buffer.append(' ');
            buffer.append(message);
            // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:03 AM
            System.err.println(buffer.toString());
        }
    }

    /**
     * Print the stack trace of the given exception to the standard error.
     *
     * @param debugLevel The level of debug. If the level requested equals or
     * is lower then the current debug level then the message will be printed.
     * @param message An additional message to print before the exception,
     * or null for none.
     * @param exception the exception (well, actually, throwable) to print.
     */
	public final static void printException(int debugLevel, String message,
                                            Throwable exception)
    {
        if(debugLevel <= m_debugLevel)
        {
            // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:03 AM
            if (message != null)
            {
                // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:04 AM
                System.err.println (message);
            }
            exception.printStackTrace();
        }
    }
    
    /**
     * Print a single byte as two hex digits.
     * 
     * @param debugLevel The level of debug.
     * @param n The byte to print.
     */
	public final static void printByte(int debugLevel, byte n) 
    {
        if(debugLevel > m_debugLevel)
        {
            return;
        }
        
        int intValue = n & 0xff;
        String s = Integer.toHexString(intValue);
        
        if (s.length() < 2)
        {
            s = "0" + s;
        }
        // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:08 AM
        System.err.print(s);
    }

    
    /**
     * Print an array of bytes.
     *
     * @param debugLevel The level of debug.
     * @param array The array to print.
     */
	public final static void printByte(int debugLevel, byte[] array) 
    {
        if(debugLevel > m_debugLevel)
        {
            return;
        }
        
        int i,j;
        for (i = 0; i < array.length; i += 16) 
        {
        	// @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:07 AM
        	System.err.println("");
            // The index.
            printByte(debugLevel, (byte)(i >> 8));
            printByte(debugLevel, (byte)(i & 0xff));
            // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:02 AM
            System.err.print(" -  ");

            int toPrint = array.length - i;
            int thisLine = (toPrint > 16 ? 16 : toPrint);

            for (j = 0; j < thisLine; j++) 
            {                
                printByte(debugLevel, array[i + j]);
//              @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:03 AM
                // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:07 AM
                System.err.print(" ");
            }
        
            if (thisLine < 16) 
            {
                for (; j < 16; j++)
                // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:04 AM                	
                System.err.print("   ");
            }
      
            for (j = 0; j < thisLine; j++) 
            {
                if (array[i + j] > 32 && array[i + j] <= 127)
                    // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:02 AM
                    System.err.print("" + (char)array[i + j]);
                else
                    // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:04 AM
                    System.err.print(".");
            
            }
        }
        // @PMD:REVIEWED:UnprotectedSystemOutOrErr: by Amirk on 9/19/04 11:04 AM
        System.err.println("");
    }

	/**
	 * dumps given byte array as user-friendly hex representation
	 * @param buffer input buffer
	 * @param offset position in buffer to start reading
	 * @param length input buffer content length
	 * @param dest destination string buffer
	 */
	public static void hexDump(byte[] buffer, int offset, int length, StringBuffer dest) {
		int count;
		for (int index = 0; length > 0; length -= count) {
			// 4 digit offset
			appendHexNumber(index, dest, 4);
			dest.append("  ");

			// 16 * 2-digit octets
			count = (length > 16) ? 16 : length;
			int i;
			for (i = 0; i < count; i++) {
				byte b = buffer[offset+i];
				dest.append(hexDigits[(b >> 4) & 0x0f]);
				dest.append(hexDigits[b & 0x0f]);
				dest.append(i == 7 ? ':' : ' ');
			}
			for (; i < 16; i++) {
				dest.append("   ");
			}
			dest.append(' ');

			// string representation of the 16 octets
			for (i = 0; i < count; i++) {
				byte b = buffer[offset+i];
				boolean printable = 32 <= b && b <= 126;
				dest.append(printable ? (char)b : '.');
			}
			dest.append('\n');
			offset += count;
			index += count;
		}
	}

	/**
	 * appends a positive int to a string buffer.
	 * 
	 * @param value source number
	 * @param buffer destination string buffer
	 * @param pad fixed number of digits to write.
	 *  if value does not have this many digits, insert 0s to the left
	 */
	private static void appendHexNumber(int value, StringBuffer buffer, int pad) {
		char dig = hexDigits[value % 16]; // rightmost digit
		int remain = value / 16;
		if (remain > 0) {
			// recurse digits from right to left
			appendHexNumber(remain, buffer, pad - 1);
		}
		else {
			// pad before printing the lefmost digit
			for (int i = 1; i < pad; i++) {
				buffer.append('0');
			}
		}
		buffer.append(dig);
    }

    /**
     // @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 11:04 AM
     * Assert that a given condition is true.
     *
     * @param condition The condition to assert.
     *
     * @exception RuntimeException if the given condition is false.
     */
	public final static void stAssert(boolean condition)     
    {
        if (!condition && m_debugLevel != 0)
        {
        	// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 11:03 AM
        	throw new RuntimeException("Assertion failed.");
        }
    }
    
    
    /**
     * Indicates whether the current debug level equals or greater then 
     * the default debug level (3). Used for optimizing unnecessary calls 
     * to the debug print method. 
     */
	public static final boolean isDebug()
    {
        return (m_debugLevel >= StackProperties.AGENT_TRACE_LEVEL_DEFAULT);    
    }
    
    
    /**
     * Indicates whether the specified debug level equals or greater then 
     * the default debug level (3). Used for optimizing unnecessary calls 
     * to the debug print method. 
     * 
     * @param level the specified debug level. 
     */
	public static final boolean isDebugLevel(int level)
    {
        return (m_debugLevel >= level);    
    }
    
	/** 
	 * get the basic prefix
	 * @return String
	 */
	private static String getBasePrefix()
	{
		return c_date + " [SIPStack]: Thread: " + Thread.currentThread().getName() ; 
	}    
}


