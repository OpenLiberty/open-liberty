/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.springboot.support.version40.test.validate.app;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestController
@RequestMapping("/")
public class TestValidate {

    @GetMapping("/name-for-day")
    public String getAppProperty(@RequestParam("dayOfWeek") @Min(1) @Max(7) Integer dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> {
                yield "Monday";
            }
            case 2 -> {
                yield "Tuesday";
            }
            case 3 -> {
                yield "Wednesday";
            }
            case 4 -> {
                yield "Thursday";
            }
            case 5 -> {
                yield "Friday";
            }
            case 6 -> {
                yield "Saturday";
            }
            case 7 -> {
                yield "Sunday";
            }
            default -> {
                yield "Unknown";
            }
        };
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public String handleValidationExceptions(
                                             HandlerMethodValidationException ex) {
        StringBuffer errors = new StringBuffer();
        ex.getAllErrors().forEach(error -> {
            errors.append(error.getDefaultMessage());
        });
        return errors.toString();
    }
}
