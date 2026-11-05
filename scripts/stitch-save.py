"""Stitch の download_file の結果（JSON）から、画像・HTML を取り出して docs/design/mockups/ に保存する。

download_file の結果が大きいと、Claude Code は結果を JSON ファイルに保存してそのパスだけを返す。
画像は base64 なので、そのファイルをデコードしないと PNG として保存できない。
サブエージェントは権限の承認を受けられないため、保存先を docs/design/mockups/ に限ったこのスクリプトだけを
.claude/settings.json で許可する。

    python -I scripts/stitch-save.py {download_file の結果の JSON ファイル} {保存先}
    例: python -I scripts/stitch-save.py C:/.../tool-results/mcp-stitch-download_file-123.txt docs/design/mockups/S-01/S-01.png

[한국어] Stitch download_file 결과(JSON)에서 이미지·HTML을 꺼내 docs/design/mockups/에 저장한다.
결과가 크면 Claude Code는 결과를 JSON 파일로 저장하고 경로만 돌려준다. 이미지는 base64라서 디코드해야 PNG로 저장된다.
서브 에이전트는 권한 승인을 받을 수 없으므로, 저장 위치를 docs/design/mockups/로 제한한 이 스크립트만
.claude/settings.json에서 허용한다.
"""

import base64
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MOCKUPS = (ROOT / "docs" / "design" / "mockups").resolve()
# 画像は PNG、HTML はそのまま。ほかの形式は想定しない
ALLOWED = {".png": "image", ".html": "html"}
# リポジトリを重くしないための上限（写真の入った試作は 1MB を超える。stitch-workflow スキルの方針で写真を使わない）
MAX_BYTES = 1_000_000


# メッセージは英語（Windows のコンソールの文字コードで文字化けしないため）
def fail(message: str) -> None:
    print(message, file=sys.stderr)
    sys.exit(1)


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: python -I scripts/stitch-save.py <download_file result JSON> <docs/design/mockups/...>")

    source = pathlib.Path(sys.argv[1])
    dest = (ROOT / sys.argv[2]).resolve()

    # 保存先を docs/design/mockups/ の下に限る（ほかの担当のファイルを上書きしないため）
    if MOCKUPS not in dest.parents:
        fail(f"destination must be under docs/design/mockups/: {dest}")
    kind = ALLOWED.get(dest.suffix.lower())
    if kind is None:
        fail("extension must be .png or .html")

    data = json.loads(source.read_text(encoding="utf-8"))
    if kind not in data:
        fail(f"JSON has no '{kind}' (extension does not match the content): keys={list(data)}")

    body = base64.b64decode(data["image"]) if kind == "image" else data["html"].encode("utf-8")
    if kind == "image" and not body.startswith(b"\x89PNG"):
        fail("not a PNG")
    if len(body) > MAX_BYTES:
        fail(f"{len(body)} bytes > {MAX_BYTES}: regenerate without photos/images (see stitch-workflow skill)")

    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(body)
    print(f"saved {dest.relative_to(ROOT)} ({len(body)} bytes)")


if __name__ == "__main__":
    main()
