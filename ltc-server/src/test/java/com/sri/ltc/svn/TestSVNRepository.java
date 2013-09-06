/*
 * #%L
 * LaTeX Track Changes (LTC) allows collaborators on a version-controlled LaTeX writing project to view and query changes in the .tex documents.
 * %%
 * Copyright (C) 2009 - 2012 SRI International
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package com.sri.ltc.svn;

import com.sri.ltc.Utils;
import com.sri.ltc.categories.IntegrationTests;
import com.sri.ltc.versioncontrol.*;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author linda
 */
@Category(IntegrationTests.class)
public class TestSVNRepository {
    @ClassRule
    public static TemporarySVNRepository temporarySVNRepository = new TemporarySVNRepository();

    @Rule
    public TemporarySVNRepository toBeRemoved = new TemporarySVNRepository();

    @Test
    public void testUntracked() {
        assertTrue(temporarySVNRepository.getRoot().exists());

        try {
            TrackedFile trackedFile = temporarySVNRepository.getTrackedFile();
            assertTrue("tracked file is not NULL", trackedFile != null);

            File untracked = new File(trackedFile.getFile().getParentFile(), "bla");
            FileWriter writer = new FileWriter(untracked);
            writer.write("testing untracked");
            writer.close();
            assertTrue("untracked file exists", untracked.exists());
            assertEquals("file is not tracked (unknown to SVN repository)",
                    TrackedFile.Status.Unknown,
                    temporarySVNRepository.getRepository().getFile(untracked).getStatus());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRemotes() {
        Set<Remote> remotes = temporarySVNRepository.getRepository().getRemotes().get();
        assertTrue("set of remotes is not NULL", remotes != null);
        assertTrue("set of remotes has one element", remotes.size() == 1);
    }

    @Test
    public void testCommits() {
        try {
            TrackedFile trackedFile = temporarySVNRepository.getTrackedFile();
            assertTrue("tracked file is not NULL", trackedFile != null);

            List<Commit> commits = trackedFile.getCommits();
            assertTrue("list of commits is not NULL", commits != null);
            assertEquals("list of commits has length 6", 6, commits.size());
            assertEquals("2nd commit has the same author as test repository",
                    temporarySVNRepository.getRepository().getSelf(),
                    commits.get(4).getAuthor());
            assertEquals("commit message of 4th version",
                    "fourth version",
                    commits.get(2).getMessage().trim());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testLimits() {
        try {
            TrackedFile trackedFile = temporarySVNRepository.getTrackedFile();
            assertTrue("tracked file is not NULL", trackedFile != null);

            // TODO: test date limit

            // TODO: test revision limit
            List<Commit> commits = trackedFile.getCommits(null, "4"); // this should actually return commits until r3!
            assertTrue("list of commits is not NULL", commits != null);
            assertEquals("list of commits has length 4", 4, commits.size());

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = VersionControlException.class)
    public void badThingsWithRepo() throws VersionControlException, IOException {
        assertTrue(toBeRemoved.getRoot().exists());
        TrackedFile trackedFile = null;

        try {
            trackedFile = temporarySVNRepository.getTrackedFile();
            assertTrue("tracked file is not NULL", trackedFile != null);

            // checking out file structure
            File[] svnDir = trackedFile.getFile().getParentFile().listFiles(RepositoryFactory.SVN_FILTER);
            assertTrue(".svn exists", svnDir != null);
            assertEquals("only 1 .svn exists", 1, svnDir.length);

            // now doing bad things...
            assertTrue(".svn is directory", svnDir[0].isDirectory());
            Utils.deleteFolder(svnDir[0]);
            assertTrue("second deletion doesn't work", !svnDir[0].delete());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // delete .svn folder
        assert trackedFile != null;
        trackedFile.getCommits();
    }
}
