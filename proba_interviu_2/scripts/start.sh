#!/usr/bin/env sh
set -eu
root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$root"

if [ ! -d api/node_modules ]; then
  npm --prefix api install
fi
if [ ! -d web/node_modules ]; then
  npm --prefix web install
fi

mkdir -p data
export HABIT_DB="$root/data/habit.db"
export PORT=3001
export HOST=0.0.0.0

node api/src/index.ts &
api_pid=$!
trap 'kill "$api_pid"' EXIT INT TERM

npm --prefix web run dev
