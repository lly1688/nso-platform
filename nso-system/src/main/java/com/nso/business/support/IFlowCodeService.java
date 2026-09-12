package com.nso.business.support;

import com.nso.business.core.NsoDtos.QrCodeBindingDto;
import com.nso.business.core.NsoDtos.ScanDetailDto;

import java.util.List;

/**
 * 业务流转码服务接口。
 */
public interface IFlowCodeService {

    /**
     * 确保项目流转码存在。
     *
     * @param projectId 项目编号
     * @return 流转码绑定信息
     */
    QrCodeBindingDto ensureProjectFlowCode(Long projectId);

    /**
     * 确保文档版本流转码存在。
     *
     * @param documentVersionId 文档版本编号
     * @return 流转码绑定信息
     */
    QrCodeBindingDto ensureDocumentVersionCode(Long documentVersionId);

    /**
     * 查询项目流转码。
     *
     * @param projectId 项目编号
     * @return 流转码绑定信息
     */
    QrCodeBindingDto findProjectFlowCode(Long projectId);

    /**
     * 解析扫码内容并校验权限。
     *
     * @param rawCode 原始扫码内容
     * @param permissions 当前用户权限
     * @return 扫码详情
     */
    ScanDetailDto resolveScan(String rawCode, List<String> permissions);
}
