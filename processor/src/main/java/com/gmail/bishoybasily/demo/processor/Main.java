package com.gmail.bishoybasily.demo.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws Exception {

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(User.class);
        enhancer.setCallback(new MethodInterceptor() {

            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                if (method.getName().equals("getName"))
                    return "bishoy";
                if (method.getName().equals("getAge"))
                    return 26;
                Object result = methodProxy.invokeSuper(o, args);

                // do some interception here

                return result;
            }
        });
        User user = (User) enhancer.create();

        System.out.println("before calling 1");
        String name = user.getName();

        System.out.println("before calling 2");
        Integer age = user.getAge();

        System.out.println(name + ", " + age);

        BeanGenerator beanGenerator = new BeanGenerator();

        beanGenerator.setSuperclass(Movie.class);

        beanGenerator.addProperty("name", String.class);
        beanGenerator.addProperty("released", Date.class);

        Movie bean = (Movie) beanGenerator.create();

        Object nameSetter = bean.getClass()
                .getMethod("setName", String.class)
                .invoke(bean, "movie 1");
        Object releasedSetter = bean.getClass()
                .getMethod("setReleased", Date.class)
                .invoke(bean, new Date());

        Object nameGetter = bean.getClass()
                .getMethod("getName")
                .invoke(bean);
        Object releasedGetter = bean.getClass()
                .getMethod("getReleased")
                .invoke(bean);

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println(objectMapper.writeValueAsString(bean));
        System.out.println(objectMapper.writeValueAsString(user));

    }

}
