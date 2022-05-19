package com.github.chad2li.dictauto.base.dto;

import java.util.Objects;

/**
 * @author chad
 * @date 2022/5/18 00:12
 * @since 1 create by chad
 */
public class DictItemDto<I> {
    protected I id;
    protected String name;

    public I getId() {
        return id;
    }

    public void setId(I id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DictItemDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DictItemDto<?> that = (DictItemDto<?>) o;
        return getId().equals(that.getId()) &&
                getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }
}
