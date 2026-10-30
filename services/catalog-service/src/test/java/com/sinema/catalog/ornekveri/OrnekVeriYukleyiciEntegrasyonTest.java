package com.sinema.catalog.ornekveri;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * "dev" profiliyle açılan uygulama boş katalogu doldurur. Bu context'in kendi MySQL container'ı vardır
 * (farklı yapılandırma = ayrı context), bu yüzden diğer testlerin verisi sayımları etkilemez.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(MysqlTestYapilandirmasi.class)
class OrnekVeriYukleyiciEntegrasyonTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrnekVeriYukleyici yukleyici;

    @Test
    void shouldSeedThreeCinemasWithThreeFourThreeHalls_whenStartedWithDevProfile() {
        List<Integer> salonSayilari = jdbcTemplate.queryForList(
                "SELECT COUNT(h.id) FROM cinemas c JOIN halls h ON h.cinema_id = c.id GROUP BY c.id ORDER BY c.id",
                Integer.class);

        assertThat(salonSayilari).containsExactly(3, 4, 3);
    }

    @Test
    void shouldGiveEveryHallItsOwnPublishedActiveLayout() {
        Integer aktifDuzenliSalon = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM halls h JOIN hall_layouts l ON l.id = h.active_layout_id
                WHERE l.status = 'PUBLISHED' AND l.version = 1""", Integer.class);

        assertThat(aktifDuzenliSalon).isEqualTo(10);
    }

    @Test
    void shouldCreateFutureShowtimesWithoutOverlap() {
        Integer seans = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        Integer gecmis = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM showtimes WHERE starts_at <= UTC_TIMESTAMP(3)", Integer.class);
        Integer cakisan = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM showtimes a JOIN showtimes b
                  ON a.hall_id = b.hall_id AND a.id < b.id AND a.starts_at < b.ends_at AND b.starts_at < a.ends_at""",
                Integer.class);

        assertThat(seans).isGreaterThan(200);
        assertThat(gecmis).isZero();
        assertThat(cakisan).isZero();
    }

    @Test
    void shouldNotDuplicateData_whenRunAgainOnFilledCatalog() throws Exception {
        Integer onceki = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movies", Integer.class);

        yukleyici.run(new DefaultApplicationArguments());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movies", Integer.class)).isEqualTo(onceki);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cinemas", Integer.class)).isEqualTo(3);
    }
}
