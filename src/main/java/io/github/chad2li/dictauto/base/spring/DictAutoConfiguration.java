package io.github.chad2li.dictauto.base.spring;

import io.github.chad2li.dictauto.base.aop.DictAopHandler;
import io.github.chad2li.dictauto.base.service.IDictService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

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
@ConditionalOnBean(name = {IDictService.SPRING_BEAN_NAME})
public class DictAutoConfiguration {
    @Resource(name = IDictService.SPRING_BEAN_NAME)
    private IDictService iDictService;

    @Bean(DictAopHandler.SPRING_BEAN_NAME)
    public DictAopHandler dictAopHandler() {
        return new DictAopHandler(this.iDictService);
    }
}
