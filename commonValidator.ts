//src/utils/commonValidator.ts
import { FieldRule } from "../../../utils/commonValidator";

export const createSalesFunnelRules: Record<string, FieldRule> = {

    locationId: {
        type: "integer",
        required: true
    },

    locationFunctionId: {
        type: "integer",
        required: true
    },

    salesFunnelName: {
        type: "string",
        required: true,
        maxLength: 100
    },

    urlSlug: {
        type: "string",
        required: true
    },

    isActive: {
        type: "boolean",
        required: true
    },

    isDateRequired: {
        type: "boolean",
        required: true
    },

    salesCutoffLocalTime: {
        type: "string",
        required: false,
        pattern: /^(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$/,
        patternMessage: "salesCutoffLocalTime must be in HH:mm:ss format"
    },

    isWebEnabled: {
        type: "boolean",
        required: true
    },

    isPosEnabled: {
        type: "boolean",
        required: true
    },

    amenityIds: {
        type: "integerArray",
        required: false
    }
};
