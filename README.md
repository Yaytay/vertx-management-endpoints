# Vertx Management Endpoints

[![Latest release](https://img.shields.io/github/release/yaytay/vertx-management-endpoints.svg)](https://github.com/yaytay/vertx-management-endpoints/latest)
[![License](https://img.shields.io/github/license/yaytay/vertx-management-endpoints)](https://github.com/yaytay/vertx-management-endpoints/blob/master/LICENCE.md)
[![Issues](https://img.shields.io/github/issues/yaytay/vertx-management-endpoints)](https://github.com/yaytay/vertx-management-endpoints/issues)
[![Build Status](https://github.com/yaytay/vertx-management-endpoints/actions/workflows/buildtest.yml/badge.svg)](https://github.com/Yaytay/vertx-management-endpoints/actions/workflows/buildtest.yml)
[![CodeCov](https://codecov.io/gh/Yaytay/vertx-management-endpoints/branch/main/graph/badge.svg?token=ACHVK20T9Q)](https://codecov.io/gh/Yaytay/vertx-management-endpoints)

A few Vert.x HTTP Server routes for providing functionality similar to Springs actuators.

The routes currently in the library are:
- [HeapDumpRoute](#heapdumproute)
- [InFlightRoute](#inflightroute)
- [LogbackMgmtRoute](#logbackmgmtroute)
- [ThreadDumpRoute](#threaddumproute)

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

Manual deployments basically involve copying the code from the createAndDeploy methods.
Note that the InFlightRoute must be installed on all routers that you want to monitor and is the most likely to be deployed in a custom manner.

## Important

Most of these routes expose information that you really do not want accessible to end users.
It is expected that the sub router used for these routes be restricted to trusted users only.

## HeapDumpRoute

A Vertx route for allowing users to download a heap dump over HTTP.

The heapdump generated is written to disk and then streamed from there to the client.
The resulting hprof file is likely to be large (the integration test routinely generates a 40MB file), avoid downloading it using any client that will try to store it in memory.
To help with this the output will include a Content-Disposition header to instruct browsers to save the body to a file.

## InFlightroute

A Vertx HTTP Server route for allowing users to download information about all HTTP requests currently being processed by the server.

This is only likely to be useful if your service has slow endpoints (or ones that generate a lot of data).

## LogbackMgmtRoute

A Vertx HTTP Server route for allowing users to pull and update logback levels.

Two routes are created:
- GET /logback
  Downloads a JSON representation of all the registered loggers.
  If the request specifies an Accept header of "text/html" (before any specification of "application/json") downloads a single HTML page that provides the ability to change log levels via a UI.
- PUT /logback/:logger
  Requires a message body that contains a JSON object with a single element ("level") with a value that is (case insensitive) a valid Logback level.
  The level of the specified logger is changed to the specified level.

## ThreadDumpRoute

A Vertx HTTP Server route for allowing users to download a thread dump of the process.

# Building

It's a standard maven project, just build it with:
```sh
mvn clean install
```

There aren't many dependencies, but there are a lot of plugins.