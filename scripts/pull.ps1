param(
    [Parameter(Mandatory, Position=0)]
    [string]$Repository
)

try {
    Set-Location "$(Split-Path "$Repository" -Parent -ErrorAction Stop)" -ErrorAction Stop
} catch {
    Exit 2
}

git pull
if ($LASTEXITCODE -ne 0) {
    git merge --abort
    Exit 3
}

Exit 0
