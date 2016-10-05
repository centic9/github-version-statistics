#!/bin/sh

cd `dirname $0`

git fetch && \
git rebase origin/master && \
rm -rf build && \
./gradlew clean && \
./gradlew check installDist && \
build/install/github-version-statistics/bin/github-version-statistics && \
./gradlew processResults && \
git add stats* && git add docs && git ci -m "Add daily results" && \
git push
