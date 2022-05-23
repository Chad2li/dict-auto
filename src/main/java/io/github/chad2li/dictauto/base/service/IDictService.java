package io.github.chad2li.dictauto.base.service;

import io.github.chad2li.dictauto.base.annotation.DictId;
import io.github.chad2li.dictauto.base.cst.DictCst;
import io.github.chad2li.dictauto.base.dto.DictItemDto;

/**
 * 获取字典数据的服务
 *
 * @author chad
 * @date 2022/5/19 00:55
 * @since 1 create by chad
 */
public interface IDictService<I> {
    /**
     * springBean注入名称
     */
    String SPRING_BEAN_NAME = DictCst.SPRING_BEAN_NAME_PREFIX + "iDictService";

    /**
     * 根据字典ID和类型获取实际的字典数据
     *
     * @param dictId   字典ID，bean中标有 {@link DictId} 注解的属性值
     * @param dictType 字典类型，{@link DictId}中{@code type}值，可能为空
     * @return 需要自动注入的字典项值
     * @date 2022/5/19 00:56
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    DictItemDto<I> dict(I dictId, String dictType);
}
