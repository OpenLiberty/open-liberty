/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package val31.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 *
 */
public record SignupForm(@NotBlank(message = "Name cannot be blank", groups = FirstGroup.class) String firstName,

                @Min(value = 18, message = "Age must be at least 18", groups = SecondGroup.class) int age) {

}
