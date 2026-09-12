import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(scriptDir, '..');

function read(relativePath) {
    return fs.readFileSync(path.join(webRoot, relativePath), 'utf8');
}

test('dashboard analysis and workspace use one shared column template', () => {
    const css = read('src/styles/pages/dashboard.css');
    assert.match(css, /--dashboard-column-template\s*:/);
    assert.match(css, /\.dashboard-analysis[\s\S]*grid-template-columns:\s*var\(--dashboard-column-template\)/);
    assert.match(css, /\.dashboard-workspace[\s\S]*grid-template-columns:\s*var\(--dashboard-column-template\)/);
});

test('technical document tabs expose a full-width content region', () => {
    const template = read('src/views/document/index.vue');
    const css = read('src/styles/pages/document.css');
    assert.match(template, /class="document-tab-content"/);
    assert.match(css, /\.document-tab-content[\s\S]*width:\s*100%/);
    assert.match(css, /\.document-tab-content[\s\S]*\.el-table[\s\S]*width:\s*100%/);
    assert.match(template, /<el-table-column prop="bomNo"[^>]*min-width=/);
    assert.match(template, /<el-table-column prop="routeNo"[^>]*min-width=/);
    assert.match(template, /<el-table-column prop="specNo"[^>]*min-width=/);
});

test('task risks expose a cockpit layout with summary, queue, and reporting areas', () => {
    const template = read('src/views/task/risks.vue');
    const css = read('src/styles/pages/task.css');
    assert.match(template, /task-risk-dashboard/);
    assert.match(template, /task-risk-summary/);
    assert.match(template, /task-risk-queue/);
    assert.match(template, /task-risk-report/);
    assert.match(css, /\.task-risk-dashboard\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+minmax\(320px,\s*380px\)/);
});

test('system panel grids stretch sibling content areas', () => {
    const css = read('src/styles/pages/system.css');
    assert.match(css, /\.system-panel-grid[\s\S]*align-items:\s*stretch/);
    assert.match(css, /\.system-panel-grid\s*>\s*\.panel[\s\S]*display:\s*flex/);
});

test('pagination controls can wrap inside narrow panels', () => {
    const css = read('src/styles/02-shared.css');
    assert.match(css, /\.pagination-bar[\s\S]*flex-wrap:\s*wrap/);
    assert.match(css, /\.pagination-bar\s*\{[^}]*min-width:\s*0/);
    assert.match(css, /\.pagination-bar\s*>\s*\.el-pagination[\s\S]*min-width:\s*0/);
    assert.match(css, /\.pagination-bar\s*>\s*\.el-pagination\s+\.el-pager[\s\S]*min-width:\s*0/);
});

test('account type presentation treats legacy external accounts as external', () => {
    const template = read('src/views/system/accounts.vue');
    assert.match(template, /EXTERNAL_LEGACY/);
    assert.match(template, /userType === 'EXTERNAL' \|\| row\.userType === 'EXTERNAL_LEGACY'/);
});

test('system subnavigation declares backend permissions for every page', () => {
    const shared = read('src/views/system/shared.ts');
    const requiredPages = [
        'sys:user:read',
        'sys:dept:read',
        'sys:role:read',
        'sys:authorization:audit'
    ];
    for (const permission of requiredPages) {
        assert.match(shared, new RegExp(`permission: '${permission}'`));
    }
    assert.match(shared, /system-rules[\s\S]*roles: \['admin'\]/);
    assert.match(shared, /system-imports[\s\S]*roles: \['admin'\]/);
});

test('Element Plus registers tooltip and step components used by system views', () => {
    const plugin = read('src/plugins/element-plus.ts');
    assert.match(plugin, /ElTooltip/);
    assert.match(plugin, /ElStep/);
    assert.match(plugin, /ElSteps/);
});

test('account profile save does not silently replace roles', () => {
    const template = read('src/views/system/accounts.vue');
    const saveBlock = template.match(/async function saveUser\(row: SystemUser\) \{[\s\S]*?\n\}/)?.[0] || '';
    assert.doesNotMatch(saveBlock, /roleCodes:\s*row\.roles/);
    assert.match(template, /replaceSystemUserRoles/);
});

test('token refresh notifies the auth store to update in-memory session', () => {
    const request = read('src/utils/request.ts');
    const app = read('src/App.vue');
    assert.match(request, /nso-session-refreshed/);
    assert.match(app, /nso-session-refreshed/);
    assert.match(app, /auth\.saveSession/);
});

test('account activation cannot grant roles through the lifecycle endpoint', () => {
    const controller = read('../nso-admin/src/main/java/com/nso/web/controller/system/SysUserController.java');
    const activation = controller.match(/public AjaxResult<\?> activate\([\s\S]*?\n    \}/)?.[0] || '';
    assert.doesNotMatch(activation, /request\.roleCodes\(\)/);
});
