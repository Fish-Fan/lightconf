package com.lightconf.core.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * light conf listener
 *
 * @author wuhaifei
 *
 * @date 2018-04-11
 */
public class LightConfListenerFactory {
    private static Logger logger = LoggerFactory.getLogger(LightConfListenerFactory.class);

    /**
     * light conf listener repository
     */
    private static ConcurrentHashMap<String, List<LightConfListener>> lightConfListenerRepository = new ConcurrentHashMap<>();
    private static List<LightConfListener> noKeyConfListener = new CopyOnWriteArrayList<>();

    /**
     * add listener with light conf change
     *
     * @param key
     * @param lightConfListener
     * @return
     */
    public static boolean addListener(String key, LightConfListener lightConfListener){
        if (lightConfListener == null) {
            return false;
        }

        if (key==null || key.trim().length()==0) {
            noKeyConfListener.add(lightConfListener);
            return true;
        } else {
            List<LightConfListener> listeners = lightConfListenerRepository.get(key);
            if (listeners == null) {
                listeners = new ArrayList<>();
                lightConfListenerRepository.put(key, listeners);
            }
            listeners.add(lightConfListener);
            return true;
        }
    }

    /**
     * invoke listener on light conf change
     *
     * @param key
     */
    public static void onChange(String key,String value) {
        if (key==null || key.trim().length()==0) {
            return;
        }

        List<LightConfListener> keyListeners = lightConfListenerRepository.get(key);
        if (keyListeners!=null && keyListeners.size()>0) {
            for (LightConfListener listener : keyListeners) {
                try {
                    listener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (noKeyConfListener.size() > 0) {
            for (LightConfListener confListener: noKeyConfListener) {
                try {
                    confListener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
