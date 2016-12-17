#!/bin/sh

echo Look at https://centic9.github.io/github-version-statistics/ for results

cd `dirname $0`

git fetch && \
git rebase origin/master && \
rm -rf build && \
./gradlew --no-daemon clean && \
./gradlew --no-daemon check installDist && \
build/install/github-version-statistics/bin/github-version-statistics && \
./gradlew --no-daemon processResults && \
git add stats* && git add docs && git ci -m "Add daily results" && \
git push
