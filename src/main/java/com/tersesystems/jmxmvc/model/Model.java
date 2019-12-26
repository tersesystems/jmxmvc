/**
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright ${year} ${name} <${email}>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tersesystems.jmxmvc.model;

import javax.management.*;
import java.util.Set;
import java.util.function.Function;

public interface Model {

    void start(MBeanServerDelegate forwarder) throws Exception;

    void stop() throws Exception;

    boolean isRunning();

    ModelItem getItem(ObjectName name) throws InstanceNotFoundException;

    Set<ObjectInstance> queryMBeans(ObjectName name, Function<ObjectName, Boolean> queryFunction);

    Set<ObjectName> queryNames(ObjectName name, Function<ObjectName, Boolean> queryFunction);

    String getDefaultDomain();

    String[] getDomains();

    Integer getMBeanCount();

    Object invoke(ModelItem item, String operationName, Object[] params, String[] signature) throws Exception;

}
