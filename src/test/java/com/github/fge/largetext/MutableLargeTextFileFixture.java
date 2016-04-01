package com.github.fge.largetext;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Geoff on 3/26/2016.
 */
public class MutableLargeTextFileFixture {

    @Test
    public void when_writing_to_new_target_without_modification_should_be_file_copy() throws IOException, URISyntaxException {
        //setup
        Path source = Paths.get(getClass().getResource("ThreeLineSimple.txt").toURI());
        LargeText largeText = LargeTextFactory.defaultFactory().load(source);
        MutableLargeTextFile mutable = new MutableLargeTextFile(largeText);
        Path target = source.getParent().resolve(source.getFileName().toString() + ".re-written");

        //act
        mutable.writeTo(target);

        //assert
        assertThat(target.toFile()).hasContentEqualTo(source.toFile());
    }

    @Test
    public void when_writing_to_new_file_with_one_change_new_file_should_include_change(){
        Path source = Paths.get(getClass().getResource("ThreeLineSimple.txt").toURI());
        LargeText largeText = LargeTextFactory.defaultFactory().load(source);
        MutableLargeTextFile mutable = new MutableLargeTextFile(largeText);
        Path target = source.getParent().resolve(source.getFileName().toString() + ".re-written");

        //act
        mutable.writeTo(target);

        //assert
        assertThat(target.toFile()).hasContentEqualTo(source.toFile());
    }
}