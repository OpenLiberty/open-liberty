/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Showtime {

    @Id
    @GeneratedValue
    public Integer id;

    public String movie;

    public LocalDateTime startTime;

    public LocalDateTime endTime;

    public Showtime() {
    }

    public Showtime(Integer id, String movie, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.movie = movie;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Showtime of(String movie, LocalDateTime startTime, LocalDateTime endTime) {
        Showtime inst = new Showtime();
        inst.movie = movie;
        inst.startTime = startTime;
        inst.endTime = endTime;
        return inst;
    }
}
