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

import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static java.lang.String.format;

public abstract class AbstractModelItem<T> implements ModelItem {

    protected final MBeanInfo mBeanInfo;
    protected final ObjectName objectName;
    protected final ObjectInstance objectInstance;
    protected final T element;

    public AbstractModelItem(ObjectName objectName, ObjectInstance objectInstance, MBeanInfo mBeanInfo, T element) {
        this.objectInstance = objectInstance;
        this.objectName = objectName;
        this.mBeanInfo = mBeanInfo;
        this.element = element;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public ObjectInstance getObjectInstance() {
        return objectInstance;
    }

    public T getElement() {
        return element;
    }

    @Override
    public boolean isInstanceOf(String className)  {
        return mBeanInfo.getClassName().equals(className);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }

    @Override
    public String toString() {
        return format("ModelItem(objectName=%s, objectInstance=%s, element=%s)", objectName, objectInstance, mBeanInfo, element.toString());
    }

}
