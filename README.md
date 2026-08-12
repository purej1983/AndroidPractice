# Kotlin Deep Dive Practice

A test-driven practice repository for experienced Android/Kotlin developers who want to deepen interview-level understanding.

## How to use it

1. Open the project in IntelliJ IDEA or Android Studio.
2. Run all tests first. They should fail because the exercise methods contain `TODO()`.
3. Implement only the production code under `src/main`.
4. Do **not** change the tests just to make them pass.
5. Run:

```bash
./gradlew test
```

If Gradle Wrapper is not present, use Android Studio/IntelliJ's Gradle runner, or run `gradle wrapper` once if Gradle is installed.

## Week 1 / Exercise 1: Kotlin scope functions

File:

`src/main/kotlin/practice/week1/ScopeFunctionsPractice.kt`

You will practise:

- `let`
- `run`
- `apply`
- `also`
- return values
- receiver (`this`) vs argument (`it`)
- object identity / mutation

The tests intentionally check not only the resulting values but also whether functions that should return the same object actually do so.

## Learning rule

Before implementing each method, write down:

- What does this scope function return?
- Is the object available as `this` or `it`?
- Why is this scope function appropriate here?

After the tests pass, try to explain why another scope function would be less clear.
