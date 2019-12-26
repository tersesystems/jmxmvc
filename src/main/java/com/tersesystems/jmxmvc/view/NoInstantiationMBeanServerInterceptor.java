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

import com.sun.jmx.interceptor.MBeanServerInterceptor;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;

/**
 * A MBeanServerInterceptor interface that provides default methods that throw UnsupportedOperationException.
 */
public interface NoInstantiationMBeanServerInterceptor extends MBeanServerInterceptor {

    default Object instantiate(String className) {
        throw new UnsupportedOperationException();
    }

    default Object instantiate(String className, ObjectName loaderName) {
        throw new UnsupportedOperationException();
    }

    default Object instantiate(String className, Object[] params, String[] signature) {
        throw new UnsupportedOperationException();
    }

    default Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException();
    }

    default ObjectInputStream deserialize(ObjectName name, byte[] data) {
        throw new UnsupportedOperationException();
    }

    default ObjectInputStream deserialize(String className, byte[] data) {
        throw new UnsupportedOperationException();
    }

    default ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) {
        throw new UnsupportedOperationException();
    }

    default ClassLoaderRepository getClassLoaderRepository() {
        throw new UnsupportedOperationException();
    }
}
