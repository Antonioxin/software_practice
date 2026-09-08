package wemove.content;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface HomeSettingsRepository extends JpaRepository<HomeSettings, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from HomeSettings s where s.id = :id")
    Optional<HomeSettings> lock(@Param("id") UUID id);
}
