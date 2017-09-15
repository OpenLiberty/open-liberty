/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.ibm.ws.logging.hpel.FormatSet;

/**
 *
 */
public class LocaleUtils {

    private static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * This utility method is used to get the Max date based on the current Locale.
     *
     * @param string
     *
     * @return
     * @throws ParseException
     */
    public static String getLocaleBasedMaxDate() throws ParseException {

        DateFormat zoneFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
        Calendar cal = zoneFormat.getCalendar();
        String timeZone = cal.getTimeZone().getDisplayName(false, TimeZone.SHORT);
        System.out.println(" Timezone : " + timeZone);
        String currentLocale = Locale.getDefault().toString();

        // In java 9 the default locale changes to have a comma ',' separate the date and time
        String dateSep = System.getProperty("java.specification.version").startsWith("1.") ? "" : ",";

        if (currentLocale.equalsIgnoreCase("en_US") && isMac(OS)) {
            return "24/02/13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("en_US")) {
            return "2/24/13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("cs_CZ")) {
            return "24.2.13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("de_DE")) {
            return "24.02.13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("es_ES")) {
            return "24/02/13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("fr_FR")) {
            return "24/02/13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("hu_HU")) {
            return "2013.02.24." + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("it_IT")) {
            return "24/02/13" + dateSep + " 23.59.59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ja_JP")) {
            return "13/02/24" + dateSep + " 23:59:59:999 " + timeZone;
        } else if (currentLocale.equalsIgnoreCase("ko_KR")) {
            return "13. 2. 24" + dateSep + "   23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("pl_PL")) {
            return "24.02.13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("pt_BR")) {
            return "24/02/13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ro_RO")) {
            return "24.02.2013" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ru_RU")) {
            return "24.02.13" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("zh")) {
            return "13-2-24" + dateSep + " 23:59:59:999 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("zh_TW")) {
            return "2013/2/24" + dateSep + "   23:59:59:999 " + timeZone;
        } else {

            DateFormat dateTimeFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
            dateTimeFormat.setLenient(false);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy" + dateSep + " HH:mm:ss:SSS");
            sdf.setLenient(false);
            Date dt = sdf.parse("24/2/2013" + dateSep + " 23:59:59:999");
            return dateTimeFormat.format(dt);

        }

    }

    /**
     * This utility method is used to get the minDate based on current locale
     *
     * @throws ParseException
     */
    public static String getLocaleBasedMinDate() throws ParseException {

        DateFormat zoneFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
        Calendar cal = zoneFormat.getCalendar();
        String timeZone = cal.getTimeZone().getDisplayName(false, TimeZone.SHORT);
        System.out.println(" Timezone : " + timeZone);
        String currentLocale = Locale.getDefault().toString();

        // In java 9 the default locale changes to have a comma ',' separate the date and time
        String dateSep = System.getProperty("java.specification.version").startsWith("1.") ? "" : ",";

        if (currentLocale.equalsIgnoreCase("en_US") && isMac(OS)) {
            return "24/02/13" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("en_US")) {
            return "2/24/13" + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("cs_CZ")) {
            return "24.2.13" + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("de_DE")) {
            return "24.02.13" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("es_ES")) {
            return "24/02/13" + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("fr_FR")) {
            return "24/02/13" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("hu_HU")) {
            return "2013.02.24." + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("it_IT")) {
            return "24/02/13" + dateSep + " 0.00.00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ja_JP")) {
            return "13/02/24" + dateSep + " 0:00:00:000 " + timeZone;
        } else if (currentLocale.equals("ko_KR")) {
            return "13. 2. 24" + dateSep + "   0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("pl_PL")) {
            return "24.02.13" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("pt_BR")) {
            return "24/02/13" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ro_RO")) {
            return "24.02.2013" + dateSep + " 00:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("ru_RU")) {
            return "24.02.13" + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("zh")) {
            return "13-2-24" + dateSep + " 0:00:00:000 " + timeZone;

        } else if (currentLocale.equalsIgnoreCase("zh_TW")) {
            return "2013/2/24" + dateSep + "   00:00:00:000 " + timeZone;
        } else {
            DateFormat dateTimeFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
            dateTimeFormat.setLenient(false);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy" + dateSep + " HH:mm:ss:SSS");
            sdf.setLenient(false);

            Date dt = sdf.parse("24/2/2013" + dateSep + " 00:00:00:000");
            return dateTimeFormat.format(dt);

        }

    }

    /**
     * This utility method is used to create a date String in proper format based on the Locale so that it can be parsed using DateFormat.
     *
     * @param string
     * @return
     */
    public static String getTestDate(String testDateStr) {

        String currentLocale = Locale.getDefault().toString();
        String datePattern = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
        String arr[] = null;
        String dateSeparator = null;

        if (datePattern.contains("/")) {
            arr = datePattern.split("/");
            dateSeparator = "/";
        } else if (datePattern.contains(".")) {
            arr = datePattern.split("\\.");
            dateSeparator = ".";
        } else if (datePattern.contains("-")) {
            arr = datePattern.split("-");
            dateSeparator = "-";
        }

        for (int i = 0; i < arr.length; i++) {

            if (arr[i].contains("d") || arr[i].contains("D"))
                arr[i] = testDateStr.split("-")[0];
            else if (arr[i].contains("m") || arr[i].contains("M"))
                arr[i] = testDateStr.split("-")[1];
            else if (arr[i].contains("y") || arr[i].contains("Y"))
                arr[i] = testDateStr.split("-")[2];

        }

        String dateStr = null;

        if (currentLocale.equalsIgnoreCase("ko") || currentLocale.equalsIgnoreCase("ko_kr")) {

            dateStr = arr[0] + dateSeparator + " " + arr[1] + dateSeparator + " " + arr[2];

        } else if (currentLocale.equalsIgnoreCase("hr_hr") || currentLocale.equalsIgnoreCase("hu_HU_PREEURO") || currentLocale.equalsIgnoreCase("sr_CS")
                   || currentLocale.equalsIgnoreCase("sr_RS_Latn")
                   || currentLocale.equalsIgnoreCase("sr_RS") || currentLocale.equalsIgnoreCase("hu_HU") || currentLocale.equalsIgnoreCase("sr_ME")
                   || currentLocale.equalsIgnoreCase("sr_RS_Cyrl")
                   || currentLocale.equalsIgnoreCase("hu_HU_EURO") || currentLocale.equalsIgnoreCase("sr") || currentLocale.equalsIgnoreCase("hu")) {

            dateStr = arr[0] + dateSeparator + arr[1] + dateSeparator + arr[2] + ".";

        } else if (currentLocale.equalsIgnoreCase("hr_hr")) {

            dateStr = "H" + arr[0] + dateSeparator + arr[1] + dateSeparator + arr[2];

        } else {

            dateStr = arr[0] + dateSeparator + arr[1] + dateSeparator + arr[2];
        }
        return dateStr;
    }

    /**
     * This utility method is used to return the time in proper format based on current Locale
     *
     * @param isBaseStartTime
     * @return
     */
    public static String getTestTime(boolean isDefaultMinTime) {

        String currentLocale = Locale.getDefault().toString();
        String datePattern = ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.MEDIUM)).toPattern();
//        String timeZone = new SimpleDateFormat("z").format(new Date());

        DateFormat zoneFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
        Calendar cal = zoneFormat.getCalendar();
        String timeZone = cal.getTimeZone().getDisplayName(false, TimeZone.SHORT);

        String time = null;
        if (currentLocale.equalsIgnoreCase("sq") || currentLocale.equalsIgnoreCase("mk") || currentLocale.equalsIgnoreCase("sq_AL")) {
            if (!isDefaultMinTime)
                time = "11:12:13:999 " + timeZone + ".";
            else {
                if (datePattern.contains("HH") || datePattern.contains("hh")) {
                    time = "00:00:00:000 " + timeZone + ".";
                } else {
                    time = "0:00:00:000 " + timeZone + ".";
                }
            }
        } else if (datePattern.contains(".")) {
            if (!isDefaultMinTime)
                time = "11.12.13:999 " + timeZone;
            else {
                if (datePattern.contains("HH") || datePattern.contains("hh")) {
                    time = "00.00.00:000 " + timeZone;
                } else {
                    time = "0.00.00:000 " + timeZone;
                }
            }
        } else if (datePattern.contains(":")) {
            if (!isDefaultMinTime)
                time = "11:12:13:999 " + timeZone;
            else {
                if (datePattern.contains("HH") || datePattern.contains("hh")) {
                    time = "00:00:00:000 " + timeZone;
                } else {
                    time = "0:00:00:000 " + timeZone;
                }
            }
        }
        return time;
    }

    public static String getTestDateTime(String dateStr, boolean isDefaultMinTime) {

        // In java 9 the default locale changes to have a comma ',' separate the date and time
        String dateSep = System.getProperty("java.specification.version").startsWith("1.") ? " " : ", ";

        String currentLocale = Locale.getDefault().toString();
        if (currentLocale.equalsIgnoreCase("th") || currentLocale.equalsIgnoreCase("th_th")) {
            return getTestDate(dateStr) + ", " + getTestTime(isDefaultMinTime);
        } else if (currentLocale.equalsIgnoreCase("iw") || currentLocale.equalsIgnoreCase("vi")
                   || currentLocale.equalsIgnoreCase("iw_IL") || currentLocale.equalsIgnoreCase("vi_vn")) {
            return getTestTime(isDefaultMinTime) + dateSep + getTestDate(dateStr);

        }

        else {
            return getTestDate(dateStr) + dateSep + getTestTime(isDefaultMinTime);
        }
    }

    /**
     * @return
     */
    public static boolean isLocaleSkippedForTimePatternTesting() {

        String currentLocale = Locale.getDefault().toString();
        if (currentLocale.equalsIgnoreCase("ko") || currentLocale.equalsIgnoreCase("th_th") ||
            currentLocale.equalsIgnoreCase("zh_tw") || currentLocale.equalsIgnoreCase("zh_hk")
            || currentLocale.equalsIgnoreCase("ko_kr") || currentLocale.equalsIgnoreCase("mk")
            || currentLocale.equalsIgnoreCase("th_TH_TH") || currentLocale.equalsIgnoreCase("mk_mk")
            || currentLocale.equalsIgnoreCase("gu_IN") || currentLocale.equalsIgnoreCase("gu")
            || currentLocale.equalsIgnoreCase("hi_IN") || currentLocale.equalsIgnoreCase("pa_IN")
            || currentLocale.equalsIgnoreCase("mr_IN") || currentLocale.equalsIgnoreCase("kn_IN")
            || currentLocale.equalsIgnoreCase("pa") || currentLocale.equalsIgnoreCase("mr")
            || currentLocale.equalsIgnoreCase("kn") || currentLocale.equalsIgnoreCase("zh_SG"))
            return false;
        else

            return true;
    }

    private static boolean isMac(String OS) {
        String datePattern = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
        if (OS.indexOf("mac") >= 0)
            if (datePattern.startsWith("d") || datePattern.startsWith("D"))
                return true;
        return false;
    }
}
