package com.tersesystems.jmxmvc.examples.akka;

import akka.actor.*;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;


import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletionStage;

public class AkkaChildrenTest {

    // https://doc.akka.io/docs/akka/2.5/java/testing.html#asynchronous-testing-testkit

    public static class SomeActor extends AbstractActor {
        ActorRef target = null;

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("hello", message -> {
                        getSender().tell("world", getSelf());
                        if (target != null) target.forward(message, getContext());
                    })
                    .match(ActorRef.class, actorRef -> {
                        target = actorRef;
                        getSender().tell("done", getSelf());
                    })
                    .build();
        }
    }

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testIt() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new TestKit(system) {{
            final Props props = Props.create(SomeActor.class);
            final ActorRef subject = system.actorOf(props);

            // the run() method needs to finish within 3 seconds
            within(duration("3 seconds"), () -> {
                Integer id = 1;
                ActorSelection allChildrenSelection = system.actorSelection(subject.path().child("*"));
                CompletionStage<Object> childrenFuture = ask(allChildrenSelection, new Identify(id), 1000);


                return null;
            });
        }};
    }

}