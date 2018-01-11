/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//This class was inspired by com.netflix.archaius.DefaultDecoder

package com.ibm.ws.microprofile.config.interfaces;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Currency;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * The helper class returns all the built-in converters.
 *
 */
public class DefaultConverters {

    private static final TraceComponent tc = Tr.register(DefaultConverters.class);

    public final static Converter<String> STRING_CONVERTER = v -> v;

    public final static Converter<Boolean> BOOLEAN_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("y") || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("1")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    };

    public final static Converter<Integer> INTEGER_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Integer.valueOf(v);
    };
    public final static Converter<Long> LONG_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Long.valueOf(v);
    };
    public final static Converter<Short> SHORT_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Short.valueOf(v);
    };
    public final static Converter<Byte> BYTE_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Byte.valueOf(v);
    };
    public final static Converter<Double> DOUBLE_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Double.valueOf(v);
    };
    public final static Converter<Float> FLOAT_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Float.valueOf(v);
    };

    public final static Converter<BigInteger> BIG_INTEGER_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return new BigInteger(v);
    };
    public final static Converter<BigDecimal> BIG_DECIMAL_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return new BigDecimal(v);
    };

    public final static Converter<AtomicInteger> ATOMIC_INTEGER_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return new AtomicInteger(Integer.parseInt(v));
    };
    public final static Converter<AtomicLong> ATOMIC_LONG_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return new AtomicLong(Long.parseLong(v));
    };

    public final static Converter<Duration> DURATION_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return Duration.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<Period> PERIOD_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return Period.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalDateTime> LOCAL_DATE_TIME_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalDate> LOCAL_DATE_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalTime> LOCAL_TIME_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return LocalTime.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<OffsetDateTime> OFFSET_DATE_TIME_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<OffsetTime> OFFSET_TIME_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return OffsetTime.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<ZonedDateTime> ZONED_DATE_TIME_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(v);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<Instant> INSTANT_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        try {
            return Instant.from(OffsetDateTime.parse(v));
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    @FFDCIgnore(DateTimeParseException.class)
    private final static Instant parseZonedDateTime(String value) {
        Instant instant = null;
        try {
            instant = ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant();
        } catch (DateTimeParseException e) {
            //ignore
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
        return instant;
    }

    @FFDCIgnore(DateTimeParseException.class)
    private final static Instant parseLocalDateTime(String value) {
        Instant instant = null;
        try {
            instant = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            //ignore
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
        return instant;
    }

    private final static Instant parseLocalDate(String value) {
        Instant instant = null;
        try {
            instant = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
        return instant;
    }

    public final static Converter<Currency> CURRENCY_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return Currency.getInstance(v);
    };

    public final static Converter<BitSet> BIT_SET_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return BitSet.valueOf(DatatypeConverter.parseHexBinary(v));
    };

    /**
     * Convert the string to a URI or throw ConvertException if unable to convert
     *
     */
    public final static Converter<URI> URI_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        URI uri = null;
        try {
            uri = new URI(v);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
        return uri;
    };

    public final static Converter<ChronoUnit> CHRONO_UNIT_CONVERTER = v -> {
        if (v == null) {
            return null;
        }
        return ChronoUnit.valueOf(v);
    };

    /**
     * Convert the string to a URL or throw ConvertException if unable to convert
     */
    public final static Converter<URL> URL_CONVERTER = (String v) -> {
        if (v == null) {
            return null;
        }
        URL url = null;
        try {
            url = new URL(v);
        } catch (MalformedURLException mfue) {
            throw new IllegalArgumentException(mfue);
        }
        return url;
    };

    private static PriorityConverterMap defaultConverters = new PriorityConverterMap();

    static {
        defaultConverters.addConverter(String.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, STRING_CONVERTER);

        defaultConverters.addConverter(Boolean.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BOOLEAN_CONVERTER);
        defaultConverters.addConverter(boolean.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BOOLEAN_CONVERTER);

        defaultConverters.addConverter(Integer.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, INTEGER_CONVERTER);
        defaultConverters.addConverter(int.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, INTEGER_CONVERTER);

        defaultConverters.addConverter(Long.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, LONG_CONVERTER);
        defaultConverters.addConverter(long.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, LONG_CONVERTER);

        defaultConverters.addConverter(Short.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, SHORT_CONVERTER);
        defaultConverters.addConverter(short.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, SHORT_CONVERTER);

        defaultConverters.addConverter(Byte.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BYTE_CONVERTER);
        defaultConverters.addConverter(byte.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BYTE_CONVERTER);

        defaultConverters.addConverter(Double.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, DOUBLE_CONVERTER);
        defaultConverters.addConverter(double.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, DOUBLE_CONVERTER);

        defaultConverters.addConverter(Float.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, FLOAT_CONVERTER);
        defaultConverters.addConverter(float.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, FLOAT_CONVERTER);

        defaultConverters.addConverter(BigInteger.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BIG_INTEGER_CONVERTER);
        defaultConverters.addConverter(BigDecimal.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BIG_DECIMAL_CONVERTER);

        defaultConverters.addConverter(AtomicInteger.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, ATOMIC_INTEGER_CONVERTER);
        defaultConverters.addConverter(AtomicLong.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, ATOMIC_LONG_CONVERTER);

        defaultConverters.addConverter(Duration.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, DURATION_CONVERTER);
        defaultConverters.addConverter(Period.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, PERIOD_CONVERTER);

        defaultConverters.addConverter(LocalDateTime.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, LOCAL_DATE_TIME_CONVERTER);
        defaultConverters.addConverter(LocalDate.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, LOCAL_DATE_CONVERTER);
        defaultConverters.addConverter(LocalTime.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, LOCAL_TIME_CONVERTER);

        defaultConverters.addConverter(OffsetDateTime.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, OFFSET_DATE_TIME_CONVERTER);
        defaultConverters.addConverter(OffsetTime.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, OFFSET_TIME_CONVERTER);
        defaultConverters.addConverter(ZonedDateTime.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, ZONED_DATE_TIME_CONVERTER);

        defaultConverters.addConverter(Instant.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, INSTANT_CONVERTER);

        defaultConverters.addConverter(Currency.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, CURRENCY_CONVERTER);
        defaultConverters.addConverter(BitSet.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, BIT_SET_CONVERTER);

        defaultConverters.addConverter(URL.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, URL_CONVERTER);
        defaultConverters.addConverter(URI.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, URI_CONVERTER);

        defaultConverters.addConverter(ChronoUnit.class, ConfigConstants.BUILTIN_CONVERTER_PRIORITY, CHRONO_UNIT_CONVERTER);

        defaultConverters.setUnmodifiable();

    }

    /**
     * Get the Type of a Converter object
     *
     * @param converter
     * @return
     */
    public static Type getConverterType(Converter<?> converter) {
        Type type = null;

        Type[] itypes = converter.getClass().getGenericInterfaces();
        for (Type itype : itypes) {
            ParameterizedType ptype = (ParameterizedType) itype;
            if (ptype.getRawType() == Converter.class) {
                Type[] atypes = ptype.getActualTypeArguments();
                if (atypes.length == 1) {
                    type = atypes[0];
                    break;
                } else {
                    throw new ConfigException(Tr.formatMessage(tc, "unable.to.determine.conversion.type.CWMCG0009E", converter.getClass().getName()));
                }
            }
        }
        if (type == null) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.determine.conversion.type.CWMCG0009E", converter.getClass().getName()));
        }

        return type;
    }

    /**
     * @return defaultConverters
     */
    public static PriorityConverterMap getDefaultConverters() {
        return defaultConverters;
    }

    /**
     * @return discoveredInventors
     */
    public static PriorityConverterMap getDiscoveredConverters(ClassLoader classLoader) {
        PriorityConverterMap discoveredConverters = new PriorityConverterMap();

        //load config sources using the service loader
        try {
            @SuppressWarnings("rawtypes")
            ServiceLoader<Converter> sl = ServiceLoader.load(Converter.class, classLoader);
            for (Converter<?> converter : sl) {
                discoveredConverters.addConverter(converter);
            }
        } catch (ServiceConfigurationError e) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.discover.converters.CWMCG0012E", e), e);
        }

        return discoveredConverters;
    }

    public static PriorityConverterMap getConverters(ClassLoader classLoader) {
        PriorityConverterMap converters = new PriorityConverterMap();
        converters.addAll(getDefaultConverters());
        converters.addAll(getDiscoveredConverters(classLoader));
        return converters;
    }

}
