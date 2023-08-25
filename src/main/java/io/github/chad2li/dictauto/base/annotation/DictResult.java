package io.github.chad2li.dictauto.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动处理返回值的字典
 *
 * @author chad
 * @copyright 2023 chad
 * @since created at 2023/8/25 01:54
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DictResult {
}
