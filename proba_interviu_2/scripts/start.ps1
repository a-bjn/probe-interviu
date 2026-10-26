$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$api = Join-Path $root "api"
$web = Join-Path $root "web"

if (-not (Test-Path (Join-Path $api "node_modules"))) {
  npm --prefix $api install
}
if (-not (Test-Path (Join-Path $web "node_modules"))) {
  npm --prefix $web install
}

New-Item -ItemType Directory -Force -Path (Join-Path $root "data") | Out-Null
$env:HABIT_DB = Join-Path $root "data\habit.db"
$env:PORT = "3001"
$env:HOST = "0.0.0.0"

$apiProcess = Start-Process -FilePath "node" -ArgumentList "src/index.ts" -WorkingDirectory $api -PassThru -NoNewWindow
try {
  npm --prefix $web run dev
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    Stop-Process -Id $apiProcess.Id -Force
  }
}
