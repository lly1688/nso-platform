package com.nso;

import com.nso.framework.web.filter.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void preservesASafeIncomingTraceIdAndExposesItToTheResponseAndMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/projects");
        request.addHeader(TraceIdFilter.HEADER, "pilot_trace-20260803");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(TraceIdFilter.currentTraceId()).isEqualTo("pilot_trace-20260803"));

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("pilot_trace-20260803");
        assertThat(TraceIdFilter.currentTraceId()).isNull();
    }

    @Test
    void replacesUnsafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/projects");
        request.addHeader(TraceIdFilter.HEADER, "invalid trace id!");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdFilter.HEADER)).matches("[A-Za-z0-9_-]{8,64}");
    }
}
