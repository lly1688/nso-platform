package com.nso.business.sample.service;

import com.nso.business.core.NsoDtos.*;

public interface ISampleService {
    PageResult<SampleDto> list(Long projectId);
    SampleDto get(Long sampleId);
    SampleDto create(SampleRequest request);
    SampleDto submitConfirm(Long sampleId);
    SampleDto confirm(Long sampleId, SampleConfirmRequest request);
    PageResult<CustomerSampleDto> customerSamples(Long projectId);
    CustomerSampleDto confirmCustomer(Long sampleId, SampleConfirmRequest request);
    PageResult<SampleCheckDto> checks(Long sampleId);
    SampleCheckDto addCheck(Long sampleId, SampleCheckRequest request);
    ConfirmationTokenDto createConfirmToken(Long sampleId, int validDays, int maxUseCount);
    SpecialReleaseDto applySpecialRelease(Long sampleId, SpecialReleaseRequest request);
    SpecialReleaseDto approveSpecialRelease(Long releaseId);
    PublicSampleConfirmationDto publicConfirmation(String token);
    PublicSampleConfirmationDto submitPublicConfirmation(String token, SampleConfirmRequest request);
}
