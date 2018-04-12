package com.lightconf.core.listener.impl;


import com.lightconf.core.listener.LightConfListener;
import com.lightconf.core.spring.LightConfFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * xxl conf annotaltion refresh
 *
 * @author xuxueli 2018-02-204 01:46:20
 */
public class BeanRefreshLightConfListener implements LightConfListener {


    // ---------------------- listener ----------------------

    // object + field
    public static class BeanField{
        private String beanName;
        private String property;

        public BeanField() {
        }

        public BeanField(String beanName, String property) {
            this.beanName = beanName;
            this.property = property;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    // key : object-field[]
    private static Map<String, List<BeanField>> key2BeanField = new ConcurrentHashMap<String, List<BeanField>>();
    public static void addBeanField(String key, BeanField beanField){
        List<BeanField> beanFieldList = key2BeanField.get(key);
        if (beanFieldList == null) {
            beanFieldList = new ArrayList<>();
            key2BeanField.put(key, beanFieldList);
        }
        for (BeanField item: beanFieldList) {
            if (item.getBeanName() == beanField.getBeanName() && item.getProperty()==beanField.getProperty()) {
                return;
            }
        }
        beanFieldList.add(beanField);
    }

    // ---------------------- onChange ----------------------

    @Override
    public void onChange(String key, String value) throws Exception {
        List<BeanField> beanFieldList = key2BeanField.get(key);
        if (beanFieldList!=null && beanFieldList.size()>0) {
            for (BeanField beanField: beanFieldList) {
//                LightConfFactory.refreshBeanField(beanField, value);
            }
        }
    }
}
