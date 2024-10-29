package com.bytetrade.obridge.db.redis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
@Slf4j
public class RedisConfig {

	@Value("${spring.redis.host}")
	private String hostName;
	@Value("${spring.redis.port}")
	private int port;
	@Value("${spring.redis.password}")
	private String passWord;
	@Value("${spring.redis.jedis.pool.max-idle}")
	private int maxIdl;
	@Value("${spring.redis.jedis.pool.min-idle}")
	private int minIdl;
	@Value("${spring.redis.timeout}")
	private int timeout;

	private int defaultDb;

	@Value("${spring.redis.dbs}")
	private List<Integer> dbs;

	public static Map<Integer, RedisTemplate<String, String>> redisTemplateMap = new HashMap<>();

	@PostConstruct
	public void initRedisTemp() throws Exception {
		log.info("###### START create Redis Pool ######");
		defaultDb = 0;
		for (Integer db : dbs) {
			log.info("###### loading Redis-db-" + db + " ######");
			redisTemplateMap.put(db, redisTemplateObject(db));
		}
		log.info("###### END create Redis poll END ######");
	}

	public RedisTemplate<String, String> redisTemplateObject(Integer dbIndex) throws Exception {
		RedisTemplate<String, String> redisTemplateObject = new RedisTemplate<String, String>();

		JedisConnectionFactory jedisConnectionFactory = redisConnectionFactory(jedisPoolConfig(), dbIndex);
		// JedisPool pool = new JedisPool(jedisConnectionFactory.getPoolConfig(),
		// jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort());

		redisTemplateObject.setConnectionFactory(jedisConnectionFactory);
		setSerializer(redisTemplateObject);
		redisTemplateObject.afterPropertiesSet();
		return redisTemplateObject;
	}

	public JedisPoolConfig jedisPoolConfig() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();

		poolConfig.setMaxIdle(maxIdl);
		poolConfig.setMaxTotal(maxIdl);
		poolConfig.setMinIdle(minIdl);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(maxIdl);
		poolConfig.setTimeBetweenEvictionRunsMillis(60000);
		poolConfig.setBlockWhenExhausted(true);

		poolConfig.setMaxWaitMillis(10000);

		log.info("JedisPoolConfig:" + poolConfig);
		return poolConfig;
	}

	public JedisConnectionFactory redisConnectionFactory(JedisPoolConfig jedisPoolConfig, int db) {

		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(hostName);
		redisStandaloneConfiguration.setDatabase(db);
		redisStandaloneConfiguration.setPassword(RedisPassword.of(passWord));
		redisStandaloneConfiguration.setPort(port);

		JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jpcb = (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration
				// .usePooling()
				.builder();

		jpcb.poolConfig(jedisPoolConfig);
		JedisClientConfiguration jedisClientConfiguration = jpcb.build();

		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration,
				jedisClientConfiguration);
		jedisConnectionFactory.afterPropertiesSet();
		return jedisConnectionFactory;
	}

	private void setSerializer(RedisTemplate<String, String> template) {
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
	}

	public RedisTemplate<String, String> getRedisTemplateByDb(int db) {
		return redisTemplateMap.get(db);
	}

	public RedisTemplate<String, String> getRedisTemplate() {
		return redisTemplateMap.get(defaultDb);
	}

	@Scheduled(fixedRate = 5000)
	public void sendPingCommand() {
		// log.info("sendPingCommand");
		sendPingToAllConnections(redisTemplateMap);
	}

	public void sendPingToAllConnections(Map<Integer, RedisTemplate<String, String>> redisTemplateMap) {
		for (Map.Entry<Integer, RedisTemplate<String, String>> entry : redisTemplateMap.entrySet()) {
			RedisTemplate<String, String> redisTemplate = entry.getValue();

			JedisConnectionFactory jedisConnectionFactory = (JedisConnectionFactory) redisTemplate
					.getConnectionFactory();
			// log.info("UsePool:" + jedisConnectionFactory.getUsePool());

			RedisConnection redisConnection = jedisConnectionFactory.getConnection();
			String channel = "SYSTEM_PING_CHANNEL";
			String pingMessage = "{\"cmd\":\"ping\",\"message\":\"0\"}";
			redisConnection.publish(channel.getBytes(), pingMessage.getBytes());
			if (redisConnection instanceof JedisConnection) {

				Jedis jedis = (Jedis) ((JedisConnection) redisConnection).getNativeConnection();
				if (jedis != null) {
					try {
						String response = jedis.ping();
						// System.out.println("Response from redis server: " + response);
					} catch (Exception e) {
						e.printStackTrace();
						// Handle connection error
					}
				}
			}
		}
	}
}