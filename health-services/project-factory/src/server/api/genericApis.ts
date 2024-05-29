// Import necessary modules and libraries
import * as XLSX from "xlsx"; // Import XLSX library for Excel file processing
import config from "../config"; // Import configuration settings
import FormData from "form-data"; // Import FormData for handling multipart/form-data requests
import { httpRequest } from "../utils/request"; // Import httpRequest function for making HTTP requests
import { getFormattedStringForDebug, logger } from "../utils/logger"; // Import logger for logging
import { correctParentValues, findMapValue, generateActivityMessage, getBoundaryRelationshipData, getDataSheetReady, getLocalizedHeaders, sortCampaignDetails, throwError } from "../utils/genericUtils"; // Import utility functions
import { validateProjectFacilityResponse, validateProjectResourceResponse, validateStaffResponse } from "../validators/genericValidator"; // Import validation functions
import { extractCodesFromBoundaryRelationshipResponse, generateFilteredBoundaryData, getLocalizedName } from '../utils/campaignUtils'; // Import utility functions
import { getFiltersFromCampaignSearchResponse, getHierarchy } from './campaignApis';
import { validateMappingId } from '../utils/campaignMappingUtils';
import { campaignStatuses } from '../config/constants';
import { getBoundaryTabName } from '../utils/boundaryUtils';
const _ = require('lodash'); // Import lodash library

// Function to retrieve workbook from Excel file URL and sheet name
const getWorkbook = async (fileUrl: string, sheetName: string) => {
  // Define headers for HTTP request
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/pdf",
  };

  // Make HTTP request to retrieve Excel file as arraybuffer
  const responseFile = await httpRequest(
    fileUrl,
    null,
    {},
    "get",
    "arraybuffer",
    headers
  );

  // Read Excel file into workbook
  const workbook = XLSX.read(responseFile, { type: "buffer" });
  // Check if the specified sheet exists in the workbook
  if (!workbook.Sheets.hasOwnProperty(sheetName)) {
    throwError(
      "FILE",
      400,
      "INVALID_SHEETNAME",
      `Sheet with name "${sheetName}" is not present in the file.`
    );
  }

  // Return the workbook
  return workbook;
};

//Function to get Workbook with different tabs (for type target)
const getTargetWorkbook = async (fileUrl: string, localizationMap?: any) => {
  // Define headers for HTTP request
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/pdf",
  };

  // Make HTTP request to retrieve Excel file as arraybuffer
  const responseFile = await httpRequest(
    fileUrl,
    null,
    {},
    "get",
    "arraybuffer",
    headers
  );

  // Read Excel file into workbook
  const workbook = XLSX.read(responseFile, { type: "buffer" });
  const mainSheet = workbook.SheetNames[1];
  const localizedMainSheet = getLocalizedName(mainSheet, localizationMap);
  if (!workbook.Sheets.hasOwnProperty(mainSheet)) {
    throwError(
      "FILE",
      400,
      "INVALID_SHEETNAME",
      `Sheet with name "${localizedMainSheet}" is not present in the file.`
    );
  }

  // Return the workbook
  return workbook;
};

function isNumeric(value: any) {
  return /^-?\d+(\.\d+)?$/.test(value);
}

// Function to retrieve data from a specific sheet in an Excel file
const getSheetData = async (
  fileUrl: string,
  sheetName: string,
  getRow = false,
  createAndSearchConfig?: any,
  localizationMap?: { [key: string]: string }
) => {
  // Retrieve workbook using the getWorkbook function
  const localizedSheetName = getLocalizedName(sheetName, localizationMap);
  const workbook: any = await getWorkbook(fileUrl, localizedSheetName);

  // If parsing array configuration is provided, validate first row of each column
  if (
    createAndSearchConfig &&
    createAndSearchConfig.parseArrayConfig &&
    createAndSearchConfig.parseArrayConfig.parseLogic
  ) {
    const parseLogic = createAndSearchConfig.parseArrayConfig.parseLogic;
    // Iterate over each column configuration
    for (const columnConfig of parseLogic) {
      const { sheetColumn } = columnConfig;
      const expectedColumnName = columnConfig.sheetColumnName;
      var localizedColumnName;
      localizedColumnName = getLocalizedName(
        expectedColumnName,
        localizationMap
      );
      // Get the value of the first row in the current column
      if (sheetColumn && localizedColumnName) {
        const firstRowValue =
          workbook.Sheets[localizedSheetName][`${sheetColumn}1`]?.v;
        // Validate the first row of the current column
        if (firstRowValue !== localizedColumnName) {
          throwError(
            "FILE",
            400,
            "INVALID_COLUMNS",
            `Invalid format: Expected '${localizedColumnName}' in the first row of column ${sheetColumn}.`
          );
        }
      }
    }
  }

  // Convert sheet data to JSON format
  const sheetData = XLSX.utils.sheet_to_json(
    workbook.Sheets[localizedSheetName],
    { blankrows: true, raw: false }
  );
  var jsonData = sheetData.map((row: any, index: number) => {
    const rowData: any = {};
    if (Object.keys(row).length > 0) {
      Object.keys(row).forEach((key) => {
        // Check if the value is a numerical string
        rowData[key] = isNumeric(row[key])
          ? Number(row[key])
          : row[key] === undefined || row[key] === ""
            ? ""
            : row[key];
      });
      if (getRow) rowData["!row#number!"] = index + 1;
      return rowData;
    }
  });

  jsonData = jsonData.filter((element) => element !== undefined);
  return jsonData;
};

const getTargetSheetData = async (
  fileUrl: string,
  getRow = false,
  getSheetName = false,
  localizationMap?: any
) => {
  const workbook: any = await getTargetWorkbook(fileUrl, localizationMap);
  const sheetNames = workbook.SheetNames;
  const localizedSheetNames = getLocalizedHeaders(sheetNames, localizationMap);

  const workbookData: { [key: string]: any[] } = {}; // Object to store data from each sheet

  for (const sheetName of localizedSheetNames) {
    const sheetData = XLSX.utils.sheet_to_json(workbook.Sheets[sheetName], {
      blankrows: true, raw: false
    });
    var jsonData = sheetData.map((row: any, index: number) => {
      const rowData: any = {};
      if (Object.keys(row).length > 0) {
        Object.keys(row).forEach((key) => {
          // Check if the value is a numerical string
          rowData[key] = isNumeric(row[key])
            ? Number(row[key])
            : row[key] === undefined || row[key] === ""
              ? ""
              : row[key];
        });
        if (getRow) rowData["!row#number!"] = index + 1;
        if (getSheetName) rowData["!sheet#name!"] = sheetName;
        return rowData;
      }
    });
    jsonData = jsonData.filter((element) => element !== undefined);
    workbookData[sheetName] = jsonData; // Store sheet data in the object
    // logger.info(`Sheet Data (${sheetName}): ${JSON.stringify(jsonData)}`);
  }
  // Return data from all sheets
  return workbookData;
};

// Function to search MDMS for specific unique identifiers
const searchMDMS: any = async (
  uniqueIdentifiers: any[],
  schemaCode: string,
  requestinfo: any,
  response: any
) => {
  // Check if unique identifiers are provided
  if (!uniqueIdentifiers) {
    return;
  }

  // Construct API URL for MDMS search
  const apiUrl = config.host.mdms + config.paths.mdms_search;

  // Construct request data for MDMS search
  const data = {
    MdmsCriteria: {
      tenantId: requestinfo?.userInfo?.tenantId,
      uniqueIdentifiers: uniqueIdentifiers,
      schemaCode: schemaCode,
    },
    RequestInfo: requestinfo,
  };

  // Make HTTP request to MDMS API
  const result = await httpRequest(
    apiUrl,
    data,
    undefined,
    undefined,
    undefined
  );

  // Log search result
  logger.info("Template search Result : " + JSON.stringify(result));

  // Return search result
  return result;
};

// Function to generate a campaign number
const getCampaignNumber: any = async (
  requestBody: any,
  idFormat: String,
  idName: string,
  tenantId: string
) => {
  // Construct request data
  const data = {
    RequestInfo: requestBody?.RequestInfo,
    idRequests: [
      {
        idName: idName,
        tenantId: tenantId,
        format: idFormat,
      },
    ],
  };

  // Construct URL for ID generation service
  const idGenUrl = config.host.idGenHost + config.paths.idGen;

  // Make HTTP request to ID generation service
  const result = await httpRequest(
    idGenUrl,
    data,
    undefined,
    undefined,
    undefined,
    undefined
  );

  // Return generated campaign number
  if (result?.idResponses?.[0]?.id) {
    return result?.idResponses?.[0]?.id;
  }

  // Throw error if ID generation fails
  throwError("COMMON", 500, "IDGEN_ERROR");
};

// Function to generate a resource number
const getResouceNumber: any = async (
  RequestInfo: any,
  idFormat: String,
  idName: string
) => {
  // Construct request data
  const data = {
    RequestInfo,
    idRequests: [
      {
        idName: idName,
        tenantId: RequestInfo?.userInfo?.tenantId,
        format: idFormat,
      },
    ],
  };

  // Construct URL for ID generation service
  const idGenUrl = config.host.idGenHost + config.paths.idGen;

  try {
    // Make HTTP request to ID generation service
    const result = await httpRequest(
      idGenUrl,
      data,
      undefined,
      undefined,
      undefined,
      undefined
    );

    // Return generated resource number
    if (result?.idResponses?.[0]?.id) {
      return result?.idResponses?.[0]?.id;
    }

    // Return null if ID generation fails
    return result;
  } catch (error: any) {
    // Log error if ID generation fails
    logger.error("Error: " + error);

    // Return error
    return error;
  }
};

// Function to get schema definition based on code and request info
const getSchema: any = async (code: string, RequestInfo: any) => {
  const data = {
    RequestInfo,
    SchemaDefCriteria: {
      tenantId: RequestInfo?.userInfo?.tenantId,
      limit: 200,
      codes: [code],
    },
  };
  const mdmsSearchUrl = config.host.mdms + config.paths.mdmsSchema;

  try {
    const result = await httpRequest(
      mdmsSearchUrl,
      data,
      undefined,
      undefined,
      undefined,
      undefined
    );
    return result?.SchemaDefinitions?.[0]?.definition;
  } catch (error: any) {
    logger.error("Error: " + error);
    return error;
  }
};

// Function to get count from response data
const getCount: any = async (
  responseData: any,
  request: any,
  response: any
) => {
  try {
    // Extract host and URL from response data
    const host = responseData?.host;
    const url = responseData?.searchConfig?.countUrl;

    // Extract request information
    const requestInfo = { RequestInfo: request?.body?.RequestInfo };

    // Make HTTP request to get count
    const result = await httpRequest(
      host + url,
      requestInfo,
      undefined,
      undefined,
      undefined,
      undefined
    );

    // Extract count from result using lodash
    const count = _.get(result, responseData?.searchConfig?.countPath);

    return count; // Return the count
  } catch (error: any) {
    // Log and throw error if any
    logger.error("Error: " + error);
    throw error;
  }
};

// Function to create Excel sheet and upload it
async function createAndUploadFile(
  updatedWorkbook: XLSX.WorkBook,
  request: any,
  tenantId?: any
) {
  // Write the updated workbook to a buffer
  const buffer = XLSX.write(updatedWorkbook, {
    bookType: "xlsx",
    type: "buffer",
  });

  // Create form data for file upload
  const formData = new FormData();
  formData.append("file", buffer, "filename.xlsx");
  formData.append(
    "tenantId",
    tenantId ? tenantId : request?.body?.RequestInfo?.userInfo?.tenantId
  );
  formData.append("module", "HCM-ADMIN-CONSOLE-SERVER");

  // Make HTTP request to upload file
  var fileCreationResult = await httpRequest(
    config.host.filestore + config.paths.filestore,
    formData,
    undefined,
    undefined,
    undefined,
    {
      "Content-Type": "multipart/form-data",
      "auth-token": request?.body?.RequestInfo?.authToken,
    }
  );

  // Extract response data
  const responseData = fileCreationResult?.files;
  if (!responseData) {
    throwError(
      "COMMON",
      500,
      "INTERNAL_SERVER_ERROR",
      "Error while uploading excel file"
    );
  }

  return responseData; // Return the response data
}

// Function to generate a list of hierarchy codes
function generateHierarchyList(data: any[], parentChain: any = []) {
  let result: any[] = [];

  // Iterate over each boundary in the current level
  for (let boundary of data) {
    let currentChain = [...parentChain, boundary.code];

    // Add the current chain to the result
    result.push(currentChain.join(","));

    // If there are children, recursively call the function
    if (boundary.children.length > 0) {
      let childResults = generateHierarchyList(boundary.children, currentChain);
      result = result.concat(childResults);
    }
  }
  return result; // Return the hierarchy list
}

// Function to generate hierarchy from boundaries
function generateHierarchy(boundaries: any[]) {
  // Create an object to store boundary types and their parents
  const parentMap: any = {};

  // Populate the object with boundary types and their parents
  for (const boundary of boundaries) {
    parentMap[boundary.boundaryType] = boundary.parentBoundaryType;
  }

  // Traverse the hierarchy to generate the hierarchy list
  const hierarchyList = [];
  for (const boundaryType in parentMap) {
    if (Object.prototype.hasOwnProperty.call(parentMap, boundaryType)) {
      const parentBoundaryType = parentMap[boundaryType];
      if (parentBoundaryType === null) {
        // This boundary type has no parent, add it to the hierarchy list
        hierarchyList.push(boundaryType);
        // Traverse its children recursively
        traverseChildren(boundaryType, parentMap, hierarchyList);
      }
    }
  }
  return hierarchyList; // Return the hierarchy list
}

// Recursive function to traverse children and generate hierarchy
function traverseChildren(parent: any, parentMap: any, hierarchyList: any[]) {
  for (const boundaryType in parentMap) {
    if (Object.prototype.hasOwnProperty.call(parentMap, boundaryType)) {
      const parentBoundaryType = parentMap[boundaryType];
      if (parentBoundaryType === parent) {
        // This boundary type has the current parent, add it to the hierarchy list
        hierarchyList.push(boundaryType);
        // Traverse its children recursively
        traverseChildren(boundaryType, parentMap, hierarchyList);
      }
    }
  }
}

// Function to create an Excel sheet
async function createExcelSheet(
  data: any,
  headers: any,
  sheetName: string = "Sheet1"
) {
  // Create a new Excel workbook
  const workbook = XLSX.utils.book_new();

  // Combine headers and data into sheet data
  const sheetData = [headers, ...data];
  const ws = XLSX.utils.aoa_to_sheet(sheetData);

  // Define column widths (in pixels)
  const columnWidths = headers.map(() => ({ width: 30 }));

  // Apply column widths to the sheet
  ws["!cols"] = columnWidths;

  // Append sheet to the workbook
  XLSX.utils.book_append_sheet(workbook, ws, sheetName);

  return { wb: workbook, ws: ws, sheetName: sheetName }; // Return the workbook, worksheet, and sheet name
}

// Function to handle getting boundary codes
async function getAutoGeneratedBoundaryCodesHandler(boundaryList: any, childParentMap: Map<{ key: string; value: string; }, { key: string; value: string; } | null>, elementCodesMap: any, countMap: any, request: any) {
  try {
    // Get updated element codes map
    logger.info("Auto Generation of Boundary code begins for the user uploaded sheet")
    const updatedelementCodesMap = await getAutoGeneratedBoundaryCodes(boundaryList, childParentMap, elementCodesMap, countMap, request);
    return updatedelementCodesMap; // Return the updated element codes map
  } catch (error) {
    // Log and propagate the error
    console.error("Error in getBoundaryCodesHandler:", error);
    throw error;
  }
}

/**
 * Function to generate auto-generated boundary codes based on boundary list, child-parent mapping,
 * element codes map, count map, and request information.
 * @param boundaryList List of boundary data
 * @param childParentMap Map of child-parent relationships
 * @param elementCodesMap Map of element codes
 * @param countMap Map of counts for each element
 * @param request HTTP request object
 * @returns Updated element codes map
 */
async function getAutoGeneratedBoundaryCodes(boundaryList: any, childParentMap: any, elementCodesMap: any, countMap: any, request: any) {
  // Initialize an array to store column data
  const columnsData: { key: string, value: string }[][] = [];
  // Extract unique elements from each column
  for (const row of boundaryList) {
    row.forEach((element: any, index: any) => {
      if (!columnsData[index]) {
        columnsData[index] = [];
      }
      const existingElement = columnsData[index].find((existing: any) => _.isEqual(existing, element));
      if (!existingElement) {
        columnsData[index].push(element);
      }
    });
  }

  // Iterate over columns to generate boundary codes
  for (let i = 0; i < columnsData.length; i++) {
    const column = columnsData[i];
    for (const element of column) {
      if (!findMapValue(elementCodesMap, element)) {
        const parentCode = findMapValue(childParentMap, element)
        if (parentCode !== undefined && parentCode !== null) {
          countMap.set(parentCode, (findMapValue(countMap, parentCode) || 0) + 1);
          let code;
          const grandParentCode = findMapValue(childParentMap, parentCode);
          if (grandParentCode != null && grandParentCode != undefined) {
            const parentBoundaryCode = findMapValue(elementCodesMap, parentCode)
            const lastUnderscoreIndex = parentBoundaryCode.lastIndexOf('_');
            const parentBoundaryCodeTrimmed = lastUnderscoreIndex !== -1 ? parentBoundaryCode.substring(0, lastUnderscoreIndex) : parentBoundaryCode;
            code = generateElementCode(countMap.get(parentCode), parentBoundaryCodeTrimmed, element.value);
          } else {
            code = generateElementCode(countMap.get(parentCode), findMapValue(elementCodesMap, parentCode), element.value);
          }
          elementCodesMap.set(element, code); // Store the code of the element in the map
        } else {
          // Generate default code if parent code is not found
          elementCodesMap.set(element, (request?.body?.ResourceDetails?.hierarchyType + "_").toUpperCase() + element.value.toString().substring(0, 2).toUpperCase());
        }
      } else {
        continue;
      }
    }
  }
  return elementCodesMap; // Return the updated element codes map
}

/**
 * Function to generate an element code based on sequence, parent code, and element.
 * @param sequence Sequence number
 * @param parentCode Parent code
 * @param element Element
 * @returns Generated element code
 */
function generateElementCode(sequence: any, parentCode: any, element: any) {
  // Pad single-digit numbers with leading zero
  let paddedSequence = sequence.toString().padStart(2, "0");
  const code = parentCode.toUpperCase() +
    "_" +
    paddedSequence +
    "_" +
    element.toUpperCase();
  return (
    code.trim()
  );
}

/**
 * Asynchronously retrieves boundary sheet data based on the provided request.
 * @param request The HTTP request object.
 * @returns Boundary sheet data.
 */
async function getBoundarySheetData(
  request: any,
  localizationMap?: { [key: string]: string }
) {
  // Retrieve boundary data based on the request parameters
  const params = {
    ...request?.query,
    includeChildren: true,
  };
  const hierarchyType = request?.query?.hierarchyType;
  logger.info(
    `processing boundary data generation for hierarchyType : ${hierarchyType}`
  );
  const boundaryData = await getBoundaryRelationshipData(request, params);
  if (!boundaryData || boundaryData.length === 0) {
    logger.info(`boundary data not found for hierarchyType : ${hierarchyType}`);
    const hierarchy = await getHierarchy(
      request,
      request?.query?.tenantId,
      hierarchyType
    );
    const modifiedHierarchy = hierarchy.map((ele) =>
      `${hierarchyType}_${ele}`.toUpperCase()
    );
    const localizedHeaders = getLocalizedHeaders(
      modifiedHierarchy,
      localizationMap
    );
    // create empty sheet if no boundary present in system
    const localizedBoundaryTab = getLocalizedName(
      getBoundaryTabName(),
      localizationMap
    );
    logger.info(`generated a empty template for boundary`);
    return await createExcelSheet(
      boundaryData,
      localizedHeaders,
      localizedBoundaryTab
    );
  } else {
    // logger.info("boundaryData for sheet " + JSON.stringify(boundaryData))
    const responseFromCampaignSearch =
      await getFiltersFromCampaignSearchResponse(request);
    if (responseFromCampaignSearch?.Filters != null) {
      const filteredBoundaryData = await generateFilteredBoundaryData(
        request,
        responseFromCampaignSearch
      );
      return await getDataSheetReady(
        filteredBoundaryData,
        request,
        localizationMap
      );
    } else {
      return await getDataSheetReady(boundaryData, request, localizationMap);
    }
  }
}
async function createStaff(resouceBody: any) {
  // Create staff
  const staffCreateUrl =
    `${config.host.projectHost}` + `${config.paths.staffCreate}`;
  logger.info("Project Staff Creation url " + staffCreateUrl);
  logger.debug(
    "Project Staff Creation body " + getFormattedStringForDebug(resouceBody)
  );
  const staffResponse = await httpRequest(
    staffCreateUrl,
    resouceBody,
    undefined,
    "post",
    undefined,
    undefined
  );
  logger.info("Project Staff mapping created");
  logger.debug(
    "Project Staff mapping response " +
    getFormattedStringForDebug(staffResponse)
  );
  validateStaffResponse(staffResponse);
}

/**
 * Asynchronously creates project resources based on the provided resource body.
 * @param resouceBody The resource body.
 */
async function createProjectResource(resouceBody: any) {
  // Create project resources
  const projectResourceCreateUrl =
    `${config.host.projectHost}` + `${config.paths.projectResourceCreate}`;
  logger.info("Project Resource Creation url " + projectResourceCreateUrl);
  logger.debug(
    "Project Resource Creation body " + getFormattedStringForDebug(resouceBody)
  );
  const projectResourceResponse = await httpRequest(
    projectResourceCreateUrl,
    resouceBody,
    undefined,
    "post",
    undefined,
    undefined
  );
  logger.debug("Project Resource Created");
  logger.debug(
    "Project Resource Creation response :: " +
    getFormattedStringForDebug(projectResourceResponse)
  );
  validateProjectResourceResponse(projectResourceResponse);
}

/**
 * Asynchronously creates project facilities based on the provided resource body.
 * @param resouceBody The resource body.
 */
async function createProjectFacility(resouceBody: any) {
  // Create project facilities
  const projectFacilityCreateUrl =
    `${config.host.projectHost}` + `${config.paths.projectFacilityCreate}`;
  logger.info("Project Facility Creation url " + projectFacilityCreateUrl);
  logger.debug(
    "Project Facility Creation body " + getFormattedStringForDebug(resouceBody)
  );
  const projectFacilityResponse = await httpRequest(
    projectFacilityCreateUrl,
    resouceBody,
    undefined,
    "post",
    undefined,
    undefined
  );
  logger.info("Project Facility Created");
  logger.debug(
    "Project Facility Creation response" +
    getFormattedStringForDebug(projectFacilityResponse)
  );
  validateProjectFacilityResponse(projectFacilityResponse);
}

/**
 * Asynchronously creates related entities such as staff, resources, and facilities based on the provided resources, tenant ID, project ID, start date, end date, and resource body.
 * @param resources List of resources.
 * @param tenantId The tenant ID.
 * @param projectId The project ID.
 * @param startDate The start date.
 * @param endDate The end date.
 * @param resouceBody The resource body.
 */
async function createRelatedEntity(
  resources: any,
  tenantId: any,
  projectId: any,
  startDate: any,
  endDate: any,
  resouceBody: any
) {
  // Create related entities
  for (const resource of resources) {
    const type = resource?.type;
    for (const resourceId of resource?.resourceIds) {
      if (type == "staff") {
        const ProjectStaff = {
          tenantId: tenantId.split(".")?.[0],
          projectId,
          userId: resourceId,
          startDate,
          endDate,
        };
        resouceBody.ProjectStaff = ProjectStaff;
        await createStaff(resouceBody);
      } else if (type == "resource") {
        const ProjectResource = {
          // FIXME : Tenant Id should not be splitted
          tenantId: tenantId.split(".")?.[0],
          projectId,
          resource: {
            productVariantId: resourceId,
            type: "DRUG",
            isBaseUnitVariant: false,
          },
          startDate,
          endDate,
        };
        resouceBody.ProjectResource = ProjectResource;
        await createProjectResource(resouceBody);
      } else if (type == "facility") {
        const ProjectFacility = {
          // FIXME : Tenant Id should not be splitted
          tenantId: tenantId.split(".")?.[0],
          projectId,
          facilityId: resourceId,
        };
        resouceBody.ProjectFacility = ProjectFacility;
        await createProjectFacility(resouceBody);
      }
    }
  }
}

/**
 * Asynchronously creates related resources based on the provided request body.
 * @param requestBody The request body.
 */
async function createRelatedResouce(requestBody: any) {
  const id = requestBody?.Campaign?.id;
  const campaignDetails = await validateMappingId(requestBody, id);
  if (campaignDetails?.status == campaignStatuses.inprogress) {
    logger.info("Campaign Already In Progress and Mapped");
  } else {
    sortCampaignDetails(requestBody?.Campaign?.CampaignDetails);
    correctParentValues(requestBody?.Campaign?.CampaignDetails);
    // Create related resources
    const { tenantId } = requestBody?.Campaign;

    for (const campaignDetails of requestBody?.Campaign?.CampaignDetails) {
      const resouceBody: any = {
        RequestInfo: requestBody.RequestInfo,
      };
      var { projectId, startDate, endDate, resources } = campaignDetails;
      startDate = parseInt(startDate);
      endDate = parseInt(endDate);
      await createRelatedEntity(
        resources,
        tenantId,
        projectId,
        startDate,
        endDate,
        resouceBody
      );
    }
  }
}

/**
 * Asynchronously creates boundary entities based on the provided request and boundary map.
 * @param request The HTTP request object.
 * @param boundaryMap Map of boundary names to codes.
 */
async function createBoundaryEntities(request: any, boundaryMap: Map<any, any>) {
  try {
    const updatedBoundaryMap: Array<{ key: string, value: string }> = Array.from(boundaryMap).map(([key, value]) => ({ key: key.value, value: value }));
    // Create boundary entities
    const requestBody = { "RequestInfo": request.body.RequestInfo } as { RequestInfo: any; Boundary?: any };
    const boundaries: any[] = [];
    const codesFromResponse: any = [];
    const boundaryCodes: any[] = [];
    Array.from(boundaryMap.entries()).forEach(([, boundaryCode]) => {
      boundaryCodes.push(boundaryCode);
    });
    const boundaryEntitiesCreated: any[] = [];
    const boundaryEntityCreateChunkSize = 200;
    const chunkSize = 20;
    const boundaryCodeChunks = [];
    for (let i = 0; i < boundaryCodes.length; i += chunkSize) {
      boundaryCodeChunks.push(boundaryCodes.slice(i, i + chunkSize));
    }

    for (const chunk of boundaryCodeChunks) {
      const string = chunk.join(', ');
      const boundaryEntityResponse = await httpRequest(config.host.boundaryHost + config.paths.boundaryServiceSearch, request.body, { tenantId: request?.body?.ResourceDetails?.tenantId, codes: string });
      const boundaryCodesFromResponse = boundaryEntityResponse.Boundary.flatMap((boundary: any) => boundary.code.toString());
      codesFromResponse.push(...boundaryCodesFromResponse);
    }

    const codeSet = new Set(codesFromResponse);// Creating a set and filling it with the codes from the response
    for (const { key: boundaryName, value: boundaryCode } of updatedBoundaryMap) {
      if (!codeSet.has(boundaryCode.toString())) {
        const boundary = {
          tenantId: request?.body?.ResourceDetails?.tenantId,
          code: boundaryCode,
          geometry: null,
          additionalDetails: {
            name: boundaryName
          }
        };
        boundaries.push(boundary);
      }
    };
    if (!(boundaries.length === 0)) {
      for (let i = 0; i < boundaries.length; i += boundaryEntityCreateChunkSize) {
        requestBody.Boundary = boundaries.slice(i, i + boundaryEntityCreateChunkSize);
        const response = await httpRequest(`${config.host.boundaryHost}boundary-service/boundary/_create`, requestBody, {}, 'POST',);
        boundaryEntitiesCreated.push(response)
      }
      logger.info('Boundary entities created');
      logger.debug('Boundary entities response: ' + getFormattedStringForDebug(boundaryEntitiesCreated));
    }
    else {
      // throwError("COMMON", 400, "VALIDATION_ERROR", "Boundary entity already present in the system");
      logger.info("Boundary Entities are already in the system")
    }
  } catch (error) {
    throwError("COMMMON", 500, "INTERNAL_SERVER_ERROR", "Error while Boundary Entity Creation")
  }
}

async function confirmBoundaryParentCreation(request: any, code: any) {
  if (code) {
    const searchBody = {
      RequestInfo: request.body.RequestInfo,
    }
    const params = {
      hierarchyType: request?.body?.ResourceDetails?.hierarchyType,
      tenantId: request?.body?.ResourceDetails?.tenantId,
      codes: code
    }
    var retry = 6;
    var boundaryFound = false;
    while (!boundaryFound && retry >= 0) {
      const response = await httpRequest(config.host.boundaryHost + config.paths.boundaryRelationship, searchBody, params);
      if (response?.TenantBoundary?.[0].boundary?.[0]) {
        boundaryFound = true;
      }
      else {
        logger.info("Boundary not found. Waiting for 1 seconds");
        retry = retry - 1
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    }
    if (!boundaryFound) {
      throwError("BOUNDARY", 500, "INTERNAL_SERVER_ERROR", "Boundary creation failed, for the boundary with code " + code);
    }
  }
}

/**
 * Asynchronously creates boundary relationships based on the provided request, boundary type map, and modified child-parent map.
 * @param request The HTTP request object.
 * @param boundaryTypeMap Map of boundary codes to types.
 * @param modifiedChildParentMap Modified child-parent map.
 */
async function createBoundaryRelationship(request: any, boundaryMap: Map<{ key: string, value: string }, string>, modifiedChildParentMap: Map<string, string | null>) {
  try {

    const updatedBoundaryMap: Array<{ key: string, value: string }> = Array.from(boundaryMap).map(([key, value]) => ({ key: value, value: key.key }));

    let activityMessage: any[] = [];
    const requestBody = { "RequestInfo": request.body.RequestInfo } as { RequestInfo: any; BoundaryRelationship?: any };
    const url = `${config.host.boundaryHost}${config.paths.boundaryRelationship}`;
    const params = {
      "type": request?.body?.ResourceDetails?.type,
      "tenantId": request?.body?.ResourceDetails?.tenantId,
      "boundaryType": null,
      "codes": null,
      "includeChildren": true,
      "hierarchyType": request?.body?.ResourceDetails?.hierarchyType
    };

    const boundaryRelationshipResponse = await httpRequest(url, request.body, params);
    const boundaryData = boundaryRelationshipResponse?.TenantBoundary?.[0]?.boundary;
    const allCodes = extractCodesFromBoundaryRelationshipResponse(boundaryData);

    let flag = 1;

    for (const { key: boundaryCode, value: boundaryType } of updatedBoundaryMap) {
      if (!allCodes.has(boundaryCode)) {
        const boundary = {
          tenantId: request?.body?.ResourceDetails?.tenantId,
          boundaryType: boundaryType,
          code: boundaryCode,
          hierarchyType: request?.body?.ResourceDetails?.hierarchyType,
          parent: modifiedChildParentMap.get(boundaryCode) || null
        };

        flag = 0;
        requestBody.BoundaryRelationship = boundary;
        // Introducing a delay of 1 second
        // await new Promise(resolve => setTimeout(resolve, config.boundary.boundaryRelationShipDelay));
        await confirmBoundaryParentCreation(request, modifiedChildParentMap.get(boundaryCode) || null);
        try {
          const response = await httpRequest(`${config.host.boundaryHost}${config.paths.boundaryRelationshipCreate}`, requestBody, {}, 'POST', undefined, undefined, true);

          if (!response.TenantBoundary || !Array.isArray(response.TenantBoundary) || response.TenantBoundary.length === 0) {
            throwError("BOUNDARY", 500, "BOUNDARY_RELATIONSHIP_CREATE_ERROR");
          }
          logger.info(`Boundary relationship created for boundaryType :: ${boundaryType} & boundaryCode :: ${boundaryCode} `);

          const newRequestBody = JSON.parse(JSON.stringify(request.body));
          activityMessage.push(await generateActivityMessage(request?.body?.ResourceDetails?.tenantId, request.body, newRequestBody, response, request?.body?.ResourceDetails?.type, url, response?.statusCode));
        } catch (error) {
          // Log the error and rethrow to be caught by the outer try...catch block
          logger.error(`Error creating boundary relationship for boundaryType :: ${boundaryType} & boundaryCode :: ${boundaryCode} :: `, error);
          throw error;
        }
      }
    };

    if (flag === 1) {
      throwError("COMMON", 400, "VALIDATION_ERROR", "Boundary already present in the system");
    }

    request.body = {
      ...request.body,
      Activities: activityMessage
    };
  } catch (error: any) {
    const errorCode = error.code || "INTERNAL_SERVER_ERROR";
    const errorMessage = error.description || "Error while boundary relationship create";
    logger.error(`Error in createBoundaryRelationship: ${errorMessage}`, error);
    throwError("COMMON", 500, errorCode, errorMessage);
  }
}



async function callMdmsData(
  request: any,
  moduleName: string,
  masterName: string,
  tenantId: string
) {
  const { RequestInfo = {} } = request?.body || {};
  const requestBody = {
    RequestInfo,
    MdmsCriteria: {
      tenantId: tenantId,
      moduleDetails: [
        {
          moduleName: moduleName,
          masterDetails: [
            {
              name: masterName,
            },
          ],
        },
      ],
    },
  };
  const url = config.host.mdms + config.paths.mdms_v1_search;
  const response = await httpRequest(url, requestBody, { tenantId: tenantId });
  return response;
}

async function callMdmsSchema(
  request: any,
  moduleName: string,
  masterName: string,
  tenantId: string
) {
  const { RequestInfo = {} } = request?.body || {};
  const requestBody = {
    RequestInfo,
    SchemaDefCriteria: {
      tenantId: tenantId,
      limit: 5,
      codes: [`${moduleName}.${masterName}`]
    }
  };
  const url = config.host.mdmsV2 + config.paths.mdmsV2SchemaSearch;
  const response = await httpRequest(url, requestBody);
  if (!response?.SchemaDefinitions?.[0]?.definition) {
    throwError("COMMON", 500, "INTERNAL_SERVER_ERROR", "Error occured during schema search");
  }
  return response?.SchemaDefinitions?.[0]?.definition;
}

async function getMDMSV1Data(request: any, moduleName: string, masterName: string, tenantId: string) {
  const resp = await callMdmsData(request, moduleName, masterName, tenantId);
  return resp;
}

export {
  getAutoGeneratedBoundaryCodes,
  getAutoGeneratedBoundaryCodesHandler,
  createBoundaryEntities,
  createBoundaryRelationship,
  getWorkbook,
  getSheetData,
  searchMDMS,
  getCampaignNumber,
  getSchema,
  getResouceNumber,
  getCount,
  getBoundarySheetData,
  createAndUploadFile,
  createRelatedResouce,
  createExcelSheet,
  generateHierarchy,
  generateHierarchyList,
  getTargetWorkbook,
  getTargetSheetData,
  callMdmsData,
  getMDMSV1Data,
  callMdmsSchema
}
