# Wookiee - Component: Memcached

[![Build Status](https://travis-ci.org/oracle/wookiee-cache-memcache.svg?branch=master)](https://travis-ci.org/oracle/wookiee-cache-memcache) [![Coverage Status](https://coveralls.io/repos/oracle/wookiee-cache-memcache/badge.svg?branch=master&service=github)](https://coveralls.io/github/oracle/wookiee-cache-memcache?branch=master) [![Latest Release](https://img.shields.io/github/release/oracle/wookiee-cache-memcache.svg)](https://github.com/oracle/wookiee-cache-memcache/releases) [![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

[Main Wookiee Project](https://github.com/oracle/wookiee)

For Configuration information see [Memcache Caching Config](docs/config.md)

For working example see [Wookie - Example Cache](example-caching)

See [Cache Docs](../wookiee-cache/README.md)

### Increasing Artifact Version
To bump the version of Wookiee simply increase it in the pom file. If you are
building a branch then it will automatically insert the branch name before SNAPSHOT.
So for example if the pom has 2.0-SNAPSHOT as a version the final artifact will end up
as 2.0-$branch-SNAPSHOT. If you create a tagged release in github, or if you change the
pom to a version that doesn't contain "SNAPSHOT" then the final artifact version will 
be literally what was in the tag/pom.
