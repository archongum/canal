package com.alibaba.otter.canal.common;

import com.alibaba.otter.canal.kafka.CanalKafkaProducer;
import com.alibaba.otter.canal.protocol.FlatMessage;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.List;


/**
 * idempotence with redis
 * @author Archon  4/4/19
 * @since
 */
public class IdempotenceClient {

    private static final Logger logger = LoggerFactory.getLogger(CanalKafkaProducer.class);

    private static String  host = Protocol.DEFAULT_HOST;
    private static int     port = Protocol.DEFAULT_PORT;
    private static String  password = "";
    private static int     database = Protocol.DEFAULT_DATABASE;

    private MQProperties properties;
    private JedisPool    jedisPool;

    private static final int MAX_COUNTDOWN = 100000;
    private int              cleanupCountdown = MAX_COUNTDOWN;

    public IdempotenceClient(MQProperties properties) {
        this.properties = properties;
        this.init();
    }

    public List<FlatMessage> idemFilter(List<FlatMessage> flatMessages, String destination) {
        if (flatMessages == null) {
            return null;
        }
        List<FlatMessage> newFlatMessages = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            for (FlatMessage flatMessage : flatMessages) {
                String key = properties.getIdemKeyPrefix() + destination;
                String es = String.valueOf(flatMessage.getEs() / 1000);
                String dataHashCode = String.valueOf(flatMessage.getData().hashCode());
                String member = es + "_" + dataHashCode;
                cleanupSet(jedis, key); // avoid large redis set.
                if (!jedis.sismember(key, member)) {
                    newFlatMessages.add(flatMessage);
                    jedis.sadd(key, member);
                } else {
                    logger.info("[idempotence] filter message: {}", flatMessage);
                }
            }
        }
        return newFlatMessages;
    }

    public void stop() {
        jedisPool.close();
    }

    private void cleanupSet(Jedis jedis, String key) {
        if (--this.cleanupCountdown < 0) {
            jedis.del(key);
            this.cleanupCountdown = MAX_COUNTDOWN;
        }
    }

    private void init() {
        String server = properties.getIdemServers();
        String[] hostAndPort = server.split(":", -1);
        host = hostAndPort[0];
        if (hostAndPort.length > 1) {
            port = Integer.parseInt(hostAndPort[1]);
        }
        password = properties.getIdemPassword();
        database = Integer.parseInt(properties.getIdemDatabase());

        jedisPool = new JedisPool(
            new GenericObjectPoolConfig(),
            host,
            port,
            Protocol.DEFAULT_TIMEOUT,
            password,
            database,
            null);
    }

    @Override
    public String toString() {
        return "IdempotenceClient{" + "properties=" + properties + '}';
    }
}
