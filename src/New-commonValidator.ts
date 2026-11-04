//src/utils/commonValidator.ts
export type FieldType =
    | "string"
    | "integer"
    | "boolean"
    | "integerArray";

export interface FieldRule {
    type: FieldType;
    required?: boolean;
    maxLength?: number;
    pattern?: RegExp;
    patternMessage?: string;
}

export function validateRequest(
    request: Record<string, unknown>,
    rules: Record<string, FieldRule>
): string[] {

    const errors: string[] = [];

    for (const [fieldName, rule] of Object.entries(rules)) {

        const value = request[fieldName];

        // Required field validation
        if (value === null || value === undefined) {
            if (rule.required) {
                errors.push(`${fieldName} is required`);
            }
            continue;
        }

        // String validation
        if (rule.type === "string") {

            if (typeof value !== "string") {
                errors.push(`${fieldName} must be a string`);
                continue;
            }

            if (rule.required && value.trim() === "") {
                errors.push(`${fieldName} cannot be empty`);
                continue;
            }

            if (rule.maxLength && value.length > rule.maxLength) {
                errors.push(
                    `${fieldName} cannot exceed ${rule.maxLength} characters`
                );
            }

            if (rule.pattern && !rule.pattern.test(value)) {
                errors.push(
                    rule.patternMessage || `${fieldName} has an invalid format`
                );
            }
        }

        // Integer validation
        if (rule.type === "integer") {
            if (typeof value !== "number" || !Number.isInteger(value)) {
                errors.push(`${fieldName} must be an integer`);
            }
        }

        // Boolean validation
        if (rule.type === "boolean") {
            if (typeof value !== "boolean") {
                errors.push(`${fieldName} must be a boolean`);
            }
        }

        // Integer array validation
        if (rule.type === "integerArray") {

            if (!Array.isArray(value)) {
                errors.push(`${fieldName} must be an array`);
                continue;
            }

            const hasInvalidValue = value.some(
                item => typeof item !== "number" || !Number.isInteger(item)
            );

            if (hasInvalidValue) {
                errors.push(`${fieldName} must contain only integers`);
            }
        }
    }

    return errors;
}
