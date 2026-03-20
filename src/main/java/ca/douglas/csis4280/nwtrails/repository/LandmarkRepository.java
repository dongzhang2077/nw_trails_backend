package ca.douglas.csis4280.nwtrails.repository;

import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LandmarkRepository extends MongoRepository<Landmark, String> {

    List<Landmark> findByCategory(LandmarkCategory category);

    List<Landmark> findByNameContainingIgnoreCase(String name);

    List<Landmark> findByCategoryAndNameContainingIgnoreCase(LandmarkCategory category, String name);
}
