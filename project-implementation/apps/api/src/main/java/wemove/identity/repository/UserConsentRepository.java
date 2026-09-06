package wemove.identity.repository;

import wemove.identity.domain.UserConsentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsentEntity, UUID> {}
