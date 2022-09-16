/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import jakarta.security.enterprise.identitystore.openid.Claims;

public class ClaimsImpl implements Claims, Serializable {

    private static final long serialVersionUID = 1L;
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException(e.getMessage());
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException();
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException(e.getMessage());
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException(e.getMessage());
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException(e.getMessage());
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
                // TODO: Determine if a translated message is needed.
                throw new IllegalArgumentException();
            }
        } else {
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException();
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
            // TODO: Determine if a translated message is needed.
            throw new IllegalArgumentException();
        }

        return null;
    }

}
