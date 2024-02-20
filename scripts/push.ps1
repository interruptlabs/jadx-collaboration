param(
    [Parameter(Mandatory, Position=0)]
    [string]$Repository
)

try {
    Set-Location "$(Split-Path "$Repository" -Parent -ErrorAction Stop)" -ErrorAction Stop
} catch {
    Exit 2
}

git reset
if ($LASTEXITCODE -ne 0) {
    Exit 3
}

git add "$Repository"
if ($LASTEXITCODE -ne 0) {
    Exit 4
}

try {
    git commit --allow-empty -m "Update $(Split-Path "$Repository" -Leaf -ErrorAction Stop) repository"
} catch {
    Exit 5
}
if ($LASTEXITCODE -ne 0) {
    Exit 6
}

git push
if ($LASTEXITCODE -ne 0) {
    git reset --hard HEAD~1
    Exit 1
}

Exit 0
