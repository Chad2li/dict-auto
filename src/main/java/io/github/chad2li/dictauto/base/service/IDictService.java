package io.github.chad2li.dictauto.base.service;

import io.github.chad2li.dictauto.base.cst.DictCst;
import io.github.chad2li.dictauto.base.dto.DictItemDto;

import java.util.List;

/**
 * 获取字典数据的服务
 *
 * @author chad
 * @date 2022/5/19 00:55
 * @since 1 create by chad
 */
public interface IDictService<I, T extends DictItemDto<I>> {
    /**
     * springBean注入名称
     */
    String SPRING_BEAN_NAME = DictCst.SPRING_BEAN_NAME_PREFIX + "iDictService";

    /**
     * 批量查询type下的字典值
     *
     * @param type 字典类型
     * @return dict list
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    List<T> list(String... type);
}
