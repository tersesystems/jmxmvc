package com.tersesystems.jmxmvc.examples.akka;

import akka.actor.*;
import akka.event.LoggingAdapter;

import akka.event.Logging;
import com.tersesystems.jmxmvc.model.AbstractModelItem;
import com.tersesystems.jmxmvc.model.ModelItem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.tersesystems.jmxmvc.view.ViewInterceptor;
import com.tersesystems.jmxmvc.view.ViewInterceptorBuilder;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class Main {

    static final class CreateChild {
        private final String name;
        public CreateChild(String name) {
            this.name = name;
        }
    }

    static final class QueryChildren {
        private static final QueryChildren instance = new QueryChildren();
        private QueryChildren() {

        }
    }

    static class TestActor extends AbstractActor {
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, s -> {
                        log.info("Received String message: {}", s);
                    }).match(QueryChildren.class, c -> {
                        queryChildren();
                    }).match(CreateChild.class, c -> {
                        createChild(c);
                    })
                    .build();
        }

        void queryChildren() {
            sender().tell(context().children().size(), self());
        }

        void createChild(CreateChild c) {
            ActorRef actorRef = context().actorOf(Props.create(TestActor.class), c.name);
            sender().tell(actorRef, self());
        }

    }

    static class ActorModelItem extends AbstractModelItem<ActorRef> {
        private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AkkaActorModel.class);

        ActorModelItem(ObjectName objectName, ObjectInstance objectInstance, MBeanInfo mBeanInfo, ActorRef actor) {
            super(objectName, objectInstance, mBeanInfo, actor);
        }

        @Override
        public Object invoke(String operationName, Object[] params, String[] signature) throws Exception {
            logger.debug("invoke: operationName = {}", operationName);
            long timeout = 1000L;
            if ("createChild".equals(operationName)) {
                String name = UUID.randomUUID().toString();
                ActorRef newChildRef = (ActorRef) await(ask(element, new CreateChild(name), timeout));
                return newChildRef.path().toStringWithoutAddress();
            }
            return null;
        }

        /*
        List<MBeanServerNotification> list =
                generateNotifications(MBeanServerNotification.REGISTRATION_NOTIFICATION, actorNames(Collections.singletonList(newChildRef)));
        sendNotifications(list);
         */

        @Override
        public Object getAttribute(String attribute) {
            logger.debug("getAttribute: attribute = {}", attribute);
            try {
                switch (attribute) {
                    case "path":
                        return getPath();
                    case "children":
                        return getChildren();
                    default:
                        return null;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String getPath() {
            return element.path().toStringWithoutAddress();
        }

        Integer getChildren() throws Exception {
            long timeout = 1000L;
            return (Integer) await(ask(element, QueryChildren.instance, timeout));
        }

        <F> F await(Future<F> future) throws Exception {
            long timeout = 1000L;
            return Await.result(future, Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    public static void main(String[] args) throws Exception {
        // Set up the bean info factory and model info factory
        final MBeanInfoFactory<ActorRef> beanInfoFactory = element -> {
            final MBeanAttributeInfo pathAttribute = new MBeanAttributeInfo("path", "string",
                    "Absolute path of the actor",
                    true, false, false);

            final MBeanAttributeInfo childrenAttribute = new MBeanAttributeInfo("children", "integer",
                    "Number of Children",
                    true, false, false);

            final MBeanOperationInfo createChildOperation = new MBeanOperationInfo("createChild",
                    "Create a child of this actor",
                    new MBeanParameterInfo[] {},
                    "String",
                    MBeanOperationInfo.ACTION);

            return new MBeanInfo("actor",
                    "An MBean representing an Actor",
                    new MBeanAttributeInfo[]{ pathAttribute, childrenAttribute },
                    null,
                    new MBeanOperationInfo[] { createChildOperation },
                    null);
        };
        final ModelItemFactory<ActorRef> modelItemFactory = ActorModelItem::new;

        // Start up the actor system
        Config config = ConfigFactory.parseString("").withFallback(ConfigFactory.load());
        final ActorSystem system = ActorSystem.create("ActorSystem", config);
        final ActorRef testActorRef = system.actorOf(Props.create(TestActor.class), "testActor");

        // Start up the akka actor model.
        final String domain = system.name();
        final AkkaActorModel model = new AkkaActorModel(domain, system, testActorRef, beanInfoFactory, modelItemFactory);
        final ViewInterceptor interceptor = new ViewInterceptorBuilder()
                .withMBeanServer(ManagementFactory.getPlatformMBeanServer())
                .withModel(model)
                .build();
        interceptor.start();
    }
}
