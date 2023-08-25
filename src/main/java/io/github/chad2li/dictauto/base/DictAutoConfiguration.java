package io.github.chad2li.dictauto.base;

import io.github.chad2li.dictauto.base.aop.DictAopHandler;
import io.github.chad2li.dictauto.base.properties.DictAutoProperties;
import io.github.chad2li.dictauto.base.service.IDictService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 自动配置 Dict 注入
 * <p>
 * 当spring环境中有名为 {@link IDictService#SPRING_BEAN_NAME} 的bean时，才启动配置
 * </p>
 *
 * @author chad
 * @date 2022/5/19 12:34
 * @since 1 create by chad
 */
@EnableAspectJAutoProxy
@EnableConfigurationProperties(DictAutoProperties.class)
@ConditionalOnBean(name = {IDictService.SPRING_BEAN_NAME})
public class DictAutoConfiguration {
    @Bean(DictAopHandler.SPRING_BEAN_NAME)
    public DictAopHandler dictAopHandler(@Qualifier(IDictService.SPRING_BEAN_NAME) IDictService iDictService,
                                         DictAutoProperties dictProps) {
        return new DictAopHandler(iDictService, dictProps);
    }
}
