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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt;

import java.util.List;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.common.jwt.exceptions.JwtContextMissingJoseObjects;

public class JwtParsingUtils {

    private static final TraceComponent tc = Tr.register(JwtParsingUtils.class);

    public static JwtContext parseJwtWithoutValidation(String jwtString) throws InvalidJwtException {
        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder().setSkipAllValidators().setDisableRequireSignature().setSkipSignatureVerification().build();
        return firstPassJwtConsumer.process(jwtString);
    }

    public static JsonWebStructure getJsonWebStructureFromJwtContext(JwtContext jwtContext) throws JwtContextMissingJoseObjects {
        List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();
        if (jsonStructures == null || jsonStructures.isEmpty()) {
            throw new JwtContextMissingJoseObjects();
        }
        JsonWebStructure jsonStruct = jsonStructures.get(0);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonWebStructure class: " + jsonStruct.getClass().getName() + " data:" + jsonStruct);
            if (jsonStruct instanceof JsonWebSignature) {
                JsonWebSignature signature = (JsonWebSignature) jsonStruct;
                Tr.debug(tc, "JsonWebSignature alg: " + signature.getAlgorithmHeaderValue() + " 3rd:'" + signature.getEncodedSignature() + "'");
            }
        }
        return jsonStruct;
    }

    public static JwtClaims parseJwtWithValidation(String jwtString, JwtConsumer jwtConsumer) throws InvalidJwtException {
        JwtContext validatedJwtContext = jwtConsumer.process(jwtString);
        return validatedJwtContext.getJwtClaims();
    }

}
