## What this is

This library allows you to use very large (up to a few GiB) text files as
[`CharSequence`](http://docs.oracle.com/javase/8/docs/api/java/lang/CharSequence.html).

OK, this does not sound very sexy, but please read on!

## Motivation; and a bit of history

This project stemmed from a [discussion on
StackOverflow](http://stackoverflow.com/q/22017480/1093528) where I suggested that the OP (in
StackOverflow jargon, that means the user asking a question) implemented `CharSequence` over a large
text file.

Even though the answer was accepted, well, nothing existed for that; and since I like a challenge
(and there are many in this case), instead of just being satisfied with the answer, I decided to
have a go at it; hence this project was born.

But the story does not end here. Since then I have also been working on
[Grappa](https://github.com/parboiled1/grappa), which is Parboiled (v1) continued; having this
package in a corner of my mind, I decided to add `CharSequence` support.

So there we are: you can now use not only regexes, but **full fledged parboiled1/grappa grammars**,
on large text files without worrying about memory consumption since you **DO NOT** need to load the
whole file into memory; all of this thanks to a very simple interface which has been there since
Java 1.4!

## Versions

The current version is **0.2.0**. Javadoc is [available online](http://fge.github.io/largetext/). It
is available on Maven Central.

Using [gradle](http://gradle.org):

```groovy
dependencies {
    compile(group: "com.github.fge", name: "largetext", version: "0.1.0");
}
```

Using maven:

```xml
<dependency>
    <groupId>com.github.fge</groupId>
    <artifactId>largetext</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Warning about `.toString()`!

Yes, this very simple, seemingly innocuous method is this package's death trap. The `CharSequence`
contract stipulates that its `.toString()` implementation must return a string whose length and
contents are that of the sequence; but we deal here with files which can potentially contain
_billions_ of charaters... And this means a billion character long string.

Using `.toString()` will therefore more than likely result in an `OutOfMemory` error, not to mention
such an error will be triggered after an inordinate amount of time... The current version does not
deal with that, so, at this moment, the only thing I can say is:

**DON'T DO THAT**

Beware when debugging!

## Quick usage

The first thing to do is to create a `LargeTextFactory`. You can customize a factory in two ways:

* specify the character encoding (`Charset` in Java) of your files;
* specify the size of byte windows for the decoding process (see below).

Sample code:

```java
// Default factory
final LargeTextFactory factory = LargeTextFactory.defaultFactory();
// Submit your own charset and window size
final LargeTextFactory factory = LargeTextFactory.newBuilder()
    .setCharset(StandardCharsets.US_ASCII) // either a Charset instance
    .setCharsetByName("windows-1252")      // or by name
    .setWindowSize(16, SizeUnit.MiB)        // set the window size
    .build();
```

The default factory uses UTF-8 as a character encoding and a 2 MiB byte window.

Then you create a `LargeText` instance; for this, you need the `Path` to the file.

Note that `LargeText` implements `Closeable` in addition to `CharSequence`, so it is important that
you use it this way... Otherwise the file descriptor associated with it will stay open! Therefore:

```java
final Path bigTextFile = Paths.get("/path/to/bigtextfile");

try (
    final LargeText largeText = factory.fromPath(bigTextFile);
) {
    // use "largeText" here
}
```

As mentioned in the introduction, the fact that it implements `CharSequence` means you can use it
with regexes:

```java
// You need Pattern.MULTILINE if you mean to match lines within
// the file! Otherwise "^" and "$" will only match the beginning
// and end of input (ie, the whole file) respectively.
private static final Pattern PATTERN = Pattern.compile("^\\d{4}:",
    Pattern.MULTILINE);

// In code:
final Path bigTextFile = Paths.get("/path/to/bigtextfile");

try (
    final LargeText largeText = factory.fromPath(bigTextFile);
) {
    final Matcher m = PATTERN.matcher(largeText);
    while (m.find())
        System.out.println("Match: " + m.group());
}
```

## Limitations

The limitations are that of `CharSequence` (which is reflected in all their implementations): if you
have more than `Integer.MAX_VALUE` characters in your file, you cannot use this class reliably!

