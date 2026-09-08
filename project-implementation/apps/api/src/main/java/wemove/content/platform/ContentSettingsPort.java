package wemove.content.platform;

/** Shared source of registration consent versions. */
public interface ContentSettingsPort {
    DocumentVersions currentDocumentVersions();
    /** Caller owns the write transaction; lock remains held through registration commit. */
    DocumentVersions lockDocumentVersions();
    record DocumentVersions(String termsVersion, String privacyVersion) {}
}
