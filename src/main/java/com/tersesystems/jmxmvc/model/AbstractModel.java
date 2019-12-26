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

import com.sun.jmx.defaults.ServiceName;
import org.slf4j.Logger;

import javax.management.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractModel<T> implements Model {
    private static final ObjectName delegateName;

    static {
        try {
            delegateName = new ObjectName(ServiceName.DELEGATE);
        } catch (Exception x) {
            throw new IllegalArgumentException(x);
        }
    }

    protected final String domain;

    public AbstractModel(String domain) {
        this.domain = domain;
    }

    @Override
    public String getDefaultDomain() {
        return domain;
    }

    @Override
    public String[] getDomains() {
        return new String[]{ getDefaultDomain() };
    }

    public ObjectName generateObjectName(T element) throws MalformedObjectNameException {
        Map<String, String> properties = generateObjectNameProperties(element);
        Hashtable<String, String> hashtable = new Hashtable<>(properties);
        return new ObjectName(getDefaultDomain(), hashtable);
    }

    protected abstract MBeanInfo generateMBeanInfo(T element);
    protected abstract Map<String, String> generateObjectNameProperties(T element);
    protected abstract String generateMBeanClassName(T element);

    @Override
    public Object invoke(ModelItem item, String operationName, Object[] params, String[] signature) throws Exception {
        return item.invoke(operationName,  params, signature);
    }

    protected ObjectInstance generateObjectInstance(ObjectName objectName, T element) {
        return new ObjectInstance(objectName, generateMBeanClassName(element));
    }

    public static List<MBeanServerNotification> generateNotifications(String type, List<ObjectName> names) {
        ArrayList<MBeanServerNotification> list = new ArrayList<>();
        for (ObjectName objectName: names) {
            list.add(new MBeanServerNotification(type, delegateName, 0, objectName));
        };
        return list;
    }

    public class NameQuery {
        private final Logger logger = org.slf4j.LoggerFactory.getLogger(NameQuery.class);

        private final ObjectName name;
        private final Supplier<Iterable<T>> supplier;
        private Function<ObjectName, Boolean> queryFunction;

        public NameQuery(ObjectName name, Supplier<Iterable<T>> supplier, Function<ObjectName, Boolean> queryFunction) {
            this.name = name;
            this.supplier = supplier;
            this.queryFunction = queryFunction;
        }

        private void addName(final ObjectName pattern, final T element, final Set<ObjectName> result) {
            try {
                if (element == null) return;
                final ObjectName n = generateObjectName(element);
                if ((!Helpers.matches(n, pattern)) || (!queryFunction.apply(n))) return;
                result.add(n);
            } catch (Exception x) {
                logger.error("addName", x);
            }
        }

        public Set<ObjectName> getResults() {
            if (name != null &&
                    !Helpers.wildmatch(domain.toCharArray(),
                            name.getDomain().toCharArray(), 0, 0))
                return Collections.emptySet();

            final Iterable<T> list = supplier.get();
            final HashSet<ObjectName> result = new HashSet<ObjectName>();

            for (T element : list) {
                addName(name, element, result);
            }
            return result;
        }
    }

    public class MBeanQuery {
        private final Logger logger = org.slf4j.LoggerFactory.getLogger(MBeanQuery.class);

        private final ObjectName name;
        private final Supplier<Iterable<T>> supplier;
        private Function<ObjectName, Boolean> queryFunction;

        public MBeanQuery(ObjectName name, Supplier<Iterable<T>> supplier, Function<ObjectName, Boolean> queryFunction) {
            this.name = name;
            this.supplier = supplier;
            this.queryFunction = queryFunction;
        }

        public Set<ObjectInstance> getResults() {
            if (name != null &&
                    !Helpers.wildmatch(domain.toCharArray(),
                            name.getDomain().toCharArray(), 0, 0))
                return Collections.emptySet();

            final Iterable<T> list = supplier.get();
            final Set<ObjectInstance> result = new HashSet<ObjectInstance>();

            for (T element : list) {
                addMBean(name, element, result);
            }
            return result;
        }

        private void addMBean(final ObjectName pattern, T element, Set<ObjectInstance> result) {
            try {
                if (element == null) return;
                final ObjectName n = generateObjectName(element);
                if ((!Helpers.matches(n, pattern)) || (!queryFunction.apply(n))) return;
                result.add(generateObjectInstance(n, element));
            } catch (Exception x) {
                logger.error("addMBean", x);
            }
        }
    }



}
