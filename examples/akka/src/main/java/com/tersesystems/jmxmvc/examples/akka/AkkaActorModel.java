package com.tersesystems.jmxmvc.examples.akka;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import com.tersesystems.jmxmvc.model.AbstractModel;
import com.tersesystems.jmxmvc.model.ModelItem;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import javax.management.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.management.MBeanServerNotification.REGISTRATION_NOTIFICATION;
import static javax.management.MBeanServerNotification.UNREGISTRATION_NOTIFICATION;

interface ModelItemFactory<T> {
    ModelItem create(ObjectName name, ObjectInstance objectInstance, MBeanInfo beanInfo, T element);
}

interface MBeanInfoFactory<T> {
    MBeanInfo create(T element);
}

public class AkkaActorModel extends AbstractModel<ActorRef> {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AkkaActorModel.class);

    private static final String AkkaActorModel = "VirtualActorSystem";
    private final ActorRef actorRef;
    private final ModelItemFactory<ActorRef> itemFactory;
    private final MBeanInfoFactory<ActorRef> beanInfoFactory;
    private MBeanServerDelegate delegate;

    private boolean running = false;

    private ActorSystem system;
    private CopyOnWriteArrayList<ActorRef> jmxVisibleActors = new CopyOnWriteArrayList<>();

    public AkkaActorModel(String domain,
                          ActorSystem system,
                          ActorRef actorRef,
                          MBeanInfoFactory<ActorRef> beanInfoFactory,
                          ModelItemFactory<ActorRef> itemFactory) {
        super(domain);
        this.system = system;
        this.actorRef = actorRef;
        this.itemFactory = itemFactory;
        this.beanInfoFactory = beanInfoFactory;
    }

    @Override
    public void start(MBeanServerDelegate delegate) throws Exception {
        // Get the children of the actor
        running = true;
        this.delegate = delegate;

        Consumer<ActorRef> addFunction = (ref) -> logger.info("Added ref {}", ref);
        Consumer<ActorRef> removeFunction = (ref) -> logger.info("Removed ref {}", ref);
        Consumer<List<ActorRef>> refreshFunction = this::refresh;
        String rootPath = "/user/" + actorRef.path().name() + "/*";
        system.actorOf(Props.create(SpyActor.class, rootPath, addFunction, removeFunction, refreshFunction), "spyActor");

        jmxVisibleActors.add(actorRef);
        List<MBeanServerNotification> list = generateNotifications(REGISTRATION_NOTIFICATION, actorNames(jmxVisibleActors));
        sendNotifications(list);
    }

    @Override
    public void stop() throws Exception {
        List<MBeanServerNotification> list = generateNotifications(UNREGISTRATION_NOTIFICATION, actorNames(jmxVisibleActors));
        sendNotifications(list);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public ModelItem getItem(ObjectName name) throws InstanceNotFoundException {
        logger.trace("getItem: name = {}", name);

        if (!isRunning()) {
            logger.error("getItem: model is not running!");
            throw new InstanceNotFoundException(name + ": MBean not found.");
        }

        if (name.getDomain().equals(getDefaultDomain())) {
            try {
                ActorRef ref = findFromName(name).orElseThrow(() -> new InstanceNotFoundException(name + ": MBean not found."));
                ObjectInstance objectInstance = generateObjectInstance(name, ref);
                MBeanInfo beanInfo = generateMBeanInfo(ref);
                return itemFactory.create(name, objectInstance, beanInfo, ref);
            } catch (Exception e) {
                logger.error("getItem", e);
                throw e;
            }
        } else {
            return null;
        }
    }

    @Override
    protected MBeanInfo generateMBeanInfo(ActorRef actorRef) {
        return beanInfoFactory.create(actorRef);
    }

    @Override
    protected String generateMBeanClassName(ActorRef actorRef) {
        return AkkaActorModel;
    }

    @Override
    public Integer getMBeanCount() {
        return jmxVisibleActors.size();
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, Function<ObjectName, Boolean> queryFunction) {
        return new NameQuery(name, () -> jmxVisibleActors, queryFunction).getResults();
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, Function<ObjectName, Boolean> queryFunction) {
        return new MBeanQuery(name, () -> jmxVisibleActors, queryFunction).getResults();
    }

    @Override
    protected Map<String, String> generateObjectNameProperties(ActorRef element) {
        Map<String, String> keys = new HashMap<>();
        ActorPath path = element.path();
        keys.put("type", path.parent().toStringWithoutAddress());
        keys.put("name", path.name());

        return keys;
    }

    private void refresh(List<ActorRef> childrenList) {
        int added = jmxVisibleActors.addAllAbsent(childrenList);
        if (added > 0) {
            logger.info("refresh: updating JMX list of actors, added = {}", added);
            List<MBeanServerNotification> list = generateNotifications(REGISTRATION_NOTIFICATION, actorNames(jmxVisibleActors));
            for (MBeanServerNotification notification : list) {
                delegate.sendNotification(notification);
            };
        }
    }

    private void sendNotifications(Iterable<MBeanServerNotification> notificationList) {
        for (MBeanServerNotification notification : notificationList) {
            delegate.sendNotification(notification);
        }
    }

    private List<ObjectName> actorNames(List<ActorRef> list) {
        return list.stream().filter(Objects::nonNull).map(actorRef -> {
            try {
                return generateObjectName(actorRef);
            } catch (MalformedObjectNameException e) {
                logger.error("Cannot generate name", e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    private Optional<ActorRef> findFromName(ObjectName objectName) {
        // https://doc.akka.io/docs/akka/2.5.5/java/actors.html#identifying-actors-via-actor-selection
        String type = objectName.getKeyProperty("type");
        String name = objectName.getKeyProperty("name");
        String path = type + "/" + name;
        if (path == null) {
            return Optional.empty();
        }

        ActorRef result = null;
        try {
            Timeout timeout = Timeout.apply(1, TimeUnit.SECONDS);
            Future<ActorRef> actorRefFuture = system.actorSelection(path).resolveOne(timeout);
            result = Await.result(actorRefFuture, timeout.duration());
        } catch (Exception e) {
            logger.error(String.format("Cannot resolve path %s", path), e);
        }
        return Optional.ofNullable(result);
    }

    static class Refresh {
        static final String key = "REFRESH_KEY";
        static final Refresh instance = new Refresh();
        private Refresh() {}
    }

    // watches for children and termination of children
    static class SpyActor extends AbstractActorWithTimers {
        private final String path;
        private final Consumer<ActorRef> addFunction;
        private final Consumer<ActorRef> removeFunction;
        private final Consumer<List<ActorRef>> refreshFunction;
        private final CopyOnWriteArrayList<ActorRef> children;

        public SpyActor(String path, Consumer<ActorRef> addFunction, Consumer<ActorRef> removeFunction, Consumer<List<ActorRef>> refreshFunction) {
            this.path = path;
            this.addFunction = addFunction;
            this.removeFunction = removeFunction;
            this.refreshFunction = refreshFunction;
            this.children = new CopyOnWriteArrayList<>();
        }

        @Override
        public void preStart() {
            getTimers().startPeriodicTimer(Refresh.key, Refresh.instance, FiniteDuration.create(1, TimeUnit.SECONDS));
        }

        public void findChildren(String path) {
            String correlationId = UUID.randomUUID().toString();
            ActorSelection allChildren = getContext().getSystem().actorSelection(path);
            allChildren.tell(Identify.apply(correlationId), self());
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create().match(Refresh.class, r -> {
                findChildren(path);
                refreshFunction.accept(children);
            }).match(ActorIdentity.class, a -> {
                a.getActorRef().map(ref -> {
                    // Do a recursive search...
                    findChildren(ref.path().toStringWithoutAddress() + "/*");
                    if (children.addIfAbsent(ref)) {
                        logger.info("added {}, ref = {}", a.correlationId(), ref);
                        addFunction.accept(ref);
                    }
                    return ref;
                });
            }).match(Terminated.class, t -> {
                ActorRef ref = t.getActor();
                if (children.remove(ref)) {
                    logger.info("Removing ref = {}", ref);
                    removeFunction.accept(t.getActor());
                }
            }).build();
        }
    }
}
