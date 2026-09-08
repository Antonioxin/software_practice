#!/usr/bin/env python3
"""Content/media smoke checks. Creates isolated test records; run against a test database only."""
import argparse
import http.cookiejar
import json
import os
import struct
import urllib.error
import urllib.request
import uuid
import zlib


class Client:
    def __init__(self, base):
        self.base = base.rstrip('/')
        self.origin = self.base.split('/api/')[0]
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.token = None

    def request(self, path, method='GET', body=None, expected=200, key=None, content_type='application/json'):
        headers = {'Accept': 'application/json, application/pdf, application/problem+json'}
        if method != 'GET':
            if not self.token:
                self.token = self.request('/auth/csrf')['data']['token']
            headers.update({'X-CSRF-Token': self.token, 'Origin': self.origin, 'Content-Type': content_type})
        if key:
            headers['Idempotency-Key'] = key
        payload = json.dumps(body).encode() if body is not None and not isinstance(body, bytes) else body
        request = urllib.request.Request(self.base + path, data=payload, headers=headers, method=method)
        try:
            response = self.opener.open(request, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        data = response.read()
        assert response.code == expected, f'{method} {path}: expected {expected}, got {response.code}: {data[:500]!r}'
        if 'json' in response.headers.get('Content-Type', ''):
            return json.loads(data)
        return data

    def upload(self, path, filename, data, mime, fields=None, metadata=None):
        boundary = 'wemove-' + uuid.uuid4().hex
        parts = []
        def part(name, value, kind=None, file_name=None):
            header = f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"'
            if file_name:
                header += f'; filename="{file_name}"'
            if kind:
                header += f'\r\nContent-Type: {kind}'
            parts.append(header.encode() + b'\r\n\r\n' + value + b'\r\n')
        part('file', data, mime, filename)
        for name, value in (fields or {}).items():
            part(name, str(value).encode())
        if metadata is not None:
            part('metadata', json.dumps(metadata).encode(), 'application/json')
        payload = b''.join(parts) + f'--{boundary}--\r\n'.encode()
        return self.request(path, 'POST', payload, 201 if path in ['/admin/media', '/admin/files'] else 200,
                            content_type=f'multipart/form-data; boundary={boundary}')['data']


def image_bytes():
    def chunk(kind, value):
        return struct.pack('>I', len(value)) + kind + value + struct.pack('>I', zlib.crc32(kind + value))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b'\x00\x72\x99\x80')) + chunk(b'IEND', b'')


def pdf_bytes(label):
    stream = f'BT /F1 12 Tf 40 80 Td ({label}) Tj ET'.encode()
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>', b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
               b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 150] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
               f'<< /Length {len(stream)} >>\nstream\n'.encode() + stream + b'\nendstream']
    result = b'%PDF-1.4\n'
    offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(result))
        result += f'{index} 0 obj\n'.encode() + obj + b'\nendobj\n'
    xref = len(result)
    result += f'xref\n0 {len(offsets)}\n0000000000 65535 f \n'.encode()
    result += b''.join(f'{offset:010d} 00000 n \n'.encode() for offset in offsets[1:])
    return result + f'trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080/api/v1')
    parser.add_argument('--admin-email', required=True)
    parser.add_argument('--allow-test-writes', action='store_true')
    args = parser.parse_args()
    if not args.allow_test_writes:
        parser.error('Specify --allow-test-writes after choosing a dedicated test database.')
    password = os.environ.get('CONTENT_SMOKE_ADMIN_PASSWORD')
    if not password:
        parser.error('Set CONTENT_SMOKE_ADMIN_PASSWORD in the environment.')
    admin, guest = Client(args.base_url), Client(args.base_url)
    admin.request('/auth/login', 'POST', {'email': args.admin_email, 'password': password})
    admin.token = None
    suffix = uuid.uuid4().hex[:8]
    media = admin.upload('/admin/media', 'smoke.png', image_bytes(), 'image/png', {'altText': 'smoke ' + suffix})
    guest.request(f'/media/{media["id"]}/content', expected=404)
    article_body = {'title': '验证文章 ' + suffix, 'summary': '内容发布与引用检查', 'body': '<p>有效正文</p><script>window.bad=1</script>', 'category': '验证', 'pageDescription': '', 'productIds': [], 'mediaIds': [media['id']], 'sortOrder': 999}
    article = admin.request('/admin/articles', 'POST', article_body, 201, str(uuid.uuid4()))['data']
    assert '<script' not in article['body']
    guest.request('/articles/' + article['id'], expected=404)
    key, command = str(uuid.uuid4()), {'expectedVersion': article['version']}
    published = admin.request(f'/admin/articles/{article["id"]}/publish', 'POST', command, key=key)['data']
    replay = admin.request(f'/admin/articles/{article["id"]}/publish', 'POST', command, key=key)['data']
    assert replay['version'] == published['version']
    guest.request('/articles/' + article['id'])
    assert guest.request(f'/media/{media["id"]}/content').startswith(b'\x89PNG')
    admin.request(f'/admin/media/{media["id"]}?expectedVersion={media["version"]}', 'DELETE', expected=409)
    banner_body = {'title': '验证 Banner ' + suffix, 'imageId': media['id'], 'buttonText': '阅读玩法', 'targetUrl': '/articles', 'sortOrder': 999}
    banner = admin.request('/admin/banners', 'POST', banner_body, 201, str(uuid.uuid4()))['data']
    banner = admin.request(f'/admin/banners/{banner["id"]}/publish', 'POST', {'expectedVersion': banner['version']}, key=str(uuid.uuid4()))['data']
    assert any(item['id'] == banner['id'] for item in guest.request('/home')['data']['banners'])
    banner = admin.request(f'/admin/banners/{banner["id"]}/unpublish', 'POST', {'expectedVersion': banner['version']}, key=str(uuid.uuid4()))['data']
    assert all(item['id'] != banner['id'] for item in guest.request('/home')['data']['banners'])
    admin.request('/admin/banners/' + banner['id'], 'PATCH', {**banner_body, 'imageId': None, 'expectedVersion': banner['version']})
    admin.request('/admin/articles/' + article['id'], 'PATCH', {**article_body, 'expectedVersion': article['version']}, expected=409)
    offline = admin.request(f'/admin/articles/{article["id"]}/unpublish', 'POST', {'expectedVersion': published['version']}, key=str(uuid.uuid4()))['data']
    guest.request('/articles/' + article['id'], expected=404)
    guest.request(f'/media/{media["id"]}/content', expected=404)
    admin.request('/admin/articles/' + article['id'], 'PATCH', {**article_body, 'mediaIds': [], 'expectedVersion': offline['version']})
    admin.request(f'/admin/media/{media["id"]}?expectedVersion={media["version"]}', 'DELETE', expected=204)
    print('PASS article: draft visibility, sanitization, publish/replay, stale version, unpublish, media reference protection')
    faq = admin.request('/admin/faqs', 'POST', {'question': '测试问题 ' + suffix, 'answer': '<p>可以查看下载中心。</p>', 'category': '验证', 'productIds': [], 'sortOrder': 999}, 201, str(uuid.uuid4()))['data']
    faq = admin.request(f'/admin/faqs/{faq["id"]}/publish', 'POST', {'expectedVersion': faq['version']}, key=str(uuid.uuid4()))['data']
    guest.request('/faqs/' + faq['id'])
    admin.request(f'/admin/faqs/{faq["id"]}/unpublish', 'POST', {'expectedVersion': faq['version']}, key=str(uuid.uuid4()))
    guest.request('/faqs/' + faq['id'], expected=404)
    print('PASS banner + FAQ: published in public queries, hidden after unpublish')

    metadata = {'title': '验证资料 ' + suffix, 'type': '测试指南', 'versionNote': 'v1', 'productIds': [], 'visibility': 'PUBLIC'}
    file = admin.upload('/admin/files', 'guide.pdf', pdf_bytes('Content smoke v1'), 'application/pdf', metadata=metadata)
    guest.request('/files/' + file['id'], expected=404)
    file = admin.request(f'/admin/files/{file["id"]}/publish', 'POST', {'expectedVersion': file['version']}, key=str(uuid.uuid4()))['data']
    old_url = file['downloadUrl'].removeprefix('/api/v1')
    assert guest.request(old_url).startswith(b'%PDF-')
    file = admin.upload(f'/admin/files/{file["id"]}/replace', 'guide.pdf', pdf_bytes('Content smoke v2'), 'application/pdf', {'expectedVersion': file['version'], 'versionNote': 'v2'})
    guest.request(old_url, expected=404)
    assert guest.request(file['downloadUrl'].removeprefix('/api/v1')).startswith(b'%PDF-')
    file = admin.request('/admin/files/' + file['id'], 'PATCH', {**metadata, 'visibility': 'INTERNAL', 'expectedVersion': file['version']})['data']
    guest.request('/files/' + file['id'], expected=404)
    guest.request(file['downloadUrl'].removeprefix('/api/v1'), expected=404)
    assert suffix not in json.dumps(guest.request('/files'), ensure_ascii=False)
    admin.request(f'/admin/files/{file["id"]}/unpublish', 'POST', {'expectedVersion': file['version']}, key=str(uuid.uuid4()))
    admin.request('/admin/files?pageSize=100', expected=422)
    print('PASS file: draft visibility, PDF streaming, replacement invalidation, private metadata/download protection, unpublish, pagination validation')
    print(json.dumps({'articleId': article['id'], 'fileId': file['id'], 'finalStatus': 'OFFLINE'}, ensure_ascii=False))


if __name__ == '__main__':
    main()
