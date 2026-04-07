#!/usr/bin/env python3
"""
PostToolUse hook: API_REFERENCE.md에 새 엔드포인트가 추가되면
최상단 '가장 최근에 추가된 엔드포인트' 줄을 자동으로 갱신합니다.
"""
import sys
import json
import re
import subprocess
import os


def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    filepath = data.get('tool_input', {}).get('file_path', '')

    if 'API_REFERENCE.MD' not in filepath.upper():
        sys.exit(0)

    # git diff HEAD 로 새로 추가된 줄 확인
    try:
        repo_dir = os.path.dirname(os.path.abspath(filepath))
        result = subprocess.run(
            ['git', 'diff', 'HEAD', '--', filepath],
            capture_output=True, text=True, encoding='utf-8',
            cwd=repo_dir
        )
        diff = result.stdout
    except Exception:
        sys.exit(0)

    # 새로 추가된 API 테이블 행 추출 (줄 앞 '+' 기호 = 추가된 줄)
    new_rows = re.findall(
        r'^\+\| `(GET|POST|PUT|DELETE|PATCH)` \| `(/api[^`]+)` \| [^|]+ \| ([^|]+) \|',
        diff, re.MULTILINE
    )

    if not new_rows:
        sys.exit(0)

    # 마지막으로 추가된 엔드포인트 사용
    method, path, desc = new_rows[-1]
    desc = desc.strip()
    new_line = f'> **가장 최근에 추가된 엔드포인트**: `{method} {path}` — {desc}'

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        updated = re.sub(
            r'> \*\*가장 최근에 추가된 엔드포인트\*\*[^\n]*',
            new_line,
            content
        )

        if updated != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(updated)
    except Exception:
        sys.exit(0)


if __name__ == '__main__':
    main()
