package com.lightconf.core;

import org.junit.Test;

/**
 * client test
 *
 * @author xuxueli 2018-02-01 20:02:56
 */
public class XxlConfClientTest {

    @Test
    public void clientTest() {
        String value = XxlConfClient.get("default.key01", null);
        System.out.println(value);
    }

}
