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
package com.ibm.ws.sip.parser.util;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * A utility class that can convert java.util.Date to 'iso 8601' format and 
 * vice versa.
 */
public class DateParser
{
    //
    // Members.
    //
    /**
     * the calendar object.
     */
    private static Calendar m_calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    
    
    //
    // Operations.
    //

    /**
     * Parse the given string in ISO 8601 format and build a Date object.
     * 
     * @param iso8601Date the date in ISO 8601 format
     * @return a java.util.Date object.
     * @exception PresenceDocumentParseException if the date is not valid
     */
    public synchronized static Date parse(String iso8601Date)
        throws UnsupportedEncodingException
    {
        Calendar calendar = getCalendar(iso8601Date);
        return calendar.getTime();
    }

    /**
     * Generate an ISO 8601 date. 
     * 
     * @param date The date as java.util.Date
     * @return a string representing the date in the ISO 8601 format
     */
    public synchronized static String getIsoDate(Date date)
    {
        m_calendar.setTime(date);
        StringBuffer buffer = new StringBuffer(16);
        buffer.append(m_calendar.get(Calendar.YEAR));
        buffer.append("-");
        buffer.append(twoDigit(m_calendar.get(Calendar.MONTH) + 1));
        buffer.append("-");
        buffer.append(twoDigit(m_calendar.get(Calendar.DAY_OF_MONTH)));
        buffer.append("T");
        buffer.append(twoDigit(m_calendar.get(Calendar.HOUR_OF_DAY)));
        buffer.append(":");
        buffer.append(twoDigit(m_calendar.get(Calendar.MINUTE)));
        buffer.append(":");
        buffer.append(twoDigit(m_calendar.get(Calendar.SECOND)));
        buffer.append(".");
        buffer.append(twoDigit(m_calendar.get(Calendar.MILLISECOND) / 10));
        buffer.append("Z");
        return buffer.toString();
    }

    //
    // Helpers.
    //
    /**
     * Create a calendar object in the iso 8601 format from the given string.
     * 
     * @param isoDate the date as a string.
     */
    private static Calendar getCalendar(String isoDate)
        throws UnsupportedEncodingException
    {
        // YYYY-MM-DDThh:mm:ss.sTZD
        StringTokenizer st = new StringTokenizer(isoDate, "-T:.+Z", true);

        //Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        m_calendar.clear();
        try
        {
            // Year
            if (st.hasMoreTokens())
            {
                int year = Integer.parseInt(st.nextToken());
                m_calendar.set(Calendar.YEAR, year);
            }
            else
            {
                return m_calendar;
            }

            // Month
            if (check(st, "-") && (st.hasMoreTokens()))
            {
                int month = Integer.parseInt(st.nextToken()) - 1;
                m_calendar.set(Calendar.MONTH, month);
            }
            else
            {
                return m_calendar;
            }

            // Day
            if (check(st, "-") && (st.hasMoreTokens()))
            {
                int day = Integer.parseInt(st.nextToken());
                m_calendar.set(Calendar.DAY_OF_MONTH, day);
            }
            else
            {
                return m_calendar;
            }

            // Hour
            if (check(st, "T") && (st.hasMoreTokens()))
            {
                int hour = Integer.parseInt(st.nextToken());
                m_calendar.set(Calendar.HOUR_OF_DAY, hour);
            }
            else
            {
                m_calendar.set(Calendar.HOUR_OF_DAY, 0);
                m_calendar.set(Calendar.MINUTE, 0);
                m_calendar.set(Calendar.SECOND, 0);
                m_calendar.set(Calendar.MILLISECOND, 0);
                return m_calendar;
            }

            // Minutes
            if (check(st, ":") && (st.hasMoreTokens()))
            {
                int minutes = Integer.parseInt(st.nextToken());
                m_calendar.set(Calendar.MINUTE, minutes);
            }
            else
            {
                m_calendar.set(Calendar.MINUTE, 0);
                m_calendar.set(Calendar.SECOND, 0);
                m_calendar.set(Calendar.MILLISECOND, 0);
                return m_calendar;
            }
            if (!st.hasMoreTokens())
            {
                return m_calendar;
            }

			// Seconds
            String tok = st.nextToken();
            if (tok.equals(":"))
            {
                
                if (!st.hasMoreTokens())
                {
                    throw new UnsupportedEncodingException("No seconds specified");
                }

                int seconds = Integer.parseInt(st.nextToken());
                m_calendar.set(Calendar.SECOND, seconds);
                if (!st.hasMoreTokens())
                {
                    return m_calendar;
                }

                // frac sec
                tok = st.nextToken();
                if (tok.equals("."))
                {
                    String nt = st.nextToken();
                    while (nt.length() < 3)
                    {
                        nt += "0";
                    }
                    nt = nt.substring(0, 3); // Cut trailing chars..
                    int millisec = Integer.parseInt(nt);
                    m_calendar.set(Calendar.MILLISECOND, millisec);
                    if (!st.hasMoreTokens())
                    {
                        return m_calendar;
                    }
                    tok = st.nextToken();
                }
                else
                {
                    m_calendar.set(Calendar.MILLISECOND, 0);
                }
            }
            else
            {
                m_calendar.set(Calendar.SECOND, 0);
                m_calendar.set(Calendar.MILLISECOND, 0);
            }

            // Timezone
            if (!tok.equals("Z"))
            { // UTC
                if (!(tok.equals("+") || tok.equals("-")))
                {
                    throw new UnsupportedEncodingException("only Z, + or - allowed");
                }
                boolean plus = tok.equals("+");
                if (!st.hasMoreTokens())
                {
                    throw new UnsupportedEncodingException("Missing hour field");
                }
                int tzhour = Integer.parseInt(st.nextToken());
                int tzmin = 0;
                if (check(st, ":") && (st.hasMoreTokens()))
                {
                    tzmin = Integer.parseInt(st.nextToken());
                }
                else
                {
                    throw new UnsupportedEncodingException("Missing minute field");
                }
                if (plus)
                {
                    m_calendar.add(Calendar.HOUR, tzhour);
                    m_calendar.add(Calendar.MINUTE, tzmin);
                }
                else
                {
                    m_calendar.add(Calendar.HOUR, -tzhour);
                    m_calendar.add(Calendar.MINUTE, -tzmin);
                }
            }
        }
        catch (NumberFormatException ex)
        {
            throw new UnsupportedEncodingException(
                "[" + ex.getMessage() + "] is not an integer");
        }
        return m_calendar;
    }

	/**
	 * Check that the given token is the next token in the given token stream.
	 * @throws PresenceDocumentParseException 
	 */
    private static boolean check(StringTokenizer st, String token)
        throws UnsupportedEncodingException
    {
        try
        {
            if (st.nextToken().equals(token))
            {
                return true;
            }
            else
            {
                throw new UnsupportedEncodingException(
                    "Missing [" + token + "]");
            }
        }
        catch (NoSuchElementException ex)
        {
            return false;
        }
    }
    
	/**
	 * Return a 2 digit number from the given int.
	 */
    private static String twoDigit(int i)
    {
        if (i >= 0 && i < 10)
        {
            return "0" + String.valueOf(i);
        }
        return String.valueOf(i);
    }

}
