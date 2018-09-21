package de.mein.drive.serialization;

import de.mein.auth.data.access.CertificateManager;
import de.mein.auth.file.FFile;
import de.mein.drive.bash.BashTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;

/**
 * Created by thefa on 7/29/2017.
 */
public class BashCommandsTest {
    File testDir = new File("testdir1");
    List<String> paths;

    @Before
    public void before() throws IOException {
        BashTools.init();
        CertificateManager.deleteDirectory(testDir);
        paths = TestDirCreator.createTestDir(testDir);
    }

    @Test
    public void bashFind() throws Exception {
        Iterator<String> iterator = BashTools.find(new FFile(testDir), new FFile("blaaaa"));
        while (iterator.hasNext())
            Lok.debug(iterator.next());
        Lok.debug("BashCommandsTest.bashtest.end");
    }

    @Test
    public void bashFindModifiedAfter1() throws Exception {
        //expect one result
        CertificateManager.deleteDirectory(testDir);
        testDir.mkdirs();
        Iterator<String> iterator = BashTools.stuffModifiedAfter(new FFile(testDir), new FFile("blaa"), 0L);
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
        Lok.debug("BashCommandsTest.bashtest.end");
    }

    @Test
    public void bashFindModifiedAfter2() throws Exception {
        //expect no result
        CertificateManager.deleteDirectory(testDir);
        testDir.mkdirs();
        Iterator<String> iterator = BashTools.stuffModifiedAfter(new FFile(testDir), new FFile("blaa"), Long.MAX_VALUE);
        assertFalse(iterator.hasNext());
        Lok.debug("BashCommandsTest.bashtest.end");
    }

    @Test
    public void bashFindModifiedAfter3() throws Exception {
        // expect whole testdir
        Iterator<String> iterator = BashTools.stuffModifiedAfter(new FFile(testDir), new FFile("blaa"), 0L);
        Iterator<String> expectedIterator = paths.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            String expected = expectedIterator.next();
            Lok.debug(path + " vs " + expected);
            assertEquals(expected, path);
        }
        assertFalse(expectedIterator.hasNext());
        Lok.debug("BashCommandsTest.bashtest.end");
    }

    @Test
    public void timestamp1() throws InterruptedException {
        Long t1 = testDir.lastModified();
        Thread.sleep(1000);
        File dir = new File(testDir.getAbsoluteFile() + File.separator + "ttttttt");
        dir.mkdirs();
        Long t2 = testDir.lastModified();
        Lok.debug("before: " + t1);
        Lok.debug("after : " + t2);
        assertTrue(t1 < t2);
    }

    @Test
    public void timestamp2() {
        File dir = new File("ttttttt");
        dir.mkdirs();
        Long t1 = dir.lastModified();
        Long tt1 = testDir.lastModified();
        testDir.renameTo(new File(dir.getAbsolutePath() + File.separator + "movedTest"));
        Long t2 = dir.lastModified();
        Long tt2 = testDir.lastModified();
        Lok.debug("upper dir before: " + t1);
        Lok.debug("upper dir after : " + t2);
        Lok.debug("moved dir before: " + tt1);
        Lok.debug("moved dir after : " + tt2);
        assertEquals(t1, t2);
        assertEquals(tt1, tt2);
    }

    @After
    public void after() {
        CertificateManager.deleteDirectory(testDir);
    }
}
