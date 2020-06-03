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

package com.ibm.ws.microprofile.config.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.config.internal.common.ConfigException;

/**
 * The helper class returns all the built-in converters.
 *
 */
public class DefaultConverters {

    private static final TraceComponent tc = Tr.register(DefaultConverters.class);

    private static final PriorityConverterMap defaultConverters = new PriorityConverterMap();

    static {
        defaultConverters.addConverter(new IdentityConverter()); // v -> v

        defaultConverters.addConverter(new OptionalConverter());

        defaultConverters.addConverter(new BooleanConverter());
        defaultConverters.addConverter(new AutomaticConverter(Integer.class));
        defaultConverters.addConverter(new AutomaticConverter(Long.class));
        defaultConverters.addConverter(new AutomaticConverter(Short.class));
        defaultConverters.addConverter(new AutomaticConverter(Byte.class));
        defaultConverters.addConverter(new AutomaticConverter(Double.class));
        defaultConverters.addConverter(new AutomaticConverter(Float.class));

        defaultConverters.addConverter(new AutomaticConverter(BigInteger.class));
        defaultConverters.addConverter(new AutomaticConverter(BigDecimal.class));

        defaultConverters.addConverter(new AtomicIntegerConverter());
        defaultConverters.addConverter(new AtomicLongConverter());

        defaultConverters.addConverter(new DateTimeConverter(Duration.class));
        defaultConverters.addConverter(new DateTimeConverter(Period.class));

        defaultConverters.addConverter(new DateTimeConverter(LocalDateTime.class));
        defaultConverters.addConverter(new DateTimeConverter(LocalDate.class));
        defaultConverters.addConverter(new DateTimeConverter(LocalTime.class));

        defaultConverters.addConverter(new DateTimeConverter(OffsetDateTime.class));
        defaultConverters.addConverter(new DateTimeConverter(OffsetTime.class));
        defaultConverters.addConverter(new DateTimeConverter(ZonedDateTime.class));

        defaultConverters.addConverter(new InstantConverter());
        defaultConverters.addConverter(new CurrencyConverter());
        defaultConverters.addConverter(new BitSetConverter());

        defaultConverters.addConverter(new URIConverter());
        defaultConverters.addConverter(new URLConverter());

        defaultConverters.addConverter(new AutomaticConverter(ChronoUnit.class));

        defaultConverters.setUnmodifiable();

    }

    /**
     * @return defaultConverters
     */
    @Trivial
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
                discoveredConverters.addConverter(UserConverter.newInstance(converter));
            }
        } catch (ServiceConfigurationError e) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.discover.converters.CWMCG0012E", e), e);
        }

        return discoveredConverters;
    }
}
