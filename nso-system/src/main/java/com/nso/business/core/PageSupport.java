package com.nso.business.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.List;
import java.util.function.Function;

// MyBatis-Plus 分页与接口分页结果转换工具。
public final class PageSupport {

    private PageSupport() {
    }

    // 规范化分页参数。
    public static PageQuery normalize(PageQuery query) {
        return query == null ? new PageQuery() : query;
    }

    // 创建 MyBatis-Plus 分页对象。
    public static <T> Page<T> page(PageQuery query) {
        PageQuery normalized = normalize(query);
        return new Page<>(normalized.pageNoValue(), normalized.pageSizeValue());
    }

    // 转换数据库分页结果。
    public static <S, T> PageResult<T> result(Page<S> page, PageQuery query, Function<S, T> mapper) {
        PageQuery normalized = normalize(query);
        List<T> rows = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(rows, page.getTotal(), normalized.pageNoValue(), normalized.pageSizeValue());
    }

    // 对内存派生集合执行分页。 数据库列表接口应使用 {@link #page(PageQuery)} 和 {@link #result(Page, PageQuery, Function)}。
    public static <T> PageResult<T> slice(List<T> rows, PageQuery query) {
        PageQuery normalized = normalize(query);
        List<T> source = rows == null ? List.of() : rows;
        int from = (int) Math.min(Math.min(normalized.offset(), Integer.MAX_VALUE), source.size());
        int to = Math.min(from + normalized.pageSizeValue(), source.size());
        return new PageResult<>(source.subList(from, to), source.size(),
                normalized.pageNoValue(), normalized.pageSizeValue());
    }
}
