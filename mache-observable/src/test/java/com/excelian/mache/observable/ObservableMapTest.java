package com.excelian.mache.observable;

import com.excelian.mache.core.Mache;
import com.excelian.mache.events.MQFactory;
import com.excelian.mache.observable.utils.UuidUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ObservableMapTest {
    private int fired;
    private final UuidUtils uuidUtils = new UuidUtils();

    @Mock
    Mache<String, String> cache;

    @Mock
    MQFactory<String> mqFactory;

    @Test
    public void testGet() throws Exception {
        ObservableMap<String, String> observable = new ObservableMap<>(mqFactory, cache, uuidUtils);
        observable.registerListener(event -> fired++);

        observable.put("a", "b");
        assertEquals(1, fired);
    }

    @Test
    public void testPut() throws Exception {
        ObservableMap<String, String> observable = new ObservableMap<>(mqFactory, cache, uuidUtils);
        observable.registerListener(event -> fired++);

        observable.get("a");
        assertEquals(0, fired);
    }
}