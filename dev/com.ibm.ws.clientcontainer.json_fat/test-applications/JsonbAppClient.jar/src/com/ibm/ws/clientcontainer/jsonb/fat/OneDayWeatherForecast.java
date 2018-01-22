/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.jsonb.fat;

import java.time.LocalDate;

/**
 * Example Java object that can be converted to/from JSON.
 */
public class OneDayWeatherForecast {
    public static class HourlyWeatherForecast {
        public short temp;
        public short windchill;
        public float chanceOfSnow;
    }
    public LocalDate date;
    public Location location;
    public HourlyWeatherForecast[] hourlyForecast = new HourlyWeatherForecast[24];
}
