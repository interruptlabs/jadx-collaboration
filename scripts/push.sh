#!/bin/sh

[ $# -eq 1 ] || exit 2

dir="$(dirname "$1")" || exit 2
file="$(basename "$1")" || exit 2

cd "$dir" || exit 2

git reset || exit 2

git add "$1" || exit 2

git commit -m "Update $file repository" || exit 2

git push || { git reset --hard HEAD~1; exit 1; }

exit 0
