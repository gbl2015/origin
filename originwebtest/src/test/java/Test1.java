import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import star.bean.User;
import star.controller.TestController;
import star.core.LoadCore;
import star.dao.UserRepository;
import star.factory.BeanFactory;
import star.factory.ConfigFactory;
import star.factory.ConnectionFactory;
import star.proxy.CGLibProxy;
import star.proxy.DynamicProxy;
import star.repository.RepositoryProxy;
import star.service.TestService;
import star.service.impl.TestServiceImpl;
import star.service.impl.TestServiceImpl2;
import star.utils.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

import static star.core.IocCore.dependencyInjection;
import static star.utils.CastUtil.castString;

/**
 * @author keshawn
 * @date 2017/12/21
 */
public class Test1 {
    @Test
    public void testProxy() {
        TestService testService = new TestServiceImpl();
        DynamicProxy proxy = new DynamicProxy(testService);
        proxy.setBeforeSupplier(() -> System.currentTimeMillis());
        proxy.setBeforeConsumer(it -> System.out.println(it));
        proxy.setAfterSupplier(() -> System.currentTimeMillis());
        proxy.setAfterConsumer((before, after) -> System.out.println((long) after - (long) before));
        TestService service = proxy.getProxy();
        try {
            Method method = TestService.class.getMethod("hello");
            ReflectionUtil.invokeMethod(service, method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCGlib() {
        CGLibProxy proxy = CGLibProxy.getInstance();
        proxy.setBeforeSupplier(() -> System.currentTimeMillis());
        proxy.setBeforeConsumer(it -> System.out.println(it));
        proxy.setAfterSupplier(() -> System.currentTimeMillis());
        proxy.setAfterConsumer((before, after) -> System.out.println((long) after - (long) before));
        TestServiceImpl service = proxy.getProxy(TestServiceImpl.class);
        service.hello();
        TestServiceImpl2 testServiceImpl2 = CGLibProxy.getInstance().getProxy(TestServiceImpl2.class);
        testServiceImpl2.hello();
    }

    @Test
    public void testGuavaProxy() {
        TestService testService = new TestServiceImpl();
        TestService service = Reflection.newProxy(TestService.class, new DynamicProxy(testService));
        service.hello();
    }

    @Test
    public void testInterfaceProxy() {
        DynamicProxy proxy = new DynamicProxy(TestService.class);
        proxy.setBeforeSupplier(() -> System.currentTimeMillis());
        proxy.setBeforeConsumer(it -> System.out.println(it));
        proxy.setAfterSupplier(() -> System.currentTimeMillis());
        TestService testService = (TestService) java.lang.reflect.Proxy.newProxyInstance(TestService.class.getClassLoader(), new Class[]{TestService.class}, proxy);
        testService.hello();
    }

    @Test
    public void testJDBC() throws Exception {
        Connection connection = ConnectionFactory.getConnection();
        String sql = "insert into user values(?,?,?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setLong(1, 11L);
        preparedStatement.setString(2, "test");
        preparedStatement.setInt(3, 22);
        preparedStatement.setTimestamp(4,null);
        preparedStatement.setObject(5, null);
        int i = preparedStatement.executeUpdate();
        System.out.println(i);
        preparedStatement.close();
        connection.commit();
        ConnectionFactory.closeConnection();
    }

    @Test
    public void testSelect() throws Exception {
        Connection connection = ConnectionFactory.getConnection();
        String sql = "select * from user where id = 4";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        boolean execute = preparedStatement.execute();
        System.out.println(execute);
        ResultSet resultSet = preparedStatement.getResultSet();
        resultSet.next();
        String string = resultSet.getString(2);
        System.out.println(string);
        System.out.println(resultSet);
    }

    @Test
    public void testJson() {
//        User user = User.newBuilder().name("fkx").age(22).build();
//        String s = JsonUtil.encodeJson(user);
//        Map map = JsonUtil.decodeJson(s, Map.class);
//        map.put("test", "test");
//        String ss = JsonUtil.encodeJson(map);
//        System.out.println(ss);
//        User user1 = JsonUtil.decodeJson(ss, User.class);
//        System.out.println(JsonUtil.encodeJson(user1));
    }

    @Test
    public void testCast() {
        String ss = "user_id";
        System.out.println(StringUtil.underLineToCamel(ss));
        String s = StringUtil.underLineToCamel(ss);
        System.out.println(StringUtil.camelToUnderlineLowerCase(s));
    }

    @Test
    public void findById() {
        RepositoryProxy proxy = new RepositoryProxy(UserRepository.class);
        UserRepository userRepository = proxy.getProxy();
        User user = userRepository.findById(2L);
        System.out.println(JsonUtil.encodeJson(user));
    }
    @Test
    public void findByIds() {
        RepositoryProxy proxy = new RepositoryProxy(UserRepository.class);
        UserRepository userRepository = proxy.getProxy();
        List<User> users = userRepository.findByIdIn(Lists.newArrayList(1L,2L));
        System.out.println(JsonUtil.encodeJson(users));
    }
    @Test
    public void testDefault() {
        Boolean autoCast = ConfigFactory.getAutoCast();
        System.out.println(autoCast);
        System.out.println(-1L <= 0);
    }

    @Test
    public void testJsonList() {
//        User user1 = User.newBuilder().name("f").age(1).id(1L).build();
//        User user2 = User.newBuilder().name("k").age(2).id(2L).build();
//        User user3 = User.newBuilder().name("x").age(3).id(3L).build();
//        List<User> users = Lists.newArrayList(user1, user2, user3);
//        String json = JsonUtil.encodeJson(users);
//        System.out.println(json);
//        List<User> userList = decodeArrayJson(json, User.class);
//        System.out.println(userList);
    }

    @Test
    public void testType() throws Exception {
        Field age = User.class.getDeclaredField("age");
        Field id = User.class.getDeclaredField("id");
        System.out.println(age.getType());
        Class<?> type = age.getType();
        System.out.println(type);
        System.out.println(type == int.class);
        System.out.println(id.getType() == Long.class);
    }

    @Test
    public void prepare(){
        String sql = "select * from user where id = #{id} and name = #{name}";
        String[] between = StringUtils.substringsBetween(sql,"#{", "}");
        for (int i = 0; i < between.length; i++) {
            String s = between[i];
            System.out.println(i+s);
        }
        String s = StringUtils.replaceAll(sql, "\\#\\{([^\\\\}]+)\\}", "?");
        System.out.println(s);
    }

    @Test
    public void testCollection(){
        System.out.println(Collection.class.isAssignableFrom(Map.class));
    }

    @Test
    public void testMethodName(){
        String methodName = "findByNameInAndAge";

    }

    @Test
    public void findByNameInAndAge() {
        RepositoryProxy proxy = new RepositoryProxy(UserRepository.class);
        UserRepository userRepository = proxy.getProxy();
        List<User> users = userRepository.findByNameInAndAge(Lists.newArrayList("na","a"),22);
        System.out.println(JsonUtil.encodeJson(users));
    }

    @Test
    public void findByName(){
        RepositoryProxy proxy = new RepositoryProxy(UserRepository.class);
        UserRepository userRepository = proxy.getProxy();
        List<User> users = userRepository.findByName("test");
        System.out.println(JsonUtil.encodeJson(users));
    }
    /**
     * 获取所有的Bean类和Bean实例之间的映射关系
     */
    private static final Map<Class<?>, Object> BEAN_MAP = BeanFactory.getBeanMap();
    /**
     * 获取yml配置文件中接口与实现类beanId的映射关系
     */
    private static final Map<Class<?>, String> IMPLEMENT_MAPPING = ConfigFactory.getImplementMapping();
    /**
     * 获取所有带有@Service注解的类的接口与自身的映射关系
     */
    private static final Map<Class<?>, Class<?>> SERVICE_MAPPING = BeanFactory.getServiceMappingMap();
    @Test
    public void fixIoc(){
        if (CollectionUtil.isNotEmpty(BEAN_MAP)) {
            for (Map.Entry<Class<?>, Object> beanEntry : BEAN_MAP.entrySet()) {
                Class<?> beanClass = beanEntry.getKey();
                Object beanInstance = beanEntry.getValue();
                dependencyInjection(beanClass, beanInstance);
            }
        }
        TestController testController = (TestController)BEAN_MAP.get(TestController.class);
        testController.h();


    }

    @Test
    public void testInit(){
        LoadCore.init();
        TestController testController = (TestController)BeanFactory.getBean("testController");
        System.out.println(JsonUtil.encodeJson(testController));
        testController.h();

    }
}
