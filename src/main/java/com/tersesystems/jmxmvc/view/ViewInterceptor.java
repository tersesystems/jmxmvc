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
package com.tersesystems.jmxmvc.view;

import com.tersesystems.jmxmvc.model.Model;
import com.tersesystems.jmxmvc.model.ModelItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.Arrays;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.*;

public class ViewInterceptor implements NoInstantiationMBeanServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ViewInterceptor.class);

    private final MBeanServerDelegate forwarder;

    private final MBeanServer server;

    private final Model model;

    public ViewInterceptor(Model model, MBeanServerDelegate forwarder, MBeanServer server) {
        this.model = model;
        this.forwarder = forwarder;
        this.server = server;
    }

    public void start() throws Exception {
        logger.trace("start", "");
        model.start(forwarder);
    }

    public void stop() throws Exception {
        logger.trace("stop", "");
        model.stop();
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        logger.trace("getObjectInstance: {}", format("name = %s", name));
        final ModelItem item = model.getItem(name);
        return item.getObjectInstance();
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        logger.trace("queryMBeans: {}", format("name = %s, query = %s", name, query));
        return model.queryMBeans(name, (ObjectName n) -> {
            if (query == null) return true;
            try {
                query.setMBeanServer(server);
                return query.apply(n);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public Set<ObjectName> queryNames(final ObjectName name,
                                      final QueryExp query) {
        logger.trace("queryNames: {}", format("name = %s, query = %s", name, query));

        return model.queryNames(name, (ObjectName n) -> {
            if (query == null) return true;
            try {
                query.setMBeanServer(server);
                return query.apply(n);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public String getDefaultDomain() {
        return model.getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        return model.getDomains();
    }

    @Override
    public Integer getMBeanCount() {
        return model.getMBeanCount();
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        logger.trace("isRegistered: {}", format("name = %s", name));

        try {
            model.getItem(name);
            return true;
        } catch (InstanceNotFoundException x) {
            return false;
        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException {
        logger.trace("isInstanceOf: {}", format("name = %s, className = %s", name, className));

        final ModelItem item = model.getItem(name);
        return item.isInstanceOf(className);
    }

    @Override
    public Object getAttribute(final ObjectName name,
                               final String attribute)
            throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        logger.trace("getAttribute", format("name = %s, attribute = %s", name, attribute));

        final ModelItem item = model.getItem(name);
        return item.getAttribute(attribute);
    }

    @Override
    public AttributeList getAttributes(final ObjectName name,
                                       final String[] attributes)
            throws InstanceNotFoundException {
        logger.trace("getAttributes: {}", format("name = %s, attributes = %s", name, Arrays.toString(attributes)));

        final ModelItem item = model.getItem(name);
        MBeanAttributeInfo[] beanAttributeInfos = item.getMBeanInfo().getAttributes();
        final String[] attn = stream(beanAttributeInfos).map(MBeanFeatureInfo::getName).toArray(String[]::new);
        final AttributeList list = new AttributeList(attn.length);
        for (String anAttn : attn) {
            try {
                final Attribute a =
                        new Attribute(anAttn, item.getAttribute(anAttn));
                list.add(a);
            } catch (Exception x) {
                // Skip the attribute that couldn't be obtained.
                //
            }
        }

        return list;
    }

    @Override
    public void setAttribute(final ObjectName name,
                             final Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
            MBeanException, ReflectionException {
        logger.trace("setAttribute: {}", format("name = %s, attribute = %s", name, attribute));

        final ModelItem item = model.getItem(name);
        if (item != null) {
            final String attname = attribute == null ? null : attribute.getName();
            if (attname == null) {
                final RuntimeException r =
                        new IllegalArgumentException("Attribute name cannot be null");
                throw new RuntimeOperationsException(r,
                        "Exception occurred trying to invoke the setter on the MBean");
            }

            // XXX FIXME
            item.getAttribute(attname);
            throw new AttributeNotFoundException(attname + " not accessible");
        }
    }

    @Override
    public AttributeList setAttributes(final ObjectName name,
                                       final AttributeList attributes)
            throws InstanceNotFoundException {
        logger.trace("setAttributes: {}", format("name = %s, attribute = %s", name, attributes));

        model.getItem(name);
        return new AttributeList(0);
    }

    @Override
    public Object invoke(final ObjectName name,
                         final String operationName,
                         final Object[] params,
                         final String[] signature)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException {
        logger.trace("invoke: {}", format("name = %s, operationName = %s, params = %s, signature = %s", name, operationName, Arrays.toString(params), Arrays.toString(signature)));

        if (operationName == null) {
            final RuntimeException r =
                    new IllegalArgumentException("Operation name cannot be null");
            throw new RuntimeOperationsException(r,
                    "Exception occurred trying to invoke the operation on the MBean");
        }

        final ModelItem item = model.getItem(name);
        if (operationName.startsWith("get") &&
                (params == null || params.length == 0) &&
                (signature == null || signature.length == 0)) {
            try {
                return item.getAttribute(operationName.substring(3));
            } catch (AttributeNotFoundException x) {
                throw new ReflectionException(
                        new NoSuchMethodException(operationName),
                        "The operation with name " + operationName +
                                " could not be found");
            }
        }

        try {
            return model.invoke(item, operationName, params, signature);
        } catch (Exception x) {
            throw new
                    MBeanException(x, "Failed to invoke " + operationName);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(final ObjectName name)
            throws InstanceNotFoundException {
        logger.trace("getMBeanInfo: {}", format("name = %s", name));

        return model.getItem(name).getMBeanInfo();
    }

    @Override
    public ClassLoader getClassLoaderFor(final ObjectName name)
            throws InstanceNotFoundException {
        return getMBeanClassLoader(name);
    }

    @Override
    public ClassLoader getClassLoader(final ObjectName loaderName) {
        return null;
    }

    // We do not accept the creation of new mbeans...
    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws MBeanException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws MBeanException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object params[], String signature[]) throws MBeanException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object params[], String signature[]) throws MBeanException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws MBeanRegistrationException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public void unregisterMBean(ObjectName name) throws MBeanRegistrationException {
        throw new MBeanRegistrationException(null, "Registration failed.");
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException("No broadcaster by that name");
    }

    private ClassLoader getMBeanClassLoader(final ObjectName name)
            throws InstanceNotFoundException {
        return model.getItem(name).getClass().getClassLoader();
    }

}
