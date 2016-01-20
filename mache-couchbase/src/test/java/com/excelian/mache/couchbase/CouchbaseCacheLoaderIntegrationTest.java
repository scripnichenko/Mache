package com.excelian.mache.couchbase;

import com.codeaffine.test.ConditionalIgnoreRule;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.excelian.mache.builder.NoMessagingProvisioner;
import com.excelian.mache.builder.storage.ConnectionContext;
import com.excelian.mache.core.Mache;
import com.google.common.cache.CacheLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;

import java.util.Objects;

import static com.couchbase.client.java.cluster.DefaultBucketSettings.builder;
import static com.excelian.mache.builder.MacheBuilder.mache;
import static com.excelian.mache.core.SchemaOptions.CREATE_AND_DROP_SCHEMA;
import static com.excelian.mache.couchbase.builder.CouchbaseProvisioner.couchbase;
import static com.excelian.mache.couchbase.builder.CouchbaseProvisioner.couchbaseConnectionContext;
import static org.junit.Assert.*;

@ConditionalIgnoreRule.IgnoreIf(condition = NoRunningCouchbaseDbForTests.class)
public class CouchbaseCacheLoaderIntegrationTest {

    @Rule
    public final ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String BUCKET = "couchbase-test";
    private static final String ADMIN_USER = "Administrator";
    private static final String PASSWORD = "password";
    private static final double DELTA = 0.000001;
    private static final String COUCHBASE_HOST = new NoRunningCouchbaseDbForTests().getHost();
    private static final DefaultCouchbaseEnvironment couchbaseEnvironment = DefaultCouchbaseEnvironment.create();

    private Mache<String, TestEntity> cache;
    private ConnectionContext<Cluster> connectionContext;

    @Before
    public void setup() throws Exception {

        connectionContext = couchbaseConnectionContext(COUCHBASE_HOST, couchbaseEnvironment);

        cache = mache(String.class, TestEntity.class)
                .backedBy(couchbase()
                        .withContext(connectionContext)
                        .withBucketSettings(builder().name(BUCKET).quota(150).build())
                        .withAdminDetails(ADMIN_USER, PASSWORD)
                        .withSchemaOptions(CREATE_AND_DROP_SCHEMA).build())
                .withMessaging(new NoMessagingProvisioner())
                .macheUp();
    }

    @After
    public void tearDown() throws Exception {
        cache.close();
        connectionContext.close();
    }

    @Test
    public void canPutAndGetValue() throws Throwable {
        cache.put("test1", new TestEntity("test1", "FXRATE", 0.91));
        assertEquals(0.91, cache.get("test1").value, DELTA);
    }

    @Test(expected = CacheLoader.InvalidCacheLoadException.class)
    public void canRemove() throws Throwable {
        TestEntity test2 = new TestEntity("test2", "FXRATE", 0.92);
        cache.put("test2", test2);
        assertEquals(test2, cache.get("test2"));
        cache.remove("test2");
        cache.get("test2");
    }

    @Test
    public void canOverwriteValue() throws Throwable {
        cache.put("test3", new TestEntity("test3", "FXRATE", 3.93));
        cache.put("test3", new TestEntity("test3", "FXRATE", 0.93));
        assertEquals(0.93, cache.get("test3").value, DELTA);
    }

    @Document
    public static class TestEntity {
        @Id
        String key;

        String type;

        double value;

        public TestEntity(String key, String type, double value) {
            this.key = key;
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            TestEntity that = (TestEntity) other;
            return Objects.equals(value, that.value)
                    && Objects.equals(key, that.key)
                    && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, type, value);
        }
    }

}

