package star.core;

import org.apache.commons.lang3.ArrayUtils;
import star.annotation.Inject;
import star.exception.ImplementDuplicateException;
import star.factory.BeanFactory;
import star.factory.ConfigFactory;
import star.utils.CollectionUtil;
import star.utils.ReflectionUtil;
import star.utils.StringUtil;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author keshawn
 * @date 2017/11/9
 */
public final class IocCore {
    static {
        //获取所有的Bean类和Bean实例之间的映射关系
        Map<Class<?>, Object> beanMap = BeanFactory.getBeanMap();
        if (CollectionUtil.isNotEmpty(beanMap)) {
            for (Map.Entry<Class<?>, Object> beanEntry : beanMap.entrySet()) {
                Class<?> beanClass = beanEntry.getKey();
                Object beanInstance = beanEntry.getValue();
                dependencyInjection(beanClass, beanInstance);
            }
        }
    }

    public static void dependencyInjection(Class<?> beanClass, Object beanInstance) {
        Field[] beanFields = beanClass.getDeclaredFields();
        if (ArrayUtils.isNotEmpty(beanFields)) {
            //循环进行依赖注入
            for (Field beanField : beanFields) {
                fieldDependencyInjection(beanClass, beanInstance, beanField);
            }
        }
    }

    private static void fieldDependencyInjection(Class<?> beanClass, Object beanInstance, Field beanField) {
        if (beanField.isAnnotationPresent(Inject.class)) {
            Class<?> beanFieldClass = beanField.getType();
            //如果是接口，注入实现类，否则注入类自身
            if (beanFieldClass.isInterface()) {
                interfaceDependencyInjection(beanInstance, beanField, beanFieldClass);
            } else {
                Object beanFieldInstance = BeanFactory.getBean(beanFieldClass);
                if (beanFieldInstance != null) {
                    //通过反射初始化BeanField的值
                    ReflectionUtil.setField(beanInstance, beanField, beanFieldInstance);
                }
            }
        }
    }

    private static void interfaceDependencyInjection(Object beanInstance, Field beanField, Class<?> beanFieldClass) {
        //获取yml配置文件中接口与实现类beanId的映射关系
        Map<Class<?>, String> yamlImplementMapping = ConfigFactory.getImplementMapping();
        //先查看配置文件中是否配置了映射
        if (yamlImplementMapping.containsKey(beanFieldClass)) {
            Object beanFieldInstance = BeanFactory.getBean(yamlImplementMapping.get(beanFieldClass));
            if (beanFieldInstance != null) {
                //通过反射初始化BeanField的值
                ReflectionUtil.setField(beanInstance, beanField, beanFieldInstance);
            }
            return;
        }

        //获取所有带有@Service注解的类的接口与自身的映射关系
        Map<Class<?>, Class<?>> serviceMappingMap = BeanFactory.getServiceMappingMap();
        Inject inject = beanField.getAnnotation(Inject.class);
        String injectValue = inject.value();
        //如果使用@Inject注解未指定实现类，则从serviceMappingMap中寻找实现类进行注入
        if (StringUtil.isEmpty(injectValue)) {
            Class<?> implementClass = serviceMappingMap.get(beanFieldClass);
            //如果实现类为ImplementDuplicateException，则代表一个接口有多个实现类，而使用者又未指明使用那个实现类
            if (implementClass.equals(ImplementDuplicateException.class)) {
                throw new ImplementDuplicateException("cannot inject bean , this interface has more than one implement");
            } else {
                Object beanFieldInstance = BeanFactory.getBean(implementClass);
                if (beanFieldInstance != null) {
                    //通过反射初始化BeanField的值
                    ReflectionUtil.setField(beanInstance, beanField, beanFieldInstance);
                }
            }
        } else {
            //@Inject注解未指定实现类 bean id
            Object beanFieldInstance = BeanFactory.getBean(injectValue);
            if (beanFieldInstance != null) {
                //通过反射初始化BeanField的值
                ReflectionUtil.setField(beanInstance, beanField, beanFieldInstance);
            }
        }
    }
}