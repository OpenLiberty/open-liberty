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
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
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
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("y") || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("1")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    };

    public final static Converter<Integer> INTEGER_CONVERTER = Integer::valueOf;
    public final static Converter<Long> LONG_CONVERTER = Long::valueOf;
    public final static Converter<Short> SHORT_CONVERTER = Short::valueOf;
    public final static Converter<Byte> BYTE_CONVERTER = Byte::valueOf;
    public final static Converter<Double> DOUBLE_CONVERTER = Double::valueOf;
    public final static Converter<Float> FLOAT_CONVERTER = Float::valueOf;

    public final static Converter<BigInteger> BIG_INTEGER_CONVERTER = BigInteger::new;
    public final static Converter<BigDecimal> BIG_DECIMAL_CONVERTER = BigDecimal::new;

    public final static Converter<AtomicInteger> ATOMIC_INTEGER_CONVERTER = s -> new AtomicInteger(Integer.parseInt(s));
    public final static Converter<AtomicLong> ATOMIC_LONG_CONVERTER = s -> new AtomicLong(Long.parseLong(s));

    public final static Converter<Duration> DURATION_CONVERTER = (String value) -> {
        try {
            return Duration.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<Period> PERIOD_CONVERTER = (String value) -> {
        try {
            return Period.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalDateTime> LOCAL_DATE_TIME_CONVERTER = (String value) -> {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalDate> LOCAL_DATE_CONVERTER = (String value) -> {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<LocalTime> LOCAL_TIME_CONVERTER = (String value) -> {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<OffsetDateTime> OFFSET_DATE_TIME_CONVERTER = (String value) -> {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<OffsetTime> OFFSET_TIME_CONVERTER = (String value) -> {
        try {
            return OffsetTime.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<ZonedDateTime> ZONED_DATE_TIME_CONVERTER = (String value) -> {
        try {
            return ZonedDateTime.parse(value);
        } catch (DateTimeException dte) {
            throw new IllegalArgumentException(dte);
        }
    };

    public final static Converter<Instant> INSTANT_CONVERTER = (String value) -> {
        try {
            return Instant.from(OffsetDateTime.parse(value));
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

    public final static Converter<Currency> CURRENCY_CONVERTER = Currency::getInstance;

    public final static Converter<BitSet> BIT_SET_CONVERTER = v -> BitSet.valueOf(DatatypeConverter.parseHexBinary(v));

    /**
     * Convert the string to a URI or throw ConvertException if unable to convert
     *
     */
    public final static Converter<URI> URI_CONVERTER = (String value) -> {
        URI uri = null;
        try {
            uri = new URI(value);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
        return uri;
    };

    public final static Converter<ChronoUnit> CHRONO_UNIT_CONVERTER = ChronoUnit::valueOf;

    /**
     * Convert the string to a URL or throw ConvertException if unable to convert
     */
    public final static Converter<URL> URL_CONVERTER = (String value) -> {
        URL url = null;
        try {
            url = new URL(value);
        } catch (MalformedURLException mfue) {
            throw new IllegalArgumentException(mfue);
        }
        return url;
    };

    private static Map<Type, Converter<?>> defaultConverters = new HashMap<Type, Converter<?>>();

    static {
        defaultConverters.put(String.class, STRING_CONVERTER);

        defaultConverters.put(Boolean.class, BOOLEAN_CONVERTER);
        defaultConverters.put(boolean.class, BOOLEAN_CONVERTER);

        defaultConverters.put(Integer.class, INTEGER_CONVERTER);
        defaultConverters.put(int.class, INTEGER_CONVERTER);

        defaultConverters.put(Long.class, LONG_CONVERTER);
        defaultConverters.put(long.class, LONG_CONVERTER);

        defaultConverters.put(Short.class, SHORT_CONVERTER);
        defaultConverters.put(short.class, SHORT_CONVERTER);

        defaultConverters.put(Byte.class, BYTE_CONVERTER);
        defaultConverters.put(byte.class, BYTE_CONVERTER);

        defaultConverters.put(Double.class, DOUBLE_CONVERTER);
        defaultConverters.put(double.class, DOUBLE_CONVERTER);

        defaultConverters.put(Float.class, FLOAT_CONVERTER);
        defaultConverters.put(float.class, FLOAT_CONVERTER);

        defaultConverters.put(BigInteger.class, BIG_INTEGER_CONVERTER);
        defaultConverters.put(BigDecimal.class, BIG_DECIMAL_CONVERTER);

        defaultConverters.put(AtomicInteger.class, ATOMIC_INTEGER_CONVERTER);
        defaultConverters.put(AtomicLong.class, ATOMIC_LONG_CONVERTER);

        defaultConverters.put(Duration.class, DURATION_CONVERTER);
        defaultConverters.put(Period.class, PERIOD_CONVERTER);

        defaultConverters.put(LocalDateTime.class, LOCAL_DATE_TIME_CONVERTER);
        defaultConverters.put(LocalDate.class, LOCAL_DATE_CONVERTER);
        defaultConverters.put(LocalTime.class, LOCAL_TIME_CONVERTER);

        defaultConverters.put(OffsetDateTime.class, OFFSET_DATE_TIME_CONVERTER);
        defaultConverters.put(OffsetTime.class, OFFSET_TIME_CONVERTER);
        defaultConverters.put(ZonedDateTime.class, ZONED_DATE_TIME_CONVERTER);

        defaultConverters.put(Instant.class, INSTANT_CONVERTER);

        defaultConverters.put(Currency.class, CURRENCY_CONVERTER);
        defaultConverters.put(BitSet.class, BIT_SET_CONVERTER);

        defaultConverters.put(URL.class, URL_CONVERTER);
        defaultConverters.put(URI.class, URI_CONVERTER);

        defaultConverters.put(ChronoUnit.class, CHRONO_UNIT_CONVERTER);

        defaultConverters = Collections.unmodifiableMap(defaultConverters);

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
    public static Map<Type, Converter<?>> getDefaultConverters() {
        return defaultConverters;
    }

    /**
     * @return discoveredInventors
     */
    public static Map<Type, Converter<?>> getDiscoveredConverters(ClassLoader classLoader) {
        Map<Type, Converter<?>> discoveredConverters = new HashMap<Type, Converter<?>>();

        //load config sources using the service loader
        try {
            @SuppressWarnings("rawtypes")
            ServiceLoader<Converter> sl = ServiceLoader.load(Converter.class, classLoader);
            for (Converter<?> converter : sl) {
                Type converterType = getConverterType(converter);
                discoveredConverters.put(converterType, converter);
            }
        } catch (ServiceConfigurationError e) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.discover.converters.CWMCG0012E", e), e);
        }

        return discoveredConverters;
    }

    public static Map<Type, Converter<?>> getConverters(ClassLoader classLoader) {
        Map<Type, Converter<?>> converters = new HashMap<Type, Converter<?>>();
        converters.putAll(getDefaultConverters());
        converters.putAll(getDiscoveredConverters(classLoader));
        return converters;
    }

}
