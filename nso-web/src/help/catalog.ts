// 页面帮助主题的数据结构。
export interface HelpTopic {
    id: string;
    routeNames: string[];
    title: string;
    summary: string;
    steps: string[];
    faqs: Array<{
        question: string;
        answer: string;
    }>;
    roleTip?: string;
}

// 各业务页面的操作帮助内容。
export const helpTopics: HelpTopic[] = [
    {
        id: 'dashboard',
        routeNames: ['dashboard'],
        title: '仪表盘与待办',
        summary: '从待办、临期任务和风险提示开始安排当天工作。',
        steps: ['先看“高风险”和“临期超期”，确定今天要处理的事项。', '点击卡片、项目行或“去处理”进入对应业务页面。', '完成当前节点后返回仪表盘，确认待办数量和状态已刷新。'],
        faqs: [
            { question: '为什么看不到经营指标？', answer: '经营指标仅对拥有对应数据权限的管理角色显示。' },
            { question: '待办没有更新怎么办？', answer: '刷新页面后仍异常时，请从帮助面板提交支持工单。' }
        ],
        roleTip: '管理角色可结合风险概览优先处理阻断交付的问题。'
    },
    {
        id: 'projects',
        routeNames: ['projects', 'project-workspace'],
        title: '客户项目',
        summary: '项目是需求、技术文件、样品、变更和任务的协同载体。',
        steps: ['确认客户、产品、交期和项目负责人后创建项目。', '在项目工作台补齐成员与职责。', '按阶段推进技术包、样品、变更和交付准备。'],
        faqs: [
            { question: '无法新建项目？', answer: '请确认账号具备项目创建权限，并已维护客户主数据。' },
            { question: '项目负责人变更如何处理？', answer: '使用项目负责人转移功能，并在交接前确认待办和风险责任。' }
        ],
        roleTip: '项目负责人应先补齐关键责任人，再推进跨部门节点。'
    },
    {
        id: 'documents',
        routeNames: ['documents'],
        title: '技术文件',
        summary: '通过版本化文件、BOM、工艺路线和检验规范维持技术基线。',
        steps: ['上传文件并关联正确项目。', '填写版本号和变更摘要后提交。', '确认依赖资料完整后发布技术包。'],
        faqs: [
            { question: '为什么不能发布？', answer: '发布前需要满足版本、关联资料和权限校验。' },
            { question: '历史版本能否覆盖？', answer: '不能覆盖，应创建新版本并保留变更摘要。' }
        ],
        roleTip: '技术与工艺人员应核对技术版本是否已同步到相关任务。'
    },
    {
        id: 'samples',
        routeNames: ['samples'],
        title: '样品管理',
        summary: '管理样品计划、检验、客户确认和特殊放行。',
        steps: ['创建样品并绑定参考技术版本。', '录入检验结果和异常处置。', '提交客户确认或按流程申请特殊放行。'],
        faqs: [
            { question: '客户确认链接失效怎么办？', answer: '在样品详情重新生成并确认对应联系人授权有效。' },
            { question: '检验未完成能否提交确认？', answer: '不能，需先完成要求的质量检验。' }
        ],
        roleTip: '质量角色应优先处理待确认且接近交期的样品。'
    },
    {
        id: 'changes',
        routeNames: ['changes'],
        title: '变更中心',
        summary: '所有影响交期、成本、质量或技术基线的事项应通过变更闭环。',
        steps: ['说明变更前后内容及原因。', '发起影响分析并汇集部门反馈。', '审批后跟踪执行反馈，全部完成后关闭。'],
        faqs: [
            { question: '为什么不能直接关闭变更？', answer: '变更必须完成影响分析、审批和必要反馈。' },
            { question: '如何定位返工影响？', answer: '查看影响矩阵和关联任务的执行状态。' }
        ],
        roleTip: '变更负责人应明确每个受影响部门的完成证据。'
    },
    {
        id: 'tasks',
        routeNames: ['tasks', 'tasks-execution', 'tasks-risks', 'tasks-deliveries'],
        title: '任务与交付',
        summary: '用任务反馈、风险处置和交付预检保持项目节奏。',
        steps: ['从任务执行页确认自己的待办和截止时间。', '遇到阻塞时立即反馈进度或上报异常。', '交付前完成预检并记录客户反馈。'],
        faqs: [
            { question: '任务被版本阻断怎么办？', answer: '查看阻断原因，等待或推动对应技术版本发布。' },
            { question: '风险关闭后还能查看吗？', answer: '可以，在风险记录和审计时间轴中可追溯历史。' }
        ],
        roleTip: '执行人员应以真实完成时间和异常说明更新任务，避免只修改状态。'
    },
    {
        id: 'reports',
        routeNames: ['reports'],
        title: '统计分析',
        summary: '通过周期、项目和风险维度定位延期与返工的主要来源。',
        steps: ['先选择合适统计周期。', '从概览指标下钻到具体项目或风险。', '导出前确认筛选条件和数据权限。'],
        faqs: [
            { question: '报表数据延迟怎么办？', answer: '确认汇总任务执行状态；仍异常时提交支持工单。' }
        ]
    },
    {
        id: 'system',
        routeNames: ['system', 'system-accounts', 'system-organization', 'system-roles', 'system-rules', 'system-imports', 'system-operations', 'system-account-support'],
        title: '系统管理',
        summary: '维护账号、组织、权限、规则和平台运行治理。',
        steps: ['先维护人员档案、所属部门、岗位和系统角色。', '创建后按“待激活 → 已激活”完成临时密码设置，并提醒首次登录改密。', '账号交接校验通过后，再选择“停用”或“离职”，最后在运维审计中追溯结果。'],
        faqs: [
            { question: '用户首次登录被拦截？', answer: '管理员重置或激活账号后，用户必须先修改临时密码。' },
            { question: '技术支持工单由谁处理？', answer: '系统管理员在账户支持队列中处理密码恢复与技术支持请求。' }
        ],
        roleTip: '系统管理员处理恢复申请前必须完成人工身份核验。'
    },
    {
        id: 'profile',
        routeNames: ['profile'],
        title: '个人中心',
        summary: '维护个人资料、头像和登录密码。',
        steps: ['核对工作联系方式。', '首次登录或临时密码登录后立即修改密码。', '资料更新冲突时刷新后重新提交。'],
        faqs: [
            { question: '忘记当前密码怎么办？', answer: '退出后从登录页提交密码恢复申请。' }
        ]
    }
];

// 未匹配页面时使用的通用帮助主题。
const fallbackTopic: HelpTopic = {
    id: 'general',
    routeNames: [],
    title: '平台使用帮助',
    summary: '从当前页面的业务目标开始，按流程完成必填信息和状态流转。',
    steps: ['确认当前账号具备操作权限。', '按页面提示完成必要信息后再提交。', '遇到异常时记录现象并提交支持工单。'],
    faqs: [
        { question: '找不到需要的功能？', answer: '功能会随账号角色和权限动态显示，请联系系统管理员确认授权。' }
    ]
};

// 根据路由定位主题，并支持关键词检索。
export function topicForRoute(routeName?: string | symbol | null): HelpTopic {
    const normalized = typeof routeName === 'string' ? routeName : '';
    return helpTopics.find((topic) => topic.routeNames.includes(normalized)) || fallbackTopic;
}

export function searchTopics(keyword: string): HelpTopic[] {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
        return helpTopics;
    }
    return helpTopics.filter((topic) => [topic.title, topic.summary, ...topic.steps,
        ...topic.faqs.flatMap((faq) => [faq.question, faq.answer])]
        .join(' ').toLowerCase().includes(normalized));
}
