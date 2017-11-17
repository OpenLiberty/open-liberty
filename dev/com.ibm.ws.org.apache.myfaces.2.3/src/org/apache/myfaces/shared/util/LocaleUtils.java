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
package org.apache.myfaces.shared.util;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class LocaleUtils
{
    //private static final Log log = LogFactory.getLog(LocaleUtils.class);
    private static final Logger log = Logger.getLogger(LocaleUtils.class.getName());

    /** Utility class, do not instatiate */
    private LocaleUtils()
    {
        // utility class, do not instantiate
    }

    /**
     * Converts a locale string to <code>Locale</code> class. Accepts both
     * '_' and '-' as separators for locale components.
     *
     * @param localeString string representation of a locale
     * @return Locale instance, compatible with the string representation
     */
    public static Locale toLocale(String localeString)
    {
        if ((localeString == null) || (localeString.length() == 0))
        {
            Locale locale = Locale.getDefault();
            if(log.isLoggable(Level.WARNING))
            {
                log.warning("Locale name in faces-config.xml null or empty, setting locale to default locale : "
                        + locale.toString());
            }
            return locale;
        }

        int separatorCountry = localeString.indexOf('_');
        char separator;
        if (separatorCountry >= 0)
        {
            separator = '_';
        }
        else
        {
            separatorCountry = localeString.indexOf('-');
            separator = '-';
        }

        String language;
        String country;
        String variant;
        if (separatorCountry < 0)
        {
            language = localeString;
            country = "";
            variant = "";
        }
        else
        {
            language = localeString.substring(0, separatorCountry);

            int separatorVariant = localeString.indexOf(separator, separatorCountry + 1);
            if (separatorVariant < 0)
            {
                country = localeString.substring(separatorCountry + 1);
                variant = "";
            }
            else
            {
                country = localeString.substring(separatorCountry + 1, separatorVariant);
                variant = localeString.substring(separatorVariant + 1);
            }
        }

        return new Locale(language, country, variant);
    }


    /**
     * Convert locale string used by converter tags to locale.
     *
     * @param name name of the locale
     * @return locale specified by the given String
     */
    public static Locale converterTagLocaleFromString(String name)
    {
        try
        {
            Locale locale;
            StringTokenizer st = new StringTokenizer(name, "_");
            String language = st.nextToken();

            if(st.hasMoreTokens())
            {
                String country = st.nextToken();

                if(st.hasMoreTokens())
                {
                    String variant = st.nextToken();
                    locale = new Locale(language, country, variant);
                }
                else
                {
                    locale = new Locale(language, country);
                }
            }
            else
            {
                locale = new Locale(language);
            }


            return locale;
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("Locale parsing exception - " +
                "invalid string representation '" + name + "'");
        }
    }
}
