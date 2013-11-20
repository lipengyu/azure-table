/**
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS
 * OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under
 * the License.
 */
package com.yammer.collections.azure.backup.tool;


import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("InstanceVariableMayNotBeInitialized")
@RunWith(MockitoJUnitRunner.class)
public class BackupCLICommandUtilTest {
    private static final String CONFIG_FILE_PATH = BackupCLICommandUtilTest.class.getResource("testBackupAccountConfiguration.yml").getPath();
    private static final String[] DO_BACKUP_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-b"};
    private static final String[] DELETE_BAD_BACKUPS_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-db"};
    private static final String[] DELETE_BACKUPS_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-d", "0"};
    private static final String[] LIST_BACKUPS_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-l", "0"};
    private static final String[] LIST_ALL_BACKUPS_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-la"};
    private static final String[] RESTORE_COMMAND_LINE = {"-cf", CONFIG_FILE_PATH, "-r", "" + Long.MAX_VALUE};
    private BackupCLICommandUtil backupCLICommandUtil;
    private ArgumentParser argumentParser;
    @Mock
    private BackupServiceFactory backupServiceFactoryMock;

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @Before
    public void setUp() {
        backupCLICommandUtil = new BackupCLICommandUtil(backupServiceFactoryMock, System.out, System.err);
        argumentParser = ArgumentParsers.newArgumentParser("test parser");
        backupCLICommandUtil.configureParser(argumentParser);
    }

    private BackupCommand parse(String args[]) throws ArgumentParserException {
        return backupCLICommandUtil.constructBackupCommand(argumentParser.parseArgs(args));
    }

    @Test
    public void backupCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(DO_BACKUP_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(DoBackupCommand.class)));
    }

    @Test
    public void deleteBadBackupsCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(DELETE_BAD_BACKUPS_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(DeleteBadBackupsCommand.class)));
    }

    @Test
    public void deleteBackupCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(DELETE_BACKUPS_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(DeleteBackupsCommand.class)));
    }

    @Test
    public void listBackupCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(LIST_BACKUPS_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(ListBackupsCommand.class)));
    }

    @Test
    public void listAllBackupCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(LIST_ALL_BACKUPS_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(ListBackupsCommand.class)));
    }

    @Test
    public void restoreBackupCommandLineOptionsParsedCorrectly() throws ArgumentParserException {
        BackupCommand createdCommand = parse(RESTORE_COMMAND_LINE);

        assertThat(createdCommand, is(instanceOf(RestoreFromBackupCommand.class)));
    }

}
