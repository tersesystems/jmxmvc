package com.tersesystems.jmxmvc.examples.alphabet;

import com.tersesystems.jmxmvc.model.AbstractModel;
import com.tersesystems.jmxmvc.model.AbstractModelItem;
import com.tersesystems.jmxmvc.model.ModelItem;
import org.slf4j.Logger;

import javax.management.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.management.MBeanServerNotification.REGISTRATION_NOTIFICATION;
import static javax.management.MBeanServerNotification.UNREGISTRATION_NOTIFICATION;

public class AlphabetModel extends AbstractModel<Character> {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AlphabetModel.class);

    private static final String VirtualAlphabetObject = "VirtualAlphabetObject";

    private boolean running = false;

    private Character[] alphabet = new Character[26];
    private MBeanServerDelegate delegate;

    public AlphabetModel(String domain) {
        super(domain);
    }

    @Override
    protected MBeanInfo generateMBeanInfo(Character element) {
        final MBeanAttributeInfo[] attributeInfos =
                new MBeanAttributeInfo[]{
                        new MBeanAttributeInfo("Vowel", "boolean",
                                "True if this MBean represents a vowel",
                                true, false, false),
                };

        return new MBeanInfo("letter",
                "An MBean representing a letter in the alphabet",
                attributeInfos,
                null, null, null);
    }

    @Override
    protected String generateMBeanClassName(Character element) {
        return VirtualAlphabetObject;
    }

    @Override
    protected Map<String, String> generateObjectNameProperties(Character element) {
        return Collections.singletonMap("letter", element.toString());
    }

    @Override
    public void start(MBeanServerDelegate delegate) throws Exception {
        this.delegate = delegate;
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            alphabet[i++] = c;
        }
        running = true;
    }

    @Override
    public void stop() throws Exception {
        alphabet = null;
        running = false;
    }

    private List<ObjectName> alphabetNames() {
        return Arrays.stream(alphabet).map(ch -> {
            try {
                return generateObjectName(ch);
            } catch (MalformedObjectNameException e) {
                return null;
            }
        }).collect(Collectors.toList());
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
            throw new InstanceNotFoundException(name +
                    ": MBean not found.");
        }

        Character ch = findFromName(name).orElseThrow(() -> new InstanceNotFoundException(name + ": MBean not found."));
        if (Arrays.binarySearch(alphabet, ch) < 0) {
            logger.error("getItem: {} is not found in alphabet!", ch);
            throw new InstanceNotFoundException(name +
                    ": MBean not found.");
        }

        ObjectInstance objectInstance = generateObjectInstance(name, ch);
        MBeanInfo beanInfo = generateMBeanInfo(ch);
        return new AlphabetModelItem(name, objectInstance, beanInfo, ch);
    }

    @Override
    public Integer getMBeanCount() {
        return alphabet.length;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, Function<ObjectName, Boolean> queryFunction) {
        return new NameQuery(name, () -> Arrays.asList(alphabet), queryFunction).getResults();
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, Function<ObjectName, Boolean> queryFunction) {
        return new MBeanQuery(name, () -> Arrays.asList(alphabet), queryFunction).getResults();
    }

    private Optional<Character> findFromName(ObjectName name) {
        logger.trace("findFromName: name = {}", name);
        String letter = name.getKeyProperty("letter");
        if (letter == null || letter.length() != 1) {
            return Optional.empty();
        }
        return Optional.of(letter.charAt(0));
    }

    static class AlphabetModelItem extends AbstractModelItem<Character> {
        AlphabetModelItem(ObjectName objectName, ObjectInstance objectInstance, MBeanInfo mBeanInfo, Character element) {
            super(objectName, objectInstance, mBeanInfo, element);
        }

        @Override
        public Object invoke(String operationName, Object[] params, String[] signature) throws Exception {
            logger.trace("invoke: operationName = {}", operationName);
            return null;
        }

        @Override
        public Object getAttribute(String attribute) {
            logger.trace("getAttribute: attribute = {}", attribute);
            return isVowel(getElement());
        }

        boolean isVowel(Character element) {
            boolean isVowel = false;
            if (element == 'a' || element == 'e' || element == 'i' || element == 'o' || element == 'u') {
                isVowel = true;
            }
            return isVowel;
        }
    }
}
