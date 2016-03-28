package com.github.fge.largetext;

import com.google.common.io.Files;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Created by Geoff on 3/26/2016.
 */
public class MutableLargeTextFileFixture {

    @Test
    public void when_writing_to_new_target_without_modification_should_be_file_copy() throws IOException {
        //setup

        Path source = Paths.get(getClass().getResource("ThreeLineSimple.txt").getPath());
        LargeText largeText = LargeTextFactory.defaultFactory().load(source);
        MutableLargeTextFile mutable = new MutableLargeTextFile(largeText);
        Path target = source.getParent().resolve(source.getFileName().toString() + ".re-written");

        //act
        mutable.writeTo(target);

        //assert
        Files.equal(source.toFile(), target.toFile());
    }
}