package bw.co.gimble.cove.locations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Starter catalogue for Botswana, only on an empty table. Staff extend it from the back office. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSeeder implements ApplicationRunner {

    private final CityRepository cityRepository;
    private final AreaRepository areaRepository;

    private record Seed(String district, List<String> areas) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (cityRepository.count() > 0) return;
        Map<String, Seed> seed = new LinkedHashMap<>();
        seed.put("Gaborone", new Seed("South-East", List.of(
                "Block 3", "Block 5", "Block 6", "Block 7", "Block 8", "Block 9", "Block 10", "Broadhurst",
                "Bontleng", "CBD", "Extension 9", "Extension 10", "Extension 12", "Fairgrounds", "Gaborone North",
                "Gaborone West", "Gaborone Village", "Kgale View", "Mokolodi", "Phakalane", "Phase 2", "Phase 4",
                "Partial", "Main Mall", "Tsholofelo", "Sebele", "Maruapula")));
        seed.put("Francistown", new Seed("North-East", List.of(
                "Area W", "Blue Town", "Coloured", "Donga", "Gerald Estates", "Ipelegeng", "Kgaphamadi",
                "Monarch", "Satellite", "Somerset East", "Somerset West", "Tatitown", "Town Centre")));
        seed.put("Maun", new Seed("North-West", List.of("Boseja", "Botshabelo", "Disaneng", "Matlapana", "Sedie", "Thito", "Wenela")));
        seed.put("Kasane", new Seed("Chobe", List.of("Plateau", "Kazungula", "Kasane Central", "Lesoma")));
        seed.put("Mogoditshane", new Seed("Kweneng", List.of("Mogoditshane Central", "Block 1", "Ledumadumane", "Nkoyaphiri")));
        seed.put("Tlokweng", new Seed("South-East", List.of("Tlokweng Central", "Mafitlhakgosi", "Phiriyabokwete")));
        seed.put("Molepolole", new Seed("Kweneng", List.of("Molepolole Central", "Borakalalo", "Ntloedibe")));
        seed.put("Mochudi", new Seed("Kgatleng", List.of("Mochudi Central", "Boseja", "Phaphane")));
        seed.put("Lobatse", new Seed("South-East", List.of("Lobatse Central", "Peleng", "Woodhall", "Tsopeng")));
        seed.put("Palapye", new Seed("Central", List.of("Palapye Central", "Extension 5", "Lotsere", "Lerala Road")));
        seed.put("Serowe", new Seed("Central", List.of("Serowe Central", "Botalaote", "Mannonye")));
        seed.put("Mahalapye", new Seed("Central", List.of("Mahalapye Central", "Xhosa 1", "Madiba")));
        seed.put("Selebi-Phikwe", new Seed("Central", List.of("Selebi-Phikwe Central", "Botshabelo", "Extension 3")));
        seed.put("Jwaneng", new Seed("Southern", List.of("Jwaneng Central", "Unit 6", "Unit 8")));
        seed.put("Kanye", new Seed("Southern", List.of("Kanye Central", "Mafhikana", "Tsopeng")));
        seed.put("Ramotswa", new Seed("South-East", List.of("Ramotswa Central", "Taung")));
        seed.put("Letlhakane", new Seed("Central", List.of("Letlhakane Central")));
        seed.put("Orapa", new Seed("Central", List.of("Orapa Central")));
        seed.put("Ghanzi", new Seed("Ghanzi", List.of("Ghanzi Central")));
        seed.forEach((name, s) -> {
            City city = cityRepository.save(City.builder().name(name).district(s.district()).build());
            s.areas().forEach(a -> areaRepository.save(Area.builder().cityId(city.getId()).name(a).build()));
        });
        log.info("Seeded {} cities/towns into the location catalogue", seed.size());
    }
}
