#!/bin/sh

[ $# -eq 1 ] || exit 2

dir="$(dirname "$1")" || exit 3

cd "$dir" || exit 4

git pull || { git merge --abort; exit 5; }

exit 0
