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
import com.sun.jmx.interceptor.MBeanServerInterceptor;
import com.sun.jmx.mbeanserver.JmxMBeanServer;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import static java.util.Objects.requireNonNull;

public class ViewInterceptorBuilder {
    protected MBeanServer mBeanServer;
    protected Model model;

    public ViewInterceptorBuilder() throws Exception {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public ViewInterceptorBuilder(MBeanServer mBeanServer) throws Exception {
        this.mBeanServer = mBeanServer;
    }
    public ViewInterceptorBuilder(MBeanServer mBeanServer, Model model) throws Exception {
        this.mBeanServer = mBeanServer;
        this.model = model;
    }

    public ViewInterceptorBuilder withModel(Model model) {
        this.model = model;
        return this;
    }

    public ViewInterceptorBuilder withMBeanServer(MBeanServer mbs) {
        this.mBeanServer = mbs;
        return this;
    }

    public ViewInterceptor build() throws Exception {
        requireNonNull(mBeanServer, "Null mbeanServer");
        requireNonNull(model, "Null model");
        setInterceptorsEnabled();

        ViewInterceptor viewInterceptor = insertViewInterceptor(model);
        return viewInterceptor;
    }

    protected void setInterceptorsEnabled() throws IllegalAccessException, NoSuchFieldException {
        Field f = mBeanServer.getClass().getDeclaredField("interceptorsEnabled");
        f.setAccessible(true);
        f.set(mBeanServer, true);
    }

    protected ViewInterceptor insertViewInterceptor(Model model) {
        final JmxMBeanServer beanServer = (JmxMBeanServer) mBeanServer;
        final MBeanServer defaultInterceptor = beanServer.getMBeanServerInterceptor();
        final MBeanServerDelegate delegate = beanServer.getMBeanServerDelegate();

        final ViewInterceptor viewInterceptor =
                new ViewInterceptor(model, delegate, mBeanServer);

        final MBeanServerInterceptor master =
                new MasterMBeanServerInterceptor(defaultInterceptor, viewInterceptor, model.getDefaultDomain());
        beanServer.setMBeanServerInterceptor(master);

        return viewInterceptor;
    }

}
