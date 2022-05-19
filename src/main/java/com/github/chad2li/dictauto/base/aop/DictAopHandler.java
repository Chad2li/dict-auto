package com.github.chad2li.dictauto.base.aop;

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

    private void injectionDict(Object result) {
        if (null == result) {
            logDebug("Result is null");
            return;
        }
        if (result instanceof DictItemDto) {
            logDebug("Skip {}", DictItemDto.class.getName());
            return;
        }
        Class resultCls = result.getClass();
        logDebug("Dict injection: {}", resultCls.getName());

        // result 为基本类型
        if (isBaseType(resultCls)) {
            logDebug("{} is base type", resultCls.getName());
            return;
        }
        // 循环解析属性
        Field[] fields = DictReflectUtil.getFieldsDirectlyHasGetter(resultCls, true);

//        Map<String, Field> fields = DictReflectUtil.getFieldMap(resultCls);
        if (ArrayUtil.isEmpty(fields)) {
            logDebug("{} has not any field", resultCls.getName());
            return;
        }

        logDebug("{} injection dict, field size: {}", resultCls.getName(), fields.length);

        for (Field field : fields) {
            injectionDict(result, field);
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
