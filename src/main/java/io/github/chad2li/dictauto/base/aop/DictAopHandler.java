package io.github.chad2li.dictauto.base.aop;

import cn.hutool.core.collection.CollUtil;
import io.github.chad2li.dictauto.base.annotation.DictId;
import io.github.chad2li.dictauto.base.cst.DictCst;
import io.github.chad2li.dictauto.base.dto.DictItemDto;
import io.github.chad2li.dictauto.base.properties.DictAutoProperties;
import io.github.chad2li.dictauto.base.service.IDictService;
import io.github.chad2li.dictauto.base.util.DictUtil;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 使用AOP拦截接口请求，给响应自动注入字典值
 *
 * @author chad
 * @date 2022/5/18 19:35
 * @since 1 create by chad
 */
@Aspect
@Order(DictAopHandler.AOP_ORDER_SEQ)
public class DictAopHandler {
    /**
     * springBean注入名称
     */
    public static final String SPRING_BEAN_NAME = DictCst.SPRING_BEAN_NAME_PREFIX + "DictAopHandler";
    /**
     * spring aop 执行顺序
     */
    public static final int AOP_ORDER_SEQ = 1024;

    private static final Logger log = LoggerFactory.getLogger(DictAopHandler.class);

    private IDictService<?, ?> dictService;
    private DictAutoProperties dictProps;

    public DictAopHandler(IDictService<?, ?> dictService, DictAutoProperties dictProps) {
        this.dictService = dictService;
        this.dictProps = dictProps;
    }

    /**
     * 自动解析并注入字典值
     * <p>
     * 1. 响应是否有 field 有 {@code DictId} 注解<br/>
     * 2. 解析字典id和类型<br/>
     * 3. 调用实现类，获取字典项<br/>
     * 4. 设置值<br/>
     * </p>
     *
     * @param result 方法响应结果
     */
    @AfterReturning(value = "@annotation(io.github.chad2li.dictauto.base.annotation.DictResult)",
            returning = "result")
    public void afterReturning(Object result) {
        try {
            log.debug("Dict classpath: {}", Objects.requireNonNull(DictAopHandler.class.getClassLoader().getResource(".")).getFile());
        } catch (Exception e) {
            log.debug("get classpath error", e);
        }
        // 1. 查询所有字典注解
        Set<DictId> dictSet = DictUtil.queryDictAnnotation(result);
        if (CollUtil.isEmpty(dictSet)) {
            return;
        }
        // 2. 指查询字典值
        String[] typeArray = dictSet.stream().map(DictId::type).distinct().toArray(String[]::new);
        List<? extends DictItemDto<?>> dictList = dictService.list(typeArray);
        // 3. 递归注入字典值
        DictUtil.injectionDict(result, DictUtil.dictMap(dictList), this.dictProps);
    }
}
