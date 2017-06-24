/*
 * #%L
 * Alfresco Records Management Module
 * %%
 * Copyright (C) 2005 - 2017 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.rest.rm.community.records;

import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.IMAGE_FILE;
import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.createElectronicRecordModel;
import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.createNonElectronicRecordModel;
import static org.alfresco.rest.rm.community.utils.FilePlanComponentsUtil.getFile;
import static org.alfresco.rest.rm.community.utils.RMSiteUtil.createDOD5015RMSiteModel;
import static org.alfresco.rest.rm.community.utils.RMSiteUtil.createStandardRMSiteModel;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.alfresco.rest.rm.community.base.BaseRMRestTest;
import org.alfresco.rest.rm.community.model.record.Record;
import org.alfresco.rest.rm.community.requests.gscore.api.RMSiteAPI;
import org.alfresco.rest.rm.community.requests.gscore.api.RecordFolderAPI;
import org.alfresco.rest.rm.community.requests.gscore.api.RecordsAPI;
import org.alfresco.test.AlfrescoTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class contains the tests for
 * Complete Record Action REST API
 *
 * @author Sara Aspery
 * @since 2.6
 */
public class CompleteRecordTests extends BaseRMRestTest
{
    private static final Boolean COMPLETE = true;
    private static final Boolean INCOMPLETE = false;
    private static final String parameters = "include=isCompleted";

    /**
     * Incomplete records with mandatory meta-data missing
     */
    @DataProvider (name = "IncompleteRecordsMandatoryMetadataMissing")
    public Object[][] getIncompleteRecordsMandatoryMetadataMissing() throws Exception
    {
        //create RM site
        RMSiteAPI rmSiteAPI = getRestAPIFactory().getRMSiteAPI();
        rmSiteAPI.deleteRMSite();
        rmSiteAPI.createRMSite(createDOD5015RMSiteModel());
        assertStatusCode(CREATED);

        // create record folder
        String recordFolderId = createCategoryFolderInFilePlan().getId();

        Record electronicRecord = createAndVerifyElectronicRecord(recordFolderId);
        Record nonElectronicRecord = createAndVerifyNonElectronicRecord(recordFolderId);

        return new String[][]
            {
                // an arbitrary record folder
                { electronicRecord.getId(), nonElectronicRecord.getId() },
            };
    }

    /**
     * Incomplete records with mandatory meta-data present
     */
    @DataProvider (name = "IncompleteRecordsMandatoryMetadataPresent")
    public Object[][] getIncompleteRecordsMandatoryMetadataPresent() throws Exception
    {
        //create RM site
        RMSiteAPI rmSiteAPI = getRestAPIFactory().getRMSiteAPI();
        rmSiteAPI.deleteRMSite();
        rmSiteAPI.createRMSite(createStandardRMSiteModel());
        assertStatusCode(CREATED);

        // create record folder
        String recordFolderId = createCategoryFolderInFilePlan().getId();

        Record electronicRecord = createAndVerifyElectronicRecord(recordFolderId);
        Record nonElectronicRecord = createAndVerifyNonElectronicRecord(recordFolderId);

        return new String[][]
            {
                // an arbitrary record folder
                { electronicRecord.getId(), nonElectronicRecord.getId() },
            };
    }

    /**
     * Document to be completed is not a record
     */
    @DataProvider (name = "Supplied node is not a record")
    public Object[][] getNodesWhichAreNotRecords() throws Exception
    {
        createRMSiteIfNotExists();
        return new String[][]
            {
                { createCategoryFolderInFilePlan().getId() },

            };
    }

    /**
     * <pre>
     * Given the repository is configured to check mandatory data before completing a record
     * And an incomplete record with missing mandatory meta-data
     * When I complete the record
     * Then I receive an error indicating that I can't complete the operation,
     * because some of the mandatory meta-data of the record is missing
     * </pre>
     */
    @Test
        (
            dataProvider = "IncompleteRecordsMandatoryMetadataMissing",
            description = "Cannot complete electronic and non-electronic records with mandatory metadata missing"
        )
    @AlfrescoTest (jira = "RM-4431")
    public void completeRecordWithMandatoryMetadataMissing(String electronicRecordId, String nonElectronicRecordId)
        throws Exception
    {
        // Get the recordsAPI
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        Record electronicRecord = recordsAPI.getRecord(electronicRecordId);
        Record nonElectronicRecord = recordsAPI.getRecord(nonElectronicRecordId);

        for (Record record : Arrays.asList(electronicRecord, nonElectronicRecord))
        {
            verifyRecordIsIncomplete(record);

            // Complete record
            recordsAPI.completeRecord(record.getId(), parameters);
            assertStatusCode(UNPROCESSABLE_ENTITY);

            verifyRecordIsIncomplete(record);
        }
    }

    /**
     * <pre>
     * Given the repository is configured to check mandatory data before completing a record
     * And an incomplete record with all mandatory meta-data present
     * When I complete the record
     * Then the record is successfully completed
     * </pre>
     */
    @Test
        (
            dataProvider = "IncompleteRecordsMandatoryMetadataPresent",
            description = "Can complete electronic and non-electronic records with mandatory metadata present"
        )
    @AlfrescoTest (jira = "RM-4431")
    public void completeRecordWithMandatoryMetadataPresent(String electronicRecordId, String nonElectronicRecordId)
        throws Exception
    {
        // Get the recordsAPI
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        Record electronicRecord = recordsAPI.getRecord(electronicRecordId);
        Record nonElectronicRecord = recordsAPI.getRecord(nonElectronicRecordId);

        for (Record record : Arrays.asList(electronicRecord, nonElectronicRecord))
        {
            verifyRecordIsIncomplete(record);

            // Complete record
            recordsAPI.completeRecord(record.getId(), parameters);
            assertStatusCode(CREATED);

            verifyRecordIsComplete(record);
        }
    }

    /**
     * <pre>
     * Given a document that is not a record or any non-document node
     * When I complete the item
     * Then I receive an unsupported operation error
     * </pre>
     */
    @Test
        (
            dataProvider = "Supplied node is not a record",
            description = "Cannot complete a document that is not a record"
        )
    @AlfrescoTest (jira = "RM-4431")
    public void completeNonRecord(String nonRecordId) throws Exception
    {
        // Get the recordsAPI
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        recordsAPI.completeRecord(nonRecordId, parameters);
        assertStatusCode(BAD_REQUEST);
    }

    /**
     * <pre>
     * Given a record that is already completed
     * When I complete the record
     * Then I receive an error indicating that I can't complete the operation, because the record is already complete
     * </pre>
     */
    @Test
        (
            dataProvider = "IncompleteRecordsMandatoryMetadataPresent",
            description = "Cannot complete a record that is already completed"
        )
    @AlfrescoTest (jira = "RM-4431")
    public void completeAlreadyCompletedRecord(String electronicRecordId, String nonElectronicRecordId)
        throws Exception
    {
        // Get the recordsAPI
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        Record electronicRecord = recordsAPI.getRecord(electronicRecordId);
        Record nonElectronicRecord = recordsAPI.getRecord(nonElectronicRecordId);

        for (Record record : Arrays.asList(electronicRecord, nonElectronicRecord))
        {
            verifyRecordIsIncomplete(record);

            // Complete record
            recordsAPI.completeRecord(record.getId(), parameters);
            assertStatusCode(CREATED);

            verifyRecordIsComplete(record);

            // Complete record
            recordsAPI.completeRecord(record.getId(), parameters);
            assertStatusCode(UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Helper method to create an electronic record and and assert successful creation
     */
    private Record createAndVerifyElectronicRecord(String recordFolderId) throws Exception
    {
        //create electronic record in record folder
        RecordFolderAPI recordFolderAPI = getRestAPIFactory().getRecordFolderAPI();
        Record electronicRecord = recordFolderAPI.createRecord(createElectronicRecordModel(), recordFolderId,
            getFile(IMAGE_FILE));
        assertStatusCode(CREATED);

        return electronicRecord;
    }

    /**
     * Helper method to create a non-electronic record and and assert successful creation
     */
    private Record createAndVerifyNonElectronicRecord(String recordFolderId) throws Exception
    {
        //create non-electronic record in record folder
        RecordFolderAPI recordFolderAPI = getRestAPIFactory().getRecordFolderAPI();
        Record nonElectronicRecord = recordFolderAPI.createRecord(createNonElectronicRecordModel(), recordFolderId);
        assertStatusCode(CREATED);

        return nonElectronicRecord;
    }

    /**
     * Helper method to verify that a record is not complete
     */
    private void verifyRecordIsIncomplete(Record record)
    {
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        Record recordModel = recordsAPI.getRecord(record.getId(), parameters);
        assertEquals(recordModel.getIsCompleted(), INCOMPLETE);
    }

    /**
     * Helper method to verify that a record is completed
     */
    private void verifyRecordIsComplete(Record record)
    {
        RecordsAPI recordsAPI = getRestAPIFactory().getRecordsAPI();
        Record recordModel = recordsAPI.getRecord(record.getId(), parameters);
        assertEquals(recordModel.getIsCompleted(), COMPLETE);
    }

}
