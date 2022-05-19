package com.github.chad2li.dictauto.base.aop;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.chad2li.dictauto.base.annotation.DictId;
import com.github.chad2li.dictauto.base.cst.DictCst;
import com.github.chad2li.dictauto.base.dto.DictItemDto;
import com.github.chad2li.dictauto.base.service.IDictService;
import com.github.chad2li.dictauto.base.util.DictReflectUtil;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Map;

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

    @Resource
    private IDictService dictService;

    public DictAopHandler(IDictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 对有 RestController 或 Controller 注解的public方法进行拦截
     */
    @Pointcut("(@within(org.springframework.web.bind.annotation.RestController)" +
            " || @within(org.springframework.stereotype.Controller))" +
            " && execution(public * *(..))")
    public void pointcut() {
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
     * @param result
     */
    @AfterReturning(value = "pointcut()", returning = "result")
    public void afterReturning(Object result) {
        // 递归注入字典值
        injectionDict(result);
    }


    private void injectionDict(Object result, Field field) {
        Class resultCls = result.getClass();
        DictId dictId = field.getAnnotation(DictId.class);
        if (null == dictId) {
            // 递归
            logDebug("{}.{} not {}", resultCls.getName(), field.getName(), DictId.class.getName());
            Object fieldValue = null;
            try {
                fieldValue = DictReflectUtil.getFieldValue(result, field);
            } catch (Exception ex) {
                throw new IllegalStateException(resultCls.getName() + "." + field.getName() + " get value error", ex);
            }

            // 深度解析
            injectionDict(fieldValue);
            return;
        }

        // dict field value
        Object fieldValue = null;
        try {
            fieldValue = DictReflectUtil.getFieldValue(result, field);
        } catch (Exception ex) {
            throw new IllegalStateException(resultCls.getName() + "." + field.getName() + " get value error", ex);
        }
        if (ObjectUtil.isEmpty(fieldValue)) {
            logDebug("{}.{} value is null", resultCls.getName(), field.getName());
            return;
        }

        // dict field type, nullable
        String dictType = dictId.type();

        // check DictItemDto field exists
        String dictName = field.getName();
        int suffixIndex = dictName.indexOf(DictCst.FIELD_DICT_ID_SUFFIX);
        if (suffixIndex > 0) {
            dictName = dictName.substring(0, suffixIndex);
        }
        String dictItemName = dictName + DictCst.FIELD_DICT_ITEM_SUFFIX;
        logDebug("{}.{} dict item name: {}", resultCls.getName(), field.getName(), dictItemName);

        if (!DictReflectUtil.hasField(resultCls, dictItemName)) {
            logDebug("{}.{} has not dict item name: {}", resultCls.getName(), field.getName(), dictItemName);
            return;
        }
        try {
            Object dictItemValue = DictReflectUtil.getFieldValue(result, dictItemName);
            if (null != dictItemValue) {
                logDebug("{}.{} value exists, skip auto injection", resultCls.getName(), dictItemName);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(resultCls.getName() + "." + dictItemName + " get value error", ex);
        }

        // 获取字典值
        DictItemDto dictItem = dictService.dict(fieldValue, dictType);
        if (null == dictItem) {
            throw new NullPointerException(resultCls.getName() + "." + field.getName() + " not found value, id:" + fieldValue + ", type:" + dictType);
        }
        try {
            DictReflectUtil.setFieldValue(result, dictItemName, dictItem);
        } catch (Exception ex) {
            throw new RuntimeException(resultCls.getName() + "." + dictItemName + " set value error", ex);
        }
    }

    /**
     * 解析对象，将其中有 {@link DictId}注解的属性，自动进行字典值注入
     *
     * @param dictObj 对象
     * @date 2022/5/19 13:13
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void injectionDict(Object dictObj) {
        if (null == dictObj) {
            logDebug("Result is null");
            return;
        }
        if (dictObj instanceof DictItemDto) {
            logDebug("Skip {}", DictItemDto.class.getName());
            return;
        }

        // iterable
        if (dictObj instanceof Iterable) {
            injectionIterable((Iterable) dictObj);
        } else if (dictObj instanceof Map) {
            injectionMap((Map) dictObj);
        } else {
            // other
            injectionObject(dictObj);
        }
    }

    /**
     * 解析 iterable，
     *
     * @param iterable 被解析的{@code iterable}对象
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void injectionIterable(Iterable iterable) {
        if (CollectionUtil.isEmpty(iterable)) {
            logDebug("{} empty", iterable.getClass().getName());
            return;
        }

        // 遍历 iterable
        iterable.forEach(i -> {
            injectionDict(i);
        });
    }

    /**
     * 解析Map，仅解析 value
     *
     * @param map 需要被解析的map
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void injectionMap(Map map) {
        if (CollectionUtil.isEmpty(map)) {
            logDebug("{} empty", map.getClass().getName());
            return;
        }

        // 遍历 iterable
        map.values().forEach(m -> {
            injectionDict(m);
        });
    }

    /**
     * 解析对象
     *
     * @param obj 需要解析的对象
     * @date 2022/5/19 13:06
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void injectionObject(Object obj) {
        Class resultCls = obj.getClass();
        logDebug("Dict injection: {}", resultCls.getName());

        // result 为基本类型
        if (isBaseType(resultCls)) {
            logDebug("{} is base type", resultCls.getName());
            return;
        }
        // 循环解析属性
        Field[] fields = DictReflectUtil.getFieldsDirectlyHasGetter(resultCls, true);
        if (ArrayUtil.isEmpty(fields)) {
            logDebug("{} has not any field", resultCls.getName());
            return;
        }

        logDebug("{} injection dict, field size: {}", resultCls.getName(), fields.length);
        for (Field field : fields) {
            injectionDict(obj, field);
        }
    }


    private boolean isBaseType(Class cls) {
        if (null == cls) {
            return true;
        }

        if (cls == int.class ||
                cls == Integer.class) {
            return true;
        }

        if (cls == boolean.class ||
                cls == Boolean.class) {
            return true;
        }

        if (cls == short.class ||
                cls == Short.class) {
            return true;
        }

        if (cls == byte.class ||
                cls == Byte.class) {
            return true;
        }

        if (cls == String.class) {
            return true;
        }

        if (cls == long.class ||
                cls == Long.class) {
            return true;
        }

        if (cls == double.class ||
                cls == Double.class) {
            return true;
        }

        if (cls == float.class ||
                cls == float.class) {
            return true;
        }

        if (cls == Object.class) {
            // Object也为基本类型
            return true;
        }

        // 非基本类型
        return false;
    }


    private void logDebug(String format, Object... params) {
        if (log.isDebugEnabled()) {
            log.debug(format, params);
        }
    }

    public IDictService getDictService() {
        return dictService;
    }

    public void setDictService(IDictService dictService) {
        this.dictService = dictService;
    }
}
