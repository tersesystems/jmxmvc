# jmxmvc

<!---freshmark shields
output = [
	link(shield('Bintray', 'bintray', 'tersesystems:jmxmvc', 'blue'), 'https://bintray.com/tersesystems/maven/jmxmvc/view'),
	link(shield('Latest version', 'latest', '{{previousVersion}}', 'blue'), 'https://github.com/tersesystems/jmxmvc/releases/latest'),
	link(shield('License Apache2', 'license', 'Apache2', 'blue'), 'https://www.tldrlegal.com/l/apache2'),
	'',
	link(image('Travis CI', 'https://travis-ci.org/tersesystems/jmxmvc.svg?branch=master'), 'https://travis-ci.org/tersesystems/jmxmvc')
	].join('\n')
-->
[![Bintray](https://img.shields.io/badge/bintray-tersesystems%3Ajmxmvc-blue.svg)](https://bintray.com/tersesystems/maven/jmxmvc/view)
[![Latest version](https://img.shields.io/badge/latest-0.0.1-blue.svg)](https://github.com/tersesystems/jmxmvc/releases/latest)
[![License Apache2](https://img.shields.io/badge/license-Apache2-blue.svg)](https://www.tldrlegal.com/l/apache2)

[![Travis CI](https://travis-ci.org/tersesystems/jmxmvc.svg?branch=master)](https://travis-ci.org/tersesystems/jmxmvc)
<!---freshmark /shields -->

This is an MVC breakout of `MBeanInterceptor`.  This lets you create a model and automatically display object names and hierarchy inside an MBeanServer -- the tree bit on the left that displays all the MBeans -- without having to do the work of registering beans individually.

It is based on the OpenDMK examples from Sun Microsystems available at https://github.com/wsargent/jmx-interceptor.

## Examples

There are two examples, one which displays Akka Actors in JMX according to the model, and another one which displays the alphabet.

## Why use virtual mbeans?

JMX is built around the concept of MBeans and an MBeanServer.  All of the methods involving JMX (Standard MBeans, MXBeans, Dynamic MBeans, Open MBeans, Model MBeans) have the common end point of registering an object instance directly against an MBeanServer:

```java
import javax.management.*;

class RegisterMBean {
    public ObjectInstance register(Object mbean, ObjectName objectName) throws Exception {
       MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
       return mBeanServer.registerMBean(mbean, objectName);
    }
}
```

A reference to the instance is always stored by the mbean server.  This means that when you make an object available through JMX, there are knock-on effects.  It takes up memory.  It exists in a flat namespace that is searched by the default install.  If you want to expose a large number of objects through JMX, you'll run into runtime overhead. 

Using an interceptor takes care of all of this. Per the OpenDMK documentation:

> "The interceptor handles operations on MBeans it owns itself, and forwards others to the default interceptor. An interceptor might own all MBeans whose names match a particular pattern, for instance. In this way, MBeans do not necessarily have to be Java objects. This is very useful when there are a great many managed objects, or when they are very volatile."

So the key idea here is that there is no persistent MBean object registered.  Instead, a short-lived dynamic mbean is created and returned for every query.  
