#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"

command -v node >/dev/null 2>&1 || {
  echo "Node.js가 없습니다. 먼저: pkg update && pkg install nodejs"
  exit 1
}

export HOST="${HOST:-0.0.0.0}"
export PORT="${PORT:-3000}"
export RBXM_DATA_DIR="${RBXM_DATA_DIR:-$PWD/data}"

if [ -z "${RBXM_API_TOKEN:-}" ]; then
  echo "주의: RBXM_API_TOKEN이 설정되지 않았습니다. 로컬 Wi-Fi에서만 사용하세요."
else
  echo "API 인증 활성화됨"
fi

termux-wake-lock 2>/dev/null || true
mkdir -p "$RBXM_DATA_DIR"
exec npm start
