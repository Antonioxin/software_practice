package wemove.content;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SiteSettings s where s.id = :id")
    Optional<SiteSettings> lock(@Param("id") UUID id);
}
