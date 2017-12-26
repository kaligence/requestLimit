package com.ice.rql;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
//最高优先级
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface RequestLimit {
	/*
	 * 请求次数限制
	 */
	long requestCount() default 1L;
	/*
	 * 请求限制单位时间 /s
	 */
	long requestTime() default 60L;
}
