package com.nso;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    @Test
    void normalizesMissingAndInvalidValues() {
        assertThat(new PageQuery(null, null).pageNoValue()).isEqualTo(1);
        assertThat(new PageQuery(null, null).pageSizeValue()).isEqualTo(20);
        assertThat(new PageQuery(-4, 0).pageNoValue()).isEqualTo(1);
        assertThat(new PageQuery(-4, 0).pageSizeValue()).isEqualTo(20);
        assertThat(new PageQuery(2, 1000).pageSizeValue()).isEqualTo(100);
    }

    @Test
    void calculatesStableOffset() {
        PageQuery query = new PageQuery(3, 50);

        assertThat(query.offset()).isEqualTo(100L);
    }

    @Test
    void keepsTotalWhenPageIsPastEnd() {
        PageResult<Integer> result = PageSupport.slice(List.of(1, 2, 3), new PageQuery(4, 2));

        assertThat(result.list()).isEmpty();
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.pageNo()).isEqualTo(4);
        assertThat(result.pageSize()).isEqualTo(2);
    }
}
