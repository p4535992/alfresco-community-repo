/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.domain.permissions;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.ApplicationContextHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Tests on ACL Concurrency
 *
 * @author Andreea Dragoi
 * @author sglover
 * @since 4.2.7
 *
 */

public class AclConcurrencyTest
{

    private ApplicationContext ctx;
    private RetryingTransactionHelper txnHelper;
    private PermissionService permissionService;
    private FileFolderService fileFolderService;
    private NodeLocatorService nodeLocatorService;
    private AuthorityService authorityService;
    private Repository repository;

    private NodeRef rootFolder;

    private static String TEST_GROUP_NAME = "BaseGroup";
    private static String TEST_GROUP_NAME_FULL = PermissionService.GROUP_PREFIX + TEST_GROUP_NAME;
    private static String TEST_GROUP_NAME_2 = "SpecificGroup";
    private static String TEST_GROUP_NAME_2_FULL = PermissionService.GROUP_PREFIX + TEST_GROUP_NAME;
    private static int NUM_DOC_PER_FOLDER = 20;

    @Before
    public void setup()
    {
        ctx = ApplicationContextHelper.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        txnHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
        fileFolderService = serviceRegistry.getFileFolderService();
        permissionService = (PermissionService) ctx.getBean("permissionService");
        nodeLocatorService = serviceRegistry.getNodeLocatorService();
        authorityService = (AuthorityService) ctx.getBean("authorityService");
        repository = (Repository) ctx.getBean("repositoryHelper");

        createAuthorities(TEST_GROUP_NAME_FULL, TEST_GROUP_NAME);
        createAuthorities(TEST_GROUP_NAME_2_FULL, TEST_GROUP_NAME_2);

        rootFolder = createStructure();
    }

    @After
    public void tearDown() throws Exception
    {
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    @Test
    public void testConcurrent() throws InterruptedException
    {
        NodeRef subFolder = txnHelper.doInTransaction(() -> getOrCreateFolder(rootFolder, "sub-folder-" + new Date().getTime()),
                false, true);

        // Create thread in which we will update the permissions of the subfolder
        Runnable setPermissionAction = () -> {
            createLocalPermissions(subFolder, TEST_GROUP_NAME_2_FULL, PermissionService.COORDINATOR);
        };
        Thread setPermissionsThread = new Thread(setPermissionAction);

        // Create thread in which we will create new nodes in the subfolder
        Runnable createNodesRunnable = () -> {
            createDocuments(subFolder);
        };
        Thread createNodesThread = new Thread(createNodesRunnable);
        createNodesThread.start();
        setPermissionsThread.start();
        setPermissionsThread.join();
        createNodesThread.join();

        // Verify documents have the expected inherited permissions from subFolder
        verifyChildPermissions(subFolder, TEST_GROUP_NAME_2_FULL, PermissionService.COORDINATOR);
    }

    @Test
    public void testConcurrentMultipleChanges() throws InterruptedException
    {
        NodeRef subFolder1 = txnHelper.doInTransaction(() -> getOrCreateFolder(rootFolder, "sub-folder-" + new Date().getTime()),
                false, true);
        NodeRef subFolder2 = txnHelper.doInTransaction(() -> getOrCreateFolder(subFolder1, "sub-folder-" + new Date().getTime()),
                false, true);

        // Create thread in which we will update the permissions of the subfolder on 1st level
        Runnable setPermissionAction1 = () -> {
            createLocalPermissions(subFolder1, TEST_GROUP_NAME_2_FULL, PermissionService.COORDINATOR);
        };
        Thread setPermissionsThread1 = new Thread(setPermissionAction1);

        // Create thread in which we will update the permissions of the subfolder on 2nd level
        Runnable setPermissionAction2 = () -> {
            createLocalPermissions(subFolder2, TEST_GROUP_NAME_FULL, PermissionService.CONTRIBUTOR);
        };
        Thread setPermissionsThread2 = new Thread(setPermissionAction2);

        // Create thread in which we will create new nodes in the subfolder in L2
        Runnable createNodesRunnable = () -> {
            createDocuments(subFolder2);
        };
        Thread createNodesThread = new Thread(createNodesRunnable);
        createNodesThread.start();
        setPermissionsThread1.start();
        setPermissionsThread2.start();
        setPermissionsThread1.join();
        setPermissionsThread2.join();
        createNodesThread.join();

        // Verify documents have the expected inherited permissions from subFolder on L2
        verifyChildPermissions(subFolder2, TEST_GROUP_NAME_FULL, PermissionService.CONTRIBUTOR);
    }

    @Test
    public void testConcurrentWithSubfolders() throws InterruptedException
    {
        // Create a nested folder structure with 5 levels
        NodeRef subFolder1 = txnHelper.doInTransaction(() -> getOrCreateFolder(rootFolder, "sub-folder-" + new Date().getTime()),
                false, true);
        NodeRef subFolder2 = txnHelper.doInTransaction(() -> getOrCreateFolder(subFolder1, "sub-folder-" + new Date().getTime()),
                false, true);
        NodeRef subFolder3 = txnHelper.doInTransaction(() -> getOrCreateFolder(subFolder2, "sub-folder-" + new Date().getTime()),
                false, true);
        NodeRef subFolder4 = txnHelper.doInTransaction(() -> getOrCreateFolder(subFolder3, "sub-folder-" + new Date().getTime()),
                false, true);
        NodeRef subFolder5 = txnHelper.doInTransaction(() -> getOrCreateFolder(subFolder4, "sub-folder-" + new Date().getTime()),
                false, true);

        // Create thread in which we will update the permissions of the 1st level sub-folder
        Runnable setPermissionAction = () -> {
            createLocalPermissions(subFolder1, TEST_GROUP_NAME_2_FULL, PermissionService.COORDINATOR);
        };
        Thread setPermissionsThread = new Thread(setPermissionAction);

        // Create thread in which we will create new nodes in the 5th level folder
        Runnable createNodesRunnable = () -> {
            createDocuments(subFolder5);
        };
        Thread createNodesThread = new Thread(createNodesRunnable);
        createNodesThread.start();
        setPermissionsThread.start();
        setPermissionsThread.join();
        createNodesThread.join();

        // Verify documents have the expected inherited permissions from subFolder on L1
        verifyChildPermissions(subFolder5, TEST_GROUP_NAME_2_FULL, PermissionService.COORDINATOR);
    }

    private void verifyChildPermissions(NodeRef parentRef, String expectedAuth, String expectedPermission)
    {
        List<FileInfo> children = fileFolderService.listFiles(parentRef);

        for (FileInfo child : children)
        {
            Set<AccessPermission> permissions = permissionService.getAllSetPermissions(child.getNodeRef());
            boolean correctPermission = false;
            for (AccessPermission permission : permissions)
            {
                if (permission.getAuthority().equals(expectedAuth) && permission.getPermission().equals(expectedPermission))
                {
                    correctPermission = true;
                }
            }
            assertTrue(correctPermission);
        }
    }

    /*
     * Create base structure with 2 levels and set the last folder as the root for tests 
     * - level1Folder: does not inherit permissions; no local set permissions 
     * - level2Folder: does not inherit permissions; TEST_GROUP_NAME has Contributor Permissions
     */
    private NodeRef createStructure()
    {
        return txnHelper.doInTransaction(() -> {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
            NodeRef companyHome = repository.getCompanyHome();
            NodeRef level1Folder = getOrCreateFolder(companyHome, "Level1");
            NodeRef level2Folder = getOrCreateFolder(level1Folder, "Level2");
            permissionService.setInheritParentPermissions(level1Folder, false);
            permissionService.setInheritParentPermissions(level2Folder, false);
            permissionService.setPermission(level2Folder, TEST_GROUP_NAME_FULL, PermissionService.CONTRIBUTOR, true);

            return level2Folder;
        }, false, true);
    }

    private void createDocuments(NodeRef parentRef)
    {
        txnHelper.doInTransaction(() -> {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
            for (int i = 1; i <= NUM_DOC_PER_FOLDER; i++)
            {
                String name = "File-" + i + "-thread-" + Thread.currentThread().getName();
                fileFolderService.create(parentRef, name, ContentModel.PROP_CONTENT);
            }
            return null;
        }, false, true);
    }

    private void createLocalPermissions(NodeRef nodeRef, String authority, String permission)
    {
        txnHelper.doInTransaction(() -> {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
            permissionService.setInheritParentPermissions(nodeRef, false);
            permissionService.setPermission(nodeRef, authority, permission, true);
            return null;
        }, false, true);
    }

    private NodeRef getOrCreateFolder(NodeRef parent, String folderName)
    {
        NodeRef folderRef = fileFolderService.searchSimple(parent, folderName);

        if (folderRef == null)
        {
            folderRef = fileFolderService.create(parent, folderName, ContentModel.TYPE_FOLDER).getNodeRef();
        }

        return folderRef;
    }

    private void createAuthorities(String fullName, String name)
    {
        txnHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
            Set<String> zones = new HashSet<String>(2, 1.0f);
            zones.add(AuthorityService.ZONE_APP_DEFAULT);
            if (!authorityService.authorityExists(fullName))
            {
                authorityService.createAuthority(AuthorityType.GROUP, name, name, zones);
            }
            return null;
        }, false, true);
    }

}
