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

import javax.management.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterMBeanServerInterceptor implements NoInstantiationMBeanServerInterceptor {

    private final MBeanServer defaultInterceptor;
    private final MBeanServer otherInterceptor;
    private final String otherDomain;

    public MasterMBeanServerInterceptor(
        MBeanServer defaultInterceptor,
        MBeanServer otherInterceptor,
        String otherDomain) {
        this.defaultInterceptor = defaultInterceptor;
        this.otherInterceptor = otherInterceptor;
        this.otherDomain = otherDomain;
    }
    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        checkRegistration(name);
        return choose(name).createMBean(className, name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        checkRegistration(name);
        return choose(name).createMBean(className, name, loaderName);
    }

    @Override
    public final ObjectInstance createMBean(String className, ObjectName name, Object params[], String signature[])
        throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {

        checkRegistration(name);
        return choose(name).createMBean(className, name, params, signature);
    }


    @Override
    public final ObjectInstance createMBean(final String className,
                                            final ObjectName name,
                                            final ObjectName loaderName,
                                            final Object params[],
                                            final String signature[])
        throws ReflectionException, InstanceAlreadyExistsException,
        MBeanException, NotCompliantMBeanException, InstanceNotFoundException {

        checkRegistration(name);
        return choose(name).createMBean(className, name, loaderName,
            params, signature);
    }


    @Override
    public final ObjectInstance getObjectInstance(final ObjectName name) throws InstanceNotFoundException {
        return choose(name).getObjectInstance(name);
    }


    @Override
    public final Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return union(
            defaultInterceptor.queryMBeans(name, query),
            otherInterceptor.queryMBeans(name, query)
        );
    }

    @Override
    public final Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return union(
            defaultInterceptor.queryNames(name, query),
            otherInterceptor.queryNames(name, query)
        );
    }

    public final String getDefaultDomain() {
        return defaultInterceptor.getDefaultDomain();
    }

    public String[] getDomains() {
        return union(
            Arrays.asList(defaultInterceptor.getDomains()),
            Arrays.asList(otherInterceptor.getDomains())
        ).toArray(new String[0]);
    }


    @Override
    public Integer getMBeanCount() {
        return normalize(defaultInterceptor.getMBeanCount())+normalize(otherInterceptor.getMBeanCount());
    }

    @Override
    public final boolean isRegistered(final ObjectName name) {
        return defaultInterceptor.isRegistered(name) || otherInterceptor.isRegistered(name);
    }

    @Override
    public final boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return choose(name).isInstanceOf(name, className);
    }

    @Override
    public final ObjectInstance registerMBean(Object object, ObjectName name)
        throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        checkRegistration(name);
        return choose(name).registerMBean(object, name);
    }

    @Override
    public final void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
        throws InstanceNotFoundException {
        choose(name).addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public final void addNotificationListener(final ObjectName name,
                                              final ObjectName listener,
                                              final NotificationFilter filter,
                                              final Object handback)
        throws InstanceNotFoundException {
        choose(name).addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public final void removeNotificationListener(
        final ObjectName name,
        final NotificationListener listener)
        throws InstanceNotFoundException, ListenerNotFoundException {

        choose(name).removeNotificationListener(name, listener);
    }

    @Override
    public final void removeNotificationListener(
        final ObjectName name,
        final ObjectName listener)
        throws InstanceNotFoundException, ListenerNotFoundException {

        choose(name).removeNotificationListener(name, listener);
    }

    @Override
    public final void removeNotificationListener(
        final ObjectName name,
        final NotificationListener listener,
        final NotificationFilter filter,
        final Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException {

        choose(name).removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public final void removeNotificationListener(
        final ObjectName name,
        final ObjectName listener,
        final NotificationFilter filter,
        final Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException {

        choose(name).removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public final void unregisterMBean(final ObjectName name)
        throws InstanceNotFoundException, MBeanRegistrationException {

        choose(name).unregisterMBean(name);
    }

    @Override
    public final Object getAttribute(final ObjectName name,
                                     final String attribute)
        throws MBeanException, AttributeNotFoundException,
        InstanceNotFoundException, ReflectionException {

        return choose(name).getAttribute(name, attribute);
    }

    @Override
    public final AttributeList getAttributes(final ObjectName name,
                                             final String[] attributes)
        throws InstanceNotFoundException, ReflectionException {
        return choose(name).getAttributes(name, attributes);
    }

    @Override
    public final void setAttribute(final ObjectName name,
                                   final Attribute attribute)
        throws InstanceNotFoundException, AttributeNotFoundException,
        InvalidAttributeValueException, MBeanException,
        ReflectionException {

        choose(name).setAttribute(name, attribute);
    }

    @Override
    public final AttributeList setAttributes(final ObjectName name,
                                             final AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException {

        return choose(name).setAttributes(name, attributes);
    }

    @Override
    public final Object invoke(final ObjectName name,
                               final String operationName,
                               final Object params[],
                               final String signature[])
        throws InstanceNotFoundException, MBeanException, ReflectionException {

        return choose(name).invoke(name, operationName, params, signature);
    }

    @Override
    public final MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return choose(name).getMBeanInfo(name);
    }

    @Override
    public final ClassLoader getClassLoader(final ObjectName loaderName) throws InstanceNotFoundException {
        return choose(loaderName).getClassLoader(loaderName);
    }

    @Override
    public final ClassLoader getClassLoaderFor(ObjectName name) throws InstanceNotFoundException {
        return choose(name).getClassLoaderFor(name);
    }

    private MBeanServer choose(ObjectName name) {
        if (name == null) return defaultInterceptor;
        if (name.getDomain().equals(otherDomain)) return otherInterceptor;
        return defaultInterceptor;
    }

    private void checkRegistration(ObjectName name)
            throws MBeanRegistrationException {
        if (name == null) return;
        if (name.getDomain().equals(otherDomain)) {
            final RuntimeException x =
                    new UnsupportedOperationException(otherDomain +
                            ": Can't register an MBean in that domain.");
            throw new MBeanRegistrationException(x, "Registration failed.");
        }
    }
    //
    //    public ClassLoader getMBeanClassLoader(ObjectName objectName) throws InstanceNotFoundException {
    //        return getClassLoader(objectName);
    //    }

    private int normalize(Integer i) {
        if(i==null || i<0)  return 0;
        return i;
    }

    private <T> Set<T> union(Set<T> lhs, Set<T> rhs) {
        Set<T> result = new HashSet<T>();
        result.addAll(lhs);
        result.addAll(rhs);
        return result;
    }

    private <T> List<T> union(List<T> lhs, List<T> rhs) {
        List<T> result = new ArrayList<T>();
        result.addAll(lhs);
        result.addAll(rhs);
        return result;
    }


}
