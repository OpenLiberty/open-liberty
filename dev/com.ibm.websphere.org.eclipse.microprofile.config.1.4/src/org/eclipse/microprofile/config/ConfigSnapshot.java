/*
 *******************************************************************************
 * Copyright (c) 2011-2018 Contributors to the Eclipse Foundation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   2018-04-04 - Mark Struberg, Manfred Huber, Alex Falb, Gerhard Petracek
 *      Initially authored in Apache DeltaSpike as ConfigSnapshot fdd1e3dcd9a12ceed831dd7460492b6dd788721c
 *      Additional reviews and feedback by Tomas Langer.
 *
 *******************************************************************************/

package org.eclipse.microprofile.config;

/**
 * A value holder for ConfigAccessor values which all got resolved in a guaranteed atomic way.
 *
 * @see Config#snapshotFor(ConfigAccessor...)
 * @see ConfigAccessor#getValue(ConfigSnapshot)
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:manfred.huber@downdrown.at">Manfred Huber</a>
 * @author <a href="mailto:elexx@apache.org">Alex Falb</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 */
public interface ConfigSnapshot {
}
