package com.github.fge.largetext;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Geoff on 3/26/2016.
 */
public class MutableLargeTextFileFixture {

    private static final Path SimpleThreeLineUTF8Doc;

    static{
        try { SimpleThreeLineUTF8Doc = Paths.get(MutableLargeTextFileFixture.class.getResource("ThreeLineSimple.txt").toURI()); }
        catch (URISyntaxException e) { throw new RuntimeException(e); }
    }

    private Path fileUnderTest;

    @Test
    public void when_writing_to_new_target_without_modification_should_be_file_copy() throws IOException, URISyntaxException {
        //setup
        MutableLargeTextFile mutable = makeMutableText(SimpleThreeLineUTF8Doc);
        fileUnderTest = generateNewFileFor(SimpleThreeLineUTF8Doc);

        //act
        mutable.writeTo(fileUnderTest);

        //assert
        assertThat(fileUnderTest.toFile()).hasContentEqualTo(SimpleThreeLineUTF8Doc.toFile());
    }

    private Path generateNewFileFor(Path fileColleague) {
        return fileColleague.getParent().resolve(fileColleague.getFileName().toString() + "." + UUID.randomUUID() + ".re-written");
    }

    @Test
    public void when_appending_change_to_new_file_with_should_include_change() throws IOException, URISyntaxException {

        //setup
        MutableLargeTextFile mutable = makeMutableText(SimpleThreeLineUTF8Doc);
        fileUnderTest = generateNewFileFor(SimpleThreeLineUTF8Doc);

        //act
        mutable.append("new seq!\n", 104, 105);
        mutable.writeTo(fileUnderTest);

        //assert
        assertThat(fileUnderTest.toFile()).hasContent(
                "This is the first line, it only contains words\n" +
                "1.23456\n" +
                "This is the last line, it contains more words\n" +
                "new seq!\n"
        );
    }

    @Test
    public void when_prepending_to_new_file_should_include_change() throws IOException {
        //setup
        MutableLargeTextFile mutable = makeMutableText(SimpleThreeLineUTF8Doc);
        fileUnderTest = generateNewFileFor(SimpleThreeLineUTF8Doc);

        //act
        mutable.append("new seq!\n", 0, 1);
        mutable.writeTo(fileUnderTest);

        //assert
        assertThat(fileUnderTest.toFile()).hasContent(
                "new seq!\n" +
                "This is the first line, it only contains words\n" +
                "1.23456\n" +
                "This is the last line, it contains more words\n"
        );
    }

    private MutableLargeTextFile makeMutableText(Path pathToPreloadedContent) throws IOException {
        LargeText largeText = LargeTextFactory.defaultFactory().load(pathToPreloadedContent);
        return new MutableLargeTextFile(largeText);
    }

    @AfterMethod
    public void delete_file_under_test() throws IOException {
        if(fileUnderTest != null){
            Files.delete(fileUnderTest);
        }
    }
}