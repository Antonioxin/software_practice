-- Java root package migration changes serialized UserPrincipal and enum class names.
-- Invalidate existing authenticated sessions once; users log in again after upgrade.
-- Anonymous sessions and all business data remain intact. Attributes cascade on delete.
DELETE FROM SPRING_SESSION
WHERE PRIMARY_ID IN (
    SELECT SESSION_PRIMARY_ID FROM SPRING_SESSION_ATTRIBUTES
    WHERE ATTRIBUTE_NAME = 'SPRING_SECURITY_CONTEXT'
);
