#!/bin/sh

[ $# -eq 1 ] || exit 2

dir="$(dirname "$1")" || exit 3
file="$(basename "$1")" || exit 4

cd "$dir" || exit 5

git reset || exit 6

git add "$1" || exit 7

git commit --allow-empty -m "Update $file repository" || exit 8

git push || { git reset --hard HEAD~1; exit 1; }

exit 0
