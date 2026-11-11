"""Real API integration checks against a running Compose stack; no third-party dependencies.

Creates small demo uploads and task history. --restart additionally restarts this
project's backend container to verify persisted sessions and interrupted tasks.
"""
import argparse
import base64
import http.cookiejar
import json
import struct
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib
from datetime import datetime
from pathlib import Path

CHECKS = 0


def check(condition, message):
    global CHECKS
    CHECKS += 1
    if not condition:
        raise AssertionError(message)


def png(color):
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)
    pixels = b''.join(b'\x00' + bytes(color) * 4 for _ in range(4))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 4, 4, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(pixels)) + chunk(b'IEND', b''))


class Client:
    def __init__(self, base):
        self.base = base.rstrip('/')
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(self.cookies))

    def call(self, method, path, payload=None, raw=None, headers=None, expected=200):
        request_headers = {'Origin': 'http://localhost:3000'}
        if payload is not None:
            raw = json.dumps(payload).encode()
            request_headers['Content-Type'] = 'application/json'
        request_headers.update(headers or {})
        request = urllib.request.Request(self.base + path, data=raw, headers=request_headers, method=method)
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            check(response.status == expected, f'{method} {path}: expected {expected}, got {response.status}: {body[:400]!r}')
            content_type = response.headers.get('Content-Type', '')
            return json.loads(body) if body and 'json' in content_type else body

    def upload(self, content, mime='image/png', filename='demo.png', expected=201):
        boundary = uuid.uuid4().hex
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
                f'Content-Type: {mime}\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
        return self.call('POST', '/api/assets', raw=body,
                         headers={'Content-Type': 'multipart/form-data; boundary=' + boundary}, expected=expected)

    def create(self, asset_ids, fail_at=None, delay=100, template='pet-portrait', expected=202):
        return self.call('POST', '/api/tasks', {'templateId': template, 'assetIds': asset_ids,
                         'simulation': {'delayMs': delay, 'failAt': fail_at}}, expected=expected)

    def task(self, task_id):
        return self.call('GET', '/api/tasks/' + task_id)

    def wait(self, task_id):
        deadline = time.monotonic() + 40
        while time.monotonic() < deadline:
            task = self.task(task_id)
            if task['status'] in ('SUCCEEDED', 'FAILED'):
                return task
            time.sleep(0.1)
        raise AssertionError('Task did not reach a terminal state: ' + task_id)


def run(base, restart):
    anonymous, alice, bob = Client(base), Client(base), Client(base)
    anonymous.call('GET', '/api/me', expected=401)
    anonymous.call('GET', '/api/tasks', expected=401)
    anonymous.call('POST', '/api/demo/session', {'account': 'alice'}, headers={'Origin': 'https://untrusted.example'}, expected=403)
    anonymous.call('POST', '/api/demo/session', {'account': 'invalid'}, expected=400)
    user_a = alice.call('POST', '/api/demo/session', {'account': 'alice'})
    user_b = bob.call('POST', '/api/demo/session', {'account': 'bob'})
    check(user_a['id'] != user_b['id'], 'Demo users must be distinct')
    check(alice.call('GET', '/api/me') == user_a, 'Session must identify its owner')

    source = png((66, 126, 145))
    first = alice.upload(source)
    second = alice.upload(png((160, 120, 74)))
    check(alice.call('GET', first['previewUrl']) == source, 'Uploaded file must be retrievable from server storage')
    bob.call('GET', first['previewUrl'], expected=404)
    anonymous.call('GET', first['previewUrl'], expected=401)
    alice.upload(b'<svg/>', mime='image/svg+xml', filename='bad.svg', expected=415)
    alice.upload(b'not a png', expected=400)
    alice.upload(source, mime='image/jpeg', expected=415)
    alice.upload(b'', expected=400)
    alice.upload(b'x' * (5 * 1024 * 1024 + 1), expected=413)

    alice.create([first['id']], template='missing', expected=404)
    alice.create([first['id']], template='coastal-postcard', expected=400)
    alice.create([first['id'], first['id']], template='coastal-postcard', expected=400)
    bob.create([first['id']], expected=404)
    alice.create([first['id']], delay=-1, expected=400)
    alice.create([first['id']], fail_at='INVALID', expected=400)
    alice.call('GET', '/api/tasks?limit=nope', expected=400)
    alice.call('GET', '/api/tasks?limit=0', expected=400)
    alice.call('GET', '/api/tasks?cursor=invalid', expected=400)
    invalid_time = base64.urlsafe_b64encode(('9223372036854775807|' + str(uuid.uuid4())).encode()).decode().rstrip('=')
    alice.call('GET', '/api/tasks?cursor=' + invalid_time, expected=400)

    accepted = alice.create([first['id'], second['id']], delay=1000, template='coastal-postcard')
    check(accepted['status'] in ('QUEUED', 'RUNNING'), 'Submit must return before processing completes')
    # No query is made while all three steps execute.
    time.sleep(3.5)
    completed = alice.task(accepted['id'])
    check(completed['status'] == 'SUCCEEDED', 'Processing must advance without polling')
    check([step['stage'] for step in completed['steps']] == ['VALIDATE', 'GENERATE', 'FINALIZE'], 'Persisted stage order')
    check(all(step['status'] == 'SUCCEEDED' and step['finishedAt'] for step in completed['steps']), 'All successful boundaries recorded')
    check(alice.call('GET', completed['resultUrl']) == source, 'Mock result should be a stored copy of the first image')
    bob.call('GET', '/api/tasks/' + completed['id'], expected=404)
    bob.call('GET', completed['resultUrl'], expected=404)
    anonymous.call('GET', completed['resultUrl'], expected=401)

    stages = ['VALIDATE', 'GENERATE', 'FINALIZE']
    mixed = [(stage, alice.create([first['id']], fail_at=stage)) for stage in stages]
    success_in_parallel = alice.create([second['id']])
    for index, (failed_stage, created) in enumerate(mixed):
        failed = alice.wait(created['id'])
        check(failed['status'] == 'FAILED' and failed['failureStage'] == failed_stage, 'Failure must belong to the correct task and stage')
        check([s['stage'] for s in failed['steps']] == stages[:index + 1], 'Later stages must not execute after failure')
        check(failed['steps'][-1]['status'] == 'FAILED' and failed['failureReason'], 'Failure boundary and reason must persist')
        check(failed['resultUrl'] is None, 'Failed tasks must not expose results')
        alice.call('GET', '/api/tasks/' + failed['id'] + '/result', expected=404)
        check(alice.task(failed['id']) == failed, 'Querying a terminal task must not mutate it')
    parallel_result = alice.wait(success_in_parallel['id'])
    check(parallel_result['status'] == 'SUCCEEDED', 'One failing task must not interrupt another')
    check(alice.call('GET', parallel_result['resultUrl']) != source, 'Task results must not be shared accidentally')
    check(alice.task(completed['id']) == completed, 'Successful terminal state must remain stable')

    seen, cursor, order = set(), None, []
    while True:
        query = {'limit': 2}
        if cursor:
            query['cursor'] = cursor
        page = alice.call('GET', '/api/tasks?' + urllib.parse.urlencode(query))
        for item in page['items']:
            check(item['id'] not in seen, 'Pagination must not duplicate rows')
            seen.add(item['id'])
            order.append((datetime.fromisoformat(item['createdAt'].replace('Z', '+00:00')), item['id']))
        cursor = page['nextCursor']
        if not cursor:
            break
        check(len(seen) < 1000, 'Pagination must terminate')
    check(order == sorted(order, reverse=True), 'History must be ordered by timestamp and id descending')
    check({completed['id'], success_in_parallel['id'], *(task['id'] for _, task in mixed)} <= seen, 'History must recover all created tasks')
    bob_ids = {item['id'] for item in bob.call('GET', '/api/tasks')['items']}
    check(not (seen & bob_ids), 'History must be isolated by account')

    old_token = next(cookie.value for cookie in alice.cookies if cookie.name == 'studio_session')
    alice.call('POST', '/api/demo/session', {'account': 'bob'})
    check(alice.call('GET', '/api/me') == user_b, 'Switching account must replace identity')
    anonymous.call('GET', '/api/me', headers={'Cookie': 'studio_session=' + old_token}, expected=401)
    alice.call('POST', '/api/demo/session', {'account': 'alice'})
    check(alice.task(completed['id'])['status'] == 'SUCCEEDED', 'New session must recover persisted history')

    if restart:
        interrupted = alice.create([first['id']], delay=30000)
        subprocess.run(['docker', 'compose', 'restart', 'backend'], cwd=Path(__file__).resolve().parents[1], check=True)
        deadline = time.monotonic() + 60
        while True:
            try:
                healthy = anonymous.call('GET', '/actuator/health')['status'] == 'UP'
                if healthy:
                    break
            except (urllib.error.URLError, OSError, AssertionError):
                pass
            if time.monotonic() > deadline:
                raise AssertionError('Backend did not restart')
            time.sleep(0.5)
        abandoned = alice.wait(interrupted['id'])
        check(abandoned['status'] == 'FAILED' and '中止' in abandoned['failureReason'], 'Restart must not leave stale processing tasks')
        check(alice.task(completed['id']) == completed, 'Restart must preserve terminal records')
        after_restart = alice.create([first['id']], delay=0)
        check(alice.wait(after_restart['id'])['status'] == 'SUCCEEDED', 'New tasks must work after restart')

    alice.call('DELETE', '/api/session', expected=204)
    alice.call('GET', '/api/me', expected=401)
    bob.call('DELETE', '/api/session', expected=204)
    print(json.dumps({'passed': True, 'assertions': CHECKS, 'restart_checked': restart,
                      'scenarios': ['session isolation', 'upload validation', 'async execution without polling',
                                    'three failure stages', 'independent concurrent tasks', 'result ownership',
                                    'pagination', 'terminal stability', 'session rotation', 'logout']}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    parser.add_argument('--restart', action='store_true')
    args = parser.parse_args()
    run(args.base_url, args.restart)
