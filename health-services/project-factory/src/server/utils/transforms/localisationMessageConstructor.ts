import {
  getLocaleFromRequest,
  getLocalisationModuleName,
} from "../localisationUtils";
import localisationController from "../../controllers/localisationController/localisation.controller";
import { logger } from "../logger";

/**
 * Transforms boundary map into localisation messages and creates localisation entries.
 * @param boundaryMap - Map of boundary keys and codes.
 * @param hierarchyType - Type of hierarchy for the localisation module.
 * @param request - Request object containing necessary information.
 */
export const transformAndCreateLocalisation = (
    boundaryMap: any,
    request: any
  ) => {
    const {tenantId,hierarchyType}=request?.body?.ResourceDetails || {};

    // Get localisation module name based on hierarchy type
    const module = getLocalisationModuleName(hierarchyType);
    
    // Get locale from request object
    const locale = getLocaleFromRequest(request);
    
    // Array to store localisation messages
    const localisationMessages: any = [];
    
    // Iterate over boundary map to transform into localisation messagess
    Array.from(boundaryMap, ([message,code]) => {
      // Add transformed message to localisation messages array
      localisationMessages.push({
        code,
        message,
        module,
        locale,
      });

    })




    logger.info("localisation message transformed successfully from the boundary map")
    // Instantiate localisation controller
    const localisation = new localisationController();
    // Call method to create localisation entries
    localisation.createLocalisation(localisationMessages, tenantId,request);
  };
