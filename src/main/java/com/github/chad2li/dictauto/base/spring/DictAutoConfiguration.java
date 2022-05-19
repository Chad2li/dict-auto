package com.github.chad2li.dictauto.base.spring;

import com.github.chad2li.dictauto.base.aop.DictAopHandler;
import com.github.chad2li.dictauto.base.service.IDictService;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

/**
 * 自动配置 Dict 注入
 *
 * @author chad
 * @date 2022/5/19 12:34
 * @since 1 create by chad
 */
public class DictAutoConfiguration {
    @Resource(name = IDictService.SPRING_BEAN_NAME)
    private IDictService iDictService;

    @Bean(DictAopHandler.SPRING_BEAN_NAME)
    public DictAopHandler dictAopHandler() {
        return new DictAopHandler(this.iDictService);
    }
}
