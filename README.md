**本项目于2023-09-21废弃**    
**请转用新项目 `auto-inject-all`: [Gitee](https://gitee.com/CodeinChad/auto-inject-all)|[Github](https://github.com/Chad2li/auto-inject-all)**

### 待办

- `1.` List，Map，Set中的字典属性 `[完成]`
- `2.` 字典集合值
- `3.` 自动配置加上条件：有 `IDictService` 的实现类 `[完成]`

### 记录

| 版本    | 创作者  | 时间         | 内容                                                 |
|-------|------|------------|----------------------------------------------------|
| 1.0   | chad | 2022-05-18 | 创建                                                 |
| 1.1   | chad | 2022-05-19 | 支持 iterable、map                                    |
| 1.1.1 | chad | 2022-05-22 | 条件启动Aop                                            |
| 1.2.0 | chad | 2022-06-17 | 1.跳过存在的DictItem属性                                  |
|       |      |            | 2.在lombok之前执行                                      |
| 1.2.1 | chad | 2022-06-24 | 1.自动生成setter和getter方法                              |
| 2.0.2 | chad | 2022-08-25 | 修复日志                                               |
| 2.0.3 | chad | 2022-08-25 | 修复字段非DictId结尾Bug                                   |
| 2.0.5 | chad | 2022-08-30 | 修复 sym.adr bug                                     |
| 3.0.0 | chad | 2023-08-25 | 1.不再使用JavaTree自动生成DictItem属性，需要手动生成对应的DictItem     |
|       |      |            | 2.支持parentId,targetField                           |
|       |      |            | 3.Aop改为拦截DictResult方法上的注解，不再只拦截Controller的public方法 |
|       |      |            | 4.将字典注解方法抽出DictUtil，方便手动调用注入字典值功能                  |

