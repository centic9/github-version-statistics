[![Build Status](https://travis-ci.org/centic9/github-version-statistics.svg)](https://travis-ci.org/centic9/github-version-statistics) 
[![Gradle Status](https://gradleupdate.appspot.com/centic9/github-version-statistics/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/github-version-statistics/status)
[![Release](https://img.shields.io/github/release/centic9/github-version-statistics.svg)](https://github.com/centic9/github-version-statistics/releases)

A small application which uses the GitHub API to search for usages of a Java library and prepares some statistics about which versions are used how often

#### Initial URL for looking at results

For now the results can be looked at directly from the Git repository, for github you can use:

https://rawgit.com/centic9/github-version-statistics/master/results/results.html

However this is likely to change with some better deployment setup in the future.

#### Getting started

##### Grab it

    git clone git://github.com/centic9/github-version-statistics

##### Build it using Gradle

    ./gradlew check installDist

#### Run it

Currently the file `Search.java` contains `org.apache.poi` as indicator of the Java library to look for. Change this if you want to run it for a different project.

The application uses the Java GitHub API from http://github-api.kohsuke.org/, in order to set credentials, create a file `~/.github` with the contents as described at http://github-api.kohsuke.org/

Then you can run it via

    build/install/github-version-statistics/bin/github-version-statistics

#### Limitations

* Currently only Gradle `build.gradle` files are searched, the same should be done for Maven `pom.xml` files.
* The GitHub API currently limits the number of results to 1000, so you only get 1000 results at max and the statistics for popular Java libraries will not be complete.
* If there are multiple files in one repository currently, we may count the version multiple times, it would probably better to count each GitHub repository only once.

## Cron-entry to run the job every night

    0 0 * * * bash /opt/github-version-statistics/runStatistics.sh

## How to setup password-less pushes to github.com

* Go to https://github.com/settings/ssh and follow the steps to create and add a ssh-key
* See http://mattmakesmaps.com/blog/2013/06/16/auto-push-to-github-via-machine-user/ for more details

#### Contribute

If you are missing things or have suggestions how to improve the plugin, please either send pull requests or create [issues](https://github.com/centic9/github-version-statistics/issues).

#### Licensing

   Copyright 2013-2016 Dominik Stadler

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

The Java GitHub API is licensed under the MIT License, see http://github-api.kohsuke.org/license.html
