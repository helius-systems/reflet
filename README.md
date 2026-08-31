![Static Badge](https://img.shields.io/badge/Java-17-green)
![Maven Central Version](https://img.shields.io/maven-central/v/systems.helius/helius-commons?color=blue)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

# Reflet: A Reflective Java Library
Reflet is a Java library that provides reflection and meta-programming utilities.

[Published on Maven Central:](https://central.sonatype.com/artifact/systems.helius/helius-commons)

```
implementation group: 'systems.helius', name: 'helius-commons', version: '0.8.1'
```

Minimum java version: 17

## Introspection
To use the introspection ability of this library, simply create an instance of BeanIntrospector.
You then call the `seek(Class, Object, MethodHandles.Lookup` method on it.

Example:
```java
    // Setup
    var structure = new ComplexStructure();

    // Usage
    Set<Foo> found = new BeanIntrospector().seek(Foo.class, structure, MethodHandles.lookup());
    
    // Validation
    assertEquals(2, found.size());
    assertTrue(found.contains(/* Some Foo I know is already hidden in the object graph of the structure */));
    assertTrue(found.contains(/* Another one */));
```
In this example, IntHolder is a deeply nested (4 classes down) class and attribute within [ComplexStructure](https://github.com/helius-systems/reflet/blob/main/src/test/java/systems/helius/reflet/fixtures/ComplexStructure.java).


For more examples, look at [the tests](https://github.com/helius-systems/reflet/blob/main/src/test/java/systems/helius/reflet/reflection/BeanIntrospectorTest.java).
You may reuse the same BeanIntrospector across different calls.

### The Lookup object
From: [Java 17 API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/invoke/MethodHandles.Lookup.html)
A Lookup object can be shared with other trusted code, such as a metaobject protocol.
A shared Lookup object delegates the capability to create method handles on private members of the lookup class.
Even if privileged code uses the Lookup object, the access checking is confined to the privileges of the original lookup class.

### Accessing base Java classes
Add the following JVM options to your launch configuration:
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
```
Read [Five Command Line Options To Hack The Java Module System](https://nipafx.dev/five-command-line-options-hack-java-module-system/#Reflectively-Accessing-Internal-APIs-With--add-opens)
to know more.

### Access related exceptions
See: https://stackoverflow.com/a/41265267

### Last resort of access in your own classes
The Invokation API enunciates the pattern of having your own classes
declare a method that returns a Lookup to themselves. You could then
pass the result of calling that method on a target class to this library.
