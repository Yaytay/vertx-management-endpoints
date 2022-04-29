# Parameters For Java

[![License](https://img.shields.io/github/license/yaytay/params4j)]

Parameters for Java is a small library for collating parameters to Java daemons.

Originally written for services running in either Docker Swarm or Kubernetes there are many other places that it can be used.

Params4J can pick up values from:
- Resource files (properties, json or yaml)
- Files on disc (properties, json or yaml)
- Plain files in a hierarchy (aimed at handling Kubernetes secrets)
- Environment variables
- System properties
- Command line arguments
 
It is entirely configurable which of these sources is used.

Files on disc (not resources) can be monitored for any changes.

The results of the gathering of parameters is always a single instance of a POJO of any class that Jackson can deserialize.
I have an intense dislike for the Spring approach of scattering @Values throughout the source code with no collation (or consistency) of the configuration values,
so Params4J takes the approach of forcing all parameters to be defined in a single class structure.
This does not mean that the structure is necessarily flat, nor that it is necessary to pass around all the parameters within your code.
Fields within the parameters object can themselves be POJOs representing a subset of the configuration.

Note that whilst Params4J will gather command line arguments it is not intended to be used for typical command line apps - there are better tools for that.
Params4J is aimed at services with complex configuration.
The support for command line arguments is aimed at providing a high priority mechanism for tweaking individual parameters.

# Getting Started

1. Define the parameters class structure that you want to use.
   This can be as complex as you want, but see [DummyParameters.java](src/test/java/uk/co/spudsoft/params4j/impl/DummyParameters.java) for an example from the unit tests.
2. Decide which sources you want to use for your parameters.
3. Decide whether you want to allow the parameters to be updated.
   It is a good idea to react to parameters being updated, even if your reaction is to just restart.
4. Somewhere near the start of your main process, gather your parameters:
```java
    Params4J<DummyParameters> p4j = new Params4JFactoryImpl<DummyParameters>()
            .withConstructor(() -> new DummyParameters())
            .withGatherer(new PropertiesResourceGatherer<>("/test1.properties"))
            .withGatherer(new DirGatherer<>("/etc/my-service", FileType.Properties, FileType.Yaml))
            .withGatherer(new SecretsGatherer<>("/etc/my-service/conf.d", 100, 100, 4, StandardCharsets.UTF_8))
            .withGatherer(new SystemPropertiesGatherer<>(props, "my-service"))
            .create();

    DummyParameters dp = p4j.gatherParameters();

    // Monitor for changes
    p4j.notifyOfChanges(updatedParams -> {
      // Do something with params here (possibly a graceful shutdown so Kubernetes will restart with new values).
    });
```

Logging is all via slf4j.
At TRACE level it can be really quite verbose, which can help when it isn't processing the files you think it should.
Failures are usually logged at WARN level and processing carries on around the error.

# Documenting

One of the problems with having a large number of parameters that can be configured in many different ways is that it can be difficult to tell your users what those parameters are.
Params4J can help with this, as long as your Parameters objects use bean-style getters and setters.

The Params4J.getDocumentation method to use reflection to walk through the Parameters class(es) building documentation.
Each property found produces a ConfigurationProperty object that provides the argument name (without any prefix), basic details of the parameter type, and a simple comment as to the purpose of the parameter.
The comment is built hierarchically - so, for example, the property 'auditDataSource.user.username' combines (CSV) the comments from the auditDataSource setter, the user setter and the username setter.
The comments on each setter come from:
1. The Comment annotation.
2. Javadoc comments on the setter.
3. Javadoc comments on the field with the same name as the property.

Javadoc comments are captured at compile time by the JavadocCapturer AnnotationProcessor.
The JavadocCapturer does nothing unless there is a class element in the compile tree with a JavadocCapture annotation.
Starting from that class (and recursively walking through any referenced classes) the processor takes any fields and setters and puts the first line of the javadoc into a properties file alongside the class.

The JavadocCapture process can only work for classes actually being compiled at the time the annotation processor runs, so any classes pulled in from external Jars will be undocumented and untouched.
When getDocumentation is run it will walk through the third party classes and will add them to the known parameters unless it is explicitly instructed not to.
This can result in a large number of undocumented parameters being listed in your documentation.
To work around this provide a Comment annotation on the setter that sets the third party class (pointing the user to external documentation) and exclude the third party class from being walked using the undocumentedClasses argument to getDocumentation.

The getDocumentation method does not actually do any output, you may want something different from me.
The TestDocs class (which is a resource so it can be compiled with the JavadocCapturer AnnotationProcessor) contains a sample for preparing output for a command line:
```java
    Params4J<Parameters> params4j = Params4J.<Parameters>factory().withConstructor(() -> new Parameters()).create();
    docs = params4j.getDocumentation(new Parameters(), "--", null, Arrays.asList(Pattern.compile(".*\\.Html.*")));
    
    int maxNameLen = docs.stream().map(p -> p.name.length()).max(Integer::compare).get();
    
    StringBuilder usageBuilder = new StringBuilder();
    for (ConfigurationProperty prop : docs) {
      usageBuilder.append("    ")
              .append(prop.name)
              .append(" ".repeat(maxNameLen + 1 - prop.name.length()))
              .append(prop.comment)
              .append('\n');

      String typeName = prop.type.getSimpleName();
      usageBuilder.append("        ")
              .append(typeName);
      
      if (prop.defaultValue != null) {
        usageBuilder.append(" ".repeat(typeName.length() + 4 > maxNameLen ? 1 : maxNameLen - typeName.length() - 3))
                .append("default: ")
                .append(prop.defaultValue);
      }
      usageBuilder.append('\n');
    }
    logger.debug("Usage:\n{}", usageBuilder);
```

There will undoubtedly be constructs that the documentation gathering mechanism cannot handle, please file issues when you have one (preferably with a PR that modifies the classes under commentcap).

# Building

It's a standard maven project, just build it with:
```sh
mvn clean install
```

There are minimal dependencies, at runtime it's jackson and slf4j, but there are a lot of plugins.