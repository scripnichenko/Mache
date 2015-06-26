package org.mache.events.integration;

import java.io.IOException;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.mache.coordination.CoordinationEntryEvent;
import org.mache.events.BaseCoordinationEntryEventProducer;

import com.google.gson.Gson;

public class KafkaEventProducer<K,V extends CoordinationEntryEvent<K>> extends BaseCoordinationEntryEventProducer<K, V> {
    private final Gson gson = new Gson();
    private final Producer<String, String> producer;
    public KafkaEventProducer(final Properties producerConfig, final String topicName) {
        super(topicName);
        producer = new Producer<String, String>( new ProducerConfig(producerConfig));
    }

    @Override
    public void send(final V event) throws InterruptedException, IOException {
        String TOPIC= getTopicName().replace("$", ".");

        KeyedMessage<String, String> data = new KeyedMessage<String, String>(TOPIC, "0", gson.toJson(event));
        producer.send(data);
    }

    @Override
    public void close() {
        producer.close();
    }
}
