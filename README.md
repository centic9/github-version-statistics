[![Build Status](https://travis-ci.org/centic9/github-version-statistics.svg)](https://travis-ci.org/centic9/github-version-statistics) 
[![Gradle Status](https://gradleupdate.appspot.com/centic9/github-version-statistics/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/github-version-statistics/status)
[![Release](https://img.shields.io/github/release/centic9/github-version-statistics.svg)](https://github.com/centic9/github-version-statistics/releases)

A small application which uses the [GitHub API](https://github.com/kohsuke/github-api) to search for usages of a Java library and 
prepares some statistics about which versions are used how often.

Currently it looks for usages of [Apache POI](http://poi.apache.org/), but this can be adjusted
by modifying the source slightly.

#### Results for Apache POI

The results of a daily run of the scan for Apache POI can be found at [https://centic9.github.io/github-version-statistics/](]https://centic9.github.io/github-version-statistics/).

#### Getting started

##### Grab it

    git clone git://github.com/centic9/github-version-statistics

#### Configure library and credentials

Currently the file `BaseSearch.java` contains `org.apache.poi` as indicator of the Java library to look for. 
Change this if you want to run it for a different project.

The application uses the Java GitHub API from [http://github-api.kohsuke.org/](http://github-api.kohsuke.org/), in order to set credentials, 
create a file `~/.github` with the contents as described at [http://github-api.kohsuke.org/](http://github-api.kohsuke.org/)

##### Build it using Gradle

    ./gradlew check installDist

##### Run it

Then you can run it via

    build/install/github-version-statistics/bin/github-version-statistics

#### Limitations

* Currently only Gradle `build.gradle` and `pom.xml` files are searched, the same should 
be done for other buildsystems that use declarative dependencies of third-party libraries,
 e.g. Apache Ivy
* The GitHub API currently limits the number of results to 1000, so you only 
get 1000 results at max per type of buildsystem and the statistics for popular 
Java libraries will not be complete.

## Cron-entry to run the job every night

    0 0 * * * bash /opt/github-version-statistics/runStatistics.sh

## How to setup password-less pushes to github.com

* Go to [https://github.com/settings/ssh](https://github.com/settings/ssh) and follow the steps to create and add a ssh-key
* See [http://mattmakesmaps.com/blog/2013/06/16/auto-push-to-github-via-machine-user/](http://mattmakesmaps.com/blog/2013/06/16/auto-push-to-github-via-machine-user/) 
for more details

#### Contribute

If you are missing things or have suggestions how to improve the project, please either 
send pull requests or create [issues](https://github.com/centic9/github-version-statistics/issues).

#### Licensing

   Copyright 2013-2017 Dominik Stadler

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

The Java GitHub API is licensed under the MIT License, see http://github-api.kohsuke.org/license.html
