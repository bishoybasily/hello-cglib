package com.gmail.bishoybasily.demo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface Before {

    String value() default "";

}
