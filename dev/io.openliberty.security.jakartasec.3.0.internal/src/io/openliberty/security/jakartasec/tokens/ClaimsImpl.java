/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.tokens;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.security.enterprise.identitystore.openid.Claims;

public class ClaimsImpl implements Claims, Serializable {

    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(ClaimsImpl.class);

    private final Map<String, Object> claims = Collections.synchronizedMap(new HashMap<>());

    public ClaimsImpl(Map<String, Object> claimsMap) {
        claims.putAll(claimsMap);
    }

    @Override
    public Optional<String> getStringClaim(String name) {
        String value = getClaim(name);

        if (value != null) {
            return Optional.of(value);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Instant> getNumericDateClaim(String name) {
        try {
            Long value = (Long) claims.get(name);

            if (value != null) {
                Instant instant = Instant.ofEpochSecond(value);
                return Optional.of(instant);
            }

            return Optional.empty();
        } catch (ClassCastException | DateTimeException e) {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "NumericDate", e.toString() });
            throw new IllegalArgumentException(msg, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getArrayStringClaim(String name) {
        List<String> result;

        Object value = claims.get(name);

        if (value == null) {
            result = Collections.emptyList();
        } else if (value instanceof String) {
            result = new ArrayList<String>();
            result.add((String) value);
        } else if (value instanceof List) {
            // Copy contents to prevent modifications.
            result = new ArrayList<String>();
            result.addAll((List<String>) value);
        } else {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "ArrayString", "Type is not String or List" });
            throw new IllegalArgumentException(msg);
        }

        return result;
    }

    @Override
    public OptionalInt getIntClaim(String name) {
        try {
            Long value = (Long) claims.get(name);

            if (value != null) {
                return OptionalInt.of(value.intValue());
            }

            return OptionalInt.empty();
        } catch (ClassCastException e) {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "Integer", e.toString() });
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public OptionalLong getLongClaim(String name) {
        try {
            Long value = (Long) claims.get(name);

            if (value != null) {
                return OptionalLong.of(value);
            }

            return OptionalLong.empty();
        } catch (ClassCastException e) {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "Long", e.toString() });
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public OptionalDouble getDoubleClaim(String name) {
        try {
            Double value = (Double) claims.get(name);

            if (value != null) {
                return OptionalDouble.of(value);
            }

            return OptionalDouble.empty();
        } catch (ClassCastException e) {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "Double", e.toString() });
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public Optional<Claims> getNested(String name) {
        Optional<Claims> result;

        Object value = claims.get(name);

        if (value == null) {
            result = Optional.empty();
        } else if (value instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Claims nestedClaims = new ClaimsImpl((Map<String, Object>) value);
                result = Optional.of(nestedClaims);
            } catch (Exception e) {
                String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, value, "Nested", e.toString() });
                throw new IllegalArgumentException(msg, e);
            }
        } else {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, value, "Nested", "Type is not Map" });
            throw new IllegalArgumentException(msg);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T getClaim(String name) {
        try {
            if (claims.containsKey(name)) {
                return (T) claims.get(name);
            }
        } catch (ClassCastException e) {
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CLAIMS_PROCESSING_ERROR", new Object[] { name, claims.get(name), "Generic Type", e.toString() });
            throw new IllegalArgumentException(msg, e);
        }

        return null;
    }

}
