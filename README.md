## What this is

In [this StackOverflow question](http://stackoverflow.com/q/22017480/1093528), I
suggested to map a large text file to Java's
[`CharSequence`](http://docs.oracle.com/javase/7/docs/api/java/lang/CharSequence.html)
since when you build a `Matcher` from a `Pattern`, the expected argument is a
`CharSequence`.

The interface is quite simple; however, implementing it is another story.

This is a try at an implementation of this idea.

**NOTE: requires Java 7**

