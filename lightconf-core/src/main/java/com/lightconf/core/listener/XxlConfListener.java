package com.lightconf.core.listener;

/**
 * xxl conf listener
 *
 * @author xuxueli 2018-02-04 01:27:30
 */
public interface XxlConfListener {

    /**
     * invoke when xxl conf change
     *
     * @param key
     */
    public void onChange(String key) throws Exception;

}
