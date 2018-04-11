package com.lightconf.core.spring;

import com.lightconf.core.XxlConfClient;
import com.lightconf.core.annotaion.XxlConf;
import com.lightconf.core.core.XxlConfLocalCacheConf;
import com.lightconf.core.core.XxlConfZkConf;
import com.lightconf.core.exception.XxlConfException;
import com.lightconf.core.listener.XxlConfListenerFactory;
import com.lightconf.core.listener.impl.BeanRefreshXxlConfListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * rewrite PropertyPlaceholderConfigurer
 *
 * @author xuxueli 2015-9-12 19:42:49
 */
public class XxlConfFactory extends PropertySourcesPlaceholderConfigurer {
	private static Logger logger = LoggerFactory.getLogger(XxlConfFactory.class);

	// ---------------------- init/destroy ----------------------

	public void init(){
		XxlConfListenerFactory.addListener(null, new BeanRefreshXxlConfListener());    // listener all key change
	}
	public void destroy(){
		XxlConfLocalCacheConf.destroy();	// destroy ehcache
		XxlConfZkConf.destroy();			// destroy zk client
	}


	// ---------------------- spring xml/annotation conf ----------------------

	private static final String placeholderPrefix = "${";
	private static final String placeholderSuffix = "}";
	private static boolean xmlKeyValid(String originKey){
		boolean start = originKey.startsWith(placeholderPrefix);
		boolean end = originKey.endsWith(placeholderSuffix);
		if (start && end) {
			return true;
		}
		return false;
	}
	private static String xmlKeyParse(String originKey){
		if (xmlKeyValid(originKey)) {
			// replace by xxl-conf
			String key = originKey.substring(placeholderPrefix.length(), originKey.length() - placeholderSuffix.length());
			return key;
		}
		return null;
	}

	/**
	 * refresh bean with xxl conf (fieldNames)
	 */
	public static void refreshBeanField(BeanRefreshXxlConfListener.BeanField beanField, String value){
		Object bean = beanFactory.getBean(beanField.getBeanName());		// TODO，springboot环境下，通过该方法 "getBean" 获取获取部分Bean，如Spring和Jackson等组件的Bean 会报错。原因未知；
		if (bean != null) {
			BeanWrapper beanWrapper = new BeanWrapperImpl(bean);

			// property descriptor
			PropertyDescriptor propertyDescriptor = null;
			PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
			if (propertyDescriptors!=null && propertyDescriptors.length>0) {
				for (PropertyDescriptor item: propertyDescriptors) {
					if (beanField.getProperty().equals(item.getName())) {
						propertyDescriptor = item;
					}
				}
			}

			// refresh field: set or field
			if (propertyDescriptor!=null && propertyDescriptor.getWriteMethod() != null) {
				beanWrapper.setPropertyValue(beanField.getProperty(), value);
				logger.info(">>>>>>>>>>> xxl-conf, refreshBeanField[set] success, {}#{}:{}",
						beanField.getBeanName(), beanField.getProperty(), value);
			} else {
				Field[] beanFields = bean.getClass().getDeclaredFields();
				if (beanFields!=null && beanFields.length>0) {
					for (Field fieldItem: beanFields) {
						if (beanField.getProperty().equals(fieldItem.getName())) {
							fieldItem.setAccessible(true);
							try {
								fieldItem.set(bean, value);
								logger.info(">>>>>>>>>>> xxl-conf, refreshBeanField[field] success, {}#{}:{}",
										beanField.getBeanName(), beanField.getProperty(), value);
							} catch (IllegalAccessException e) {
								throw new XxlConfException(e);
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, ConfigurablePropertyResolver propertyResolver) throws BeansException {
		//super.processProperties(beanFactoryToProcess, propertyResolver);

		// BeanDefinitionVisitor
		/*BeanDefinitionVisitor beanDefinitionVisitor = new BeanDefinitionVisitor(new StringValueResolver() {
			@Override
			public String resolveStringValue(String strVal) {
				if (xmlKeyValid(strVal)) {
					// object + property
					String confKey = xmlKeyParse(strVal);
					String confValue = XxlConfClient.get(confKey, "");

					return confValue;
				}
				return strVal;
			}
		});
		beanDefinitionVisitor.visitBeanDefinition(beanDefinition);*/

		// visit bean definition
		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		if (beanNames != null && beanNames.length > 0) {
			for (final String beanName : beanNames) {
				if (!(beanName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {

					// 1、XML('${...}')：resolves placeholders + watch
					BeanDefinition beanDefinition = beanFactoryToProcess.getBeanDefinition(beanName);

                    MutablePropertyValues pvs = beanDefinition.getPropertyValues();
                    PropertyValue[] pvArray = pvs.getPropertyValues();
					for (PropertyValue pv : pvArray) {
						if (pv.getValue() instanceof TypedStringValue) {
							String propertyName = pv.getName();
							String typeStringVal = ((TypedStringValue) pv.getValue()).getValue();
							if (xmlKeyValid(typeStringVal)) {

								// object + property
								String confKey = xmlKeyParse(typeStringVal);
								String confValue = XxlConfClient.get(confKey, "");

								// resolves placeholders
								pvs.add(pv.getName(), confValue);

								// watch
								BeanRefreshXxlConfListener.BeanField beanField = new BeanRefreshXxlConfListener.BeanField(beanName, propertyName);
								BeanRefreshXxlConfListener.addBeanField(confKey, beanField);
							}
						}
					}

					// 2、Annotation('@XxlConf')：resolves conf + watch
					if (beanDefinition.getBeanClassName() == null) {
						continue;
					}
					Class beanClazz = null;
					try {
						beanClazz = Class.forName(beanDefinition.getBeanClassName());
					} catch (ClassNotFoundException e) {
						logger.error(">>>>>>>>>>> xxl-conf, annotation bean class invalid, error msg:{}", e.getMessage());
					}
					if (beanClazz == null) {
						continue;
					}
					ReflectionUtils.doWithFields(beanClazz, new ReflectionUtils.FieldCallback() {
						@Override
						public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
							if (field.isAnnotationPresent(XxlConf.class)) {
								String propertyName = field.getName();
								XxlConf xxlConf = field.getAnnotation(XxlConf.class);

								String confKey = xxlConf.value();
								String confValue = XxlConfClient.get(confKey, xxlConf.defaultValue());


								// resolves placeholders
								BeanRefreshXxlConfListener.BeanField beanField = new BeanRefreshXxlConfListener.BeanField(beanName, propertyName);
								refreshBeanField(beanField, confValue);

								// watch
								if (xxlConf.callback()) {
									BeanRefreshXxlConfListener.addBeanField(confKey, beanField);
								}

								/*field.setAccessible(true);
								field.set(beanWithXxlConf, confValue);*/
							}
						}
					});
				}
			}
		}

		logger.info(">>>>>>>>>>> xxl-conf, XxlConfFactory process success");
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private String beanName;
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	private static BeanFactory beanFactory;
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		super.setIgnoreUnresolvablePlaceholders(true);
	}

}
