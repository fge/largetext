/*
 * Copyright (c) 2003 - 2017 Tyro Payments Limited.
 * Lv1, 155 Clarence St, Sydney NSW 2000.
 * All rights reserved.
 */
package com.github.fge.largetext.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.github.fge.largetext.LargeText;
import com.github.fge.largetext.LargeTextFactory;

public final class EmptyCharSequenceTest {

    @Test
    public void testEmptyRegexMatchReturnsAnEmptyString() throws IOException
    {
        Pattern simpleEmptyCharsetPattern = Pattern.compile("'(.*?)'");
        String stringToTest = "here is some text '' will be an empty regex match";
        File temp = File.createTempFile("file-with-contents-that-match-empty-charset-regex", ".tmp");
        try (PrintWriter out = new PrintWriter(temp))
        {
            out.println(stringToTest);
        }

        final LargeTextFactory factory = LargeTextFactory.defaultFactory();

        try (LargeText largeText = factory.load(temp.toPath()))
        {
            final Matcher largeTextMatcher = simpleEmptyCharsetPattern.matcher(largeText);
            while (largeTextMatcher.find())
            {
                String emptyRegexMatch = largeTextMatcher.group(1);
                System.out.println("emptyRegexMatch = " + emptyRegexMatch);
                assertThat(emptyRegexMatch).isEqualTo("").overridingErrorMessage("LargeText did not return empty string: %s", emptyRegexMatch);
            }
        }

        temp.delete();
    }
}
