# Vertx Management Endpoints

[![Latest release](https://img.shields.io/github/release/yaytay/vertx-management-endpoints.svg)](https://github.com/yaytay/vertx-management-endpoints/latest)
[![License](https://img.shields.io/github/license/yaytay/vertx-management-endpoints)](https://github.com/yaytay/vertx-management-endpoints/blob/master/LICENCE.md)
[![Issues](https://img.shields.io/github/issues/yaytay/vertx-management-endpoints)](https://github.com/yaytay/vertx-management-endpoints/issues)
[![Build Status](https://github.com/yaytay/vertx-management-endpoints/actions/workflows/buildtest.yml/badge.svg)](https://github.com/Yaytay/vertx-management-endpoints/actions/workflows/buildtest.yml)
[![CodeCov](https://codecov.io/gh/Yaytay/vertx-management-endpoints/branch/main/graph/badge.svg?token=ACHVK20T9Q)](https://codecov.io/gh/Yaytay/vertx-management-endpoints)

A few Vert.x HTTP Server routes for providing functionality similar to Springs actuators.

The routes currently in the library are:
- (#HeapDumpVerticle)[HeapDumpVerticle]
- InFlightVerticle
- LogbackMgmtVerticle
- ThreadDumpVerticle

# Basic Usage

The routes can all be deployed either manually or via a static helper method.

Using the helper methods:
```
Router router = Router.router(vertx);
Router mgmtRouter = Router.router(vertx);
router.route("/manage/*").subRouter(mgmtRouter);
    
HeapDumpRoute.createAndDeploy(mgmtRouter);
InFlightRoute.createAndDeployt(router, mgmtRouter);
LogbackMgmtRoute.createAndDeploy(mgmtRouter);
ThreadDumpRoute.createAndDeploy(mgmtRouter);
```

## 


# Building

It's a standard maven project, just build it with:
```sh
mvn clean install
```

There aren't many dependencies, but there are a lot of plugins.