#!/bin/sh

set -u

echo Look at https://centic9.github.io/github-version-statistics/ for results

cd `dirname $0`

echo
echo Checkout and rebase
git checkout upgrades.csv && \
git fetch && \
git rebase origin/master
if [ $? -ne 0 ]
then
  echo "Failed to Rebase"
  exit 1
fi

echo
echo Cleaning
rm -rf build && \
./gradlew --no-daemon clean
if [ $? -ne 0 ]
then
  echo "Failed to Clean"
  exit 2
fi

echo
echo Testing
./gradlew --no-daemon check
if [ $? -ne 0 ]
then
  echo "Failed to Test"
  cat build/test-results/test/TEST-*.xml
  exit 3
fi

echo
echo Installing
./gradlew --no-daemon installDist
if [ $? -ne 0 ]
then
  echo "Failed to install"
  exit 4
fi

echo
echo Executing
build/install/github-version-statistics/bin/github-version-statistics
if [ $? -ne 0 ]
then
  echo "Failed to Execute"
  exit 5
fi

echo
echo Processing results
./gradlew --no-daemon processResults
if [ $? -ne 0 ]
then
  echo "Failed to process results"
  exit 6
fi

echo
echo Commit and push to Github
git add stats* && \
git add docs && \
git commit -m "[ci skip] Add daily results" && \
git push
if [ $? -ne 0 ]
then
  echo "Failed to Commit and Push to github"
  exit 7
fi

exit 0
