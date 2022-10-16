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
package io.openliberty.checkpoint.internal.beta;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.condition.Condition;

@Component(property = { Condition.CONDITION_ID + "=io.openliberty.checkpoint.feature" })
public class CheckpointFeatureCondition implements Condition {

}
