# kotlin-json

This project is forked from [JKid](https://github.com/yole/jkid). 


### About JKid

JKid is a simple JSON serialization/deserialization library for Kotlin data classes.
To serialize or deserialize an object use the 'serialize' and 'deserialize' functions. Also, it accompanies the Chapter 10, "Introspecting Kotlin Code" of the [Kotlin in Action](https://www.manning.com/books/kotlin-in-action) book.

### Exercise

- [x] Support the annotation `DateFormat`
  - The Annotation `DateFormat` allows to annotate the date property with `@DateFormat("dd-MM-yyyy")` specifying the date format as an argument.
    

- [ ] Support maps as property values
  - Make JKid support serialization and deserialization of maps as property values. For now it supports only objects and collections.
The example is in the file `test/kotlin/exercise/Map.kt`. Remove `@Ignore` from the test `MapTest` and make it pass.
To support deserialization of maps, create a class `MapSeed` similar to `ObjectSeed` and collection seeds.
The function `createSeedForType` should now return an instance of `MapSeed` if a map is expected.
The example solution can be found in the branch `solution-map`.

### Additional Features
- This project is forked from JKid and will provide additional features.