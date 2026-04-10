package ca.douglas.csis4280.nwtrails.repository;

import ca.douglas.csis4280.nwtrails.domain.RoutePlan;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RouteRepository extends MongoRepository<RoutePlan, String> {

    List<RoutePlan> findByDifficultyIgnoreCase(String difficulty);
}
