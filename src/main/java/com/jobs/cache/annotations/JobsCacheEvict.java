package com.jobs.cache.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JobsCacheEvict {

    String domain() default "";

    String domainKey() default "";

    String key() default "";

    String condition() default "";

}
