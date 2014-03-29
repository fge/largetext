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

It works! Some parts are heavily tested, some are not.

And it seriously lacks documentation, except for the following paragraph...

## Usage

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
you use it this way...  Otherwise the file descriptor associated with it will stay open! Therefore:

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

## How it works internally

There are two essential core classes to the `LargeText` class:

* `TextDecoder`: this class decodes the text file chunk by chunk, in the background;
* `TextLoader`: this class uses a `LoadingCache` (from Guava) to provide `CharBuffer` instances to
  the methods requiring it.

### `TextDecoder` and waiting operations

Each of the `.charAt()`, `.subSequence()` and `.length()` methods of `LargeText` can potentially
require the caller to wait until the decoder has processed up to the number of required characters
(all of them in the case of `.length()`).

This class therefore queues a list of waiters (in a `DecodeStatus` instance) which it wakes _in
order_. That is, a waiter on 231000 chars will be woken up before a waiter on 562000 chars.

When a waiter is woken up, it is guaranteed that the decoding went OK up to this point, which means
the caller can now obtain its result.

If the process is interrupted for one reason or another, the interrupt status is restored and a
`RuntimeException` is thrown.

Note that it may happen that a byte window will not exactly map to a char window; the decoding
process detects that and restarts from the appropriate offset in the file.

### `TextLoader`

As mentioned earlier, this uses a `LoadingCache`. It will load, if needed, a new `CharBuffer` from
the file by mapping the required zone from the file and decoding it using a `CharsetDecoder`.

The default expiry policy (not configurable at this moment) is 30 seconds after last access.

## Limitations

The limitations are that of `CharSequence` (which is reflected in all their implementations): if you
have more than `Integer.MAX_VALUE` characters in your file, you cannot use this class reliably!

