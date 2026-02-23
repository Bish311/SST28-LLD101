/**
 * Contract:
 * - Must accept any non-null ExportRequest and return a non-null ExportResult.
 * - Must never throw exceptions for valid inputs.
 * - If the format cannot handle the given content, return an error ExportResult
 *   (bytes may be empty, errorMessage will be set).
 * - For null requests, throw NullPointerException (fail-fast).
 */
public abstract class Exporter {
    public abstract ExportResult export(ExportRequest req);
}
