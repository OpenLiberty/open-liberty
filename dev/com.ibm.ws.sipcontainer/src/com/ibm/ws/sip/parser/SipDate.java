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
package com.ibm.ws.sip.parser;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * A wrraper on standard Java Date class, to enforce sip encoding. 
 * 
 * @author Assaf Azaria
 */
public class SipDate implements Separators
{
    //
    // Constants.
    //
    public static final String SUN = "Sun";
    public static final String MON = "Mon";
    public static final String TUE = "Tue";
    public static final String WED = "Wed";
    public static final String THU = "Thu";
    public static final String FRI = "Fri";
    public static final String SAT = "Sat";
    public static final String JAN = "Jan";
    public static final String FEB = "Feb";
    public static final String MAR = "Mar";
    public static final String APR = "Apr";
    public static final String MAY = "May";
    public static final String JUN = "Jun";
    public static final String JUL = "Jul";
    public static final String AUG = "Aug";
    public static final String SEP = "Sep";
    public static final String OCT = "Oct";
    public static final String NOV = "Nov";
    public static final String DEC = "Dec";
    public static final String GMT = "GMT";

    /** 
     * 
     */
    String m_sipWeekDay;

    /**
     *  
     */
    String m_sipMonth;

    /** 
     * The day of the week.
     */
    int m_dayOfWeek = -1;

    /** 
     * The day.
     */
    int m_day = -1;

    /** 
     * The month. 
     */
    int m_month = -1;

    /** 
     * The year.
     */
    int m_year = -1;

    /** 
     * The hour.
     */
    int m_hour = -1;

    /** 
     * The minute.
     */
    int m_minute = -1;

    /** 
     * The second.
     */
    int m_second = -1;

    /**
     * The Java date object.
     */
    Date m_date;

    /** 
     * The Java calendar object.
     */
    Calendar m_calendar;

    /**
    * Construct a new SipDate.  
    */
    public SipDate()
    {}

    /**
     * Construct a Sipdate.
     * 
     * @param time represantation of the date in milliseconds.
     */
    public SipDate(long time)
    {
        this(new Date(time));
    }

    /**
     * Construct a Sipdate.
     * 
     * @param date representation of the date in Java Date object.
     */
    public SipDate(Date date)
    {
        m_date = date;

        m_calendar =
            new GregorianCalendar(
                TimeZone.getTimeZone("GMT"),
                Locale.ENGLISH);
        m_calendar.setTime(m_date);

        m_dayOfWeek = m_calendar.get(Calendar.DAY_OF_WEEK);
        switch (m_dayOfWeek)
        {
            case Calendar.MONDAY :
                m_sipWeekDay = MON;
                break;
            case Calendar.TUESDAY :
                m_sipWeekDay = TUE;
                break;
            case Calendar.WEDNESDAY :
                m_sipWeekDay = WED;
                break;
            case Calendar.THURSDAY :
                m_sipWeekDay = THU;
                break;
            case Calendar.FRIDAY :
                m_sipWeekDay = FRI;
                break;
            case Calendar.SATURDAY :
                m_sipWeekDay = SAT;
                break;
            case Calendar.SUNDAY :
                m_sipWeekDay = SUN;
                break;
            default :
            	m_sipMonth = null;
                throw new RuntimeException("Illegal SipDate day ="+m_dayOfWeek);
        }

        m_day = m_calendar.get(Calendar.DAY_OF_MONTH);
        m_month = m_calendar.get(Calendar.MONTH);
        switch (m_month)
        {
            case Calendar.JANUARY :
                m_sipMonth = JAN;
                break;
            case Calendar.FEBRUARY :
                m_sipMonth = FEB;
                break;
            case Calendar.MARCH :
                m_sipMonth = MAR;
                break;
            case Calendar.APRIL :
                m_sipMonth = APR;
                break;
            case Calendar.MAY :
                m_sipMonth = MAY;
                break;
            case Calendar.JUNE :
                m_sipMonth = JUN;
                break;
            case Calendar.JULY :
                m_sipMonth = JUL;
                break;
            case Calendar.AUGUST :
                m_sipMonth = AUG;
                break;
            case Calendar.SEPTEMBER :
                m_sipMonth = SEP;
                break;
            case Calendar.OCTOBER :
                m_sipMonth = OCT;
                break;
            case Calendar.NOVEMBER :
                m_sipMonth = NOV;
                break;
            case Calendar.DECEMBER :
                m_sipMonth = DEC;
                break;
            default :
            	m_sipMonth = null;
                throw new RuntimeException("Illegal SipDate month ="+m_month);
        }
        m_year = m_calendar.get(Calendar.YEAR);
        m_hour = m_calendar.get(Calendar.HOUR_OF_DAY);
        m_minute = m_calendar.get(Calendar.MINUTE);
        m_second = m_calendar.get(Calendar.SECOND);
    }

    /**
     * Get a canonical string representation of the date.
     */
    public String toString()
    {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
        writeToCharBuffer(buffer);
        
        String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value; 
    }
    
    /**
     * Dump this object to the specified char array
     * @param ret
     */
    public void writeToCharBuffer(CharsBuffer ret)
    {
        if (m_sipWeekDay != null)
        {
            ret.append(m_sipWeekDay);
			ret.append(COMMA);
			ret.append(SP);
        }
        
        ret.append((m_day < 10) ? "0" : "");
        ret.append(m_day);
        ret.append(SP);

        if (m_sipMonth != null)
        {
            ret.append(m_sipMonth);
			ret.append(SP);
        }

        ret.append(m_year);
        ret.append(SP);
        ret.append((m_hour < 10) ? "0" : "");
        ret.append(m_hour);
        ret.append(COLON);
        ret.append((m_minute < 10) ? "0" : "");
        ret.append(m_minute);
        ret.append(COLON);
        ret.append((m_second < 10) ? "0" : "");
        ret.append(m_second);
        ret.append(SP); 
        ret.append(GMT);
    }

    //
    // Accessors.
    //

    /**
     */
    public Calendar getCalendar()
    {
        if (m_calendar == null)
            setJavaCal();
        return m_calendar;
    }

    /**
     * Get the Java Date object.
     * @return
     */
    public Date getDate()
    {
        return m_date;
    }

    /** 
     * Get the WkDay field
     */
    public String getWkday()
    {
        return m_sipWeekDay;
    }

    /** 
     * Get the month
     */
    public String getMonth()
    {
        return m_sipMonth;
    }

    /** 
     * Get the hour
     */
    public int getHour()
    {
        return m_hour;
    }

    /** 
     * Get the minute
     */
    public int getMinute()
    {
        return m_minute;
    }

    /** 
     * Get the second
     */
    public int getSecond()
    {
        return m_second;
    }

    /**
    * Get the year.
    */
    public int getYear()
    {
        return m_year;
    }

    /**
    * convert the SIP Date of this structure to a Java Date.
    * SIP Dates are forced to be GMT. Stores the converted time
    * as a java Calendar class.
    */
    private void setJavaCal()
    {
        /*javaCal =
            new GregorianCalendar(
                TimeZone.getTimeZone("GMT:0"),
                Locale.getDefault());
        if (year != -1)
            javaCal.set(Calendar.YEAR, year);
        if (day != -1)
            javaCal.set(Calendar.DAY_OF_MONTH, day);
        if (month != -1)
            javaCal.set(Calendar.MONTH, month);
        if (wkday != -1)
            javaCal.set(Calendar.DAY_OF_WEEK, wkday);
        if (hour != -1)
            javaCal.set(Calendar.HOUR, hour);
        if (minute != -1)
            javaCal.set(Calendar.MINUTE, minute);
        if (second != -1)
            javaCal.set(Calendar.SECOND, second);
            
            */
    }

    /**
      * Set the wkday member
      * @param w String to set
      * @throws IllegalArgumentException if w is not a valid day.
      */
    public void setWkday(String w) throws IllegalArgumentException
    {
        m_sipWeekDay = w;
        if (m_sipWeekDay.compareToIgnoreCase(MON) == 0)
        {
            m_dayOfWeek = Calendar.MONDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(TUE) == 0)
        {
            m_dayOfWeek = Calendar.TUESDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(WED) == 0)
        {
            m_dayOfWeek = Calendar.WEDNESDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(THU) == 0)
        {
            m_dayOfWeek = Calendar.THURSDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(FRI) == 0)
        {
            m_dayOfWeek = Calendar.FRIDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(SAT) == 0)
        {
            m_dayOfWeek = Calendar.SATURDAY;
        }
        else if (m_sipWeekDay.compareToIgnoreCase(SUN) == 0)
        {
            m_dayOfWeek = Calendar.SUNDAY;
        }
        else
        {
            throw new IllegalArgumentException("Illegal Week day :" + w);
        }
    }

    /**
      * Set the day member
      * @param d int to set
      * @throws IllegalArgumentException if d is not a valid day
      */
    public void setDay(int d) throws IllegalArgumentException
    {
		if (d < 1 || d > 31)
		{
			throw new IllegalArgumentException("Illegal Day of the month " + 
			new Integer(d).toString());
        
		}
         
        m_day = d;
    }

    /**
	  * Set the month member
	  * @param m String to set.
	  * @throws IllegalArgumentException if m is not a valid month
	  */
    public void setMonth(String m) throws IllegalArgumentException
    {
        m_sipMonth = m;
        if (m_sipMonth.compareToIgnoreCase(JAN) == 0)
        {
            m_month = Calendar.JANUARY;
        }
        else if (m_sipMonth.compareToIgnoreCase(FEB) == 0)
        {
            m_month = Calendar.FEBRUARY;
        }
        else if (m_sipMonth.compareToIgnoreCase(MAR) == 0)
        {
            m_month = Calendar.MARCH;
        }
        else if (m_sipMonth.compareToIgnoreCase(APR) == 0)
        {
            m_month = Calendar.APRIL;
        }
        else if (m_sipMonth.compareToIgnoreCase(MAY) == 0)
        {
            m_month = Calendar.MAY;
        }
        else if (m_sipMonth.compareToIgnoreCase(JUN) == 0)
        {
            m_month = Calendar.JUNE;
        }
        else if (m_sipMonth.compareToIgnoreCase(JUL) == 0)
        {
            m_month = Calendar.JULY;
        }
        else if (m_sipMonth.compareToIgnoreCase(AUG) == 0)
        {
            m_month = Calendar.AUGUST;
        }
        else if (m_sipMonth.compareToIgnoreCase(SEP) == 0)
        {
            m_month = Calendar.SEPTEMBER;
        }
        else if (m_sipMonth.compareToIgnoreCase(OCT) == 0)
        {
            m_month = Calendar.OCTOBER;
        }
        else if (m_sipMonth.compareToIgnoreCase(NOV) == 0)
        {
            m_month = Calendar.NOVEMBER;
        }
        else if (m_sipMonth.compareToIgnoreCase(DEC) == 0)
        {
            m_month = Calendar.DECEMBER;
        }
        else
        {
            throw new IllegalArgumentException("Illegal Month :" + m);
        } 
    }

    /**
	 * Set the year member
	 * @param y int to set
	 * @throws IllegalArgumentException if y is not a valid year.
	 */
    public void setYear(int y) throws IllegalArgumentException
    {
        if (y < 0)
        {    
        	throw new IllegalArgumentException("Illegal year : " + y);
        }
        
        m_calendar = null;
        
        m_year = y;
    }

    /**
	  * Set the hour member
	  * @param h int to set
	  * @throws IllegalArgumentException if h is not a valid hour.
	  */
    public void setHour(int h) throws IllegalArgumentException
    {
        if (h < 0 || h > 24)
        {    
        	throw new IllegalArgumentException("Illegal hour : " + h);
        }
        m_calendar = null;
        m_hour = h;
    }

    /**
	  * Set the minute member
	  * @param m int to set
	  * @throws IllegalArgumentException if m is not a valid minute
	  */
    public void setMinute(int m) throws IllegalArgumentException
    {
        if (m < 0 || m >= 60)
        {    
        	throw new IllegalArgumentException("Illegal minute : " + 
        		new Integer(m).toString());
        }
               
        m_calendar = null;
        m_minute = m;
    }

    /**
	  * Set the second member
	  * @param s int to set
	  * @throws IllegalArgumentException if s is not a valid second
	  */
    public void setSecond(int s) throws IllegalArgumentException
    {
        if (s < 0 || s >= 60)
        {
       		throw new IllegalArgumentException("Illegal second : " + 
       			new Integer(s).toString());
        }
        
        m_calendar = null;
        m_second = s;    
    }
}
