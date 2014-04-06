## What this is

In [this StackOverflow question](http://stackoverflow.com/q/22017480/1093528), I
suggested to map a large text file to Java's
[`CharSequence`](http://docs.oracle.com/javase/7/docs/api/java/lang/CharSequence.html)
since when you build a `Matcher` from a `Pattern`, the expected argument is a
`CharSequence`.

So there you are. This package does exactly that! You can now search huge text files with regexes
(there are limitations however, see below).

**NOTE: requires Java 7**

## Status

It works! However, no version is released yet.

Full Javadoc is now written and [available online](http://fge.github.io/largetext/). The javadoc
contains technical details about the implementation.

### Performance

Not up to par with other tools you could use for such purposes yet. But reasonable enough that it is
usable! For instance, searching all lines more than 10 characters long in a 800 MB file takes
approximately 12 seconds on my machine (yielding 1.6 million matches). This is faster than `python`,
and on par with `perl` but slower than the monstruously fast `grep` (3 seconds!!). This is of course
using regexes; there _are_ faster ways to do this without regexes.

### Warning about `.toString()`!

This is the most expensive operation of them all, since when you get a `CharSequence`'s
`.toString()`, you are supposed to get the contents of that `CharSequence`. Therefore a _very_ huge
`String`...

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

