/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for org.apache.tools.ant.util.FileUtils.
 *
 */
public class FileUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final String ROOT = System.getProperty("root");
    private File removeThis;
    private String root;

    @Before
    public void setUp() {
        // Windows adds the drive letter in uppercase, unless you run Cygwin
        root = new File(File.separator).getAbsolutePath().toUpperCase();
    }

    @After
    public void tearDown() {
        if (removeThis != null && removeThis.exists() && !removeThis.delete()) {
            removeThis.deleteOnExit();
        }
    }

    /**
     * test modification.
     * Since Ant1.7, the method being tested no longer uses
     * reflection to provide backwards support to Java1.1, so this
     * test is not so critical. But it does explore file system
     * behaviour and will help catch any regression in Java itself,
     * so is worth retaining.
     * @see FileUtils#setFileLastModified(java.io.File, long)
     * @throws IOException if something goes wrong
     */
    @Test
    public void testSetLastModified() throws IOException {
        removeThis = new File("dummy");
        FileOutputStream fos = new FileOutputStream(removeThis);
        fos.write(new byte[0]);
        fos.close();
        assumeTrue("Could not change file modified time",
                removeThis.setLastModified(removeThis.lastModified() - 2000));
        long modTime = removeThis.lastModified();
        assertNotEquals(0, modTime);


        FILE_UTILS.setFileLastModified(removeThis, -1);
        long secondModTime = removeThis.lastModified();
        assertTrue(secondModTime > modTime);

        // number of milliseconds in a day
        final int millisperday = 24 * 3600 * 1000;
        // in a previous version, the date of the file was set to 123456
        // milliseconds since 01.01.1970
        // it did not work on a computer running JDK 1.4.1_02 + Windows 2000
        FILE_UTILS.setFileLastModified(removeThis, secondModTime + millisperday);
        long thirdModTime = removeThis.lastModified();
        /*
         * I would love to compare this with 123456, but depending on
         * the filesystems granularity it can take an arbitrary value.
         *
         * Just assert the time has changed.
         */
        assertNotEquals(thirdModTime, secondModTime);
    }

    @Test
    public void testResolveFilePosix() {
        assumeTrue("DOS or NetWare", !Os.isFamily("dos") && !Os.isFamily("netware"));
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator, FILE_UTILS.resolveFile(null, "/").getPath());
        assertEquals(File.separator, FILE_UTILS.resolveFile(null, "\\").getPath());
    }

    @Test
    public void testResolveFileDosOrNetware() {
        assumeTrue("Not DOS or Netware", Os.isFamily("dos") || Os.isFamily("netware"));
        assertEqualsIgnoreDriveCase(localize(File.separator),
                FILE_UTILS.resolveFile(null, "/").getPath());
        assertEqualsIgnoreDriveCase(localize(File.separator),
                FILE_UTILS.resolveFile(null, "\\").getPath());
        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec + "\\",
                FILE_UTILS.resolveFile(null, driveSpec + "/").getPath());
        assertEquals(driveSpec + "\\",
                FILE_UTILS.resolveFile(null, driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpecLower + "\\",
                FILE_UTILS.resolveFile(null, driveSpecLower + "/").getPath());
        assertEquals(driveSpecLower + "\\",
                FILE_UTILS.resolveFile(null, driveSpecLower + "\\").getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\",
                FILE_UTILS.resolveFile(null, driveSpec + "/////").getPath());
        assertEquals(driveSpec + "\\",
                FILE_UTILS.resolveFile(null, driveSpec + "\\\\\\\\\\\\").getPath());

        if (Os.isFamily("netware")) {
            /*
             * throw in NetWare volume names
             */
            driveSpec = "SYS:";
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpec + "/").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpec + "\\").getPath());
            driveSpecLower = "sys:";
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpecLower + "/").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpecLower + "\\").getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpec + "/////").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.resolveFile(null, driveSpec + "\\\\\\\\\\\\").getPath());
        }
    }
    @Test
    public void testResolveFileNotDos() {
        assumeFalse("is DOS", Os.isFamily("dos"));
        /*
         * drive letters must be considered just normal filenames.
         */
        String driveSpec = "C:";
        String udir = System.getProperty("user.dir");
        assertEquals(udir + File.separator + driveSpec,
                FILE_UTILS.resolveFile(null, driveSpec + "/").getPath());
        assertEquals(udir + File.separator + driveSpec,
                FILE_UTILS.resolveFile(null, driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(udir + File.separator + driveSpecLower,
                FILE_UTILS.resolveFile(null, driveSpecLower + "/").getPath());
        assertEquals(udir + File.separator + driveSpecLower,
                FILE_UTILS.resolveFile(null, driveSpecLower + "\\").getPath());
    }

    /*
     * Test some relative file name magic.
     */
    @Test
    public void testResolveRelativeFile() {
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "./4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), ".\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "./.\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "../3/4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "..\\3\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "../../5/.././2/./3/6/../4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "..\\../5/..\\./2/./3/6\\../4").getPath());

        assertEquals("meaningless result but no exception",
                new File(localize("/1/../../b")),
                FILE_UTILS.resolveFile(new File(localize("/1")), "../../b"));

    }

    @Test
    public void testNormalizePosix() {
        assumeTrue("DOS or NetWare", !Os.isFamily("dos") && !Os.isFamily("netware"));
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator, FILE_UTILS.normalize("/").getPath());
        assertEquals(File.separator, FILE_UTILS.normalize("\\").getPath());

        // Expected exception caught
        thrown.expect(BuildException.class);
        String driveSpec = "C:";
        assertEquals(driveSpec, FILE_UTILS.normalize(driveSpec).getPath());
    }

    @Test
    public void testNormalizeSlashDosOrNetware() {
        assumeTrue("Not DOS or Netware", Os.isFamily("dos") || Os.isFamily("netware"));
        thrown.expect(BuildException.class);
        FILE_UTILS.normalize("/").getPath();
    }

    @Test
    public void testNormalizeBackSlashDosOrNetware() {
        assumeTrue("Not DOS or Netware", Os.isFamily("dos") || Os.isFamily("netware"));
        thrown.expect(BuildException.class);
        FILE_UTILS.normalize("\\").getPath();
    }

    @Test
    public void testNormalizeDosOrNetware() {
        assumeTrue("Not DOS or Netware", Os.isFamily("dos") || Os.isFamily("netware"));
        if (Os.isFamily("dos")) {
            /*
             * throw in drive letters
             */
            String driveSpec = "C:";
            assertEquals(driveSpec + "\\",
                    FILE_UTILS.normalize(driveSpec + "/").getPath());
            assertEquals(driveSpec + "\\",
                    FILE_UTILS.normalize(driveSpec + "\\").getPath());
            String driveSpecLower = "c:";
            assertEquals(driveSpecLower + "\\",
                    FILE_UTILS.normalize(driveSpecLower + "/").getPath());
            assertEquals(driveSpecLower + "\\",
                    FILE_UTILS.normalize(driveSpecLower + "\\").getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEquals(driveSpec + "\\",
                    FILE_UTILS.normalize(driveSpec + "/////").getPath());
            assertEquals(driveSpec + "\\",
                    FILE_UTILS.normalize(driveSpec + "\\\\\\\\\\\\").getPath());

            // Expected exception caught
            thrown.expect(BuildException.class);
            FILE_UTILS.normalize(driveSpec).getPath();
        } else if (Os.isFamily("netware")) {
            /*
             * throw in NetWare volume names
             */
            String driveSpec = "SYS:";
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpec).getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpec + "/").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpec + "\\").getPath());
            String driveSpecLower = "sys:";
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpecLower).getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpecLower + "/").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpecLower + "\\").getPath());
            assertEquals(driveSpec + "\\junk",
                    FILE_UTILS.normalize(driveSpecLower + "\\junk").getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpec + "/////").getPath());
            assertEquals(driveSpec,
                    FILE_UTILS.normalize(driveSpec + "\\\\\\\\\\\\").getPath());
        }
    }

    /**
     * Test some relative file name magic.
     */
    @Test
    public void testNormalizeRelativeFile() {
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/./4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/./.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/../3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/..\\3\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/../../5/.././2/./3/6/../4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/..\\../5/..\\./2/./3/6\\../4")).getPath());

        assertEquals("will not go outside FS root (but will not throw an exception either)",
                new File(localize("/1/../../b")),
                FILE_UTILS.normalize(localize("/1/../../b")));

        // Expected exception caught
        thrown.expect(BuildException.class);
        FILE_UTILS.normalize("foo");
    }

    /**
     * Test handling of null arguments.
     */
    @Test
    public void testNullArgs() {
        File f = FILE_UTILS.resolveFile(null, "a");
        assertEquals(f, new File("a").getAbsoluteFile());

        // Expected exception caught
        thrown.expect(NullPointerException.class);
        FILE_UTILS.normalize(null);
    }


    /**
     * Test createTempFile
     */
    @Test
    public void testCreateTempFile() {
        // null parent dir
        File tmp1 = FILE_UTILS.createTempFile("pre", ".suf", null, false, true);
        String tmploc = System.getProperty("java.io.tmpdir");
        String name = tmp1.getName();
        assertThat("starts with pre", name, startsWith("pre"));
        assertThat("ends with .suf", name, endsWith(".suf"));
        assertTrue("File was created", tmp1.exists());
        assertEquals((new File(tmploc, tmp1.getName())).getAbsolutePath(),
                tmp1.getAbsolutePath());
        tmp1.delete();

        File dir2 = new File(tmploc + "/ant-test");
        dir2.mkdir();
        removeThis = dir2;

        File tmp2 = FILE_UTILS.createTempFile("pre", ".suf", dir2, true, true);
        String name2 = tmp2.getName();
        assertThat("starts with pre", name2, startsWith("pre"));
        assertThat("ends with .suf", name2, endsWith(".suf"));
        assertTrue("File was created", tmp2.exists());
        assertEquals((new File(dir2, tmp2.getName())).getAbsolutePath(),
                tmp2.getAbsolutePath());
        tmp2.delete();
        dir2.delete();

        File parent = new File((new File("/tmp")).getAbsolutePath());
        tmp1 = FILE_UTILS.createTempFile("pre", ".suf", parent, false);
        assertFalse("new file", tmp1.exists());

        name = tmp1.getName();
        assertThat("starts with pre", name, startsWith("pre"));
        assertThat("ends with .suf", name, endsWith(".suf"));
        assertEquals("is inside parent dir", parent.getAbsolutePath(), tmp1
                .getParent());

        tmp2 = FILE_UTILS.createTempFile("pre", ".suf", parent, false);
        assertNotEquals("files are different", tmp1.getAbsolutePath(), tmp2.getAbsolutePath());

        // null parent dir
        File tmp3 = FILE_UTILS.createTempFile("pre", ".suf", null, false);
        tmploc = System.getProperty("java.io.tmpdir");
        assertEquals((new File(tmploc, tmp3.getName())).getAbsolutePath(),
                tmp3.getAbsolutePath());
    }

    /**
     * Test contentEquals
     */
    @Test
    public void testContentEquals() throws IOException {
        assertTrue("Non existing files",
                FILE_UTILS.contentEquals(new File(ROOT, "foo"),
                        new File(ROOT, "bar")));
        assertFalse("One exists, the other one doesn\'t",
                FILE_UTILS.contentEquals(new File(ROOT, "foo"),
                        new File(ROOT, "build.xml")));
        assertFalse("Don\'t compare directories",
                FILE_UTILS.contentEquals(new File(ROOT, "src"),
                        new File(ROOT, "src")));
        assertTrue("File equals itself",
                FILE_UTILS.contentEquals(new File(ROOT, "build.xml"),
                        new File(ROOT, "build.xml")));
        assertFalse("Files are different",
                FILE_UTILS.contentEquals(new File(ROOT, "build.xml"),
                        new File(ROOT, "docs.xml")));
    }

    /**
     * Test createNewFile
     */
    @Test
    public void testCreateNewFile() throws IOException {
        removeThis = new File("dummy");
        assertFalse(removeThis.exists());
        FILE_UTILS.createNewFile(removeThis);
        assertTrue(removeThis.exists());
    }

    /**
     * Test removeLeadingPath.
     */
    @Test
    public void testRemoveLeadingPath() {
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("/foo"),
                                                 new File("/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("/foo/"),
                                                 new File("/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("\\foo"),
                                                 new File("\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("\\foo\\"),
                                                 new File("\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:/foo"),
                                                 new File("c:/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:/foo/"),
                                                 new File("c:/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:\\foo"),
                                                 new File("c:\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:\\foo\\"),
                                                 new File("c:\\foo\\bar")));
        if (!Os.isFamily("dos") && !Os.isFamily("netware")) {
            assertEquals(FILE_UTILS.normalize("/bar").getAbsolutePath(),
                         FILE_UTILS.removeLeadingPath(new File("/foo"), new File("/bar")));
            assertEquals(FILE_UTILS.normalize("/foobar").getAbsolutePath(),
                         FILE_UTILS.removeLeadingPath(new File("/foo"), new File("/foobar")));
        }
        // bugzilla report 19979
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar"),
                                              new File("/foo/bar")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar"),
                                              new File("/foo/bar/")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar/"),
                                              new File("/foo/bar/")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar/"),
                                              new File("/foo/bar")));

        String expected = "foo/bar".replace('\\', File.separatorChar)
            .replace('/', File.separatorChar);
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("/"),
                                                    new File("/foo/bar")));
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("c:/"),
                                                    new File("c:/foo/bar")));
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("c:\\"),
                                                    new File("c:\\foo\\bar")));
    }

    /**
     * test toUri
     */
    @Test
    public void testToURI() {
        String dosRoot;
        if (Os.isFamily("dos") || Os.isFamily("netware")) {
            dosRoot = System.getProperty("user.dir")
                .substring(0, 3).replace(File.separatorChar, '/');
        } else {
            dosRoot = "";
        }
        if (Os.isFamily("dos")) {
            assertEquals("file:/c:/foo", removeExtraneousAuthority(FILE_UTILS.toURI("c:\\foo")));
        }
        if (Os.isFamily("netware")) {
            assertEquals("file:/SYS:/foo", removeExtraneousAuthority(FILE_UTILS.toURI("sys:\\foo")));
        }
        if (File.pathSeparatorChar == '/') {
            assertEquals("file:/foo", removeExtraneousAuthority(FILE_UTILS.toURI("/foo")));
            assertThat("file: URIs must name absolute paths", FILE_UTILS.toURI("./foo"), startsWith("file:/"));
            assertThat(FILE_UTILS.toURI("./foo"), endsWith("/foo"));
            assertEquals("file:/" + dosRoot + "foo%20bar", removeExtraneousAuthority(FILE_UTILS.toURI("/foo bar")));
            assertEquals("file:/" + dosRoot + "foo%23bar", removeExtraneousAuthority(FILE_UTILS.toURI("/foo#bar")));
        } else if (File.pathSeparatorChar == '\\') {
            assertEquals("file:/" + dosRoot + "foo", removeExtraneousAuthority(FILE_UTILS.toURI("\\foo")));
            assertThat("file: URIs must name absolute paths", FILE_UTILS.toURI(".\\foo"), startsWith("file:/"));
            assertThat(FILE_UTILS.toURI(".\\foo"), endsWith("/foo"));
            assertEquals("file:/" + dosRoot + "foo%20bar", removeExtraneousAuthority(FILE_UTILS.toURI("\\foo bar")));
            assertEquals("file:/" + dosRoot + "foo%23bar", removeExtraneousAuthority(FILE_UTILS.toURI("\\foo#bar")));
        }
        // a test with ant for germans
        // the escaped character used for the test is the "a umlaut"
        // this is the fix for the bug 37348
        assertEquals("file:/" + dosRoot + "%C3%A4nt", removeExtraneousAuthority(FILE_UTILS.toURI("/\u00E4nt")));
    }

    /**
     * Authority field is unnecessary, but harmless, in file: URIs.
     * Java 1.4 does not produce it when using File.toURI.
     */
    private static String removeExtraneousAuthority(String uri) {
        String prefix = "file:///";
        if (uri.startsWith(prefix)) {
            return "file:/" + uri.substring(prefix.length());
        } else {
            return uri;
        }
    }

    @Test
    public void testIsContextRelativePath() {
        assumeTrue("Test only runs on DOS", Os.isFamily("dos"));
        assertTrue(FileUtils.isContextRelativePath("/\u00E4nt"));
        assertTrue(FileUtils.isContextRelativePath("\\foo"));
    }

    /**
     * test fromUri
     */
    @Test
    public void testFromURI() {
        String dosRoot;
        if (Os.isFamily("dos") || Os.isFamily("netware")) {
            dosRoot = System.getProperty("user.dir").substring(0, 2);
        } else {
            dosRoot = "";
        }
        if (Os.isFamily("netware")) {
            assertEqualsIgnoreDriveCase("SYS:\\foo", FILE_UTILS.fromURI("file:///sys:/foo"));
        }
        if (Os.isFamily("dos")) {
            assertEqualsIgnoreDriveCase("C:\\foo", FILE_UTILS.fromURI("file:///c:/foo"));
        }
        assertEqualsIgnoreDriveCase(dosRoot + File.separator + "foo", FILE_UTILS.fromURI("file:///foo"));
        assertEquals("." + File.separator + "foo",
                     FILE_UTILS.fromURI("file:./foo"));
        assertEquals(dosRoot + File.separator + "foo bar", FILE_UTILS.fromURI("file:///foo%20bar"));
        assertEquals(dosRoot + File.separator + "foo#bar", FILE_UTILS.fromURI("file:///foo%23bar"));
    }

    @Test
    public void testModificationTests() {

        //get a time
        long firstTime = System.currentTimeMillis();
        //add some time. We assume no OS has a granularity this bad
        long secondTime = firstTime + 60000;
        /*
        assertTrue("same timestamp is up to date",
                fu.isUpToDate(firstTime, firstTime));
         */

        //check that older is up to date with a newer dest
        assertTrue("older source files are up to date",
                FILE_UTILS.isUpToDate(firstTime,secondTime));
        //check that older is up to date with a newer dest
        assertFalse("newer source files are no up to date",
                FILE_UTILS.isUpToDate(secondTime, firstTime));

        assertFalse("-1 dest timestamp implies nonexistence",
                FILE_UTILS.isUpToDate(firstTime, -1L));
    }

    @Test
    public void testHasErrorInCase() {
        File tempFolder = new File(System.getProperty("java.io.tmpdir"));
        File wellcased = FILE_UTILS.createTempFile("alpha", "beta", tempFolder,
                                                   true, true);
        String s = wellcased.getName().toUpperCase();
        File wrongcased = new File(tempFolder, s);
        if (Os.isFamily("mac") && Os.isFamily("unix")) {
            //no guarantees on filesystem case-sensitivity
        } else if (Os.isFamily("dos")) {
            assertTrue(FILE_UTILS.hasErrorInCase(wrongcased));
            assertFalse(FILE_UTILS.hasErrorInCase(wellcased));
        } else {
            assertFalse(FILE_UTILS.hasErrorInCase(wrongcased));
            assertFalse(FILE_UTILS.hasErrorInCase(wellcased));
        }

    }

    @Test
    public void testGetDefaultEncoding() {
        // This just tests that the function does not blow up
        FILE_UTILS.getDefaultEncoding();
    }

    /**
     * @see "https://bz.apache.org/bugzilla/show_bug.cgi?id=62502"
     */
    @Test
    public void isLeadingPathCannotBeFooledByTooManyDoubleDots() {
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/../../bar")));
        assertFalse(FILE_UTILS.isLeadingPath(new File("c:\\foo"), new File("c:\\foo\\..\\..\\bar")));
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/../..")));
    }

    /**
     * @see "https://bz.apache.org/bugzilla/show_bug.cgi?id=62502"
     */
    @Test
    public void isLeadingPathCanonicalVersionCannotBeFooledByTooManyDoubleDots() throws IOException {
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/../../bar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("c:\\foo"), new File("c:\\foo\\..\\..\\bar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/../.."), true));
    }

    @Test
    public void isLeadingPathCanonicalVersionWorksAsExpectedOnUnix() throws IOException {
        assumeFalse("Test doesn't run on DOS", Os.isFamily("dos"));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/baz/../bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/../foo/bar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foobar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/bar"), true));
    }

    @Test
    public void isLeadingPathAndTrailingSlashesOnUnix() throws IOException {
        assumeFalse("Test doesn't run on DOS", Os.isFamily("dos"));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/bar/"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/"), true));

        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/bar"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/bar/"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo/"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo/"), new File("/foo"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("/foo"), new File("/foo/"), false));
    }

    @Test
    public void isLeadingPathCanonicalVersionWorksAsExpectedOnDos() throws IOException {
        assumeTrue("Test only runs on DOS", Os.isFamily("dos"));
        assertTrue(FILE_UTILS.isLeadingPath(new File("C:\\foo"), new File("C:\\foo\\bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("C:\\foo"), new File("C:\\foo\\baz\\..\\bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("C:\\foo"), new File("C:\\foo\\..\\foo\\bar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("C:\\foo"), new File("C:\\foobar"), true));
        assertFalse(FILE_UTILS.isLeadingPath(new File("C:\\foo"), new File("C:\\bar"), true));
    }

    @Test
    public void isLeadingPathAndTrailingSlashesOnDos() throws IOException {
        assumeTrue("Test only runs on DOS", Os.isFamily("dos"));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\bar"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\bar\\"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo"), true));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo"), new File("c:\\foo\\"), true));

        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\bar"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\bar\\"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo\\"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo\\"), new File("c:\\foo"), false));
        assertTrue(FILE_UTILS.isLeadingPath(new File("c:\\foo"), new File("c:\\foo\\"), false));
    }

    /**
     * adapt file separators to local conventions
     */
    private String localize(String path) {
        path = root + path.substring(1);
        return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }

    /**
     * convenience method
     * normalize brings the drive in uppercase
     * the drive letter is in lower case under cygwin
     * calling this method allows tests where normalize is called to pass under cygwin
     */
    private void assertEqualsIgnoreDriveCase(String s1, String s2) {
        assumeTrue("Not DOS or Netware", Os.isFamily("dos") || Os.isFamily("netware"));
        if (!s1.isEmpty() && !s2.isEmpty()) {
            StringBuilder sb1 = new StringBuilder(s1);
            StringBuilder sb2 = new StringBuilder(s2);
            sb1.setCharAt(0, Character.toUpperCase(s1.charAt(0)));
            sb2.setCharAt(0, Character.toUpperCase(s2.charAt(0)));
            assertEquals(sb1.toString(), sb2.toString());
        } else {
            assertEquals(s1, s2);
        }
    }
}
