#!/bin/sh

[ $# -eq 1 ] || exit 2

dir="$(dirname "$1")" || exit 2

cd "$dir" || exit 2

git pull || { git merge --abort; exit 2; }

exit 0
