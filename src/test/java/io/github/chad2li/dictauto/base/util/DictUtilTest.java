package io.github.chad2li.dictauto.base.util;

import io.github.chad2li.dictauto.base.annotation.DictId;
import io.github.chad2li.dictauto.base.cst.DictCst;
import io.github.chad2li.dictauto.base.dto.DictItemDto;
import io.github.chad2li.dictauto.base.properties.DictAutoProperties;
import lombok.ToString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DictUtilTest
 *
 * @author chad
 * @copyright 2023 chad
 * @since created at 2023/8/25 15:21
 */
public class DictUtilTest {
    private DictAutoProperties dictProps;

    @Before
    public void before() {
        dictProps = new DictAutoProperties();
        dictProps.setDefaultParentId(DictCst.DEFAULT_PARENT_ID);
        dictProps.setDictIdSuffix(DictCst.FIELD_DICT_ID_SUFFIX);
        dictProps.setDictItemSuffix(DictCst.FIELD_DICT_ITEM_SUFFIX);
    }

    @Test
    public void queryDictAnnotation() {
        DemoVo demo = demoVo(true);
        // 1.
        Set<DictId> dictIdSet = DictUtil.queryDictAnnotation(demo);
        Set<String> typeSet = dictIdSet.stream().map(DictId::type).collect(Collectors.toSet());
        Assert.assertEquals(2, typeSet.size());
        // role没有getter方法
        Assert.assertTrue(typeSet.contains("gender"));
        Assert.assertTrue(typeSet.contains("city"));
        // 2.
        DictUtil.injectionDict(demo, dictMap(), dictProps);
        assertDemo(demo);
        // - list
        assertDemo(demo.getList().get(0));
        assertDemo(demo.getList().get(1));
        assertDemo(demo.getList().get(2));
        for (DemoVo demoI : demo.getSet()) {
            assertDemo(demoI);
        }
        for (DemoVo demoI : demo.getMap().values()) {
            assertDemo(demoI);
        }
    }

    private void assertDemo(DemoVo demo) {
        Assert.assertEquals("男", demo.getGenderDictItem().getName());
        Assert.assertEquals("浙江", demo.getProvinceDict().getName());
        Assert.assertEquals("杭州", demo.getCityDict().getName());
        Assert.assertNull(demo.getRoleDictItem());
    }


    private Map<String, DictItemDto<String>> dictMap() {
        Map<String, DictItemDto<String>> dictMap = new HashMap<>();
        // gender
        DictItemDto<String> male = dict("1", "0", "gender", "男");
        DictItemDto<String> female = dict("2", "0", "gender", "女");
        DictItemDto<String> unknown = dict("0", "0", "gender", "未知");
        putDictMap(dictMap, male);
        putDictMap(dictMap, female);
        putDictMap(dictMap, unknown);
        // city
        DictItemDto<String> zhejiang = dict("zhejiang", "0", "city", "浙江");
        DictItemDto<String> anhui = dict("anhui", "0", "city", "安徽");
        DictItemDto<String> hangzhou = dict("hangzhou", "zhejiang", "city", "杭州");
        DictItemDto<String> yiwu = dict("yiwu", "zhejiang", "city", "义乌");
        DictItemDto<String> hefei = dict("hefei", "anhui", "city", "合肥");
        putDictMap(dictMap, zhejiang);
        putDictMap(dictMap, anhui);
        putDictMap(dictMap, hangzhou);
        putDictMap(dictMap, yiwu);
        putDictMap(dictMap, hefei);
        // role
        DictItemDto<String> admin = dict("normal", "0", "role", "管理员");
        DictItemDto<String> normal = dict("admin", "0", "role", "普通用户");
        putDictMap(dictMap, admin);
        putDictMap(dictMap, normal);
        return dictMap;
    }

    private void putDictMap(Map<String, DictItemDto<String>> dictMap, DictItemDto<String> dict) {
        dictMap.put(DictUtil.dictKey(dict), dict);
    }

    private DictItemDto<String> dict(String id, String parentId, String type, String name) {
        return new DictItemDto<>(id, parentId, type, name);
    }


    private DemoVo demoVo(boolean isRoot) {
        DemoVo demoVo = new DemoVo();
        demoVo.setGender("1");
        demoVo.setProvince("zhejiang");
        demoVo.setCity("hangzhou");
        demoVo.role = "Normal";
        if (!isRoot) {
            return demoVo;
        }
        List<DemoVo> list = new ArrayList<>(3);
        demoVo.setList(list);
        for (int i = 0; i < 3; i++) {
            list.add(demoVo(false));
        }
        Map<String, DemoVo> map = new HashMap<>(3);
        demoVo.setMap(map);
        for (int i = 0; i < 3; i++) {
            map.put(String.valueOf(i), demoVo(false));
        }
        Set<DemoVo> set = new HashSet<>(3);
        demoVo.setSet(set);
        for (int i = 0; i < 3; i++) {
            set.add(demoVo(false));
        }

        return demoVo;
    }

    @ToString
    public class DemoVo {
        @DictId(type = "gender", parent = "0")
        private String gender;
        @DictId(type = "city", parent = "0", targetField = "provinceDict")
        private String province;
        @DictId(type = "city", parentField = "province", targetField = "cityDict")
        private String city;
        /**
         * 无get方法属性
         */
        @DictId(type = "role")
        private String role;
        private List<DemoVo> list;
        private Map<String, DemoVo> map;
        private Set<DemoVo> set;
        private DictItemDto<String> genderDictItem;
        private DictItemDto<String> provinceDict;
        private DictItemDto<String> cityDict;
        private DictItemDto<String> roleDictItem;

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public List<DemoVo> getList() {
            return list;
        }

        public void setList(List<DemoVo> list) {
            this.list = list;
        }

        public Map<String, DemoVo> getMap() {
            return map;
        }

        public void setMap(Map<String, DemoVo> map) {
            this.map = map;
        }

        public Set<DemoVo> getSet() {
            return set;
        }

        public void setSet(Set<DemoVo> set) {
            this.set = set;
        }

        public DictItemDto<String> getGenderDictItem() {
            return genderDictItem;
        }

        public void setGenderDictItem(DictItemDto<String> genderDictItem) {
            this.genderDictItem = genderDictItem;
        }

        public DictItemDto<String> getProvinceDict() {
            return provinceDict;
        }

        public void setProvinceDict(DictItemDto<String> provinceDict) {
            this.provinceDict = provinceDict;
        }

        public DictItemDto<String> getCityDict() {
            return cityDict;
        }

        public void setCityDict(DictItemDto<String> cityDict) {
            this.cityDict = cityDict;
        }

        public DictItemDto<String> getRoleDictItem() {
            return roleDictItem;
        }

        public void setRoleDictItem(DictItemDto<String> roleDictItem) {
            this.roleDictItem = roleDictItem;
        }
    }
}