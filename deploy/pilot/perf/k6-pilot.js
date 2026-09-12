import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = String(__ENV.BASE_URL || '').replace(/\/$/, '');
const token = String(__ENV.NSO_LOAD_TOKEN || '');
const projectId = String(__ENV.PROJECT_ID || '');
const listPath = String(__ENV.PROJECT_LIST_PATH || '/api/v1/admin/projects?pageNo=1&pageSize=20');
const writePath = String(__ENV.WRITE_PATH || '');
const writeBody = String(__ENV.WRITE_BODY || '{}');

if (!baseUrl || !token) {
    throw new Error('BASE_URL and NSO_LOAD_TOKEN are required.');
}

const scenarios = {
    project_reads: {
        executor: 'constant-vus',
        vus: Number(__ENV.READ_VUS || 50),
        duration: String(__ENV.READ_DURATION || '5m'),
        exec: 'readProjectViews'
    }
};
if (writePath) {
    scenarios.project_writes = {
        executor: 'constant-vus',
        vus: Number(__ENV.WRITE_VUS || 50),
        duration: String(__ENV.WRITE_DURATION || '2m'),
        exec: 'writeFixture'
    };
}

export const options = {
    scenarios,
    thresholds: {
        'http_req_duration{operation:project_list}': ['p(95)<2000'],
        'http_req_duration{operation:project_detail}': ['p(95)<2000'],
        'http_req_duration{operation:project_write}': ['p(95)<2000'],
        'http_req_failed{operation:project_list}': ['rate<0.01'],
        'http_req_failed{operation:project_detail}': ['rate<0.01'],
        'http_req_failed{operation:project_write}': ['rate<0.01']
    }
};

function headers(operation) {
    return {
        headers: {
            Authorization: 'Bearer ' + token,
            'Content-Type': 'application/json',
            'X-Request-Id': 'k6-' + operation + '-' + __VU + '-' + __ITER,
            'X-Trace-Id': 'k6-' + operation + '-' + __VU + '-' + __ITER
        },
        tags: { operation }
    };
}

export function readProjectViews() {
    const list = http.get(baseUrl + listPath, headers('project_list'));
    check(list, {
        'project list returns success': (response) => response.status === 200,
        'project list returns trace id': (response) => Boolean(response.headers['X-Trace-Id'])
    });

    if (projectId) {
        const detail = http.get(baseUrl + '/api/v1/admin/projects/' + projectId, headers('project_detail'));
        check(detail, {
            'project detail returns success': (response) => response.status === 200,
            'project detail returns trace id': (response) => Boolean(response.headers['X-Trace-Id'])
        });
    }
    sleep(1);
}

export function writeFixture() {
    const response = http.post(baseUrl + writePath, writeBody, headers('project_write'));
    check(response, {
        'fixture write returns success': (result) => result.status >= 200 && result.status < 300,
        'fixture write returns trace id': (result) => Boolean(result.headers['X-Trace-Id'])
    });
    sleep(1);
}
