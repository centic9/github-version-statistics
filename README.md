[![Build Status](https://travis-ci.org/centic9/github-version-statistics.svg)](https://travis-ci.org/centic9/github-version-statistics) 
[![Gradle Status](https://gradleupdate.appspot.com/centic9/github-version-statistics/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/github-version-statistics/status)
[![Release](https://img.shields.io/github/release/centic9/github-version-statistics.svg)](https://github.com/centic9/github-version-statistics/releases)

A small application which uses the GitHub API to search for usages of a Java library and prepares some statistics about which versions are used how often

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
