package com.github.fge.largetext;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;

/**
 * String-builder-ish wrapper on LargeText.
 *
 * TODO
 */
public class MutableLargeTextFile implements Appendable, CharSequence{

    private final LargeText source;
    private final RangeMap<Integer, CharSequence> changes = TreeRangeMap.create();

    public MutableLargeTextFile(LargeText source) {
        this.source = source;

        // note that the target file cannot be retrieved from a LargeText object,
        // either add a private API or tweak the ctor here to invoke the builder or some kind of delegate.
        // a jul.Function<Path, LargeText> would suffice and be reasonably easy to implement with a LargeTextFactory
        // instance, but is Java-8 only.
    }

    /**
     * Writes the current contents of this StringBuilder-ish object to a file at the specified path
     *
     * TODO
     */
    public void writeTo(Path newTarget){
        // let 'ranges' be an iterator over the universe of ranges implied by 'changes'
        // (ie, if changes contains [3, 5), [7, 9) then 'ranges' is over [0, 3), [3, 5), [5, 7), [7, 9)
        // let 'targetChannel' be a FileChannel at newTarget
        // let 'sourceChannel' be a FileChannel at source

        // for each range 'currentRange' in ranges
        //   let byteRange = convertCharIdxToByteIdx(source, currentRange);
        //   if changes contains currentRange:
        //     let fileChannelFriendlyBuffer = new DMAableBuffer(changes.get(currentRange));
        //     targetChannel.write(fileChannelFriendlyBuffer, byteRange);
        //   otherwise:
        //     targetChannel.transferFrom(sourceChannel, byteRange)

        // maybe do some funny business with an auto-appended '.part' file extension
        // to ~gracefully fail if the JVM dies mid transfer.
    }

    /**
     * Writes the current contents to the source file, deleting all existing content in the source file
     */
    public void writeThrough(){
        // this method more-or-less delegates to the above method except that we delete source and replace it with our file.
        // this method will likely involve just a couple of calls to the java.nio.file.Files object,
        // could let it be the callers problem to implement it.
    }

    // the more I think about these methods
    // the more I think they're solving the issue of writing at the wrong abstraction level,
    // what if we want to append to a file, what if we want to control the file creation strategy?
    // The encoding scheme?
    // perhalps another method...

    /**
     * Dumps the current contents into the channel in as few and as fast a set of operations as possible.
     */
    // this gives us both a very high level API (paths) and a very low granular one (FileChannels).
    public void writeTo(FileChannel channel){
        throw new UnsupportedOperationException();
    }

    @Override
    public MutableLargeTextFile append(CharSequence csq) throws IOException {
        Range targetSite = Range.closedOpen(source.length(), source.length() + csq.length());
        changes.put(targetSite, csq);

        return this;
    }

    @Override
    public MutableLargeTextFile append(CharSequence csq, int start, int end) throws IOException {

        //TODO validate start and end?

        changes.put(Range.closedOpen(start, end), csq);

        return this;
    }

    @Override
    public MutableLargeTextFile append(char c) throws IOException {
        Range<Integer> targetSite = Range.singleton(source.length());
        changes.put(targetSite, CharBuffer.wrap(new char[]{c}));

        return this;
    }

    @Override
    public int length() {
        return changes.span().upperEndpoint() + 1;
    }

    @Override
    public char charAt(int index) {
        Map.Entry<Range<Integer>, CharSequence> changedValue = changes.getEntry(index);

        if(changedValue != null){
            int indexIntoChange = index - changedValue.getKey().lowerEndpoint();
            return changedValue.getValue().charAt(indexIntoChange);
        }

        return source.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // uhh, some kind of forwarding or delegating thing must exist
        // effectively all we need to do here is override charAt and subtract 'start' from the supplied value
        // and maybe validation
        // for performance however, we might want to try to delegate to source's subSequence as much as possible.
        // hmm.
        throw new UnsupportedOperationException();
    }
}
