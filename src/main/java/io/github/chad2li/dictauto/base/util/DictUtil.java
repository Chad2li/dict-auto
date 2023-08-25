package io.github.chad2li.dictauto.base.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import io.github.chad2li.dictauto.base.annotation.DictId;
import io.github.chad2li.dictauto.base.dto.DictItemDto;
import io.github.chad2li.dictauto.base.properties.DictAutoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * dict util
 *
 * @author chad
 * @copyright 2023 chad
 * @since created at 2023/8/25 09:05
 */
@Slf4j
public class DictUtil {

    /**
     * 查询对象中所有DictId注解
     *
     * @param dictObj 对象
     * @return dictId
     * @date 2022/5/19 13:13
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static Set<DictId> queryDictAnnotation(Object dictObj) {
        return injectionDict(dictObj, true, null, null);
    }

    /**
     * 注入字典值
     *
     * @param dictObj 对象
     * @param dictMap 字典值map, key:
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    public static <I> void injectionDict(Object dictObj, Map<String, DictItemDto<I>> dictMap,
                                         DictAutoProperties dictProps) {
        injectionDict(dictObj, false, dictMap, dictProps);
    }

    /**
     * 解析对象，将其中有 {@link DictId}注解的属性，自动进行字典值注入
     *
     * @param dictObj 对象
     * @param isQuery 是否仅获取注解
     * @param dictMap 所有字典值
     * @date 2022/5/19 13:13
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static <I> Set<DictId> injectionDict(Object dictObj, boolean isQuery,
                                                @Nullable Map<String, DictItemDto<I>> dictMap,
                                                DictAutoProperties dictProps) {
        if (null == dictObj) {
            log.debug("Result is null");
            return Collections.emptySet();
        }
        if (dictObj instanceof DictItemDto) {
            log.debug("Skip {}", DictItemDto.class.getName());
            return Collections.emptySet();
        }
        Set<DictId> dictSet;
        if (dictObj instanceof Iterable) {
            // iterable
            dictSet = injectionIterable((Iterable<?>) dictObj, isQuery, dictMap, dictProps);
        } else if (dictObj instanceof Map) {
            // map
            dictSet = injectionMap((Map<?, ?>) dictObj, isQuery, dictMap, dictProps);
        } else {
            // other
            dictSet = injectionObject(dictObj, isQuery, dictMap, dictProps);
        }
        return CollUtil.newHashSet(dictSet);
    }


    /**
     * 解析特定属性
     *
     * @param dictObj 当前对象
     * @param field   对象属性
     * @param isQuery 是否仅获取注解
     * @param dictMap 所有字典值
     * @return 所有DictId注解
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    @Nullable
    private static <I> Set<DictId> injectionDict(Object dictObj, Field field, boolean isQuery,
                                                 @Nullable Map<String, DictItemDto<I>> dictMap,
                                                 DictAutoProperties dictProps) {
        Class<?> resultCls = dictObj.getClass();
        DictId dictId = field.getAnnotation(DictId.class);
        if (null == dictId) {
            // 递归
            log.debug("{}.{} not {}", resultCls.getName(), field.getName(), DictId.class.getName());
            Object fieldValue;
            try {
                fieldValue = DictReflectUtil.getFieldValue(dictObj, field);
            } catch (Exception ex) {
                throw new IllegalStateException(resultCls.getName() + "." + field.getName() + " get value error", ex);
            }

            // 深度解析
            return injectionDict(fieldValue, isQuery, dictMap, dictProps);
        }

        if (isQuery) {
            // 如果仅查询注解，则直接返回
            return Collections.singleton(dictId);
        }

        // dict field value
        I fieldValue;
        try {
            fieldValue = (I) DictReflectUtil.getFieldValue(dictObj, field);
        } catch (Exception ex) {
            throw new IllegalStateException(resultCls.getName() + "." + field.getName() + " get value error", ex);
        }
        if (ObjectUtil.isEmpty(fieldValue)) {
            log.debug("{}.{} value is null", resultCls.getName(), field.getName());
            return Collections.singleton(dictId);
        }

        // check DictItemDto field exists
        String dictItemName = getTargetName(dictId, field.getName(), dictProps);
        log.debug("{}.{} dict item name: {}", resultCls.getName(), field.getName(), dictItemName);

        if (!DictReflectUtil.hasField(resultCls, dictItemName)) {
            log.debug("{}.{} has not dict item name: {}", resultCls.getName(), field.getName(), dictItemName);
            return Collections.singleton(dictId);
        }
        try {
            Object dictItemValue = DictReflectUtil.getFieldValue(dictObj, dictItemName);
            if (null != dictItemValue) {
                log.debug("{}.{} value exists, skip auto injection", resultCls.getName(), dictItemName);
                return Collections.singleton(dictId);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(resultCls.getName() + "." + dictItemName + " get value error", ex);
        }
        // 解析 parent，可能为null
        String parentId = parseParentId(dictObj, dictId, dictProps);
        // 获取字典值
        DictItemDto<?> dictItem = getDict(dictMap, dictId.type(), parentId, fieldValue);
        if (null == dictItem) {
            throw new NullPointerException(resultCls.getName() + "." + field.getName()
                    + " not found value, id:" + fieldValue + ", type:" + dictId.type());
        }
        try {
            DictReflectUtil.setFieldValue(dictObj, dictItemName, dictItem);
        } catch (Exception ex) {
            throw new RuntimeException(resultCls.getName() + "." + dictItemName + " set value error", ex);
        }
        return Collections.singleton(dictId);
    }

    /**
     * 解析 iterable，
     *
     * @param iterable 被解析的{@code iterable}对象
     * @param isQuery  是否仅获取注解
     * @param dictMap  所有字典值
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private static <I> Set<DictId> injectionIterable(Iterable<?> iterable, boolean isQuery,
                                                     @Nullable Map<String, DictItemDto<I>> dictMap,
                                                     DictAutoProperties dictProps) {
        if (CollectionUtil.isEmpty(iterable)) {
            log.debug("{} empty", iterable.getClass().getName());
            return Collections.emptySet();
        }

        Set<DictId> dictSet = new HashSet<>(4);
        // 遍历 iterable
        Set<DictId> subDictSet;
        for (Object o : iterable) {
            subDictSet = injectionDict(o, isQuery, dictMap, dictProps);
            if (CollUtil.isNotEmpty(subDictSet)) {
                dictSet.addAll(subDictSet);
            }
        }
        return dictSet;
    }

    /**
     * 解析Map，仅解析 value
     *
     * @param map     需要被解析的map
     * @param isQuery 是否仅获取注解
     * @param dictMap 所有字典值
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private static <I> Set<DictId> injectionMap(Map<?, ?> map, boolean isQuery,
                                                @Nullable Map<String, DictItemDto<I>> dictMap,
                                                DictAutoProperties dictProps) {
        if (CollectionUtil.isEmpty(map)) {
            log.debug("{} empty", map.getClass().getName());
            return Collections.emptySet();
        }

        // 遍历 iterable
        return injectionIterable(map.values(), isQuery, dictMap, dictProps);
    }

    /**
     * 解析对象
     *
     * @param dictObj 需要解析的对象
     * @param isQuery 是否仅获取注解
     * @param dictMap 所有字典值
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private static <I> Set<DictId> injectionObject(Object dictObj, boolean isQuery,
                                                   @Nullable Map<String, DictItemDto<I>> dictMap,
                                                   DictAutoProperties dictProps) {
        Class<?> resultCls = dictObj.getClass();
        log.debug("Dict injection: {}", resultCls.getName());

        // dictObj 为基本类型
        if (isBaseType(resultCls)) {
            log.debug("{} is base type", resultCls.getName());
            return Collections.emptySet();
        }
        // 循环解析属性
        Field[] fields = DictReflectUtil.getFieldsDirectlyHasGetter(resultCls, true);
        if (ArrayUtil.isEmpty(fields)) {
            log.debug("{} has not any field", resultCls.getName());
            return Collections.emptySet();
        }

        log.debug("{} injection dict, field size: {}", resultCls.getName(), fields.length);
        Set<DictId> dictSet = new HashSet<>(4);
        Set<DictId> subDictSet;
        for (Field field : fields) {
            subDictSet = injectionDict(dictObj, field, isQuery, dictMap, dictProps);
            if (null != subDictSet && !subDictSet.isEmpty()) {
                dictSet.addAll(subDictSet);
            }
        }
        return dictSet;
    }

    /**
     * 获取字典值注入的 fieldName
     *
     * @param dictId   dictId annotation
     * @param dictName 字典fieldName
     * @return 注入目标字段的名称，默认为 dictFieldName去年 DictId（如果有该后缀）再拼接上DictItem
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    private static String getTargetName(DictId dictId, String dictName,
                                        DictAutoProperties dictProps) {
        String targetName = dictId.targetField();
        if (CharSequenceUtil.isNotEmpty(targetName)) {
            return targetName.trim();
        }
        // 自动拼接
        int suffixIndex = dictName.indexOf(dictProps.getDictIdSuffix());
        if (suffixIndex > 0) {
            dictName = dictName.substring(0, suffixIndex);
        }
        return dictName + dictProps.getDictItemSuffix();
    }

    /**
     * 判断类是否为基础类型
     *
     * @param cls cls
     * @return true是基础类，不注入字典值
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    public static boolean isBaseType(@Nullable Class<?> cls) {
        if (null == cls) {
            return true;
        }
        if (cls.isAssignableFrom(Enum.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Integer.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Boolean.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Short.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Byte.class)) {
            return true;
        }
        if (cls.isAssignableFrom(String.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Long.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Double.class)) {
            return true;
        }
        if (cls.isAssignableFrom(Float.class)) {
            return true;
        }
        // Object也为基本类型
        return cls == Object.class;
    }


    /**
     * @param dict dict
     * @return type/parentId/id
     * @author chad
     * @see DictUtil#dictKey(String, Object, Object)
     * @since 1 by chad at 2023/8/25
     */
    public static <I> String dictKey(DictItemDto<I> dict) {
        if (null == dict) {
            return "";
        }
        return dictKey(dict.getType(), dict.getParentId(), dict.getId());
    }

    /**
     * 拼接字典key，用于快捷获取字典值
     *
     * @param type     dict type
     * @param parentId dict parent id
     * @param dictId   dict id
     * @return [type/][parentId/]id
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    public static <I> String dictKey(String type, Object parentId, I dictId) {
        String dictKey = "";
        // type
        type = null != type ? type.trim() : "";
        if (!type.isEmpty()) {
            dictKey += type + "/";
        }
        // parentId
        String parentIdStr = null != parentId ? String.valueOf(parentId).trim() : "";
        if (!parentIdStr.isEmpty()) {
            dictKey += parentIdStr + "/";
        }
        // id
        return dictKey + String.valueOf(dictId).trim();
    }

    /**
     * list 转 map
     *
     * @param dictList list
     * @return key: {@link DictUtil#dictKey(DictItemDto)}
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    public static <I> Map<String, DictItemDto<I>> dictMap(List<? extends DictItemDto<?>> dictList) {
        if (CollUtil.isEmpty(dictList)) {
            return Collections.emptyMap();
        }

        return (Map<String, DictItemDto<I>>) dictList.stream()
                .collect(Collectors.toMap(it -> DictUtil.dictKey(it), Function.identity()));
    }

    /**
     * 获取字典值
     *
     * @param dictMap  字典分组,key: {@link DictUtil#dictKey(DictItemDto)}
     * @param type     dict type
     * @param parentId dict parent id
     * @param dictId   dict id
     * @return dict dto
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    @Nullable
    public static <I> DictItemDto<?> getDict(Map<String, DictItemDto<I>> dictMap, String type,
                                             String parentId, I dictId) {
        if (ObjectUtil.hasEmpty(dictMap, dictId)) {
            return null;
        }
        // 获取值
        // 获取parent值，如果有
        return dictMap.get(dictKey(type, parentId, dictId));
    }

    /**
     * 解析 parentId
     *
     * @param dictObj 当前 dictId 所属的对象，注意并不是 DictId 注解标注的属性，是属性所在的对象
     * @param dictId  DictId注解
     * @return parentId or null if not parent
     * @author chad
     * @since 1 by chad at 2023/8/25
     */
    @Nullable
    public static String parseParentId(Object dictObj, DictId dictId,
                                       DictAutoProperties dictProps) {
        if (ObjectUtil.hasEmpty(dictObj, dictId)) {
            return null;
        }
        if (CharSequenceUtil.isNotEmpty(dictId.parent())) {
            // parent优先级高于parentField
            return dictId.parent();
        }
        String parentFieldName = dictId.parentField();
        if (CharSequenceUtil.isEmpty(parentFieldName)) {
            // parent和parentField都无值，则返回配置值
            return dictProps.getDefaultParentId();
        }
        Object parentId = ReflectUtil.getFieldValue(dictObj, parentFieldName);
        if (null == parentId) {
            return null;
        }
        return String.valueOf(parentId).trim();
    }

    private DictUtil() {
        // do nothing
    }
}
