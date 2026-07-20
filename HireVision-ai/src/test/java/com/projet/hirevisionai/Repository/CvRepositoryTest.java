package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CvRepositoryTest {

    @Autowired
    private CvRepository cvRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .fullName("Adem Ben")
                .email("adem@test.com")
                .password("pass")
                .age(22)
                .role(Role.CANDIDATE)
                .build();
        entityManager.persistAndFlush(user);
    }

    @Test
    void findByUserIdUser_shouldReturnAllCvsOfUser() {
        CV cv1 = CV.builder().filePath("cv1.pdf").uploadDate(LocalDate.now().minusDays(2)).user(user).build();
        CV cv2 = CV.builder().filePath("cv2.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(cv1);
        entityManager.persistAndFlush(cv2);

        List<CV> result = cvRepository.findByUserIdUser(user.getIdUser());

        assertThat(result).hasSize(2)
                .extracting(CV::getFilePath)
                .containsExactlyInAnyOrder("cv1.pdf", "cv2.pdf");
    }

    @Test
    void findByUserIdUser_shouldReturnEmptyList_whenUserHasNoCv() {
        List<CV> result = cvRepository.findByUserIdUser(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findTopByUserIdUserOrderByUploadDateDesc_shouldReturnMostRecentCv() {
        CV older = CV.builder().filePath("old.pdf").uploadDate(LocalDate.now().minusDays(5)).user(user).build();
        CV recent = CV.builder().filePath("recent.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(older);
        entityManager.persistAndFlush(recent);

        Optional<CV> result = cvRepository.findTopByUserIdUserOrderByUploadDateDesc(user.getIdUser());

        assertThat(result).isPresent();
        assertThat(result.get().getFilePath()).isEqualTo("recent.pdf");
    }

    @Test
    void countByUserIdUser_shouldReturnCorrectCount() {
        entityManager.persistAndFlush(CV.builder().filePath("a.pdf").uploadDate(LocalDate.now()).user(user).build());
        entityManager.persistAndFlush(CV.builder().filePath("b.pdf").uploadDate(LocalDate.now()).user(user).build());

        long count = cvRepository.countByUserIdUser(user.getIdUser());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByUploadDateAfter_shouldReturnOnlyRecentCvs() {
        CV old = CV.builder().filePath("old.pdf").uploadDate(LocalDate.now().minusDays(10)).user(user).build();
        CV recent = CV.builder().filePath("recent.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(old);
        entityManager.persistAndFlush(recent);

        List<CV> result = cvRepository.findByUploadDateAfter(LocalDate.now().minusDays(1));

        assertThat(result).extracting(CV::getFilePath).containsExactly("recent.pdf");
    }
}
