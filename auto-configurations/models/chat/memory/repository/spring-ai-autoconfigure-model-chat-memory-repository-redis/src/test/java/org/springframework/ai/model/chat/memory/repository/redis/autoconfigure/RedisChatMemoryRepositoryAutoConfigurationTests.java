/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.model.chat.memory.repository.redis.autoconfigure;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisClientConfig;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link RedisChatMemoryRepositoryAutoConfiguration}.
 *
 * @author Vinay Reddy Kalluri
 */
class RedisChatMemoryRepositoryAutoConfigurationTests {

	@Test
	void sslIsDisabledByDefault() {
		RedisChatMemoryRepositoryProperties properties = bind(new MockEnvironment());

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null);

		assertThat(properties.getSsl().isEnabled()).isFalse();
		assertThat(config.isSsl()).isFalse();
		assertThat(config.getSslSocketFactory()).isNull();
	}

	@Test
	void credentialsAreApplied() {
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.repository.redis.username", "user")
					.withProperty("spring.ai.chat.memory.repository.redis.password", "secret"));

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null);

		assertThat(config.getUser()).isEqualTo("user");
		assertThat(config.getPassword()).isEqualTo("secret");
	}

	@Test
	void sslCanBeEnabled() {
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.repository.redis.ssl.enabled", "true"));

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null);

		assertThat(config.isSsl()).isTrue();
		assertThat(config.getSslSocketFactory()).isNull();
	}

	@Test
	void sslCanBeEnabledWithLegacyPrefix() {
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.redis.ssl.enabled", "true"));

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null);

		assertThat(config.isSsl()).isTrue();
	}

	@Test
	void sslBundleEnablesSslAndAppliesBundle() {
		SslOptions options = SslOptions.of(new String[] { "TLS_AES_128_GCM_SHA256" }, new String[] { "TLSv1.3" });
		DefaultSslBundleRegistry sslBundles = new DefaultSslBundleRegistry("redis",
				SslBundle.of(SslStoreBundle.NONE, null, options));
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.repository.redis.ssl.bundle", "redis"));

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, sslBundles);

		assertThat(config.isSsl()).isTrue();
		assertThat(config.getSslSocketFactory()).isNotNull();
		assertThat(config.getSslParameters()).isNotNull();
		assertThat(config.getSslParameters().getCipherSuites()).containsExactly("TLS_AES_128_GCM_SHA256");
		assertThat(config.getSslParameters().getProtocols()).containsExactly("TLSv1.3");
	}

	@Test
	void sslBundleIsIgnoredWhenSslIsExplicitlyDisabled() {
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.repository.redis.ssl.bundle", "redis")
					.withProperty("spring.ai.chat.memory.repository.redis.ssl.enabled", "false"));

		JedisClientConfig config = RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null);

		assertThat(config.isSsl()).isFalse();
		assertThat(config.getSslSocketFactory()).isNull();
	}

	@Test
	void sslBundleWithoutSslBundlesFails() {
		RedisChatMemoryRepositoryProperties properties = bind(
				new MockEnvironment().withProperty("spring.ai.chat.memory.repository.redis.ssl.bundle", "redis"));

		assertThatIllegalStateException()
			.isThrownBy(() -> RedisChatMemoryRepositoryAutoConfiguration.jedisClientConfig(properties, null))
			.withMessageContaining("redis");
	}

	private static RedisChatMemoryRepositoryProperties bind(MockEnvironment environment) {
		return new RedisChatMemoryRepositoryAutoConfiguration().redisChatMemoryRepositoryProperties(environment);
	}

}
