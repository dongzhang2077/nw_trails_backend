package ca.douglas.csis4280.nwtrails.config;

import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import ca.douglas.csis4280.nwtrails.repository.LandmarkRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final LandmarkRepository landmarkRepository;

    public DataSeeder(LandmarkRepository landmarkRepository) {
        this.landmarkRepository = landmarkRepository;
    }

    @Override
    public void run(String... args) {
        if (landmarkRepository.count() == 0) {
            landmarkRepository.saveAll(seedLandmarks());
        }
    }

    private List<Landmark> seedLandmarks() {
        return List.of(
            new Landmark("l1", "Irving House", LandmarkCategory.historic, "302 Royal Ave",
                "A preserved 19th century home museum.", 49.2064, -122.9094,
                "https://picsum.photos/seed/nw-l1/1200/700", 4.7),
            new Landmark("l2", "New Westminster Museum", LandmarkCategory.historic, "777 Columbia St",
                "Local history exhibitions and archives.", 49.2060, -122.9079,
                "https://picsum.photos/seed/nw-l2/1200/700", 4.5),
            new Landmark("l3", "City Hall", LandmarkCategory.historic, "511 Royal Ave",
                "Historic municipal building in downtown core.", 49.2070, -122.9119,
                "https://picsum.photos/seed/nw-l3/1200/700", 4.3),
            new Landmark("l4", "Westminster Pier Park", LandmarkCategory.historic, "1 Sixth St",
                "Riverfront park with boardwalk views.", 49.2046, -122.9119,
                "https://picsum.photos/seed/nw-l4/1200/700", 4.8),
            new Landmark("l5", "Queens Park", LandmarkCategory.nature, "First St",
                "Large urban park with open lawns and trails.", 49.2120, -122.9056,
                "https://picsum.photos/seed/nw-l5/1200/700", 4.9),
            new Landmark("l6", "Fraser River Trail", LandmarkCategory.nature, "Fraser River Waterfront",
                "Scenic walk route along the river.", 49.2043, -122.9110,
                "https://picsum.photos/seed/nw-l6/1200/700", 4.6),
            new Landmark("l7", "Hume Park", LandmarkCategory.nature, "660 East Columbia St",
                "Forest-style city park and recreation space.", 49.2067, -122.8963,
                "https://picsum.photos/seed/nw-l7/1200/700", 4.4),
            new Landmark("l8", "Tipperary Park", LandmarkCategory.nature, "315 Queens Ave",
                "Small downtown park near civic landmarks.", 49.2076, -122.9099,
                "https://picsum.photos/seed/nw-l8/1200/700", 4.2),
            new Landmark("l9", "River Market", LandmarkCategory.food, "810 Quayside Dr",
                "Popular food market by the waterfront.", 49.2028, -122.9121,
                "https://picsum.photos/seed/nw-l9/1200/700", 4.7),
            new Landmark("l10", "Columbia Street Cafes", LandmarkCategory.food, "Columbia St",
                "Coffee shops and local student hangouts.", 49.2060, -122.9090,
                "https://picsum.photos/seed/nw-l10/1200/700", 4.1),
            new Landmark("l11", "Steel and Oak Brewing", LandmarkCategory.food, "1319 Third Ave",
                "Local craft brewery with tasting room.", 49.2093, -122.9020,
                "https://picsum.photos/seed/nw-l11/1200/700", 4.6),
            new Landmark("l12", "Anvil Centre", LandmarkCategory.culture, "777 Columbia St",
                "Arts and culture venue with public events.", 49.2058, -122.9079,
                "https://picsum.photos/seed/nw-l12/1200/700", 4.5),
            new Landmark("l13", "Massey Theatre", LandmarkCategory.culture, "735 Eighth Ave",
                "Performance venue for concerts and shows.", 49.2124, -122.9054,
                "https://picsum.photos/seed/nw-l13/1200/700", 4.4),
            new Landmark("l14", "Douglas College New West", LandmarkCategory.culture, "700 Royal Ave",
                "Campus hub for Douglas College students.", 49.2071, -122.9115,
                "https://picsum.photos/seed/nw-l14/1200/700", 4.0),
            new Landmark("l15", "Samson V Maritime Museum", LandmarkCategory.culture, "Gallery at Quayside",
                "Historic paddlewheeler ship museum exhibit.", 49.2030, -122.9100,
                "https://picsum.photos/seed/nw-l15/1200/700", 4.3)
        );
    }
}
