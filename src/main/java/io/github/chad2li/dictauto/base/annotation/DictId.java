package io.github.chad2li.dictauto.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author chad
 * @date 2022/5/13 22:51
 * @since 1 by chad at 2022/5/13<br/>
 * 2 by chad at 2023/8/25: 增加的target
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DictId {
    String type();

    String targetField() default "";

    String parentField() default "";

    /**
     * parent和parentField仅需填一个，parent优先级高于parentField
     * 仅支持 String 和 Long 类型
     */
    String parent() default "";
}
